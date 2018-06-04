package org.anthillplatform.runtime.services;

import org.anthillplatform.runtime.requests.JsonRequest;
import org.anthillplatform.runtime.AnthillRuntime;
import org.anthillplatform.runtime.requests.Request;
import org.anthillplatform.runtime.requests.StringRequest;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.LinkedList;

/**
 * An authentication service for Anthill platform
 *
 * See https://github.com/anthill-platform/anthill-login
 */
public class LoginService extends Service
{
    public static final String ID = "login";
    public static final String API_VERSION = "0.2";

    private static AccessToken nullToken = new AccessToken();

    private AccessToken currentAccessToken;
    private ExternalAuthenticator externalAuthenticator;

    public static class Scopes extends HashSet<String>
    {
        public static Scopes ALL = new Scopes("*");

        public static Scopes FromString(String scopes)
        {
            return new Scopes(scopes.split(","));
        }

        public Scopes()
        {

        }

        public Scopes(String ... scopes)
        {
            for (String scope : scopes)
            {
                add(scope);
            }
        }

        public Scopes(JSONArray scopes)
        {
            for (int i = 0; i < scopes.length(); i++)
            {
                add(scopes.getString(i));
            }
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();

            for (String item : this)
            {
                if (sb.length() > 0)
                {
                    sb.append(",");
                }

                sb.append(item);
            }

            return sb.toString();
        }
    }

    public static class AccessToken
    {
        private String raw;

        private AccessToken()
        {
            this.raw = "";
        }

        private AccessToken(String raw)
        {
            this.raw = raw;
        }

        public String get()
        {
            return raw;
        }

        @Override
        public String toString()
        {
            return raw;
        }
    }

    public static class MergeOption
    {
        public String name;
        public String credential;
        public String account;
        public JSONObject profile;
    }

    public static class MergeOptions extends LinkedList<MergeOption> {}

    public interface MergeResolveCallback
    {
        void resolve(MergeOption selectedOption);
    }

    public interface MergeRequiredCallback
    {
        void run(LoginService service, MergeOptions options, MergeResolveCallback resolve);
    }

    public interface AuthenticationCallback
    {
        void complete(
            LoginService service, Request request, Request.Result result,
            AccessToken accessToken, String account, String credential, Scopes scopes);
    }

    public interface ValidationCallback
    {
        void complete(
            LoginService service, Request request, Request.Result result,
            String account, String credential, Scopes scopes);
    }

    public static abstract class ExternalAuthenticator
    {
        public abstract String getCredentialType();

        public abstract void authenticate(
            LoginService loginService,
			String gamespace,
            LoginService.Scopes scopes,
			Request.Fields other,
            LoginService.AuthenticationCallback callback,
            LoginService.MergeRequiredCallback mergeRequiredCallback,
            LoginService.Scopes shouldHaveScopes);

        public abstract void attach(
            LoginService loginService,
			String gamespace,
			LoginService.Scopes scopes,
			Request.Fields other,
            LoginService.AuthenticationCallback callback,
            LoginService.MergeRequiredCallback mergeRequiredCallback,
            LoginService.Scopes shouldHaveScopes);
    };

    /**
     * Please note that you should not create an instance of the service yourself,
     * and use AnthillRuntime.Get(LoginService.ID, LoginService.class) to get existing one instead
     */
    public LoginService(AnthillRuntime runtime, String location)
    {
        super(runtime, location, ID, API_VERSION);

        currentAccessToken = null;
    }

    public static LoginService Get()
    {
        return AnthillRuntime.Get(ID, LoginService.class);
    }

    public void authenticate(
        String credentialType,
        String gamespace,
        Scopes scopes,
        Request.Fields other,
        final AuthenticationCallback callback,
        MergeRequiredCallback mergeRequiredCallback)
    {
        authenticate(credentialType, gamespace, scopes, other, callback, mergeRequiredCallback, Scopes.ALL);
    }

    public void authenticate(
        String credentialType,
        String gamespace,
        Scopes scopes,
        Request.Fields other,
        final AuthenticationCallback callback,
        MergeRequiredCallback mergeRequiredCallback,
        Scopes shouldHaveScopes)
    {
        JsonRequest request = new JsonRequest(getLocation() + "/auth",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                switch (result)
                {
                    case success:
                    {
                        JSONObject value = ((JsonRequest) request).getObject();

                        AccessToken accessToken = new AccessToken(value.getString("token"));
                        Scopes scopes;
                        String credential = "";
                        String account = "";

                        if (value.has("scopes"))
                        {
                            scopes = new Scopes(value.optJSONArray("scopes"));
                        }
                        else
                        {
                            scopes = new Scopes();
                        }

                        if (value.has("credential"))
                            credential = value.optString("credential");

                        if (value.has("account"))
                            account = value.optString("account");

                        callback.complete(
                            LoginService.this, request, result,
                                accessToken, account, credential, scopes);

                        break;
                    }
                    case multipleChoices:
                    {


                        break;
                    }
                    default:
                    {
                        callback.complete(
                            LoginService.this, request, result,
                                nullToken, "", "", new Scopes());

                        break;
                    }
                }
            }
        });

        Request.Fields data = new Request.Fields();

        data.put("credential", credentialType);
        data.put("scopes", scopes);
        data.put("should_have", shouldHaveScopes);
        data.put("gamespace", getRuntime().getApplicationInfo().gamespace);
        data.put("full", "true");

        data.putAll(other);

        request.setAPIVersion(getAPIVersion());
        request.post(data);
    }


    public void attach(
        AccessToken accessToken,
        String gamespace,
        String credentialType,
        Scopes scopes,
        Request.Fields other,
        final AuthenticationCallback callback,
        MergeRequiredCallback mergeRequiredCallback,
        Scopes shouldHaveScopes)
    {
        if (other == null)
        {
            other = new Request.Fields();
        }

        other.put("attach_to", accessToken.get());

        authenticate(credentialType, gamespace, scopes, other, callback, mergeRequiredCallback, shouldHaveScopes);
    }

    public void attach(
        AccessToken accessToken,
        String gamespace,
        String credentialType,
        Scopes scopes,
        Request.Fields other,
        final AuthenticationCallback callback,
        MergeRequiredCallback mergeRequiredCallback)
    {
        if (other == null)
        {
            other = new Request.Fields();
        }

        other.put("attach_to", accessToken.get());

        authenticate(credentialType, gamespace, scopes, other, callback, mergeRequiredCallback);
    }

    public void extend(
        AccessToken accessToken,
        AccessToken extendWith, 
        LoginService.Scopes scopes, 
        final AuthenticationCallback callback)
    {
        JsonRequest request = new JsonRequest(getLocation() + "/extend",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                if (result == Request.Result.success)
                {
                    JSONObject value = ((JsonRequest) request).getObject();

                    AccessToken accessToken = new AccessToken(value.getString("token"));
                    Scopes scopes;
                    String credential = "";
                    String account = "";

                    if (value.has("scopes"))
                    {
                        scopes = new Scopes(value.optJSONArray("scopes"));
                    }
                    else
                    {
                        scopes = new Scopes();
                    }

                    if (value.has("credential"))
                        credential = value.optString("credential");

                    if (value.has("account"))
                        account = value.optString("account");

                    callback.complete(
                        LoginService.this, request, result,
                            accessToken, account, credential, scopes);
                }
                else
                {
                    callback.complete(
                        LoginService.this, request, result,
                            nullToken, "", "", new Scopes());
                }
            }
        });

        request.setToken(accessToken);

        Request.Fields data = new Request.Fields();
        data.put("extend", extendWith.get());
        data.put("scopes", scopes);

        request.setAPIVersion(getAPIVersion());
        request.post(data);
    }

    public AccessToken getCurrentAccessToken()
    {
        return currentAccessToken;
    }

    public void authAnonymous(
        String anonymousId,
        String key,
        String gamespace,
        Scopes scopes,
        Request.Fields other,
        AuthenticationCallback callback,
        MergeRequiredCallback mergeRequiredCallback,
        Scopes shouldHaveScopes)
    {
        Request.Fields _options = new Request.Fields();

        _options.put("username", anonymousId);
        _options.put("key", key);

        if (other != null)
        {
            _options.putAll(other);
        }

        authenticate("anonymous", gamespace, scopes, _options, callback, mergeRequiredCallback, shouldHaveScopes);
    }

    public void authDev(
        String username,
        String password,
        String gamespace,
        Scopes scopes,
        Request.Fields other,
        AuthenticationCallback callback,
        MergeRequiredCallback mergeRequiredCallback,
        Scopes shouldHaveScopes)
    {
        Request.Fields _options = new Request.Fields();

        _options.put("username", username);
        _options.put("key", password);

        if (other != null)
        {
            _options.putAll(other);
        }

        authenticate("dev", gamespace, scopes, _options, callback, mergeRequiredCallback, shouldHaveScopes);
    }

    public void validateAccessToken(
        final ValidationCallback callback)
    {
        validateAccessToken(getCurrentAccessToken(), callback);
    }

    public void validateAccessToken(
        final AccessToken token,
        final ValidationCallback callback)
    {
        JsonRequest request = new JsonRequest(getLocation() + "/validate",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                if (result == Request.Result.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    Scopes scopes;
                    String credential = "";
                    String account = "";

                    if (response.has("scopes"))
                    {
                        scopes = new Scopes(response.optJSONArray("scopes"));
                    }
                    else
                    {
                        scopes = new Scopes();
                    }

                    if (response.has("credential"))
                        credential = response.optString("credential");

                    if (response.has("account"))
                        account = response.optString("account");

                    callback.complete(LoginService.this, request, result, account, credential, scopes);
                }
                else
                {
                    callback.complete(LoginService.this, request, result, "", "", new Scopes());
                }
            }
        });

        request.setToken(token);
        request.get();
    }

    public void resolve(
        AccessToken resolveToken,
        String methodToResolve,
        String resolveWith,
        Scopes scopes,
        Request.Fields other,
        final AuthenticationCallback callback)
    {
        resolve(resolveToken, methodToResolve, resolveWith, scopes, other, callback, Scopes.ALL, null);
    }

    public void resolve(
        AccessToken resolveToken,
        String methodToResolve,
        String resolveWith,
        Scopes scopes,
        Request.Fields other,
        final AuthenticationCallback callback,
        Scopes shouldHaveScopes,
        AccessToken attachTo)
    {
        JsonRequest request = new JsonRequest(getLocation() + "/resolve",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                if (result == Request.Result.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    AccessToken accessToken = new AccessToken(response.getString("token"));
                    Scopes scopes;
                    String credential = "";
                    String account = "";

                    if (response.has("scopes"))
                    {
                        scopes = new Scopes(response.optJSONArray("scopes"));
                    }
                    else
                    {
                        scopes = new Scopes();
                    }

                    if (response.has("credential"))
                        credential = response.optString("credential");

                    if (response.has("account"))
                        account = response.optString("account");

                    callback.complete(
                        LoginService.this, request, result,
                            accessToken, account, credential, scopes);
                }
                else
                {
                    callback.complete(
                        LoginService.this, request, result,
                            nullToken, "", "", new Scopes());
                }
            }
        });

        Request.Fields _options = new Request.Fields();

        _options.put("scopes", scopes);
        _options.put("full", "true");
        _options.put("resolve_method", methodToResolve);
        _options.put("resolve_with", resolveWith);
        _options.put("access_token", resolveToken);
        _options.put("should_have", shouldHaveScopes.toString());

        if (attachTo != null)
        {
            _options.put("attach_to", attachTo.get());
        }

        if (other != null)
        {
            _options.putAll(other);
        }

        request.setAPIVersion(getAPIVersion());
        request.post(_options);
    }

    public AccessToken newAccessToken(String raw)
    {
        return new AccessToken(raw);
    }

    public void clearCurrentAccessToken()
    {
        currentAccessToken = null;
    }

    public void setCurrentAccessToken(AccessToken currentAccessToken)
    {
        this.currentAccessToken = currentAccessToken;
    }

    public AccessToken setCurrentAccessToken(String raw)
    {
        this.currentAccessToken = new AccessToken(raw);
        return this.currentAccessToken;
    }

    public void setExternalAuthenticator(ExternalAuthenticator externalAuthenticator)
    {
        this.externalAuthenticator = externalAuthenticator;
    }

    public ExternalAuthenticator getExternalAuthenticator()
    {
        return externalAuthenticator;
    }
}
