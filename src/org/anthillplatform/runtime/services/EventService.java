package org.anthillplatform.runtime.services;

import org.anthillplatform.runtime.AnthillRuntime;
import org.anthillplatform.runtime.requests.JsonRequest;
import org.anthillplatform.runtime.requests.Request;
import org.anthillplatform.runtime.util.Utils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Time-Limited events service for Anthill platform
 *
 * See https://github.com/anthill-platform/anthill-event
 */
public class EventService extends Service
{
    private JsonRequest currentRequest;

    public static final String ID = "event";
    public static final String API_VERSION = "0.2";

    /**
     * Please note that you should not create an instance of the service yourself,
     * and use AnthillRuntime.Get(EventService.ID, EventService.class)  to get existing one instead
     */
    public EventService(AnthillRuntime runtime, String location)
    {
        super(runtime, location, ID, API_VERSION);
    }

    public static EventService Get()
    {
        return AnthillRuntime.Get(ID, EventService.class);
    }

    public interface PostEventScoreCallback
    {
        void complete(EventService service, Request request, Request.Result result, float newScore);
    }

    public interface JoinEventCallback
    {
        void complete(EventService service, Request request, Request.Result result);
    }

    public interface LeaveEventCallback
    {
        void complete(EventService service, Request request, Request.Result result);
    }

    public interface PostEventProfileCallback
    {
        void complete(EventService service, Request request, Request.Result result, JSONObject newData);
    }

    public interface EventListCallback
    {
        void complete(EventService service, Request request, Request.Result result, EventList events);
    }

    public interface GroupProfileParticipantsCallback
    {
        void complete(EventService service, Request request, Request.Result result,
                      Map<String, GroupEventParticipant> participants);
    }

    public static class GroupEventParticipant
    {
        public JSONObject profile;
        public float score;
    }

    public static class EventList extends ArrayList<Event>
    {
        private JSONObject source;

        public JSONObject write()
        {
            return source;
        }

        public void read(JSONObject object)
        {
            this.source = object;

            clear();

            JSONArray eventsValue = object.getJSONArray("events");

            for (int i = 0, t = eventsValue.length(); i < t; i++)
            {
                Event event = new Event();
                JSONObject data = eventsValue.getJSONObject(i);

                if (data != null)
                {
                    event.read(data);
                }

                this.add(event);
            }
        }
    }

    private static SimpleDateFormat getTimeFormat()
    {
        return Utils.DATE_FORMAT;
    }

    public enum EventKind
    {
        account,
        group
    }

    public static class Event
    {
        public HashMap<String, String> title;
        public HashMap<String, String> description;
        public boolean enabled;
        public boolean joined;
        public int id;
        public EventKind kind;

        public Date timeStart;
        public Date timeEnd;
        public int timeLeft;
        public Date timeLeftNow;

        public String category;
        public float score;
        public JSONObject profile;
        public JSONObject groupProfile;

        public boolean tournament;
        public String leaderboardName;
        public String leaderboardOrder;

        public JSONObject data;

        public boolean isActive()
        {
            return enabled && getSecondsLeft() > 0;
        }

        public int getSecondsLeft()
        {
            long passed = (System.currentTimeMillis() - timeLeftNow.getTime()) / 1000;

            return Math.max((int)(timeLeft - passed), 0);
        }

        public Event()
        {
            this.title = new HashMap<String, String>();
            this.description = new HashMap<String, String>();

            this.timeLeftNow = new Date();
        }

        public void read(JSONObject data)
        {
            if (data.has("title"))
            {
                JSONObject title = data.getJSONObject("title");

                for (Object key : title.keySet())
                {
                    this.title.put(key.toString(), title.getString(key.toString()));
                }
            }

            if (data.has("description"))
            {
                JSONObject description = data.getJSONObject("description");

                for (Object key : description.keySet())
                {
                    this.description.put(key.toString(), description.getString(key.toString()));
                }
            }

            try
            {
                kind = EventKind.valueOf(data.optString("kind", EventKind.account.toString()));
            }
            catch (IllegalArgumentException e)
            {
                kind = EventKind.account;
            }

            category = data.optString("category", "");
            enabled = data.optBoolean("enabled", false);
            joined = data.optBoolean("joined", false);
            id = data.getInt("id");
            score = (float)data.optDouble("score", 0);

            profile = data.optJSONObject("profile");
            groupProfile = data.optJSONObject("group_profile");

            JSONObject time = data.getJSONObject("time");

            try
            {
                SimpleDateFormat format = getTimeFormat();

                timeStart = format.parse(time.getString("start"));
                timeEnd = format.parse(time.getString("end"));
                timeLeft = time.getInt("left");
            }
            catch (Exception ignored)
            {
                enabled = false;
            }

            if (data.has("tournament"))
            {
                tournament = true;
                JSONObject tournament = data.getJSONObject("tournament");

                leaderboardName = tournament.getString("leaderboard_name");
                leaderboardOrder = tournament.getString("leaderboard_order");
            }

            this.data = data;
        }
    }

    public void updateEventProfile(
        LoginService.AccessToken accessToken,
        String eventId,
        JSONObject profile,
        final PostEventProfileCallback callback)
    {
        updateEventProfile(accessToken, eventId, profile, null, true, callback);
    }

    public void updateEventProfile(
        LoginService.AccessToken accessToken,
        String eventId,
        JSONObject profile,
        String path,
        boolean merge,
        final PostEventProfileCallback callback)
    {
        JsonRequest scorePost = new JsonRequest(
                getLocation() + "/event/" + eventId + "/profile",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result status)
            {
                if (status == Request.Result.success)
                {
                    JSONObject result = ((JsonRequest) request).getObject();
                    callback.complete(EventService.this, request, status, result);
                }
                else
                {
                    callback.complete(EventService.this, request, status, null);
                }
            }
        });

        scorePost.setAPIVersion(getAPIVersion());
        scorePost.setToken(accessToken);

        Request.Fields fields = new Request.Fields();

        fields.put("profile", profile.toString());
        fields.put("merge", merge ? "true" : "false");

        if (path != null)
        {
            fields.put("path", path);
        }

        scorePost.post(fields);
    }

    public void updateGroupEventProfile(
        LoginService.AccessToken accessToken,
        String eventId,
        String groupId,
        JSONObject profile,
        final PostEventProfileCallback callback)
    {
        updateGroupEventProfile(accessToken, eventId, groupId, profile, null, true, callback);
    }

    public void updateGroupEventProfile(
        LoginService.AccessToken accessToken,
        String eventId,
        String groupId,
        JSONObject profile,
        String path,
        boolean merge,
        final PostEventProfileCallback callback)
    {
        JsonRequest scorePost = new JsonRequest(
                getLocation() + "/event/" + eventId + "/group/profile",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result status)
            {
                if (status == Request.Result.success)
                {
                    JSONObject result = ((JsonRequest) request).getObject();
                    callback.complete(EventService.this, request, status, result);
                }
                else
                {
                    callback.complete(EventService.this, request, status, null);
                }
            }
        });

        scorePost.setAPIVersion(getAPIVersion());
        scorePost.setToken(accessToken);

        Request.Fields fields = new Request.Fields();

        fields.put("profile", profile.toString());
        fields.put("group_id", groupId);
        fields.put("merge", merge ? "true": "false");

        if (path != null)
        {
            fields.put("path", path);
        }

        scorePost.post(fields);
    }

    public void addEventScore(
        LoginService.AccessToken accessToken,
        String eventId,
        float score,
        final PostEventScoreCallback callback)
    {
        addEventScore(accessToken, eventId, score, false, null, callback);
    }

    public void addEventScore(
        LoginService.AccessToken accessToken,
        String eventId,
        float score,
        boolean autoJoin,
        JSONObject leaderboardInfo,
        final PostEventScoreCallback callback)
    {
        JsonRequest scorePost = new JsonRequest(getLocation() + "/event/" + eventId + "/score/add",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result status)
            {
                if (status == Request.Result.success)
                {
                    JSONObject result = ((JsonRequest) request).getObject();

                    float newScore = 0;

                    if (result != null)
                    {
                        newScore = ((float) result.optDouble("score"));
                    }

                    callback.complete(EventService.this, request, status, newScore);
                }
                else
                {
                    callback.complete(EventService.this, request, status, 0);
                }
            }
        });

        scorePost.setAPIVersion(getAPIVersion());
        scorePost.setToken(accessToken);

        Request.Fields fields = new Request.Fields();

        fields.put("score", String.valueOf(score));
        fields.put("auto_join", autoJoin ? "true" : "false");

        if (leaderboardInfo != null)
        {
            fields.put("leaderboard_info", leaderboardInfo.toString());
        }

        scorePost.post(fields);
    }

    public void leaveEvent(
        LoginService.AccessToken accessToken,
        String eventId,
        final LeaveEventCallback callback)
    {
        JsonRequest scorePost = new JsonRequest(
                getLocation() + "/event/" + eventId + "/leave",
        new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result status)
            {
                callback.complete(EventService.this, request, status);
            }
        });

        scorePost.setAPIVersion(getAPIVersion());
        scorePost.setToken(accessToken);
        scorePost.post();
    }

    public void leaveGroupEvent(
        LoginService.AccessToken accessToken,
        String eventId,
        String groupId,
        final LeaveEventCallback callback)
    {
        JsonRequest scorePost = new JsonRequest(
                getLocation() + "/event/" + eventId + "/group/leave",
        new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result status)
            {
                callback.complete(EventService.this, request, status);
            }
        });

        scorePost.setAPIVersion(getAPIVersion());
        scorePost.setToken(accessToken);

        Request.Fields fields = new Request.Fields();
        fields.put("group_id", groupId);
        scorePost.post(fields);
    }

    public void joinEvent(
        LoginService.AccessToken accessToken,
        String eventId,
        final JoinEventCallback callback)
    {
        joinEvent(accessToken, eventId, 0, null, callback);
    }

    public void joinEvent(
        LoginService.AccessToken accessToken,
        String eventId,
        float score,
        JSONObject leaderboardInfo,
        final JoinEventCallback callback)
    {
        JsonRequest scorePost = new JsonRequest(getLocation() + "/event/" + eventId + "/join",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result status)
            {
                callback.complete(EventService.this, request, status);
            }
        });

        scorePost.setAPIVersion(getAPIVersion());
        scorePost.setToken(accessToken);

        Request.Fields fields = new Request.Fields();

        fields.put("score", String.valueOf(score));

        if (leaderboardInfo != null)
        {
            fields.put("leaderboard_info", leaderboardInfo.toString());
        }

        scorePost.post(fields);
    }

    public void joinGroupEvent(
        LoginService.AccessToken accessToken,
        String eventId,
        String groupId,
        final JoinEventCallback callback)
    {
        joinGroupEvent(accessToken, eventId, groupId, 0, null, callback);
    }

    public void joinGroupEvent(
        LoginService.AccessToken accessToken,
        String eventId,
        String groupId,
        float score,
        JSONObject leaderboardInfo,
        final JoinEventCallback callback)
    {
        JsonRequest scorePost = new JsonRequest(getLocation() + "/event/" + eventId + "/group/join",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result status)
            {
                callback.complete(EventService.this, request, status);
            }
        });

        scorePost.setAPIVersion(getAPIVersion());
        scorePost.setToken(accessToken);

        Request.Fields fields = new Request.Fields();

        fields.put("score", String.valueOf(score));
        fields.put("group_id", groupId);

        if (leaderboardInfo != null)
        {
            fields.put("leaderboard_info", leaderboardInfo.toString());
        }

        scorePost.post(fields);
    }

    public void addGroupEventScore(
        LoginService.AccessToken accessToken,
        String eventId,
        String groupId,
        float score,
        final PostEventScoreCallback callback)
    {
        addGroupEventScore(accessToken, eventId, groupId, score, false, null, callback);
    }

    public void addGroupEventScore(
        LoginService.AccessToken accessToken,
        String eventId,
        String groupId,
        float score,
        boolean autoJoin,
        JSONObject leaderboardInfo,
        final PostEventScoreCallback callback)
    {
        JsonRequest scorePost = new JsonRequest(getLocation() + "/event/" + eventId + "/group/score/add",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result status)
            {
                if (status == Request.Result.success)
                {
                    JSONObject result = ((JsonRequest) request).getObject();

                    float newScore = 0;

                    if (result != null)
                    {
                        newScore = ((float) result.optDouble("score"));
                    }

                    callback.complete(EventService.this, request, status, newScore);
                } else
                {
                    callback.complete(EventService.this, request, status, 0);
                }
            }
        });

        scorePost.setAPIVersion(getAPIVersion());
        scorePost.setToken(accessToken);

        Request.Fields fields = new Request.Fields();

        fields.put("score", String.valueOf(score));
        fields.put("group_id", groupId);
        fields.put("auto_join", autoJoin ? "true" : "false");

        if (leaderboardInfo != null)
        {
            fields.put("leaderboard_info", leaderboardInfo.toString());
        }

        scorePost.post(fields);
    }

    public void getGroupEventParticipants(
        LoginService.AccessToken accessToken,
        String eventId,
        String groupId,
        final GroupProfileParticipantsCallback callback)
    {
        currentRequest = new JsonRequest(getLocation() + "/event/" + eventId + "/group/participants",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result status)
            {
                if (status == Request.Result.success)
                {
                    JSONObject result = ((JsonRequest) request).getObject();

                    Map<String, GroupEventParticipant> participants = new HashMap<String, GroupEventParticipant>();

                    JSONObject participants_ = result.optJSONObject("participants");

                    if (participants_ != null)
                    {
                        for (Object key : participants_.keySet())
                        {
                            String accountId = key.toString();
                            JSONObject participant_ = participants_.optJSONObject(accountId);

                            if (participant_ != null)
                            {
                                GroupEventParticipant participant = new GroupEventParticipant();

                                participant.profile = participant_.optJSONObject("profile");
                                participant.score = (float)participant_.optDouble("score", 0.0d);

                                participants.put(accountId, participant);
                            }
                        }
                    }

                    callback.complete(EventService.this, request, status, participants);
                }
                else
                {
                    callback.complete(EventService.this, request, status, null);
                }

                currentRequest = null;
            }
        });

        currentRequest.setAPIVersion(getAPIVersion());
        Request.Fields queryArguments = new Request.Fields();
        queryArguments.put("group_id", groupId);
        currentRequest.setQueryArguments(queryArguments);

        currentRequest.setToken(accessToken);
        currentRequest.get();
    }

    public void getEvents(
        LoginService.AccessToken accessToken,
        final EventListCallback callback)
    {
        getEvents(accessToken, null, 0, callback);
    }


    public void getEvents(
        LoginService.AccessToken accessToken,
        int extraTime,
        final EventListCallback callback)
    {
        getEvents(accessToken, null, extraTime, callback);
    }

    public void getEvents(
        LoginService.AccessToken accessToken,
        String groupContext,
        int extraTime,
        final EventListCallback callback)
    {
        if (accessToken == null)
        {
            callback.complete(EventService.this, null, Request.Result.forbidden, null);
            return;
        }

        if (currentRequest != null)
        {
            callback.complete(EventService.this, null, Request.Result.pending, null);
            return;
        }

        currentRequest = new JsonRequest(getLocation() + "/events", new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                if (result == Request.Result.success)
                {
                    EventList events = new EventList();
                    events.read(((JsonRequest) request).getObject());
                    callback.complete(EventService.this, request, result, events);
                }
                else
                {
                    callback.complete(EventService.this, request, result, null);
                }

                currentRequest = null;
            }
        });

        currentRequest.setAPIVersion(getAPIVersion());
        Request.Fields queryArguments = new Request.Fields();

        if (groupContext != null)
        {
            queryArguments.put("group_id", groupContext);
        }

        if (extraTime > 0)
        {
            queryArguments.put("extra_time", String.valueOf(extraTime));
        }

        currentRequest.setQueryArguments(queryArguments);

        currentRequest.setToken(accessToken);
        currentRequest.get();
    }
}
