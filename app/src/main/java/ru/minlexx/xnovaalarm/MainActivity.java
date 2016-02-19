package ru.minlexx.xnovaalarm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;



public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState()");
        //
        final CheckBox cb = (CheckBox)findViewById(R.id.cb_remember);
        final EditText e_login = (EditText)findViewById(R.id.et_xnovalogin);
        final EditText e_pass = (EditText)findViewById(R.id.et_xnovapassword);
        //
        outState.putBoolean("cb_remember", cb.isChecked());
        outState.putString("login", e_login.getText().toString());
        outState.putString("pass", e_pass.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG, "onRestoreInstanceState()");
        //
        final CheckBox cb = (CheckBox)findViewById(R.id.cb_remember);
        final EditText e_login = (EditText)findViewById(R.id.et_xnovalogin);
        final EditText e_pass = (EditText)findViewById(R.id.et_xnovapassword);
        //
        cb.setChecked(savedInstanceState.getBoolean("cb_remember"));
        e_login.setText(savedInstanceState.getString("login"));
        e_pass.setText(savedInstanceState.getString("pass"));
    }

    public void onClickStartService(View view) {
        //Log.d(TAG, "onClickStartService()");
        Intent ssi = new Intent(this, RefresherService.class);
        startService(ssi);

        new RetrieveTask().execute("");
    }

    public void onClickStopService(View view) {
        //Log.d(TAG, "onClickStopService()");
        Intent ssi = new Intent(this, RefresherService.class);
        stopService(ssi);
    }
}
