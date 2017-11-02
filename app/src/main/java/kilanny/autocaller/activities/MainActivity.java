package kilanny.autocaller.activities;

import android.Manifest;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import kilanny.autocaller.R;
import kilanny.autocaller.data.ContactsList;
import kilanny.autocaller.data.ContactsListItem;
import kilanny.autocaller.data.ListOfCallingLists;
import kilanny.autocaller.services.AutoCallService;
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
    private ContactsList list;

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
        AutoCallService.stopFinishRingtune();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        myFinish();
    }

    private void fabClick(View view, int callListId) {
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
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.SYSTEM_ALERT_WINDOW)
                    != PackageManager.PERMISSION_GRANTED) {
                neededPermission = 3;
            }
        }
        if (neededPermission > 0) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(android.R.string.dialog_alert_title))
                    .setMessage(R.string.mission_permissions_msg)
                    .setCancelable(true)
                    .setIcon(ContextCompat.getDrawable(this, android.R.drawable.ic_dialog_alert))
                    .show();
            return;
        }
        //already running?
        if (OsUtils.isServiceRunning(MainActivity.this,
                AutoCallService.class)) {
            new AlertDialog.Builder(MainActivity.this)
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
        serviceIntent.putExtra("callListId", callListId);
        startService(serviceIntent);
        bindService(serviceIntent, MainActivity.this, 0);
        lastServiceIntent = serviceIntent;
        Snackbar.make(view, R.string.toast_starting_calls, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
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
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    fabClick(view, callListId);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> neededPermissions = Arrays.asList(
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.SYSTEM_ALERT_WINDOW
            );
            for (int i = neededPermissions.size() - 1; i >= 0; --i) {
                if (ActivityCompat.checkSelfPermission(this, neededPermissions.get(i))
                        == PackageManager.PERMISSION_GRANTED) {
                    neededPermissions.remove(i);
                }
            }
            if (!neededPermissions.isEmpty()) {
                requestPermissions(neededPermissions.toArray(new String[0]),
                        PERMISSION_RQUEST);
            }
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
                                String num = TextUtils.fixPhoneNumber(cursor.getString(phoneIdx));
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
