package com.basicaide.custommap;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class JSONDownloader
{
    public static String download(String url){
        StringBuffer result=new StringBuffer();
        try
        {
            HttpURLConnection httpConn = ((HttpURLConnection)new URL(url).openConnection());
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(httpConn.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null)
                result.append(line).append("\n");

            reader.close();
            httpConn.disconnect();

        }
        catch (IOException e)
        {
            e.printStackTrace();
            return "";
        }

        return result.toString();
    }
}
