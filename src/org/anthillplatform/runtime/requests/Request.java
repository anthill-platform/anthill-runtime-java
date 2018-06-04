package org.anthillplatform.runtime.requests;

import com.mashape.unirest.http.HttpMethod;
import org.anthillplatform.runtime.AnthillRuntime;
import org.anthillplatform.runtime.services.LoginService;
import com.mashape.unirest.http.Headers;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import org.anthillplatform.runtime.util.InputStreamRequest;

import java.io.InputStream;
import java.util.HashMap;

public abstract class Request
{
    public static class Fields extends HashMap<String, Object> {}

    private final RequestCallback requestCallback;
    private final String location;
    private Fields queryArguments;
    private RequestMethod method;
    private Fields postFields;
    private String responseContentType;
    private LoginService.AccessToken workingAccessToken;
    private Headers responseHeaders;
    private InputStream putStream;
    private String APIVersion;

    public enum Result
    {
        success,
        malformedUrl,
        notFound,
        failed,
        dataCorrupted,
        badRequest,
        cannotAcquireService,
        forbidden,
        multipleChoices,
        noDeviceInfo,
        noInternet,
        pending,
        tooManyRequests,
        banned,
        serviceUnavailable,
        gone,
        conflict
    }

    public void setToken(LoginService.AccessToken workingAccessToken)
    {
        if (workingAccessToken == null)
            return;

        this.workingAccessToken = workingAccessToken;

        if (queryArguments == null)
            queryArguments = new Fields();

        queryArguments.put("access_token", workingAccessToken);
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

    public interface RequestCallback
    {
        void complete(Request request, Result result);
    }

    public void setQueryArguments(Fields queryArguments)
    {
        this.queryArguments = queryArguments;
    }

    public Request(String location, RequestCallback requestCallback)
    {
        this.location = location;

        this.requestCallback = requestCallback;
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
            for (Fields.Entry<String, Object> entry : queryArguments.entrySet())
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
                        complete(Result.multipleChoices);
                        break;
                    }

                    case 404:
                    {
                        complete(Result.notFound);
                        break;
                    }
                    case 410:
                    {
                        complete(Result.gone);
                        break;
                    }
                    case 400:
                    {
                        complete(Result.badRequest);
                        break;
                    }
                    case 429:
                    {
                        complete(Result.tooManyRequests);
                        break;
                    }
                    case 403:
                    {
                        complete(Result.forbidden);
                        break;
                    }
                    case 409:
                    {
                        complete(Result.conflict);
                        break;
                    }
                    case 423:
                    {
                        complete(Result.banned);
                        break;
                    }
                    case 503:
                    {
                        complete(Result.serviceUnavailable);
                        break;
                    }
                    default:
                    {
                        if (response.getStatus() >= 200 && response.getStatus() < 400)
                        {
                            String newToken = response.getHeaders().getFirst("Access-Token");

                            if (newToken != null)
                            {
                                LoginService loginService = AnthillRuntime.Get(LoginService.ID, LoginService.class);

                                /*
                                if (loginService != null)
                                {
                                    if (workingAccessToken != null)
                                    {
                                        workingAccessToken = loginService.setCurrentAccessToken(newToken);
                                    }
                                }
                                */
                            }

                            complete(Result.success);
                        }
                        else
                        {
                            complete(Result.failed);
                        }
                    }
                }


            }

            @Override
            public void failed(UnirestException e)
            {
                e.printStackTrace();

                complete(Result.failed);
            }

            @Override
            public void cancelled()
            {
                complete(Result.failed);
            }
        });
    }

    public void post()
    {
        post(null);
    }

    public void post(Fields fields)
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

    public void delete(Fields fields)
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

    protected void complete(Result result)
    {
        if (requestCallback != null)
        {
            requestCallback.complete(this, result);
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
