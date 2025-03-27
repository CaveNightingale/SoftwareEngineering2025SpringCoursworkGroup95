package io.github.software.coursework.data;

import java.io.IOException;

/**
 * A serializable item.
 */
public interface Item<T extends Item<T>> {
    /**
     * Serialize an item to a writer.
     * Typically, a class implementing this interface should be immutable and final.
     * @param writer The writer to write to.
     */
    void serialize(Document.Writer writer) throws IOException;
}
