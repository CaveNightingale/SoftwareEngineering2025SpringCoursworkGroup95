package io.github.software.coursework;

import io.github.software.coursework.data.Document;
import io.github.software.coursework.data.Reference;
import io.github.software.coursework.data.SyntaxException;
import io.github.software.coursework.data.json.JsonDocument;
import io.github.software.coursework.data.schema.Entity;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

public class JsonTest {
    @Test
    public void testIntegers() throws IOException {
        JsonDocument document = new JsonDocument();
        try (Document.Writer writer = document.writer()) {
            writer.writeInteger("a", 1);
            writer.writeInteger("b", -1);
            writer.writeInteger("0", 1000000000000000000L);
            writer.writeEnd();
        }
        try (Document.Reader reader = document.reader()) {
            assertEquals(1, reader.readInteger("a"));
            assertEquals(-1, reader.readInteger("b"));
            assertEquals(1000000000000000000L, reader.readInteger("0"));
            reader.readEnd();
        }
        assertEquals("{\"a\":1,\"b\":-1,\"0\":1000000000000000000}", document.toString());

        // Read in incorrect order is an error
        assertThrows(SyntaxException.class, () -> {
            try (Document.Reader reader = document.reader()) {
                reader.readInteger("b");
                reader.readInteger("a");
                reader.readInteger("0");
            }
        });

        // Read missing key is an error
        assertThrows(SyntaxException.class, () -> {
            try (Document.Reader reader = document.reader()) {
                reader.readInteger("c");
            }
        });

        // Read end but not at the end is an error
        assertThrows(SyntaxException.class, () -> {
            try (Document.Reader reader = document.reader()) {
                reader.readEnd();
            }
        });

        // Read a field twice is an error
        assertThrows(SyntaxException.class, () -> {
            try (Document.Reader reader = document.reader()) {
                reader.readInteger("a");
                reader.readInteger("a");
            }
        });
    }

    @Test
    public void testDoubles() throws IOException {
        JsonDocument document = new JsonDocument();
        try (Document.Writer writer = document.writer()) {
            writer.writeFloat("a", 1.0); // Normal number
            writer.writeInteger("b", -1); // Write as integer, read as double
            writer.writeFloat("0", 1.0e100); // Large number
            writer.writeEnd();
        }
        try (Document.Reader reader = document.reader()) {
            assertEquals(1.0, reader.readFloat("a"));
            assertEquals(-1.0, reader.readFloat("b"));
            assertEquals(1.0e100, reader.readFloat("0"));
            reader.readEnd();
        }
    }

    @Test
    public void testReferences() throws IOException {
        JsonDocument document = new JsonDocument();
        Reference<Entity> ref1 = new Reference<>();
        Reference<Entity> ref2 = new Reference<>();
        try (Document.Writer writer = document.writer()) {
            writer.writeReference("a", ref1);
            writer.writeReference("b", ref2);
            writer.writeReference("c", null);
            writer.writeEnd();
        }
        try (Document.Reader reader = document.reader()) {
            assertEquals(ref1, reader.readReference("a"));
            assertEquals(ref2, reader.readReference("b"));
            assertFalse(reader.isEnd());
            assertNull(reader.readReference("c"));
            assertTrue(reader.isEnd());
            reader.readEnd();
        }
    }

    @Test
    public void testStrings() throws IOException {
        JsonDocument document = new JsonDocument();
        try (Document.Writer writer = document.writer()) {
            writer.writeString("a", "Hello, world!");
            writer.writeString("b", "");
            writer.writeString("c", "\n");
            writer.writeEnd();
        }
        assertEquals("{\"a\":\"Hello, world!\",\"b\":\"\",\"c\":\"\\n\"}", document.toString());
        try (Document.Reader reader = document.reader()) {
            assertEquals("Hello, world!", reader.readString("a"));
            assertEquals("", reader.readString("b"));
            assertFalse(reader.isEnd());
            assertEquals("\n", reader.readString("c"));
            assertTrue(reader.isEnd());
            reader.readEnd();
        }
    }

    @Test
    public void testCompound() throws IOException {
        // Empty compound should be serialized as empty array instead of empty object
        JsonDocument document = new JsonDocument();
        try (Document.Writer writer = document.writer()) {
            writer.writeEnd();
        }
        assertEquals("[]", document.toString());
        try (Document.Reader reader = document.reader()) {
            assertTrue(reader.isEnd());
            reader.readEnd();
        }

        // Nested compounds
        try (Document.Writer writer = document.writer()) {
            try (Document.Writer nestedArray = writer.writeCompound("array")) {
                for (int i = 0; i < 5; i++) {
                    nestedArray.writeInteger(i, i + 1);
                }
                nestedArray.writeEnd();
            }
            try (Document.Writer nestedObject = writer.writeCompound("object")) {
                nestedObject.writeInteger("a", 1);
                nestedObject.writeInteger("b", 2);
                nestedObject.writeEnd();
            }
            writer.writeEnd();
        }
        try (Document.Reader reader = document.reader()) {
            try (Document.Reader nestedArray = reader.readCompound("array")) {
                for (int i = 0; i < 5; i++) {
                    assertEquals(i + 1, nestedArray.readInteger(i));
                }
                assertTrue(nestedArray.isEnd());
                nestedArray.readEnd();
            }
            try (Document.Reader nestedObject = reader.readCompound("object")) {
                assertEquals(1, nestedObject.readInteger("a"));
                assertEquals(2, nestedObject.readInteger("b"));
                assertTrue(nestedObject.isEnd());
                nestedObject.readEnd();
            }
            assertTrue(reader.isEnd());
            reader.readEnd();
        }
    }
}
