package io.github.software.coursework.data;

import java.io.IOException;

@FunctionalInterface
public interface DeserializationConstructor<T> {
    T deserialize(Document.Reader reader) throws IOException;
}
