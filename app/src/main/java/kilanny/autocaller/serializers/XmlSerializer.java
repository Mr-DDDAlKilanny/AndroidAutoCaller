package kilanny.autocaller.serializers;

import java.io.IOException;
import java.io.InputStream;

import kilanny.autocaller.data.AutoCallProfileList;
import kilanny.autocaller.data.CityList;
import kilanny.autocaller.data.ListOfCallingLists;

/**
 * Created by user on 10/15/2017.
 */
//TODO: xml serializer
public final class XmlSerializer implements Serializer {

    @Override
    public byte[] serialize(ListOfCallingLists listOfCallingLists) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListOfCallingLists deserializeListOfCallingLists(InputStream stream) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] serialize(CityList cityList) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public CityList deserializeCityList(InputStream stream) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] serialize(AutoCallProfileList autoCallProfiles) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public AutoCallProfileList deserializeAutoCallProfiles(InputStream stream) throws IOException {
        throw new UnsupportedOperationException();
    }
}
