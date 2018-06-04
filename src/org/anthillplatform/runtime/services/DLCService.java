package org.anthillplatform.runtime.services;

import org.anthillplatform.runtime.AnthillRuntime;
import org.anthillplatform.runtime.requests.JsonRequest;
import org.anthillplatform.runtime.requests.Request;
import org.anthillplatform.runtime.util.ApplicationInfo;
import org.json.JSONObject;

import java.util.List;

/**
 * Downloadable content (DLC) management service for Anthill platform
 *
 * See https://github.com/anthill-platform/anthill-dlc
 */
public class DLCService extends Service
{
    public static final String ID = "dlc";
    public static final String API_VERSION = "0.2";

    public class Bundle
    {
        public String name;
        public String url;
        public String hash;
        public int size;
        public JSONObject payload;
    }

    public interface GetUpdatesCallback
    {
        void complete(DLCService service, Request request, Request.Result result);
    }

    /**
     * Please note that you should not create an instance of the service yourself,
     * and use AnthillRuntime.Get(DLCService.ID, DLCService.class) to get existing one instead
     */
    public DLCService(AnthillRuntime runtime, String location)
    {
        super(runtime, location, ID, API_VERSION);
    }

    public static DLCService Get()
    {
        return AnthillRuntime.Get(ID, DLCService.class);
    }

    public void getUpdates(final List<Bundle> bundlesOutput, final GetUpdatesCallback callback)
    {
        getUpdates(bundlesOutput, callback, null);
    }

    public void getUpdates(final List<Bundle> bundlesOutput, final GetUpdatesCallback callback, JSONObject env)
    {
        ApplicationInfo applicationInfo = AnthillRuntime.Get().getApplicationInfo();

        JsonRequest jsonRequest = new JsonRequest(
            getLocation() + "/data/" + applicationInfo.applicationName + "/" + applicationInfo.applicationVersion,
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                if (result == Request.Result.success)
                {
                    JSONObject object = ((JsonRequest) request).getObject();
                    JSONObject bundles_ = object.optJSONObject("bundles");

                    if (bundles_ != null)
                    {
                        for (Object fileName: bundles_.keySet())
                        {
                            Bundle bundle = new Bundle();

                            bundle.name = (String)fileName;

                            JSONObject bundle_ = ((JSONObject) bundles_.get(fileName.toString()));
                            bundle.url = bundle_.optString("url");
                            bundle.hash = bundle_.optString("hash");
                            bundle.size = bundle_.optInt("size", 0);
                            bundle.payload = bundle_.optJSONObject("payload");

                            bundlesOutput.add(bundle);
                        }
                    }

                    callback.complete(DLCService.this, request, result);
                }
                else
                {
                    callback.complete(DLCService.this, request, result);
                }
            }
        });

        if (env != null)
        {
            Request.Fields fields = new Request.Fields();

            fields.put("env", env.toString());

            jsonRequest.setQueryArguments(fields);
        }

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.get();
    }
}
