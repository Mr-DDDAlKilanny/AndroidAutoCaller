package kilanny.autocaller.db;

import androidx.room.ColumnInfo;

public class CallSessionItemViewModel extends CallSessionItem {

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "number")
    public String number;
}
