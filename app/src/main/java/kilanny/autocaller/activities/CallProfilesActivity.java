package kilanny.autocaller.activities;

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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import kilanny.autocaller.R;
import kilanny.autocaller.data.AutoCallProfile;
import kilanny.autocaller.data.ContactsListItem;
import kilanny.autocaller.databinding.ActivityCallProfilesBinding;
import kilanny.autocaller.db.AppDb;
import kilanny.autocaller.utils.AnalyticsTrackers;

public class CallProfilesActivity extends AppCompatActivity {

    private ActivityCallProfilesBinding binding;
    private ArrayAdapter<AutoCallProfile> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_profiles);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_call_profiles);
        binding.setFabHandler(new CallProfilesActivityFabHandler());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        initListView();
    }

    private void initListView() {
        ArrayList<AutoCallProfile> list = new ArrayList<>(Arrays.asList(
                AppDb.getInstance(this).callProfileDao().getAll()));
        adapter = new ArrayAdapter<AutoCallProfile>(this, R.layout.call_profile_list_item, list) {
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
                TextView txtProfileName = rowView.findViewById(R.id.textViewProfileName);
                txtProfileName.setText(item.name);
                rowView.findViewById(R.id.btnEditItem).setOnClickListener(v -> editProfile(item));
                rowView.findViewById(R.id.btnDeleteItem).setOnClickListener(v -> deleteProfile(item));
                return rowView;
            }
        };
        ListView listView = findViewById(R.id.listViewProfiles);
        listView.setAdapter(adapter);
        listView.setItemsCanFocus(false);
    }

    private void editProfile(final AutoCallProfile callProfile) {
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
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    AutoCallProfile editItem;
                    if (callProfile != null) {
                        editItem = callProfile;
                    } else {
                        editItem = new AutoCallProfile();
                        adapter.add(editItem);
                    }
                    editItem.name = txtProfileName.getText().toString();
                    editItem.noReplyTimeoutSeconds = numNoReplyTimeoutSeconds.getValue();
                    editItem.killCallAfterSeconds = numKillCallAfterSeconds.getValue();
                    if (editItem.id == 0)
                        editItem.id = (int) AppDb.getInstance(this).callProfileDao().insert(editItem);
                    else
                        AppDb.getInstance(this).callProfileDao().update(editItem);
                    adapter.notifyDataSetChanged();
                    AnalyticsTrackers.getInstance(CallProfilesActivity.this)
                            .logEditProfile(callProfile != null);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteProfile(final AutoCallProfile callProfile) {
        if (callProfile.id == 1) {
            Toast.makeText(this, R.string.err_delete_default_item, Toast.LENGTH_LONG).show();
            return;
        }
        ContactsListItem[] contacts = AppDb.getInstance(this).contactDao().getByProfileId(callProfile.id);
        if (contacts.length > 0) {
            String s = Arrays.stream(contacts).map(c -> c.name + " " + c.number + "\n")
                    .collect(Collectors.joining()).trim();
            Toast.makeText(this, getString(R.string.err_delete_item_in_use) + "\n" + s,
                    Toast.LENGTH_LONG).show();
            AnalyticsTrackers.getInstance(CallProfilesActivity.this)
                    .logDeleteProfile(false);
            return;
        }
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.delete_item)
                .setMessage(R.string.delete_item_message)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    adapter.remove(callProfile);
                    AppDb.getInstance(this).callProfileDao().delete(callProfile);
                    adapter.notifyDataSetChanged();
                    AnalyticsTrackers.getInstance(CallProfilesActivity.this)
                            .logDeleteProfile(true);
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
