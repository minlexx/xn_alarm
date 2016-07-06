package ru.minlexx.xnovaalarm;

import android.os.AsyncTask;
import android.util.Log;

import java.net.HttpURLConnection;
import java.net.URL;


public class AuthTask extends AsyncTask<URL, Void, String> {

    private static final String TAG = RetrieveTask.class.getName();
    private Exception m_exception = null;

    @Override
    protected String doInBackground(URL... params) {
        int num_params = params.length;
        if (num_params < 1) {
            Log.e(TAG, "AuthTask did not receive enough params!");
            return null;
        }

        return null;
    }
}
