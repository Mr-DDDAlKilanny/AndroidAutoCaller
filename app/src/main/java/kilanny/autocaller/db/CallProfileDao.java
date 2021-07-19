package kilanny.autocaller.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import kilanny.autocaller.data.AutoCallProfile;

@Dao
public interface CallProfileDao {

    @Query("SELECT * FROM call_profile WHERE :id = id")
    AutoCallProfile find(long id);

    @Query("SELECT * FROM call_profile")
    AutoCallProfile[] getAll();

    @Insert
    long insert(AutoCallProfile city);

    @Update
    void update(AutoCallProfile city);

    @Delete
    void delete(AutoCallProfile city);
}
