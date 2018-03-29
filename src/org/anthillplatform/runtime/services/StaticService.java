package org.anthillplatform.runtime.services;

import org.anthillplatform.runtime.AnthillRuntime;
import org.anthillplatform.runtime.Status;
import org.anthillplatform.runtime.entity.AccessToken;
import org.anthillplatform.runtime.request.JsonRequest;
import org.anthillplatform.runtime.request.Request;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple static files hosting service (for players to upload)
 *
 * See https://github.com/anthill-platform/anthill-static
 */
public class StaticService extends Service
{
    public static final String ID = "static";
    public static final String API_VERSION = "0.2";

    private static StaticService instance;
    public static StaticService get() { return instance; }
    private static void set(StaticService service) { instance = service; }

    /**
     * Please note that you should not create an instance of the service yourself,
     * and use StaticService.get() to get existing one instead
     */
    public StaticService(AnthillRuntime runtime, String location)
    {
        super(runtime, location, ID, API_VERSION);

        set(this);
    }

    public interface FileUploadCallback
    {
        void complete(String url, Status status);
    }

    public void uploadFile(InputStream file, String fileName,
                           AccessToken accessToken, final FileUploadCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getRuntime(), getLocation() + "/upload",
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    callback.complete(response.optString("url"), Status.success);
                } else
                {
                    callback.complete(null, status);
                }
            }
        });

        Map<String, String> query = new HashMap<String, String>();
        query.put("filename", fileName);

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setQueryArguments(query);
        jsonRequest.setToken(accessToken);
        jsonRequest.put(file);
    }
}
