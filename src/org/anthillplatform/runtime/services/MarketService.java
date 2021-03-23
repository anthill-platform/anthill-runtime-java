package org.anthillplatform.runtime.services;

import org.anthillplatform.runtime.AnthillRuntime;
import org.anthillplatform.runtime.requests.JsonRequest;
import org.anthillplatform.runtime.requests.Request;
import org.anthillplatform.runtime.util.ApplicationInfo;
import org.anthillplatform.runtime.util.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * User submitted reports collecting service
 *
 * See https://github.com/anthill-platform/anthill-market
 */
public class MarketService extends Service
{
    public static final String ID = "market";
    public static final String API_VERSION = "0.2";

    public static class MarketItemEntry
    {
        public String name;
        public int amount;
        public JSONObject payload;

        public MarketItemEntry(String name, int amount, JSONObject payload)
        {
            this.name = name;
            this.amount = amount;
            this.payload = payload;
        }
    }

    public enum MarkerEntriesOrder
    {
        none,
        takeAmountDesc,
        takeAmountAsc,
        giveAmountDesc,
        giveAmountAsc
    }

    public static class MarketOrderEntry
    {
        public String orderId;
        public String ownerId;
        public String giveItem;
        public String takeItem;
        public int giveAmount;
        public int takeAmount;
        public JSONObject givePayload;
        public JSONObject takePayload;
        public int available;
        public JSONObject orderPayload;
        public Date time;
        public Date deadline;
    }

    /**
     * Please note that you should not create an instance of the service yourself,
     * and use AnthillRuntime.Get(ReportService.ID, ReportService.class) to get existing one instead
     */
    public MarketService(AnthillRuntime runtime, String location)
    {
        super(runtime, location, ID, API_VERSION);
    }

    public static MarketService Get()
    {
        return AnthillRuntime.Get(ID, MarketService.class);
    }

    public interface GetMarketItemsCallback
    {
        void complete(Request request, Request.Result result, List<MarketItemEntry> entries);
    }

    public interface GetMarketItemCallback
    {
        void complete(Request request, Request.Result result, int amount);
    }

    public interface GetMarketSettingsCallback
    {
        void complete(Request request, Request.Result result, JSONObject settings);
    }

    public interface UpdateMarketItemsCallback
    {
        void complete(Request request, Request.Result result);
    }

    public interface UpdateMarketItemCallback
    {
        void complete(Request request, Request.Result result);
    }

    public interface PostOrderCallback
    {
        void complete(Request request, Request.Result result, String orderId, boolean fulfilled);
    }

    public interface DeleteOrderCallback
    {
        void complete(Request request, Request.Result result);
    }

    public interface ListMarketOrdersCallback
    {
        void complete(Request request, Request.Result result, List<MarketOrderEntry> entries);
    }

    public interface GetMarketOrderCallback
    {
        void complete(Request request, Request.Result result, MarketOrderEntry order);
    }

    public void getMarketSettings(
        String marketName,
        LoginService.AccessToken accessToken,
        final GetMarketSettingsCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(
            getLocation() + "/markets/" + marketName,
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result status)
            {
                if (status == Request.Result.success)
                {
                    JSONObject result = ((JsonRequest) request).getObject();

                    callback.complete(request, status, result.optJSONObject("settings"));
                    return;
                }

                callback.complete(request, status, null);
            }
        });

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.get();
    }

    public void getMarketItems(
        String marketName,
        LoginService.AccessToken accessToken,
        final GetMarketItemsCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(
            getLocation() + "/markets/" + marketName + "/items",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result status)
            {
                if (status == Request.Result.success)
                {
                    JSONObject result = ((JsonRequest) request).getObject();

                    if (result != null)
                    {
                        JSONArray items = result.optJSONArray("items");

                        if (items != null)
                        {
                            List<MarketItemEntry> entries = new ArrayList<>();

                            for (int i = 0, t = items.length(); i <= t; i++)
                            {
                                JSONObject entry = items.optJSONObject(i);
                                if (entry == null)
                                    continue;
                                String name = entry.optString("name");
                                JSONObject payload = entry.optJSONObject("payload");
                                int amount = entry.optInt("amount", 1);

                                if (name != null && payload != null)
                                {
                                    entries.add(new MarketItemEntry(name, amount, payload));
                                }
                            }

                            callback.complete(request, status, entries);
                            return;
                        }
                    }
                }

                callback.complete(request, status, null);
            }
        });

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.get();
    }

    public void getMarketItem(
        String marketName,
        String item,
        JSONObject payload,
        LoginService.AccessToken accessToken,
        final GetMarketItemCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(
            getLocation() + "/markets/" + marketName + "/items/" + item,
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result status)
            {
                if (status == Request.Result.success)
                {
                    JSONObject result = ((JsonRequest) request).getObject();

                    if (result != null)
                    {
                        callback.complete(request, status, result.optInt("amount", 0));
                        return;
                    }
                }

                callback.complete(request, status, 0);
            }
        });

        Request.Fields fields = new Request.Fields();
        fields.put("payload", payload.toString());

        jsonRequest.setQueryArguments(fields);
        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.get();
    }

    public void updateMarketItems(
        String marketName,
        List<MarketItemEntry> entries,
        LoginService.AccessToken accessToken,
        final UpdateMarketItemsCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(
            getLocation() + "/markets/" + marketName + "/items",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result status)
            {
                callback.complete(request, status);
            }
        });

        Request.Fields fields = new Request.Fields();

        JSONArray items = new JSONArray();
        for (MarketItemEntry entry: entries)
        {
            JSONObject o = new JSONObject();
            o.put("name", entry.name);
            o.put("amount", entry.amount);
            o.put("payload", entry.payload);
            items.put(o);
        }

        fields.put("items", items.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.post(fields);
    }

    public void updateMarketItem(
        String marketName,
        String item,
        JSONObject payload,
        int updateAmount,
        LoginService.AccessToken accessToken,
        final UpdateMarketItemsCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(
            getLocation() + "/markets/" + marketName + "/items/" + item,
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result status)
            {
                callback.complete(request, status);
            }
        });

        Request.Fields fields = new Request.Fields();

        fields.put("payload", payload.toString());
        fields.put("amount", updateAmount);

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.post(fields);
    }

    private static SimpleDateFormat getTimeFormat()
    {
        return Utils.DATE_FORMAT;
    }

    public void postOrder(
        String marketName,
        String giveItem,
        int giveAmount,
        JSONObject givePayload,
        String takeItem,
        int takeAmount,
        JSONObject takePayload,
        int amount,
        JSONObject orderPayload,
        Date deadline,
        LoginService.AccessToken accessToken,
        final PostOrderCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(
            getLocation() + "/markets/" + marketName + "/orders",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result status)
            {
                if (status == Request.Result.success)
                {
                    JSONObject result = ((JsonRequest) request).getObject();

                    if (result != null)
                    {
                        callback.complete(request, status, result.optString("order_id"),
                            result.optBoolean("fulfilled_immediately", false));
                        return;
                    }
                }

                callback.complete(request, status, null, false);
            }
        });

        Request.Fields fields = new Request.Fields();

        fields.put("give_item", giveItem);
        fields.put("give_amount", giveAmount);
        fields.put("give_payload", givePayload.toString());
        fields.put("take_item", takeItem);
        fields.put("take_amount", takeAmount);
        fields.put("take_payload", takePayload.toString());
        fields.put("orders_amount", amount);
        if (orderPayload != null)
        {
            fields.put("payload", orderPayload.toString());
        }
        fields.put("deadline", getTimeFormat().format(deadline));

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.post(fields);
    }

    public void fulfillOrder(
            String marketName,
            String orderId,
            int fulfillAmount,
            LoginService.AccessToken accessToken,
            final PostOrderCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(
            getLocation() + "/markets/" + marketName + "/orders/" + orderId + "/fulfill",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result status)
            {
                if (status == Request.Result.success)
                {
                    JSONObject result = ((JsonRequest) request).getObject();

                    if (result != null)
                    {
                        callback.complete(request, status, orderId,
                            result.optBoolean("fulfilled_immediately", false));
                        return;
                    }
                }

                callback.complete(request, status, null, false);
            }
        });

        Request.Fields fields = new Request.Fields();

        fields.put("amount", fulfillAmount);

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.post(fields);
    }

    public void deleteOrder(
        String marketName,
        String orderId,
        LoginService.AccessToken accessToken,
        final DeleteOrderCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(
            getLocation() + "/markets/" + marketName + "/orders/" + orderId + "/delete",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result status)
            {
                callback.complete(request, status);
            }
        });

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.post();
    }

    public enum ListOrderComparison
    {
        none,

        more,
        less,
        equal,
        less_or_equal,
        more_or_equal
    }

    private String listOrderComparisonToString(ListOrderComparison comp)
    {
        switch (comp)
        {
            case more: return ">";
            case less: return "<";
            case equal: return "=";
            case less_or_equal: return "<=";
            case more_or_equal: return ">=";
        }

        return null;
    }

    public void listOrders(
            String marketName,
            String ownerId,
            String giveItem,
            int giveAmount,
            ListOrderComparison giveAmountComparison,
            JSONObject givePayload,
            String takeItem,
            int takeAmount,
            ListOrderComparison takeAmountComparison,
            JSONObject takePayload,
            MarkerEntriesOrder sortOrder,
            LoginService.AccessToken accessToken,
            final ListMarketOrdersCallback callback)
    {
        listOrders(marketName, ownerId, giveItem, giveAmount, giveAmountComparison, givePayload, takeItem,
            takeAmount, takeAmountComparison, takePayload, sortOrder, accessToken, callback, 0, 1000);
    }

    public void listOrders(
        String marketName,
        String ownerId,
        String giveItem,
        int giveAmount,
        ListOrderComparison giveAmountComparison,
        JSONObject givePayload,
        String takeItem,
        int takeAmount,
        ListOrderComparison takeAmountComparison,
        JSONObject takePayload,
        MarkerEntriesOrder sortOrder,
        LoginService.AccessToken accessToken,
        final ListMarketOrdersCallback callback,
        int offset,
        int limit)
    {
        JsonRequest jsonRequest = new JsonRequest(
            getLocation() + "/markets/" + marketName + "/orders",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result status)
            {
                if (status == Request.Result.success)
                {
                    JSONObject result = ((JsonRequest) request).getObject();

                    if (result != null)
                    {
                        JSONArray orders = result.optJSONArray("orders");

                        if (orders != null)
                        {
                            List<MarketOrderEntry> entries = new ArrayList<>();

                            for (int i = 0, t = orders.length(); i <= t; i++)
                            {
                                JSONObject entry = orders.optJSONObject(i);
                                if (entry == null)
                                    continue;

                                MarketOrderEntry e = new MarketOrderEntry();
                                e.orderId = entry.optString("order_id");
                                e.ownerId = entry.optString("owner_id");
                                e.giveItem = entry.optString("give_item");
                                e.takeItem = entry.optString("take_item");
                                e.giveAmount = entry.optInt("give_amount", 1);
                                e.takeAmount = entry.optInt("take_amount", 1);
                                e.available = entry.optInt("available", 1);
                                e.givePayload = entry.optJSONObject("give_payload");
                                e.takePayload = entry.optJSONObject("take_payload");
                                e.orderPayload = entry.optJSONObject("payload");

                                try
                                {
                                    e.time = getTimeFormat().parse(entry.getString("time"));
                                }
                                catch (ParseException | JSONException ex)
                                {
                                    e.time = null;
                                }

                                try
                                {
                                    e.deadline = getTimeFormat().parse(entry.getString("deadline"));
                                }
                                catch (ParseException | JSONException ex)
                                {
                                    e.deadline = null;
                                }

                                entries.add(e);
                            }

                            callback.complete(request, status, entries);
                            return;
                        }
                    }
                }

                callback.complete(request, status, null);
            }
        });

        Request.Fields query = new Request.Fields();
        query.put("offset", offset);
        query.put("limit", limit);

        if (ownerId != null)
            query.put("owner_id", ownerId);

        if (giveItem != null)
            query.put("give_item", giveItem);

        if (givePayload != null)
            query.put("give_payload", givePayload.toString());

        if (takeItem != null)
            query.put("take_item", takeItem);

        if (takePayload != null)
            query.put("take_payload", takePayload.toString());

        if (giveAmountComparison != ListOrderComparison.none)
        {
            query.put("give_amount", giveAmount);
            query.put("give_amount_comparison", listOrderComparisonToString(giveAmountComparison));
        }

        if (takeAmountComparison != ListOrderComparison.none)
        {
            query.put("take_amount", takeAmount);
            query.put("take_amount_comparison", listOrderComparisonToString(takeAmountComparison));
        }

        switch (sortOrder)
        {
            case giveAmountAsc:
            {
                query.put("sort_by", "give_amount");
                query.put("sort_desc", "false");
                break;
            }
            case giveAmountDesc:
            {
                query.put("sort_by", "give_amount");
                query.put("sort_desc", "true");
                break;
            }
            case takeAmountAsc:
            {
                query.put("sort_by", "take_amount");
                query.put("sort_desc", "false");
                break;
            }
            case takeAmountDesc:
            {
                query.put("sort_by", "take_amount");
                query.put("sort_desc", "true");
                break;
            }
        }

        jsonRequest.setQueryArguments(query);
        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.get();
    }

    public void listMyOrders(
            String marketName,
            LoginService.AccessToken accessToken,
            final ListMarketOrdersCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(
            getLocation() + "/markets/" + marketName + "/orders/my",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result status)
            {
                if (status == Request.Result.success)
                {
                    JSONObject result = ((JsonRequest) request).getObject();

                    if (result != null)
                    {
                        JSONArray orders = result.optJSONArray("orders");

                        if (orders != null)
                        {
                            List<MarketOrderEntry> entries = new ArrayList<>();

                            for (int i = 0, t = orders.length(); i <= t; i++)
                            {
                                JSONObject entry = orders.optJSONObject(i);
                                if (entry == null)
                                    continue;

                                MarketOrderEntry e = new MarketOrderEntry();
                                e.orderId = entry.optString("order_id");
                                e.ownerId = entry.optString("owner_id");
                                e.giveItem = entry.optString("give_item");
                                e.takeItem = entry.optString("take_item");
                                e.giveAmount = entry.optInt("give_amount", 1);
                                e.takeAmount = entry.optInt("take_amount", 1);
                                e.available = entry.optInt("available", 1);
                                e.givePayload = entry.optJSONObject("give_payload");
                                e.takePayload = entry.optJSONObject("take_payload");
                                e.orderPayload = entry.optJSONObject("payload");

                                try
                                {
                                    e.time = getTimeFormat().parse(entry.getString("time"));
                                }
                                catch (ParseException | JSONException ex)
                                {
                                    e.time = null;
                                }

                                try
                                {
                                    e.deadline = getTimeFormat().parse(entry.getString("deadline"));
                                }
                                catch (ParseException | JSONException ex)
                                {
                                    e.deadline = null;
                                }

                                entries.add(e);
                            }

                            callback.complete(request, status, entries);
                            return;
                        }
                    }
                }

                callback.complete(request, status, null);
            }
        });

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.get();
    }

    public void getOrder(
        String marketName, String orderId,
        LoginService.AccessToken accessToken,
        final GetMarketOrderCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(
            getLocation() + "/markets/" + marketName + "/orders/" + orderId,
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result status)
            {
                if (status == Request.Result.success)
                {
                    JSONObject result = ((JsonRequest) request).getObject();

                    MarketOrderEntry e = new MarketOrderEntry();
                    e.orderId = result.optString("order_id");
                    e.ownerId = result.optString("owner_id");
                    e.giveItem = result.optString("give_item");
                    e.takeItem = result.optString("take_item");
                    e.giveAmount = result.optInt("give_amount", 1);
                    e.takeAmount = result.optInt("take_amount", 1);
                    e.available = result.optInt("available", 1);
                    e.givePayload = result.optJSONObject("give_payload");
                    e.takePayload = result.optJSONObject("take_payload");

                    try
                    {
                        e.time = getTimeFormat().parse(result.getString("time"));
                    }
                    catch (ParseException | JSONException ex)
                    {
                        e.time = null;
                    }

                    try
                    {
                        e.deadline = getTimeFormat().parse(result.getString("deadline"));
                    }
                    catch (ParseException | JSONException ex)
                    {
                        e.deadline = null;
                    }

                    callback.complete(request, status, e);
                    return;
                }

                callback.complete(request, status, null);
            }
        });

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.get();
    }
}
