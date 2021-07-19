package kilanny.autocaller.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import kilanny.autocaller.data.City;

@Dao
public interface CityDao {

    @Query("SELECT * FROM city")
    City[] getAll();

    @Insert
    long insert(City city);

    @Update
    void update(City city);

    @Delete
    void delete(City city);

    @Query("SELECT * FROM city WHERE id = :cityId")
    City find(long cityId);

    @Query("SELECT * FROM city WHERE country = :country AND name = :name")
    City findByName(String country, String name);

    @Query("DELETE FROM city WHERE country = :country")
    void deleteByCountry(String country);
}
