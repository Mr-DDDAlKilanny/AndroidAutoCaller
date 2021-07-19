package kilanny.autocaller.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;

public class ContactListItem2 {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public transient long id;

    @ColumnInfo(name = "name")
    @NonNull
    public String name;

    @ColumnInfo(name = "number")
    @NonNull
    public String number;

    @ColumnInfo(name = "call_count")
    public int callCount;

    @ColumnInfo(name = "index")
    public int index;

    @Nullable
    @ColumnInfo(name = "city_id")
    public Integer cityId;

    @Nullable
    @ColumnInfo(name = "call_profile_id")
    public Integer callProfileId;

    @ColumnInfo(name = "contact_id")
    public long contactId;
}
