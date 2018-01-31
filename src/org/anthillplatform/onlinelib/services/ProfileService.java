package org.anthillplatform.onlinelib.services;

import org.anthillplatform.onlinelib.OnlineLib;
import org.anthillplatform.onlinelib.Status;
import org.anthillplatform.onlinelib.entity.AccessToken;
import org.anthillplatform.onlinelib.request.JsonRequest;
import org.anthillplatform.onlinelib.request.Request;
import org.anthillplatform.onlinelib.request.StringRequest;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ProfileService extends Service
{
    public static final String ID = "profile";
    public static final String API_VERSION = "0.2";

    private static ProfileService instance;
    public static ProfileService get() { return instance; }
    private static void set(ProfileService service) { instance = service; }

    private Map<String, Profile> profiles;

    public class Profile
    {
        private JSONObject ext;

        @SuppressWarnings("SuspiciousMethodCalls")
        public Profile(JSONObject object)
        {
            this.ext = object;
        }

        public ProfileService getService()
        {
            return ProfileService.this;
        }

        public JSONObject getObject()
        {
            return ext;
        }
    }

    public ProfileService(OnlineLib onlineLib, String location)
    {
        super(onlineLib, location, ID, API_VERSION);

        set(this);

        this.profiles = new HashMap<String, Profile>();
    }

    public interface ProfileCallback
    {
        void complete(Profile profile, Status status);
    }

    public void getMyProfile(AccessToken accessToken, final ProfileCallback profileCallback)
    {
        getAccountProfile("me", accessToken, profileCallback);
    }

    public void getAccountProfile(final String account, AccessToken accessToken, final ProfileCallback profileCallback)
    {
        JsonRequest jsonRequest = new JsonRequest(getOnlineLib(), getLocation() + "/profile/" + account,
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JsonRequest asJson = ((JsonRequest) request);

                    Profile profile = new Profile(asJson.getObject());

                    ProfileService.this.registerProfile(account, profile);

                    profileCallback.complete(profile, Status.success);
                } else
                {
                    profileCallback.complete(null, status);
                }
            }
        });

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.get();
    }

    public void createAccountProfile(String account, JSONObject ext, AccessToken accessToken,
                              final ProfileCallback profileCallback)
    {
        Map<String, String> queryArguments = new HashMap<String, String>();
        queryArguments.put("access_token", accessToken.getToken());

        StringRequest jsonRequest = new StringRequest(getOnlineLib(),
            getLocation() + "/profile/" + account,
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                profileCallback.complete(null, status);
            }
        });

        Map<String, Object> options = new HashMap<String, Object>();

        options.put("data", ext.toString());

        jsonRequest.setQueryArguments(queryArguments);
        jsonRequest.post(options);
    }

    public void createMyProfile(JSONObject ext, AccessToken accessToken,
          final ProfileCallback profileCallback)
    {
        createAccountProfile("me", ext, accessToken, profileCallback);
    }

    public void updateAccountProfile(String account, JSONObject ext, String path, boolean merge,
                              AccessToken accessToken,
                              final ProfileCallback profileCallback)
    {
        Map<String, String> queryArguments = new HashMap<String, String>();
        queryArguments.put("access_token", accessToken.getToken());

        StringRequest jsonRequest = new StringRequest(getOnlineLib(),
            getLocation() + "/profile/" + account + (path != null ? "/" + path : ""),
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                profileCallback.complete(null, status);
            }
        });

        Map<String, Object> options = new HashMap<String, Object>();

        options.put("data", ext.toString());
        options.put("merge", merge ? "true" : "false");

        jsonRequest.setQueryArguments(queryArguments);
        jsonRequest.post(options);
    }

    public void updateMyProfile(JSONObject ext, String path, boolean merge,
                              AccessToken accessToken,
                              final ProfileCallback profileCallback)
    {
        updateAccountProfile("me", ext, path, merge, accessToken, profileCallback);
    }

    private void registerProfile(String account, Profile profile)
    {
        profiles.put(account, profile);
    }

    public Map<String, Profile> getProfiles()
    {
        return profiles;
    }
}
