package kilanny.autocaller.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface CallSessionItemDao {

    @Query("SELECT COUNT(*) FROM call_session_item WHERE call_session_id = :callSessionId")
    int count(long callSessionId);

    @Query("SELECT csi.*, c.number, c.name " +
            "FROM call_session_item csi" +
            " LEFT OUTER JOIN contact c ON csi.contact_id = c.id " +
            "WHERE call_session_id = :callSessionId " +
            "ORDER BY csi.id")
    CallSessionItemViewModel[] getBySessionId(long callSessionId);

    @Query("SELECT csi.*, c.number, c.name " +
            "FROM call_session_item csi" +
            " LEFT OUTER JOIN contact c ON csi.contact_id = c.id " +
            "WHERE call_session_id = :callSessionId " +
            "ORDER BY csi.id " +
            "LIMIT :position, 1")
    CallSessionItemViewModel getItemAt(long callSessionId, int position);

    @Query("SELECT * " +
            "FROM call_session_item " +
            "ORDER BY id DESC " +
            "LIMIT 1")
    CallSessionItem getLast();

    @Insert
    long insert(CallSessionItem city);

    @Update
    void update(CallSessionItem city);

    @Query("UPDATE call_session_item SET result = :result WHERE id = :id")
    void updateResult(long id, Integer result);

    @Delete
    void delete(CallSessionItem city);

    @Query("DELETE FROM call_session_item WHERE call_session_id IN " +
            "(SELECT id FROM call_session WHERE list_id = :contactListId)")
    void deleteByContactListId(long contactListId);
}
