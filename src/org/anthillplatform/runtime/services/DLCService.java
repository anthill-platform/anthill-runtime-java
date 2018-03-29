package org.anthillplatform.runtime.services;

import org.anthillplatform.runtime.AnthillRuntime;
import org.anthillplatform.runtime.Status;
import org.anthillplatform.runtime.request.JsonRequest;
import org.anthillplatform.runtime.request.Request;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Downloadable content (DLC) management service for Anthill platform
 *
 * See https://github.com/anthill-platform/anthill-dlc
 */
public class DLCService extends Service
{
    private ArrayList<DLCRecord> records;

    public static final String ID = "dlc";
    public static final String API_VERSION = "0.2";

    private static DLCService instance;
    public static DLCService get() { return instance; }
    private static void set(DLCService service) { instance = service; }

    public class DLCRecord
    {
        public String name;
        public String url;
        public String hash;
    }

    public interface DLCCallback
    {
        void complete(DLCService service, Status status);
    }

    /**
     * Please note that you should not create an instance of the service yourself,
     * and use DLCService.get() to get existing one instead
     */
    public DLCService(AnthillRuntime runtime, String location)
    {
        super(runtime, location, ID, API_VERSION);

        set(this);

        this.records = new ArrayList<DLCRecord>();
    }

    public void download(String appName, String appVersion, final DLCCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getRuntime(),
            getLocation() + "/data/" + appName + "/" + appVersion,
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    DLCService.this.parseFiles(((JsonRequest) request).getObject());

                    callback.complete(DLCService.this, Status.success);
                } else
                {
                    callback.complete(DLCService.this, status);
                }
            }
        });

        jsonRequest.setAPIVersion(getAPIVersion());

        jsonRequest.get();
    }

    private void parseFiles(JSONObject object)
    {
        records.clear();

        for (Object fileName: object.keySet())
        {
            DLCRecord record = new DLCRecord();

            record.name = (String)fileName;

            JSONObject r = ((JSONObject) object.get(fileName.toString()));
            record.url = ((String) r.get("url"));
            record.hash = ((String) r.get("hash"));

            records.add(record);
        }
    }

    public ArrayList<DLCRecord> getFileRecords()
    {
        return records;
    }
}
