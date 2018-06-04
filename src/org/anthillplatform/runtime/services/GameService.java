package org.anthillplatform.runtime.services;

import org.anthillplatform.runtime.AnthillRuntime;
import org.anthillplatform.runtime.util.ApplicationInfo;
import org.anthillplatform.runtime.requests.JsonRequest;
import org.anthillplatform.runtime.requests.Request;
import org.anthillplatform.runtime.util.JsonRPC;
import org.anthillplatform.runtime.util.WebSocketJsonRPC;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.SSLContext;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.*;

/**
 * Game servers hosting & matchmaking service for Anthill platform
 *
 * See https://github.com/anthill-platform/anthill-game-master
 *     https://github.com/anthill-platform/anthill-game-controller
 */
public class GameService extends Service
{
    public static final String ID = "game";
    public static final String API_VERSION = "0.2";

    public interface JoinGameCallback
    {
        void success(String roomId, String key, String host, int[] ports, JSONObject settings);
        void fail(Request request, Request.Result result);
    }

    public static class JoinMultiSlot
    {
        public String slot;
        public String key;
    }

    public interface ListPlayerRecordsCallback
    {
        void result(GameService service, Request request, Request.Result result, List<PlayerRecord> records);
    }

    public interface ListMultiplePlayersRecordsCallback
    {
        void result(GameService service, Request request, Request.Result result,
                    Map<String, List<PlayerRecord>> records);
    }

    public interface JoinGameMultiCallback
    {
        void success(String roomId,
            HashMap<String, JoinMultiSlot> slots, String host, int[] ports, JSONObject settings);
        void fail(Request request, Request.Result result);
    }

    public interface GetPartyCallback
    {
        void result(GameService service, Request request, Request.Result result, Party party);
    }

    public interface CreateEmptyPartyCallback
    {
        void result(GameService service, Request request, Request.Result result, Party party);
    }

    public interface DeletePartyCallback
    {
        void result(GameService service, Request request, Request.Result result);
    }

    public static class JoinMultiWrapper
    {
        public LoginService.AccessToken accessToken;
        public String ip;

        public JoinMultiWrapper(LoginService.AccessToken accessToken, String ip)
        {
            this.accessToken = accessToken;
            this.ip = ip;
        }
    }

    public static class PartyMember
    {
        private String account;
        private int role;
        private JSONObject profile;

        public PartyMember(JSONObject data)
        {
            this.account = data.optString("account");
            this.role = data.optInt("role", 0);
            this.profile = data.optJSONObject("profile");
        }

        public String getAccount()
        {
            return account;
        }

        public int getRole()
        {
            return role;
        }

        public JSONObject getProfile()
        {
            return profile;
        }
    }

    public static class Party
    {
        private String id;
        private int members;
        private int maxMembers;
        private JSONObject settings;

        public Party(JSONObject data)
        {
            this.id = data.optString("id");
            this.members = data.optInt("num_members", 0);
            this.maxMembers = data.optInt("max_members", 8);
            this.settings = data.optJSONObject("settings");
        }

        public String getId()
        {
            return id;
        }

        public int getMembers()
        {
            return members;
        }

        public int getMaxMembers()
        {
            return maxMembers;
        }

        public JSONObject getSettings()
        {
            return settings;
        }
    }

    public static class PartySession
    {
        private PartySessionRPC jsonRPC;
        private Listener listener;
        private HashMap<String, InternalMessageHandler> internalHandlers;

        private static final String MESSAGE_TYPE_PLAYER_JOINED = "player_joined";
        private static final String MESSAGE_TYPE_PLAYER_LEFT = "player_left";
        private static final String MESSAGE_TYPE_GAME_STARTING = "game_starting";
        private static final String MESSAGE_TYPE_GAME_START_FAILED = "game_start_failed";
        private static final String MESSAGE_TYPE_GAME_STARTED = "game_started";
        private static final String MESSAGE_TYPE_CUSTOM = "custom";
        private static final String MESSAGE_TYPE_PARTY_CLOSED = "party_closed";

        public PartySession(Listener listener)
        {
            this.listener = listener;
        }

        private interface InternalMessageHandler
        {
            void onMessage(String messageType, JSONObject payload);
        }

        public interface Listener
        {
            void onError(int code, String message, String data);
            void onError(Exception e);
            void onOpen();
            void onClose(int code, String message, boolean remote);
            void onPartyInfoReceived(Party party, List<PartyMember> members);

            void onPlayerJoined(PartyMember member);
            void onPlayerLeft(PartyMember member);
            void onGameStarting(JSONObject payload);
            void onGameStartFailed(int code, String message);
            void onGameStarted(String roomId, String slot, String key, String host, ArrayList<Integer> ports,
                               JSONObject roomSettings);
            void onPartyClosed(JSONObject payload);
            void onCustomMessage(String messageType, JSONObject payload);
        }

        private class PartySessionRPC extends WebSocketJsonRPC
        {
            public PartySessionRPC(URI serverURI)
            {
                super(serverURI);
            }

            @Override
            protected void onError(int code, String message, String data)
            {
                listener.onError(code, message, data);
            }

            @Override
            public void onOpen(ServerHandshake serverHandshake)
            {
                listener.onOpen();
            }

            @Override
            public void onClose(int i, String s, boolean b)
            {
                listener.onClose(i, s, b);
            }

            @Override
            public void onError(Exception e)
            {
                listener.onError(e);
            }
        }

        public void close()
        {
            jsonRPC.close();
        }

        public PartySessionRPC getRPC()
        {
            return jsonRPC;
        }

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        public boolean isOpen()
        {
            return jsonRPC != null && jsonRPC.isOpen();
        }

        public boolean sendCustomMessage(JSONObject payload, JsonRPC.ResponseHandler callback)
        {
            if (!isOpen())
                return false;

            JSONObject args = new JSONObject();

            args.put("payload", payload);
            jsonRPC.request("send_message", callback, args);

            return true;
        }

        public boolean closeParty(JSONObject message, JsonRPC.ResponseHandler callback)
        {
            if (!isOpen())
                return false;

            JSONObject args = new JSONObject();

            args.put("message", message);
            jsonRPC.request("close_party", callback, args);

            return true;
        }

        public boolean join(JSONObject memberProfile, JsonRPC.ResponseHandler callback)
        {
            return join(memberProfile, null, callback);
        }

        public boolean join(JSONObject memberProfile, JSONObject checkMembers, JsonRPC.ResponseHandler callback)
        {
            if (!isOpen())
                return false;

            JSONObject args = new JSONObject();

            args.put("member_profile", memberProfile);

            if (checkMembers != null)
                args.put("check_members", checkMembers);

            jsonRPC.request("join_party", callback, args);

            return true;
        }

        public boolean leave(JsonRPC.ResponseHandler callback)
        {
            if (!isOpen())
                return false;

            JSONObject args = new JSONObject();

            jsonRPC.request("leave_party", callback, args);

            return true;
        }

        public boolean startGame(JSONObject message, JsonRPC.ResponseHandler callback)
        {
            if (!isOpen())
                return false;

            JSONObject args = new JSONObject();

            args.put("message", message);
            jsonRPC.request("start_game", callback, args);

            return true;
        }

        public void open(String location, HashMap<String, String> args)
        {
            URI uri;

            try
            {
                StringBuilder queryString = new StringBuilder();

                queryString.append(location);

                boolean first = true;

                for (Map.Entry<String, String> entry : args.entrySet())
                {
                    if (first)
                    {
                        queryString.append("?");
                        first = false;
                    }
                    else
                    {
                        queryString.append("&");
                    }

                    try {
                        queryString
                                .append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                                .append("=")
                                .append(URLEncoder.encode((entry.getValue() == null) ? "" :
                                        entry.getValue(), "UTF-8"));

                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                }

                uri = new URI(queryString.toString());
            }
            catch (URISyntaxException e)
            {
                e.printStackTrace();
                listener.onError(e);
                return;
            }

            if (uri.getScheme().equals("https"))
            {
                try
                {
                    uri = new URI("wss", null, uri.getHost(), uri.getPort(), uri.getPath(),
                        uri.getQuery(), uri.getFragment());
                    jsonRPC = new PartySessionRPC(uri);

                    SSLContext context = SSLContext.getInstance( "TLS" );

                    context.init(null, null, null);

                    jsonRPC.setSocket(context.getSocketFactory().createSocket());
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    listener.onError(e);
                    return;
                }
            }
            else
            {
                try
                {
                    uri = new URI("ws", null, uri.getHost(), uri.getPort(), uri.getPath(),
                        uri.getQuery(), uri.getFragment());
                    jsonRPC = new PartySessionRPC(uri);
                }
                catch (URISyntaxException e)
                {
                    e.printStackTrace();
                    listener.onError(e);
                    return;
                }
            }

            init();

            jsonRPC.connect();
        }

        private void init()
        {
            internalHandlers = new HashMap<String, InternalMessageHandler>();

            internalHandlers.put(MESSAGE_TYPE_PLAYER_JOINED, new InternalMessageHandler()
            {
                @Override
                public void onMessage(String messageType, JSONObject payload)
                {
                    listener.onPlayerJoined(new PartyMember(payload));
                }
            });

            internalHandlers.put(MESSAGE_TYPE_PLAYER_LEFT, new InternalMessageHandler()
            {
                @Override
                public void onMessage(String messageType, JSONObject payload)
                {
                    listener.onPlayerLeft(new PartyMember(payload));
                }
            });

            internalHandlers.put(MESSAGE_TYPE_GAME_STARTING, new InternalMessageHandler()
            {
                @Override
                public void onMessage(String messageType, JSONObject payload)
                {
                    listener.onGameStarting(payload);
                }
            });

            internalHandlers.put(MESSAGE_TYPE_GAME_START_FAILED, new InternalMessageHandler()
            {
                @Override
                public void onMessage(String messageType, JSONObject payload)
                {
                    listener.onGameStartFailed(payload.optInt("code", 500),
                        payload.optString("reason", "error"));
                }
            });

            internalHandlers.put(MESSAGE_TYPE_PARTY_CLOSED, new InternalMessageHandler()
            {
                @Override
                public void onMessage(String messageType, JSONObject payload)
                {
                    listener.onPartyClosed(payload);
                }
            });

            internalHandlers.put(MESSAGE_TYPE_CUSTOM, new InternalMessageHandler()
            {
                @Override
                public void onMessage(String messageType, JSONObject payload)
                {
                    listener.onCustomMessage(messageType, payload);
                }
            });

            internalHandlers.put(MESSAGE_TYPE_GAME_STARTED, new InternalMessageHandler()
            {
                @Override
                public void onMessage(String messageType, JSONObject payload)
                {
                    String roomId = payload.optString("id");
                    String slot = payload.optString("slot");
                    String key = payload.optString("key");

                    JSONObject location = payload.optJSONObject("location");
                    JSONObject roomSettings = payload.optJSONObject("settings");

                    if (roomId == null || key == null || location == null || roomSettings == null)
                    {
                        listener.onError(500, "Bad " + MESSAGE_TYPE_GAME_STARTED + " message received", "");
                        return;
                    }

                    String host = location.optString("host");
                    JSONArray ports = location.optJSONArray("ports");

                    ArrayList<Integer> _ports = new ArrayList<Integer>();

                    for (int i = 0, t = ports.length(); i < t; i++)
                    {
                        int port = ports.optInt(i, 0);
                        _ports.add(port);
                    }

                    listener.onGameStarted(roomId, slot, key, host, _ports, roomSettings);
                }
            });

            jsonRPC.addHandler("message", new JsonRPC.MethodHandler()
            {
                @Override
                public Object called(Object params)
                {
                    JSONObject args = ((JSONObject) params);

                    String messageType = args.optString("message_type");
                    JSONObject payload = args.optJSONObject("payload");

                    if (messageType == null || payload == null)
                    {
                        listener.onError(500, "Corrupted message received", args.toString());
                        return false;
                    }

                    InternalMessageHandler internalHandler = internalHandlers.get(messageType);

                    if (internalHandler == null)
                    {
                        listener.onError(500, "Unkonwn message type received", messageType);
                        return false;
                    }

                    internalHandler.onMessage(messageType, payload);

                    return null;
                }
            });

            jsonRPC.addHandler("party", new JsonRPC.MethodHandler()
            {
                @Override
                public Object called(Object params)
                {
                    JSONObject args = ((JSONObject) params);
                    JSONObject partyInfo = args.optJSONObject("party_info");

                    if (partyInfo == null)
                    {
                        listener.onError(500, "Bad data", "No party info argument");
                        return null;
                    }

                    JSONObject partyItself = partyInfo.optJSONObject("party");
                    JSONArray members = partyInfo.optJSONArray("members");

                    if (partyItself == null || members == null)
                    {
                        listener.onError(500, "Bad data", "No party/members argument");
                        return null;
                    }

                    ArrayList<PartyMember> members_ = new ArrayList<PartyMember>();

                    for (int i = 0, t = members.length(); i < t; i++)
                    {
                        JSONObject member_ = members.optJSONObject(i);
                        if (member_ == null)
                            continue;

                        members_.add(new PartyMember(member_));
                    }

                    listener.onPartyInfoReceived(new Party(partyItself), members_);

                    return null;
                }
            });
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

    public interface GetGamesStatusCallback
    {
        void result(GameService service, Request request, Request.Result result, GamesStatus status);
    }

    public interface ListGamesCallback
    {
        void result(GameService service, Request request, Request.Result result, List<Room> rooms);
    }

    public interface ListRegionsCallback
    {
        void result(GameService service, Request request, Request.Result result, List<Region> regions, String myRegion);
    }

    /**
     * Please note that you should not create an instance of the service yourself,
     * and use AnthillRuntime.Get(GameService.ID, GameService.class) to get existing one instead
     */
    public GameService(AnthillRuntime runtime, String location)
    {
        super(runtime, location, ID, API_VERSION);
    }

    public static GameService Get()
    {
        return AnthillRuntime.Get(ID, GameService.class);
    }

    public void getStatus(final GetGamesStatusCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getLocation() + "/status",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                if (result == Request.Result.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    GamesStatus gamesStatus = new GamesStatus();

                    gamesStatus.players = response.optInt("players", 0);

                    callback.result(GameService.this, request, result, gamesStatus);
                }
                else
                {
                    callback.result(GameService.this, request, result, null);
                }
            }
        });

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.get();
    }

    public void getRegions(LoginService.AccessToken accessToken, final ListRegionsCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getLocation() + "/regions",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                if (result == Request.Result.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    JSONObject regionsData = response.optJSONObject("regions");
                    String myRegion = response.optString("my_region");

                    if (regionsData != null && myRegion != null)
                    {
                        LinkedList<Region> regions = new LinkedList<Region>();

                        for (Object key : regionsData.keySet())
                        {
                            String name = key.toString();

                            JSONObject region = regionsData.getJSONObject(name);
                            regions.add(new Region(name, region));
                        }


                        callback.result(GameService.this, request, result, regions, myRegion);
                    } else
                    {
                        callback.result(GameService.this, request, result, null, null);
                    }

                } else
                {
                    callback.result(GameService.this, request, result, null, null);
                }
            }
        });


        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);

        jsonRequest.get();
    }

    public void createGame(LoginService.AccessToken accessToken, String gameServerName, RoomSettings createSettings,
                           final JoinGameCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(
                getLocation() + "/create/" + getRuntime().getApplicationInfo().applicationName + "/" + gameServerName + "/" +
                getRuntime().getApplicationInfo().applicationVersion,
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result status)
            {
                if (status == Request.Result.success)
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

        Request.Fields fields = new Request.Fields();

        fields.put("settings", createSettings.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.post(fields);

    }

    public void listGames(
        LoginService.AccessToken accessToken,
        String gameServerName, RoomsFilter filter,
        ListGamesCallback callback)
    {
        listGames(accessToken, gameServerName, filter, callback, false, true, null);
    }

    public void listGames(
        LoginService.AccessToken accessToken,
        String gameServerName,
        RoomsFilter filter,
        final ListGamesCallback callback,
        boolean myRegionOnly,
        boolean showFull,
        String region)
    {
        ApplicationInfo applicationInfo = getRuntime().getApplicationInfo();

        JsonRequest jsonRequest = new JsonRequest(
            getLocation() + "/rooms/" + applicationInfo.applicationName + "/" + gameServerName + "/" +
            applicationInfo.applicationVersion,
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                if (result == Request.Result.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    JSONArray roomsData = response.getJSONArray("rooms");

                    LinkedList<Room> rooms = new LinkedList<Room>();

                    for (int i = 0; i < roomsData.length(); i++)
                    {
                        JSONObject roomData = roomsData.getJSONObject(i);
                        rooms.add(new Room(roomData));
                    }

                    callback.result(GameService.this, request, result, rooms);
                } else
                {
                    callback.result(GameService.this, request, result, null);
                }
            }
        });

        Request.Fields fields = new Request.Fields();

        fields.put("settings", filter.toString());
        fields.put("show_full", showFull ? "true" : "false");
        fields.put("my_region_only", myRegionOnly ? "true" : "false");

        if (region != null)
        {
            fields.put("region", region);
        }

        jsonRequest.setQueryArguments(fields);

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.get();

    }

    public void joinGame(LoginService.AccessToken accessToken, String roomId, final JoinGameCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(
                getLocation() + "/room/" + getRuntime().getApplicationInfo().applicationName + "/" + roomId + "/join",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result status)
            {
                if (status == Request.Result.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    String _roomId = response.getString("id");
                    String key = response.getString("key");
                    JSONObject location = response.optJSONObject("location");

                    if (location == null)
                    {
                        callback.fail(request, Request.Result.failed);
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

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.post(null);
    }

    public void joinGameMulti(
        LoginService.AccessToken accessToken,
        ArrayList<JoinMultiWrapper> players,
        String gameServerName,
        RoomsFilter filer,
        boolean autoCreate,
        boolean myRegionOnly,
        RoomSettings createSettings,
        final JoinGameMultiCallback callback)
    {
        ApplicationInfo applicationInfo = getRuntime().getApplicationInfo();

        JsonRequest jsonRequest = new JsonRequest(
            getLocation() + "/join/multi/" + applicationInfo.applicationName + "/" + gameServerName + "/" +
            applicationInfo.applicationVersion,
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result status)
            {
                if (status == Request.Result.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    String roomId = response.getString("id");
                    JSONObject location = response.optJSONObject("location");

                    if (location == null)
                    {
                        callback.fail(request, Request.Result.failed);
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
                        callback.fail(request, Request.Result.failed);
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

        Request.Fields fields = new Request.Fields();

        JSONArray accounts = new JSONArray();

        for (JoinMultiWrapper player : players)
        {
            JSONObject wrapped = new JSONObject();
            wrapped.put("token", player.accessToken.get());
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

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.post(fields);

    }

    public void joinGame(
        LoginService.AccessToken accessToken, String gameServerName, RoomsFilter filer,
        boolean autoCreate, RoomSettings createSettings,
        JoinGameCallback callback)
    {
        joinGame(accessToken, gameServerName, filer, autoCreate, createSettings, callback, true, null);
    }

    public void joinGame(
        LoginService.AccessToken accessToken,
        String gameServerName, RoomsFilter filer,
        boolean autoCreate, RoomSettings createSettings,
        final JoinGameCallback callback,
        boolean myRegionOnly, String region)
    {
        ApplicationInfo applicationInfo = getRuntime().getApplicationInfo();

        JsonRequest jsonRequest = new JsonRequest(
            getLocation() + "/join/" + applicationInfo.applicationName + "/" + gameServerName + "/" +
            applicationInfo.applicationVersion,
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result status)
            {
                if (status == Request.Result.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    String _roomId = response.getString("id");
                    String key = response.getString("key");
                    JSONObject location = response.optJSONObject("location");

                    if (location == null)
                    {
                        callback.fail(request, Request.Result.failed);
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


        Request.Fields fields = new Request.Fields();

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

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.post(fields);

    }

    public void listAccountRecords(
        LoginService.AccessToken accessToken,
        String accountId,
        final ListPlayerRecordsCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getLocation() + "/player/" + accountId,
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                if (result == Request.Result.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    JSONArray records = response.getJSONArray("records");

                    List<PlayerRecord> recordsResult = new ArrayList<PlayerRecord>();

                    for (int i = 0; i < records.length(); i++)
                    {
                        JSONObject record = records.getJSONObject(i);
                        recordsResult.add(new PlayerRecord(record));
                    }

                    callback.result(GameService.this, request, result, recordsResult);
                }
                else
                {
                    callback.result(GameService.this, request, result, null);
                }
            }
        });

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.get();
    }

    public void listMultipleAccountsRecords(
        LoginService.AccessToken accessToken,
        List<String> accountIds,
        final ListMultiplePlayersRecordsCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getLocation() + "/players",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                if (result == Request.Result.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    JSONObject records = response.optJSONObject("records");
                    HashMap<String, List<PlayerRecord>> result_ = new HashMap<String, List<PlayerRecord>>();

                    for (Object o : records.keySet())
                    {
                        String accountId = o.toString();
                        JSONArray accountRecords = records.optJSONArray(accountId);

                        List<PlayerRecord> accountRecordsResult = new ArrayList<PlayerRecord>();

                        for (int i = 0; i < accountRecords.length(); i++)
                        {
                            JSONObject record = accountRecords.getJSONObject(i);
                            accountRecordsResult.add(new PlayerRecord(record));
                        }

                        result_.put(accountId, accountRecordsResult);
                    }

                    callback.result(GameService.this, request, result, result_);
                }
                else
                {
                    callback.result(GameService.this, request, result, null);
                }
            }
        });

        Request.Fields fields = new Request.Fields();

        JSONArray accounts = new JSONArray();

        for (String id : accountIds)
        {
            accounts.put(id);
        }

        fields.put("accounts", accounts.toString());
        jsonRequest.setQueryArguments(fields);

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.get();
    }

    public void createParty(
        LoginService.AccessToken accessToken,
        String gameServerName,
        JSONObject partySettings,
        JSONObject roomSettings,
        JSONObject roomFilters,
        int maxMembers,
        String region,
        boolean autoStart,
        boolean autoClose,
        String closeCallback,
        final CreateEmptyPartyCallback callback)
    {
        ApplicationInfo applicationInfo = getRuntime().getApplicationInfo();

        JsonRequest jsonRequest = new JsonRequest(
            getLocation() + "/party/create/" + applicationInfo.applicationName + "/" +
                applicationInfo.applicationVersion + "/" + gameServerName,
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                if (result == Request.Result.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();
                    JSONObject party_ = response.optJSONObject("party");
                    Party party;

                    if (party_ != null)
                    {
                        party = new Party(party_);
                    }
                    else
                    {
                        party = null;
                    }

                    callback.result(GameService.this, request, result, party);
                }
                else
                {
                    callback.result(GameService.this, request, result, null);
                }
            }
        });

        Request.Fields args = new Request.Fields();
        args.put("max_members", String.valueOf(maxMembers));
        args.put("auto_start", autoStart ? "true" : "false");
        args.put("auto_close", autoClose ? "true" : "false");

        if (region != null)
            args.put("region", region);
        if (partySettings != null)
            args.put("party_settings", partySettings.toString());
        if (roomSettings != null)
            args.put("room_settings", roomSettings.toString());
        if (roomFilters != null)
            args.put("room_filters", roomFilters.toString());
        if (closeCallback != null)
            args.put("close_callback", closeCallback);

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.post(args);

    }

    public void closeParty(
        LoginService.AccessToken accessToken,
        String partyId,
        JSONObject message,
        final DeletePartyCallback callback)
    {
        ApplicationInfo applicationInfo = getRuntime().getApplicationInfo();

        JsonRequest jsonRequest = new JsonRequest(getLocation() + "/party/" + partyId,
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                callback.result(GameService.this, request, result);
            }
        });

        Request.Fields args = new Request.Fields();
        args.put("message", message.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.delete(args);
    }

    public void getParty(
        LoginService.AccessToken accessToken,
        String partyId,
        final GetPartyCallback callback)
    {
        ApplicationInfo applicationInfo = getRuntime().getApplicationInfo();

        JsonRequest jsonRequest = new JsonRequest(getLocation() + "/party/" + partyId,
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                if (result == Request.Result.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();
                    JSONObject party_ = response.optJSONObject("party");
                    Party party;

                    if (party_ != null)
                    {
                        party = new Party(party_);
                    }
                    else
                    {
                        party = null;
                    }

                    callback.result(GameService.this, request, result, party);
                }
                else
                {
                    callback.result(GameService.this, request, result, null);
                }
            }
        });

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.get();
    }

    public PartySession openNewPartySession(
        String gameServerName, LoginService.AccessToken accessToken, PartySession.Listener listener)
    {
        return openNewPartySession(gameServerName, null, null, null, null, 8, null, null,
            true, true, true, accessToken, listener);
    }

    public PartySession openNewPartySession(
            String gameServerName, JSONObject partySettings, JSONObject roomSettings, JSONObject roomFilters,
            JSONObject memberProfile, int maxMembers, String region,
            String closeCallback, boolean autoJoin, boolean autoStart, boolean autoClose,
            LoginService.AccessToken accessToken, PartySession.Listener listener)
    {
        ApplicationInfo applicationInfo = getRuntime().getApplicationInfo();

        HashMap<String, String> args = new HashMap<String, String>();

        args.put("max_members", String.valueOf(maxMembers));
        args.put("auto_join", autoJoin ? "true" : "false");
        args.put("auto_start", autoStart ? "true" : "false");
        args.put("auto_close", autoClose ? "true" : "false");

        if (region != null)
            args.put("region", region);
        if (partySettings != null)
            args.put("party_settings", partySettings.toString());
        if (roomSettings != null)
            args.put("room_settings", roomSettings.toString());
        if (roomFilters != null)
            args.put("room_filters", roomFilters.toString());
        if (memberProfile != null && autoJoin)
            args.put("member_profile", memberProfile.toString());
        if (closeCallback != null)
            args.put("close_callback", closeCallback);

        args.put("access_token", accessToken.get());

        PartySession partySession = new PartySession(listener);
        partySession.open(
            getLocation() + "/party/create/" +
            applicationInfo.applicationName + "/" + applicationInfo.applicationVersion +
                "/" + gameServerName + "/session",
            args);

        return partySession;
    }

    public PartySession openExistingPartySession(
        String partyId, LoginService.AccessToken accessToken, PartySession.Listener listener)
    {
        return openExistingPartySession(partyId, null, null, true, accessToken, listener);
    }

    public PartySession openExistingPartySession(
        String partyId, JSONObject memberProfile, JSONObject checkMembers,
        boolean autoJoin,
        LoginService.AccessToken accessToken, PartySession.Listener listener)
    {
        ApplicationInfo applicationInfo = getRuntime().getApplicationInfo();

        HashMap<String, String> args = new HashMap<String, String>();

        if (memberProfile != null && autoJoin)
            args.put("member_profile", memberProfile.toString());
        if (checkMembers != null)
            args.put("check_members", checkMembers.toString());

        args.put("auto_join", autoJoin ? "true" : "false");
        args.put("access_token", accessToken.get());

        PartySession partySession = new PartySession(listener);
        partySession.open(
            getLocation() + "/party/" + partyId + "/session",
            args);

        return partySession;
    }
}
