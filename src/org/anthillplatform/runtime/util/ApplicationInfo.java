package org.anthillplatform.runtime.util;

import org.anthillplatform.runtime.services.LoginService;

/**
 * A class that represents information about your application
 */
public class ApplicationInfo
{
    public String gamespace;
    public String applicationName;
    public String applicationVersion;
    public LoginService.Scopes requiredScopes;
    public LoginService.Scopes shouldHaveScopes;

    public ApplicationInfo(String gamespace, String applicationName, String applicationVersion)
    {
        this(gamespace, applicationName, applicationVersion, new LoginService.Scopes(), new LoginService.Scopes("*"));
    }

    public ApplicationInfo(
        String gamespace, String applicationName, String applicationVersion,
        LoginService.Scopes requiredScopes)
    {
        this(gamespace, applicationName, applicationVersion, requiredScopes, new LoginService.Scopes("*"));
    }

    public ApplicationInfo(
            String gamespace, String applicationName, String applicationVersion,
            LoginService.Scopes requiredScopes, LoginService.Scopes shouldHaveScopes)
    {
        this.gamespace = gamespace;
        this.applicationName = applicationName;
        this.applicationVersion = applicationVersion;
        this.requiredScopes = requiredScopes;
        this.shouldHaveScopes = shouldHaveScopes;
    }
}
