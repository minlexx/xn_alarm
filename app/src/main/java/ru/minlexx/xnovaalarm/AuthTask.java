package ru.minlexx.xnovaalarm;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;


public class AuthTask extends AsyncTask<String, Void, String> {

    private static final String TAG = AuthTask.class.getName();
    private Exception m_exception = null;
    private AuthResultListener m_resultListener = null;
    private String m_login = null;
    private String m_pass = null;
    private boolean m_loginOk = false;
    private String m_loginErrorStr = null;

    public AuthTask(AuthResultListener resultListener, String login, String pass) {
        m_resultListener = resultListener;
        m_login = login;
        m_pass = pass;
    }

    @Override
    protected String doInBackground(String... unused) {
        m_loginOk = false;
        m_loginErrorStr = "";

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
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded; charset=UTF-8");
            conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
            conn.setRequestProperty("Origin", "http://uni5.xnova.su");
            conn.setRequestProperty("Referer", "http://uni5.xnova.su/");
            conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64)" +
                    " AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36");
            conn.setDoOutput(true);
            conn.getOutputStream().write(postDataBytes);
            //
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"));
            for (int c; (c = in.read()) >= 0; ) {
                response.append((char)c);
            }
            String responseString = response.toString();
            Log.d(TAG, responseString);
            // * correct login response string:
            //   {"status":1,"message":"","html":"","data":{"redirect":"\/overview\/"}}
            // * incorrect login respons string:
            //   {"status":0,"message":"\u041d... E-mail ...","html":"","data":[]}
            // parse response status code
            JSONObject rootObj = new JSONObject(responseString);
            int loginStatus = rootObj.getInt("status");
            if (loginStatus == 1) {
                Log.i(TAG, "Login OK");
                m_loginOk = true;
            } else {
                m_loginErrorStr = rootObj.getString("message");
                Log.e(TAG, "Login error: " + m_loginErrorStr);
            }
            //
            conn.disconnect();
            conn = null;
        } catch (UnsupportedEncodingException uee) {
            m_exception = uee;
            Log.e(TAG, "Unsupported encoding!", uee);
        } catch (IOException ioe) {
            m_exception = ioe;
            Log.e(TAG, "Network error during auth process!", ioe);
        } catch (JSONException je) {
            m_exception = je;
            Log.e(TAG, "Response JSON parse error!", je);
        } finally {
            if (conn != null)
                conn.disconnect();
        }

        return null;
    }

    @Override
    protected void onPostExecute(String nullString) {
        super.onPostExecute(nullString);
        if (m_loginOk && (m_resultListener != null))
            m_resultListener.onXNovaLoginOK();
        if (!m_loginOk && (m_resultListener != null)) {
            String message = m_loginErrorStr;
            if (m_loginErrorStr == null) {
                if (m_exception != null) {
                    message = String.format("Login error: Exception happened: %s",
                            m_exception.toString());
                } else {
                    message = "Error unknown!";
                }
            }
            m_resultListener.onXNovaLoginFail(message);
        }
    }

    /**
     * Callers should implements these callbacks
     */
    public interface AuthResultListener {
        void onXNovaLoginOK();
        void onXNovaLoginFail(String errorStr);
    }
}
