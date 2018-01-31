package org.anthillplatform.onlinelib.services;

import org.anthillplatform.onlinelib.request.JsonRequest;
import org.anthillplatform.onlinelib.OnlineLib;
import org.anthillplatform.onlinelib.Status;
import org.anthillplatform.onlinelib.entity.AccessToken;
import org.anthillplatform.onlinelib.request.Request;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

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

        public class Item
        {
            private String category;
            private Billing billing;
            private String id;
            private Map<String, Integer> contents;
            private JSONObject publicPayload;

            public abstract class Billing
            {
                public abstract void parse(JSONObject data);
            }

            public class IAPBilling extends Billing
            {
                private Tier tier;

                @Override
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

            public class OfflineBilling extends Billing
            {
                private String currency;
                private int amount;

                @Override
                public void parse(JSONObject data)
                {
                    this.currency = data.optString("currency");
                    this.amount = data.optInt("amount", 1);
                }

                public String getCurrency()
                {
                    return currency;
                }

                public int getAmount()
                {
                    return amount;
                }
            }

            public Item()
            {
                this.contents = new HashMap<String, Integer>();
            }

            private Billing newBilling(String type)
            {
                if (type.equals("iap"))
                {
                    return new IAPBilling();
                }

                return new OfflineBilling();
            }

            public void parse(JSONObject data)
            {
                this.id = data.getString("id");
                this.category = data.optString("category");
                this.publicPayload = data.optJSONObject("public");

                JSONObject billing = data.optJSONObject("billing");

                if (billing != null)
                {
                    String type = billing.optString("type", "offline");
                    this.billing = newBilling(type);
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

            public Tier()
            {
                this.prices = new HashMap<String, Price>();
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

        public Store(String name)
        {
            this.name = name;
            this.items = new ArrayList<Item>();
            this.tiers = new HashMap<String, Tier>();
        }

        public List<Item> getItems()
        {
            return items;
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
                        Tier tier = new Tier();
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
        }
    }

    public StoreService(OnlineLib onlineLib, String location)
    {
        super(onlineLib, location, ID, API_VERSION);

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
        JsonRequest jsonRequest = new JsonRequest(getOnlineLib(), getLocation() + "/store/" + name,
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
        JsonRequest jsonRequest = new JsonRequest(getOnlineLib(), getLocation() + "/orders",
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
        JsonRequest jsonRequest = new JsonRequest(getOnlineLib(), getLocation() + "/order/" +
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
        JsonRequest jsonRequest = new JsonRequest(getOnlineLib(), getLocation() + "/order/new",
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
}
