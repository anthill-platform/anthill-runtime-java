package org.anthillplatform.onlinelib.services;

import org.anthillplatform.onlinelib.Status;
import org.anthillplatform.onlinelib.request.JsonRequest;
import org.anthillplatform.onlinelib.request.Request;
import org.anthillplatform.onlinelib.request.StringRequest;
import org.anthillplatform.onlinelib.OnlineLib;
import org.anthillplatform.onlinelib.entity.AccessToken;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LeaderboardService extends Service
{
    public static final String ID = "leaderboard";
    public static final String API_VERSION = "0.2";

    private static LeaderboardService instance;
    public static LeaderboardService get() { return instance; }
    private static void set(LeaderboardService service) { instance = service; }

    public static class LeaderboardResult
    {
        public static class Entry
        {
            public int rank;
            public float score;
            public String display_name;
            public String account;
            public JSONObject profile;
        }

        private ArrayList<Entry> entries = new ArrayList<Entry>();

        public ArrayList<Entry> getEntries()
        {
            return entries;
        }
    }

    public LeaderboardService(OnlineLib onlineLib, String location)
    {
        super(onlineLib, location, ID, API_VERSION);

        set(this);
    }

    public interface LeaderboardCallback
    {
        void complete(LeaderboardService service, LeaderboardResult result, Status status);
    }

    public void getLeaderboard(String name, String order, AccessToken accessToken,
              final LeaderboardCallback profileCallback)
    {
        getLeaderboard(name, order, 100, 0, accessToken, profileCallback);
    }

    public void getLeaderboard(String name, String order, int limit, int offset,
                               AccessToken accessToken, final LeaderboardCallback profileCallback)
    {
        getLeaderboard(name, order, limit, offset, null, accessToken, profileCallback);
    }

    public void getLeaderboard(String name, String order, int limit, int offset, String arbitraryAccount,
                               AccessToken accessToken, final LeaderboardCallback profileCallback)
    {
        JsonRequest jsonRequest = new JsonRequest(getOnlineLib(), getLocation() + "/leaderboard/" +
                order + "/" + name,

            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JsonRequest asJson = ((JsonRequest) request);

                    LeaderboardResult result = new LeaderboardResult();

                    JSONArray entries = asJson.getObject().getJSONArray("data");
                    for (int i = 0, t = entries.length(); i < t; i++)
                    {
                        JSONObject entryValue = entries.getJSONObject(i);
                        LeaderboardResult.Entry entry = new LeaderboardResult.Entry();

                        entry.display_name = entryValue.optString("display_name", "??");
                        entry.score = (float) entryValue.optDouble("score", 0);
                        entry.account = entryValue.optString("account",
                            String.valueOf(entryValue.optInt("account", 0)));
                        entry.rank = entryValue.optInt("rank", 1);
                        entry.profile = entryValue.optJSONObject("profile");

                        result.getEntries().add(entry);
                    }
                    profileCallback.complete(LeaderboardService.this, result, Status.success);
                }
                else
                {
                    profileCallback.complete(LeaderboardService.this, null, status);
                }
            }
        });

        Map<String, String> args = new HashMap<String, String >();

        args.put("limit", String.valueOf(limit));
        args.put("offset", String.valueOf(offset));

        if (arbitraryAccount != null)
        {
            args.put("arbitrary_account", arbitraryAccount);
        }

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setQueryArguments(args);
        jsonRequest.setToken(accessToken);
        jsonRequest.get();
    }

    public void postLeaderboard(String name, String order, float score,
                                String display_name, int expire_in,
                                AccessToken accessToken,
                                final OnlineLib.Callback callback)
    {
        postLeaderboard(name, order, score, display_name, expire_in, null, null, accessToken, callback);
    }

    public void postLeaderboard(String name, String order, float score,
                                String display_name, int expire_in,
                                JSONObject profile, String arbitraryAccount,
                                AccessToken accessToken,
                                final OnlineLib.Callback callback)
    {
        Map<String, String> queryArguments = new HashMap<String, String>();

        queryArguments.put("access_token", accessToken.getToken());

        StringRequest jsonRequest = new StringRequest(getOnlineLib(),
            getLocation() + "/leaderboard/" + order + "/" + name,
                new Request.RequestResult()
                {
                    @Override
                    public void complete(Request request, Status status)
                    {
                        callback.complete(LeaderboardService.this.getOnlineLib(), status);
                    }
                });

        Map<String, Object> options = new HashMap<String, Object>();

        options.put("score", String.valueOf(score));
        options.put("display_name", display_name);
        options.put("expire_in", String.valueOf(expire_in));

        // warning, specifying this would require 'arbitrary_account'
        if (arbitraryAccount != null)
        {
            options.put("arbitrary_account", arbitraryAccount);
        }

        if (profile != null)
        {
            options.put("profile", profile.toString(0));
        }

        jsonRequest.setQueryArguments(queryArguments);
        jsonRequest.post(options);
    }
}
