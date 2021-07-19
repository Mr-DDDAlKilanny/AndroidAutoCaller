package kilanny.autocaller.data;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "call_profile")
public class AutoCallProfile implements Serializable {
    static final long serialVersionUID=1L;

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "name")
    @NonNull
    public String name;

    @ColumnInfo(name = "no_reply_timeout_seconds")
    public int noReplyTimeoutSeconds;

    @ColumnInfo(name = "kill_call_after_seconds")
    public int killCallAfterSeconds;

    @Ignore
    AutoCallProfile(int id) {
        this.id = id;
    }

    @Keep
    public AutoCallProfile() {
    }
}
