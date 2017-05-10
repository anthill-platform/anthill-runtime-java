package org.anthillplatform.onlinelib;

import org.anthillplatform.onlinelib.entity.ApplicationInfo;
import org.anthillplatform.onlinelib.request.JsonRequest;
import com.mashape.unirest.http.Unirest;
import org.anthillplatform.onlinelib.request.Request;
import org.anthillplatform.onlinelib.services.*;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class OnlineLib
{
    private static OnlineLib instance;
    public static OnlineLib get() { return instance; }
    private static void set(OnlineLib service) { instance = service; }

    private final HashMap<String, Object> environmentVariables;

    private ApplicationInfo applicationInfo;
    private boolean initialized;
    private DiscoveryService discoveryService;

    private static Map<String, ServiceGen> generators = new HashMap<String, ServiceGen>();

    static
    {
        generators.put(GameService.ID, new ServiceGen()
        {
            @Override
            public Service newService(OnlineLib lib, String serviceLocation)
            {
                return new GameService(lib, serviceLocation);
            }
        });

        generators.put(DLCService.ID, new ServiceGen()
        {
            @Override
            public Service newService(OnlineLib lib, String serviceLocation)
            {
                return new DLCService(lib, serviceLocation);
            }
        });

        generators.put(LoginService.ID, new ServiceGen()
        {
            @Override
            public Service newService(OnlineLib lib, String serviceLocation)
            {
                return new LoginService(lib, serviceLocation);
            }
        });

        generators.put(ProfileService.ID, new ServiceGen()
        {
            @Override
            public Service newService(OnlineLib lib, String serviceLocation)
            {
                return new ProfileService(lib, serviceLocation);
            }
        });

        generators.put(LeaderboardService.ID, new ServiceGen()
        {
            @Override
            public Service newService(OnlineLib lib, String serviceLocation)
            {
                return new LeaderboardService(lib, serviceLocation);
            }
        });

        generators.put(PromoService.ID, new ServiceGen()
        {
            @Override
            public Service newService(OnlineLib lib, String serviceLocation)
            {
                return new PromoService(lib, serviceLocation);
            }
        });

        generators.put(EventService.ID, new ServiceGen()
        {
            @Override
            public Service newService(OnlineLib lib, String serviceLocation)
            {
                return new EventService(lib, serviceLocation);
            }
        });

        generators.put(MessageService.ID, new ServiceGen()
        {
            @Override
            public Service newService(OnlineLib lib, String serviceLocation)
            {
                return new MessageService(lib, serviceLocation);
            }
        });

        generators.put(StoreService.ID, new ServiceGen()
        {
            @Override
            public Service newService(OnlineLib lib, String serviceLocation)
            {
                return new StoreService(lib, serviceLocation);
            }
        });
    }

    public static ServiceGen getGenerator(String serviceId)
    {
        return generators.get(serviceId);
    }

    public boolean isInitialized()
    {
        return initialized;
    }

    public DiscoveryService getDiscoveryService()
    {
        return discoveryService;
    }

    public interface ServiceGen
    {
        Service newService(OnlineLib lib, String serviceLocation);
    }

    public interface Callback
    {
        void complete(OnlineLib lib, Status status);
    }

    public OnlineLib(ApplicationInfo applicationInfo)
    {
        set(this);

        Unirest.setTimeouts(60000, 60000);

        this.applicationInfo = applicationInfo;

        this.environmentVariables = new HashMap<String, Object>();

        this.discoveryService = new DiscoveryService(this);
        this.initialized = false;
    }

    public void init(Callback complete)
    {
        if (initialized)
        {
            complete.complete(this, Status.success);
        }
        else
        {
            getEnvironmentInfo(complete);
        }
    }

    private void getEnvironmentInfo(final Callback complete)
    {
        JsonRequest request = new JsonRequest(this,
            applicationInfo.getEnvironmentService() + "/" + applicationInfo.getGameId() + "/" +
                applicationInfo.getGameVersion(),
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject object = ((JsonRequest) request).getObject();

                    environmentVariables.clear();
                    for (Object key : object.keySet())
                    {
                        environmentVariables.put(key.toString(), object.get(key.toString()));
                    }

                    try
                    {
                        String environmentName = object.optString("name", "");
                        String discoveryService1 = ((String) object.get("discovery"));

                        setEnvironmentInfo(discoveryService1, environmentName);
                        complete.complete(OnlineLib.this, Status.success);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        complete.complete(OnlineLib.this, Status.dataCorrupted);
                    }
                }
                else
                {
                    complete.complete(OnlineLib.this, status);
                }
            }
        });

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

    public void setEnvironmentInfo(String discoveryService, String environmentName)
    {
        initDiscovery(discoveryService, environmentName);
    }

    private void initDiscovery(String discoveryService, String environmentName)
    {
        this.initialized = true;

        this.discoveryService.setLocation(discoveryService);
        this.discoveryService.setEnvironmentName(environmentName);
    }

    public void release()
    {
        try
        {
            Unirest.shutdown();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public ApplicationInfo getApplicationInfo()
    {
        return applicationInfo;
    }
}
