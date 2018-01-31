package org.anthillplatform.onlinelib.request;

import com.mashape.unirest.http.HttpMethod;
import org.anthillplatform.onlinelib.Status;
import org.anthillplatform.onlinelib.entity.AccessToken;
import org.anthillplatform.onlinelib.services.LoginService;
import com.mashape.unirest.http.Headers;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import org.anthillplatform.onlinelib.util.InputStreamRequest;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public abstract class Request
{
    private final RequestResult requestResult;
    private final String location;
    private Map<String, String> queryArguments;
    private RequestMethod method;
    private Map<String, Object> postFields;
    private String responseContentType;
    private AccessToken workingAccessToken;
    private Headers responseHeaders;
    private InputStream putStream;
    private String APIVersion;

    public void setToken(AccessToken workingAccessToken)
    {
        if (workingAccessToken == null)
            return;

        this.workingAccessToken = workingAccessToken;

        if (queryArguments == null)
            queryArguments = new HashMap<String, String>();

        queryArguments.put("access_token", workingAccessToken.getToken());
    }

    public String getResponseContentType()
    {
        return responseContentType;
    }

    public enum RequestMethod
    {
        get,
        post,
        put,
        delete
    }

    public interface RequestResult
    {
        void complete(Request request, Status status);
    }

    public void setQueryArguments(Map<String, String> queryArguments)
    {
        this.queryArguments = queryArguments;
    }

    public Request(String location, RequestResult requestResult)
    {
        this.location = location;

        this.requestResult = requestResult;
        this.method = RequestMethod.get;
        this.queryArguments = null;
    }

    public void get()
    {
        init(RequestMethod.get);

        start();
    }

    public void setAPIVersion(String APIVersion)
    {
        this.APIVersion = APIVersion;
    }

    private void start()
    {
        HttpRequest request;

        switch (method)
        {
            case post:
            {
                HttpRequestWithBody post = Unirest.post(this.location);

                if (postFields != null && !postFields.isEmpty())
                {
                    post.fields(postFields);
                }

                request = post;

                break;
            }
            case delete:
            {
                HttpRequestWithBody delete = Unirest.delete(this.location);

                if (postFields != null && !postFields.isEmpty())
                {
                    delete.fields(postFields);
                }

                request = delete;

                break;
            }
            case put:
            {
                InputStreamRequest put = new InputStreamRequest(
                    HttpMethod.PUT, this.location, this.putStream);

                request = put;

                break;
            }
            case get:
            default:
            {
                GetRequest get = Unirest.get(this.location);

                request = get;

                break;
            }
        }

        if (queryArguments != null)
        {
            for (Map.Entry<String, String> entry : queryArguments.entrySet())
            {
                request.queryString(entry.getKey(), entry.getValue());
            }
        }

        if (APIVersion != null)
        {
            request.header("X-Api-Version", APIVersion);
        }

        request.asStringAsync(new Callback<String>()
        {
            @Override
            public void completed(HttpResponse<String> response)
            {
                responseHeaders = response.getHeaders();
                responseContentType = response.getHeaders().getFirst("Content-Type");

                if (response.getStatus() >= 300)
                {
                    System.err.println("Request failed: " + response.getBody());
                }

                parse(response.getBody());

                switch (response.getStatus())
                {
                    case 300:
                    {
                        complete(Status.multipleChoices);
                        break;
                    }

                    case 404:
                    {
                        complete(Status.notFound);
                        break;
                    }
                    case 410:
                    {
                        complete(Status.gone);
                        break;
                    }
                    case 400:
                    {
                        complete(Status.badRequest);
                        break;
                    }
                    case 429:
                    {
                        complete(Status.tooManyRequests);
                        break;
                    }
                    case 403:
                    {
                        complete(Status.forbidden);
                        break;
                    }
                    case 409:
                    {
                        complete(Status.conflict);
                        break;
                    }
                    case 423:
                    {
                        complete(Status.banned);
                        break;
                    }
                    case 503:
                    {
                        complete(Status.serviceUnavailable);
                        break;
                    }
                    default:
                    {
                        if (response.getStatus() >= 200 && response.getStatus() < 400)
                        {
                            String newToken = response.getHeaders().getFirst("Access-Token");

                            if (newToken != null)
                            {
                                LoginService loginService = LoginService.get();

                                if (loginService != null)
                                {
                                    if (workingAccessToken != null)
                                    {
                                        workingAccessToken.update(newToken);
                                    }
                                }
                            }

                            complete(Status.success);
                        }
                        else
                        {
                            complete(Status.failed);
                        }
                    }
                }


            }

            @Override
            public void failed(UnirestException e)
            {
                e.printStackTrace();

                complete(Status.failed);
            }

            @Override
            public void cancelled()
            {
                complete(Status.failed);
            }
        });
    }

    public void post()
    {
        post(null);
    }

    public void post(Map<String, Object> fields)
    {
        if (this.postFields == null)
        {
            this.postFields = fields;
        }
        else
        {
            if (fields != null)
                this.postFields.putAll(fields);
        }

        init(RequestMethod.post);

        start();
    }

    public void delete(Map<String, Object> fields)
    {
        if (this.postFields == null)
        {
            this.postFields = fields;
        }
        else
        {
            if (fields != null)
                this.postFields.putAll(fields);
        }

        init(RequestMethod.delete);

        start();
    }

    public void put(InputStream inputStream)
    {
        this.putStream = inputStream;

        init(RequestMethod.put);

        start();
    }

    private void init(RequestMethod method)
    {
        this.method = method;
    }

    protected void complete(Status status)
    {
        if (requestResult != null)
        {
            requestResult.complete(this, status);
        }
    }

    protected abstract void parse(String response);

    public String getLocation()
    {
        return location;
    }

    public Headers getResponseHeaders()
    {
        return responseHeaders;
    }
}
