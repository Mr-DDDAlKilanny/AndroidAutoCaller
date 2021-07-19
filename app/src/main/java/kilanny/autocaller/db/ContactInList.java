package kilanny.autocaller.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

import kilanny.autocaller.data.ContactsListItem;

@Entity(tableName = "contact_in_list",
        primaryKeys = {"contact_id", "list_id", "index"},
        foreignKeys = {@ForeignKey(entity = ContactList.class, parentColumns = "id", childColumns = "list_id"),
                @ForeignKey(entity = ContactsListItem.class, parentColumns = "id", childColumns = "contact_id")},
        indices = {@Index(value = "list_id"), @Index(value = "contact_id"), @Index(value = "index")})
public class ContactInList {

    @ColumnInfo(name = "contact_id")
    public long contactId;

    @ColumnInfo(name = "list_id")
    public long listId;

    @ColumnInfo(name = "call_count")
    public int callCount;

    @ColumnInfo(name = "index")
    public int index;
}
