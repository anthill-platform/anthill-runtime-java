package org.anthillplatform.onlinelib.services;

import org.anthillplatform.onlinelib.OnlineLib;
import org.anthillplatform.onlinelib.Status;
import org.anthillplatform.onlinelib.entity.AccessToken;
import org.anthillplatform.onlinelib.request.JsonRequest;
import org.anthillplatform.onlinelib.request.Request;
import org.json.JSONObject;

public class PromoService extends Service
{
    public static final String ID = "promo";
    public static final String API_VERSION = "0.2";

    private static PromoService instance;
    public static PromoService get() { return instance; }
    private static void set(PromoService service) { instance = service; }

    public PromoService(OnlineLib onlineLib, String location)
    {
        super(onlineLib, location, ID, API_VERSION);

        set(this);
    }

    public interface PromoCallback
    {
        void complete(JSONObject promo, Status status);
    }

    public void usePromoCode(AccessToken accessToken, String promoCode, final PromoCallback profileCallback)
    {
        JsonRequest jsonRequest = new JsonRequest(getOnlineLib(), getLocation() + "/use/" + promoCode,
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
