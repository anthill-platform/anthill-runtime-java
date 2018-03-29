package org.anthillplatform.runtime.services;

import org.anthillplatform.runtime.AnthillRuntime;
import org.anthillplatform.runtime.Status;
import org.anthillplatform.runtime.entity.AccessToken;
import org.anthillplatform.runtime.request.JsonRequest;
import org.anthillplatform.runtime.request.Request;
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

    private static EventService instance;
    public static EventService get() { return instance; }
    private static void set(EventService service) { instance = service; }

    /**
     * Please note that you should not create an instance of the service yourself,
     * and use EventService.get() to get existing one instead
     */
    public EventService(AnthillRuntime runtime, String location)
    {
        super(runtime, location, ID, API_VERSION);

        set(this);
    }

    public interface PostEventScoreCallback
    {
        void complete(float newScore, Status status);
    }

    public interface JoinEventCallback
    {
        void complete(Status status);
    }

    public interface LeaveEventCallback
    {
        void complete(Status status);
    }

    public interface PostEventProfileCallback
    {
        void complete(JSONObject newData, Status status);
    }

    public interface EventListCallback
    {
        void complete(EventList events, Status status);
    }


    public interface GroupProfileParticipantsCallback
    {
        void complete(Map<String, GroupEventParticipant> participants, Status status);
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

    public void updateEventProfile(String eventId, JSONObject profile,
                                   final PostEventProfileCallback callback, AccessToken accessToken)
    {
        updateEventProfile(eventId, profile, null, true, callback, accessToken);
    }

    public void updateEventProfile(String eventId, JSONObject profile, String path, boolean merge,
                                   final PostEventProfileCallback callback, AccessToken accessToken)
    {
        JsonRequest scorePost = new JsonRequest(getRuntime(),
            getLocation() + "/event/" + eventId + "/profile",
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject result = ((JsonRequest) request).getObject();
                    callback.complete(result, Status.success);
                } else
                {
                    callback.complete(null, status);
                }
            }
        });

        scorePost.setAPIVersion(getAPIVersion());
        scorePost.setToken(accessToken);

        Map<String, Object> fields = new HashMap<String, Object>();

        fields.put("profile", profile.toString());
        fields.put("merge", merge ? "true" : "false");

        if (path != null)
        {
            fields.put("path", path);
        }

        scorePost.post(fields);
    }

    public void updateGroupEventProfile(String eventId, String groupId, JSONObject profile,
                                        final PostEventProfileCallback callback, AccessToken accessToken)
    {
        updateGroupEventProfile(eventId, groupId, profile, null, true, callback, accessToken);
    }

    public void updateGroupEventProfile(String eventId, String groupId, JSONObject profile, String path, boolean merge,
                                   final PostEventProfileCallback callback, AccessToken accessToken)
    {
        JsonRequest scorePost = new JsonRequest(getRuntime(),
            getLocation() + "/event/" + eventId + "/group/profile",
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject result = ((JsonRequest) request).getObject();
                    callback.complete(result, Status.success);
                } else
                {
                    callback.complete(null, status);
                }
            }
        });

        scorePost.setAPIVersion(getAPIVersion());
        scorePost.setToken(accessToken);

        Map<String, Object> fields = new HashMap<String, Object>();

        fields.put("profile", profile.toString());
        fields.put("group_id", groupId);
        fields.put("merge", merge ? "true": "false");

        if (path != null)
        {
            fields.put("path", path);
        }

        scorePost.post(fields);
    }

    public void addEventScore(String eventId, float score,
                              final PostEventScoreCallback callback, AccessToken accessToken)
    {
        addEventScore(eventId, score, false, null, callback, accessToken);
    }

    public void addEventScore(String eventId, float score, boolean autoJoin, JSONObject leaderboardInfo,
                              final PostEventScoreCallback callback, AccessToken accessToken)
    {
        JsonRequest scorePost = new JsonRequest(getRuntime(),
            getLocation() + "/event/" + eventId + "/score/add",
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject result = ((JsonRequest) request).getObject();

                    float newScore = 0;

                    if (result != null)
                    {
                        newScore = ((float) result.optDouble("score"));
                    }

                    callback.complete(newScore, Status.success);
                } else
                {
                    callback.complete(0, status);
                }
            }
        });

        scorePost.setAPIVersion(getAPIVersion());
        scorePost.setToken(accessToken);

        Map<String, Object> fields = new HashMap<String, Object>();

        fields.put("score", String.valueOf(score));
        fields.put("auto_join", autoJoin ? "true" : "false");

        if (leaderboardInfo != null)
        {
            fields.put("leaderboard_info", leaderboardInfo.toString());
        }

        scorePost.post(fields);
    }

    public void leaveEvent(String eventId, final LeaveEventCallback callback, AccessToken accessToken)
    {
        JsonRequest scorePost = new JsonRequest(getRuntime(),
            getLocation() + "/event/" + eventId + "/leave",
        new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                callback.complete(status);
            }
        });

        scorePost.setAPIVersion(getAPIVersion());
        scorePost.setToken(accessToken);
        scorePost.post();
    }

    public void leaveGroupEvent(String eventId, String groupId,
                                final LeaveEventCallback callback, AccessToken accessToken)
    {
        JsonRequest scorePost = new JsonRequest(getRuntime(),
            getLocation() + "/event/" + eventId + "/group/leave",
        new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                callback.complete(status);
            }
        });

        scorePost.setAPIVersion(getAPIVersion());
        scorePost.setToken(accessToken);

        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("group_id", groupId);
        scorePost.post(fields);
    }

    public void joinEvent(String eventId,
                          final JoinEventCallback callback, AccessToken accessToken)
    {
        joinEvent(eventId, 0, null, callback, accessToken);
    }

    public void joinEvent(String eventId, float score, JSONObject leaderboardInfo,
                          final JoinEventCallback callback, AccessToken accessToken)
    {
        JsonRequest scorePost = new JsonRequest(getRuntime(),
            getLocation() + "/event/" + eventId + "/join",
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                callback.complete(status);
            }
        });

        scorePost.setAPIVersion(getAPIVersion());
        scorePost.setToken(accessToken);

        Map<String, Object> fields = new HashMap<String, Object>();

        fields.put("score", String.valueOf(score));

        if (leaderboardInfo != null)
        {
            fields.put("leaderboard_info", leaderboardInfo.toString());
        }

        scorePost.post(fields);
    }

    public void joinGroupEvent(String eventId, String groupId,
                               final JoinEventCallback callback, AccessToken accessToken)
    {
        joinGroupEvent(eventId, groupId, 0, null, callback, accessToken);
    }

    public void joinGroupEvent(String eventId, String groupId, float score, JSONObject leaderboardInfo,
                          final JoinEventCallback callback, AccessToken accessToken)
    {
        JsonRequest scorePost = new JsonRequest(getRuntime(),
                getLocation() + "/event/" + eventId + "/group/join",
                new Request.RequestResult()
                {
                    @Override
                    public void complete(Request request, Status status)
                    {
                        callback.complete(status);
                    }
                });

        scorePost.setAPIVersion(getAPIVersion());
        scorePost.setToken(accessToken);

        Map<String, Object> fields = new HashMap<String, Object>();

        fields.put("score", String.valueOf(score));
        fields.put("group_id", groupId);

        if (leaderboardInfo != null)
        {
            fields.put("leaderboard_info", leaderboardInfo.toString());
        }

        scorePost.post(fields);
    }

    public void addGroupEventScore(String eventId, String groupId, float score,
                              final PostEventScoreCallback callback, AccessToken accessToken)
    {
        addGroupEventScore(eventId, groupId, score, false, null, callback, accessToken);
    }

    public void addGroupEventScore(String eventId, String groupId,
                                   float score, boolean autoJoin, JSONObject leaderboardInfo,
                                   final PostEventScoreCallback callback, AccessToken accessToken)
    {
        JsonRequest scorePost = new JsonRequest(getRuntime(),
            getLocation() + "/event/" + eventId + "/group/score/add",
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject result = ((JsonRequest) request).getObject();

                    float newScore = 0;

                    if (result != null)
                    {
                        newScore = ((float) result.optDouble("score"));
                    }

                    callback.complete(newScore, Status.success);
                } else
                {
                    callback.complete(0, status);
                }
            }
        });

        scorePost.setAPIVersion(getAPIVersion());
        scorePost.setToken(accessToken);

        Map<String, Object> fields = new HashMap<String, Object>();

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
        String eventId, String groupId,
        final GroupProfileParticipantsCallback callback, AccessToken accessToken)
    {
        currentRequest = new JsonRequest(getRuntime(),
            getLocation() + "/event/" + eventId + "/group/participants",
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
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

                    callback.complete(participants, Status.success);
                }
                else
                {
                    callback.complete(null, status);
                }

                currentRequest = null;
            }
        });

        currentRequest.setAPIVersion(getAPIVersion());
        Map<String, String> queryArguments = new HashMap<String, String>();
        queryArguments.put("group_id", groupId);
        currentRequest.setQueryArguments(queryArguments);

        currentRequest.setToken(accessToken);
        currentRequest.get();
    }

    public void getEvents(final EventListCallback callback, AccessToken accessToken)
    {
        getEvents(null, 0, callback, accessToken);
    }


    public void getEvents(int extraTime, final EventListCallback callback, AccessToken accessToken)
    {
        getEvents(null, extraTime, callback, accessToken);
    }

    public void getEvents(String groupContext, int extraTime,
                          final EventListCallback callback, AccessToken accessToken)
    {
        if (accessToken == null)
        {
            callback.complete(null, Status.forbidden);
            return;
        }

        if (currentRequest != null)
        {
            callback.complete(null, Status.pending);
            return;
        }

        currentRequest = new JsonRequest(getRuntime(), getLocation() + "/events", new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    EventList events = new EventList();
                    events.read(((JsonRequest) request).getObject());
                    callback.complete(events, Status.success);
                } else
                {
                    callback.complete(null, status);
                }

                currentRequest = null;
            }
        });

        currentRequest.setAPIVersion(getAPIVersion());
        Map<String, String> queryArguments = new HashMap<String, String>();

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
