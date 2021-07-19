package kilanny.autocaller.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.databinding.DataBindingUtil;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import kilanny.autocaller.BuildConfig;
import kilanny.autocaller.R;
import kilanny.autocaller.data.AutoCallSession;
import kilanny.autocaller.data.ListOfCallingLists;
import kilanny.autocaller.databinding.ActivityCallListsBinding;
import kilanny.autocaller.db.AppDb;
import kilanny.autocaller.db.ContactList;
import kilanny.autocaller.services.AutoCallService;
import kilanny.autocaller.utils.AnalyticsTrackers;
import kilanny.autocaller.services.UpgradeDataService;
import kilanny.autocaller.utils.OsUtils;
import kilanny.autocaller.utils.ResultCallback;
import kilanny.autocaller.utils.UpdateCheckUtil;

public class CallListsActivity extends AppCompatActivity {

    private ActivityCallListsBinding binding;
    private Animation fabOpenAnimation;
    private Animation fabCloseAnimation;
    private boolean isFabMenuOpen = false;
    private ArrayAdapter<ContactList> adapter;

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onStart() {
        super.onStart();

        if (AutoCallSession.getLastSession(this) != null &&
                !OsUtils.isServiceRunning(this, AutoCallService.class)) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(R.string.dlg_sessionFound_title)
                    .setMessage(R.string.dlg_sessionFound_msg)
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        Intent i = new Intent(CallListsActivity.this, MainActivity.class);
                        i.putExtra("continueLastSession", true);
                        startActivity(i);
                    })
                    .setNegativeButton(android.R.string.no, (dialog, which) -> AutoCallSession.clear(CallListsActivity.this))
                    .show();
        } else if (adapter.getCount() == 0) {
            Toast.makeText(this, R.string.list_empty_add_plus, Toast.LENGTH_LONG).show();
        } else if (UpdateCheckUtil.isConnected(this) !=
                UpdateCheckUtil.CONNECTION_STATUS_NOT_CONNECTED) {
            final String code = String.format(Locale.ENGLISH, "%d",
                    BuildConfig.VERSION_CODE);
            String s = UpdateCheckUtil.getLastCheckWhatsnew(this);
            if (s != null) {
                if (!s.split("#")[0].equals(code)) {
                    showNewUpdateDlg();
                    return;
                }
            }
            if (UpdateCheckUtil.shouldCheckForUpdates(this)) {
                new AsyncTask<Void, Void, String[]>() {

                    @Override
                    protected String[] doInBackground(Void... voids) {
                        return UpdateCheckUtil.getLatestVersion();
                    }

                    @Override
                    protected void onPostExecute(String[] strings) {
                        super.onPostExecute(strings);
                        if (strings != null) {
                            if (!strings[0].equals(code)) {
                                UpdateCheckUtil.setHasCheckedForUpdates(getApplicationContext(),
                                        strings[0] + "#" + strings[2]);
                                showNewUpdateDlg();
                            } else {
                                UpdateCheckUtil.setHasCheckedForUpdates(getApplicationContext(), null);
                            }
                        }
                    }
                }.execute();
            }
        }
    }

    private void showNewUpdateDlg() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("New Version إصدار جديد")
                .setMessage("There is a new version. Update now?\nيتوفر إصدار جديد. تحميل الآن؟\n" +
                        UpdateCheckUtil.getLastCheckWhatsnew(getApplicationContext()))
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                    String url = "https://sites.google.com/view/auto-caller/home";
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse(url)));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void inputString(String title, String initValue,
                             final ResultCallback<String> resultCallback) {
        AlertDialog.Builder b = new AlertDialog.Builder(CallListsActivity.this)
                .setTitle(title)
                .setCancelable(true);
        final EditText editText = new EditText(b.getContext());
        editText.setText(initValue);
        b.setView(editText)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    resultCallback.onResult(editText.getText().toString());
                    dialog.dismiss();
                });
        b.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean isRunning = OsUtils.isServiceRunning(this, UpgradeDataService.class);
        if (isRunning || AppDb.getInstance(getApplicationContext()).cityDao().getAll().length == 0) {
            if (isRunning || ListOfCallingLists.getInstance(this).size() > 0) {
                if (!isRunning) {
                    Intent intent = new Intent(getApplicationContext(), UpgradeDataService.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        getApplicationContext().startForegroundService(intent);
                    else
                        getApplication().startService(intent);
                }
                Intent i = new Intent(getApplicationContext(), UpgradeDataActivity.class);
                i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(i);
                finish();
                return;
            }
        }
        setContentView(R.layout.activity_call_lists);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_call_lists);
        binding.setFabHandler(new CallListsFabHandler());

        getAnimations();
        FloatingActionButton fab = findViewById(R.id.addListFab);
        fab.setOnClickListener(view -> inputString(getString(R.string.add_new_list_title), "", result -> {
            if (result.trim().length() == 0) {
                Toast.makeText(view.getContext(),
                        getString(R.string.please_input_group_name),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            ContactList list = new ContactList();
            list.name = result;
            list.id = AppDb.getInstance(this).contactListDao().insert(list);
            adapter.add(list); // we have to add it here also
            //becasue adapter's list is not binded to ListOfCallingLists
            adapter.notifyDataSetChanged();
            Snackbar.make(view,
                    getString(R.string.add_success),
                    Snackbar.LENGTH_LONG)
                    //.setAction("Action", null)
                    .show();
            AnalyticsTrackers.getInstance(CallListsActivity.this).logAddList();
        }));

        adapter = new ArrayAdapter<ContactList>(this, android.R.layout.simple_list_item_1,
                new ArrayList<>(Arrays.asList(AppDb.getInstance(this).contactListDao().getAll()))) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View rowView;
                if (convertView == null)
                    rowView = getLayoutInflater().inflate(
                            android.R.layout.simple_list_item_1, parent, false);
                else
                    rowView = convertView;
                ContactList item = adapter.getItem(position);
                TextView contactName = (TextView) rowView
                        .findViewById(android.R.id.text1);
                contactName.setText(item.name);
                return rowView;
            }
        };
        ListView listView = (ListView) findViewById(R.id.listViewLists);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            //Toast.makeText(parent.getContext(), "OnItemClick", Toast.LENGTH_SHORT).show();
            ContactList item = (ContactList) parent.getItemAtPosition(position);
            Intent i = new Intent(view.getContext(), MainActivity.class);
            i.putExtra("list", item.id);
            startActivity(i);
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            final ContactList item = (ContactList) parent.getItemAtPosition(position);
            final AlertDialog.Builder b = new AlertDialog.Builder(parent.getContext());
            b.setCancelable(true);
            b.setTitle(item.name);
            final String[] options = new String[] {
                    //getString(R.string.set_sched_for_list),
                    getString(R.string.edit_list_name),
                    getString(R.string.delete_list)
            };
            b.setItems(options, (dialog, which) -> {
                dialog.dismiss();
                switch (which + 1) {
                    case 0:
                        break;
                    case 1:
                        inputString(options[1], item.name, result -> {
                            if (result.trim().length() == 0) {
                                Toast.makeText(b.getContext(),
                                        getString(R.string.please_input_group_name),
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            item.name = result;
                            AppDb.getInstance(this).contactListDao().update(item);
                            adapter.notifyDataSetChanged();
                            AnalyticsTrackers.getInstance(CallListsActivity.this)
                                    .logEditList();
                        });
                        break;
                    case 2: {
                        AlertDialog.Builder b1 = new AlertDialog.Builder(b.getContext());
                        b1.setTitle(options[2]);
                        b1.setMessage(getString(R.string.delete_list_confirm_body));
                        b1.setCancelable(true);
                        b1.setPositiveButton(getString(android.R.string.yes), (dialog1, which1) -> {
                            AppDb.getInstance(this).contactListDao().delete(item);
                            adapter.remove(item);
                            adapter.notifyDataSetChanged();
                            AnalyticsTrackers.getInstance(CallListsActivity.this)
                                    .logDeleteList();
                        });
                        b1.setNegativeButton(getString(android.R.string.cancel), null);
                        b1.show();
                    }
                    break;
                }
            });
            b.show();
            return true;
        });
    }

    private void getAnimations() {
        fabOpenAnimation = AnimationUtils.loadAnimation(this, R.anim.fab_open);
        fabCloseAnimation = AnimationUtils.loadAnimation(this, R.anim.fab_close);
    }

    private void expandFabMenu() {
        ViewCompat.animate(binding.baseFloatingActionButton)
                .rotation(45.0F)
                .withLayer()
                .setDuration(300)
                .setInterpolator(new OvershootInterpolator(10.0F))
                .start();
        binding.settingsLayout.startAnimation(fabOpenAnimation);
        binding.citiesLayout.startAnimation(fabOpenAnimation);
        binding.callProfilesLayout.startAnimation(fabOpenAnimation);
        binding.citiesFab.setClickable(true);
        binding.fabPrefs.setClickable(true);
        binding.callProfilesFab.setClickable(true);
        isFabMenuOpen = true;
    }

    private void collapseFabMenu() {
        ViewCompat.animate(binding.baseFloatingActionButton)
                .rotation(0.0F)
                .withLayer()
                .setDuration(300)
                .setInterpolator(new OvershootInterpolator(10.0F))
                .start();
        binding.settingsLayout.startAnimation(fabCloseAnimation);
        binding.citiesLayout.startAnimation(fabCloseAnimation);
        binding.callProfilesLayout.startAnimation(fabCloseAnimation);
        binding.citiesFab.setClickable(false);
        binding.fabPrefs.setClickable(false);
        binding.callProfilesFab.setClickable(false);
        isFabMenuOpen = false;
    }

    @Override
    public void onBackPressed() {
        if (isFabMenuOpen)
            collapseFabMenu();
        else
            super.onBackPressed();
    }

    public class CallListsFabHandler {

        public void onBaseFabClick(View view) {
            if (isFabMenuOpen)
                collapseFabMenu();
            else
                expandFabMenu();
        }

        public void onSettingsFabClick(View view) {
            //Snackbar.make(binding.coordinatorLayout, "Create FAB tapped", Snackbar.LENGTH_SHORT).show();
            startActivity(new Intent(CallListsActivity.this, PrefsActivity.class));
        }

        public void onCitiesFabClick(View view) {
            //Snackbar.make(binding.coordinatorLayout, "Share FAB tapped", Snackbar.LENGTH_SHORT).show();
            startActivity(new Intent(CallListsActivity.this, CitiesActivity.class));
        }

        public void onCallProfilesFabClick(View view) {
            startActivity(new Intent(CallListsActivity.this, CallProfilesActivity.class));
        }
    }
}
