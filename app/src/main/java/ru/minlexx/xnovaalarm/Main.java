package ru.minlexx.xnovaalarm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;

import ru.minlexx.xnovaalarm.ifaces.IMainActivity;
//import ru.minlexx.xnovaalarm.pojo.XNFlight;


public class Main extends FragmentActivity
    implements IMainActivity {

    private static final String TAG = Main.class.getName();
    private static final String TAG_LOGINFORM = LoginFormFragment.class.getName();
    private static final String TAG_CONFIGFORM = ConfigureFragment.class.getName();

    private RefresherService m_service = null; // null if not bound
    private ServiceConnection mConnection = new MainServiceConnection();

    // child fragments
    private LoginFormFragment m_login_form = null;
    private ConfigureFragment m_config_form = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Log.d(TAG, "onCreate() start");

        // setup fragments: if we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            // fragments are already created and inserted into layouts,
            // but we need to init member pointers to them
            final FragmentManager fm = getSupportFragmentManager();
            m_login_form = (LoginFormFragment)fm.findFragmentByTag(TAG_LOGINFORM);
            m_config_form = (ConfigureFragment)fm.findFragmentByTag(TAG_CONFIGFORM);
            assert(m_login_form != null);
            assert(m_config_form != null);
            Log.d(TAG, "onCreate(): found already existing fragments!");
            return;
        }

        Log.d(TAG, "onCreate(): instantiating fragments...");

        m_login_form = LoginFormFragment.newInstance();
        m_config_form = ConfigureFragment.newInstance();

        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.layout_top, m_login_form, TAG_LOGINFORM);
        ft.add(R.id.layout_bottom, m_config_form, TAG_CONFIGFORM);
        ft.commit();
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart(): will bind to service");
        // restore saved data
        //SharedPreferences prefs = getSharedPreferences(..., Context.MODE_PRIVATE);
        // Bind to local service
        final Intent intent = new Intent(this, RefresherService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop(): begin, ubinding from service...");
        // Unbind from the service
        if (m_service != null) {
            // we cannot longer receive notifications from service
            m_service.set_mainActivity(null);
            // ^^ and do this before unbinding
            unbindService(mConnection);
            m_service = null;
            Log.d(TAG, "unbound from service.");
        }
        // save savedata
        //final SharedPreferences prefs = getSharedPreferences(..., Context.MODE_PRIVATE);
        //final SharedPreferences.Editor prefs_editor = prefs.edit();
        //prefs_editor.apply();
    }

    @Override
    public RefresherService getRefresherService() {
        return m_service;
    }

    protected void updateButtonsEnabledStates() {
        boolean isStarted = false;
        boolean isAuthed = false;
        if (m_service != null) {
            isStarted = m_service.isStarted();
            isAuthed = m_service.isAuthorized();
        }
        Log.d(TAG, String.format("updateButtonsEnabledStates(): srv started: %b, authed: %b",
                isStarted, isAuthed));
        m_login_form.updateButtonsEnabledStates(isAuthed);
        m_config_form.updateButtonsEnabledStates(isStarted, isAuthed);
    }


    public class MainServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "ServiceConnection.onServiceConnected(): successfully bound to service");
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            final RefresherService.LocalBinder binder = (RefresherService.LocalBinder)service;
            m_service = binder.getService();
            // let the service know about us
            m_service.set_mainActivity(Main.this);
            //
            updateButtonsEnabledStates();
            //
            // test notification section
            //XNFlight test_fl = new XNFlight();
            //test_fl.timeLeft = 915; // 15 min 15 sec
            //m_service.showNotification_AM(test_fl, 5);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d(TAG, "ServiceConnection.onServiceDisconnected(): mark as not bound to service");
            m_service = null;
        }
    }


    ///////////////////////////////////////////
    // IMainActivity callbacks

    @Override
    public void notifyServiceStateChange() {
        Log.d(TAG, "notifyServiceStateChange(): will update buttons states");
        this.updateButtonsEnabledStates();
    }

}
