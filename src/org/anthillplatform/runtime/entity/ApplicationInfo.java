package org.anthillplatform.runtime.entity;

/**
 * A class that represents information about your application
 */
public class ApplicationInfo
{
    /**
     * A link to the environment service
     *
     * See https://github.com/anthill-platform/anthill-environment#environment-service
     */
    private String environmentService;

    /**
     * gameName and gameVersion identify the application in the environment service
     */
    private String gameName;
    private String gameVersion;

    /**
     * An gamespace alias name
     *
     * See https://github.com/anthill-platform/anthill-login#gamespace
     */
    private String gamespace;

    public ApplicationInfo(String environmentService, String gameName, String gameVersion, String gamespace)
    {
        this.environmentService = environmentService;
        this.gameName = gameName;
        this.gameVersion = gameVersion;
        this.gamespace = gamespace;
    }

    public String getEnvironmentService()
    {
        return environmentService;
    }

    public String getGameName()
    {
        return gameName;
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

    public void setGameName(String gameName)
    {
        this.gameName = gameName;
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
