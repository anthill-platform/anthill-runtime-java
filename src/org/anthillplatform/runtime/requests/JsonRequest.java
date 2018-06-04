package org.anthillplatform.runtime.requests;


import org.json.JSONException;
import org.json.JSONObject;

public class JsonRequest extends Request
{
    private JSONObject object;

    public JsonRequest(String location, RequestCallback requestCallback)
    {
        super(location, requestCallback);
    }

    public void parse(String response)
    {
        if ("application/json".equals(getResponseContentType()))
        {
            try
            {
                this.object = new JSONObject(response);
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
        }
    }

    public JSONObject getObject()
    {
        return object;
    }
}
