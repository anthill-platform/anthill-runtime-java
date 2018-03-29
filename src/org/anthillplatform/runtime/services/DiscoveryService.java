package org.anthillplatform.runtime.services;

import org.anthillplatform.runtime.AnthillRuntime;
import org.anthillplatform.runtime.Status;
import org.anthillplatform.runtime.request.JsonRequest;
import org.anthillplatform.runtime.request.Request;
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
    private String environmentName;
    private Map<String, Service> services;

    public static final String ID = "discovery";
    public static final String API_VERSION = "0.2";

    private static DiscoveryService instance;
    public static DiscoveryService get() { return instance; }
    private static void set(DiscoveryService service) { instance = service; }

    public interface DiscoveryCallback
    {
        void complete(Status status);
    }

    /**
     * Please note that you should not create an instance of the service yourself,
     * and use DiscoveryService.get() to get existing one instead
     */
    public DiscoveryService(AnthillRuntime runtime)
    {
        super(runtime, null, null, API_VERSION);

        set(this);

        this.services = new HashMap<String, Service>();
    }

    public void setEnvironmentName(String environmentName)
    {
        this.environmentName = environmentName;
    }

    /**
     * Setups known service into the discovery database
     *
     * @param id: the 'id' of the service
     * @param location: a known http(s)://root URI of the service
     */
    public void setService(String id, String location)
    {
        this.services.put(id, acquireService(id, location));
    }

    /**
     * Looks up services
     * @param ids: an array of service's ids too lookup
     * @param discoveryCallback: the callback that would be called once the services have been found
     *                           (or not, depending on the status argument in the callback)
     */
    public void discoverServices(String[] ids, final DiscoveryCallback discoveryCallback)
    {
        String serviceIds = Utils.join(ids);

        JsonRequest request = new JsonRequest(getRuntime(),
            getLocation() + "/services/" + serviceIds,
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request1, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject services = ((JsonRequest) request1).getObject();

                    for (Object key : services.keySet())
                    {
                        String id = key.toString();
                        String serviceLocation = services.getString(key.toString());

                        Service service1 = DiscoveryService.this.acquireService(id, serviceLocation);

                        if (service1 != null)
                        {
                            DiscoveryService.this.setService(id, serviceLocation);
                        }
                        else
                        {
                            discoveryCallback.complete(Status.cannotAcquireService);
                            return;
                        }
                    }

                    discoveryCallback.complete(Status.success);
                }
                else
                {
                    discoveryCallback.complete(status);
                }
            }
        });

        request.setAPIVersion(getAPIVersion());

        request.get();
    }

    private Service acquireService(String id, String serviceLocation)
    {
        AnthillRuntime.ServiceGen gen = AnthillRuntime.getGenerator(id);

        if (gen == null)
        {
            return new Service(getRuntime(), serviceLocation, id, null);
        }

        return gen.newService(getRuntime(), serviceLocation);
    }

    public String getEnvironmentName()
    {
        return environmentName;
    }
}
