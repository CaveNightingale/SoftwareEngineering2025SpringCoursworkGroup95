package io.github.software.coursework.data;

import java.io.IOException;
import java.util.SequencedCollection;

/**
 * A serializable item.
 */
public interface Item {
    /**
     * Serialize an item to a writer.
     * Typically, a class implementing this interface should be immutable and final.
     * @param writer The writer to write to.
     * @throws IOException If an I/O error occurs.
     */
    void serialize(Document.Writer writer) throws IOException;

    /**
     * Serialize a list of items to a writer.
     * @param items The items to serialize.
     * @return A function that writes the items to a writer.
     */
    static Item asList(SequencedCollection<? extends Item> items) {
        return writer -> {
            int i = 0;
            for (Item item : items) {
                item.serialize(writer.writeCompound(i++));
            }
            writer.writeEnd();
        };
    }
}
