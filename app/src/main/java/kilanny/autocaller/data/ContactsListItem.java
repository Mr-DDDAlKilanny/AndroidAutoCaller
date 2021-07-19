package kilanny.autocaller.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.io.Serializable;

/**
 * Created by Yasser on 05/31/2016.
 */
@Entity(tableName = "contact",
foreignKeys = {@ForeignKey(entity = City.class, parentColumns = {"id"}, childColumns = {"city_id"}),
        @ForeignKey(entity = AutoCallProfile.class, parentColumns = {"id"}, childColumns = {"call_profile_id"})},
indices = {@Index(value = "city_id"), @Index(value = "call_profile_id")})
public class ContactsListItem implements Serializable {
    static final long serialVersionUID=6222460388325562750L;

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public transient long id;

    @ColumnInfo(name = "name")
    @NonNull
    public String name;

    @ColumnInfo(name = "number")
    @NonNull
    public String number;

    @Ignore
    @Deprecated
    public int callCount;

    @Ignore
    @Deprecated
    public int index;

    @Nullable
    @ColumnInfo(name = "city_id")
    public Integer cityId;

    @Nullable
    @ColumnInfo(name = "call_profile_id")
    public Integer callProfileId;
}
