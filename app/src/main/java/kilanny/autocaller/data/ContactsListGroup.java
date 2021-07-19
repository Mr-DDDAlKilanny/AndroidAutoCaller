package kilanny.autocaller.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.HashMap;

import kilanny.autocaller.db.ContactList;

/**
 * Created by Yasser on 08/12/2016.
 */
@Entity(tableName = "contact_group",
foreignKeys = @ForeignKey(entity = ContactList.class, parentColumns = "id", childColumns = "contact_list_id"),
indices = @Index(value = "contact_list_id"))
public class ContactsListGroup implements Serializable {
    static final long serialVersionUID =1L;

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public transient long id;

    @ColumnInfo(name = "name")
    @NonNull
    public String name;

    @ColumnInfo(name = "contact_list_id")
    public transient long contactListId;

    //<number, name>
    @Ignore
    @Deprecated
    public HashMap<String, String> contacts = new HashMap<>();
}
