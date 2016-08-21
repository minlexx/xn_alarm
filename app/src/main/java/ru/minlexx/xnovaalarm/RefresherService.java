package ru.minlexx.xnovaalarm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import ru.minlexx.xnovaalarm.ifaces.IMainActivity;
import ru.minlexx.xnovaalarm.net.MyCookieStore;
import ru.minlexx.xnovaalarm.pojo.XNFlight;


public class RefresherService extends Service implements AuthTask.AuthResultListener {

    private static final String TAG = RefresherService.class.getName();
    public static final String EXTRA_REFRESH_INTERVAL =
            "ru.minlexx.xnovaalarm.INTENT_EXTRA_REFRESH_INTERVAL";
    public static final String EXTRA_VIBRATE_ON_NEW_MESSAGES =
            "ru.minlexx.xnovaalarm.INTENT_EXTRA_VIBRATE_ON_NEW_MESSAGES";

    // Notifications
    private final int NOTIFICATION_ID = R.string.local_service_started;
    private final int NOTIFICATION_ID_ALARM = R.string.attack_alarm;
    private NotificationManager mNM = null;
    private Notification m_serviceNotification = null;

    private final LocalBinder mBinder = new LocalBinder();
    private boolean m_is_started = false;

    private IMainActivity m_mainActivity = null;

    // cookies
    private static final String COOKIES_PREFS_FILENAME = "cookies";
    private MyCookieStore m_cookieStore = null;
    private CookieManager m_cookMgr = null;
    boolean m_loginOk = false;

    // timer and Overview refresher
    private Timer m_timer = null;
    private OverviewRefreshTask m_refreshTask = null;
    private int m_refreshInterval = 15; // default - 15 minutes
    private long m_lastUpdateTime = 0;
    private boolean m_vibrateOnNewMsgs = true;

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

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        m_serviceNotification = this.createServiceNotification();
        this.doInitializeCookies();
        Log.d(TAG, "onCreate(): cookies manager init complete");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        //
        onDestroy_handler();
        //
        m_mainActivity = null;
        m_cookieStore = null;
        m_cookMgr = null;
        m_loginOk = false;
        m_serviceNotification = null;
    }

    protected void onDestroy_handler() {
        stopForeground(true); // true = remove notification
        hideNotification();
        hideNotification_AM();
        // store cookies to SharedPreferences
        SharedPreferences prefs = getSharedPreferences(COOKIES_PREFS_FILENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefs_editor = prefs.edit();
        m_cookieStore.storeCookiesTo(prefs_editor);
        prefs_editor.apply();
        Log.d(TAG, "onDestroy_handler(): stored cookies");
        //
        // this is where real actions are stopped
        if (m_refreshTask != null) {
            m_refreshTask.cancel();
            m_refreshTask = null;
            Log.d(TAG, "onDestroy_handler(): stopped refresh task");
        }
        if (m_timer != null) {
            m_timer.cancel();
            m_timer = null;
            Log.d(TAG, "onDestroy_handler(): stopped timer");
        }
        //
        m_is_started = false; // mark self as stopped
    }

    public void please_stopSelf() {
        stopSelf();
        onDestroy_handler();
        if (m_mainActivity != null)
            m_mainActivity.notifyServiceStateChange();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Starts the service and periodic overview refresher.
     * Also marks self as foreground service
     * @param intent ignored here
     * @param flags ignored here
     * @param startId also ignored here
     * @return START_STICKY (this service is important, restart it)
     */
    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        // care, intent may be null
        Log.i(TAG, "onStartCommand(): Received start id " + startId + ": " +
                ((intent != null) ? intent.toString(): "No intent"));
        //
        m_is_started = true;
        //
        // mark self as foreground service
        this.startForeground(NOTIFICATION_ID, m_serviceNotification);

        // showNotification();
        // ^^ not needed for foreground service as notification is
        // already shown in onCreate()
        if (this.m_mainActivity != null)
            this.m_mainActivity.notifyServiceStateChange();
        // get param
        if (intent != null) {
            m_refreshInterval = intent.getIntExtra(EXTRA_REFRESH_INTERVAL, 15);
            m_vibrateOnNewMsgs = intent.getBooleanExtra(EXTRA_VIBRATE_ON_NEW_MESSAGES, true);
        }
        //
        // use timer
        Log.d(TAG, String.format(Locale.getDefault(),
                "Will run timer task after 500 ms, interval %d min...", m_refreshInterval));
        if (m_timer == null) {
            m_timer = new Timer("OverviewRefreshTimer", false);
        }
        if (m_refreshTask == null) {
            m_refreshTask = new OverviewRefreshTask();
        }
        // execute task every 10 minutes after a 0.5 sec delay
        m_timer.schedule(m_refreshTask, 500L, m_refreshInterval*60*1000L);

        return START_STICKY;
    }

    protected void doInitializeCookies() {
        m_cookieStore = new MyCookieStore();
        m_cookMgr = new CookieManager(m_cookieStore, CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(m_cookMgr);
        // restore cookies from SharedPreferences
        SharedPreferences prefs = getSharedPreferences(COOKIES_PREFS_FILENAME, Context.MODE_PRIVATE);
        m_cookieStore.loadCookiesFrom(prefs);
    }

    public boolean isStarted() { return m_is_started; }

    public boolean isAuthorized() {
        // either we just had a successful login attempt,
        // or cookies were just loaded from persistent storage
        return m_loginOk || m_cookieStore.doWeHaveAllLoginCookies();
    }

    public void set_mainActivity(IMainActivity mainActivity) {
        this.m_mainActivity = mainActivity;
    }

    protected Notification createServiceNotification() {
        return this.createServiceNotification(null);
    }

    protected Notification createServiceNotification(CharSequence contentText) {
        // we will use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.local_service_started);
        // intent to launch Main activity on notification click
        Intent activityIntent = new Intent(this, Main.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, activityIntent, 0);
        // build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.service_icon);  // the status icon
        builder.setTicker(text);  // the status text
        builder.setWhen(System.currentTimeMillis());  // the time stamp
        builder.setContentTitle(getText(R.string.local_service_label));  // the label of the entry
        // the contents of the entry
        if (contentText == null)
            builder.setContentText(text);
        else
            builder.setContentText(contentText);
        builder.setContentIntent(contentIntent);  // The intent to send when the entry is clicked
        builder.setOngoing(true);
        builder.build();
        return builder.build();
    }

    private synchronized void updateServiceNotification() {
        //long curTime = System.currentTimeMillis();
        //long secsPassed = (curTime - m_lastUpdateTime) / 1000;
        //
        StringBuilder content = new StringBuilder();
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(m_lastUpdateTime);
        CharSequence s_lastUpdate = getText(R.string.last_update);
        content.append(s_lastUpdate);
        content.append(" ");
        content.append(String.format(Locale.getDefault(),
                "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)));
        m_serviceNotification = this.createServiceNotification(content.toString());
        //
        this.showNotification();
    }

    private void showNotification() {
        if (m_serviceNotification != null)
            mNM.notify(NOTIFICATION_ID, m_serviceNotification);
    }

    /**
     * Display notification about incoming attack or new messages
     * @param flight - closest incoming enemy flight
     * @param new_messages - new messages count
     */
    protected void showNotification_AM(XNFlight flight, int new_messages) {
        Log.i(TAG, String.format(Locale.getDefault(),
                "showNotification_AM(flight=\"%s\", new_msgs=%d)",
                (flight != null ? flight.toString() : "no flight"), new_messages));
        //
        CharSequence text = getText(R.string.attack_alarm);
        CharSequence title = getText(R.string.xnova_alarm);
        // for multi-line notification
        String flights_line = null; // 1st line
        String messages_line = null; // 2nd line
        // for single-line notification
        StringBuilder contents = new StringBuilder();
        if (flight != null) {
            flights_line = ""; // not null!
            int secs = flight.timeLeft;
            int hrs = secs / 3600;
            secs -= hrs / 3600;
            int mins = secs / 60;
            secs -= mins * 60;
            //
            if (hrs > 0) {
                flights_line += String.valueOf(hrs);
                flights_line += getText(R.string.hrs);
                flights_line += " ";
            }
            if (mins > 0) {
                flights_line += String.valueOf(mins);
                flights_line += getText(R.string.mins);
                flights_line += " ";
            }
            flights_line += String.valueOf(secs);
            flights_line += getText(R.string.secs);
            flights_line += " ";
            flights_line += getText(R.string.till_attack).toString();
            contents.append(flights_line);
        }
        if (new_messages > 0) {
            messages_line = String.format(Locale.getDefault(),
                    getText(R.string.new_messages).toString(),  // "%d new messages.",
                    new_messages);
            if (contents.length() > 0) contents.append("; ");
            contents.append(messages_line);
        }
        //
        long[] vibPattern = {500,500,500,500,500,500,500,500,500};
        //
        NotificationCompat.InboxStyle nstyle = new NotificationCompat.InboxStyle();
        if (flights_line != null) nstyle.addLine(flights_line);
        if (messages_line != null)  nstyle.addLine(messages_line);
        nstyle.setBigContentTitle(title); // his a real title
        nstyle.setSummaryText(text); // this is the lowest line
        //
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.service_icon); // the status icon
        builder.setTicker(text); // the status text
        builder.setWhen(System.currentTimeMillis()); // time stamp
        builder.setContentTitle(title); // label of the entry
        builder.setContentText(contents.toString()); // contents of the entry
        builder.setOngoing(false);
        builder.setLights(Color.BLUE, 500, 500);
        // Vibrate ONLY if:
        //   1) either there is an enemy attack
        //   2) or there are new messages and we are allowed to vibrate in settings
        if ((flight != null) ||
                ((new_messages > 0)  &&  m_vibrateOnNewMsgs))
            builder.setVibrate(vibPattern);
        builder.setStyle(nstyle);
        // set notification sound only if there is an attacking flight
        if (flight != null) {
            //Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            builder.setSound(soundUri);
        }
        //
        Notification notification = builder.build();
        mNM.notify(NOTIFICATION_ID_ALARM, notification);
    }

    private void hideNotification() {
        if (mNM != null) {
            mNM.cancel(NOTIFICATION_ID);
        }
    }

    private void hideNotification_AM() {
        if (mNM != null) {
            mNM.cancel(NOTIFICATION_ID_ALARM);
        }
    }

    ///////////////////////////////////////////////////////////////
    // XNova login

    public void beginXNovaLogin(String login, String password) {
        AuthTask at = new AuthTask(this, login, password);
        at.execute("");
    }

    @Override
    public void onXNovaLoginOK() {
        m_loginOk = true; // we had a ssuccess ful login attempt
        //
        Log.i(TAG, "Login OK!");
        //final CharSequence toastText = getResources().getText(R.string.login_ok, "Login OK!");
        //Toast tst = Toast.makeText(this, toastText, Toast.LENGTH_SHORT);
        Toast tst = Toast.makeText(this, R.string.login_ok, Toast.LENGTH_SHORT);
        tst.show();
        // notify activity
        if (m_mainActivity != null)
            m_mainActivity.notifyServiceStateChange();
    }

    @Override
    public void onXNovaLoginFail(String errorStr) {
        m_loginOk = false; // login failed!
        //
        if ((errorStr != null) && (errorStr.length() > 0)) {
            Log.e(TAG, "Login error: " + errorStr);
            Toast tst = Toast.makeText(this, errorStr, Toast.LENGTH_SHORT);
            tst.show();
        }
        // notify activity
        if (m_mainActivity != null)
            m_mainActivity.notifyServiceStateChange();
    }

    //////////////////////////////////////////////////////////
    // Overview reresher

    public class OverviewRefreshTask extends TimerTask {

        private final String ITAG = OverviewRefreshTask.class.getName();
        private final String XN_HOST = "uni5.xnova.su";

        private int m_newMessagesCount = 0;
        private List<XNFlight> m_flights = null;

        @Override
        public void run() {
            Log.d(ITAG, "run() !");
            //
            try {
                String overview_content = download_overview();
                parse_overview(overview_content);
                process_results();
            } catch (Exception e) {
                Log.e(ITAG, "Refresh overview failed!", e);
            }
        }

        protected String download_overview() {
            RefresherService.this.hideNotification_AM();
            //
            URL xn_url;
            HttpURLConnection conn = null;
            StringBuilder response = new StringBuilder();
            String s;
            //
            try {
                xn_url = new URL("http", XN_HOST, 80, "/overview/");
                Log.d(ITAG, "Downloading: " + xn_url.toString());
                conn = (HttpURLConnection) xn_url.openConnection();
                // connection parameters
                conn.setRequestProperty("Referer", "http://uni5.xnova.su/overview/");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/50.0.2661.102 Safari/537.36");
                //
                InputStreamReader ins = new InputStreamReader(conn.getInputStream());
                BufferedReader bufr = new BufferedReader(ins);
                while ((s = bufr.readLine()) != null) {
                    response.append(s);
                }
                bufr.close();
                conn.disconnect();
                conn = null;
                RefresherService.this.m_lastUpdateTime = System.currentTimeMillis();
            } catch (IOException ioe) {
                Log.e(ITAG, "Failed to download overview!", ioe);
            } finally {
                if (conn != null)
                    conn.disconnect();
            }
            //
            return response.toString();
        }

        protected void parse_overview(String html) {
            m_newMessagesCount = 0;
            if (m_flights == null) {
                m_flights = new ArrayList<>(20);
            }
            m_flights.clear();
            //
            Document doc = Jsoup.parse(html, "https://uni5.xnova.su/");
            Log.d(ITAG, "Title: " + doc.title());

            // get new messages count
            // <span class="sprite ico_mail"></span> <b>0</b>
            Elements spans_mail = doc.select("span.ico_mail");
            if (spans_mail != null) {
                for (Element span : spans_mail) {
                    Element bold = span.nextElementSibling();
                    if (bold != null) {
                        String snum_mails = bold.text();
                        m_newMessagesCount = Integer.parseInt(snum_mails);
                    }
                }
            }

            // get flights
            // <th class="text-xs-left" colspan="3">
            // first child: <span class="flight owntransport"> ...
            // next sibling: <script>FlotenTime('bxxfs4', 1636);</script>
            Elements ths = doc.select("th.text-xs-left");
            Pattern regex_pattern = Pattern.compile("(\\d+)\\);");
            //
            if (ths != null) {
                for (Element th: ths) {
                    Element script = th.nextElementSibling();
                    if (script != null) {
                        String script_text = script.data();
                        System.out.printf("Found flight: %s (%s)\n",
                                script.toString(), script_text);
                        XNFlight flight = new XNFlight();
                        //
                        // get flight time left from script tag
                        script_text = script_text.substring(21); // = "1636);"
                        Matcher regex_matcher = regex_pattern.matcher(script_text);
                        if (regex_matcher.find()) {
                            String s_flotenTime = regex_matcher.group(1);
                            flight.timeLeft = Integer.parseInt(s_flotenTime);
                        }

                        // get flight span
                        Element span = th.child(0);
                        if ((span != null) && (span.nodeName().equals("span"))) {
                            // get flight mission from span class
                            // examples: attack espionage / flight return
                            // <span class="flight ownespionage">
                            // <span class="return ownespionage">
                            Set<String> span_classes = span.classNames();
                            if (span_classes != null) {
                                for (String span_class: span_classes) {
                                    if (!span_class.equals("flight") && !span_class.equals("return"))
                                        flight.mission = span_class;
                                    if (span_class.equals("return"))
                                        flight.isReturn = true;
                                }
                            }
                        }
                        //
                        m_flights.add(flight);
                    } else {
                        Log.d(ITAG, "Flight with no floten-time");
                    }

                    // statistics output
                    Log.d(ITAG, "=================================");
                    for (XNFlight fl: m_flights) {
                        Log.d(ITAG, "Flight " + fl.toString());
                    }
                }
            }
        }

        protected void process_results() {
            XNFlight shortest_flight = null;
            int min_time = -1;
            for (XNFlight fl: m_flights) {
                // only attacking flights
                if (fl.isEnemyAttack()) {
                    int time_left = fl.timeLeft;
                    if (min_time == -1) {
                        // first result ever
                        min_time = time_left;
                        shortest_flight = fl;
                    } else {
                        // is new result shorter that current
                        if (time_left < min_time) {
                            min_time = time_left;
                            shortest_flight = fl;
                        }
                    }
                }
            }

            if ((shortest_flight != null) || (m_newMessagesCount > 0)) {
                // either we have attacking flight, or we have new message(s)
                RefresherService.this.showNotification_AM(shortest_flight, m_newMessagesCount);
            } else {
                Log.d(ITAG, "process_results(): no incoming attacks or new messages.");
            }
            // update general service notification
            RefresherService.this.updateServiceNotification();
        }
    }
}
