package io.github.software.coursework.data;

import java.io.IOException;

public record ReferenceItemPair<T extends Item>(Reference<T> reference, T item) implements Item {
    public ReferenceItemPair<T> withReference(Reference<T> reference) {
        return new ReferenceItemPair<>(reference, item);
    }

    public ReferenceItemPair<T> withItem(T item) {
        return new ReferenceItemPair<>(reference, item);
    }

    @Override
    public void serialize(Document.Writer writer) throws IOException {
        writer.writeReference("reference", reference);
        item.serialize(writer.writeCompound("item"));
        writer.writeEnd();
    }

    @SuppressWarnings("unchecked")
    public static <T extends Item> ReferenceItemPair<T> deserialize(Document.Reader reader, Deserialize<T> constructor) throws IOException {
        Reference<T> reference = (Reference<T>) reader.readReference("reference");
        T item = constructor.deserialize(reader.readCompound("item"));
        reader.readEnd();
        return new ReferenceItemPair<>(reference, item);
    }
}
