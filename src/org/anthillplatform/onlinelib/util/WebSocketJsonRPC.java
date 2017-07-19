package org.anthillplatform.onlinelib.util;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.json.JSONObject;

import java.net.URI;

public abstract class WebSocketJsonRPC extends WebSocketClient
{
    private JsonRPC rpc;

    public WebSocketJsonRPC(URI serverURI)
    {
        super(serverURI, new Draft_17());

        rpc = new JsonRPC()
        {
            @Override
            protected void send(String data)
            {
                WebSocketJsonRPC.this.send(data);
            }

            @Override
            public void onError(int code, String message, String data)
            {
                WebSocketJsonRPC.this.onError(code, message, data);
            }
        };
    }

    protected abstract void onError(int code, String message, String data);

    @Override
    public void onMessage(String message)
    {
        rpc.received(message);
    }

    public void request(String method, JsonRPC.ResponseHandler responseHandler, JSONObject params)
    {
        rpc.request(method, responseHandler, params);
    }

    public void request(String method, JsonRPC.ResponseHandler responseHandler, Object... params)
    {
        rpc.request(method, responseHandler, params);
    }

    public void rpc(String method, JSONObject params)
    {
        rpc.rpc(method, params);
    }

    public void rpc(String method, Object... params)
    {
        rpc.rpc(method, params);
    }

    public void addHandler(String method, JsonRPC.MethodHandler handler)
    {
        rpc.addHandler(method, handler);
    }
}
