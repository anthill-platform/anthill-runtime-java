package org.anthillplatform.onlinelib.entity;

import org.json.JSONObject;

import java.util.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sun.misc.BASE64Decoder;

public class AccessToken
{
    private boolean valid;
    private String credential;
    private String token;
    private Map<String, String> containers;
    private ArrayList<String> scopes;

    private static BASE64Decoder decoder = new BASE64Decoder();

    public static String CNT_EXPIRATION_DATE = "exp";
    public static String CNT_SCOPES = "sco";
    public static String CNT_USERNAME = "unm";
    public static String CNT_ACCOUNT = "acc";
    public static String CNT_GAMESPACE = "gms";

    static Pattern TOKEN_PATTERN = Pattern.compile("([^\\.]+)\\.([^\\.]+)\\.([^\\.]+)");

    public AccessToken(String token)
    {
        this.token = token;
        this.containers = new HashMap<String, String>();
        this.scopes = new ArrayList<String>();

        this.valid = parse();
    }

    public void update(String token)
    {
        this.token = token;
        this.containers.clear();
        this.scopes.clear();

        this.valid = parse();
    }

    public String getAccount()
    {
        return getContainer(CNT_ACCOUNT);
    }

    private boolean parse()
    {
        if (token == null) return false;

        Matcher matcher = TOKEN_PATTERN.matcher(token);

        if (matcher.find())
        {
            try
            {
                String header = new String(decoder.decodeBuffer(matcher.group(1)));
                String payload = new String(decoder.decodeBuffer(matcher.group(2)));

                JSONObject container = new JSONObject(payload);

                for (Object key : container.keySet())
                {
                    containers.put(key.toString(), container.get(key.toString()).toString());
                }
            }
            catch (Exception e)
            {
                return false;
            }

            init();

            return validate();
        }

        return false;
    }

    private void init()
    {
        this.credential = getContainer(CNT_USERNAME);
    }

    private boolean validate()
    {
        String scopesContainer = getContainer(CNT_SCOPES);

        if (scopesContainer == null)
        {
            return false;
        }

        String scopes[] = scopesContainer.split(",");

        Collections.addAll(this.scopes, scopes);

        return true;
    }

    public boolean hasScope(String scope)
    {
        return scopes.contains(scope);
    }

    public String getContainer(String container)
    {
        return containers.get(container);
    }

    public String getToken()
    {
        return token;
    }

    public String getCredential()
    {
        return credential;
    }

    public boolean isValid()
    {
        return valid;
    }
}
