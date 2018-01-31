package org.anthillplatform.onlinelib.services;

import org.anthillplatform.onlinelib.OnlineLib;

public class Service
{
    private final String id;
    private final OnlineLib onlineLib;
    private String location;
    private String APIVersion;

    public Service(OnlineLib onlineLib, String location, String id, String APIVersion)
    {
        this.id = id;
        this.onlineLib = onlineLib;
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

    public OnlineLib getOnlineLib()
    {
        return onlineLib;
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
