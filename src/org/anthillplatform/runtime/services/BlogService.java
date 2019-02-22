package org.anthillplatform.runtime.services;

import org.anthillplatform.runtime.AnthillRuntime;
import org.anthillplatform.runtime.requests.JsonRequest;
import org.anthillplatform.runtime.requests.Request;
import org.anthillplatform.runtime.util.Utils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * A service to deliver news and patch notes feed to the users inside the game
 *
 * See https://github.com/anthill-platform/anthill-blog
 */
public class BlogService extends Service
{
    private JsonRequest currentRequest;

    public static final String ID = "blog";
    public static final String API_VERSION = "0.2";

    /**
     * Please note that you should not create an instance of the service yourself,
     * and use AnthillRuntime.Get(BlogService.ID, BlogService.class) to get existing one instead
     */
    public BlogService(AnthillRuntime runtime, String location)
    {
        super(runtime, location, ID, API_VERSION);
    }

    public static BlogService Get()
    {
        return AnthillRuntime.Get(ID, BlogService.class);
    }

    public interface GetBlogEntriesCallback
    {
        void complete(BlogService service, Request request, Request.Result result, BlogEntriesList blogEntries);
    }

    public static class BlogEntriesList extends LinkedList<BlogEntry>
    {
        public void read(JSONArray entries)
        {
            clear();

            for (int i = 0, t = entries.length(); i < t; i++)
            {
                BlogEntry event = new BlogEntry();
                JSONObject data = entries.getJSONObject(i);

                if (data != null)
                {
                    if (!event.read(data))
                        continue;
                }

                this.add(event);
            }
        }
    }

    private static SimpleDateFormat getTimeFormat()
    {
        return Utils.DATE_FORMAT;
    }

    public static class BlogEntry
    {
        public JSONObject data;
        public int id;
        public Date dateCreate;
        public Date dateUpdate;

        public BlogEntry()
        {
        }

        public boolean read(JSONObject payload)
        {
            id = payload.getInt("id");
            data = payload.getJSONObject("data");

            try
            {
                SimpleDateFormat format = getTimeFormat();

                dateCreate = format.parse(payload.getString("create_date"));
                dateUpdate = format.parse(payload.getString("update_date"));
            }
            catch (Exception ignored)
            {
                return false;
            }

            return true;
        }
    }

    public void getBlogEntries(
        LoginService.AccessToken accessToken,
        String blog,
        final GetBlogEntriesCallback callback)
    {
        if (accessToken == null)
        {
            callback.complete(BlogService.this, null, Request.Result.forbidden, null);
            return;
        }

        if (currentRequest != null)
        {
            callback.complete(BlogService.this, null, Request.Result.pending, null);
            return;
        }

        currentRequest = new JsonRequest(getLocation() + "/blog/" + blog, new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                currentRequest = null;

                if (result == Request.Result.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    JSONArray entries = response.getJSONArray("entries");
                    if (entries != null)
                    {
                        BlogEntriesList blogEntries = new BlogEntriesList();
                        blogEntries.read(entries);
                        callback.complete(BlogService.this, request, result, blogEntries);
                        return;
                    }

                }

                callback.complete(BlogService.this, request, result, null);
            }
        });

        currentRequest.setAPIVersion(getAPIVersion());
        Request.Fields queryArguments = new Request.Fields();
        currentRequest.setQueryArguments(queryArguments);
        currentRequest.setToken(accessToken);
        currentRequest.get();
    }
}
