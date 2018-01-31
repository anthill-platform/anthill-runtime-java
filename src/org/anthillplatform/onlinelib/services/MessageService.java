package org.anthillplatform.onlinelib.services;

import org.anthillplatform.onlinelib.OnlineLib;
import org.anthillplatform.onlinelib.Status;
import org.anthillplatform.onlinelib.entity.AccessToken;
import org.anthillplatform.onlinelib.request.JsonRequest;
import org.anthillplatform.onlinelib.request.Request;
import org.anthillplatform.onlinelib.util.JsonRPC;
import org.anthillplatform.onlinelib.util.WebSocketJsonRPC;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.SSLContext;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.anthillplatform.onlinelib.util.Utils.DATE_FORMAT;

public class MessageService extends Service
{
    public static final String ID = "message";

    private static MessageService instance;
    public static MessageService get() { return instance; }
    private static void set(MessageService service) { instance = service; }

    public interface GetMessagesCallback
    {
        void complete(MessageDestination replyTo, Status status);
    }

    public interface MessageCallback
    {
        void onMessage(String messageType, String recipientClass, String recipientKey, Date time,
                       String messageId, String sender, int gamespace, JSONObject payload);
    }

    public interface LastReadMessageCallback
    {
        void onLastReadMessage(String recipientClass, String recipientKey, Date time, String messageId);
    }

    public MessageService(OnlineLib onlineLib, String location)
    {
        super(onlineLib, location, ID);

        set(this);
    }

    public static class Message
    {

        public final String uuid;
        public final String recipientClass;
        public final String recipient;
        public final String sender;
        public final String type;
        public final JSONObject payload;
        public final Date time;
        public final int gamespace;
        public Set<String> flags;

        public Message(JSONObject data)
        {
            this.uuid = data.optString("uuid");
            this.recipientClass = data.optString("recipient_class");
            this.recipient = data.optString("recipient");
            this.sender = data.optString("sender");
            this.type = data.optString("type");
            this.gamespace = data.optInt("gamespace", 0);
            this.payload = data.optJSONObject("payload");

            Date tmp;

            try
            {
                tmp = DATE_FORMAT.parse(data.optString("time"));
            }
            catch (ParseException e)
            {
                tmp = null;
            }

            JSONArray flags = data.optJSONArray("flags");

            if (flags != null && flags.length() > 0)
            {
                this.flags = new HashSet<String>();

                for (int i = 0; i < flags.length(); i++)
                {
                    String flag = flags.optString(i);

                    if (flag != null)
                    {
                        this.flags.add(flag);
                    }
                }
            }

            this.time = tmp;
        }
    }

    public static class LastReadMessage
    {
        public final String uuid;
        public final String recipientClass;
        public final String recipient;
        public final Date time;

        public LastReadMessage(JSONObject data)
        {
            this.uuid = data.optString("uuid");
            this.recipientClass = data.optString("recipient_class");
            this.recipient = data.optString("recipient");

            Date tmp;

            try
            {
                tmp = DATE_FORMAT.parse(data.optString("time"));
            }
            catch (ParseException e)
            {
                tmp = null;
            }

            this.time = tmp;
        }
    }

    public static class MessageSession
    {
        private static final JSONArray EmptyFlags = new JSONArray();

        public interface Listener
        {
            void onError(int code, String message, String data);
            void onError(Exception e);
            void onOpen();
            void onClose(int code, String message, boolean remote);

            void onMessage(String messageType, String recipientClass, String recipientKey,
                           String messageId, Date time, String sender, int gamespace,
                           JSONObject payload, Set<String> flags);

            void onMessageDeleted(String messageId, String sender, int gamespace);
            void onMessageUpdated(String messageId, String sender, int gamespace, JSONObject payload);
        }

        private MessageSessionRPC jsonRPC;
        private Listener listener;
        private final Set<String> messageTypes;

        private class MessageSessionRPC extends WebSocketJsonRPC
        {
            public MessageSessionRPC(URI serverURI)
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

        public MessageSession(Listener listener, Set<String> messageTypes)
        {
            this.listener = listener;
            this.messageTypes = messageTypes;
        }

        public void close()
        {
            jsonRPC.close();
        }

        public MessageSessionRPC getRPC()
        {
            return jsonRPC;
        }

        public boolean isOpen()
        {
            return jsonRPC != null && jsonRPC.isOpen();
        }

        public boolean sendMessage(String recipientClass, String recipientKey, String messageType,
                                   JSONObject message)
        {
            return sendMessage(recipientClass, recipientKey, messageType, message, null);
        }

        public boolean sendMessage(String recipientClass, String recipientKey, String messageType,
                                   JSONObject message, Set<String> flags)
        {
            return sendMessage(recipientClass, recipientKey, messageType, message, flags, null);
        }

        public boolean markMessageAsRead(String messageId)
        {
            return markMessageAsRead(messageId, null);
        }

        public boolean markMessageAsRead(String messageId, JsonRPC.ResponseHandler callback)
        {
            if (!isOpen())
                return false;

            JSONObject args = new JSONObject();

            args.put("message_id", messageId);

            jsonRPC.request("mark_as_read", callback, args);

            return true;
        }

        public boolean sendMessage(String recipientClass, String recipientKey, String messageType,
                                   JSONObject message, Set<String> flags, JsonRPC.ResponseHandler callback)
        {
            if (!isOpen())
                return false;

            JSONObject args = new JSONObject();

            args.put("recipient_class", recipientClass);
            args.put("recipient_key", recipientKey);
            args.put("message_type", messageType);
            args.put("message", message);

            if (flags != null)
            {
                JSONArray _flags = new JSONArray();
                for (String flag : flags)
                {
                    _flags.put(flag);
                }
                args.put("flags", _flags);
            }
            else
            {
                args.put("flags", EmptyFlags);
            }

            jsonRPC.request("send_message", callback, args);

            return true;
        }

        public boolean deleteMessage(String messageId, JsonRPC.ResponseHandler callback)
        {
            if (!isOpen())
                return false;

            JSONObject args = new JSONObject();
            args.put("message_id", messageId);
            jsonRPC.request("delete_message", callback, args);

            return true;
        }

        public boolean updateMessage(String messageId, JSONObject payload, JsonRPC.ResponseHandler callback)
        {
            if (!isOpen())
                return false;

            JSONObject args = new JSONObject();
            args.put("message_id", messageId);
            args.put("payload", payload);
            jsonRPC.request("update_message", callback, args);

            return true;
        }

        public void open(MessageService messageService, AccessToken accessToken)
        {
            HashMap<String, String> args = new HashMap<String, String>();
            args.put("access_token", accessToken.getToken());

            if (messageTypes != null)
            {
                JSONArray messageTypes_ = new JSONArray();
                for (String messageType : messageTypes)
                {
                    messageTypes_.put(messageType);
                }

                args.put("message_types", messageTypes_.toString());
            }

            URI uri;

            try
            {
                StringBuilder queryString = new StringBuilder();

                queryString.append(messageService.getLocation()).append("/listen");

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
                    jsonRPC = new MessageSessionRPC(uri);

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
                    jsonRPC = new MessageSessionRPC(uri);
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
            jsonRPC.addHandler("message", new JsonRPC.MethodHandler()
            {
                @Override
                public Object called(Object params) throws JsonRPC.JsonRPCException
                {
                    JSONObject args = ((JSONObject) params);

                    int gamespace = args.optInt("gamespace_id");
                    String messageId = args.optString("message_id");
                    String sender = args.optString("sender");
                    String recipientClass = args.optString("recipient_class");
                    String recipientKey = args.optString("recipient_key");
                    String messageType = args.optString("message_type");
                    JSONObject payload = args.optJSONObject("payload");

                    Date time;

                    try
                    {
                        time = DATE_FORMAT.parse(args.optString("time", ""));
                    }
                    catch (ParseException e)
                    {
                        listener.onError(500, "Corrupted message received", e.toString());
                        return false;
                    }

                    if (gamespace <= 0 ||
                            messageId == null ||
                            sender == null ||
                            recipientClass == null ||
                            recipientKey == null ||
                            messageType == null ||
                            payload == null)
                    {
                        listener.onError(500, "Corrupted message received", args.toString());
                        return false;
                    }


                    JSONArray flags_ = args.optJSONArray("flags");
                    Set<String> flags;

                    if (flags_ != null && flags_.length() > 0)
                    {
                        flags = new HashSet<String>();

                        for (int i = 0; i < flags_.length(); i++)
                        {
                            String flag = flags_.optString(i);

                            if (flag != null)
                            {
                                flags.add(flag);
                            }
                        }
                    }
                    else
                    {
                        flags = null;
                    }

                    listener.onMessage(messageType, recipientClass, recipientKey,
                            messageId, time, sender, gamespace, payload, flags);

                    return null;
                }
            });

            jsonRPC.addHandler("message_deleted", new JsonRPC.MethodHandler()
            {
                @Override
                public Object called(Object params) throws JsonRPC.JsonRPCException
                {
                    JSONObject args = ((JSONObject) params);

                    int gamespace = args.optInt("gamespace_id");
                    String messageId = args.optString("message_id");
                    String sender = args.optString("sender");

                    if (gamespace <= 0 || messageId == null || sender == null)
                    {
                        return false;
                    }

                    listener.onMessageDeleted(messageId, sender, gamespace);

                    return null;
                }
            });

            jsonRPC.addHandler("message_updated", new JsonRPC.MethodHandler()
            {
                @Override
                public Object called(Object params) throws JsonRPC.JsonRPCException
                {
                    JSONObject args = ((JSONObject) params);

                    int gamespace = args.optInt("gamespace_id");
                    String messageId = args.optString("message_id");
                    String sender = args.optString("sender");
                    JSONObject payload = args.optJSONObject("payload");

                    if (gamespace <= 0 || messageId == null || sender == null || payload == null)
                    {
                        return false;
                    }

                    listener.onMessageUpdated(messageId, sender, gamespace, payload);

                    return null;
                }
            });
        }
    }

    public static class MessageDestination
    {
        private String recipientClass;
        private String recipientKey;

        public MessageDestination(JSONObject data)
        {
            this.recipientClass = data.optString("recipient_class");
            this.recipientKey = data.optString("recipient");
        }

        public String getRecipientClass()
        {
            return recipientClass;
        }

        public String getRecipientKey()
        {
            return recipientKey;
        }
    }

    public MessageSession session(AccessToken accessToken, MessageSession.Listener listener)
    {
        return session(accessToken, null, listener);
    }

    public MessageSession session(AccessToken accessToken, Set<String> messageTypes, MessageSession.Listener listener)
    {
        MessageSession session = new MessageSession(listener, messageTypes);
        session.open(this, accessToken);
        return session;
    }

    public void getMessages(ArrayList<Message> messagesToFill,
                            ArrayList<LastReadMessage> lastReadMessagesToFill,
                            AccessToken accessToken, final GetMessagesCallback callback)
    {
        getMessages(messagesToFill, lastReadMessagesToFill, 0, 100, accessToken, callback);
    }

    public void getMessages(ArrayList<Message> messagesToFill,
                            ArrayList<LastReadMessage> lastReadMessagesToFill, int limit,
                            AccessToken accessToken, final GetMessagesCallback callback)
    {
        getMessages(messagesToFill, lastReadMessagesToFill, 0, limit, accessToken, callback);
    }

    public void getMessages(final ArrayList<Message> messagesToFill,
                            final ArrayList<LastReadMessage> lastReadMessagesToFill,
                            int offset, int limit,
                            AccessToken accessToken, final GetMessagesCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getOnlineLib(), getLocation() + "/messages",
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    JSONArray messages = response.optJSONArray("messages");

                    if (messages == null)
                    {
                        callback.complete(null, Status.dataCorrupted);
                        return;
                    }
                    else
                    {
                        for (int i = 0, t = messages.length(); i < t; i++)
                        {
                            messagesToFill.add(new Message(messages.optJSONObject(i)));
                        }
                    }

                    JSONArray lastReadMessages = response.optJSONArray("last_read_messages");

                    if (lastReadMessages != null)
                    {
                        for (int i = 0, t = lastReadMessages.length(); i < t; i++)
                        {
                            lastReadMessagesToFill.add(new LastReadMessage(lastReadMessages.optJSONObject(i)));
                        }
                    }

                    callback.complete(null, Status.success);
                }
                else
                {
                    callback.complete(null, status);
                }
            }
        });

        jsonRequest.setToken(accessToken);
        jsonRequest.get();
    }

    public void getMessages(MessageCallback messageCallback, LastReadMessageCallback lastReadMessageCallback,
                            AccessToken accessToken, final GetMessagesCallback callback)
    {
        getMessages(messageCallback, lastReadMessageCallback, 0, 100, accessToken, callback);
    }

    public void getMessages(MessageCallback messageCallback, LastReadMessageCallback lastReadMessageCallback,
                            int limit,
                            AccessToken accessToken, final GetMessagesCallback callback)
    {
        getMessages(messageCallback, lastReadMessageCallback, 0, limit, accessToken, callback);
    }

    public void getMessages(final MessageCallback messageCallback,
                            final LastReadMessageCallback lastReadMessageCallback,
                            int offset, int limit,
                            AccessToken accessToken, final GetMessagesCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getOnlineLib(), getLocation() + "/messages",
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    JSONArray messages = response.optJSONArray("messages");
                    JSONObject replyTo_ = response.optJSONObject("reply_to");

                    if (messages == null || replyTo_ == null)
                    {
                        callback.complete(null, Status.dataCorrupted);
                        return;
                    }
                    else
                    {
                        for (int i = 0, t = messages.length(); i < t; i++)
                        {
                            JSONObject data = messages.optJSONObject(i);

                            String uuid = data.optString("uuid");
                            String recipientClass = data.optString("recipient_class");
                            String recipient = data.optString("recipient");
                            String sender = data.optString("sender");
                            int gamespace = data.optInt("gamespace", 0);
                            String type = data.optString("type");
                            JSONObject payload = data.optJSONObject("payload");

                            Date tmp;

                            try
                            {
                                tmp = DATE_FORMAT.parse(data.optString("time"));
                            }
                            catch (ParseException e)
                            {
                                tmp = null;
                            }

                            messageCallback.onMessage(type, recipientClass, recipient, tmp,
                                    uuid, sender, gamespace, payload);
                        }
                    }

                    JSONArray lastReadMessages = response.optJSONArray("last_read_messages");

                    if (lastReadMessages != null)
                    {
                        for (int i = 0, t = lastReadMessages.length(); i < t; i++)
                        {
                            JSONObject data = lastReadMessages.optJSONObject(i);

                            String uuid = data.optString("uuid");
                            String recipientClass = data.optString("recipient_class");
                            String recipient = data.optString("recipient");

                            Date tmp;

                            try
                            {
                                tmp = DATE_FORMAT.parse(data.optString("time"));
                            }
                            catch (ParseException e)
                            {
                                tmp = null;
                            }

                            lastReadMessageCallback.onLastReadMessage(recipientClass, recipient, tmp, uuid);
                        }
                    }

                    MessageDestination replyTo = new MessageDestination(replyTo_);
                    callback.complete(replyTo, Status.success);
                }
                else
                {
                    callback.complete(null, status);
                }
            }
        });

        jsonRequest.setToken(accessToken);
        jsonRequest.get();
    }

    public void getGroupMessages(final ArrayList<Message> messagesToFill,
        String groupClass, String groupKey,
        AccessToken accessToken, final GetMessagesCallback callback)
    {
        getGroupMessages(messagesToFill, groupClass, groupKey, 100, accessToken, callback);
    }

    public void getGroupMessages(final ArrayList<Message> messagesToFill,
        String groupClass, String groupKey, int limit,
        AccessToken accessToken, final GetMessagesCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getOnlineLib(),
            getLocation() + "/group/" + groupClass + "/" + groupKey,
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    JSONArray messages = response.optJSONArray("messages");
                    JSONObject replyTo_ = response.optJSONObject("reply_to");

                    if (messages == null || replyTo_ == null)
                    {
                        callback.complete(null, Status.dataCorrupted);
                        return;
                    }
                    else
                    {
                        for (int i = 0, t = messages.length(); i < t; i++)
                        {
                            messagesToFill.add(new Message(messages.optJSONObject(i)));
                        }
                    }

                    MessageDestination replyTo = new MessageDestination(replyTo_);
                    callback.complete(replyTo, Status.success);
                }
                else
                {
                    callback.complete(null, status);
                }
            }
        });

        jsonRequest.setToken(accessToken);
        jsonRequest.get();
    }

    public void getGroupMessages(final MessageCallback messageCallback,
        String groupClass, String groupKey,
        AccessToken accessToken, final GetMessagesCallback callback)
    {
        getGroupMessages(messageCallback, groupClass, groupKey, 100, accessToken, callback);
    }

    public void getGroupMessages(final MessageCallback messageCallback,
        String groupClass, String groupKey, int limit,
        AccessToken accessToken, final GetMessagesCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getOnlineLib(),
            getLocation() + "/group/" + groupClass + "/" + groupKey,
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    JSONArray messages = response.optJSONArray("messages");
                    JSONObject replyTo_ = response.optJSONObject("reply_to");

                    if (messages == null || replyTo_ == null)
                    {
                        callback.complete(null, Status.dataCorrupted);
                        return;
                    }
                    else
                    {
                        for (int i = 0, t = messages.length(); i < t; i++)
                        {
                            JSONObject data = messages.optJSONObject(i);

                            String uuid = data.optString("uuid");
                            String recipientClass = data.optString("recipient_class");
                            String recipient = data.optString("recipient");
                            String sender = data.optString("sender");
                            int gamespace = data.optInt("gamespace", 0);
                            String type = data.optString("type");
                            JSONObject payload = data.optJSONObject("payload");

                            Date tmp;

                            try
                            {
                                tmp = DATE_FORMAT.parse(data.optString("time"));
                            }
                            catch (ParseException e)
                            {
                                tmp = null;
                            }

                            messageCallback.onMessage(type, recipientClass, recipient, tmp,
                                    uuid, sender, gamespace, payload);
                        }
                    }

                    MessageDestination replyTo = new MessageDestination(replyTo_);
                    callback.complete(replyTo, Status.success);
                }
                else
                {
                    callback.complete(null, status);
                }
            }
        });

        jsonRequest.setToken(accessToken);
        jsonRequest.get();
    }
}
