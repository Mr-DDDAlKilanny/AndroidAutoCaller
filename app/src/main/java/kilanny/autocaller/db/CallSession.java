package kilanny.autocaller.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import kilanny.autocaller.data.ContactsList;
import kilanny.autocaller.data.ContactsListItem;

@Entity(tableName = "call_session",
        foreignKeys = {@ForeignKey(entity = ContactList.class, parentColumns = {"id"}, childColumns = {"list_id"})},
        indices = {@Index(value = "list_id")})
public class CallSession {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "date")
    public long date;

    @ColumnInfo(name = "list_id")
    public long listId;
}
