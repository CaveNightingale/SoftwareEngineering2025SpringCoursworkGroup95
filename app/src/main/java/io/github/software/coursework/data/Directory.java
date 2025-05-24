package io.github.software.coursework.data;

import java.io.IOException;

/**
 * A directory is a collection of items that can be accessed by name.
 * Different from a {@link DirectoryAccessor}, this interface is used to
 * represent a directory that can be flushed to a file or other storage,
 * while the {@link DirectoryAccessor} may delegate to other objects.
 */
public interface Directory extends AutoCloseable, DirectoryAccessor {

    /**
     * Flush the directory to the underlying storage.
     * Notice this will not flush subdirectories associated with this directory.
     * @throws IOException if an I/O error occurs
     */
    void flush() throws IOException;

    /**
     * Open a subdirectory with the given name.
     * @param namespace the name of the subdirectory
     * @return the subdirectory
     */
    Directory withNamespace(String namespace);

    default void close() throws IOException {
        flush();
    }
}
