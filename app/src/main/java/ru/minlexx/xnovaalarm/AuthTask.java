package ru.minlexx.xnovaalarm;

import android.os.AsyncTask;
import android.util.Log;

import java.net.HttpCookie;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import ru.minlexx.xnovaalarm.ifaces.IMainActivity;


public class AuthTask extends AsyncTask<URL, Void, List<HttpCookie>> {

    private static final String TAG = RetrieveTask.class.getName();
    private Exception m_exception = null;
    private IMainActivity m_mainActivity;

    public AuthTask(IMainActivity mainActivity) {
        m_mainActivity = mainActivity;
    }

    @Override
    protected List<HttpCookie> doInBackground(URL... params) {
        int num_params = params.length;
        if (num_params < 1) {
            Log.e(TAG, "AuthTask did not receive enough params!");
            return null;
        }

        List<HttpCookie> ret = new ArrayList<HttpCookie>();

        HttpCookie cook1 = new HttpCookie("u5_id", "87");
        cook1.setDomain("uni5.xnova.su");
        cook1.setPath("/");
        cook1.setVersion(0);

        ret.add(cook1);

        return ret;
    }

    @Override
    protected void onPostExecute(List<HttpCookie> httpCookies) {
        super.onPostExecute(httpCookies);
        m_mainActivity.onXNovaLoginOK(httpCookies);
    }
}
