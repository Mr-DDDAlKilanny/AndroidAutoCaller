package kilanny.autocaller.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

import kilanny.autocaller.data.ContactsListGroup;
import kilanny.autocaller.data.ContactsListItem;

@Entity(tableName = "contact_in_group",
        primaryKeys = {"contact_id", "group_id"},
        foreignKeys = {@ForeignKey(entity = ContactsListItem.class, parentColumns = "id", childColumns = "contact_id"),
                @ForeignKey(entity = ContactsListGroup.class, parentColumns = "id", childColumns = "group_id")},
        indices = {@Index(value = "contact_id"), @Index(value = "group_id")})
public class ContactInGroup {

    @ColumnInfo(name = "contact_id")
    public long contactId;

    @ColumnInfo(name = "group_id")
    public long groupId;
}
