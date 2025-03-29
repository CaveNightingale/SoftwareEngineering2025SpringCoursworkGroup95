package io.github.software.coursework;

import com.fasterxml.jackson.core.JsonFactory;
import io.github.software.coursework.data.Document;
import io.github.software.coursework.data.json.EncryptedLogger;
import io.github.software.coursework.data.json.JsonReader;
import io.github.software.coursework.util.IntegerItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.SequencedCollection;

import static org.junit.jupiter.api.Assertions.*;

public class EncryptedLoggerTest {

    @Test
    public void testEncryptedLogger(@TempDir File tempDir) throws Exception {
        byte[] key = new byte[16];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(key);
        try(EncryptedLogger encryptedLogger = new EncryptedLogger(tempDir, key)) {
            encryptedLogger.log("EVENT_1");
            encryptedLogger.log("EVENT_2", new IntegerItem(5), new IntegerItem(10));
        }
        SequencedCollection<String> events = EncryptedLogger.decodeLog(Objects.requireNonNull(tempDir.listFiles())[0], key);
        assertEquals(2, events.size());
        try (Document.Reader reader = JsonReader.createReader(JsonFactory.builder().build().createParser(events.removeFirst()))) {
            assertTrue(reader.readString("time").matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
            assertEquals("EVENT_1", reader.readString("event"));
            Document.Reader items = reader.readCompound("items");
            items.readEnd();
            reader.readEnd();
        }
        try (Document.Reader reader = JsonReader.createReader(JsonFactory.builder().build().createParser(events.removeFirst()))) {
            assertTrue(reader.readString("time").matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
            assertEquals("EVENT_2", reader.readString("event"));
            Document.Reader items = reader.readCompound("items");
            assertEquals(5, IntegerItem.deserialize(items.readCompound(0)).value());
            assertEquals(10, IntegerItem.deserialize(items.readCompound(1)).value());
            items.readEnd();
            reader.readEnd();
        }
    }
}
