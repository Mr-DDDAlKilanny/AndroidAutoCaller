package kilanny.autocaller.serializers;

import java.io.IOException;
import java.io.InputStream;

import kilanny.autocaller.data.ListOfCallingLists;

/**
 * Created by user on 10/15/2017.
 */

public interface ISerializer {
    byte[] serialize(ListOfCallingLists listOfCallingLists) throws IOException;
    ListOfCallingLists deserialize(InputStream stream) throws IOException;
}
