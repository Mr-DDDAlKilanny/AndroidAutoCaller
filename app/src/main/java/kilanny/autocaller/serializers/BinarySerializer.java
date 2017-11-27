package kilanny.autocaller.serializers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import kilanny.autocaller.data.ListOfCallingLists;

/**
 * Created by user on 10/15/2017.
 */

public class BinarySerializer implements Serializer {
    @Override
    public byte[] serialize(ListOfCallingLists listOfCallingLists) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(byteArrayOutputStream);
        os.writeObject(listOfCallingLists);
        os.close();
        byteArrayOutputStream.flush();
        byte[] result = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();
        return result;
    }

    @Override
    public ListOfCallingLists deserialize(InputStream stream)
            throws IOException {
        ObjectInputStream is = new ObjectInputStream(stream);
        ListOfCallingLists instance = null;
        try {
            instance = (ListOfCallingLists) is.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        is.close();
        return instance;
    }
}
