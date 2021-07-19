package kilanny.autocaller.data;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import kilanny.autocaller.R;
import kilanny.autocaller.serializers.SerializerFactory;
import kilanny.autocaller.utils.PrayTimes;

/**
 * Created by user on 12/6/2017.
 */
@Deprecated
public class CityList extends ArrayList<City> implements Serializable {

    private static final String LIST_FILE_NAME = "CityList.dat";

    private int idCounter = 1;

    public CityList(Context context) {
        try {
            FileInputStream fis = context.openFileInput(LIST_FILE_NAME);
            CityList instance = SerializerFactory.getSerializer()
                    .deserializeCityList(fis);
            addAll(instance);
            idCounter = instance.idCounter;
            fis.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            addInitialCities(context);
        }
    }

    public void save(Context context) {
        try {
            FileOutputStream fos = context.openFileOutput(LIST_FILE_NAME,
                    Context.MODE_PRIVATE);
            byte[] bytes = SerializerFactory.getSerializer()
                    .serialize(this);
            fos.write(bytes, 0, bytes.length);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public City createNewCity() {
        return new City(idCounter++);
    }

    public City addNewCity() {
        City city = createNewCity();
        add(city);
        return city;
    }

    private void addInitialCities(Context context) {
        City city = addNewCity();
        city.prayerCalcMethod = PrayTimes.CALC_Makkah;
        city.asrPrayerCalcMethod = PrayTimes.ASR_Shafii;
        city.name = context.getString(R.string.makkah);
        city.plateName = context.getString(R.string.makkah);
        city.country = context.getString(R.string.saudi);
        city.timezone = 3;
        city.minMinutesBeforeSunrise = 5;
        city.minMinuteAfterFajr = 5;
        city.lat = 21.389082;
        city.lng = 39.857912;

        city = addNewCity();
        city.prayerCalcMethod = PrayTimes.CALC_Makkah;
        city.asrPrayerCalcMethod = PrayTimes.ASR_Shafii;
        city.name = context.getString(R.string.madinah);
        city.plateName = context.getString(R.string.madinah);
        city.country = context.getString(R.string.saudi);
        city.timezone = 3;
        city.minMinutesBeforeSunrise = 5;
        city.minMinuteAfterFajr = 5;
        city.lat = 24.524654;
        city.lng = 39.569184;

        city = addNewCity();
        city.prayerCalcMethod = PrayTimes.CALC_Makkah;
        city.asrPrayerCalcMethod = PrayTimes.ASR_Shafii;
        city.name = context.getString(R.string.riyadh);
        city.plateName = context.getString(R.string.riyadh);
        city.country = context.getString(R.string.saudi);
        city.timezone = 3;
        city.minMinutesBeforeSunrise = 5;
        city.minMinuteAfterFajr = 5;
        city.lat = 24.713552;
        city.lng = 46.675296;

        city = addNewCity();
        city.prayerCalcMethod = PrayTimes.CALC_Egypt;
        city.asrPrayerCalcMethod = PrayTimes.ASR_Shafii;
        city.name = context.getString(R.string.cairo);
        city.plateName = context.getString(R.string.cairo);
        city.country = context.getString(R.string.egypt);
        city.timezone = 2;
        city.minMinutesBeforeSunrise = 5;
        city.minMinuteAfterFajr = 5;
        city.lat = 30.044420;
        city.lng = 31.235712;

        city = addNewCity();
        city.prayerCalcMethod = PrayTimes.CALC_Makkah;
        city.asrPrayerCalcMethod = PrayTimes.ASR_Shafii;
        city.name = context.getString(R.string.tabuk);
        city.plateName = context.getString(R.string.tabuk);
        city.country = context.getString(R.string.saudi);
        city.timezone = 3;
        city.minMinutesBeforeSunrise = 5;
        city.minMinuteAfterFajr = 5;
        city.lat = 28.383508;
        city.lng = 36.566191;

        city = addNewCity();
        city.prayerCalcMethod = PrayTimes.CALC_Makkah;
        city.asrPrayerCalcMethod = PrayTimes.ASR_Shafii;
        city.name = context.getString(R.string.jeddah);
        city.plateName = context.getString(R.string.jeddah);
        city.country = context.getString(R.string.saudi);
        city.timezone = 3;
        city.minMinutesBeforeSunrise = 5;
        city.minMinuteAfterFajr = 5;
        city.lat = 21.285407;
        city.lng = 39.237551;

        city = addNewCity();
        city.prayerCalcMethod = PrayTimes.CALC_Makkah;
        city.asrPrayerCalcMethod = PrayTimes.ASR_Shafii;
        city.name = context.getString(R.string.dammam);
        city.plateName = context.getString(R.string.dammam);
        city.country = context.getString(R.string.saudi);
        city.timezone = 3;
        city.minMinutesBeforeSunrise = 5;
        city.minMinuteAfterFajr = 5;
        city.lat = 26.392667;
        city.lng = 49.977714;

        city = addNewCity();
        city.prayerCalcMethod = PrayTimes.CALC_Egypt;
        city.asrPrayerCalcMethod = PrayTimes.ASR_Shafii;
        city.name = context.getString(R.string.alexandria);
        city.plateName = context.getString(R.string.alexandria);
        city.country = context.getString(R.string.egypt);
        city.timezone = 2;
        city.minMinutesBeforeSunrise = 5;
        city.minMinuteAfterFajr = 5;
        city.lat = 31.200092;
        city.lng = 29.918739;

        city = addNewCity();
        city.prayerCalcMethod = PrayTimes.CALC_Egypt;
        city.asrPrayerCalcMethod = PrayTimes.ASR_Shafii;
        city.name = context.getString(R.string.benha);
        city.plateName = context.getString(R.string.benha);
        city.country = context.getString(R.string.egypt);
        city.timezone = 2;
        city.minMinutesBeforeSunrise = 5;
        city.minMinuteAfterFajr = 5;
        city.lat = 30.465993;
        city.lng = 31.184831;
    }

    public City findByName(@NonNull String country, @NonNull String name) {
        for (City city : this) {
            if (country.equals(city.country) && name.equals(city.name)) {
                return city;
            }
        }
        return null;
    }

    public City findById(int id) {
        for (City city : this) {
            if (city.id == id) {
                return city;
            }
        }
        return null;
    }
}
