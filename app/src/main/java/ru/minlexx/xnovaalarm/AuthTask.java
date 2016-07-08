package ru.minlexx.xnovaalarm;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import ru.minlexx.xnovaalarm.ifaces.IMainActivity;


public class AuthTask extends AsyncTask<String, Void, List<HttpCookie>> {

    private static final String TAG = RetrieveTask.class.getName();
    private Exception m_exception = null;
    private IMainActivity m_mainActivity = null;
    private String m_login = null;
    private String m_pass = null;

    public AuthTask(IMainActivity mainActivity, String login, String pass) {
        m_mainActivity = mainActivity;
        m_login = login;
        m_pass = pass;
    }

    @Override
    protected List<HttpCookie> doInBackground(String... unused) {
        /*int num_params = params.length;
        if (num_params < 1) {
            Log.e(TAG, "AuthTask did not receive enough params!");
            return null;
        }*/

        List<HttpCookie> ret = new ArrayList<HttpCookie>();
        // test content
        HttpCookie cook1 = new HttpCookie("u5_id", "87");
        cook1.setDomain("uni5.xnova.su");
        cook1.setPath("/");
        cook1.setVersion(0);
        ret.add(cook1);

        // vars for HTTP interaction
        URL url;
        HttpURLConnection conn = null;
        StringBuilder response = new StringBuilder();
        StringBuilder postData = new StringBuilder();

        try {
            url = new URL("http", "uni5.xnova.su", 80, "/login/");
            Log.i(TAG, "Opening: " + url.toString() + "...");
        } catch (MalformedURLException mue) {
            m_exception = mue;
            Log.e(TAG, "Failed to construct URL! Failing...", mue);
            return null;
        }

        try {
            // construnct post data
            postData.append("email=");
            postData.append(URLEncoder.encode(m_login, "UTF-8"));
            postData.append("&password=");
            postData.append(URLEncoder.encode(m_pass, "UTF-8"));
            postData.append("&rememberme=on");
            byte[] postDataBytes = postData.toString().getBytes("UTF-8");
            //
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
            conn.setRequestProperty("Origin", "http://uni5.xnova.su");
            conn.setRequestProperty("Referer", "http://uni5.xnova.su/");
            conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64)" +
                    " AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36");
            conn.setDoOutput(true);
            conn.getOutputStream().write(postDataBytes);
            //
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            for (int c; (c = in.read()) >= 0; ) {
                //System.out.print((char) c);
                response.append((char)c);
            }
            String responseString = response.toString();
            //
            conn.disconnect();
        } catch (UnsupportedEncodingException uee) {
            m_exception = uee;
            Log.e(TAG, "Unsupported encoding!", uee);
        } catch (IOException ioe) {
            m_exception = ioe;
            Log.e(TAG, "Network error during auth process!", ioe);
        } finally {
            if (conn != null)
                conn.disconnect();
        }

        return ret;
    }

    @Override
    protected void onPostExecute(List<HttpCookie> httpCookies) {
        super.onPostExecute(httpCookies);
        m_mainActivity.onXNovaLoginOK(httpCookies);
    }
}
