package org.anthillplatform.onlinelib.services;

import org.anthillplatform.onlinelib.OnlineLib;
import org.anthillplatform.onlinelib.Status;
import org.anthillplatform.onlinelib.entity.AccessToken;
import org.anthillplatform.onlinelib.entity.ApplicationInfo;
import org.anthillplatform.onlinelib.request.JsonRequest;
import org.anthillplatform.onlinelib.request.Request;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GameService extends Service
{
    public static final String ID = "game";

    private static GameService instance;
    public static GameService get() { return instance; }
    private static void set(GameService service) { instance = service; }

    public interface JoinGameCallback
    {
        void success(String roomId, String key, String host, int[] ports, JSONObject settings);
        void fail(Request request, Status status);
    }

    public static class JoinMultiSlot
    {
        public String slot;
        public String key;
    }

    public interface ListPlayerRecordsCallback
    {
        void result(Status status, ArrayList<PlayerRecord> records);
    }

    public interface ListMultiplePlayersRecordsCallback
    {
        void result(Status status, Map<String, ArrayList<PlayerRecord>> records);
    }

    public interface JoinGameMultiCallback
    {
        void success(String roomId,
            HashMap<String, JoinMultiSlot> slots, String host, int[] ports, JSONObject settings);
        void fail(Request request, Status status);
    }

    public static class JoinMultiWrapper
    {
        public AccessToken accessToken;
        public String ip;

        public JoinMultiWrapper(AccessToken accessToken, String ip)
        {
            this.accessToken = accessToken;
            this.ip = ip;
        }
    }

    public class PlayerRecord
    {
        private String roomId;
        private JSONObject roomSettings;
        private int players;
        private int playersMax;
        private String gameName;
        private String gameVersion;
        private String gameServer;

        public PlayerRecord(JSONObject data)
        {
            this.roomId = data.optString("id");
            this.roomSettings = data.optJSONObject("settings");
            this.players = data.optInt("players", 0);
            this.playersMax = data.optInt("max_players", 1);
            this.gameName = data.optString("game_name");
            this.gameVersion = data.optString("game_version");
            this.gameServer = data.optString("game_server");
        }

        public String getRoomId()
        {
            return roomId;
        }

        public JSONObject getRoomSettings()
        {
            return roomSettings;
        }

        public int getPlayers()
        {
            return players;
        }

        public int getPlayersMax()
        {
            return playersMax;
        }

        public String getGameName()
        {
            return gameName;
        }

        public String getGameVersion()
        {
            return gameVersion;
        }

        public String getGameServer()
        {
            return gameServer;
        }
    }

    public static class RoomSettings
    {
        private JSONObject settings;

        public RoomSettings()
        {
            this.settings = new JSONObject();
        }

        public <T> RoomSettings add(String key, T value)
        {
            settings.put(key, value);

            return this;
        }

        @Override
        public String toString()
        {
            return settings.toString();
        }

        public JSONObject getSettings()
        {
            return settings;
        }
    }

    public static class RoomsFilter
    {
        private JSONObject filters;

        public RoomsFilter()
        {
            this.filters = new JSONObject();
        }

        private <T> JSONObject condition(String func, T value)
        {
            JSONObject object = new JSONObject();

            object.put("@func", func);
            object.put("@value", value);

            return object;
        }

        public <T> RoomsFilter putEquals(String key, T value)
        {
            filters.put(key, value);
            return this;
        }

        public <T> RoomsFilter putGreater(String key, T value)
        {
            filters.put(key, condition(">", value));
            return this;
        }

        public <T> RoomsFilter putGreaterOrEqual(String key, T value)
        {
            filters.put(key, condition(">=", value));
            return this;
        }

        public <T> RoomsFilter putLess(String key, T value)
        {
            filters.put(key, condition("<", value));
            return this;
        }

        public <T> RoomsFilter putLessOrEqual(String key, T value)
        {
            filters.put(key, condition("<=", value));
            return this;
        }

        public <T> RoomsFilter putNotEqual(String key, T value)
        {
            filters.put(key, condition("!=", value));
            return this;
        }

        public <T> RoomsFilter putBetween(String key, T a, T b)
        {
            JSONObject cond = new JSONObject();

            cond.put("@func", "between");
            cond.put("@a", a);
            cond.put("@b", b);

            filters.put(key, cond);
            return this;
        }

        @Override
        public String toString()
        {
            return filters.toString();
        }
    }

    public class Room
    {
        public String id;
        public String host;
        public int[] ports;
        public int players;
        public int maxPlayers;
        public JSONObject settings;

        public boolean isFull()
        {
            return players >= maxPlayers;
        }

        public Room(JSONObject data)
        {
            id = data.getString("id");

            JSONObject location = data.getJSONObject("location");

            host = location.getString("host");
            JSONArray ports = location.getJSONArray("ports");

            this.ports = new int[ports.length()];

            for (int i = 0; i < ports.length(); i++)
            {
                this.ports[i] = ports.getInt(i);
            }

            players = data.getInt("players");
            maxPlayers = data.getInt("max_players");
            settings = data.getJSONObject("settings");
        }
    }

    public static class Region
    {
        public String name;
        public JSONObject settings;
        public float locationX;
        public float locationY;

        public Region(String name, JSONObject region)
        {
            this.name = name;

            if (region != null)
            {
                this.settings = region.optJSONObject("settings");

                JSONObject location = region.optJSONObject("location");

                if (location != null)
                {
                    locationX = ((float) location.optDouble("x"));
                    locationY = ((float) location.optDouble("y"));
                }
            }
        }

        @Override
        public String toString()
        {
            return name;
        }
    }

    public class GamesStatus
    {
        public int players;
    }

    public interface GamesStatusCallback
    {
        void result(boolean success, GamesStatus status);
    }

    public interface FindGamesCallback
    {
        void success(ArrayList<Room> rooms);
        void fail(Status status);
    }

    public interface GetRegionsCallback
    {
        void success(ArrayList<Region> regions, String myRegion);
        void fail(Status status);
    }

    public GameService(OnlineLib onlineLib, String location)
    {
        super(onlineLib, location, ID);

        set(this);
    }

    public void getStatus(final GamesStatusCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getOnlineLib(), getLocation() + "/status",
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    GamesStatus gamesStatus = new GamesStatus();

                    gamesStatus.players = response.optInt("players", 0);

                    callback.result(true, gamesStatus);
                } else
                {
                    callback.result(false, null);
                }
            }
        });

        jsonRequest.get();
    }

    public void getRegions(AccessToken accessToken, final GetRegionsCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getOnlineLib(), getLocation() + "/regions",
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    JSONObject regionsData = response.optJSONObject("regions");
                    String myRegion = response.optString("my_region");

                    if (regionsData != null && myRegion != null)
                    {
                        ArrayList<Region> regions = new ArrayList<Region>();

                        for (Object key : regionsData.keySet())
                        {
                            String name = key.toString();

                            JSONObject region = regionsData.getJSONObject(name);
                            regions.add(new Region(name, region));
                        }


                        callback.success(regions, myRegion);
                    } else
                    {
                        callback.fail(Status.badRequest);
                    }

                } else
                {
                    callback.fail(status);
                }
            }
        });

        jsonRequest.setToken(accessToken);

        jsonRequest.get();
    }

    public void createGame(String gameServerName, RoomSettings createSettings,
                           AccessToken accessToken, final JoinGameCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getOnlineLib(),
            getLocation() + "/create/" + getOnlineLib().getApplicationInfo().getGameId() + "/" + gameServerName + "/" +
                getOnlineLib().getApplicationInfo().getGameVersion(),
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    String roomId = response.getString("id");
                    String key = response.getString("key");
                    JSONObject location = response.getJSONObject("location");

                    String host = location.getString("host");
                    JSONArray ports = location.getJSONArray("ports");

                    int[] pts = new int[ports.length()];

                    for (int i = 0; i < ports.length(); i++)
                    {
                        pts[i] = ports.getInt(i);
                    }

                    callback.success(roomId, key, host, pts, location.getJSONObject("init"));
                } else
                {
                    callback.fail(request, status);
                }
            }
        });

        HashMap<String, Object> fields = new HashMap<String, Object>();

        fields.put("settings", createSettings.toString());

        jsonRequest.setToken(accessToken);
        jsonRequest.post(fields);

    }

    public void listGames(String gameServerName, RoomsFilter filter, AccessToken accessToken,
                          FindGamesCallback callback)
    {
        listGames(gameServerName, filter, accessToken, callback, false, true, null);
    }

    public void listGames(String gameServerName, RoomsFilter filter, AccessToken accessToken,
                          final FindGamesCallback callback, boolean myRegionOnly, boolean showFull, String region)
    {
        ApplicationInfo applicationInfo = getOnlineLib().getApplicationInfo();

        JsonRequest jsonRequest = new JsonRequest(getOnlineLib(),
            getLocation() + "/rooms/" + applicationInfo.getGameId() + "/" + gameServerName + "/" +
                applicationInfo.getGameVersion(),
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    JSONArray roomsData = response.getJSONArray("rooms");

                    ArrayList<Room> rooms = new ArrayList<Room>();

                    for (int i = 0; i < roomsData.length(); i++)
                    {
                        JSONObject roomData = roomsData.getJSONObject(i);
                        rooms.add(new Room(roomData));
                    }

                    callback.success(rooms);
                } else
                {
                    callback.fail(status);
                }
            }
        });

        HashMap<String, String> fields = new HashMap<String, String>();

        fields.put("settings", filter.toString());
        fields.put("show_full", showFull ? "true" : "false");
        fields.put("my_region_only", myRegionOnly ? "true" : "false");

        if (region != null)
        {
            fields.put("region", region);
        }

        jsonRequest.setQueryArguments(fields);

        jsonRequest.setToken(accessToken);
        jsonRequest.get();

    }

    public void joinGame(String roomId, AccessToken accessToken, final JoinGameCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getOnlineLib(),
            getLocation() + "/room/" + getOnlineLib().getApplicationInfo().getGameId() + "/" + roomId + "/join",
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    String _roomId = response.getString("id");
                    String key = response.getString("key");
                    JSONObject location = response.optJSONObject("location");

                    if (location == null)
                    {
                        callback.fail(request, Status.failed);
                        return;
                    }

                    String host = location.getString("host");
                    JSONArray ports = location.getJSONArray("ports");

                    int[] pts = new int[ports.length()];

                    for (int i = 0; i < ports.length(); i++)
                    {
                        pts[i] = ports.getInt(i);
                    }

                    callback.success(_roomId, key, host, pts, response.optJSONObject("settings"));
                } else
                {
                    callback.fail(request, status);
                }
            }
        });

        jsonRequest.setToken(accessToken);
        jsonRequest.post(null);
    }

    public void joinGameMulti(
            ArrayList<JoinMultiWrapper> players,
            String gameServerName, RoomsFilter filer, boolean autoCreate, boolean myRegionOnly,
            RoomSettings createSettings,
            AccessToken accessToken,
            final JoinGameMultiCallback callback)
    {
        ApplicationInfo applicationInfo = getOnlineLib().getApplicationInfo();

        JsonRequest jsonRequest = new JsonRequest(getOnlineLib(),
            getLocation() + "/join/multi/" + applicationInfo.getGameId() + "/" + gameServerName + "/" +
                applicationInfo.getGameVersion(),
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    String roomId = response.getString("id");
                    JSONObject location = response.optJSONObject("location");

                    if (location == null)
                    {
                        callback.fail(request, Status.failed);
                        return;
                    }

                    String host = location.getString("host");
                    JSONArray ports = location.getJSONArray("ports");

                    int[] pts = new int[ports.length()];

                    for (int i = 0; i < ports.length(); i++)
                    {
                        pts[i] = ports.getInt(i);
                    }

                    JSONObject slots = response.optJSONObject("slots");

                    if (slots == null)
                    {
                        callback.fail(request, Status.failed);
                        return;
                    }

                    HashMap<String, JoinMultiSlot> newSlots = new HashMap<String, JoinMultiSlot>();

                    for (Object account_ : slots.keySet())
                    {
                        String account = account_.toString();
                        JSONObject entry = slots.optJSONObject(account);

                        if (entry != null)
                        {
                            String key = entry.optString("key");
                            String slot = entry.optString("slot");

                            if (key != null && slot != null)
                            {
                                JoinMultiSlot newSlot = new JoinMultiSlot();

                                newSlot.key = key;
                                newSlot.slot = slot;

                                newSlots.put(account, newSlot);
                            }
                        }
                    }

                    callback.success(roomId, newSlots, host, pts, response.optJSONObject("settings"));
                } else
                {
                    callback.fail(request, status);
                }
            }
        });

        HashMap<String, Object> fields = new HashMap<String, Object>();

        JSONArray accounts = new JSONArray();

        for (JoinMultiWrapper player : players)
        {
            JSONObject wrapped = new JSONObject();
            wrapped.put("token", player.accessToken.getToken());
            wrapped.put("ip", player.ip);

            accounts.put(wrapped);
        }

        fields.put("settings", filer.toString());
        fields.put("auth_create", autoCreate ? "true" : "false");
        fields.put("my_region_only", myRegionOnly ? "true" : "false");
        fields.put("accounts", accounts.toString());

        if (autoCreate)
        {
            fields.put("create_settings", createSettings.toString());
        }

        jsonRequest.setToken(accessToken);
        jsonRequest.post(fields);

    }

    public void joinGame(String gameServerName, RoomsFilter filer,
                         boolean autoCreate, RoomSettings createSettings,
                         AccessToken accessToken, JoinGameCallback callback)
    {
        joinGame(gameServerName, filer, autoCreate, createSettings, accessToken, callback, true, null);
    }

    public void joinGame(String gameServerName, RoomsFilter filer,
                         boolean autoCreate, RoomSettings createSettings,
                         AccessToken accessToken, final JoinGameCallback callback,
                         boolean myRegionOnly, String region)
    {
        ApplicationInfo applicationInfo = getOnlineLib().getApplicationInfo();

        JsonRequest jsonRequest = new JsonRequest(getOnlineLib(),
            getLocation() + "/join/" + applicationInfo.getGameId() + "/" + gameServerName + "/" +
                applicationInfo.getGameVersion(),
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    String _roomId = response.getString("id");
                    String key = response.getString("key");
                    JSONObject location = response.optJSONObject("location");

                    if (location == null)
                    {
                        callback.fail(request, Status.failed);
                        return;
                    }

                    String host = location.getString("host");
                    JSONArray ports = location.getJSONArray("ports");

                    int[] pts = new int[ports.length()];

                    for (int i = 0; i < ports.length(); i++)
                    {
                        pts[i] = ports.getInt(i);
                    }

                    callback.success(_roomId, key, host, pts, response.optJSONObject("settings"));
                } else
                {
                    callback.fail(request, status);
                }
            }
        });


        HashMap<String, Object> fields = new HashMap<String, Object>();

        fields.put("settings", filer.toString());
        fields.put("auth_create", autoCreate ? "true" : "false");
        fields.put("my_region_only", myRegionOnly ? "true" : "false");

        if (autoCreate)
        {
            fields.put("create_settings", createSettings.toString());
        }

        if (region != null)
        {
            fields.put("region", region);
        }

        jsonRequest.setToken(accessToken);
        jsonRequest.post(fields);

    }

    public void listAccountRecords(String accountId,
                                   AccessToken accessToken,
                                   final ListPlayerRecordsCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getOnlineLib(),
                getLocation() + "/player/" + accountId,
                new Request.RequestResult()
                {
                    @Override
                    public void complete(Request request, Status status)
                    {
                        if (status == Status.success)
                        {
                            JSONObject response = ((JsonRequest) request).getObject();

                            JSONArray records = response.getJSONArray("records");

                            ArrayList<PlayerRecord> recordsResult = new ArrayList<PlayerRecord>();

                            for (int i = 0; i < records.length(); i++)
                            {
                                JSONObject record = records.getJSONObject(i);
                                recordsResult.add(new PlayerRecord(record));
                            }

                            callback.result(status, recordsResult);
                        }
                        else
                        {
                            callback.result(status, null);
                        }
                    }
                });

        jsonRequest.setToken(accessToken);
        jsonRequest.get();
    }

    public void listMultipleAccountsRecords(ArrayList<String> accountIds,
                                            AccessToken accessToken,
                                            final ListMultiplePlayersRecordsCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getOnlineLib(),
            getLocation() + "/players",
        new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    JSONObject records = response.optJSONObject("records");
                    HashMap<String, ArrayList<PlayerRecord>> result = new HashMap<String, ArrayList<PlayerRecord>>();

                    for (Object o : records.keySet())
                    {
                        String accountId = o.toString();
                        JSONArray accountRecords = records.optJSONArray(accountId);

                        ArrayList<PlayerRecord> accountRecordsResult = new ArrayList<PlayerRecord>();

                        for (int i = 0; i < accountRecords.length(); i++)
                        {
                            JSONObject record = accountRecords.getJSONObject(i);
                            accountRecordsResult.add(new PlayerRecord(record));
                        }

                        result.put(accountId, accountRecordsResult);
                    }

                    callback.result(status, result);
                }
                else
                {
                    callback.result(status, null);
                }
            }
        });

        HashMap<String, String> fields = new HashMap<String, String>();

        JSONArray accounts = new JSONArray();

        for (String id : accountIds)
        {
            accounts.put(id);
        }

        fields.put("accounts", accounts.toString());
        jsonRequest.setQueryArguments(fields);

        jsonRequest.setToken(accessToken);
        jsonRequest.get();
    }
}
