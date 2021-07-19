package kilanny.autocaller.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import kilanny.autocaller.data.ContactsListItem;

@Dao
public interface ContactInGroupDao {

    @Query("SELECT * FROM contact_in_group WHERE contact_id = :contactId")
    ContactInGroup[] getByContactId(long contactId);

    @Query("SELECT c.id, c.name, c.number, c.city_id, c.call_profile_id " +
            "FROM contact_in_group g" +
            "   INNER JOIN contact c ON g.contact_id = c.id " +
            "WHERE group_id = :groupId")
    ContactsListItem[] getNumbersByGroupId(long groupId);

    @Insert
    void insert(ContactInGroup city);

    @Update
    void update(ContactInGroup city);

    @Delete
    void delete(ContactInGroup city);

    @Query("DELETE FROM contact_in_group WHERE group_id = :groupId")
    void deleteByGroupId(long groupId);
}
