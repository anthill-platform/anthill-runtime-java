package org.anthillplatform.runtime.services;

import org.anthillplatform.runtime.AnthillRuntime;
import org.anthillplatform.runtime.Status;
import org.anthillplatform.runtime.entity.AccessToken;
import org.anthillplatform.runtime.request.JsonRequest;
import org.anthillplatform.runtime.request.Request;
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

    private static PromoService instance;
    public static PromoService get() { return instance; }
    private static void set(PromoService service) { instance = service; }

    /**
     * Please note that you should not create an instance of the service yourself,
     * and use PromoService.get() to get existing one instead
     */
    public PromoService(AnthillRuntime runtime, String location)
    {
        super(runtime, location, ID, API_VERSION);

        set(this);
    }

    public interface PromoCallback
    {
        void complete(JSONObject promo, Status status);
    }

    public void usePromoCode(AccessToken accessToken, String promoCode, final PromoCallback profileCallback)
    {
        JsonRequest jsonRequest = new JsonRequest(getRuntime(), getLocation() + "/use/" + promoCode,
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    profileCallback.complete(((JsonRequest) request).getObject(), Status.success);
                } else
                {
                    profileCallback.complete(null, status);
                }
            }
        });

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.post(null);
    }
}
