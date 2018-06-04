package org.anthillplatform.runtime.requests;

import org.anthillplatform.runtime.AnthillRuntime;

public class StringRequest extends Request
{
    private String data;

    public StringRequest(AnthillRuntime runtime, String location, RequestCallback requestCallback)
    {
        super(location, requestCallback);
    }

    @Override
    protected void complete(Result result)
    {
        if (result != Result.success)
        {
            System.err.println("Service error: " + result.toString());

            String response = getData();

            if (response != null)
            {
                System.err.println("Response: " + response);
            }
        }

        super.complete(result);
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
