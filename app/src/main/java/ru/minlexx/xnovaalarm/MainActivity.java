package ru.minlexx.xnovaalarm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;


public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClickStartService(View view) {
        Log.d(TAG, "onClickStartService()");
        Intent ssi = new Intent(this, RefresherService.class);
        startService(ssi);

        new RetrieveTask().execute("");
    }

    public void onClickStopService(View view) {
        Log.d(TAG, "onClickStopService()");
        Intent ssi = new Intent(this, RefresherService.class);
        stopService(ssi);
    }
}
