package kilanny.autocaller.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ExpandableListView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import kilanny.autocaller.data.AutoCallLog;
import kilanny.autocaller.data.ContactsList;
import kilanny.autocaller.adapters.ExpandableListAdapter;
import kilanny.autocaller.data.ListOfCallingLists;
import kilanny.autocaller.R;

public class ShowLogActivity extends AppCompatActivity {

    ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    List<String> listDataHeader;
    HashMap<String, List<String>> listDataChild;
    private ContactsList list;

    //http://www.androidhive.info/2013/07/android-expandable-list-view-tutorial/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_log);

        Intent intent = getIntent();
        list = ListOfCallingLists.getInstance(this).getById(intent.getIntExtra("list", -1));

        // get the listview
        expListView = (ExpandableListView) findViewById(R.id.expList);

        // preparing list data
        prepareListData();

        listAdapter = new ExpandableListAdapter(this, listDataHeader, listDataChild);

        // setting list adapter
        expListView.setAdapter(listAdapter);
    }

    /*
     * Preparing the list data
     */
    private void prepareListData() {
        listDataHeader = new ArrayList<>();
        listDataChild = new HashMap<>();
        DateFormat timeInstance = SimpleDateFormat.getTimeInstance(),
            dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy HH:mm", Locale.getDefault());
        //test if default locale is not working, replace with US
        if (Character.isDigit(dateFormat.format(new Date()).charAt(0)))
            dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy HH:mm", Locale.US);
        for (int i = list.getLog().sessions.size() - 1; i >= 0; --i) {
            AutoCallLog.AutoCallSession session = list.getLog().sessions.get(i);
            String head = dateFormat.format(session.date);
            listDataHeader.add(head);
            ArrayList<String> childs = new ArrayList<>();
            for (AutoCallLog.AutoCallItem item : session) {
                if (item instanceof AutoCallLog.AutoCall) {
                    AutoCallLog.AutoCall call = (AutoCallLog.AutoCall) item;
                    childs.add(String.format("%s (%s): %s, %s", call.name, call.number,
                            timeInstance.format(call.date),
                            call.result == AutoCallLog.AutoCall.RESULT_NOT_ANSWERED ?
                                    getString(R.string.call_status_no_answer)
                                    : call.result == AutoCallLog.AutoCall.RESULT_ANSWERED_OR_REJECTED ?
                                    getString(R.string.call_status_answered_or_rejected)
                                    : getString(R.string.call_status_unknown)));
                } else
                    childs.add(getString(R.string.auto_call_restart));
            }
            listDataChild.put(head, childs);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.showlog_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_delete_log) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.delete_log_confirm_title))
                    .setMessage(getString(R.string.delete_log_confirm_body))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            list.getLog().sessions.clear();
                            list.save(ShowLogActivity.this);
                            listDataHeader.clear();
                            listDataChild.clear();
                            listAdapter.notifyDataSetChanged();
                        }})
                    .setNegativeButton(android.R.string.no, null).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
