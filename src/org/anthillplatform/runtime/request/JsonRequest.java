package org.anthillplatform.runtime.request;


import org.anthillplatform.runtime.AnthillRuntime;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonRequest extends Request
{
    private JSONObject object;

    public JsonRequest(AnthillRuntime runtime, String location, RequestResult requestResult)
    {
        super(location, requestResult);
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
