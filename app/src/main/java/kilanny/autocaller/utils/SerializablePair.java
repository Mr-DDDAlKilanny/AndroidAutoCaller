package kilanny.autocaller.utils;

import java.io.Serializable;

/**
 * Created by Yasser on 11/18/2016.
 */

public class SerializablePair<T, F> implements Serializable {

    private T first;
    private F second;

    public SerializablePair() {
    }

    public SerializablePair(T first, F second) {
        this();
        setFirst(first);
        setSecond(second);
    }

    public T getFirst() {
        return first;
    }

    public void setFirst(T first) {
        this.first = first;
    }

    public F getSecond() {
        return second;
    }

    public void setSecond(F second) {
        this.second = second;
    }
}
