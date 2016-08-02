package ru.minlexx.xnovaalarm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import java.util.Locale;

import ru.minlexx.xnovaalarm.ifaces.IMainActivity;


public class ConfigureFragment extends Fragment {

    private static final String TAG = ConfigureFragment.class.getName();
    private static final String PREFS_CONFIG_FILENAME = "config";
    private static final String PREFS_REFRESH_INTERVAL = "refresh_interval";

    // GUI controls
    private EditText et_refreshinterval = null;
    private Switch sw_alarm_enabled = null;
    private boolean switch_handle = true;

    IMainActivity m_mainActivity = null;

    public ConfigureFragment() {
        // Required empty public constructor
    }

    public static ConfigureFragment newInstance() {
        return new ConfigureFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof IMainActivity) {
            m_mainActivity = (IMainActivity)context;
        } else {
            throw new RuntimeException(context.toString() + " must implement IMainActivity");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        m_mainActivity = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fragmentView = inflater.inflate(R.layout.fragment_configure, container, false);
        // get GUI controls
        et_refreshinterval = (EditText)fragmentView.findViewById(R.id.et_refreshinterval);
        sw_alarm_enabled = (Switch)fragmentView.findViewById(R.id.sw_alarm_enabled);
        //
        assert(et_refreshinterval != null);
        assert(sw_alarm_enabled != null);
        //
        this.doRestoreInstanceState(savedInstanceState);
        //
        sw_alarm_enabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onSwitchChanged(buttonView, isChecked);
            }
        });
        switch_handle = true;
        //
        return fragmentView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState()");
        //
        outState.putBoolean("sw_alarm_enabled", sw_alarm_enabled.isChecked());
        outState.putString("et_refreshinterval", et_refreshinterval.getText().toString());
    }

    protected void doRestoreInstanceState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) return;
        Log.d(TAG, "doRestoreInstanceState()");
        //
        switch_handle = false;
        sw_alarm_enabled.setChecked(savedInstanceState.getBoolean("sw_alarm_enabled"));
        switch_handle = true;
        et_refreshinterval.setText(savedInstanceState.getString("et_refreshinterval"));
    }

    @Override
    public void onStart() {
        super.onStart();
        SharedPreferences prefs = getActivity().getSharedPreferences(
                PREFS_CONFIG_FILENAME, Context.MODE_PRIVATE);
        String refresh_interval = prefs.getString(PREFS_REFRESH_INTERVAL, "15");
        et_refreshinterval.setText(refresh_interval);
        Log.d(TAG, "onStart(): loaded savedata");
    }

    @Override
    public void onStop() {
        super.onStop();
        // save config data
        SharedPreferences prefs = getActivity().getSharedPreferences(
                PREFS_CONFIG_FILENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefs_editor = prefs.edit();
        prefs_editor.putString(PREFS_REFRESH_INTERVAL, et_refreshinterval.getText().toString());
        prefs_editor.apply();
        Log.d(TAG, "onStop(): saved savedata");
    }

    public void onSwitchChanged(View view, boolean isChecked) {
        if (m_mainActivity == null) return;
        if (!switch_handle) return; // handler disabled
        if (isChecked) {
            int refresh_interval = Integer.valueOf(et_refreshinterval.getText().toString());
            Log.i(TAG, String.format(Locale.getDefault(),
                    "onSwitchChanged(): will call startService() with interval of %d minutes",
                    refresh_interval));
            Activity act = getActivity();
            Intent intent = new Intent(act, RefresherService.class);
            intent.putExtra(RefresherService.EXTRA_REFRESH_INTERVAL, refresh_interval);
            act.startService(intent);
        } else {
            //Log.i(TAG, "onSwitchChanged(): will call stopService()");
            //act.stopService(intent);
            RefresherService rsrv = m_mainActivity.getRefresherService();
            if (rsrv != null) {
                Log.d(TAG, "onSwitchChanged(): will call RefresherService.please_stopSelf()");
                // this will stop overview refresher, remove notification icon
                // and stop service at once
                rsrv.please_stopSelf();
            } else {
                Log.e(TAG, "m_mainActivity.getRefresherService() == null! This should not happen!");
            }
        }
    }

    public void updateButtonsEnabledStates(boolean isStarted, boolean isAuthed) {
        Log.d(TAG, String.format(Locale.getDefault(),
                "updateButtonsEnabledStates(): started: %b, authed: %b", isStarted, isAuthed));
        // authed? switch enabled state
        if (!isAuthed) {
            sw_alarm_enabled.setEnabled(false);
        } else {
            sw_alarm_enabled.setEnabled(true);
        }
        // service started/stopped affects config controls
        switch_handle = false; // disable onCheckedChange() handler
        if (isStarted) {
            et_refreshinterval.setEnabled(false);
            sw_alarm_enabled.setChecked(true);
        } else {
            et_refreshinterval.setEnabled(true);
            sw_alarm_enabled.setChecked(false);
        }
        switch_handle = true; // enable switch handler again
    }
}
