package org.anthillplatform.runtime.util;

import java.text.SimpleDateFormat;

public class Utils
{
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    public static String join(String[] items)
    {
        StringBuilder sb = new StringBuilder();

        for (String item : items)
        {
            if (sb.length() > 0)
            {
                sb.append(",");
            }

            sb.append(item);
        }

        return sb.toString();
    }
}
