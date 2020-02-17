package kilanny.autocaller.services;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.telephony.TelephonyManager;
import android.text.format.Time;
import android.util.Log;
import android.util.Pair;
import android.view.WindowManager;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.AlarmManagerCompat;
import androidx.core.app.NotificationCompat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import kilanny.autocaller.App;
import kilanny.autocaller.data.AutoCallLog;
import kilanny.autocaller.data.AutoCallProfile;
import kilanny.autocaller.data.AutoCallProfileList;
import kilanny.autocaller.data.AutoCallSession;
import kilanny.autocaller.data.City;
import kilanny.autocaller.data.CityList;
import kilanny.autocaller.di.ContextComponent;
import kilanny.autocaller.di.ContextModule;
import kilanny.autocaller.di.DaggerContextComponent;
import kilanny.autocaller.intents.AutoCallPhoneStateListener;
import kilanny.autocaller.data.ContactsList;
import kilanny.autocaller.data.ContactsListGroup;
import kilanny.autocaller.data.ContactsListGroupList;
import kilanny.autocaller.data.ContactsListItem;
import kilanny.autocaller.data.ListOfCallingLists;
import kilanny.autocaller.activities.MainActivity;
import kilanny.autocaller.R;
import kilanny.autocaller.utils.PrayTimes;
import kilanny.autocaller.utils.TextUtils;

/**
 * Created by ibraheem on 5/11/2017.
 */
public class AutoCallService extends Service {

    private static AutoCallService _lastInstance;

    public static final int MESSAGE_EXIT = -123456;

    static final int NOTIFICATION_ID = 13051992;
    private static final int WAIT_BETWEEN_CALLS_SECONDS = 5;
    private static final int PHONE_APP_CRASH_NOTIFICATION_TIMEOUT = 80;
    //private static final int NO_REPLY_TIMEOUT_SECONDS = 30 + WAIT_BETWEEN_CALLS_SECONDS;
    //private static final int KILL_CALL_AFTER_SECONDS = NO_REPLY_TIMEOUT_SECONDS + 35;

    @Inject
    ListOfCallingLists listOfCallingLists;
    @Inject
    CityList cityList;
    @Inject
    AutoCallProfileList profileList;
    private NotificationManager mNM;
    private NotificationCompat.Builder notificationBuilder;
    private AtomicBoolean canMakeCalls, isPaused;
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private Handler runOnUiThreadHandler;
    private int lastServiceStartId = -1;
    private RemoteViews notificationRemoteViews;
    private BroadcastReceiver phoneStatusChangedBroadcastReceiver;

    private AutoCallSession callSession;
    private boolean pref_cityBasedRingEnabled;
    private int pref_autoRecallListNo;
    private int myTimezoneOffset;
    private boolean lastCallTerminatedByApp = false;
    private int lastCallState = -1;
    private final Map<Integer, Pair<Integer, Integer>> cachedCitiesTimes = new HashMap<>();

    private AutoCallLog.AutoCallSession currentSession;
    private ContactsList list;
    private Timer autoHangupTimer, phoneAppCrashNotificationTimer;
    static MediaPlayer mediaPlayer;

    /**
     * Handler of incoming messages from clients.
     */
    private static class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_EXIT:
                    myStopSelf();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private class AlarmBroadCastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            pendingRecallRunnable.run();
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    private static void myStopSelf() {
        if (_lastInstance != null) {
            if (_lastInstance.phoneStatusChangedBroadcastReceiver != null) {
                _lastInstance.unregisterReceiver(_lastInstance.phoneStatusChangedBroadcastReceiver);
                _lastInstance.phoneStatusChangedBroadcastReceiver = null;
            }
            if (_lastInstance.pendingRecallRunnable != null) {
                _lastInstance.runOnUiThreadHandler.removeCallbacks(_lastInstance.pendingRecallRunnable);
                _lastInstance.pendingRecallRunnable = null;
            }
            if (_lastInstance.lastServiceStartId != -1) {
                int tmp = _lastInstance.lastServiceStartId;
                _lastInstance.lastServiceStartId = -1;
                _lastInstance.stopSelf(tmp);
                //_lastInstance.mServiceLooper.quit();
                //_lastInstance.mServiceLooper.getThread().stop();
            }
        }
    }

    public static class StopButtonListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (_lastInstance != null) {
                _lastInstance.isPaused.set(false);
                _lastInstance.canMakeCalls.set(false);
                _lastInstance.stopAutoCall(true);
            }
            Toast.makeText(context, R.string.stopping_auto_calls, Toast.LENGTH_SHORT).show();
        }
    }

    public static class PauseButtonListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (_lastInstance != null) {
                if (_lastInstance.canMakeCalls.get()) {
                    _lastInstance.updateNotificationTextView(R.id.btnPauseCalls,
                            _lastInstance.getString(R.string.resume_calls));
                    Toast.makeText(context, R.string.pausing_auto_calls, Toast.LENGTH_SHORT).show();
                } else {
                    _lastInstance.updateNotificationTextView(R.id.btnPauseCalls,
                            _lastInstance.getString(R.string.pause_calls));
                    Toast.makeText(context, R.string.resuming_auto_calls, Toast.LENGTH_SHORT).show();
                }
                _lastInstance.isPaused.set(!_lastInstance.isPaused.get());
                _lastInstance.canMakeCalls.set(!_lastInstance.canMakeCalls.get());
                if (!_lastInstance.isPaused.get()) {
                    _lastInstance.nextCall();
                }
            }
        }
    }

    private void runOnUiThread(Runnable runnable) {
        runOnUiThreadHandler.post(runnable);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel() {
        String channelId = "kilanny.autocaller.AutoCallService";
        String channelName = "Auto Call Service";
        NotificationChannel channel = new NotificationChannel(channelId, channelName,
                NotificationManager.IMPORTANCE_HIGH);
        channel.setLightColor(Color.BLUE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        mNM.createNotificationChannel(channel);
        return channelId;
    }

    private void initNotification() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        notificationRemoteViews = new RemoteViews(getPackageName(), R.layout.notification_statusbar);

        //this is the intent that is supposed to be called when the button is clicked
        Intent stopIntent = new Intent(this, StopButtonListener.class);
        PendingIntent pendingStopIntent = PendingIntent.getBroadcast(this,
                0, stopIntent, 0);
        notificationRemoteViews.setOnClickPendingIntent(R.id.btnStopCalls, pendingStopIntent);
        Intent pauseIntent = new Intent(this, PauseButtonListener.class);
        PendingIntent pendingPauseIntent = PendingIntent.getBroadcast(this,
                0, pauseIntent, 0);
        notificationRemoteViews.setOnClickPendingIntent(R.id.btnPauseCalls, pendingPauseIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = createNotificationChannel();
            notificationBuilder = new NotificationCompat.Builder(this, channelId);
        } else {
            notificationBuilder = new NotificationCompat.Builder(this);
        }
        notificationBuilder.setContentIntent(pendingIntent)
                .setContent(notificationRemoteViews)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.mipmap.ic_launcher);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ContextComponent contextComponent = DaggerContextComponent.builder()
                .appComponent(App.get(this).getComponent())
                .contextModule(new ContextModule(this))
                .build();
        contextComponent.inject(this);
        _lastInstance = this;
        runOnUiThreadHandler = new Handler();

        initNotification();
        startForeground(NOTIFICATION_ID, notificationBuilder.getNotification());

        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
        registerReceiver(phoneStatusChangedBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int state = intent.getIntExtra("state", -1);
                Log.d("ServicePhoneListener", "State Received: " + state);
                boolean isDifferentCallState = lastCallState != state;
                lastCallState = state;
                if (canMakeCalls == null || !callSession.isStarted() || !isDifferentCallState) {
                    // just for handling the first time CALL_STATE_IDLE
                    // and also ignoring duplicated phone-state report
                } else if (TelephonyManager.CALL_STATE_IDLE == state) {
                    cancelCallHangupTimer(true);
                    if (!canMakeCalls.get()) {
                        Log.d("ServicePhoneListener", "canMakeCalls == false");
                        if (!isPaused.get())
                            stopAutoCall(true);
                    } else nextCall();
                } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                    App app = App.get(AutoCallService.this);
                    if (app.verifiedByOutgoingReceiver) {
                        app.lastOutgoingCallStartRinging = new Date();
                        runKillAutoCallTask(profileList.findById(app.lastNumberCallProfileId)
                                .killCallAfterSeconds);
                        // maybe the phone will crash also when the outgoing call OFFHOOK,
                        // so when need to schedule again to reset the period
                        // because the OFFHOOK may come late after callNumber().
                        runPhoneAppCrashNotificationTimer();
                    }
                }
            }
        }, new IntentFilter(AutoCallPhoneStateListener.BROADCAST_ACTION));
    }

    private void updateNotificationTextView(int txtView, String text) {
        notificationRemoteViews.setTextViewText(txtView, text);
        mNM.notify(NOTIFICATION_ID, notificationBuilder.getNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }
        int callListId;
        boolean isNewSession = false;
        if (intent.getBooleanExtra("continueLastSession", false)) {
            callSession = AutoCallSession.getLastSession(this);
            callListId = callSession.contactsListId;
            list = listOfCallingLists.getById(callListId);
            currentSession = list.getLog().sessions.get(list.getLog().sessions.size() - 1);
        } else {
            callListId = intent.getIntExtra("callListId", -1);
            list = listOfCallingLists.getById(callListId);
            callSession = new AutoCallSession(callListId, new Date(), this);
            isNewSession = true;

            String ignoredNumbers = intent.getStringExtra("ignoreNumbers");
            if (ignoredNumbers != null && ignoredNumbers.trim().length() > 0)
                for (String number : ignoredNumbers.split(","))
                    callSession.addNumberToRejectersList(number);
        }
        callSession.setStarted(false);
        if (isNewSession) {
            currentSession = new AutoCallLog.AutoCallSession();
            currentSession.date = new Date();
            list.getLog().sessions.add(currentSession);
            list.save(this);
        }
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        pref_cityBasedRingEnabled = pref.getBoolean("cityBasedRingEnabled", true);
        pref_autoRecallListNo = Integer.parseInt(pref.getString(
                "autoRecallListNo", getString(R.string.auto_recall_count_array_default_value)));
        myTimezoneOffset = TimeZone.getDefault().getOffset(new Date().getTime());
        updateNotificationTextView(R.id.callListName, list.getName());

        canMakeCalls = new AtomicBoolean(true);
        isPaused = new AtomicBoolean(false);
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Make sure our notification is gone.
        stopAutoCall(false);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    private boolean hasRejectedOrAnsweredOutgoingCall(String number, int callProfileId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
                        != PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        number = number.replace(" ", "").trim();
        Cursor cursor = getContentResolver().query(CallLog.Calls.CONTENT_URI,
                null, null, null, CallLog.Calls.DATE + " DESC");
        int num = cursor.getColumnIndex(CallLog.Calls.NUMBER);
        int type = cursor.getColumnIndex(CallLog.Calls.TYPE);
        //int date = cursor.getColumnIndex(CallLog.Calls.DATE);
        int duration = cursor.getColumnIndex(CallLog.Calls.DURATION);
        try {
            if (cursor.moveToNext()) {
                String phNumber = cursor.getString(num).replace(" ", "").trim();
                if (!phNumber.endsWith(number) && !number.endsWith(phNumber)) {
                    Log.d("CALL_CHECK", "false: numbers does not match: " + phNumber
                            + " , " + number);
                    return false;
                }
                String callType = cursor.getString(type);
                //Date callDayTime = new Date(Long.valueOf(cursor.getString(date)));
                String callDuration = cursor.getString(duration);
                long diffSeconds = 0;
                App app = App.get(this);
                if (app.lastOutgoingCallStartRinging != null) {
                    diffSeconds = (new Date().getTime() -
                            app.lastOutgoingCallStartRinging.getTime()) / 1000;
                }
                switch (Integer.parseInt(callType)) {
                    case 5: // rejected
                        Log.d("CALL_CHECK", "true: rejected (status 5)");
                        return true;
                    case CallLog.Calls.OUTGOING_TYPE: {
                        AutoCallProfile profile = profileList.findById(callProfileId);
                        if (Integer.parseInt(callDuration) > 0 || diffSeconds < profile.noReplyTimeoutSeconds) {
                            Log.d("CALL_CHECK", "true: callDuration = " + callDuration
                                    + ", diffSeconds = " + diffSeconds);
                            return true;
                        }
                        break;
                    }
                    default:
                        Log.d("CALL_CHECK", "false: unknown type: " + callType);
                        return false;
                }
            }
            Log.d("CALL_CHECK", "false: no reply");
            return false;
        } finally {
            cursor.close();
        }
    }

    private boolean numberInRejectedNumbers(ContactsListGroupList groups, String number) {
        for (ContactsListGroup g : groups) // search in all groups
            if (g.contacts.containsKey(number)) //which I'm member of
                for (String num : g.contacts.keySet()) //search all members of that group
                    if (callSession.containsNumberInRejectersList(num)) //if someone already rejected
                        return true;
        return false;
    }

    /**
     * http://stackoverflow.com/a/8380418/3441905
     */
    private boolean terminateActiveCall() {
        try {
            //String serviceManagerName = "android.os.IServiceManager";
            String serviceManagerName = "android.os.ServiceManager";
            String serviceManagerNativeName = "android.os.ServiceManagerNative";
            String telephonyName = "com.android.internal.telephony.ITelephony";

            Class telephonyClass;
            Class telephonyStubClass;
            Class serviceManagerClass;
            Class serviceManagerNativeClass;

            Method telephonyEndCall;
            // Method getService;
            Object telephonyObject;
            Object serviceManagerObject;

            telephonyClass = Class.forName(telephonyName);
            telephonyStubClass = telephonyClass.getClasses()[0];
            serviceManagerClass = Class.forName(serviceManagerName);
            serviceManagerNativeClass = Class.forName(serviceManagerNativeName);

            Method getService = serviceManagerClass.getMethod("getService", String.class);

            Method tempInterfaceMethod = serviceManagerNativeClass.getMethod("asInterface",
                    IBinder.class);

            Binder tmpBinder = new Binder();
            tmpBinder.attachInterface(null, "fake");

            serviceManagerObject = tempInterfaceMethod.invoke(null, tmpBinder);
            IBinder retbinder = (IBinder) getService.invoke(serviceManagerObject, "phone");
            Method serviceMethod = telephonyStubClass.getMethod("asInterface", IBinder.class);

            telephonyObject = serviceMethod.invoke(null, retbinder);
            //telephonyCall = telephonyClass.getMethod("call", String.class);
            telephonyEndCall = telephonyClass.getMethod("endCall");
            //telephonyAnswerCall = telephonyClass.getMethod("answerRingingCall");

            telephonyEndCall.invoke(telephonyObject);

            Log.i("terminateActiveCall", "Successfully terminated current call");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(AutoCallService.this, R.string.automatically_terminated_call,
                            Toast.LENGTH_LONG).show();
                }
            });
            return lastCallTerminatedByApp = true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("OutgoingCallReceiver", "Exception object: " + e);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(AutoCallService.this, R.string.automatic_terminate_call_failed,
                            Toast.LENGTH_LONG).show();
                }
            });
            return false;
        }
    }

    @Deprecated
    public boolean killCall(TelephonyManager telephonyManager) {
        try {
            // Get the getITelephony() method
            Class classTelephony = Class.forName(telephonyManager.getClass().getName());
            Method methodGetITelephony = classTelephony.getDeclaredMethod("getITelephony");

            // Ignore that the method is supposed to be private
            methodGetITelephony.setAccessible(true);

            // Invoke getITelephony() to get the ITelephony interface
            Object telephonyInterface = methodGetITelephony.invoke(telephonyManager);

            // Get the endCall method from ITelephony
            Class telephonyInterfaceClass =
                    Class.forName(telephonyInterface.getClass().getName());
            Method methodEndCall = telephonyInterfaceClass.getDeclaredMethod("endCall");

            // Invoke endCall()
            methodEndCall.invoke(telephonyInterface);

        } catch (Exception ex) { // Many things can go wrong with reflection calls
            Log.d("killCall", "PhoneStateReceiver **" + ex.toString());
            return false;
        }
        return true;
    }

    private void cancelCallHangupTimer(boolean shouldCancelAppCrashTimer) {
        if (shouldCancelAppCrashTimer)
            cancelPhoneAppCrashNotificationTimer();
        if (autoHangupTimer != null) {
            autoHangupTimer.cancel();
            autoHangupTimer = null;
        }
    }

    private void cancelPhoneAppCrashNotificationTimer() {
        if (phoneAppCrashNotificationTimer != null) {
            phoneAppCrashNotificationTimer.cancel();
            phoneAppCrashNotificationTimer = null;
        }
    }

    /**
     * TODO: fix: timers in service not working. Use AlarmService instead
     */
    private void runPhoneAppCrashNotificationTimer() {
        cancelPhoneAppCrashNotificationTimer();
        stopFinishRingtone();
        phoneAppCrashNotificationTimer = new Timer();
        phoneAppCrashNotificationTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.d("PhoneAppCrash", "PhoneApp Crash Detected. Playing warning sound...");
                playFinishTune();
            }
        }, PHONE_APP_CRASH_NOTIFICATION_TIMEOUT * 1000);
    }

    private void runKillAutoCallTask(int afterSeconds) {
        // in case a new call is made, and the previous call rejected, cancel the old timer
        cancelCallHangupTimer(false);
        autoHangupTimer = new Timer();
        autoHangupTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                //App application = App.get(AutoCallService.this);
                //callSession.addNumberToRejectersList(application.lastCallNumber);
                Log.d("runKillAutoCallTask", "Call period timed out. Terminating...");
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
                    terminateActiveCall();
            }
        }, afterSeconds * 1000);
    }

    private void updateLastCallResult(boolean ans) {
        //ignore the first result when retry calls has been chosen
        if (currentSession.get(currentSession.size() - 1)
                instanceof AutoCallLog.AutoCall) {
            Log.d("AutoCallItem", "calling result: " + ans
                    + ", index = " + callSession.getListCurrentCallItemIdx()
                    + ", count = " + callSession.getListCurrentCallItemCount());
            ((AutoCallLog.AutoCall) currentSession.get(currentSession.size() - 1)).result = ans ?
                    AutoCallLog.AutoCall.RESULT_ANSWERED_OR_REJECTED :
                    AutoCallLog.AutoCall.RESULT_NOT_ANSWERED;
        }
    }

    private Runnable pendingRecallRunnable;

    private void nextCall() {
        if (App.get(this).verifiedByOutgoingReceiver) {
            cancelCallHangupTimer(true);
            // call after 1 seconds, to enable the phone to update the call log
            // for the last call
            Executors.newSingleThreadScheduledExecutor().schedule(new Runnable() {
                @Override
                public void run() {
                    ContactsListGroupList groups = list.getGroups();
                    try {
                        boolean ans = false;
                        boolean finished = false;
                        App app = App.get(AutoCallService.this);
                        if (!app.verifiedByOutgoingReceiver || !callSession.isStarted())
                            return; // stopped by user
                        ContactsListItem listItem = list.get(callSession.getListCurrentCallItemIdx());
                        try {
                            // check even the current number, maybe sunrised and cannot inc count
                            if ((!lastCallTerminatedByApp && (ans = hasRejectedOrAnsweredOutgoingCall(listItem.number, app.lastNumberCallProfileId)))
                                    || (listItem.cityId != null && getCanCallStatus(listItem.cityId) != AutoCallLog.AutoCallIgnored.RESULT_NOT_IGNORED)
                                    || 1 + callSession.getListCurrentCallItemCount() > listItem.callCount) {

                                if (ans)
                                    callSession.addNumberToRejectersList(listItem.number);

                                callSession.incrementListCurrentCallItemIdx();
                                callSession.setListCurrentCallItemCount(1);

                                while (callSession.getListCurrentCallItemIdx() < list.size()) {
                                    listItem = list.get(callSession.getListCurrentCallItemIdx());
                                    if (!callSession.containsNumberInRejectersList(listItem.number)
                                            && !numberInRejectedNumbers(groups, listItem.number)) {
                                        byte canCall = listItem.cityId != null ? getCanCallStatus(listItem.cityId)
                                                : AutoCallLog.AutoCallIgnored.RESULT_NOT_IGNORED;
                                        if (canCall != AutoCallLog.AutoCallIgnored.RESULT_NOT_IGNORED) {
                                            updateLastCallResult(ans);
                                            addSessionIgnoredCall(new Date(), listItem.name, listItem.number, canCall);
                                        } else break;
                                    }
                                    callSession.incrementListCurrentCallItemIdx();
                                }
                                if (callSession.getListCurrentCallItemIdx() >= list.size()) {
                                    finished = true;
                                    return;
                                }
                            } else
                                callSession.incrementListCurrentCallItemCount();
                        } finally {
                            updateLastCallResult(ans);
                            if (finished) {
                                int firstNotAnsweredIdx = -1;
                                boolean allBeforeFajr = true;
                                String minFajrTime = null;
                                for (int i = 0; i < list.size(); ++i) {
                                    listItem = list.get(i);
                                    if (!callSession.containsNumberInRejectersList(listItem.number)
                                            && !numberInRejectedNumbers(groups, listItem.number)) {
                                        byte canCall = listItem.cityId != null ? getCanCallStatus(listItem.cityId)
                                                : AutoCallLog.AutoCallIgnored.RESULT_NOT_IGNORED;
                                        if (canCall != AutoCallLog.AutoCallIgnored.RESULT_IGNORED_AFTER_SUNRISE) {
                                            if (firstNotAnsweredIdx == -1)
                                                firstNotAnsweredIdx = i;
                                            if (canCall == AutoCallLog.AutoCallIgnored.RESULT_NOT_IGNORED)
                                                allBeforeFajr = false;
                                            else {
                                                String time = getFajrTime(listItem.cityId);
                                                if (minFajrTime == null || minFajrTime.compareTo(time) > 0)
                                                    minFajrTime = time;
                                            }
                                        }
                                    }
                                }
                                if (firstNotAnsweredIdx >= 0) {
                                    final int tmpIdx = firstNotAnsweredIdx;
                                    final Runnable recallRunnable = new Runnable() {
                                        @Override
                                        public void run() {
                                            pendingRecallRunnable = null;
                                            callSession.setListCurrentCallItemCount(0);
                                            callSession.setListCurrentCallItemIdx(tmpIdx);
                                            currentSession.add(new AutoCallLog.AutoCallRetry());
                                            list.save(AutoCallService.this);
                                            nextCall();
                                        }
                                    };
                                    if (pref_autoRecallListNo > callSession.getListAutoRecallCount()) {
                                        callSession.setListAutoRecallCount(
                                                callSession.getListAutoRecallCount() + 1);
                                        recallRunnable.run();
                                    } else {
                                        final boolean _allBeforeFajr = allBeforeFajr;
                                        final String _minFajr = minFajrTime;
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                AlertDialog dlg;
                                                if (_allBeforeFajr) {
                                                    dlg = new AlertDialog.Builder(getApplicationContext())
                                                            .setTitle(R.string.some_not_answered_title)
                                                            .setCancelable(false)
                                                            .setMessage(String.format(getString(R.string.all_before_fajr_message), _minFajr))
                                                            .setIcon(android.R.drawable.ic_dialog_info)
                                                            .setPositiveButton(R.string.retry_now, new DialogInterface.OnClickListener() {

                                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                                    stopFinishRingtone();
                                                                    recallRunnable.run();
                                                                }
                                                            })
                                                            .setNeutralButton(R.string.schedule_recall, new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialog, int which) {
                                                                    stopFinishRingtone();
                                                                    updateNotificationTextView(R.id.currentCallee, getString(R.string.waiting_fajr));
                                                                    Time dtNow = new Time();
                                                                    dtNow.setToNow();
                                                                    int currentTime = dtNow.hour * 60 + dtNow.minute;
                                                                    String sTime[] = _minFajr.split(":");
                                                                    int nextTime = Integer.parseInt(sTime[0]) * 60 +
                                                                            Integer.parseInt(sTime[1]);
                                                                    long time = (nextTime - currentTime + 1) * 60 * 1000;
                                                                    time += new Date().getTime();
                                                                    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                                                                    Intent intentToFire = new Intent(AutoCallService.this,
                                                                            AlarmBroadCastReceiver.class);
                                                                    pendingRecallRunnable = recallRunnable;
                                                                    AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager,
                                                                            AlarmManager.RTC_WAKEUP, time,
                                                                            PendingIntent.getBroadcast(AutoCallService.this,
                                                                                    1, intentToFire, PendingIntent.FLAG_UPDATE_CURRENT));
                                                                    Log.v("schNext", "Scheduled next alarm at " + new Date(time));
                                                                }
                                                            })
                                                            .setNegativeButton(R.string.stop_calls, new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialog, int which) {
                                                                    stopFinishRingtone();
                                                                    stopAutoCall(true);
                                                                }
                                                            }).create();
                                                } else {
                                                    dlg = new AlertDialog.Builder(getApplicationContext())
                                                            .setTitle(R.string.some_not_answered_title)
                                                            .setCancelable(false)
                                                            .setMessage(R.string.some_not_answered_body)
                                                            .setIcon(android.R.drawable.ic_dialog_info)
                                                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                                    stopFinishRingtone();
                                                                    recallRunnable.run();
                                                                }
                                                            })
                                                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialog, int which) {
                                                                    stopFinishRingtone();
                                                                    stopAutoCall(true);
                                                                }
                                                            }).create();
                                                }
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                    dlg.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
                                                } else {
                                                    dlg.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                                                }
                                                dlg.show();
                                                playFinishTuneIfRequired();
                                            }
                                        });
                                    }
                                } else {
                                    playFinishTuneIfRequired();
                                    stopAutoCall(true);
                                }
                            }
                        }
                        listItem = list.get(callSession.getListCurrentCallItemIdx());
                        Integer profile = listItem.callProfileId;
                        if (profile == null)
                            profile = AutoCallProfileList.DEFAULT_PROFILE_ID;
                        callNumber(listItem.number, listItem.name, callSession.getListCurrentCallItemCount(),
                                listItem.callCount, profile);
                    } catch (Exception ex) { //prevent phone app crash
                        ex.printStackTrace();
                        onFatalError(ex);
                    }
                }
            }, WAIT_BETWEEN_CALLS_SECONDS, TimeUnit.SECONDS);
        } else
            Log.d("nextCall", "Not verified by outgoing listener; ignoring");
    }

    private void onFatalError(Exception ex) {
        AlertDialog dlg = new AlertDialog.Builder(getApplicationContext())
                .setTitle(R.string.fatal_exception_dlg_title)
                .setCancelable(false)
                .setMessage(R.string.fatal_exception_dlg_msg + "\n" + ex)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        stopFinishRingtone();
                        stopAutoCall(true);
                    }
                })
                .create();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dlg.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        } else {
            dlg.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        }
        dlg.show();
        playFinishTune();
    }

    public static void stopFinishRingtone() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void playFinishTuneIfRequired() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (pref.getBoolean("playFinishTune", true)) {
            playFinishTune();
        }
    }

    private void playFinishTune() {
        stopFinishRingtone();
        mediaPlayer = MediaPlayer.create(this, R.raw.finish_call);
        mediaPlayer.setLooping(false);
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        float vol = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
        mediaPlayer.setVolume(vol, vol);
        mediaPlayer.start();
    }

    private void stopAutoCall(boolean shouldStopSelf) {
        cancelCallHangupTimer(true);
        //iHaveStartedTheOutgoingCall = false;
        AutoCallSession.clear(this);
        App app = App.get(this);
        app.lastCallNumber = null;
        app.verifiedByOutgoingReceiver = false;
        app.lastNumberCallProfileId = 0;
        if (list != null) {
            list.save(this);
            list = null;
        }
        currentSession = null;
        callSession = null;
        cachedCitiesTimes.clear();

        if (shouldStopSelf) {
            myStopSelf();
        }
    }

    private void addSessionIgnoredCall(Date date, String name, String number, byte result) {
        AutoCallLog.AutoCallIgnored autoCall = new AutoCallLog.AutoCallIgnored();
        autoCall.date = date;
        autoCall.name = name;
        autoCall.number = number;
        autoCall.result = result;
        currentSession.add(autoCall);
        list.save(this);
    }

    private void addSessionCall(Date date, String name, String number) {
        AutoCallLog.AutoCall autoCall = new AutoCallLog.AutoCall();
        autoCall.date = date;
        autoCall.name = name;
        autoCall.number = number;
        currentSession.add(autoCall);
        list.save(this);
    }

    private void callNumber(String number, String name, int currentCallCount, int totalCallCount,
                            int callProfileId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                    != PackageManager.PERMISSION_GRANTED) {
                throw new IllegalStateException("Permissions must be fully granted to current app before starting the service");
            }
        }

        //iHaveStartedTheOutgoingCall = true;
        App app = App.get(this);
        app.lastOutgoingCallStartRinging = null;
        app.lastNumberCallProfileId = callProfileId;
        app.lastCallNumber = number;
        app.lastCallName = name;
        app.lastCallCurrentCount = currentCallCount;
        app.lastCallTotalCount = totalCallCount;
        app.verifiedByOutgoingReceiver = false;
        lastCallTerminatedByApp = false;
        String cur = TextUtils.getCurrentCalleeProgressMessage(this,
                app.lastCallName, app.lastCallCurrentCount,
                app.lastCallTotalCount, false);
        updateNotificationTextView(R.id.currentCallee, cur);
        runPhoneAppCrashNotificationTimer();
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        callIntent.setData(Uri.parse("tel:" + number));
        startActivity(callIntent);
        addSessionCall(new Date(), name, number);
    }

    /**
     * 0 => yes,
     * 1 => fajr not started,
     * 2 => sunrise was started
     */
    private byte getCanCallStatus(int cityId) {
        if (!pref_cityBasedRingEnabled)
            return AutoCallLog.AutoCallIgnored.RESULT_NOT_IGNORED;

        Time dtNow = new Time();
        dtNow.setToNow();
        int currentTime = dtNow.hour * 60 + dtNow.minute;
        int fajr, sun;

        if (cachedCitiesTimes.containsKey(cityId)) {
            Pair<Integer, Integer> pair = cachedCitiesTimes.get(cityId);
            fajr = pair.first;
            sun = pair.second;
        } else {
            City city = cityList.findById(cityId);
            Date now = new Date();
            int timeZoneOffset = 1000 * 60 * 60 * city.timezone;
            TimeZone timeZone = TimeZone.getTimeZone(TimeZone.getAvailableIDs(timeZoneOffset)[0]);
            Calendar calendar = Calendar.getInstance(timeZone);
            calendar.setTime(now);

            PrayTimes prayers = new PrayTimes();
            prayers.setTimeFormat(PrayTimes.TIME_Time24);
            prayers.setCalcMethod(city.prayerCalcMethod);
            prayers.setAsrJuristic(city.asrPrayerCalcMethod);
            prayers.setAdjustHighLats(PrayTimes.ADJ_AngleBased);
            int[] offsets = {0, 0, 0, 0, 0, 0, 0}; // {Fajr,Sunrise,Dhuhr,Asr,Sunset,Maghrib,Isha}
            prayers.tune(offsets);

            ArrayList<String> prayerTimes = prayers.getPrayerTimes(calendar,
                    city.lat, city.lng, city.timezone);
            String[] fajrS = prayerTimes.get(0).split(":");
            String[] sunriseS = prayerTimes.get(1).split(":");
            int myTimezone = myTimezoneOffset / (1000 * 60 * 60);
            int diffTimezone = myTimezone - city.timezone;
            fajr = ((Integer.parseInt(fajrS[0]) + diffTimezone) % 24) * 60 + Integer.parseInt(fajrS[1]);
            sun = ((Integer.parseInt(sunriseS[0]) + diffTimezone) % 24) * 60 + Integer.parseInt(sunriseS[1]);
            fajr += city.minMinuteAfterFajr;
            sun -= city.minMinutesBeforeSunrise;
            cachedCitiesTimes.put(cityId, new Pair<>(fajr, sun));
        }
        if (currentTime >= fajr && currentTime < sun) {
            Log.d("canCallStatus",
                    String.format(Locale.ENGLISH, "Call is available at %d %d - fajr %d sunrise %d",
                    cityId, currentTime, fajr, sun));
            return AutoCallLog.AutoCallIgnored.RESULT_NOT_IGNORED;
        }
        if (currentTime < fajr) {
            Log.d("canCallStatus",
                    String.format(Locale.ENGLISH, "Call is before fajr at %d %d - fajr %d sunrise %d",
                            cityId, currentTime, fajr, sun));
            return AutoCallLog.AutoCallIgnored.RESULT_IGNORED_BEFORE_FAJR;
        }
        Log.d("canCallStatus",
                String.format(Locale.ENGLISH, "Call is after sunrise at %d %d - fajr %d sunrise %d",
                        cityId, currentTime, fajr, sun));
        return AutoCallLog.AutoCallIgnored.RESULT_IGNORED_AFTER_SUNRISE;
    }

    private String getFajrTime(int cityId) {
        if (!cachedCitiesTimes.containsKey(cityId))
            getCanCallStatus(cityId);
        Pair<Integer, Integer> pair = cachedCitiesTimes.get(cityId);
        int fajr = pair.first;
        return String.format(Locale.ENGLISH, "%02d:%02d", fajr / 60, fajr % 60);
    }

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            callSession.setStarted(true);
            ContactsListGroupList groups = list.getGroups();

            while (callSession.getListCurrentCallItemIdx() < list.size()) {
                ContactsListItem item = list.get(callSession.getListCurrentCallItemIdx());
                if (!callSession.containsNumberInRejectersList(item.number)
                                && !numberInRejectedNumbers(groups, item.number)) {
                    byte canCall = AutoCallLog.AutoCallIgnored.RESULT_NOT_IGNORED;
                    if (item.cityId != null)
                        canCall = getCanCallStatus(item.cityId);
                    if (canCall == AutoCallLog.AutoCallIgnored.RESULT_NOT_IGNORED)
                        break;
                    else
                        addSessionIgnoredCall(new Date(), item.name, item.number, canCall);
                }
                callSession.incrementListCurrentCallItemIdx();
            }
            if (callSession.getListCurrentCallItemIdx() >= list.size()) {
                // All replied, sunrised, or before fajr
                lastServiceStartId = msg.arg1;
                android.app.AlertDialog dlg = new AlertDialog.Builder(getApplicationContext())
                        .setTitle(R.string.dlg_failedToStart_title)
                        .setCancelable(false)
                        .setMessage(R.string.dlg_failedToStart_msg)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                myStopSelf();
                            }
                        })
                        .create();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    dlg.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
                } else {
                    dlg.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                }
                dlg.show();
            } else {
                ContactsListItem item = list.get(callSession.getListCurrentCallItemIdx());
                Integer profile = item.callProfileId;
                if (profile == null)
                    profile = AutoCallProfileList.DEFAULT_PROFILE_ID;
                callNumber(item.number, item.name, 1, item.callCount, profile);
                lastServiceStartId = msg.arg1;
            }
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            //stopSelf(msg.arg1);
        }
    }
}
