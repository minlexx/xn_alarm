package ru.minlexx.xnovaalarm;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.HttpURLConnection;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


public class RetrieveTask extends AsyncTask<String, Void, String> {

    private static final String TAG = RetrieveTask.class.getName();
    private static final String XN_HOST = "uni4.xnova.su";

    private Exception m_exception = null;

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
            this.m_exception = e;
            return null;
        }
    }


    protected String test_jsoup(String url) {
        try {
            String full_url = "http://" + XN_HOST + "/" + url;
            Log.d(TAG, "JSOUP Downloading: " + full_url);
            Document doc = Jsoup.connect(full_url)
                    .userAgent("XNovaAlarm")
                    .timeout(10000)
                    .cookie("name", "value")
                    .get();
            Log.i(TAG, doc.title());
            return doc.html();
        } catch (IOException ioe) {
            this.m_exception = ioe;
        }
        return null;
    }


    @Override
    protected String doInBackground(String... urls) {
        String url = urls[0];
        // return test_download(url);
        return test_jsoup(url);
    }

    @Override
    protected void onPostExecute(String result) {
        // check this.exception
        if (this.m_exception != null) {
            Log.e(TAG, "RetrieveTask failed:", this.m_exception);
            return;
        }
        // do something with the result
        // Log.i(TAG, result); // log to logger
        Log.i(TAG, String.format("Downloaded %d bytes", result.length()));
        // try to save to a file
        String extState = Environment.getExternalStorageState();
        // check if it is writable
        if (extState.equals(Environment.MEDIA_MOUNTED)) {
            File extDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);
            Log.d(TAG, extDir.getAbsolutePath());

            try {
                File outFile = new File(extDir, "xnova.html");
                FileWriter fw = new FileWriter(outFile);
                fw.write(result);
                fw.close();
                Log.d(TAG, String.format("Saved result to [%s]", outFile.getAbsolutePath()));
            } catch (IOException ioe) {
                Log.e(TAG, "Failed to save download result!", ioe);
            }
        }
    }
}
