package org.anthillplatform.runtime.services;

import org.anthillplatform.runtime.request.JsonRequest;
import org.anthillplatform.runtime.AnthillRuntime;
import org.anthillplatform.runtime.Status;
import org.anthillplatform.runtime.entity.AccessToken;
import org.anthillplatform.runtime.request.Request;
import org.anthillplatform.runtime.request.StringRequest;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * An authentication service for Anthill platform
 *
 * See https://github.com/anthill-platform/anthill-login
 */
public class LoginService extends Service
{
    public static final String ID = "login";
    public static final String API_VERSION = "0.2";

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

    /**
     * Please note that you should not create an instance of the service yourself,
     * and use LoginService.get() to get existing one instead
     */
    public LoginService(AnthillRuntime runtime, String location)
    {
        super(runtime, location, ID, API_VERSION);

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
        JsonRequest request = new JsonRequest(getRuntime(), getLocation() + "/auth",
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
        data.put("gamespace", getRuntime().getApplicationInfo().getGamespace());
        data.put("full", "true");

        data.putAll(options);

        request.setAPIVersion(getAPIVersion());
        request.post(data);
    }

    public void extend(AccessToken token, AccessToken extend, String scopes, final AuthCallback callback)
    {
        JsonRequest request = new JsonRequest(getRuntime(), getLocation() + "/extend",
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

        request.setAPIVersion(getAPIVersion());
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
        StringRequest request = new StringRequest(getRuntime(),
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
        JsonRequest request = new JsonRequest(getRuntime(), getLocation() + "/resolve",
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

        request.setAPIVersion(getAPIVersion());
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
