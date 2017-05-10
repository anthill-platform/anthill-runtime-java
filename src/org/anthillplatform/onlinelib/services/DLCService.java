package org.anthillplatform.onlinelib.services;

import org.anthillplatform.onlinelib.OnlineLib;
import org.anthillplatform.onlinelib.Status;
import org.anthillplatform.onlinelib.request.JsonRequest;
import org.anthillplatform.onlinelib.request.Request;
import org.json.JSONObject;

import java.util.ArrayList;

public class DLCService extends Service
{
    private ArrayList<DLCRecord> records;

    public static final String ID = "dlc";

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

    public DLCService(OnlineLib onlineLib, String location)
    {
        super(onlineLib, location, ID);

        set(this);

        this.records = new ArrayList<DLCRecord>();
    }

    public void download(String appName, String appVersion, final DLCCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getOnlineLib(),
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
