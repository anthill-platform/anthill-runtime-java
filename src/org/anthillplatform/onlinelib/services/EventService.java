package org.anthillplatform.onlinelib.services;

import org.anthillplatform.onlinelib.OnlineLib;
import org.anthillplatform.onlinelib.Status;
import org.anthillplatform.onlinelib.entity.AccessToken;
import org.anthillplatform.onlinelib.request.JsonRequest;
import org.anthillplatform.onlinelib.request.Request;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class EventService extends Service
{
    private JsonRequest currentRequest;

    public static final String ID = "event";

    private static EventService instance;
    public static EventService get() { return instance; }
    private static void set(EventService service) { instance = service; }

    public EventService(OnlineLib onlineLib, String location)
    {
        super(onlineLib, location, ID);

        set(this);
    }

    public interface PostEventScoreCallback
    {
        void complete(float newScore, Status status);
    }

    public interface PostEventProfileCallback
    {
        void complete(JSONObject newData, Status status);
    }

    public interface EventListCallback
    {
        void complete(EventList events, Status status);
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
        return new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    }

    public static class Event
    {
        public HashMap<String, String> title;
        public HashMap<String, String> description;
        public boolean enabled;
        public boolean joined;
        public int id;

        public Date timeStart;
        public Date timeEnd;
        public int timeLeft;
        public Date timeLeftNow;

        public String category;
        public float score;
        public JSONObject profile;

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

            category = data.optString("category", "");
            enabled = data.optBoolean("enabled", false);
            joined = data.optBoolean("joined", false);
            id = data.getInt("id");
            score = (float)data.optDouble("score", 0);
            profile = data.optJSONObject("profile");

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

    public void postEventProfile(String eventId, JSONObject data,
            final PostEventProfileCallback callback, AccessToken accessToken)
    {
        JsonRequest scorePost = new JsonRequest(getOnlineLib(),
                getLocation() + "/event/" + eventId + "/profile", new Request.RequestResult()
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
        scorePost.setToken(accessToken);

        Map<String, Object> fields = new HashMap<String, Object>();

        fields.put("data", data.toString());

        scorePost.post(fields);
    }

    public void addEventScore(String eventId, float score, Map<String, String> additional,
                              final PostEventScoreCallback callback, AccessToken accessToken)
    {
        JsonRequest scorePost = new JsonRequest(getOnlineLib(),
            getLocation() + "/event/" + eventId + "/score/add", new Request.RequestResult()
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

        scorePost.setToken(accessToken);

        Map<String, Object> fields = new HashMap<String, Object>();

        fields.put("score", String.valueOf(score));

        if (additional != null)
        {
            fields.putAll(additional);
        }

        scorePost.post(fields);
    }

    public void getEvents(final EventListCallback callback, AccessToken accessToken)
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

        currentRequest = new JsonRequest(getOnlineLib(), getLocation() + "/events", new Request.RequestResult()
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

        currentRequest.setToken(accessToken);
        currentRequest.get();
    }
}
