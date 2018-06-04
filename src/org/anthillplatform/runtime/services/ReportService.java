package org.anthillplatform.runtime.services;

import org.anthillplatform.runtime.AnthillRuntime;
import org.anthillplatform.runtime.util.ApplicationInfo;
import org.anthillplatform.runtime.requests.JsonRequest;
import org.anthillplatform.runtime.requests.Request;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * User submitted reports collecting service
 *
 * See https://github.com/anthill-platform/anthill-report
 */
public class ReportService extends Service
{
    public static final String ID = "report";
    public static final String API_VERSION = "0.2";

    public enum ReportFormat
    {
        json,
        binary,
        text
    }

    /**
     * Please note that you should not create an instance of the service yourself,
     * and use AnthillRuntime.Get(ReportService.ID, ReportService.class) to get existing one instead
     */
    public ReportService(AnthillRuntime runtime, String location)
    {
        super(runtime, location, ID, API_VERSION);
    }

    public static ReportService Get()
    {
        return AnthillRuntime.Get(ID, ReportService.class);
    }

    public interface UploadReportCallback
    {
        void complete(String reportId, Request request, Request.Result result);
    }

    public void uploadTextReport(
            String category,
            String message,
            JSONObject info,
            String contents,
            LoginService.AccessToken accessToken,
            final UploadReportCallback callback)
    {
        InputStream stream = new ByteArrayInputStream(contents.getBytes());
        uploadReport(category, message, ReportFormat.text, info, stream, accessToken, callback);
    }

    public void uploadJSONReport(
            String category,
            String message,
            JSONObject info,
            JSONObject contents,
            LoginService.AccessToken accessToken,
            final UploadReportCallback callback)
    {
        String jsonContents = contents.toString();
        InputStream stream = new ByteArrayInputStream(jsonContents.getBytes());
        uploadReport(category, message, ReportFormat.json, info, stream, accessToken, callback);
    }

    public void uploadReport(
        String category,
        String message,
        ReportFormat format,
        JSONObject info,
        InputStream contents,
        LoginService.AccessToken accessToken,
        final UploadReportCallback callback)
    {
        ApplicationInfo applicationInfo = getRuntime().getApplicationInfo();

        JsonRequest jsonRequest = new JsonRequest(
            getLocation() + "/upload/" + applicationInfo.applicationName + "/" + applicationInfo.applicationVersion,
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
                        String reportId = result.optString("id");

                        callback.complete(reportId, request, status);
                        return;
                    }
                }

                callback.complete(null, request, status);
            }
        });

        Request.Fields args = new Request.Fields();

        args.put("category", category);
        args.put("message", message);
        args.put("format", format.toString());
        args.put("info", info.toString());

        jsonRequest.setQueryArguments(args);
        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.put(contents);
    }

}
