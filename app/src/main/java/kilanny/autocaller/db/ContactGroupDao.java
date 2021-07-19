package kilanny.autocaller.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import kilanny.autocaller.data.ContactsListGroup;

@Dao
public interface ContactGroupDao {
    @Query("SELECT * FROM contact_group")
    ContactsListGroup[] getAll();

    @Query("SELECT * FROM contact_group WHERE contact_list_id = :listId")
    ContactsListGroup[] getByListId(long listId);

    @Query("SELECT * FROM contact_group WHERE contact_list_id = :listId AND name = :name")
    ContactsListGroup findByListIdAndName(long listId, String name);

    @Insert
    long insert(ContactsListGroup city);

    @Update
    void update(ContactsListGroup city);

    @Delete
    void delete(ContactsListGroup city);
}
