package org.anthillplatform.runtime.services;

import org.anthillplatform.runtime.AnthillRuntime;
import org.anthillplatform.runtime.requests.JsonRequest;
import org.anthillplatform.runtime.requests.Request;
import org.anthillplatform.runtime.requests.StringRequest;
import org.json.JSONObject;

/**
 * User profiles service for Anthill platform
 *
 * See https://github.com/anthill-platform/anthill-profile
 */
public class ProfileService extends Service
{
    public static final String ID = "profile";
    public static final String API_VERSION = "0.2";

    /**
     * Please note that you should not create an instance of the service yourself,
     * and use AnthillRuntime.Get(ProfileService.ID, ProfileService.class) to get existing one instead
     */
    public ProfileService(AnthillRuntime runtime, String location)
    {
        super(runtime, location, ID, API_VERSION);
    }

    public static ProfileService Get()
    {
        return AnthillRuntime.Get(ID, ProfileService.class);
    }

    public interface GetProfileCallback
    {
        void complete(ProfileService profileService, Request request, Request.Result result, JSONObject profile);
    }

    public interface UpdateProfileCallback
    {
        void complete(ProfileService profileService, Request request, Request.Result result, JSONObject profile);
    }

    public interface UpdateProfilesCallback
    {
        void complete(ProfileService profileService, Request request, Request.Result result, JSONObject profiles);
    }

    public void getMyProfile(LoginService.AccessToken accessToken, final GetProfileCallback callback)
    {
        getAccountProfile(accessToken, "me", callback);
    }

    public void getAccountProfile(
            LoginService.AccessToken accessToken, final String account,
            final GetProfileCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getLocation() + "/profile/" + account,
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                if (result == Request.Result.success)
                {
                    JsonRequest asJson = ((JsonRequest) request);
                    callback.complete(ProfileService.this, request, result, asJson.getObject());
                }
                else
                {
                    callback.complete(ProfileService.this, request, result, null);
                }
            }
        });

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.get();
    }

    public void createAccountProfile(
        LoginService.AccessToken accessToken,
        String account,
        JSONObject ext,
        final UpdateProfileCallback callback)
    {
        StringRequest jsonRequest = new StringRequest(getRuntime(),
            getLocation() + "/profile/" + account,
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                callback.complete(ProfileService.this, request, result, null);
            }
        });

        Request.Fields options = new Request.Fields();

        options.put("data", ext.toString());

        jsonRequest.setToken(accessToken);
        jsonRequest.post(options);
    }

    public void createMyProfile(
        LoginService.AccessToken accessToken,
        JSONObject ext,
        final UpdateProfileCallback callback)
    {
        createAccountProfile(accessToken, "me", ext, callback);
    }

    public void updateAccountProfile(
        LoginService.AccessToken accessToken,
        String account,
        JSONObject ext,
        String path,
        boolean merge,
        final UpdateProfileCallback callback)
    {
        final JsonRequest jsonRequest = new JsonRequest(
            getLocation() + "/profile/" + account + (path != null ? "/" + path : ""),
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                if (result == Request.Result.success)
                {
                    JsonRequest asJson = ((JsonRequest) request);
                    callback.complete(ProfileService.this, request, result, asJson.getObject());
                }
                else
                {
                    callback.complete(ProfileService.this, request, result, null);
                }
            }
        });

        Request.Fields options = new Request.Fields();

        options.put("data", ext.toString());
        options.put("merge", merge ? "true" : "false");

        jsonRequest.setToken(accessToken);
        jsonRequest.post(options);
    }

    public void updateMultipleAccountProfiles(
        LoginService.AccessToken accessToken,
        JSONObject accounts,
        boolean merge,
        final UpdateProfilesCallback callback)
    {
        final JsonRequest jsonRequest = new JsonRequest(
            getLocation() + "/profiles",
            new Request.RequestCallback()
            {
                @Override
                public void complete(Request request, Request.Result result)
                {
                    if (result == Request.Result.success)
                    {
                        JsonRequest asJson = ((JsonRequest) request);
                        callback.complete(ProfileService.this, request, result, asJson.getObject());
                    }
                    else
                    {
                        callback.complete(ProfileService.this, request, result, null);
                    }
                }
            });

        Request.Fields options = new Request.Fields();

        options.put("data", accounts.toString());
        options.put("merge", merge ? "true" : "false");

        jsonRequest.setToken(accessToken);
        jsonRequest.post(options);
    }

    public void updateMyProfile(
        LoginService.AccessToken accessToken,
        JSONObject ext,
        String path,
        boolean merge,
        final UpdateProfileCallback callback)
    {
        updateAccountProfile(accessToken, "me", ext, path, merge, callback);
    }
}
