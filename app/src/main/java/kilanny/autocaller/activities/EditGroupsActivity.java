package kilanny.autocaller.activities;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import kilanny.autocaller.R;
import kilanny.autocaller.adapters.ExpandableListAdapter_Groups;
import kilanny.autocaller.data.ContactsListGroup;
import kilanny.autocaller.data.ContactsListItem;
import kilanny.autocaller.db.AppDb;
import kilanny.autocaller.db.ContactInGroup;
import kilanny.autocaller.db.ContactListItem2;

/**
 * Created by Yasser on 08/12/2016.
 */
public class EditGroupsActivity extends AppCompatActivity {

    public static class MyListItem {
        public ContactListItem2 item;
        public boolean isSelected;
    }

    ExpandableListAdapter_Groups listAdapter;
    ExpandableListView expListView;
    List<String> listDataHeader;
    HashMap<String, List<String>> listDataChild;
    private long listId;

    private ArrayList<MyListItem> initAddDlgListView() {
        final ArrayList<MyListItem> items = new ArrayList<>();
        HashSet<String> nums = new HashSet<>();
        for (ContactListItem2 li : AppDb.getInstance(this).contactInListDao().getByListId(listId)) {
            if (nums.contains(li.number)) continue;
            nums.add(li.number);
            MyListItem myListItem = new MyListItem();
            myListItem.isSelected = false;
            myListItem.item = li;
            items.add(myListItem);
        }
        return items;
    }

    private ArrayList<MyListItem> initAddDlgListView(ListView listView, final ArrayList<MyListItem> items) {
        //http://www.mysamplecode.com/2012/07/android-listview-checkbox-example.html
        listView.setAdapter(new ArrayAdapter<MyListItem>(this,
                R.layout.contactgrouplist_item, items) {
            class ViewHolder {
                TextView code;
                CheckBox name;
            }
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ViewHolder holder;

                if (convertView == null) {
                    LayoutInflater vi = (LayoutInflater) getSystemService(
                            Context.LAYOUT_INFLATER_SERVICE);
                    convertView = vi.inflate(R.layout.contactgrouplist_item, null);

                    holder = new ViewHolder();
                    holder.code = convertView.findViewById(R.id.code);
                    holder.name = convertView.findViewById(R.id.checkBox1);
                    convertView.setTag(holder);

                    holder.name.setOnClickListener(v -> {
                        CheckBox cb = (CheckBox) v ;
                        MyListItem country = (MyListItem) cb.getTag();
                        country.isSelected = cb.isChecked();
                    });
                } else {
                    holder = (ViewHolder) convertView.getTag();
                }

                MyListItem country = items.get(position);
                holder.code.setText(" (" +  country.item.number + ")");
                holder.name.setText(country.item.name);
                holder.name.setChecked(country.isSelected);
                holder.name.setTag(country);

                return convertView;
            }
        });
        return items;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_groups);
        Intent intent = getIntent();
        listId = intent.getLongExtra("list", -1);
        findViewById(R.id.fab_addgroup).setOnClickListener(v -> {
            final ArrayList<MyListItem> items = initAddDlgListView();
            if (items.size() == 0) {
                Toast.makeText(EditGroupsActivity.this, R.string.contacts_list_emptry,
                        Toast.LENGTH_LONG).show();
                return;
            }
            final Dialog dlg = new Dialog(EditGroupsActivity.this);
            dlg.setContentView(R.layout.dlg_add_group);
            dlg.setTitle(R.string.add_contactgroup);
            ListView listView = dlg.findViewById(R.id.listViewGroupContacts);
            final ArrayList<MyListItem> myListItems = initAddDlgListView(listView, items);
            dlg.findViewById(R.id.btnAddGroupDlgOk).setOnClickListener(v1 -> {
                EditText txt = dlg.findViewById(R.id.txtGroupName);
                if (txt.getText().toString().trim().length() == 0) {
                    Toast.makeText(dlg.getContext(), R.string.please_input_group_name,
                            Toast.LENGTH_LONG).show();
                    return;
                }
                for (String s : listDataHeader) {
                    if (s.equals(txt.getText().toString())) {
                        Toast.makeText(dlg.getContext(), R.string.group_name_already_exists,
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                int numSelected = 0;
                for (MyListItem li : myListItems)
                    if (li.isSelected)
                        ++numSelected;
                if (numSelected < 2) {
                    Toast.makeText(dlg.getContext(), R.string.group_members_atleast_two,
                            Toast.LENGTH_LONG).show();
                    return;
                }
                ContactsListGroup g = new ContactsListGroup();
                g.name = txt.getText().toString();
                g.contactListId = listId;
                g.id = AppDb.getInstance(this).contactGroupDao().insert(g);
                listDataHeader.add(g.name);
                ArrayList<String> childs = new ArrayList<>();
                for (MyListItem li : myListItems) {
                    if (li.isSelected) {
                        ContactInGroup contactInGroup = new ContactInGroup();
                        contactInGroup.groupId = g.id;
                        contactInGroup.contactId = li.item.id;
                        AppDb.getInstance(this).contactInGroupDao().insert(contactInGroup);
                        childs.add(li.item.name + " (" + li.item.number + ")");
                    }
                }
                listDataChild.put(g.name, childs);
                listAdapter.notifyDataSetChanged();
                dlg.dismiss();
            });
            dlg.show();
        });

        // get the listview
        expListView = findViewById(R.id.expList_groups);

        // preparing list data
        prepareListData();

        listAdapter = new ExpandableListAdapter_Groups(this,
                listDataHeader, listDataChild, listId);

        // setting list adapter
        expListView.setAdapter(listAdapter);
    }

    /*
     * Preparing the list data
     */
    private void prepareListData() {
        listDataHeader = new ArrayList<>();
        listDataChild = new HashMap<>();
        for (ContactsListGroup group : AppDb.getInstance(this).contactGroupDao().getByListId(listId)) {
            String head = group.name;
            listDataHeader.add(head);
            ArrayList<String> childs = new ArrayList<>();
            for (ContactsListItem contactsListItem : AppDb.getInstance(this).contactInGroupDao()
                    .getNumbersByGroupId(group.id)) {
                childs.add(contactsListItem.name + " (" + contactsListItem.number + ")");
            }
            listDataChild.put(head, childs);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.edit_groups_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_group_help) {
            //TODO: should also display this dialog to the new user
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setCancelable(true)
                    .setMessage(getString(R.string.groups_help))
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
