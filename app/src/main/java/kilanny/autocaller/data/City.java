package kilanny.autocaller.data;

import android.content.Intent;

import java.io.Serializable;

/**
 * Created by user on 12/6/2017.
 */
public class City implements Serializable {
    static final long serialVersionUID=1L;

    public final int id;

    public int prayerCalcMethod, asrPrayerCalcMethod;
    public int timezone, minMinutesBeforeSunrise, minMinuteAfterFajr;
    public double lng, lat;
    public String name, plateName;
    public String country;

    City(int id) {
        this.id = id;
    }

    public void setFrom(City city) {
        if (city.id != id)
            throw new IllegalArgumentException("id of copied city is not matched with current id");
        country = city.country;
        name = city.name;
        plateName = city.plateName;
        timezone = city.timezone;
        lng = city.lng;
        lat = city.lat;
        minMinuteAfterFajr = city.minMinuteAfterFajr;
        minMinutesBeforeSunrise = city.minMinutesBeforeSunrise;
        prayerCalcMethod = city.prayerCalcMethod;
        asrPrayerCalcMethod = city.asrPrayerCalcMethod;
    }

    public static City getCityFromIntent(Intent intent) {
        City city = new City(intent.getIntExtra("id", 0));
        city.country = intent.getStringExtra("country");
        city.name = intent.getStringExtra("city");
        city.plateName = intent.getStringExtra("plateName");
        city.timezone = intent.getIntExtra("timezone", 0);
        city.minMinutesBeforeSunrise = intent.getIntExtra("minMinutesBeforeSunrise", 0);
        city.minMinuteAfterFajr = intent.getIntExtra("minMinuteAfterFajr", 0);
        city.lng = intent.getDoubleExtra("lng", 0);
        city.lat = intent.getDoubleExtra("lat", 0);
        city.prayerCalcMethod = intent.getIntExtra("prayerCalcMethod", 0);
        city.asrPrayerCalcMethod = intent.getIntExtra("asrPrayerCalcMethod", 0);
        return city;
    }

    public void putExtraInIntent(Intent intent) {
        intent.putExtra("id", id);
        intent.putExtra("country", country);
        intent.putExtra("city", name);
        intent.putExtra("plateName", plateName);
        intent.putExtra("timezone", timezone);
        intent.putExtra("minMinuteAfterFajr", minMinuteAfterFajr);
        intent.putExtra("minMinutesBeforeSunrise", minMinutesBeforeSunrise);
        intent.putExtra("lng", lng);
        intent.putExtra("lat", lat);
        intent.putExtra("prayerCalcMethod", prayerCalcMethod);
        intent.putExtra("asrPrayerCalcMethod", asrPrayerCalcMethod);
    }
}
