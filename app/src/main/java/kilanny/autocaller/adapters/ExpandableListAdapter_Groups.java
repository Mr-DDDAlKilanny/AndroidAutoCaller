package kilanny.autocaller.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;

import kilanny.autocaller.R;
import kilanny.autocaller.data.ContactsList;
import kilanny.autocaller.data.ContactsListGroup;
import kilanny.autocaller.data.ContactsListGroupList;
import kilanny.autocaller.db.AppDb;

public class ExpandableListAdapter_Groups extends BaseExpandableListAdapter {

    private Context _context;
    private List<String> _listDataHeader; // header titles
    // child data in format of header title, child title
    private HashMap<String, List<String>> _listDataChild;
    private long contactsListId;

    public ExpandableListAdapter_Groups(Context context, List<String> listDataHeader,
                                        HashMap<String, List<String>> listChildData,
                                        long contactsListId) {
        this._context = context;
        this.contactsListId = contactsListId;
        this._listDataHeader = listDataHeader;
        this._listDataChild = listChildData;
    }

    @Override
    public Object getChild(int groupPosition, int childPosititon) {
        return this._listDataChild.get(this._listDataHeader.get(groupPosition))
                .get(childPosititon);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        final String childText = (String) getChild(groupPosition, childPosition);

        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this._context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.contactgroup_item, null);
        }

        TextView txtListChild = (TextView) convertView.findViewById(R.id.lblListItem_1);

        txtListChild.setText(childText);
        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this._listDataChild.get(this._listDataHeader.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return this._listDataHeader.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return this._listDataHeader.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        final String headerTitle = (String) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this._context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.contactgroup_group, null);
        }

        TextView lblListHeader = convertView.findViewById(R.id.lblListHeader_1);
        lblListHeader.setTypeface(null, Typeface.BOLD);
        lblListHeader.setText(headerTitle);

        Button btn = convertView.findViewById(R.id.btnDeleteGroup);
        btn.setOnClickListener(v -> new androidx.appcompat.app.AlertDialog.Builder(_context)
                .setTitle(R.string.confirm_delete_group_title)
                .setMessage(R.string.confirm_delete_group_body)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                    _listDataChild.remove(headerTitle);
                    _listDataHeader.remove(headerTitle);
                    ContactsListGroup remove = AppDb.getInstance(_context).contactGroupDao()
                            .findByListIdAndName(this.contactsListId, headerTitle);
                    if (remove != null) {
                        AppDb.getInstance(_context).contactInGroupDao().deleteByGroupId(remove.id);
                        AppDb.getInstance(_context).contactGroupDao().delete(remove);
                    }
                    notifyDataSetChanged();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show());

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}