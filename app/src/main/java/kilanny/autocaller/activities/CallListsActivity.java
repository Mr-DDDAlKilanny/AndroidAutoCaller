package kilanny.autocaller.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import javax.inject.Inject;

import kilanny.autocaller.App;
import kilanny.autocaller.data.ContactsList;
import kilanny.autocaller.data.ListOfCallingLists;
import kilanny.autocaller.R;
import kilanny.autocaller.di.ContextComponent;
import kilanny.autocaller.di.ContextModule;
import kilanny.autocaller.di.DaggerContextComponent;
import kilanny.autocaller.utils.ResultCallback;

public class CallListsActivity extends AppCompatActivity {

    private ArrayAdapter<ContactsList> adapter;
    private ContextComponent contextComponent;
    @Inject
    ListOfCallingLists list;

    @Override
    protected void onStart() {
        super.onStart();
        //TODO: display a dialog if the list is empty to help user
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
        contextComponent = DaggerContextComponent.builder()
                .appComponent(App.get(this).getComponent())
                .contextModule(new ContextModule(this))
                .build();
        contextComponent.inject(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.listsToolbar);
        setSupportActionBar(toolbar);
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

        findViewById(R.id.fabPrefs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(CallListsActivity.this, PrefsActivity.class));
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
                        getString(R.string.set_sched_for_list),
                        getString(R.string.edit_list_name),
                        getString(R.string.delete_list)
                };
                b.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        switch (which) {
                            case 0:
                                break;
                            case 1:
                                inputString(options[1], item.getName(), new ResultCallback<String>() {
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
                                b1.setTitle(options[2]);
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
}
