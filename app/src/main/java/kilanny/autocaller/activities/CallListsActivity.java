package kilanny.autocaller.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.databinding.DataBindingUtil;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

import javax.inject.Inject;

import kilanny.autocaller.App;
import kilanny.autocaller.BuildConfig;
import kilanny.autocaller.data.AutoCallSession;
import kilanny.autocaller.data.ContactsList;
import kilanny.autocaller.data.ListOfCallingLists;
import kilanny.autocaller.R;
import kilanny.autocaller.databinding.ActivityCallListsBinding;
import kilanny.autocaller.di.ContextComponent;
import kilanny.autocaller.di.ContextModule;
import kilanny.autocaller.di.DaggerContextComponent;
import kilanny.autocaller.services.AutoCallService;
import kilanny.autocaller.utils.OsUtils;
import kilanny.autocaller.utils.ResultCallback;
import kilanny.autocaller.utils.UpdateCheckUtil;

public class CallListsActivity extends AppCompatActivity {

    private ActivityCallListsBinding binding;
    private Animation fabOpenAnimation;
    private Animation fabCloseAnimation;
    private boolean isFabMenuOpen = false;
    private ArrayAdapter<ContactsList> adapter;
    private ContextComponent contextComponent;
    @Inject
    ListOfCallingLists list;

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onStart() {
        super.onStart();
        //TODO: display a dialog if the list is empty to help user
        if (AutoCallSession.getLastSession(this) != null &&
                !OsUtils.isServiceRunning(this, AutoCallService.class)) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(R.string.dlg_sessionFound_title)
                    .setMessage(R.string.dlg_sessionFound_msg)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent i = new Intent(CallListsActivity.this, MainActivity.class);
                            i.putExtra("continueLastSession", true);
                            startActivity(i);
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AutoCallSession.clear(CallListsActivity.this);
                        }
                    })
                    .show();
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
                .setTitle("New Version")
                .setMessage("There is a new version. Update now?\n" +
                        UpdateCheckUtil.getLastCheckWhatsnew(getApplicationContext()))
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String url = "https://sites.google.com/view/auto-caller/home";
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse(url)));
                    }
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
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        resultCallback.onResult(editText.getText().toString());
                        dialog.dismiss();
                    }
                });
        b.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_lists);
        binding = DataBindingUtil.<ActivityCallListsBinding>setContentView(this, R.layout.activity_call_lists);
        binding.setFabHandler(new CallListsFabHandler());

        getAnimations();
        contextComponent = DaggerContextComponent.builder()
                .appComponent(App.get(this).getComponent())
                .contextModule(new ContextModule(this))
                .build();
        contextComponent.inject(this);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.addListFab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                inputString(getString(R.string.add_new_list_title), "", new ResultCallback<String>() {
                    @Override
                    public void onResult(String result) {
                        if (result.trim().length() == 0) {
                            Toast.makeText(view.getContext(),
                                    getString(R.string.please_input_group_name),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        ContactsList list = new ContactsList(CallListsActivity.this.list, result);
                        CallListsActivity.this.list.add(list);
                        CallListsActivity.this.list.save(view.getContext());
                        adapter.add(list); // we have to add it here also
                        //becasue adapter's list is not binded to ListOfCallingLists
                        adapter.notifyDataSetChanged();
                        Snackbar.make(view,
                                getString(R.string.add_success),
                                Snackbar.LENGTH_LONG)
                                //.setAction("Action", null)
                                .show();
                    }
                });
            }
        });
    }

    @Override
    public void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        initListView();
    }

    private void initListView() {
        adapter = new ArrayAdapter<ContactsList>(this, android.R.layout.simple_list_item_1, list.toList()) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View rowView;
                if (convertView == null)
                    rowView = getLayoutInflater().inflate(
                            android.R.layout.simple_list_item_1, parent, false);
                else
                    rowView = convertView;
                ContactsList item = adapter.getItem(position);
                TextView contactName = (TextView) rowView
                        .findViewById(android.R.id.text1);
                contactName.setText(item.getName());
                return rowView;
            }
        };
        ListView listView = (ListView) findViewById(R.id.listViewLists);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Toast.makeText(parent.getContext(), "OnItemClick", Toast.LENGTH_SHORT).show();
                ContactsList item = (ContactsList) parent.getItemAtPosition(position);
                Intent i = new Intent(view.getContext(), MainActivity.class);
                i.putExtra("list", list.idOf(item));
                startActivity(i);
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final ContactsList item = (ContactsList) parent.getItemAtPosition(position);
                final AlertDialog.Builder b = new AlertDialog.Builder(parent.getContext());
                b.setCancelable(true);
                b.setTitle(item.getName());
                final String[] options = new String[] {
                        //getString(R.string.set_sched_for_list),
                        getString(R.string.edit_list_name),
                        getString(R.string.delete_list)
                };
                b.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        switch (which + 1) {
                            case 0:
                                break;
                            case 1:
                                inputString(options[0], item.getName(), new ResultCallback<String>() {
                                    @Override
                                    public void onResult(String result) {
                                        if (result.trim().length() == 0) {
                                            Toast.makeText(b.getContext(),
                                                    getString(R.string.please_input_group_name),
                                                    Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        item.setName(result);
                                        list.save(b.getContext());
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                                break;
                            case 2: {
                                AlertDialog.Builder b1 = new AlertDialog.Builder(b.getContext());
                                b1.setTitle(options[1]);
                                b1.setMessage(getString(R.string.delete_list_confirm_body));
                                b1.setCancelable(true);
                                b1.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        list.remove(item);
                                        list.save(b.getContext());
                                        adapter.remove(item);
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                                b1.setNegativeButton(getString(android.R.string.cancel), null);
                                b1.show();
                            }
                            break;
                        }
                    }
                });
                b.show();
                return true;
            }
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
