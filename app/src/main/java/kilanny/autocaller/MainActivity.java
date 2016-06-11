package kilanny.autocaller;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final int CALL_PHONE_PERMISSION_RQUEST = 1992;
    private static final int READ_LOG_PERMISSION_REQUEST = 1994;
    private static final int PICK_CONTACT = 1993;

    private String lastCallNumber, lastCallName;
    private ArrayAdapter<ContactsListItem> adapter;

    private boolean iHaveStartedTheOutgoingCall = false;
    private int listCallingIndex = -1;
    private int listCallingCount = 0;
    private AutoCallLog.AutoCallSession currentSession;

    private void rebind(ContactsList list) {
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
                        if (Integer.parseInt(callDuration) > 0 || diffSeconds < 30) {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ContactsList list = ContactsList.getInstance(this);
        Collections.sort(list, new Comparator<ContactsListItem>() {
            @Override
            public int compare(ContactsListItem lhs, ContactsListItem rhs) {
                return Integer.valueOf(lhs.index).compareTo(rhs.index);
            }
        });
        TelephonyManager mTM = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTM.listen(new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (TelephonyManager.CALL_STATE_IDLE == state) {
                    if (iHaveStartedTheOutgoingCall) {
                        // call after 1 seconds, to enable the phone to update the call log
                        // for the last call
                        Executors.newSingleThreadScheduledExecutor().schedule(new Runnable() {
                            @Override
                            public void run() {
                                boolean ans = false;
                                boolean finished = false;
                                if (!iHaveStartedTheOutgoingCall || listCallingIndex == -1)
                                    return; // stopped by user
                                ContactsListItem listItem = list.get(listCallingIndex);
                                try {
                                    if (++listCallingCount > listItem.callCount ||
                                            (ans = hasRejectedOrAnsweredOutgoingCall(listItem.number))) {
                                        listCallingCount = 1;
                                        if (++listCallingIndex >= list.size()) {
                                            finished = true;
                                            return;
                                        }
                                    }
                                } finally {
                                    Log.d("AutoCall", "calling result: " + ans
                                            + ", index = " + listCallingIndex
                                            + ", count = " + listCallingCount);
                                    currentSession.get(currentSession.size() - 1).result = ans ?
                                            AutoCallLog.AutoCall.RESULT_ANSWERED_OR_REJECTED :
                                            AutoCallLog.AutoCall.RESULT_NOT_ANSWERED;
                                    if (finished) {
                                        stopAutoCall();
                                    }
                                }
                                listItem = list.get(listCallingIndex);
                                callNumber(listItem.number, listItem.name);
                            }
                        }, 1, TimeUnit.SECONDS);
                    }
                }
            }
        }, PhoneStateListener.LISTEN_CALL_STATE);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //already running?
                    if (listCallingIndex != -1 && listCallingIndex < list.size()) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("المكالمات الآلية")
                                .setMessage("تم بدء المكالمات الآلية بالفعل. هل تريد إيقافها الآن؟")
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        stopAutoCall();
                                    }})
                                .setNegativeButton(android.R.string.no, null).show();
                        return;
                    }
                    if (list.size() == 0) {
                        Snackbar.make(view, "الرجاء إضافة رقم واحد على الأقل للقائمة",
                                Snackbar.LENGTH_LONG).setAction("Action", null).show();
                        return;
                    }
                    listCallingCount = 1;
                    listCallingIndex = 0;
                    currentSession = new AutoCallLog.AutoCallSession();
                    currentSession.date = new Date();
                    callNumber(list.get(listCallingIndex).number, list.get(listCallingIndex).name);
                    Snackbar.make(view, "يجري الآن الاتصال بالقائمة...", Snackbar.LENGTH_LONG)
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
                        rebind(list);
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
                        rebind(list);
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
                            rebind(list);
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
                            rebind(list);
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
        iHaveStartedTheOutgoingCall = false;
        listCallingIndex = -1;
        Application app = Application.getInstance(this);
        app.lastCallNumber = null;
        app.save(this);
        AutoCallLog instance = AutoCallLog.getInstance(MainActivity.this);
        instance.sessions.add(currentSession);
        instance.save(MainActivity.this);
        currentSession = null;
    }

    private void addSessionCall(Date date, String name, String number) {
        AutoCallLog.AutoCall autoCall = new AutoCallLog.AutoCall();
        autoCall.date = date;
        autoCall.name = name;
        autoCall.number = number;
        currentSession.add(autoCall);
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
                    callNumber(lastCallNumber, lastCallName);
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
    }

    private void callNumber(String number, String name) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.CALL_PHONE)
                    != PackageManager.PERMISSION_GRANTED) {
                lastCallNumber = number;
                lastCallName = name;
                requestPermissions(new String[] {Manifest.permission.CALL_PHONE}, CALL_PHONE_PERMISSION_RQUEST);
                return;
            }
        }

        iHaveStartedTheOutgoingCall = true;
        Application app = Application.getInstance(this);
        app.lastOutgoingCallStartRinging = null;
        app.lastCallNumber = number;
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
            startActivity(new Intent(this, ShowLogActivity.class));
        } else if (id == R.id.action_info) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("المكالمات الآلية")
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
                        ContactsList list = ContactsList.getInstance(this);
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
                            rebind(list);
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
}
