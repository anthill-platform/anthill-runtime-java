package org.anthillplatform.runtime.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public abstract class JsonRPC
{
    private static final ResponseHandler NoResponse = new ResponseHandler()
    {
        public void success(Object response) {}
        public void error(int code, String message, String data) {}
    };

    private HashMap<String, MethodHandler> handlers;
    private HashMap<Integer, ResponseHandler> responseHandlers;
    private int nextId = 1;

    public interface MethodHandler
    {
        Object called(Object params) throws JsonRPCException;
    }

    public interface ResponseHandler
    {
        void success(Object response);
        void error(int code, String message, String data);
    }

    protected abstract void send(String data);
    public abstract void onError(int code, String message, String data);

    public JsonRPC()
    {
        this.handlers = new HashMap<String, MethodHandler>();
        this.responseHandlers = new HashMap<Integer, ResponseHandler>();
    }

    public class JsonRPCException extends Exception
    {
        public int code;
        public String message;
        public String data;

        public JsonRPCException(int code, String message, String data)
        {
            this.code = code;
            this.message = message;
            this.data = data;
        }

        public JsonRPCException(int code, String message)
        {
            this(code, message, null);
        }
    }

    public void addHandler(String method, MethodHandler handler)
    {
        handlers.put(method, handler);
    }

    private JSONObject serializeError(int code, String message, String data)
    {
        JSONObject error = new JSONObject();

        error.put("code", code);
        error.put("message", message);

        if (data != null)
        {
            error.put("data", data);
        }

        return error;
    }

    private void writeError(int code, String message)
    {
        writeError(code, message, null);
    }

    private void writeError(int code, String message, String data)
    {
        writeError(code, message, data, -1);
    }

    private void writeError(int code, String message, String data, int id)
    {
        JSONObject toWrite = new JSONObject();

        toWrite.put("jsonrpc", "2.0");
        toWrite.put("error", serializeError(code, message, data));

        if (id >= 0)
        {
            toWrite.put("id", id);
        }

        send(toWrite.toString());
    }

    private void writeResponse(Object result)
    {
        writeResponse(result, -1);
    }

    private void writeResponse(Object result, int id)
    {
        JSONObject toWrite = new JSONObject();

        toWrite.put("jsonrpc", "2.0");
        toWrite.put("result", result);

        if (id >= 0)
        {
            toWrite.put("id", id);
        }

        send(toWrite.toString());
    }

    public void received(String message)
    {
        JSONObject msg;

        try
        {
            msg = new JSONObject(message);
        }
        catch (JSONException e)
        {
            writeError(-32700, "Parse error");
            return;
        }

        if (!msg.has("jsonrpc"))
        {
            writeError(-32600, "Invalid Request", "No 'jsonrpc' field.");
            return;
        }

        if (!msg.getString("jsonrpc").equals("2.0"))
        {
            writeError(-32600, "Bad version of 'jsonrpc': " + msg.getString("jsonrpc") + ".");
            return;
        }

        boolean hasId = msg.has("id") && msg.optInt("id") > 0;
        boolean hasMethod = msg.has("method") && !msg.optString("method").isEmpty();
        boolean hasParams = msg.has("params");
        boolean hasResult = msg.has("result");
        boolean hasError = msg.has("error");

        Object params = hasParams ? msg.get("params") : null;
        String method = hasMethod ? msg.getString("method") : null;
        int id = hasId ? msg.getInt("id") : 0;
        JSONObject error = hasError ? msg.getJSONObject("error") : null;
        Object result = hasResult ? msg.get("result") : null;

        if (hasId && hasMethod)
        {
            // a request
            if (handlers.containsKey(method))
            {
                // call a request
                Object response;

                try
                {
                    response = handlers.get(method).called(params);
                }
                catch (JsonRPCException e)
                {
                    writeError(e.code, e.message, e.data, id);
                    return;
                }

                if (response != null)
                {
                    // if a result of callback is a deffered object, then handle it asynchronously
                    writeResponse(response, id);
                }
                else
                {
                    writeError(-32603, "Internal error", "Response cannot be null", id);
                }
            }
            else
            {
                writeError(-32601, "Method not found");
            }
        }
        else if (hasId)
        {
            if (hasError == hasResult)
            {
                writeError(-32600, "Invalid Request", "Should be (only) one 'result' or 'error' field.");
                return;
            }

            // a success

            if (responseHandlers.containsKey(id))
            {
                ResponseHandler handler = responseHandlers.get(id);
                responseHandlers.remove(id);

                if (hasResult)
                {
                    handler.success(result);
                }
                else
                {
                    if (error.has("code") &&
                            error.has("message"))
                    {
                        int responseCode = error.getInt("code");
                        String responseMessage = error.getString("message");
                        String responseData = error.has("data") ? error.getString("data") : null;

                        // hasError
                        handler.error(responseCode, responseMessage, responseData);
                    }
                    else
                    {
                        writeError(-32600, "Invalid Request", "Bad 'error' field.");
                    }
                }
            }
            else
            {
                writeError(-32600, "Invalid Request", "No such handler.", id);
            }
        }
        else if (hasMethod)
        {
            // an rpc
            if (handlers.containsKey(method))
            {
                try
                {
                    handlers.get(method).called(params);
                }
                catch (JsonRPCException ignored)
                {
                    //
                }
            }
        }
        else if (hasError)
        {
            if (error.has("code") &&
                error.has("message"))
            {
                int code = error.getInt("code");
                String errorMessage = error.getString("message");
                String data = error.has("data") ? error.getString("data") : null;

                // hasError
                onError(code, errorMessage, data);
            }
            else
            {
                writeError(-32600, "Invalid Request", "Bad 'error' field.");
            }
        }
        else
        {
            writeError(-32600, "Invalid Request", "No 'method' nor 'id' field.");
        }
    }

    public void request(String method, ResponseHandler responseHandler, JSONObject params)
    {
        JSONObject toWrite = new JSONObject();

        toWrite.put("jsonrpc", "2.0");
        toWrite.put("method", method);
        toWrite.put("id", nextId);
        toWrite.put("params", params);

        if (responseHandler == null)
            responseHandler = NoResponse;

        responseHandlers.put(nextId, responseHandler);
        nextId++;

        send(toWrite.toString());
    }

    public void request(String method, ResponseHandler responseHandler, Object... params)
    {
        JSONObject toWrite = new JSONObject();

        toWrite.put("jsonrpc", "2.0");
        toWrite.put("method", method);

        JSONArray p = new JSONArray();

        for (Object param : params)
        {
            p.put(param);
        }

        toWrite.put("id", nextId);
        toWrite.put("params", p);

        if (responseHandler == null)
            responseHandler = NoResponse;

        responseHandlers.put(nextId, responseHandler);
        nextId++;

        send(toWrite.toString());
    }

    public void rpc(String method, JSONObject params)
    {
        JSONObject toWrite = new JSONObject();

        toWrite.put("jsonrpc", "2.0");
        toWrite.put("method", method);
        toWrite.put("params", params);

        send(toWrite.toString());
    }

    public void rpc(String method, Object... params)
    {
        JSONObject toWrite = new JSONObject();

        toWrite.put("jsonrpc", "2.0");
        toWrite.put("method", method);

        JSONArray p = new JSONArray();

        for (Object param : params)
        {
            p.put(param);
        }

        toWrite.put("params", p);

        send(toWrite.toString());
    }
}
