package kilanny.autocaller.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface CallSessionDao {

    @Query("SELECT * FROM call_session WHERE list_id = :listId ORDER BY date DESC LIMIT :position, 1")
    CallSession getItemAt(long listId, int position);

    @Query("SELECT COUNT(*) FROM call_session WHERE list_id = :listId")
    int count(long listId);

    @Query("SELECT strftime('%Y', datetime(date / 1000, 'unixepoch', 'localtime')) year," +
            "   strftime('%m', datetime(date / 1000, 'unixepoch', 'localtime')) month," +
            "   COUNT(*) `count`" +
            "FROM call_session " +
            "WHERE list_id = :listId " +
            "GROUP BY strftime('%m', datetime(date / 1000, 'unixepoch', 'localtime')), " +
            "   strftime('%Y', datetime(date / 1000, 'unixepoch', 'localtime')) " +
            "ORDER BY 1 DESC, 2 DESC")
    CallSessionMonthViewModel[] getMonths(long listId);

    @Insert
    long insert(CallSession city);

    @Update
    void update(CallSession city);

    @Delete
    void delete(CallSession city);

    @Query("DELETE FROM call_session WHERE list_id = :contactListId")
    void deleteByContactListId(long contactListId);
}
