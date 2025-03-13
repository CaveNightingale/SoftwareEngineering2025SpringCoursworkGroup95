package io.github.software.coursework;

import io.github.software.coursework.data.Document;
import io.github.software.coursework.data.text.TextDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TextStorageTest {
    @Test
    void testDocument() {
        TextDocument document = new TextDocument("", true);
        Document.Writer writer1 = document.writer();
        writer1.writeInteger("key", 42);
        writer1.writeFloat("key1", 42.114514);
        writer1.writeString("key2", "value\n");
        Document.Writer writer2 = writer1.writeCompound("key3");
        writer2.writeInteger("key4", 123);
        Document.Writer writer3 = writer2.writeCompound("key5");
        writer3.writeInteger("key6", 33);
        writer3.writeEnd();
        writer2.writeEnd();
        writer1.writeInteger("key8", 66);
        writer1.writeString("key9", "");
        Document.Writer writer4 = writer1.writeCompound("key10");
        writer4.writeEnd();
        writer1.writeEnd();
        System.out.println(document.toString());
        Document.Reader reader1 = document.reader();
        Assertions.assertEquals(42, reader1.readInteger("key"));
        Assertions.assertEquals(42.114514, reader1.readFloat("key1"));
        Assertions.assertEquals("value\n", reader1.readString("key2"));
        Document.Reader reader2 = reader1.readCompound("key3");
        Assertions.assertEquals(123, reader2.readInteger("key4"));
        Document.Reader reader3 = reader2.readCompound("key5");
        Assertions.assertEquals(33, reader3.readInteger("key6"));
        Assertions.assertTrue(reader3.isEnd());
        reader3.readEnd();
        Assertions.assertTrue(reader2.isEnd());
        reader2.readEnd();
        Assertions.assertEquals(66, reader1.readInteger("key8"));
        Assertions.assertEquals("", reader1.readString("key9"));
        Document.Reader reader4 = reader1.readCompound("key10");
        Assertions.assertTrue(reader4.isEnd());
        reader4.readEnd();
        reader1.readEnd();
    }
}
