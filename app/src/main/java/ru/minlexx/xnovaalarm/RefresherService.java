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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import ru.minlexx.xnovaalarm.ifaces.IMainActivity;


public class RefresherService extends Service {

    private static final String TAG = RefresherService.class.getName();
    private final int NOTIFICATION_ID = R.string.local_service_started;
    private NotificationManager mNM = null;
    private final LocalBinder mBinder = new LocalBinder();
    private boolean m_is_started = false;

    private IMainActivity m_mainActivity = null;

    private Timer m_timer = null;

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        public RefresherService getService() {
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
        m_timer = new Timer("OverviewRefreshTimer", false);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        m_timer.cancel();
        m_timer = null;
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
        //new RetrieveTask().execute("");
        //
        // use timer instead, test
        Log.d(TAG, "Will run task after 500 ms...");
        createTimer();

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

    /******************************************************************************/

    private void createTimer() {
        // execute task every 10 minutes after a 0.5 sec delay
        m_timer.schedule(new OverviewRefreshTask(), 500L, 10*60*1000L);
    }

    public class OverviewRefreshTask extends TimerTask {

        private final String ITAG = OverviewRefreshTask.class.getName();
        private final String XN_HOST = "uni5.xnova.su";

        @Override
        public void run() {
            Log.d(ITAG, "run() !");
            //
            try {
                String overview_content = download_overview();
            } catch (Exception e) {
                Log.e(ITAG, "Refresh overview failed!", e);
            }
        }

        protected String download_overview() {
            URL xn_url;
            HttpURLConnection conn = null;
            StringBuilder response = new StringBuilder();
            String s;
            //
            try {
                xn_url = new URL("http", XN_HOST, 80, "/overview/");
                Log.d(ITAG, "Downloading: " + xn_url.toString());
                conn = (HttpURLConnection) xn_url.openConnection();
                InputStreamReader ins = new InputStreamReader(conn.getInputStream());
                BufferedReader bufr = new BufferedReader(ins);
                while ((s = bufr.readLine()) != null) {
                    response.append(s);
                }
                bufr.close();
                conn.disconnect();
            } catch (IOException ioe) {
                Log.e(ITAG, "Failed to download overview!", ioe);
            } finally {
                if (conn != null)
                    conn.disconnect();
            }
            //
            return response.toString();
        }
    }
}
