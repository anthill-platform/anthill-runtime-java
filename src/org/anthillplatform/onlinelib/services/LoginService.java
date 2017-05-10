package org.anthillplatform.onlinelib.services;

import org.anthillplatform.onlinelib.request.JsonRequest;
import org.anthillplatform.onlinelib.OnlineLib;
import org.anthillplatform.onlinelib.Status;
import org.anthillplatform.onlinelib.entity.AccessToken;
import org.anthillplatform.onlinelib.request.Request;
import org.anthillplatform.onlinelib.request.StringRequest;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LoginService extends Service
{
    public static final String ID = "login";

    private static LoginService instance;
    public static LoginService get() { return instance; }
    private static void set(LoginService service) { instance = service; }

    private AccessToken currentAccessToken;

    public enum CredentialType
    {
        anonymous,
        dev
    }

    public interface AuthCallback
    {
        void success(AccessToken token);
        void error(Status status, JSONObject response);
    }

    public LoginService(OnlineLib onlineLib, String location)
    {
        super(onlineLib, location, ID);

        set(this);

        currentAccessToken = null;
    }

    public void auth(CredentialType credentialType, String scopes,
                     Map<String, String> options, final AuthCallback callback)
    {
        auth(credentialType.toString(), scopes, options, callback);
    }

    public void auth(String credentialType, String scopes,
                     Map<String, String> options, final AuthCallback callback)
    {
        JsonRequest request = new JsonRequest(getOnlineLib(), getLocation() + "/auth",
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request1, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject response = ((JsonRequest) request1).getObject();
                    callback.success(LoginService.this.parse(response));
                } else
                {
                    callback.error(status, ((JsonRequest) request1).getObject());
                }
            }
        });

        Map<String, Object> data = new HashMap<String, Object>();

        data.put("credential", credentialType);
        data.put("scopes", scopes);
        data.put("gamespace", getOnlineLib().getApplicationInfo().getGamespace());
        data.put("full", "true");

        data.putAll(options);

        request.post(data);
    }

    public void extend(AccessToken token, AccessToken extend, String scopes, final AuthCallback callback)
    {
        JsonRequest request = new JsonRequest(getOnlineLib(), getLocation() + "/extend",
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request1, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject response = ((JsonRequest) request1).getObject();
                    callback.success(LoginService.this.parse(response));
                } else
                {
                    callback.error(status, ((JsonRequest) request1).getObject());
                }
            }
        });

        request.setToken(token);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("extend", extend.getToken());
        data.put("scopes", scopes);

        request.post(data);
    }

    public AccessToken getCurrentAccessToken()
    {
        return currentAccessToken;
    }

    private AccessToken parse(JSONObject response)
    {
        return new AccessToken(response.get("token").toString());
    }

    public void authAnonymous(String anonymousId, String key, String scopes, Map<String, String> options,
                              AuthCallback callback)
    {
        Map<String, String> _options = new HashMap<String, String>();

        _options.put("username", anonymousId);
        _options.put("key", key);

        if (options != null)
        {
            _options.putAll(options);
        }

        auth(CredentialType.anonymous, scopes, _options, callback);
    }

    public void authDev(String username, String password, String scopes, Map<String, String> options,
        AuthCallback callback)
    {
        Map<String, String> _options = new HashMap<String, String>();

        _options.put("username", username);
        _options.put("key", password);

        if (options != null)
        {
            _options.putAll(options);
        }

        auth(CredentialType.dev, scopes, _options, callback);
    }

    public void validate(final String token, final AuthCallback callback)
    {
        StringRequest request = new StringRequest(getOnlineLib(),
                getLocation() + "/validate",
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request1, Status status)
            {
                if (status == Status.success)
                {
                    callback.success(new AccessToken(token));
                } else
                {
                    callback.error(status, null);
                }
            }
        });

        Map<String, String> _options = new HashMap<String, String>();

        _options.put("access_token", token);
        request.setQueryArguments(_options);
        request.get();
    }

    public void resolve(String method, String with, String scopes, Map<String, String> options, String resolveToken,
                        final AuthCallback callback)
    {
        JsonRequest request = new JsonRequest(getOnlineLib(), getLocation() + "/resolve",
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request1, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject response = ((JsonRequest) request1).getObject();
                    callback.success(LoginService.this.parse(response));
                } else
                {
                    callback.error(status, ((JsonRequest) request1).getObject());
                }
            }
        });

        Map<String, Object> _options = new HashMap<String, Object>();

        _options.put("scopes", scopes);
        _options.put("full", "true");
        _options.put("resolve_method", method);
        _options.put("resolve_with", with);
        _options.put("access_token", resolveToken);

        if (options != null)
        {
            _options.putAll(options);
        }

        request.post(_options);
    }

    public void clearCurrentAccessToken()
    {
        currentAccessToken = null;
    }

    public void setCurrentAccessToken(AccessToken currentAccessToken)
    {
        this.currentAccessToken = currentAccessToken;
    }
}
