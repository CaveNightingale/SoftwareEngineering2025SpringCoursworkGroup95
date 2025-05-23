package io.github.software.coursework.data;

import java.io.IOException;
import java.util.SequencedCollection;
import java.util.function.Supplier;

/**
 * A functional interface for deserializing an object from a {@link Document.Reader}.
 * @param <T> the type of the object to deserialize
 */
@FunctionalInterface
public interface Deserialize<T> {
    /**
     * Deserialize an object from a {@link Document.Reader}.
     * @param reader the reader to read from
     * @return the deserialized object
     * @throws IOException if an I/O error occurs
     */
    T deserialize(Document.Reader reader) throws IOException;

    /**
     * Deserialize an object from a {@link Document.Reader} and return it as a list.
     * @param supplier a supplier of the list to create
     * @return a deserializer that returns the list
     * @param <S> the type of the list
     */
    default <S extends SequencedCollection<T>> Deserialize<S> asList(Supplier<S> supplier) {
        return reader -> {
            S list = supplier.get();
            for (int i = 0; !reader.isEnd(); i++) {
                list.add(deserialize(reader.readCompound(i)));
            }
            reader.readEnd();
            return list;
        };
    }
}
