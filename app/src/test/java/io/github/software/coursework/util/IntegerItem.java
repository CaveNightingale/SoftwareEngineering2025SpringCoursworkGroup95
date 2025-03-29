package io.github.software.coursework.util;

import io.github.software.coursework.JsonDirectoryTest;
import io.github.software.coursework.data.Document;
import io.github.software.coursework.data.Item;

import java.io.IOException;

public record IntegerItem(long value) implements Item<IntegerItem>, Comparable<IntegerItem> {
    @Override
    public void serialize(Document.Writer writer) throws IOException {
        writer.writeInteger("value", value);
        writer.writeEnd();
    }

    public static IntegerItem deserialize(Document.Reader reader) throws IOException {
        long value = reader.readInteger("value");
        reader.readEnd();
        return new IntegerItem(value);
    }

    @Override
    public int compareTo(IntegerItem o) {
        return Long.compare(value, o.value);
    }
}
