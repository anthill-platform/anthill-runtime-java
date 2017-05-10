package org.anthillplatform.onlinelib.request;

import org.anthillplatform.onlinelib.OnlineLib;
import org.anthillplatform.onlinelib.Status;

public class StringRequest extends Request
{
    private String data;

    public StringRequest(OnlineLib onlineLib, String location, RequestResult requestResult)
    {
        super(location, requestResult);
    }

    @Override
    protected void complete(Status status)
    {
        if (status != Status.success)
        {
            System.err.println("Service error: " + status.toString());

            String response = getData();

            if (response != null)
            {
                System.err.println("Response: " + response);
            }
        }

        super.complete(status);
    }

    @Override
    protected void parse(String response)
    {
        this.data = response;
    }

    public String getData()
    {
        return data;
    }
}
