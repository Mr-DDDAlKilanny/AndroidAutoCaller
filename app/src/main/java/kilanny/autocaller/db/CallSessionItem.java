package kilanny.autocaller.db;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import kilanny.autocaller.data.ContactsListItem;

@Entity(tableName = "call_session_item",
        foreignKeys = {@ForeignKey(entity = ContactsListItem.class, parentColumns = {"id"}, childColumns = {"contact_id"}),
                @ForeignKey(entity = CallSession.class, parentColumns = {"id"}, childColumns = {"call_session_id"})},
        indices = {@Index(value = "contact_id"), @Index("call_session_id")})
public class CallSessionItem {

    public static final int RESULT_UNKNOWN = 0;
    public static final int RESULT_NOT_ANSWERED = 1;
    public static final int RESULT_ANSWERED_OR_REJECTED = 2;
    public static final byte RESULT_IGNORED_BEFORE_FAJR = 3;
    public static final byte RESULT_IGNORED_AFTER_SUNRISE = 4;

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "call_session_id")
    public long callSessionId;

    @ColumnInfo(name = "contact_id")
    @Nullable
    public Long contactId;

    @ColumnInfo(name = "date")
    @Nullable
    public Long date;

    @ColumnInfo(name = "result")
    @Nullable
    public Integer result;
}
