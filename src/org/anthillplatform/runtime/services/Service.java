package org.anthillplatform.runtime.services;

import org.anthillplatform.runtime.AnthillRuntime;

public class Service
{
    private final String id;
    private final AnthillRuntime runtime;
    private String location;
    private String APIVersion;

    public Service(AnthillRuntime runtime, String location, String id, String APIVersion)
    {
        this.id = id;
        this.runtime = runtime;
        this.location = location;
        this.APIVersion = APIVersion;
    }

    public String getAPIVersion()
    {
        return APIVersion;
    }

    public void setAPIVersion(String APIVersion)
    {
        this.APIVersion = APIVersion;
    }

    public String getId()
    {
        return id;
    }

    public AnthillRuntime getRuntime()
    {
        return runtime;
    }

    public String getLocation()
    {
        return location;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }
}
