package kilanny.autocaller.serializers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import kilanny.autocaller.data.CityList;
import kilanny.autocaller.data.ListOfCallingLists;

/**
 * Created by user on 10/15/2017.
 */

public class BinarySerializer implements Serializer {
    @Override
    public byte[] serialize(ListOfCallingLists listOfCallingLists) throws IOException {
        return serialize((Object) listOfCallingLists);
    }

    @Override
    public ListOfCallingLists deserializeListOfCallingLists(InputStream stream)
            throws IOException {
        return (ListOfCallingLists) deserialize(stream);
    }

    @Override
    public byte[] serialize(CityList cityList) throws IOException {
        return serialize((Object) cityList);
    }

    @Override
    public CityList deserializeCityList(InputStream stream) throws IOException {
        return (CityList) deserialize(stream);
    }

    private byte[] serialize(Object o) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(byteArrayOutputStream);
        os.writeObject(o);
        os.close();
        byteArrayOutputStream.flush();
        byte[] result = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();
        return result;
    }

    private Object deserialize(InputStream stream) throws IOException {
        ObjectInputStream is = new ObjectInputStream(stream);
        Object instance = null;
        try {
            instance = is.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        is.close();
        return instance;
    }
}
