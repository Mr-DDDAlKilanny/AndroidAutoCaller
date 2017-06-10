package kilanny.autocaller;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import kilanny.autocaller.utils.OsUtils;

public class MainActivity extends AppCompatActivity implements ServiceConnection {

    private static final boolean USE_SERVICE = true;

    private static final int CALL_PHONE_PERMISSION_RQUEST = 1992;
    private static final int READ_LOG_PERMISSION_REQUEST = 1994;
    private static final int PICK_CONTACT = 1993;

    private static final int WAIT_BETWEEN_CALLS_SECONDS = 5;
    private static final int NO_REPLY_TIMEOUT_SECONDS = 30 + WAIT_BETWEEN_CALLS_SECONDS;
    private static final int KILL_CALL_AFTER_SECONDS = NO_REPLY_TIMEOUT_SECONDS + 35;

    /** Messenger for communicating with the service. */
    Messenger mService = null;
    private boolean isServiceBound = false;

    // fields for onRequestPermissionsResult()
    private String lastCallNumber, lastCallName;
    private int lastCallCurrentCount, lastCallTotalCount;

    private ArrayAdapter<ContactsListItem> adapter;
    //private boolean iHaveStartedTheOutgoingCall = false;
    private int listCallingIndex = -1;
    private final HashSet<String> ansOrRejectedNumbers = new HashSet<>();
    private int listCallingCount = 0;
    private AutoCallLog.AutoCallSession currentSession;
    private ContactsList list;
    //private PhoneStateListener phoneStateListener;
    private Timer lastTimer;
    private MediaPlayer mediaPlayer;
    private BroadcastReceiver phoneStatusChangedBroadcastReceiver;

    private void rebind() {
        list.save(this);
        adapter.notifyDataSetChanged();
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
                Application app = Application.getInstance(this);
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
                    if (ansOrRejectedNumbers.contains(num)) //if someone already rejected
                        return true;
        return false;
    }

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

    private void cancelCallHangupTimer() {
        if (lastTimer != null) {
            lastTimer.cancel();
            lastTimer = null;
        }
    }

    private void runKillAutoCallTask() {
        // in case a new call is made, and the previous call rejected, cancel the old timer
        cancelCallHangupTimer();
        lastTimer = new Timer();
        lastTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                ansOrRejectedNumbers.add(lastCallNumber);
                Log.d("runKillAutoCallTask", "Call period timed out. Terminating...");
                terminateActiveCall();
            }
        }, KILL_CALL_AFTER_SECONDS * 1000);
    }

    private void nextCall() {
        if (Application.getInstance(MainActivity.this).verifiedByOutgoingReceiver) {
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
                        if (!Application.getInstance(MainActivity.this)
                                .verifiedByOutgoingReceiver || listCallingIndex == -1)
                            return; // stopped by user
                        ContactsListItem listItem = list.get(listCallingIndex);
                        try {
                            if ((ans = hasRejectedOrAnsweredOutgoingCall(listItem.number))
                                    || ++listCallingCount > listItem.callCount) {
                                listCallingCount = 1;
                                if (ans)
                                    ansOrRejectedNumbers.add(listItem.number);
                                while (++listCallingIndex < list.size() && (
                                        ansOrRejectedNumbers.contains(list.get(listCallingIndex).number)
                                                || numberInRejectedNumbers(groups, list.get(listCallingIndex).number))) {
                                }
                                if (listCallingIndex >= list.size()) {
                                    finished = true;
                                    return;
                                }
                            }
                        } finally {
                            //ignore the first result when retry calls has been chosen
                            if (currentSession.get(currentSession.size() - 1)
                                    instanceof AutoCallLog.AutoCall) {
                                Log.d("AutoCallItem", "calling result: " + ans
                                        + ", index = " + listCallingIndex
                                        + ", count = " + listCallingCount);
                                ((AutoCallLog.AutoCall) currentSession.get(currentSession.size() - 1)).result = ans ?
                                        AutoCallLog.AutoCall.RESULT_ANSWERED_OR_REJECTED :
                                        AutoCallLog.AutoCall.RESULT_NOT_ANSWERED;
                            }
                            if (finished) {
                                int firstNotAnsweredIdx = -1;
                                for (int i = 0; i < list.size(); ++i) {
                                    if (!ansOrRejectedNumbers.contains(list.get(i).number)
                                            && !numberInRejectedNumbers(groups, list.get(i).number)) {
                                        firstNotAnsweredIdx = i;
                                        break;
                                    }
                                }
                                if (firstNotAnsweredIdx >= 0) {
                                    final int tmpIdx = firstNotAnsweredIdx;
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
                                                            stopFinishRingtune();
                                                            listCallingCount = 0;
                                                            listCallingIndex = tmpIdx;
                                                            currentSession.add(new AutoCallLog.AutoCallRetry());
                                                            list.save(MainActivity.this);
                                                            nextCall();
                                                        }
                                                    })
                                                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            stopFinishRingtune();
                                                            stopAutoCall();
                                                        }
                                                    }).create();
                                            dlg.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                                            dlg.show();
                                            playFinishTune();
                                        }
                                    });
                                } else {
                                    playFinishTune();
                                    stopAutoCall();
                                }
                            }
                        }
                        listItem = list.get(listCallingIndex);
                        callNumber(listItem.number, listItem.name, listCallingCount,
                                listItem.callCount);
                    } catch (Exception ex) { //prevent phone app crash
                        ex.printStackTrace();
                    }
                }
            }, WAIT_BETWEEN_CALLS_SECONDS, TimeUnit.SECONDS);
        }
    }

    private void stopFinishRingtune() {
        if (mediaPlayer == null)
            mediaPlayer = AutoCallService.mediaPlayer;
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            mediaPlayer = null;
        }
    }

    private void playFinishTune() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (pref.getBoolean("playFinishTune", true)) {
            stopFinishRingtune();
            mediaPlayer = MediaPlayer.create(this, R.raw.finish_call);
            mediaPlayer.setLooping(false);
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            float vol = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
            mediaPlayer.setVolume(vol, vol);
            mediaPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myFinish();
    }

    private void myFinish() {
        if (USE_SERVICE) {
            if (isServiceBound) {
                unbindService(this);
                isServiceBound = false;
            }
        } else {
            stopAutoCall();
        }
        stopFinishRingtune();
        if (phoneStatusChangedBroadcastReceiver != null) {
            unregisterReceiver(phoneStatusChangedBroadcastReceiver);
            phoneStatusChangedBroadcastReceiver = null;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        myFinish();
    }

    private boolean isCalling() {
        return listCallingIndex != -1 && listCallingIndex < list.size();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Intent intent = getIntent();
        final int callListId = intent.getIntExtra("list", -1);
        list = ListOfCallingLists.getInstance(this).getById(callListId);
        setTitle(list.getName());
        Collections.sort(list, new Comparator<ContactsListItem>() {
            @Override
            public int compare(ContactsListItem lhs, ContactsListItem rhs) {
                return Integer.valueOf(lhs.index).compareTo(rhs.index);
            }
        });
        final Intent serviceIntent = new Intent(MainActivity.this, AutoCallService.class);
        serviceIntent.putExtra("callListId", callListId);
        if (!USE_SERVICE) {
            /*TelephonyManager mTM = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (phoneStateListener != null) {
                mTM.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            }
            mTM.listen(phoneStateListener = new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, String incomingNumber) {
                    if (!isCalling()) {
                    } else if (TelephonyManager.CALL_STATE_IDLE == state) {
                        nextCall();
                    } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                        Application app = Application.getInstance(MainActivity.this);
                        if (app.verifiedByOutgoingReceiver) {
                            runKillAutoCallTask();
                            app.lastOutgoingCallStartRinging = new Date();
                            app.save(MainActivity.this);
                        }
                    }
                }
            }, PhoneStateListener.LISTEN_CALL_STATE);*/
            registerReceiver(phoneStatusChangedBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int state = intent.getIntExtra("state", -1);
                    if (!isCalling()) {
                    } else if (TelephonyManager.CALL_STATE_IDLE == state) {
                        nextCall();
                    } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                        Application app = Application.getInstance(MainActivity.this);
                        if (app.verifiedByOutgoingReceiver) {
                            runKillAutoCallTask();
                            app.lastOutgoingCallStartRinging = new Date();
                            app.save(MainActivity.this);
                        }
                    }
                }
            }, new IntentFilter(AutoCallPhoneStateListener.BROADCAST_ACTION));
            /*AutoCallPhoneStateListener.setCallback(new PhoneListenerCallback() {
                @Override
                public void onStateChanged(int state) {
                    if (!isCalling()) {
                    } else if (TelephonyManager.CALL_STATE_IDLE == state) {
                        nextCall();
                    } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                        Application app = Application.getInstance(MainActivity.this);
                        if (app.verifiedByOutgoingReceiver) {
                            runKillAutoCallTask();
                            app.lastOutgoingCallStartRinging = new Date();
                            app.save(MainActivity.this);
                        }
                    }
                }
            });*/
        }
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //already running?
                    if (USE_SERVICE ?
                            OsUtils.isServiceRunning(MainActivity.this,
                                    AutoCallService.class)
                            : isCalling()) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(R.string.already_started_msg_title)
                                .setMessage(R.string.already_started_msg_body)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        if (USE_SERVICE) {
                                            sendServiceMessage(AutoCallService.MESSAGE_EXIT);
                                        }
                                        else {
                                            stopAutoCall();
                                        }
                                    }})
                                .setNegativeButton(android.R.string.no, null).show();
                        return;
                    }
                    if (list.size() == 0) {
                        Snackbar.make(view, R.string.toast_please_add_items,
                                Snackbar.LENGTH_LONG).setAction("Action", null).show();
                        return;
                    }
                    if (!USE_SERVICE) {
                        listCallingCount = 1;
                        listCallingIndex = 0;
                        currentSession = new AutoCallLog.AutoCallSession();
                        currentSession.date = new Date();
                        list.getLog().sessions.add(currentSession);
                        list.save(MainActivity.this);
                        ansOrRejectedNumbers.clear();
                        callNumber(list.get(listCallingIndex).number, list.get(listCallingIndex).name,
                                1, list.get(listCallingIndex).callCount);
                    } else {
                        startService(serviceIntent);
                        bindService(serviceIntent, MainActivity.this, 0);
                    }
                    Snackbar.make(view, R.string.toast_starting_calls, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            });
        }

        //startService(new Intent(this, CallDetectService.class));

        adapter = new ArrayAdapter<ContactsListItem>(this, R.layout.contact_list_item, list) {
            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                View rowView;
                if (convertView == null)
                    rowView = getLayoutInflater().inflate(R.layout.contact_list_item,
                            parent, false);
                else
                    rowView = convertView;
                final ContactsListItem listItem = adapter.getItem(position);
                TextView contactName = (TextView) rowView.findViewById(R.id.textViewContactName);
                contactName.setText(listItem.name);
                TextView contactNumber = (TextView) rowView.findViewById(R.id.textViewContactNumber);
                contactNumber.setText(listItem.number);
                final Button clickCount = (Button) rowView.findViewById(R.id.btnCallCount);
                clickCount.setText(listItem.callCount + "");
                clickCount.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int num = Integer.parseInt(clickCount.getText().toString());
                        num = Math.max(1, (num + 1) % 16);
                        clickCount.setText(num + "");
                        listItem.callCount = num;
                        rebind();
                    }
                });
                rowView.findViewById(R.id.delete_item).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        adapter.remove(listItem);
                        int idx = list.indexOf(listItem);
                        for (++idx; idx < list.size(); ++idx)
                            list.get(idx).index--;
                        list.remove(listItem);
                        rebind();
                    }
                });
                rowView.findViewById(R.id.btnMoveUp).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (position > 0) {
                            adapter.remove(listItem);
                            adapter.insert(listItem, position - 1);
                            listItem.index--;
                            adapter.getItem(position).index++;
                            rebind();
                        }
                    }
                });
                rowView.findViewById(R.id.btnMoveDown).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (position < adapter.getCount() - 1) {
                            adapter.remove(listItem);
                            adapter.insert(listItem, position + 1);
                            listItem.index++;
                            adapter.getItem(position).index--;
                            rebind();
                        }
                    }
                });
                return rowView;
            }
        };
        ListView listView = (ListView) findViewById(R.id.listViewNumbers);
        listView.setAdapter(adapter);
    }

    private void stopAutoCall() {
        cancelCallHangupTimer();
        /*if (phoneStateListener != null) {
            TelephonyManager mTM = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            mTM.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            phoneStateListener = null;
        }*/
        //iHaveStartedTheOutgoingCall = false;
        listCallingIndex = -1;
        Application app = Application.getInstance(this);
        app.lastCallNumber = null;
        app.verifiedByOutgoingReceiver = false;
        app.save(this);
        list.save(this);
        currentSession = null;
    }

    private void addSessionCall(Date date, String name, String number) {
        AutoCallLog.AutoCall autoCall = new AutoCallLog.AutoCall();
        autoCall.date = date;
        autoCall.name = name;
        autoCall.number = number;
        currentSession.add(autoCall);
        list.save(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case CALL_PHONE_PERMISSION_RQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    callNumber(lastCallNumber, lastCallName, lastCallCurrentCount,
                            lastCallTotalCount);
                }
                return;
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.CALL_PHONE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] {Manifest.permission.CALL_PHONE},
                        CALL_PHONE_PERMISSION_RQUEST);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.READ_CALL_LOG)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] {Manifest.permission.READ_CALL_LOG},
                        READ_LOG_PERMISSION_REQUEST);
            }
        }
        if (USE_SERVICE) {
            bindService(new Intent(this, AutoCallService.class), this, 0);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (USE_SERVICE) {
            if (isServiceBound) {
                unbindService(this);
                isServiceBound = false;
            }
        }
    }

    private void callNumber(String number, String name, int currentCallCount, int totalCallCount) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                    != PackageManager.PERMISSION_GRANTED) {
                lastCallNumber = number;
                lastCallName = name;
                lastCallCurrentCount = currentCallCount;
                lastCallTotalCount = totalCallCount;
                requestPermissions(new String[] {Manifest.permission.CALL_PHONE}, CALL_PHONE_PERMISSION_RQUEST);
                return;
            }
        }

        //iHaveStartedTheOutgoingCall = true;
        Application app = Application.getInstance(this);
        app.lastOutgoingCallStartRinging = null;
        app.lastCallNumber = number;
        app.lastCallName = name;
        app.lastCallCurrentCount = currentCallCount;
        app.lastCallTotalCount = totalCallCount;
        app.verifiedByOutgoingReceiver = false;
        app.save(this);
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + number));
        startActivity(callIntent);
        addSessionCall(new Date(), name, number);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings
                // don't modify while the calls are running
                && (listCallingIndex == -1 || listCallingIndex >= adapter.getCount())) {
            try {
                Intent intent = new Intent(Intent.ACTION_PICK,
                        ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(intent, PICK_CONTACT);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        } else if (id == R.id.action_show_log) {
            Intent intent = new Intent(this, ShowLogActivity.class);
            intent.putExtra("list", ListOfCallingLists.getInstance(this).idOf(list));
            startActivity(intent);
        } else if (id == R.id.action_edit_groups) {
            Intent intent = new Intent(this, EditGroupsActivity.class);
            intent.putExtra("list", ListOfCallingLists.getInstance(this).idOf(list));
            startActivity(intent);
        } else if (id == R.id.action_info) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.app_name)
                    .setCancelable(true)
                    .setMessage(getString(R.string.help))
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case (PICK_CONTACT) :
                if (resultCode == RESULT_OK) {
                    Cursor cursor = null;
                    try {
                        Uri result = data.getData();
                        String id = result.getLastPathSegment();
                        cursor = getContentResolver().query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?",
                                new String[] { id }, null);
                        int phoneIdx = cursor.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.DATA);
                        int nameIdx = cursor.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                        HashSet<String> alreadyAddedNumbers = new HashSet<>();
                        if (cursor.moveToFirst()) {
                            while (!cursor.isAfterLast()) {
                                String num = cursor.getString(phoneIdx).replace(" ", "").trim();
                                if (!alreadyAddedNumbers.contains(num)) {
                                    alreadyAddedNumbers.add(num);
                                    ContactsListItem item = new ContactsListItem();
                                    item.number = num;
                                    item.name = cursor.getString(nameIdx);
                                    item.index = adapter.getCount();
                                    item.callCount = 1;
                                    adapter.add(item);
                                }
                                cursor.moveToNext();
                            }
                            rebind();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mService = new Messenger(service);
        isServiceBound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mService = null;
        isServiceBound = false;
    }

    private void sendServiceMessage(int message) {
        if (mService == null) return;
        // Create and send a message to the service, using a supported 'what' value
        Message msg = Message.obtain(null, message, 0, 0);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
