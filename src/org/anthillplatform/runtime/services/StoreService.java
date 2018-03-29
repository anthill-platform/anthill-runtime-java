package org.anthillplatform.runtime.services;

import org.anthillplatform.runtime.request.JsonRequest;
import org.anthillplatform.runtime.AnthillRuntime;
import org.anthillplatform.runtime.Status;
import org.anthillplatform.runtime.entity.AccessToken;
import org.anthillplatform.runtime.request.Request;
import org.anthillplatform.runtime.util.Utils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Monetization service for Anthill platform
 *
 * See https://github.com/anthill-platform/anthill-store
 */
public class StoreService extends Service
{
    public static final String ID = "store";
    public static final String API_VERSION = "0.2";

    private static StoreService instance;
    public static StoreService get() { return instance; }
    private static void set(StoreService service) { instance = service; }

    public class Store
    {
        private String name;
        private List<Item> items;
        private Map<String, Tier> tiers;
        private List<Campaign> campaigns;

        public class Item
        {
            private String category;
            private Billing billing;
            private String id;
            private Map<String, Integer> contents;
            private JSONObject publicPayload;

            public class Billing
            {
                private Tier tier;

                public void parse(JSONObject data)
                {
                    String tierName = data.optString("tier", "");
                    this.tier = tiers.get(tierName);
                }

                public Tier getTier()
                {
                    return tier;
                }
            }

            public Item()
            {
                this.contents = new HashMap<String, Integer>();
            }

            public Campaign.CampaignItem getCampaignItem()
            {
                for (Campaign campaign : getCampaigns())
                {
                    Campaign.CampaignItem campaignItem = campaign.getItems().get(id);

                    if (campaignItem != null)
                        return campaignItem;
                }

                return null;
            }

            public void parse(JSONObject data)
            {
                this.id = data.getString("id");
                this.category = data.optString("category");
                this.publicPayload = data.optJSONObject("public");

                JSONObject billing = data.optJSONObject("billing");

                if (billing != null)
                {
                    this.billing = new Billing();
                    this.billing.parse(billing);
                }

                JSONObject contents = data.optJSONObject("contents");

                if (contents != null)
                {
                    for (Object key: contents.keySet())
                    {
                        String id = key.toString();

                        this.contents.put(id, contents.getInt(id));
                    }
                }
            }

            public Billing getBilling()
            {
                return billing;
            }

            public String getCategory()
            {
                return category;
            }

            public Map<String, Integer> getContents()
            {
                return contents;
            }

            public JSONObject getPublicPayload()
            {
                return publicPayload;
            }

            public String getId()
            {
                return id;
            }

            public Store getStore()
            {
                return Store.this;
            }
        }

        public class Tier
        {
            private String id;
            private Map<String, Price> prices;
            private String product;

            public class Price
            {
                private String currency;
                private String format;
                private int price;
                private String title;
                private String symbol;
                private String label;

                public Price(String currency)
                {
                    this.currency = currency;
                }

                public void parse(JSONObject data)
                {
                    this.format = data.optString("format", "Unknown");
                    this.price = data.optInt("price", 0);
                    this.title = data.optString("title", "Unknown");
                    this.symbol = data.optString("symbol", "Unknown");
                    this.label = data.optString("label", "Unknown");
                }

                public String getFormat()
                {
                    return format;
                }

                public int getPrice()
                {
                    return price;
                }

                public String getTitle()
                {
                    return title;
                }

                public String getSymbol()
                {
                    return symbol;
                }

                public String getLabel()
                {
                    return label;
                }

                public String getCurrency()
                {
                    return currency;
                }
            }

            public Tier(String id)
            {
                this.id = id;
                this.prices = new HashMap<String, Price>();
            }

            public String getId()
            {
                return id;
            }

            public Map<String, Price> getPrices()
            {
                return prices;
            }

            public String getProduct()
            {
                return product;
            }

            public void parse(JSONObject data)
            {
                JSONObject pricesItems = data.optJSONObject("prices");

                if (pricesItems != null)
                {
                    for (Object key: pricesItems.keySet())
                    {
                        String id = key.toString();
                        JSONObject child = pricesItems.optJSONObject(id);

                        if (child != null)
                        {
                            Price price = new Price(id);
                            price.parse(child);
                            this.prices.put(id, price);
                        }
                    }
                }

                this.product = data.optString("product", "Unknown");
            }


        }

        public class Campaign
        {
            private JSONObject payload;
            private Date timeStart;
            private Date timeEnd;
            private Map<String, CampaignItem> items;

            public class CampaignItem
            {
                private Tier updatedTier;
                private JSONObject updatedPublicPayload;

                public CampaignItem()
                {

                }

                public boolean parse(JSONObject data)
                {
                    String updatedTier = data.optString("tier", null);

                    if (updatedTier == null)
                        return false;

                    this.updatedTier = getTiers().get(updatedTier);
                    this.updatedPublicPayload = data.optJSONObject("public");

                    return true;
                }

                public JSONObject getUpdatedPublicPayload()
                {
                    return updatedPublicPayload;
                }

                public Tier getUpdatedTier()
                {
                    return updatedTier;
                }
            }

            public Campaign()
            {
                items = new HashMap<String, CampaignItem>();
            }

            public boolean parse(JSONObject data)
            {
                payload = data.optJSONObject("payload");

                JSONObject time = data.optJSONObject("time");

                if (time != null)
                {
                    try
                    {
                        SimpleDateFormat format = getTimeFormat();

                        timeStart = format.parse(time.getString("start"));
                        timeEnd = format.parse(time.getString("end"));
                    }
                    catch (Exception ignored)
                    {
                        return false;
                    }
                }
                else
                {
                    return false;
                }

                JSONObject items = data.optJSONObject("items");

                if (items != null)
                {
                    for (Object key: items.keySet())
                    {
                        String id = key.toString();
                        JSONObject child = items.optJSONObject(id);

                        if (child != null)
                        {
                            CampaignItem item = new CampaignItem();
                            if (!item.parse(child))
                                continue;
                            this.items.put(id, item);
                        }
                    }
                }

                return true;
            }

            public Map<String, CampaignItem> getItems()
            {
                return items;
            }

            public JSONObject getPayload()
            {
                return payload;
            }


        }

        public Store(String name)
        {
            this.name = name;
            this.items = new LinkedList<Item>();
            this.tiers = new HashMap<String, Tier>();
            this.campaigns = new LinkedList<Campaign>();
        }

        public List<Item> getItems()
        {
            return items;
        }

        public List<Campaign> getCampaigns()
        {
            return campaigns;
        }

        public Map<String, Tier> getTiers()
        {
            return tiers;
        }

        public String getName()
        {
            return name;
        }

        public void parse(JSONObject data)
        {
            JSONObject store = data.optJSONObject("store");

            if (store == null)
                return;

            JSONObject tierItems = store.optJSONObject("tiers");

            if (tierItems != null)
            {
                for (Object key: tierItems.keySet())
                {
                    String id = key.toString();
                    JSONObject child = tierItems.optJSONObject(id);

                    if (child != null)
                    {
                        Tier tier = new Tier(id);
                        tier.parse(child);
                        this.tiers.put(id, tier);
                    }
                }
            }

            JSONArray dataItems = store.optJSONArray("items");

            if (dataItems != null)
            {
                for (int i = 0, t = dataItems.length(); i < t; i++)
                {
                    JSONObject child = dataItems.optJSONObject(i);

                    if (child != null)
                    {
                        Item item = new Item();
                        item.parse(child);
                        this.items.add(item);
                    }
                }
            }

            JSONArray campaigns = store.optJSONArray("campaigns");

            if (campaigns != null)
            {
                for (int i = 0, t = campaigns.length(); i < t; i++)
                {
                    JSONObject child = campaigns.optJSONObject(i);

                    if (child != null)
                    {
                        Campaign campaign = new Campaign();
                        if (!campaign.parse(child))
                            continue;
                        this.campaigns.add(campaign);
                    }
                }
            }
        }
    }

    /**
     * Please note that you should not create an instance of the service yourself,
     * and use StoreService.get() to get existing one instead
     */
    public StoreService(AnthillRuntime runtime, String location)
    {
        super(runtime, location, ID, API_VERSION);

        set(this);
    }

    public interface GetStoreCallback
    {
        void complete(Store store, Status status);
    }

    public interface NewOrderCallback
    {
        void complete(long orderId, Status status);
    }

    public interface UpdateOrderCallback
    {
        void complete(String store, long orderId, String currency,
                      int total, JSONObject publicPayload, JSONObject privatePayload, int amount,
                      String item, Status status);
    }


    public interface UpdateOrdersCallback
    {
        void updated(String store, long orderId, String currency,
                     int total, JSONObject publicPayload, JSONObject privatePayload, int amount,
                     String item, Status status);
    }

    public void getStore(AccessToken accessToken, final String name, final GetStoreCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getRuntime(), getLocation() + "/store/" + name,
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    Store store = new Store(name);
                    store.parse(((JsonRequest) request).getObject());
                    callback.complete(store, Status.success);
                } else
                {
                    callback.complete(null, status);
                }
            }
        });

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.get();
    }

    public void updateOrders(
            AccessToken accessToken,
            final UpdateOrdersCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getRuntime(), getLocation() + "/orders",
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject result = ((JsonRequest) request).getObject();

                    if (result != null)
                    {
                        JSONArray orders = result.optJSONArray("orders");

                        for (int i = 0; i < orders.length(); i++)
                        {
                            JSONObject order = orders.optJSONObject(i);

                            JSONObject publicPayload = order.optJSONObject("public");
                            JSONObject privatePayload = order.optJSONObject("private");

                            int amount = order.optInt("amount", 1);
                            long responseOrderId = order.optLong("order_id", -1);
                            int total = order.optInt("total", 0);
                            String currency = order.optString("currency", "");
                            String store = order.optString("store", "");
                            String item = order.optString("item", "");

                            callback.updated(store, responseOrderId, currency, total,
                                    publicPayload, privatePayload, amount, item, Status.success);
                        }
                    }
                }
            }
        });

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.post(null);
    }

    public void updateOrder(
        AccessToken accessToken,
        final long orderId,
        final UpdateOrderCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getRuntime(), getLocation() + "/order/" +
            String.valueOf(orderId),
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject result = ((JsonRequest) request).getObject();

                    JSONObject publicPayload = result.optJSONObject("public");
                    JSONObject privatePayload = result.optJSONObject("private");

                    int amount = result.optInt("amount", 1);
                    long responseOrderId = result.optLong("order_id", -1);
                    int total = result.optInt("total", 0);
                    String currency = result.optString("currency", "");
                    String store = result.optString("store", "");
                    String item = result.optString("item", "");

                    callback.complete(store, responseOrderId, currency, total,
                            publicPayload, privatePayload, amount, item, Status.success);
                } else
                {
                    callback.complete(null, orderId, null, 0, null, null, 0, null, status);
                }
            }
        });

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.post(null);
    }

    public void newOrder(
        AccessToken accessToken,
        String storeName,
        String item,
        int amount,
        String currency,
        String component,
        Map<String, String> environment,
        final NewOrderCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getRuntime(), getLocation() + "/order/new",
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject result = ((JsonRequest) request).getObject();
                    int orderId = result.optInt("order_id", -1);
                    callback.complete(orderId, Status.success);
                } else
                {
                    callback.complete(-1, status);
                }
            }
        });

        Map<String, Object> fields = new HashMap<String, Object>();

        fields.put("store", storeName);
        fields.put("item", item);
        fields.put("amount", String.valueOf(amount));
        fields.put("currency", currency);
        fields.put("component", component);

        JSONObject env = new JSONObject();
        for (String key : environment.keySet())
        {
            env.put(key, environment.get(key));
        }

        fields.put("env", env.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.post(fields);
    }

    private static SimpleDateFormat getTimeFormat()
    {
        return Utils.DATE_FORMAT;
    }
}
