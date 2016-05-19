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
import android.widget.CheckBox;
import android.widget.EditText;



public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getName();
    private static final String PREFS_AUTH_FILENAME = "auth";
    private static final String PREFS_LOGIN = "xn_login";
    private static final String PREFS_PASS = "xn_pass";
    private static final String PREFS_REMEMBER = "xn_remember";

    private RefresherService m_service = null;
    private boolean m_bound = false;

    // GUI controls
    private CheckBox cb_remember = null;
    private EditText et_login = null;
    private EditText et_pass = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //
        // get GUI controls
        cb_remember = (CheckBox)findViewById(R.id.cb_remember);
        et_login = (EditText)findViewById(R.id.et_xnovalogin);
        et_pass = (EditText)findViewById(R.id.et_xnovapassword);
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
        // Bind to local service
        Intent intent = new Intent(this, RefresherService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (m_bound) {
            unbindService(mConnection);
            m_bound = false;
            m_service = null;
            Log.d(TAG, "onStop(): unbound from service.");
        }
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
        prefs_editor.apply();
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
        //
        updateButtonsEnabledStates(); // this does not work
    }

    public void onClickStopService(View view) {
        Log.d(TAG, "onClickStopService()");
        Intent ssi = new Intent(this, RefresherService.class);
        stopService(ssi);
        //
        updateButtonsEnabledStates(); // this does not work
    }

    private void updateButtonsEnabledStates() {
        if (m_service == null) return;
        boolean is_st = m_service.isStarted();
        //
        Log.d(TAG, String.format("updateButtonsEnabledStates(): service is started: %b", is_st));
        //
        View vbtn_starts = findViewById(R.id.button_starts);
        View vbtn_stops = findViewById(R.id.button_stops);
        if (is_st) {
            vbtn_starts.setEnabled(false);
            vbtn_stops.setEnabled(true);
        } else {
            vbtn_starts.setEnabled(true);
            vbtn_stops.setEnabled(false);
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
            Log.d(TAG, "onServiceConnected(): successfully bound to service");
            //
            updateButtonsEnabledStates();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            m_bound = false;
            m_service = null;
        }
    };
}
