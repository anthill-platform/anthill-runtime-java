package org.anthillplatform.onlinelib.services;

import org.anthillplatform.onlinelib.OnlineLib;
import org.anthillplatform.onlinelib.Status;
import org.anthillplatform.onlinelib.request.JsonRequest;
import org.anthillplatform.onlinelib.request.Request;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

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

    public DiscoveryService(OnlineLib onlineLib)
    {
        super(onlineLib, null, null, API_VERSION);

        set(this);

        this.services = new HashMap<String, Service>();
    }

    public void setEnvironmentName(String environmentName)
    {
        this.environmentName = environmentName;
    }

    public void setService(String id, String location)
    {
        this.services.put(id, acquireService(id, location));
    }

    public static String join(String[] items)
    {
        StringBuilder sb = new StringBuilder();

        for (String item : items)
        {
            if (sb.length() > 0)
            {
                sb.append(",");
            }

            sb.append(item);
        }

        return sb.toString();
    }

    public void discoverServices(String[] ids, final DiscoveryCallback discoveryCallback)
    {
        String serviceIds = join(ids);

        JsonRequest request = new JsonRequest(getOnlineLib(),
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
                        } else
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
        OnlineLib.ServiceGen gen = OnlineLib.getGenerator(id);

        if (gen == null)
        {
            return new Service(getOnlineLib(), serviceLocation, id, null);
        }

        return gen.newService(getOnlineLib(), serviceLocation);
    }

    public String getEnvironmentName()
    {
        return environmentName;
    }
}
