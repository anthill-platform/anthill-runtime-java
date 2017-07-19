package org.anthillplatform.onlinelib.util;

import com.mashape.unirest.http.HttpMethod;
import com.mashape.unirest.request.HttpRequest;
import com.mashape.unirest.request.body.Body;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.InputStreamEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class InputStreamRequest extends HttpRequest
{
    public InputStreamRequest(HttpMethod method, String url, final InputStream inputStream)
    {
        super(method, url);

        this.body = new Body()
        {
            @Override
            public HttpEntity getEntity()
            {
                return new InputStreamEntity(inputStream);
            }
        };
    }
}
