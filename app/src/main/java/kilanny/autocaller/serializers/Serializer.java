package kilanny.autocaller.serializers;

import java.io.IOException;
import java.io.InputStream;

import kilanny.autocaller.data.AutoCallProfileList;
import kilanny.autocaller.data.CityList;
import kilanny.autocaller.data.ListOfCallingLists;

/**
 * Created by user on 10/15/2017.
 */

public interface Serializer {
    byte[] serialize(ListOfCallingLists listOfCallingLists) throws IOException;
    ListOfCallingLists deserializeListOfCallingLists(InputStream stream) throws IOException;

    byte[] serialize(CityList cityList) throws IOException;
    CityList deserializeCityList(InputStream stream) throws IOException;

    byte[] serialize(AutoCallProfileList autoCallProfiles) throws IOException;
    AutoCallProfileList deserializeAutoCallProfiles(InputStream stream) throws IOException;
}
