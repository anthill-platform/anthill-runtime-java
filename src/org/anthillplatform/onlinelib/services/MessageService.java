package org.anthillplatform.onlinelib.services;

import org.anthillplatform.onlinelib.OnlineLib;
import org.anthillplatform.onlinelib.entity.AccessToken;
import org.anthillplatform.onlinelib.util.WebSocketJsonRPC;

import javax.net.ssl.SSLContext;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class MessageService extends Service
{
    public static final String ID = "message";

    private static MessageService instance;
    public static MessageService get() { return instance; }
    private static void set(MessageService service) { instance = service; }

    public MessageService(OnlineLib onlineLib, String location)
    {
        super(onlineLib, location, ID);

        set(this);
    }

    public interface ConversationConstructor
    {
        WebSocketJsonRPC newConversation(URI uri);
    }

    public WebSocketJsonRPC listener(AccessToken accessToken, ConversationConstructor listener)
    {
        HashMap<String, String> args = new HashMap<String, String>();
        args.put("access_token", accessToken.getToken());

        URI uri;

        try
        {
            StringBuilder queryString = new StringBuilder();

            queryString.append(getLocation()).append("/listen");

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
            return null;
        }

        WebSocketJsonRPC jsonRPC;

        if (uri.getScheme().equals("https"))
        {
            try
            {
                jsonRPC = listener.newConversation(
                        new URI("wss", uri.getHost(), uri.getPath(), uri.getQuery(), uri.getFragment()));

                SSLContext context = SSLContext.getInstance( "TLS" );

                context.init(null, null, null);

                jsonRPC.setSocket(context.getSocketFactory().createSocket());
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return null;
            }
        }
        else
        {
            try
            {
                jsonRPC = listener.newConversation(new URI("ws", uri.getHost(), uri.getPath(),
                        uri.getQuery(), uri.getFragment()));
            }
            catch (URISyntaxException e)
            {
                e.printStackTrace();
                return null;
            }
        }

        return jsonRPC;
    }
}
