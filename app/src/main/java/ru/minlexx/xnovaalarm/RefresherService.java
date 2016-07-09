package ru.minlexx.xnovaalarm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import ru.minlexx.xnovaalarm.ifaces.IMainActivity;


public class RefresherService extends Service {

    private static final String TAG = RefresherService.class.getName();
    private final int NOTIFICATION_ID = R.string.local_service_started;
    private NotificationManager mNM = null;
    private final LocalBinder mBinder = new LocalBinder();
    private boolean m_is_started = false;

    private IMainActivity m_mainActivity = null;

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        RefresherService getService() {
            return RefresherService.this;
        }
    }

    public RefresherService() {
    }

    public void set_mainActivity(IMainActivity mainActivity) {
        this.m_mainActivity = mainActivity;
        if (mainActivity != null)
            Log.d(TAG, "IMainActivity pointer was set on the service!");
        else
            Log.d(TAG, "IMainActivity pointer was unset from the service.");
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        hideNotification();
        m_is_started = false;
        m_mainActivity = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand(): Received start id " + startId + ": " + intent);
        //
        m_is_started = true;
        //
        showNotification();
        if (this.m_mainActivity != null)
            this.m_mainActivity.notifyServiceStateChange();
        //
        new RetrieveTask().execute("");

        return START_NOT_STICKY;
    }


    public boolean isStarted() { return m_is_started; }


    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.local_service_started);

        Intent activityIntent = new Intent(this, MainActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, activityIntent, 0);

        // Set the info for the views that show in the notification panel.
        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.logo)  // the status icon
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(getText(R.string.local_service_label))  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .setOngoing(true)
                .build();

        // Send the notification.
        mNM.notify(NOTIFICATION_ID, notification);
    }

    private void hideNotification() {
        if (mNM != null) {
            mNM.cancel(NOTIFICATION_ID);
        }
    }
}
