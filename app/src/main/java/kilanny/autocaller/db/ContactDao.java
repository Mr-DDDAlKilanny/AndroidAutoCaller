package kilanny.autocaller.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import kilanny.autocaller.data.ContactsListItem;

@Dao
public interface ContactDao {
    //@Query("SELECT * FROM contact_in_list WHERE list_id = :listId")
    //ContactsListItem[] getByListId(int listId);

    @Insert
    long insert(ContactsListItem city);

    @Update
    void update(ContactsListItem city);

    @Query("UPDATE contact SET call_profile_id = :callProfileId WHERE id = :contactId")
    void updateCallProfile(long contactId, Long callProfileId);

    @Query("UPDATE contact SET city_id = :cityId WHERE id = :contactId")
    void updateCity(long contactId, Long cityId);

    @Query("SELECT * FROM contact WHERE number = :number")
    ContactsListItem findByNumber(String number);

    @Query("SELECT * FROM contact WHERE city_id = :cityId")
    ContactsListItem[] getByCityId(long cityId);

    @Query("SELECT * FROM contact WHERE call_profile_id = :profileId")
    ContactsListItem[] getByProfileId(long profileId);

    @Delete
    void delete(ContactsListItem city);
}
