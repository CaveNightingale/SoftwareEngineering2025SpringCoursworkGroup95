package io.github.software.coursework.data;

import java.io.IOException;

/**
 *
 */
public interface Item<T extends Item<T>> {
    /**
     * Serialize an item to a writer.
     * @param writer The writer to write to.
     */
    void serialize(Document.Writer writer) throws IOException;
}
