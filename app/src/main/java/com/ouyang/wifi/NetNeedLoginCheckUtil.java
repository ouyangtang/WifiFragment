package com.ouyang.wifi;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by admin on 2016/10/14.
 */

public class NetNeedLoginCheckUtil extends AsyncTask<Integer, Integer, Boolean> {
    NeedLoginCallBack callBack;

    public NetNeedLoginCheckUtil(NeedLoginCallBack callBack) {
        super();
        this.callBack = callBack;
    }

    @Override
    protected Boolean doInBackground(Integer... params) {
        return isWifiSetPortal();
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (callBack != null) {
            callBack.needLogin(result);
        }
    }

    private boolean isWifiSetPortal() {
        final int WALLED_GARDEN_SOCKET_TIMEOUT_MS = 10000;
        HttpURLConnection urlConnection = null;
        InputStream ips = null;
        try {
            URL url = new URL("http://g.cn/generate_204");
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setInstanceFollowRedirects(false);
            urlConnection.setConnectTimeout(WALLED_GARDEN_SOCKET_TIMEOUT_MS);
            urlConnection.setReadTimeout(WALLED_GARDEN_SOCKET_TIMEOUT_MS);
            urlConnection.setUseCaches(false);
            ips = urlConnection.getInputStream();
            StringBuilder builder = new StringBuilder();
            BufferedReader buffer = null;
            buffer = new BufferedReader(new InputStreamReader(
                    ips, "UTF-8"));
            String line = null;
            while ((line = buffer.readLine()) != null) {
                builder.append(line);
            }
            Log.i("netportal", urlConnection.getResponseCode() + "**"
                    + builder.toString());

           // copyFileByIps(urlConnection.getResponseCode() + "**" + builder.toString());

            return urlConnection.getResponseCode() != 204;
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("netportal", "error");
            copyFileByIps("error");
            return false;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (ips != null) {
                try {
                    ips.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //
    public static void copyFileByIps(String content) {
        File file = new File(Environment.getExternalStorageDirectory().getPath() + File.separator +"nettest.txt");
        String strContent = content + "\r\n";
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "rwd");
            raf.seek(file.length());
            raf.write(strContent.getBytes());
            raf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void needLoginNetworkCheck(NeedLoginCallBack callBack) {
        new NetNeedLoginCheckUtil(callBack).execute();
    }

    public interface NeedLoginCallBack {
        void needLogin(boolean needLogin);
    }
}
