package ru.minlexx.xnovaalarm;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.util.List;

import ru.minlexx.xnovaalarm.ifaces.IMainActivity;
import ru.minlexx.xnovaalarm.net.MyCookieStore;


public class MainActivity extends Activity
    implements IMainActivity
{

    private static final String TAG = MainActivity.class.getName();
    private static final String PREFS_AUTH_FILENAME = "auth";
    private static final String PREFS_LOGIN = "xn_login";
    private static final String PREFS_PASS = "xn_pass";
    private static final String PREFS_REMEMBER = "xn_remember";

    private RefresherService m_service = null;
    private boolean m_bound = false;

    // cookies!
    MyCookieStore m_cookieStore = null;
    CookieManager m_cookMgr = null;

    // GUI controls
    private CheckBox cb_remember = null;
    private EditText et_login = null;
    private EditText et_pass = null;
    private Button btn_starts = null;
    private Button btn_stops = null;
    private Button btn_login = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //
        m_cookieStore = new MyCookieStore();
        doInitializeCookiesManager();
        // get GUI controls
        cb_remember = (CheckBox)findViewById(R.id.cb_remember);
        et_login = (EditText)findViewById(R.id.et_xnovalogin);
        et_pass = (EditText)findViewById(R.id.et_xnovapassword);
        btn_starts = (Button)findViewById(R.id.button_starts);
        btn_stops = (Button)findViewById(R.id.button_stops);
        btn_login = (Button)findViewById(R.id.button_login);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState()");
        //
        outState.putBoolean("cb_remember", cb_remember.isChecked());
        outState.putString("login", et_login.getText().toString());
        outState.putString("pass", et_pass.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG, "onRestoreInstanceState()");
        //
        cb_remember.setChecked(savedInstanceState.getBoolean("cb_remember"));
        et_login.setText(savedInstanceState.getString("login"));
        et_pass.setText(savedInstanceState.getString("pass"));
    }

    @Override
    protected void onStart() {
        super.onStart();
        // restore saved data
        SharedPreferences prefs = getSharedPreferences(PREFS_AUTH_FILENAME, Context.MODE_PRIVATE);
        String saved_login = prefs.getString(PREFS_LOGIN, "");
        String saved_pass = prefs.getString(PREFS_PASS, "");
        boolean saved_remember = prefs.getBoolean(PREFS_REMEMBER, false);
        // restore?
        if (saved_remember) {
            cb_remember.setChecked(true);
            et_login.setText(saved_login);
            et_pass.setText(saved_pass);
            Log.d(TAG, "onStart(): loaded savedata");
        } else {
            cb_remember.setChecked(false);
            et_login.setText("");
            et_pass.setText("");
        }
        // restore cookies
        m_cookieStore.loadCookiesFrom(prefs);
        // Bind to local service
        Intent intent = new Intent(this, RefresherService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        doUnbindFromService();
        // save savedata?
        SharedPreferences prefs = getSharedPreferences(PREFS_AUTH_FILENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefs_editor = prefs.edit();
        if (cb_remember.isChecked()) {
            prefs_editor.putBoolean(PREFS_REMEMBER, true);
            prefs_editor.putString(PREFS_LOGIN, et_login.getText().toString());
            prefs_editor.putString(PREFS_PASS, et_pass.getText().toString());
            Log.d(TAG, "onStop(): saved savedata");
        } else {
            prefs_editor.putBoolean(PREFS_REMEMBER, false);
            prefs_editor.putString(PREFS_LOGIN, "");
            prefs_editor.putString(PREFS_PASS, "");
        }
        // save cookies
        m_cookieStore.storeCookiesTo(prefs_editor);
        //
        prefs_editor.apply();
    }

    protected void doUnbindFromService() {
        // Unbind from the service
        if (m_bound) {
            // we cannot longer receive notifications from service
            //if (m_service != null)
            //    m_service.set_mainActivity(null);
            // and do this before unbinding
            unbindService(mConnection);
            m_bound = false;
            m_service = null;
            Log.d(TAG, "unbound from service.");
            updateButtonsEnabledStates();
        }
    }

    protected void doInitializeCookiesManager() {
        m_cookMgr = new CookieManager(m_cookieStore, CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(m_cookMgr);
        Log.d(TAG, "cookies manager init complete");
    }

    public void onClickBeginLogin(View view) {
        Log.d(TAG, "onClickBeginLogin()");
        // get user auth data
        final String s_login = et_login.getText().toString();
        final String s_pass = et_pass.getText().toString();
        // begin auth
        AuthTask at = new AuthTask(this, s_login, s_pass);
        at.execute("");
    }

    public void onClickStartService(View view) {
        Log.d(TAG, "onClickStartService()");

        // get user auth data
        final String s_login = et_login.getText().toString();
        final String s_pass = et_pass.getText().toString();

        Intent ssi = new Intent(this, RefresherService.class);
        ssi.putExtra(RefresherService.EXTRA_XNOVA_LOGIN, s_login);
        ssi.putExtra(RefresherService.EXTRA_XNOVA_PASS, s_pass);
        startService(ssi);
    }

    public void onClickStopService(View view) {
        Log.d(TAG, "onClickStopService()");
        Intent ssi = new Intent(this, RefresherService.class);
        stopService(ssi);
        // also unbind from it... we do not need it running
        this.doUnbindFromService();
    }

    private void updateButtonsEnabledStates() {
        if (m_service == null) {
            // well, service is most probably not running and we are not bound to it
            // mark buttons as ready to start it
            btn_starts.setEnabled(true);
            btn_stops.setEnabled(false);
            return;
        }
        boolean is_st = m_service.isStarted();
        //
        Log.d(TAG, String.format("updateButtonsEnabledStates(): service is started: %b", is_st));
        //
        if (is_st) {
            btn_starts.setEnabled(false);
            btn_stops.setEnabled(true);
        } else {
            btn_starts.setEnabled(true);
            btn_stops.setEnabled(false);
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            RefresherService.LocalBinder binder = (RefresherService.LocalBinder)service;
            m_service = binder.getService();
            m_bound = true;
            m_service.set_mainActivity(MainActivity.this);
            Log.d(TAG, "ServiceConnection.onServiceConnected(): successfully bound to service");
            //
            updateButtonsEnabledStates();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d(TAG, "ServiceConnection.onServiceDisconnected(): mark as not bound to service");
            m_bound = false;
            m_service = null;
        }
    };

    @Override
    public void notifyServiceStateChange() {
        Log.i(TAG, "notifyServiceStateChange(): will update buttons");
        updateButtonsEnabledStates();
    }

    @Override
    public void onXNovaLoginOK(List<HttpCookie> cookies) {
        btn_login.setEnabled(false);
    }

    @Override
    public void onXNovaLoginFail() {
        btn_login.setEnabled(true);
    }
}
