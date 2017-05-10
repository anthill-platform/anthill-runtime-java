package org.anthillplatform.onlinelib.entity;

public class ApplicationInfo
{
    private String environmentService;
    private String gameId;
    private String gameVersion;
    private String gamespace;

    public ApplicationInfo(String environmentService, String gameId, String gameVersion, String gamespace)
    {
        this.environmentService = environmentService;
        this.gameId = gameId;
        this.gameVersion = gameVersion;
        this.gamespace = gamespace;
    }

    public String getEnvironmentService()
    {
        return environmentService;
    }

    public String getGameId()
    {
        return gameId;
    }

    public String getGamespace()
    {
        return gamespace;
    }

    public String getGameVersion()
    {
        return gameVersion;
    }

    public void setEnvironmentService(String environmentService)
    {
        this.environmentService = environmentService;
    }

    public void setGameId(String gameId)
    {
        this.gameId = gameId;
    }

    public void setGamespace(String gamespace)
    {
        this.gamespace = gamespace;
    }

    public void setGameVersion(String gameVersion)
    {
        this.gameVersion = gameVersion;
    }
}
