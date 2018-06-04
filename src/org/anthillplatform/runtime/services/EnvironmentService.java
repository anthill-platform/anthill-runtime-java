package org.anthillplatform.runtime.services;

import org.anthillplatform.runtime.AnthillRuntime;
import org.anthillplatform.runtime.requests.JsonRequest;
import org.anthillplatform.runtime.requests.Request;
import org.anthillplatform.runtime.util.ApplicationInfo;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * An application environment service for Anthill platform
 *
 * See https://github.com/anthill-platform/anthill-environment
 */
public class EnvironmentService extends Service
{
    public static final String ID = "environment";
    public static final String API_VERSION = "0.2";
    public static class EnvironmentInformation extends HashMap<String, Object> {}

    private EnvironmentInformation environmentVariables;

    public EnvironmentService(AnthillRuntime runtime, String location)
    {
        super(runtime, location, ID, API_VERSION);

        environmentVariables = new EnvironmentInformation();
    }

    public static EnvironmentService Get()
    {
        return AnthillRuntime.Get(ID, EnvironmentService.class);
    }

    public interface EnvironmentInfoCallback
    {
        void complete(
                EnvironmentService service, Request request, Request.Result result,
                String discoveryLocation, EnvironmentInformation environmentInformation);
    }

    public void getEnvironmentInfo(final EnvironmentInfoCallback callback)
    {
        ApplicationInfo applicationInfo = getRuntime().getApplicationInfo();

        JsonRequest request = new JsonRequest(getLocation() + "/" + applicationInfo.applicationName +
            "/" + applicationInfo.applicationVersion, new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                AnthillRuntime anthillRuntime = AnthillRuntime.Get();

                if (result == Request.Result.success)
                {
                    JSONObject object = ((JsonRequest) request).getObject();

                    environmentVariables.clear();

                    for (String key : object.keySet())
                    {
                        environmentVariables.put(key, object.get(key));
                    }

                    try
                    {
                        String discoveryServiceLocation = ((String) object.get("discovery"));

                        anthillRuntime.setService(DiscoveryService.ID, discoveryServiceLocation);

                        callback.complete(EnvironmentService.this, request, result,
                                discoveryServiceLocation, environmentVariables);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();

                        callback.complete(EnvironmentService.this, request, result, null, environmentVariables);
                    }
                }
                else
                {
                    callback.complete(EnvironmentService.this, request, result, null, null);
                }
            }
        });

        request.setAPIVersion(getAPIVersion());
        request.get();
    }

    @SuppressWarnings("unchecked")
    public <T> T variable(String name, Class<? extends T> clazz)
    {
        return (T)environmentVariables.get(name);
    }

    @SuppressWarnings("unchecked")
    public <T> T variable(String name, T def, Class<? extends T> clazz)
    {
        if (environmentVariables.containsKey(name))
        {
            return (T)environmentVariables.get(name);
        }

        return def;
    }

    public HashMap<String, Object> getEnvironmentVariables()
    {
        return environmentVariables;
    }
}
