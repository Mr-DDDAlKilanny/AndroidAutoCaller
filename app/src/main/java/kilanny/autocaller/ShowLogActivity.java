package kilanny.autocaller;

import android.content.DialogInterface;
import android.content.Intent;
import android.provider.ContactsContract;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ExpandableListView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ShowLogActivity extends AppCompatActivity {

    ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    List<String> listDataHeader;
    HashMap<String, List<String>> listDataChild;

    //http://www.androidhive.info/2013/07/android-expandable-list-view-tutorial/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_log);

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
        AutoCallLog log = AutoCallLog.getInstance(this);
        DateFormat timeInstance = SimpleDateFormat.getTimeInstance(),
            dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy HH:mm",
                    Locale.getDefault());
        for (AutoCallLog.AutoCallSession session : log.sessions) {
            String head = dateFormat.format(session.date);
            listDataHeader.add(head);
            ArrayList<String> childs = new ArrayList<>();
            for (AutoCallLog.AutoCall call : session) {
                childs.add(String.format("%s (%s): %s, %s", call.name, call.number,
                        timeInstance.format(call.date),
                        call.result == AutoCallLog.AutoCall.RESULT_NOT_ANSWERED ?
                                "لا رد" : call.result == AutoCallLog.AutoCall.RESULT_ANSWERED_OR_REJECTED ?
                                "رفض أو رد" : "غير معروف"));
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
                    .setTitle("حذف السجل")
                    .setMessage("متأكد من رغبتك في حذف السجل؟")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            AutoCallLog instance = AutoCallLog.getInstance(ShowLogActivity.this);
                            instance.sessions.clear();
                            instance.save(ShowLogActivity.this);
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
