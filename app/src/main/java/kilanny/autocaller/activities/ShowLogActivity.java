package kilanny.autocaller.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SectionIndexer;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import in.myinnos.alphabetsindexfastscrollrecycler.IndexFastScrollRecyclerView;
import kilanny.autocaller.R;
import kilanny.autocaller.db.AppDb;
import kilanny.autocaller.db.CallSession;
import kilanny.autocaller.db.CallSessionItem;
import kilanny.autocaller.db.CallSessionItemViewModel;
import kilanny.autocaller.db.CallSessionMonthViewModel;
import kilanny.autocaller.utils.AnalyticsTrackers;

public class ShowLogActivity extends AppCompatActivity {

    public static class ParentRecyclerViewAdapter extends RecyclerView.Adapter<ParentRecyclerViewViewHolder>
            implements SectionIndexer {

        public final long mListId;
        public final Context mContext;
        private final int mCount;
        private final CallSessionMonthViewModel[] mMonths;
        private final String[] mSections;

        public ParentRecyclerViewAdapter(Context context, long listId) {
            mListId = listId;
            mContext = context;
            mCount = AppDb.getInstance(mContext).callSessionDao().count(mListId);
            mMonths = AppDb.getInstance(mContext).callSessionDao().getMonths(mListId);
            mSections = Arrays.stream(mMonths).map(s -> s.year + "/" + s.month).toArray(String[]::new);
            Arrays.sort(mSections, Collections.reverseOrder());
        }

        @NonNull
        @Override
        public ParentRecyclerViewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ParentRecyclerViewViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.showloglist_group, parent, false), viewType);
        }

        @Override
        public void onBindViewHolder(@NonNull ParentRecyclerViewViewHolder holder, int position) {
            holder.bind(AppDb.getInstance(mContext).callSessionDao().getItemAt(mListId, position));
        }

        @Override
        public int getItemCount() {
            return mCount;
        }

        @Override
        public Object[] getSections() {
            return mSections;
        }

        @Override
        public int getPositionForSection(int sectionIndex) {
            int sum = 0;
            for (int i = 0; i < sectionIndex; ++i) {
                sum += mMonths[i].count;
            }
            return sum;
        }

        @Override
        public int getSectionForPosition(int position) {
            int sum = 0;
            for (int index = 0; index < mMonths.length; ++index) {
                sum += mMonths[index].count;
                if (position < sum)
                    return index;
            }
            return -1;
        }
    }

    public static class ParentRecyclerViewViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final TextView mTextView;
        private final ImageButton btnExpand;
        private final RecyclerView mRecyclerView;

        private long callSessionId;
        private boolean isExpanded = false;

        public ParentRecyclerViewViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            mTextView = itemView.findViewById(R.id.lblListHeader);
            btnExpand = itemView.findViewById(R.id.btnExpand);
            mRecyclerView = itemView.findViewById(R.id.child_recycler_view);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));

            // for fixing loading all items once
            mRecyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {

                @Override
                public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                    int action = e.getAction();
                    switch (action) {
                        case MotionEvent.ACTION_MOVE:
                            rv.getParent().requestDisallowInterceptTouchEvent(true);
                            break;
                    }
                    return false;
                }

                @Override
                public void onTouchEvent(RecyclerView rv, MotionEvent e) {

                }

                @Override
                public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

                }
            });
            itemView.setOnClickListener(this);
            btnExpand.setOnClickListener(this);
        }

        private void collapse() {
            isExpanded = false;
            mRecyclerView.setVisibility(View.GONE);
            mRecyclerView.setAdapter(null);
            btnExpand.setImageResource(android.R.drawable.arrow_down_float);
        }

        public void bind(CallSession callSession) {
            callSessionId = callSession.id;
            DateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy HH:mm", Locale.getDefault());
            //test if default locale is not working, replace with US
            if (Character.isDigit(dateFormat.format(new Date()).charAt(0)))
                dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy HH:mm", Locale.US);
            mTextView.setText(dateFormat.format(new Date(callSession.date)));
            collapse();
        }

        @Override
        public void onClick(View v) {
            if (!isExpanded) {
                isExpanded = true;
                btnExpand.setImageResource(android.R.drawable.arrow_up_float);
                if (callSessionId > 0) {
                    ChildRecyclerViewAdapter adapter = new ChildRecyclerViewAdapter(
                            mTextView.getContext(), callSessionId);
                    // for fixing loading all items once
                    ViewGroup.LayoutParams params = mRecyclerView.getLayoutParams();
                    params.height = Math.min(1250, adapter.getItemCount() * 250);
                    mRecyclerView.setLayoutParams(params);
                    mRecyclerView.setAdapter(adapter);
                    mRecyclerView.setVisibility(View.VISIBLE);
                }
            } else {
                collapse();
            }
        }
    }

    public static class ChildRecyclerViewAdapter extends RecyclerView.Adapter<ChildRecyclerViewViewHolder> {

        public final long mSessionId;
        public final Context mContext;
        public final int mCount;
        //private final CallSessionItemViewModel[] mItems;

        public ChildRecyclerViewAdapter(Context context, long sessionId) {
            mSessionId = sessionId;
            mContext = context;
            mCount = AppDb.getInstance(mContext).callSessionItemDao().count(mSessionId);
            //mItems = AppDb.getInstance(mContext).callSessionItemDao().getBySessionId(mSessionId);
        }

        @NonNull
        @Override
        public ChildRecyclerViewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ChildRecyclerViewViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.showloglist_item, parent, false), viewType);
        }

        @Override
        public void onBindViewHolder(@NonNull ChildRecyclerViewViewHolder holder, int position) {
            holder.bind(AppDb.getInstance(mContext).callSessionItemDao().getItemAt(mSessionId, position));
            //holder.bind(mItems[position]);
            Log.d("child-recycler", "Bound to item #" + position);
        }

        @Override
        public int getItemCount() {
            return mCount;
            //return mItems.length;
        }
    }

    public static class ChildRecyclerViewViewHolder extends RecyclerView.ViewHolder {

        private final TextView mTextView;
        private final Context mContext;

        public ChildRecyclerViewViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            mTextView = itemView.findViewById(R.id.lblListItem);
            mContext = mTextView.getContext();
        }

        public void bind(CallSessionItemViewModel callSession) {
            DateFormat timeInstance = SimpleDateFormat.getTimeInstance();
            String text;
            if (callSession.date != null && callSession.result != null
                    && callSession.result <= CallSessionItem.RESULT_ANSWERED_OR_REJECTED) {
                text = String.format("%s (%s): %s, %s", callSession.name, callSession.number,
                        timeInstance.format(new Date(callSession.date)),
                        callSession.result == CallSessionItem.RESULT_NOT_ANSWERED ?
                                mContext.getString(R.string.call_status_no_answer)
                                : callSession.result == CallSessionItem.RESULT_ANSWERED_OR_REJECTED ?
                                mContext.getString(R.string.call_status_answered_or_rejected)
                                : mContext.getString(R.string.call_status_unknown));
            } else if (callSession.date != null && callSession.result != null) {
                text = String.format("%s (%s): %s, %s", callSession.name, callSession.number,
                        timeInstance.format(new Date(callSession.date)),
                        callSession.result == CallSessionItem.RESULT_IGNORED_BEFORE_FAJR ?
                                mContext.getString(R.string.ignored_before_fajr)
                                : callSession.result == CallSessionItem.RESULT_IGNORED_AFTER_SUNRISE ?
                                mContext.getString(R.string.ignored_after_sunrise)
                                : mContext.getString(R.string.call_status_unknown));
            } else {
                text = mContext.getString(R.string.auto_call_restart);
            }

            mTextView.setText(text);
        }
    }

    private long listId;
    private ParentRecyclerViewAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_log);
        Intent intent = getIntent();
        listId = intent.getLongExtra("list", -1);
        IndexFastScrollRecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setIndexbarMargin(48);
        recyclerView.setIndexbarWidth(48);
        recyclerView.setIndexTextSize(9);
        recyclerView.setIndexBarTextColor("#33334c");
        recyclerView.setIndexBarColor("#FF334c");
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        refreshAdapter(recyclerView);
    }

    private void refreshAdapter(IndexFastScrollRecyclerView recyclerView) {
        recyclerView.setAdapter(mAdapter = new ParentRecyclerViewAdapter(this, listId));
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
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(getString(R.string.delete_log_confirm_title))
                    .setMessage(getString(R.string.delete_log_confirm_body))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        AppDb.getInstance(this).callSessionItemDao().deleteByContactListId(listId);
                        AppDb.getInstance(this).callSessionDao().deleteByContactListId(listId);
                        IndexFastScrollRecyclerView recyclerView = findViewById(R.id.recycler_view);
                        refreshAdapter(recyclerView);
                        AnalyticsTrackers.getInstance(ShowLogActivity.this).logClearLog();
                    })
                    .setNegativeButton(android.R.string.no, null).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
