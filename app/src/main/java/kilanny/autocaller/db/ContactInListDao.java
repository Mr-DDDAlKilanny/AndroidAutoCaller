package kilanny.autocaller.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import kilanny.autocaller.data.ContactsListItem;

@Dao
public interface ContactInListDao {

    @Query("SELECT c.id, c.name, c.number, l.call_count, l.`index`, c.call_profile_id, c.city_id, c.id `contact_id` " +
            "FROM contact_in_list l" +
            " INNER JOIN contact c ON l.contact_id = c.id " +
            "WHERE list_id = :listId " +
            "ORDER BY l.`index`")
    ContactListItem2[] getByListId(long listId);

    @Insert
    void insert(ContactInList city);

    @Query("UPDATE contact_in_list SET call_count = :callCount WHERE list_id = :listId AND contact_id = :contactId")
    void updateCallCount(long listId, long contactId, int callCount);

    @Query("UPDATE contact_in_list SET `index` = :newIndex WHERE list_id = :listId AND contact_id = :contactId AND `index` = :oldIndex")
    void updateIndex(long listId, long contactId, int oldIndex, int newIndex);

    @Query("UPDATE contact_in_list " +
            "SET `index` = `index` - 1 " +
            "WHERE list_id = :listId AND `index` >= :index")
    void decrementIndex(long listId, int index);

    @Query("DELETE FROM contact_in_list WHERE list_id = :listId AND contact_id = :contactId")
    void delete(long listId, long contactId);
}
