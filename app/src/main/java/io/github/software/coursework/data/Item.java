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
     */
    void serialize(Document.Writer writer) throws IOException;

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
