package kilanny.autocaller.activities;

import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;

import javax.inject.Inject;

import kilanny.autocaller.App;
import kilanny.autocaller.R;
import kilanny.autocaller.data.AutoCallProfile;
import kilanny.autocaller.data.AutoCallProfileList;
import kilanny.autocaller.data.ContactsList;
import kilanny.autocaller.data.ContactsListItem;
import kilanny.autocaller.data.ListOfCallingLists;
import kilanny.autocaller.databinding.ActivityCallProfilesBinding;
import kilanny.autocaller.di.ContextComponent;
import kilanny.autocaller.di.ContextModule;
import kilanny.autocaller.di.DaggerContextComponent;

public class CallProfilesActivity extends AppCompatActivity {

    private ActivityCallProfilesBinding binding;
    private ArrayAdapter<AutoCallProfile> adapter;

    @Inject
    AutoCallProfileList profileList;
    @Inject
    ListOfCallingLists listOfCallingLists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_profiles);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_call_profiles);
        binding.setFabHandler(new CallProfilesActivityFabHandler());

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ContextComponent contextComponent = DaggerContextComponent.builder()
                .appComponent(App.get(this).getComponent())
                .contextModule(new ContextModule(this))
                .build();
        contextComponent.inject(this);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        initListView();
    }

    private void initListView() {
        adapter = new ArrayAdapter<AutoCallProfile>(this, R.layout.call_profile_list_item, profileList) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View rowView;
                if (convertView == null)
                    rowView = getLayoutInflater().inflate(
                            R.layout.call_profile_list_item, parent, false);
                else
                    rowView = convertView;
                final AutoCallProfile item = adapter.getItem(position);
                TextView txtProfileName = (TextView) rowView
                        .findViewById(R.id.textViewProfileName);
                txtProfileName.setText(item.name);
                rowView.findViewById(R.id.btnEditItem).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        editProfile(item);
                    }
                });
                rowView.findViewById(R.id.btnDeleteItem).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteProfile(item);
                    }
                });
                return rowView;
            }
        };
        ListView listView = (ListView) findViewById(R.id.listViewProfiles);
        listView.setAdapter(adapter);
        listView.setItemsCanFocus(false);
    }

    private void editProfile(final AutoCallProfile callProfile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Toast.makeText(this, R.string.hangup_not_working_android_9, Toast.LENGTH_LONG).show();
        }
        View view = getLayoutInflater().inflate(R.layout.dlg_edit_call_profile, null);
        final EditText txtProfileName = view.findViewById(R.id.txtProfileName);
        final NumberPicker numNoReplyTimeoutSeconds = view.findViewById(R.id.numNoReplyTimeoutSeconds);
        final NumberPicker numKillCallAfterSeconds = view.findViewById(R.id.numKillCallAfterSeconds);
        numNoReplyTimeoutSeconds.setMaxValue(300);
        numNoReplyTimeoutSeconds.setMinValue(5);
        numKillCallAfterSeconds.setMaxValue(300);
        numKillCallAfterSeconds.setMinValue(5);

        if (callProfile != null) {
            txtProfileName.setText(callProfile.name);
            numNoReplyTimeoutSeconds.setValue(callProfile.noReplyTimeoutSeconds);
            numKillCallAfterSeconds.setValue(callProfile.killCallAfterSeconds);
        }
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(view)
                .setTitle(R.string.action_edit_profile)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AutoCallProfile editItem;
                        if (callProfile != null) {
                            editItem = callProfile;
                        } else {
                            editItem = profileList.addNewProfile();
                            adapter.add(editItem);
                        }
                        editItem.name = txtProfileName.getText().toString();
                        editItem.noReplyTimeoutSeconds = numNoReplyTimeoutSeconds.getValue();
                        editItem.killCallAfterSeconds = numKillCallAfterSeconds.getValue();
                        adapter.notifyDataSetChanged();
                        profileList.save(CallProfilesActivity.this);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteProfile(final AutoCallProfile callProfile) {
        if (callProfile.id == 1) {
            Toast.makeText(this, R.string.err_delete_default_item, Toast.LENGTH_LONG).show();
            return;
        }
        for (int i = 0; i < listOfCallingLists.size(); ++i) {
            ContactsList list = listOfCallingLists.get(i);
            for (ContactsListItem item : list) {
                if (item.callProfileId != null && item.callProfileId == callProfile.id) {
                    Toast.makeText(this, R.string.err_delete_item_in_use, Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.delete_item)
                .setMessage(R.string.delete_item_message)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        adapter.remove(callProfile);
                        profileList.remove(callProfile);
                        profileList.save(CallProfilesActivity.this);
                        adapter.notifyDataSetChanged();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void createProfile() {
        editProfile(null);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public class CallProfilesActivityFabHandler {

        public void onBaseFabClick(View view) {
            createProfile();
        }
    }
}
