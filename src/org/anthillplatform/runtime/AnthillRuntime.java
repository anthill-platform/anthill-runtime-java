package org.anthillplatform.runtime;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.options.Option;
import com.mashape.unirest.http.options.Options;
import org.anthillplatform.runtime.services.*;
import org.anthillplatform.runtime.util.ApplicationInfo;
import org.anthillplatform.runtime.util.Listener;
import org.anthillplatform.runtime.util.Storage;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Java Runtime for Anthill Platform
 * @author desertkun
 *
 * https://github.com/anthill-platform/anthill-runtime-java
 */
public class AnthillRuntime
{
    private static AnthillRuntime instance;

    private ApplicationInfo applicationInfo;
    private boolean initialized;

    private Map<String, Class> servicesClasses;
    private Map<String, Service> services;

    private Storage storage;
    private Listener listener;

    public static AnthillRuntime Get() { return instance; }

    public boolean isInitialized()
    {
        return initialized;
    }

    public static AnthillRuntime Create(
        String environmentLocation,
        ApplicationInfo applicationInfo,
        Storage storage,
        Listener listener)
    {
        if (instance != null)
            return instance;

        instance = new AnthillRuntime(
            environmentLocation,
            applicationInfo,
            storage,
            listener);

        return instance;
    }

    public static AnthillRuntime Create(
        String environmentLocation,
        ApplicationInfo applicationInfo)
    {
        return Create(environmentLocation, applicationInfo, null, null);
    }

    public static <T extends Service> T Get(String serviceId, Class<T> tClass)
    {
        AnthillRuntime instance = Get();

        if (instance == null)
        {
            throw new IllegalStateException("AnthillRuntime is not initialized");
        }

        return instance.get(serviceId, tClass);
    }

    public <T extends Service> T get(String serviceId, Class<T> tClass)
    {
        Service service = services.get(serviceId);

        if (service == null)
            return null;

        if (tClass.isInstance(service))
        {
            //noinspection unchecked
            return (T)service;
        }

        throw new NoSuchElementException("No such service " + serviceId + " registered, or it is being casted " +
                "to wrong class.");
    }

    private <T extends Service> void register(String serviceId, Class<T> tClass)
    {
        servicesClasses.put(serviceId, tClass);
    }

    private void registerServices()
    {
        register(DiscoveryService.ID, DiscoveryService.class);
        register(DLCService.ID, DLCService.class);
        register(EnvironmentService.ID, EnvironmentService.class);
        register(EventService.ID, EventService.class);
        register(GameService.ID, GameService.class);
        register(LeaderboardService.ID, LeaderboardService.class);
        register(LoginService.ID, LoginService.class);
        register(MessageService.ID, MessageService.class);
        register(ProfileService.ID, ProfileService.class);
        register(PromoService.ID, PromoService.class);
        register(ReportService.ID, ReportService.class);
        register(SocialService.ID, SocialService.class);
        register(StaticService.ID, StaticService.class);
        register(StoreService.ID, StoreService.class);
        register(BlogService.ID, BlogService.class);
    }

    public Service setService(String serviceId, String location)
    {
        Service existing = services.get(serviceId);

        if (existing != null)
            return existing;

        @SuppressWarnings("unchecked")
        Class<Service> tClass = (Class<Service>)servicesClasses.get(serviceId);

        Service newInstance;

        if (tClass == null)
        {
            // no class for this ID, then give up to generic "service"

            newInstance = new Service(this, location, location, "0.2");
        }
        else
        {
            Constructor<Service> constructor;

            try
            {
                constructor = tClass.getConstructor(AnthillRuntime.class, String.class);
            }
            catch (NoSuchMethodException e)
            {
                e.printStackTrace();
                throw new RuntimeException("Failed to acquire service constructor for service " + serviceId);
            }


            try
            {
                newInstance = constructor.newInstance(this, location);
            }
            catch (InstantiationException e)
            {
                e.printStackTrace();
                throw new RuntimeException("Failed to acquire instance for service " + serviceId);
            }
            catch (IllegalAccessException e)
            {
                e.printStackTrace();
                throw new RuntimeException("Failed to acquire instance for service " + serviceId);
            }
            catch (InvocationTargetException e)
            {
                e.printStackTrace();
                throw new RuntimeException("Failed to acquire instance for service " + serviceId);
            }
        }

        services.put(serviceId, newInstance);

        return newInstance;
    }

    private AnthillRuntime(
        String environmentLocation,
        ApplicationInfo applicationInfo,
        Storage storage,
        Listener listener)
    {
        this.applicationInfo = applicationInfo;
        this.initialized = false;
        this.services = new HashMap<String, Service>();
        this.servicesClasses = new HashMap<String, Class>();

        this.storage = storage;
        this.listener = listener;

        registerServices();

        setService(EnvironmentService.ID, environmentLocation);
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
