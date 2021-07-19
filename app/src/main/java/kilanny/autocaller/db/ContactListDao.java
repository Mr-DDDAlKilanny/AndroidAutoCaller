package kilanny.autocaller.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface ContactListDao {

    @Query("SELECT * FROM contact_list WHERE id = :id")
    ContactList find(long id);

    @Query("SELECT * FROM contact_list")
    ContactList[] getAll();

    @Insert
    long insert(ContactList city);

    @Update
    void update(ContactList city);

    @Delete
    void delete(ContactList city);
}
