package io.github.software.coursework.data;

import java.io.IOException;

public interface Directory extends AutoCloseable, DirectoryAccessor {
    void flush() throws IOException;
    Directory withNamespace(String namespace);

    default void close() throws IOException {
        flush();
    }
}
