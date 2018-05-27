package kilanny.autocaller.services;

import android.Manifest;
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
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Pair;
import android.view.WindowManager;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
    private static final int PHONE_APP_CRASH_NOTIFICATION_TIMEOUT = 60;
    private static final int NO_REPLY_TIMEOUT_SECONDS = 30 + WAIT_BETWEEN_CALLS_SECONDS;
    private static final int KILL_CALL_AFTER_SECONDS = NO_REPLY_TIMEOUT_SECONDS + 35;

    private static final Class<?>[] mSetForegroundSignature = new Class[]{
            boolean.class};
    private static final Class<?>[] mStartForegroundSignature = new Class[]{
            int.class, Notification.class};
    private static final Class<?>[] mStopForegroundSignature = new Class[]{
            boolean.class};

    @Inject
    ListOfCallingLists listOfCallingLists;
    @Inject
    CityList cityList;
    private NotificationManager mNM;
    private Notification.Builder notificationBuilder;
    private Method mSetForeground;
    private Method mStartForeground;
    private Method mStopForeground;
    private Object[] mSetForegroundArgs = new Object[1];
    private Object[] mStartForegroundArgs = new Object[2];
    private Object[] mStopForegroundArgs = new Object[1];
    private AtomicBoolean canMakeCalls;
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private Handler runOnUiThreadHandler;
    private int lastServiceStartId = -1;
    private RemoteViews notificationRemoteViews;
    private BroadcastReceiver phoneStatusChangedBroadcastReceiver;

    private AutoCallSession callSession;
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
            if (_lastInstance.lastServiceStartId != -1) {
                int tmp = _lastInstance.lastServiceStartId;
                _lastInstance.lastServiceStartId = -1;
                _lastInstance.stopSelf(tmp);
            }
        }
    }

    private void invokeMethod(Method method, Object[] args) {
        try {
            method.invoke(this, args);
        } catch (InvocationTargetException e) {
            // Should not happen.
            Log.w("ApiDemos", "Unable to invoke method", e);
        } catch (IllegalAccessException e) {
            // Should not happen.
            Log.w("ApiDemos", "Unable to invoke method", e);
        }
    }

    /**
     * This is a wrapper around the new startForeground method, using the older
     * APIs if it is not available.
     */
    private void startForegroundCompat(int id, Notification notification) {
        // If we have the new startForeground API, then use it.
        if (mStartForeground != null) {
            mStartForegroundArgs[0] = Integer.valueOf(id);
            mStartForegroundArgs[1] = notification;
            invokeMethod(mStartForeground, mStartForegroundArgs);
            return;
        }

        // Fall back on the old API.
        mSetForegroundArgs[0] = Boolean.TRUE;
        invokeMethod(mSetForeground, mSetForegroundArgs);
        mNM.notify(id, notification);
    }

    /**
     * This is a wrapper around the new stopForeground method, using the older
     * APIs if it is not available.
     */
    private void stopForegroundCompat(int id) {
        // If we have the new stopForeground API, then use it.
        if (mStopForeground != null) {
            mStopForegroundArgs[0] = Boolean.TRUE;
            invokeMethod(mStopForeground, mStopForegroundArgs);
            return;
        }

        // Fall back on the old API.  Note to cancel BEFORE changing the
        // foreground state, since we could be killed at that point.
        mNM.cancel(id);
        mSetForegroundArgs[0] = Boolean.FALSE;
        invokeMethod(mSetForeground, mSetForegroundArgs);
    }

    public static class StopButtonListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (_lastInstance != null) {
                _lastInstance.canMakeCalls.set(false);
                _lastInstance.stopAutoCall(true);
            }
            Toast.makeText(context, R.string.stopping_auto_calls, Toast.LENGTH_SHORT).show();
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
                NotificationManager.IMPORTANCE_NONE);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = createNotificationChannel();
            notificationBuilder = new Notification.Builder(this, channelId);
        } else {
            notificationBuilder = new Notification.Builder(this);
        }
        notificationBuilder.setContentIntent(pendingIntent)
                .setContent(notificationRemoteViews)
                .setSmallIcon(R.mipmap.ic_launcher);
        try {
            mStartForeground = getClass().getMethod("startForeground",
                    mStartForegroundSignature);
            mStopForeground = getClass().getMethod("stopForeground",
                    mStopForegroundSignature);
            return;
        } catch (NoSuchMethodException e) {
            // Running on an older platform.
            mStartForeground = mStopForeground = null;
        }
        try {
            mSetForeground = getClass().getMethod("setForeground",
                    mSetForegroundSignature);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                    "OS doesn't have Service.startForeground OR Service.setForeground!");
        }
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
        startForegroundCompat(NOTIFICATION_ID, notificationBuilder.getNotification());

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
                if (canMakeCalls == null || !callSession.isStarted()) {
                    // just for handling the first time CALL_STATE_IDLE
                } else if (TelephonyManager.CALL_STATE_IDLE == state) {
                    cancelPhoneAppCrashNotificationTimer();
                    if (!canMakeCalls.get())
                        stopAutoCall(true);
                    else nextCall();
                } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                    App app = App.get(AutoCallService.this);
                    if (app.verifiedByOutgoingReceiver) {
                        runKillAutoCallTask();
                        app.lastOutgoingCallStartRinging = new Date();
                    }
                }
            }
        }, new IntentFilter(AutoCallPhoneStateListener.BROADCAST_ACTION));

        /*AutoCallPhoneStateListener.setCallback(new PhoneListenerCallback() {
            @Override
            public void onStateChanged(int state) {
                if (canMakeCalls == null || listCurrentCallItemIdx == -1) {
                    // just for handling the first time CALL_STATE_IDLE
                } else if (TelephonyManager.CALL_STATE_IDLE == state) {
                    if (!canMakeCalls.get())
                        stopAutoCall(true);
                    else nextCall();
                } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                    ApplicationState app = ApplicationState.getInstance(AutoCallService.this);
                    if (app.verifiedByOutgoingReceiver) {
                        runKillAutoCallTask();
                        app.lastOutgoingCallStartRinging = new Date();
                        app.save(AutoCallService.this);
                    }
                }
            }
        });*/
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
        }
        callSession.setStarted(false);
        if (isNewSession) {
            currentSession = new AutoCallLog.AutoCallSession();
            currentSession.date = new Date();
            list.getLog().sessions.add(currentSession);
            list.save(this);
        }
        updateNotificationTextView(R.id.callListName, list.getName());

        canMakeCalls = new AtomicBoolean(true);
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
        stopForegroundCompat(NOTIFICATION_ID);
        stopAutoCall(false);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    private boolean hasRejectedOrAnsweredOutgoingCall(String number) {
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
                    case CallLog.Calls.OUTGOING_TYPE:
                        if (Integer.parseInt(callDuration) > 0 || diffSeconds < NO_REPLY_TIMEOUT_SECONDS) {
                            Log.d("CALL_CHECK", "true: callDuration = " + callDuration
                                    + ", diffSeconds = " + diffSeconds);
                            return true;
                        }
                        break;
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
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("OutgoingCallReceiver", "Exception object: " + e);
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

    private void cancelCallHangupTimer() {
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
        phoneAppCrashNotificationTimer = new Timer();
        phoneAppCrashNotificationTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.d("PhoneAppCrash", "PhoneApp Crash Detected. Playing warning sound...");
                playFinishTune();
            }
        }, PHONE_APP_CRASH_NOTIFICATION_TIMEOUT * 1000);
    }

    private void runKillAutoCallTask() {
        // in case a new call is made, and the previous call rejected, cancel the old timer
        cancelCallHangupTimer();
        autoHangupTimer = new Timer();
        autoHangupTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                App application = App.get(AutoCallService.this);
                callSession.addNumberToRejectersList(application.lastCallNumber);
                Log.d("runKillAutoCallTask", "Call period timed out. Terminating...");
                terminateActiveCall();
            }
        }, KILL_CALL_AFTER_SECONDS * 1000);
    }

    private void nextCall() {
        if (App.get(this).verifiedByOutgoingReceiver) {
            cancelCallHangupTimer();
            // call after 1 seconds, to enable the phone to update the call log
            // for the last call
            Executors.newSingleThreadScheduledExecutor().schedule(new Runnable() {
                @Override
                public void run() {
                    ContactsListGroupList groups = list.getGroups();
                    try {
                        boolean ans = false;
                        boolean finished = false;
                        if (!App.get(AutoCallService.this)
                                .verifiedByOutgoingReceiver || !callSession.isStarted())
                            return; // stopped by user
                        ContactsListItem listItem = list.get(callSession.getListCurrentCallItemIdx());
                        byte canCall = AutoCallLog.AutoCallIgnored.RESULT_NOT_IGNORED;
                        try {
                            if (listItem.cityId != null)
                                canCall = getCanCallStatus(listItem.cityId);
                            if ((ans = hasRejectedOrAnsweredOutgoingCall(listItem.number))
                                    || canCall != AutoCallLog.AutoCallIgnored.RESULT_NOT_IGNORED
                                    || 1 + callSession.getListCurrentCallItemCount() > listItem.callCount) {
                                callSession.setListCurrentCallItemCount(1);
                                if (canCall != AutoCallLog.AutoCallIgnored.RESULT_NOT_IGNORED)
                                    addSessionIgnoredCall(new Date(), listItem.name, listItem.number, canCall);
                                else if (ans)
                                    callSession.addNumberToRejectersList(listItem.number);

                                callSession.setListCurrentCallItemIdx(1 + callSession.getListCurrentCallItemIdx());
                                while (callSession.getListCurrentCallItemIdx() < list.size() && (
                                        callSession.containsNumberInRejectersList(list.get(callSession.getListCurrentCallItemIdx()).number)
                                                || numberInRejectedNumbers(groups, list.get(callSession.getListCurrentCallItemIdx()).number))) {
                                    callSession.setListCurrentCallItemIdx(1 + callSession.getListCurrentCallItemIdx());
                                }
                                if (callSession.getListCurrentCallItemIdx() >= list.size()) {
                                    finished = true;
                                    return;
                                }
                            } else
                                callSession.setListCurrentCallItemCount(1 + callSession.getListCurrentCallItemCount());
                        } finally {
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
                            if (finished) {
                                int firstNotAnsweredIdx = -1;
                                for (int i = 0; i < list.size(); ++i) {
                                    if (!callSession.containsNumberInRejectersList(list.get(i).number)
                                            && !numberInRejectedNumbers(groups, list.get(i).number)) {
                                        firstNotAnsweredIdx = i;
                                        break;
                                    }
                                }
                                if (firstNotAnsweredIdx >= 0) {
                                    final int tmpIdx = firstNotAnsweredIdx;
                                    final Runnable recallRunnable = new Runnable() {
                                        @Override
                                        public void run() {
                                            callSession.setListCurrentCallItemCount(0);
                                            callSession.setListCurrentCallItemIdx(tmpIdx);
                                            currentSession.add(new AutoCallLog.AutoCallRetry());
                                            list.save(AutoCallService.this);
                                            nextCall();
                                        }
                                    };
                                    if (getNumAutoRecallList() > callSession.getListAutoRecallCount()) {
                                        callSession.setListAutoRecallCount(
                                                callSession.getListAutoRecallCount() + 1);
                                        recallRunnable.run();
                                    } else {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                android.app.AlertDialog dlg = new android.app.AlertDialog.Builder(getApplicationContext())
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
                        callNumber(listItem.number, listItem.name, callSession.getListCurrentCallItemCount(),
                                listItem.callCount);
                    } catch (Exception ex) { //prevent phone app crash
                        ex.printStackTrace();
                    }
                }
            }, WAIT_BETWEEN_CALLS_SECONDS, TimeUnit.SECONDS);
        }
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
        if (pref.getBoolean("playFinishTuneIfRequired", true)) {
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

    private int getNumAutoRecallList() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String string = preferences.getString("autoRecallListNo",
                getString(R.string.auto_recall_count_array_default_value));
        return Integer.parseInt(string);
    }

    private void stopAutoCall(boolean shouldStopSelf) {
        cancelCallHangupTimer();
        //iHaveStartedTheOutgoingCall = false;
        AutoCallSession.clear(this);
        App app = App.get(this);
        app.lastCallNumber = null;
        app.verifiedByOutgoingReceiver = false;
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

    private void callNumber(String number, String name, int currentCallCount, int totalCallCount) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                    != PackageManager.PERMISSION_GRANTED) {
                throw new IllegalStateException("Permissions must be fully granted to current app before starting the service");
            }
        }

        //iHaveStartedTheOutgoingCall = true;
        App app = App.get(this);
        app.lastOutgoingCallStartRinging = null;
        app.lastCallNumber = number;
        app.lastCallName = name;
        app.lastCallCurrentCount = currentCallCount;
        app.lastCallTotalCount = totalCallCount;
        app.verifiedByOutgoingReceiver = false;
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
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (!pref.getBoolean("cityBasedRingEnabled", true)) {
            return AutoCallLog.AutoCallIgnored.RESULT_NOT_IGNORED;
        }
        City city = cityList.findById(cityId);
        Date now = new Date();
        int timeZoneOffset = 1000 * 60 * 60 * city.timezone;
        TimeZone timeZone = TimeZone.getTimeZone(TimeZone.getAvailableIDs(timeZoneOffset)[0]);
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTime(now);
        int currentTime = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        int fajr, sun;

        if (cachedCitiesTimes.containsKey(cityId)) {
            Pair<Integer, Integer> pair = cachedCitiesTimes.get(cityId);
            fajr = pair.first;
            sun = pair.second;
        } else {
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
            fajr = Integer.parseInt(fajrS[0]) * 60 + Integer.parseInt(fajrS[1]);
            sun = Integer.parseInt(sunriseS[0]) * 60 + Integer.parseInt(sunriseS[1]);
            cachedCitiesTimes.put(cityId, new Pair<>(fajr, sun));
        }
        fajr += city.minMinuteAfterFajr;
        sun -= city.minMinutesBeforeSunrise;
        if (currentTime >= fajr && currentTime < sun) {
            Log.d("canCallStatus",
                    String.format(Locale.ENGLISH, "Call is available at %s %d - fajr %d sunrise %d",
                    city.name, currentTime, fajr, sun));
            return AutoCallLog.AutoCallIgnored.RESULT_NOT_IGNORED;
        }
        if (currentTime < fajr) {
            Log.d("canCallStatus",
                    String.format(Locale.ENGLISH, "Call is before fajr at %s %d - fajr %d sunrise %d",
                            city.name, currentTime, fajr, sun));
            return AutoCallLog.AutoCallIgnored.RESULT_IGNORED_BEFORE_FAJR;
        }
        Log.d("canCallStatus",
                String.format(Locale.ENGLISH, "Call is after sunrise at %s %d - fajr %d sunrise %d",
                        city.name, currentTime, fajr, sun));
        return AutoCallLog.AutoCallIgnored.RESULT_IGNORED_AFTER_SUNRISE;
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
                if (callSession.containsNumberInRejectersList(item.number)
                                || numberInRejectedNumbers(groups, item.number))
                    callSession.setListCurrentCallItemIdx(1 + callSession.getListCurrentCallItemIdx());
                else {
                    byte canCall = AutoCallLog.AutoCallIgnored.RESULT_NOT_IGNORED;
                    if (item.cityId != null) {
                        canCall = getCanCallStatus(item.cityId);
                    }
                    if (canCall == AutoCallLog.AutoCallIgnored.RESULT_NOT_IGNORED)
                        break;
                    else {
                        addSessionIgnoredCall(new Date(), item.name, item.number, canCall);
                        callSession.setListCurrentCallItemIdx(1 + callSession.getListCurrentCallItemIdx());
                    }
                }
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
                callNumber(list.get(callSession.getListCurrentCallItemIdx()).number,
                        list.get(callSession.getListCurrentCallItemIdx()).name,
                        1, list.get(callSession.getListCurrentCallItemIdx()).callCount);
                lastServiceStartId = msg.arg1;
            }
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            //stopSelf(msg.arg1);
        }
    }
}
