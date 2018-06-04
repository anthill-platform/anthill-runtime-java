package org.anthillplatform.runtime.services;

import org.anthillplatform.runtime.AnthillRuntime;
import org.anthillplatform.runtime.requests.JsonRequest;
import org.anthillplatform.runtime.requests.Request;
import org.json.JSONObject;

import java.io.InputStream;

/**
 * Simple static files hosting service (for players to upload)
 *
 * See https://github.com/anthill-platform/anthill-static
 */
public class StaticService extends Service
{
    public static final String ID = "static";
    public static final String API_VERSION = "0.2";

    /**
     * Please note that you should not create an instance of the service yourself,
     * and use AnthillRuntime.Get(StaticService.ID, StaticService.class) to get existing one instead
     */
    public StaticService(AnthillRuntime runtime, String location)
    {
        super(runtime, location, ID, API_VERSION);
    }

    public static StaticService Get()
    {
        return AnthillRuntime.Get(ID, StaticService.class);
    }

    public interface ReportUploadCallback
    {
        void complete(StaticService service, Request request, Request.Result result, String url);
    }

    public void upload(
        LoginService.AccessToken accessToken,
        InputStream stream, String fileName,
        final ReportUploadCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getLocation() + "/upload",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                if (result == Request.Result.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    callback.complete(StaticService.this, request, result, response.optString("url"));
                }
                else
                {
                    callback.complete(StaticService.this, request, result, null);
                }
            }
        });

        Request.Fields query = new Request.Fields();
        query.put("filename", fileName);

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setQueryArguments(query);
        jsonRequest.setToken(accessToken);
        jsonRequest.put(stream);
    }
}
