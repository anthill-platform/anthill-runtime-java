package org.anthillplatform.runtime.services;

import org.anthillplatform.runtime.AnthillRuntime;
import org.anthillplatform.runtime.requests.JsonRequest;
import org.anthillplatform.runtime.requests.Request;
import org.json.JSONObject;

/**
 * Promo codes service for Anthill platform
 *
 * See https://github.com/anthill-platform/anthill-promo
 */
public class PromoService extends Service
{
    public static final String ID = "promo";
    public static final String API_VERSION = "0.2";

    /**
     * Please note that you should not create an instance of the service yourself,
     * and use AnthillRuntime.Get(PromoService.ID, PromoService.class) to get existing one instead
     */
    public PromoService(AnthillRuntime runtime, String location)
    {
        super(runtime, location, ID, API_VERSION);
    }

    public static PromoService Get()
    {
        return AnthillRuntime.Get(ID, PromoService.class);
    }

    public interface UsePromoCodeCallback
    {
        void complete(PromoService service, Request request, Request.Result result, JSONObject promo);
    }

    public void usePromoCode(
        LoginService.AccessToken accessToken,
        String promoCode,
        final UsePromoCodeCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getLocation() + "/use/" + promoCode,
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                if (result == Request.Result.success)
                {
                    callback.complete(PromoService.this, request, result, ((JsonRequest) request).getObject());
                } else
                {
                    callback.complete(PromoService.this, request, result, null);
                }
            }
        });

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.post(null);
    }
}
