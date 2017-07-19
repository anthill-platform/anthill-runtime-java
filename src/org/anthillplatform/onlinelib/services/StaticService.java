package org.anthillplatform.onlinelib.services;

import org.anthillplatform.onlinelib.OnlineLib;
import org.anthillplatform.onlinelib.Status;
import org.anthillplatform.onlinelib.entity.AccessToken;
import org.anthillplatform.onlinelib.request.JsonRequest;
import org.anthillplatform.onlinelib.request.Request;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class StaticService extends Service
{
    public static final String ID = "static";

    private static StaticService instance;
    public static StaticService get() { return instance; }
    private static void set(StaticService service) { instance = service; }

    public StaticService(OnlineLib onlineLib, String location)
    {
        super(onlineLib, location, ID);

        set(this);
    }

    public interface FileUploadCallback
    {
        void complete(String url, Status status);
    }

    public void uploadFile(InputStream file, String fileName,
                           AccessToken accessToken, final FileUploadCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getOnlineLib(), getLocation() + "/upload",
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

        jsonRequest.setQueryArguments(query);
        jsonRequest.setToken(accessToken);
        jsonRequest.put(file);
    }
}
