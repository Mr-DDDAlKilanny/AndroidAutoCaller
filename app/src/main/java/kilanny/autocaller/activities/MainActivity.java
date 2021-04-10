package kilanny.autocaller.activities;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;

import javax.inject.Inject;

import kilanny.autocaller.App;
import kilanny.autocaller.R;
import kilanny.autocaller.data.AutoCallProfile;
import kilanny.autocaller.data.AutoCallProfileList;
import kilanny.autocaller.data.AutoCallSession;
import kilanny.autocaller.data.City;
import kilanny.autocaller.data.CityList;
import kilanny.autocaller.data.ContactsList;
import kilanny.autocaller.data.ContactsListItem;
import kilanny.autocaller.data.ListOfCallingLists;
import kilanny.autocaller.data.SerializableInFile;
import kilanny.autocaller.di.ContextComponent;
import kilanny.autocaller.di.ContextModule;
import kilanny.autocaller.di.DaggerContextComponent;
import kilanny.autocaller.services.AutoCallService;
import kilanny.autocaller.utils.AnalyticsTrackers;
import kilanny.autocaller.utils.OsUtils;
import kilanny.autocaller.utils.TextUtils;

public class MainActivity extends AppCompatActivity implements ServiceConnection {

    private static final int PERMISSION_RQUEST = 1992;
    private static final int PICK_CONTACT = 1993;

    /** Messenger for communicating with the service. */
    Messenger mService = null;
    private boolean isServiceBound = false;

    private static Intent lastServiceIntent;
    private ArrayAdapter<ContactsListItem> adapter;
    private int callListId;
    private ContactsList list;
    @Inject ListOfCallingLists listOfCallingLists;
    @Inject CityList cityList;
    @Inject AutoCallProfileList callProfileList;
    private boolean continueLastSession = false;

    private void rebind() {
        list.save(this);
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myFinish();
    }

    private void myFinish() {
        if (isServiceBound) {
            unbindService(this);
            isServiceBound = false;
        }
        AutoCallService.stopFinishRingtone();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        myFinish();
    }

    private static boolean shareAd(Context context) {
        final SerializableInFile<Integer> response = new SerializableInFile<>(
                context, "share__st", 0);
        if (response.getData() == 0) {
            dispalyShareAd(context, response);
            return true;
        } else if (response.getData() == -1) {
            Date date = response.getFileLastModifiedDate(context);
            if (date == null) {
                response.setData(0, context);
                dispalyShareAd(context, response);
                return true;
            }
            long diffTime = new Date().getTime() - date.getTime();
            long diffDays = diffTime / (1000 * 60 * 60 * 24);
            if (diffDays > 30) {
                dispalyShareAd(context, response);
                return true;
            }
        }
        return false;
    }

    private static void dispalyShareAd(final Context context, final SerializableInFile<Integer> response) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.share_app);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(R.string.share_msg_dlg);
        builder.setPositiveButton(R.string.share, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                response.setData(1, context);
                try {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT,
                            context.getString(R.string.share_msg)
                                    + "\n https://sites.google.com/view/auto-caller/home");
                    sendIntent.setType("text/plain");
                    context.startActivity(sendIntent);
                } catch (Throwable th) {
                    th.printStackTrace();
                }
            }
        });
        builder.setNegativeButton(R.string.not_now, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                response.setData(-1, context);
            }
        });
        builder.create().show();
    }

    private void fabClick(final View view) {
        int neededPermission = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.CALL_PHONE)
                    != PackageManager.PERMISSION_GRANTED) {
                neededPermission = 1;
            }
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.READ_CALL_LOG)
                    != PackageManager.PERMISSION_GRANTED) {
                neededPermission = 2;
            }
            if (!Settings.canDrawOverlays(this)) {
                neededPermission = 3;
            }
        }
        if (neededPermission > 0) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(getString(android.R.string.dialog_alert_title))
                    .setMessage(R.string.mission_permissions_msg)
                    .setCancelable(true)
                    .setIcon(ContextCompat.getDrawable(this, android.R.drawable.ic_dialog_alert))
                    .show();
            checkPermissions();
            return;
        }
        //already running?
        if (OsUtils.isServiceRunning(MainActivity.this,
                AutoCallService.class)) {
            new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.already_started_msg_title)
                    .setMessage(R.string.already_started_msg_body)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            sendServiceMessage(AutoCallService.MESSAGE_EXIT);
                        }})
                    .setNegativeButton(android.R.string.no, null).show();
            return;
        }
        if (list.size() == 0) {
            Snackbar.make(view, R.string.toast_please_add_items,
                    Snackbar.LENGTH_LONG).setAction("Action", null).show();
            return;
        }
        final Intent serviceIntent = new Intent(
                MainActivity.this.getApplicationContext(), AutoCallService.class);
        final Handler handler = new Handler();
        final Runnable start = new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    startForegroundService(serviceIntent);
                else
                    startService(serviceIntent);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        bindService(serviceIntent, MainActivity.this, 0);
                    }
                }, 3000);
                lastServiceIntent = serviceIntent;
                Snackbar.make(view, R.string.toast_starting_calls, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        };
        if (continueLastSession) {
            serviceIntent.putExtra("continueLastSession", true);
            continueLastSession = false;
        } else {
            serviceIntent.putExtra("callListId", callListId);
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            if (pref.getBoolean("showIgnoreDlg", false)) {
                androidx.appcompat.app.AlertDialog.Builder builder =
                        new androidx.appcompat.app.AlertDialog.Builder(this);
                builder.setTitle(R.string.do_you_want_to_ignore);
                final String numbers[] = getListNumbers();
                final ArrayList<Integer> selectedItems = new ArrayList<>();
                builder.setMultiChoiceItems(numbers, null, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        if (isChecked)
                            selectedItems.add(which);
                        else if (selectedItems.contains(which))
                            selectedItems.remove(which);
                    }
                });
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        StringBuilder b = new StringBuilder();
                        for (Integer idx : selectedItems) {
                            String num = numbers[idx].substring(numbers[idx].lastIndexOf('('));
                            b.append(num.substring(1, num.length() - 1)).append(',');
                        }
                        if (b.length() > 0) {
                            b.deleteCharAt(b.length() - 1);
                            serviceIntent.putExtra("ignoreNumbers", b.toString());
                            AnalyticsTrackers.getInstance(MainActivity.this).logIgnoredSomeNumbers();
                        }
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        start.run();
                    }
                });
                builder.show();
                return;
            }
        }
        start.run();
    }

    private String[] getListNumbers() {
        final ArrayList<String> items = new ArrayList<>();
        HashSet<String> nums = new HashSet<>();
        for (ContactsListItem li : list) {
            if (nums.contains(li.number)) continue;
            nums.add(li.number);
            items.add(li.name + String.format(" (%s)", li.number));
        }
        return items.toArray(new String[0]);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Intent intent = getIntent();
        if (intent.getBooleanExtra("continueLastSession", false)) {
            continueLastSession = true;
            callListId = AutoCallSession.getLastSession(this).contactsListId;
        } else
            callListId = intent.getIntExtra("list", -1);
        ContextComponent contextComponent = DaggerContextComponent.builder()
                .appComponent(App.get(this).getComponent())
                .contextModule(new ContextModule(this))
                .build();
        contextComponent.inject(this);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    fabClick(view);
                }
            });
        }

        //startService(new Intent(this, CallDetectService.class));

    }

    @Override
    public void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        list = listOfCallingLists.getById(callListId);
        setTitle(list.getName());
        Collections.sort(list, new Comparator<ContactsListItem>() {
            @Override
            public int compare(ContactsListItem lhs, ContactsListItem rhs) {
                return Integer.valueOf(lhs.index).compareTo(rhs.index);
            }
        });
        initListView();
        if (continueLastSession) {
            AnalyticsTrackers.getInstance(MainActivity.this).logContinueLastSession();
            fabClick(findViewById(R.id.fab));
        }
    }

    private void initListView() {
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
                final TextView contactNumber = (TextView) rowView.findViewById(R.id.textViewContactNumber);
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
                        AnalyticsTrackers.getInstance(MainActivity.this).logEditContactCount();
                    }
                });
                rowView.findViewById(R.id.delete_item).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.delete_item)
                        .setMessage(R.string.delete_item_message)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                int idx = list.indexOf(listItem);
                                for (++idx; idx < list.size(); ++idx)
                                    list.get(idx).index--;
                                //adapter.remove(listItem);
                                list.remove(listItem);
                                rebind();
                                AnalyticsTrackers.getInstance(MainActivity.this).logDeleteContact();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                    }
                });
                rowView.findViewById(R.id.btnMoveUp).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (position > 0) {
                            //adapter.remove(listItem);
                            //adapter.insert(listItem, position - 1);
                            list.remove(listItem);
                            list.add(position - 1, listItem);
                            listItem.index--;
                            adapter.getItem(position).index++;
                            rebind();
                            AnalyticsTrackers.getInstance(MainActivity.this).logEditContactOrder();
                        }
                    }
                });
                rowView.findViewById(R.id.btnMoveDown).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (position < adapter.getCount() - 1) {
                            //adapter.remove(listItem);
                            //adapter.insert(listItem, position + 1);
                            list.remove(listItem);
                            list.add(position + 1, listItem);
                            listItem.index++;
                            adapter.getItem(position).index--;
                            rebind();
                            AnalyticsTrackers.getInstance(MainActivity.this).logEditContactOrder();
                        }
                    }
                });
                rowView.findViewById(R.id.btnSelectCity).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSelectCityDialog(listItem);
                    }
                });
                rowView.findViewById(R.id.btnSelectProfile).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSelectCallProfileDialog(listItem);
                    }
                });
                return rowView;
            }
        };
        ListView listView = (ListView) findViewById(R.id.listViewNumbers);
        listView.setAdapter(adapter);
    }

    private void showSelectCityDialog(final ContactsListItem listItem) {
        String[] cities = new String[cityList.size()];
        int selectedItem = -1;
        for (int i = 0; i < cityList.size(); ++i) {
            City city = cityList.get(i);
            cities[i] = city.country + " - " + city.name;
            if (listItem.cityId != null && listItem.cityId == city.id)
                selectedItem = i;
        }

        new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                .setTitle(getString(R.string.dlg_select_city_title) + " - " + listItem.number)
                .setSingleChoiceItems(cities, selectedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String phoneNumber = TextUtils.fixPhoneNumber(listItem.number);
                        for (int iList = 0; iList < list.myList.size(); ++iList) {
                            ContactsList list = MainActivity.this.list.myList.get(iList);
                            for (int i = 0; i < list.size(); ++i) {
                                ContactsListItem item = list.get(i);
                                String other = TextUtils.fixPhoneNumber(item.number);
                                boolean equals = other.equals(phoneNumber)
                                        || other.endsWith(phoneNumber)
                                        || phoneNumber.endsWith(other);
                                if (equals)
                                    item.cityId = cityList.get(which).id;
                            }
                        }
                        rebind();
                        dialog.dismiss();
                        Toast.makeText(MainActivity.this,
                                String.format(getString(R.string.dlg_select_city_msg), listItem.number),
                                Toast.LENGTH_LONG).show();
                        AnalyticsTrackers.getInstance(MainActivity.this).logEditContactCity();
                    }
                })
                .show();
    }

    private void showSelectCallProfileDialog(final ContactsListItem listItem) {
        String[] profiles = new String[callProfileList.size()];
        int selectedItem = -1;
        for (int i = 0; i < callProfileList.size(); ++i) {
            AutoCallProfile profile = callProfileList.get(i);
            profiles[i] = profile.name;
            if (listItem.callProfileId != null && listItem.callProfileId == profile.id)
                selectedItem = i;
        }

        new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                .setTitle(getString(R.string.dlg_select_profile_title) + " - " + listItem.number)
                .setSingleChoiceItems(profiles, selectedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String phoneNumber = TextUtils.fixPhoneNumber(listItem.number);
                        for (int iList = 0; iList < list.myList.size(); ++iList) {
                            ContactsList list = MainActivity.this.list.myList.get(iList);
                            for (int i = 0; i < list.size(); ++i) {
                                ContactsListItem item = list.get(i);
                                String other = TextUtils.fixPhoneNumber(item.number);
                                boolean equals = other.equals(phoneNumber)
                                        || other.endsWith(phoneNumber)
                                        || phoneNumber.endsWith(other);
                                if (equals)
                                    item.callProfileId = callProfileList.get(which).id;
                            }
                        }
                        rebind();
                        dialog.dismiss();
                        Toast.makeText(MainActivity.this,
                                String.format(getString(R.string.dlg_select_city_msg), listItem.number),
                                Toast.LENGTH_LONG).show();
                        AnalyticsTrackers.getInstance(MainActivity.this).logEditContactProfile();
                    }
                })
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> neededPermissions = new ArrayList<>(Arrays.asList(
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.PROCESS_OUTGOING_CALLS
                    //,Manifest.permission.SYSTEM_ALERT_WINDOW
            ));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                neededPermissions.add(Manifest.permission.ANSWER_PHONE_CALLS);
            }
            for (int i = neededPermissions.size() - 1; i >= 0; --i) {
                if (ActivityCompat.checkSelfPermission(this, neededPermissions.get(i))
                        == PackageManager.PERMISSION_GRANTED) {
                    neededPermissions.remove(i);
                }
            }
            if (!neededPermissions.isEmpty()) {
                requestPermissions(neededPermissions.toArray(new String[0]),
                        PERMISSION_RQUEST);
                return false;
            } else if (!Settings.canDrawOverlays(this)) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_title_systemalert_permission)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(R.string.dialog_msg_systemalert_permission)
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                OsUtils.requestSystemAlertPermission(
                                        MainActivity.this,
                                        PERMISSION_RQUEST);
                            }
                        })
                        .show();
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (checkPermissions()) {
            shareAd(this);
        }
        if (OsUtils.isServiceRunning(this, AutoCallService.class)) {
            if (lastServiceIntent != null)
                bindService(lastServiceIntent, this, 0);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isServiceBound) {
            unbindService(this);
            isServiceBound = false;
        }
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
                && !OsUtils.isServiceRunning(this, AutoCallService.class)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.READ_CONTACTS)
                        != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Missing Permission: read contacts.", Toast.LENGTH_LONG)
                            .show();
                    //TODO: display a helpful dialog instead
                    return true;
                }
            }
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
            intent.putExtra("list", listOfCallingLists.idOf(list));
            startActivity(intent);
        } else if (id == R.id.action_edit_groups) {
            Intent intent = new Intent(this, EditGroupsActivity.class);
            intent.putExtra("list", listOfCallingLists.idOf(list));
            startActivity(intent);
        } else if (id == R.id.action_info) {
            new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
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
                                String num = TextUtils.fixPhoneNumber(cursor.getString(phoneIdx));
                                if (!alreadyAddedNumbers.contains(num)) {
                                    alreadyAddedNumbers.add(num);
                                    ContactsListItem item = new ContactsListItem();
                                    item.number = num;
                                    item.name = cursor.getString(nameIdx);
                                    item.index = adapter.getCount();
                                    item.callCount = 1;
                                    //adapter.add(item);
                                    list.add(item);
                                    showSelectCityDialog(item);
                                }
                                cursor.moveToNext();
                            }
                            rebind();
                            AnalyticsTrackers.getInstance(MainActivity.this).logAddContact();
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
        if (mService == null) {
            Log.w("sendServiceMessage",
                    "mService is null; ignoring sent message: " + message);
            return;
        }
        // Create and send a message to the service, using a supported 'what' value
        Message msg = Message.obtain(null, message, 0, 0);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
