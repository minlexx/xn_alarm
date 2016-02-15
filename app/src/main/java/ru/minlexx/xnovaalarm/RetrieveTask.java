package ru.minlexx.xnovaalarm;


import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.HttpURLConnection;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


public class RetrieveTask extends AsyncTask<String, Void, String> {

    private static final String TAG = RetrieveTask.class.getName();
    private static final String XN_HOST = "uni4.xnova.su";

    private Exception exception;

    protected String test_download(String url) {
        try {
            URL xn_url;
            HttpURLConnection conn;
            StringBuilder response = new StringBuilder();
            String s;
            //
            xn_url = new URL("http", XN_HOST, 80, url);
            Log.d(TAG, "Downloading: " + xn_url.toString());
            conn = (HttpURLConnection)xn_url.openConnection();
            InputStreamReader ins = new InputStreamReader(conn.getInputStream());
            BufferedReader bufr = new BufferedReader(ins);
            while ((s = bufr.readLine()) != null) {
                response.append(s);
            }
            bufr.close();
            conn.disconnect();
            //
            //
            return response.toString();
        } catch (Exception e) {
            this.exception = e;
            return null;
        }
    }


    protected String test_jsoup(String url) {
        try {
            String full_url = "http://" + XN_HOST + "/" + url;
            Log.d(TAG, "Downloading: " + full_url);
            Document doc = Jsoup.connect(full_url)
                    .userAgent("XNovaAlarm")
                    .timeout(10000)
                    .cookie("name", "value")
                    .get();
            Log.i(TAG, doc.title());
            return doc.html();
        } catch (IOException ioe) {
            Log.e(TAG, "test_jsoup failed", ioe);
        }
        return null;
    }


    @Override
    protected String doInBackground(String... urls) {
        String url = urls[0];
        //return test_download(url);
        return test_jsoup(url);
    }

    @Override
    protected void onPostExecute(String result) {
        // TODO: check this.exception
        // TODO: do something with the result
        Log.i(TAG, result);
    }
}
