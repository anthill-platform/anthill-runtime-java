package org.anthillplatform.runtime.util;

import org.anthillplatform.runtime.services.EnvironmentService;
import org.anthillplatform.runtime.services.LoginService;

public abstract class Listener
{
    public abstract void multipleAccountsAttached(
        LoginService service,
        LoginService.MergeOptions mergeOptions,
        LoginService.MergeResolveCallback resolve);

    public void servicesDiscovered(Runnable proceed)
    {
        proceed.run();
    }

    public void environmentVariablesReceived(EnvironmentService.EnvironmentInformation variables)
    {
        //
    }

    public void authenticated(String account, String credential, LoginService.Scopes scopes)
    {
        //
    }

    public boolean shouldHaveExternalAuthenticator()
    {
        return false;
    }

    public LoginService.ExternalAuthenticator createExternalAuthenticator()
    {
        return null;
    }

    public boolean shouldSaveExternalStorageAccessToken()
    {
        return true;
    }
}
