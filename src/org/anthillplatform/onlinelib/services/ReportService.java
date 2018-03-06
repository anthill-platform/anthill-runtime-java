package org.anthillplatform.onlinelib.services;

import org.anthillplatform.onlinelib.OnlineLib;
import org.anthillplatform.onlinelib.Status;
import org.anthillplatform.onlinelib.entity.AccessToken;
import org.anthillplatform.onlinelib.entity.ApplicationInfo;
import org.anthillplatform.onlinelib.request.JsonRequest;
import org.anthillplatform.onlinelib.request.Request;
import org.anthillplatform.onlinelib.util.Utils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReportService extends Service
{
    public static final String ID = "report";
    public static final String API_VERSION = "0.2";

    private static ReportService instance;
    public static ReportService get() { return instance; }
    private static void set(ReportService service) { instance = service; }

    public enum ReportFormat
    {
        json,
        binary,
        text
    }

    public ReportService(OnlineLib onlineLib, String location)
    {
        super(onlineLib, location, ID, API_VERSION);

        set(this);
    }

    public interface UploadReportCallback
    {
        void complete(String reportId, Request request, Status status);
    }

    public void uploadTextReport(
            String category,
            String message,
            JSONObject info,
            String contents,
            AccessToken accessToken,
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
            AccessToken accessToken,
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
        AccessToken accessToken,
        final UploadReportCallback callback)
    {
        ApplicationInfo applicationInfo = getOnlineLib().getApplicationInfo();

        JsonRequest jsonRequest = new JsonRequest(getOnlineLib(),
            getLocation() + "/upload/" + applicationInfo.getGameId() + "/" + applicationInfo.getGameVersion(),
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
                        String reportId = result.optString("id");

                        callback.complete(reportId, request, status);
                        return;
                    }
                }

                callback.complete(null, request, status);
            }
        });

        Map<String, String> args = new HashMap<String, String>();

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
