package org.anthillplatform.runtime.services;

import org.anthillplatform.runtime.requests.JsonRequest;
import org.anthillplatform.runtime.requests.Request;
import org.anthillplatform.runtime.requests.StringRequest;
import org.anthillplatform.runtime.AnthillRuntime;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * User ranking service for Anthill Platform
 *
 * See https://github.com/anthill-platform/anthill-leaderboard
 */
public class LeaderboardService extends Service
{
    public static final String ID = "leaderboard";
    public static final String API_VERSION = "0.2";

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

    /**
     * Please note that you should not create an instance of the service yourself,
     * and use AnthillRuntime.Get(LeaderboardService.ID, LeaderboardService.class) to get existing one instead
     */
    public LeaderboardService(AnthillRuntime runtime, String location)
    {
        super(runtime, location, ID, API_VERSION);
    }

    public static LeaderboardService Get()
    {
        return AnthillRuntime.Get(ID, LeaderboardService.class);
    }

    public interface PostLeaderboardCallback
    {
        void complete(LeaderboardService service, Request request, Request.Result result);
    }

    public interface GetLeaderboardCallback
    {
        void complete(LeaderboardService service, Request request, Request.Result result, LeaderboardResult data);
    }

    public void getLeaderboard(
        LoginService.AccessToken accessToken,
        String name,
        String order,
        final GetLeaderboardCallback profileCallback)
    {
        getLeaderboard(accessToken, name, order, 100, 0, profileCallback);
    }

    public void getLeaderboard(
        LoginService.AccessToken accessToken,
        String name,
        String order,
        int limit,
        int offset,
        final GetLeaderboardCallback profileCallback)
    {
        getLeaderboard(accessToken, name, order, limit, offset, null, profileCallback);
    }

    public void getLeaderboard(
        LoginService.AccessToken accessToken,
        String name,
        String order,
        int limit,
        int offset,
        String arbitraryAccount,
        final GetLeaderboardCallback profileCallback)
    {
        JsonRequest jsonRequest = new JsonRequest(getLocation() + "/leaderboard/" + order + "/" + name,
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                if (result == Request.Result.success)
                {
                    JsonRequest asJson = ((JsonRequest) request);

                    LeaderboardResult data = new LeaderboardResult();

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

                        data.getEntries().add(entry);
                    }
                    profileCallback.complete(LeaderboardService.this, request, result, data);
                }
                else
                {
                    profileCallback.complete(LeaderboardService.this, request, result, null);
                }
            }
        });

        Request.Fields args = new Request.Fields();

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

    public void postLeaderboard(
        LoginService.AccessToken accessToken,
        String name,
        String order,
        float score,
        String display_name,
        int expire_in,
        final PostLeaderboardCallback callback)
    {
        postLeaderboard(accessToken, name, order, score, display_name, expire_in, null, null, callback);
    }

    public void postLeaderboard(
        LoginService.AccessToken accessToken,
        String name,
        String order,
        float score,
        String display_name,
        int expire_in,
        JSONObject profile,
        String arbitraryAccount,
        final PostLeaderboardCallback callback)
    {
        StringRequest jsonRequest = new StringRequest(getRuntime(),
            getLocation() + "/leaderboard/" + order + "/" + name,
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                callback.complete(LeaderboardService.this, request, result);
            }
        });

        Request.Fields options = new Request.Fields();

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

        jsonRequest.setToken(accessToken);
        jsonRequest.post(options);
    }
}
