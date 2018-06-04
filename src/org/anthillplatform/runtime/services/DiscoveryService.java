package org.anthillplatform.runtime.services;

import org.anthillplatform.runtime.AnthillRuntime;
import org.anthillplatform.runtime.requests.JsonRequest;
import org.anthillplatform.runtime.requests.Request;
import org.anthillplatform.runtime.util.Utils;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * A dynamic server discovery service for Anthill platform
 *
 * See https://github.com/anthill-platform/anthill-discovery
 */
public class DiscoveryService extends Service
{
    public static final String ID = "discovery";
    public static final String API_VERSION = "0.2";

    public interface DiscoveryInfoCallback
    {
        void complete(DiscoveryService service, Request request, Request.Result result,
                      Map<String, Service> discoveredServices);
    }

    /**
     * Please note that you should not create an instance of the service yourself,
     * and use AnthillRuntime.Get(DiscoveryService.ID, DiscoveryService.class) to get existing one instead
     */
    public DiscoveryService(AnthillRuntime runtime, String location)
    {
        super(runtime, location, ID, API_VERSION);
    }

    public static DiscoveryService Get()
    {
        return AnthillRuntime.Get(ID, DiscoveryService.class);
    }

    /**
     * Looks up services
     * @param services: an array of service's services too lookup
     * @param callback: the callback that would be called once the services have been found
     *                           (or not, depending on the status argument in the callback)
     */
    public void discoverServices(String[] services, final DiscoveryInfoCallback callback)
    {
        String serviceIds = Utils.join(services);

        JsonRequest request = new JsonRequest(
                getLocation() + "/services/" + serviceIds,
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                AnthillRuntime anthillRuntime = AnthillRuntime.Get();

                if (result == Request.Result.success)
                {
                    Map<String, Service> discoveredServices = new HashMap<String, Service>();

                    JSONObject services = ((JsonRequest) request).getObject();

                    for (Object key : services.keySet())
                    {
                        String id = key.toString();
                        String serviceLocation = services.getString(key.toString());

                        Service service = anthillRuntime.setService(id, serviceLocation);
                        discoveredServices.put(id, service);
                    }

                    callback.complete(DiscoveryService.this, request, result, discoveredServices);
                }
                else
                {
                    callback.complete(DiscoveryService.this, request, result, null);
                }
            }
        });

        request.setAPIVersion(getAPIVersion());

        request.get();
    }

}
