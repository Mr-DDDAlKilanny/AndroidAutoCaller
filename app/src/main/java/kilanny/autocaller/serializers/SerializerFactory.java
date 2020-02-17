package kilanny.autocaller.serializers;


import androidx.annotation.NonNull;

public final class SerializerFactory {

    @NonNull
    public static Serializer getSerializer() {
        return new BinarySerializer();
    }

    private SerializerFactory() {
    }
}
