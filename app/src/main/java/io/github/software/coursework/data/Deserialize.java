package io.github.software.coursework.data;

import java.io.IOException;
import java.util.SequencedCollection;
import java.util.function.Supplier;

@FunctionalInterface
public interface Deserialize<T> {
    T deserialize(Document.Reader reader) throws IOException;

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
