package io.github.software.coursework;

import io.github.software.coursework.data.Directory;
import io.github.software.coursework.data.Document;
import io.github.software.coursework.data.Item;
import io.github.software.coursework.data.Reference;
import io.github.software.coursework.data.json.ChunkedIndex;
import io.github.software.coursework.data.json.EncryptedDirectory;
import io.github.software.coursework.data.schema.Entity;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class JsonDirectoryTest {
    @Test
    public void testDirectory(@TempDir File tempDir) throws IOException {
        SecureRandom random = new SecureRandom();
        byte[] key = new byte[256 / 8];
        random.nextBytes(key);
        try (Directory directory = new EncryptedDirectory(tempDir, key)) {
            // Buffered put
            directory.put("hello", new Entity("111", "222", "333", "444", "555", Entity.Type.INDIVIDUAL));
            Entity entity = directory.get("hello", Entity::deserialize);
            assertNotNull(entity);
            assertEquals("111", entity.name());
            assertEquals("222", entity.telephone());
            assertEquals("333", entity.email());
            assertEquals("444", entity.address());
            assertEquals("555", entity.website());
            assertEquals(Entity.Type.INDIVIDUAL, entity.type());

            // Attempt to delete non-existent key
            directory.put("world", null);
            assertNull(directory.get("world", Entity::deserialize));

            // Flush to disk
            directory.flush();
            entity = directory.get("hello", Entity::deserialize);
            assertNotNull(entity);
            assertEquals("111", entity.name());
            assertEquals("222", entity.telephone());
            assertEquals("333", entity.email());
            assertEquals("444", entity.address());
            assertEquals("555", entity.website());
            assertEquals(Entity.Type.INDIVIDUAL, entity.type());

            // Update in memory
            directory.put("hello", new Entity("666", "777", "888", "999", "aaa", Entity.Type.COMMERCIAL));
            directory.put("world", new Entity("bbb", "ccc", "ddd", "eee", "fff", Entity.Type.NONPROFIT));
            entity = directory.get("hello", Entity::deserialize);
            assertNotNull(entity);
            assertEquals("666", entity.name());
            assertEquals("777", entity.telephone());
            assertEquals("888", entity.email());
            assertEquals("999", entity.address());
            assertEquals("aaa", entity.website());
            assertEquals(Entity.Type.COMMERCIAL, entity.type());

            // Test a different namespace with the same key
            try (Directory directory2 = directory.withNamespace("sub")) {
                entity = directory2.get("hello", Entity::deserialize);
                assertNull(entity);
                directory2.put(new Reference<>(0), new Entity("ggg", "hhh", "iii", "jjj", "kkk", Entity.Type.EDUCATION));
                assertNotNull(directory2.get(new Reference<>(0), Entity::deserialize));
            }
        }

        // Another instance on same directory and key
        try (Directory directory = new EncryptedDirectory(tempDir, key)) {
            Entity entity = directory.get("hello", Entity::deserialize);
            assertNotNull(entity);
            assertEquals("666", entity.name());
            assertEquals("777", entity.telephone());
            assertEquals("888", entity.email());
            assertEquals("999", entity.address());
            assertEquals("aaa", entity.website());
            assertEquals(Entity.Type.COMMERCIAL, entity.type());
            entity = directory.get("world", Entity::deserialize);
            assertNotNull(entity);
            assertEquals("bbb", entity.name());
            assertEquals("ccc", entity.telephone());
            assertEquals("ddd", entity.email());
            assertEquals("eee", entity.address());
            assertEquals("fff", entity.website());
            assertEquals(Entity.Type.NONPROFIT, entity.type());
        }

        // Assert file extensions
        for (File file : Objects.requireNonNull(tempDir.listFiles())) {
            assertTrue(file.getName().endsWith(".txt"));
        }
    }

    record IntegerItem(long value) implements Item<IntegerItem>, Comparable<IntegerItem> {
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

    @Test
    public void testChunkIndex(@TempDir File tempDir) throws IOException {
        SecureRandom random = new SecureRandom();
        byte[] key = new byte[256 / 8];
        random.nextBytes(key);
        Random random1 = new Random();
        ArrayList<IntegerItem> items = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            items.add(new IntegerItem(random1.nextLong()));
        }
        ArrayList<IntegerItem> sorted = new ArrayList<>(items);
        sorted.sort(IntegerItem::compareTo);
        try (ChunkedIndex<IntegerItem> chunkedIndex = new ChunkedIndex<>(
                new EncryptedDirectory(tempDir, key), IntegerItem::compareTo, IntegerItem::deserialize)) {
            chunkedIndex.setMergeThreshold(2);
            chunkedIndex.setSplitThreshold(6);
            for (IntegerItem item : items) {
                chunkedIndex.addSample(item);
            }
            int queryLeft = random1.nextInt(sorted.size());
            int queryRight = random1.nextInt(sorted.size());
            if (queryLeft > queryRight) {
                int temp = queryLeft;
                queryLeft = queryRight;
                queryRight = temp;
            }

            // Range query
            ArrayList<IntegerItem> queryResult = chunkedIndex.querySamples(sorted.get(queryLeft), sorted.get(queryRight), 0, Integer.MAX_VALUE);
            List<IntegerItem> subList = sorted.subList(queryLeft, queryRight);
            assertEquals(queryResult.size(), subList.size());
            for (int i = 0; i < subList.size(); i++) {
                assertEquals(subList.get(i), queryResult.get(i));
            }

            // Single query
            queryResult = chunkedIndex.querySamples(sorted.get(queryLeft), null, 0, 1);
            assertEquals(1, queryResult.size());
            assertEquals(sorted.get(queryLeft), queryResult.getFirst());

            // Skip query
            queryResult = chunkedIndex.querySamples(sorted.get(queryLeft), sorted.get(queryRight), 1, Integer.MAX_VALUE);
            assertEquals(queryResult.size(), subList.size() - 1);
            for (int i = 1; i < subList.size(); i++) {
                assertEquals(subList.get(i), queryResult.get(i - 1));
            }

            // Deleting
            int deleteCount = random1.nextInt(items.size());
            for (int i = 0; i < deleteCount; i++) {
                chunkedIndex.removeSample(items.get(items.size() - i - 1));
            }
            sorted = new ArrayList<>(items.subList(0, items.size() - deleteCount));
            sorted.sort(IntegerItem::compareTo);
            queryLeft = random1.nextInt(sorted.size());
            queryRight = random1.nextInt(sorted.size());
            if (queryLeft > queryRight) {
                int temp = queryLeft;
                queryLeft = queryRight;
                queryRight = temp;
            }

            // Range query
            queryResult = chunkedIndex.querySamples(sorted.get(queryLeft), sorted.get(queryRight), 0, Integer.MAX_VALUE);
            subList = sorted.subList(queryLeft, queryRight);
            assertEquals(queryResult.size(), subList.size());
            for (int i = 0; i < subList.size(); i++) {
                assertEquals(subList.get(i), queryResult.get(i));
            }

            // Single query
            queryResult = chunkedIndex.querySamples(sorted.get(queryLeft), null, 0, 1);
            assertEquals(1, queryResult.size());
            assertEquals(sorted.get(queryLeft), queryResult.getFirst());

            // Skip query
            queryResult = chunkedIndex.querySamples(sorted.get(queryLeft), sorted.get(queryRight), 1, Integer.MAX_VALUE);
            assertEquals(queryResult.size(), subList.size() - 1);
            for (int i = 1; i < subList.size(); i++) {
                assertEquals(subList.get(i), queryResult.get(i - 1));
            }
        }
        try (ChunkedIndex<IntegerItem> chunkedIndex = new ChunkedIndex<>(
                new EncryptedDirectory(tempDir, key), IntegerItem::compareTo, IntegerItem::deserialize)) {
            chunkedIndex.setMergeThreshold(2);
            chunkedIndex.setSplitThreshold(6);
            ArrayList<IntegerItem> allItems = chunkedIndex.querySamples(null, null, 0, Integer.MAX_VALUE);
            for (int i = 0; i < allItems.size(); i++) {
                assertEquals(sorted.get(i), allItems.get(i));
            }
        }
    }

    // Expected to run in 3 minutes on Intel(R) Core(TM) i7-14700HX
    // However, loose time constraint is set to 5 minutes for older CPUs
    @Test
    @Timeout(300)
    @Disabled
    public void testChunkSpeed(@TempDir File tempDir) throws IOException {
        SecureRandom random = new SecureRandom();
        byte[] key = new byte[256 / 8];
        random.nextBytes(key);
        Random random1 = new Random();
        try (ChunkedIndex<IntegerItem> chunkedIndex = new ChunkedIndex<>(
                new EncryptedDirectory(tempDir, key), IntegerItem::compareTo, IntegerItem::deserialize)) {
            chunkedIndex.setSplitThreshold(500);
            for (int i = 0; i < 1000; i++) {
                long start = System.nanoTime();
                for (int j = 0; j < 1000; j++) {
                    chunkedIndex.addSample(new IntegerItem(random1.nextLong()));
                }
                long end = System.nanoTime();
                chunkedIndex.flush();
                long flush = System.nanoTime();
                System.out.println("Iter " + i + ", inserting time " + (end - start) / 1e9 + "s, flushing time " + (flush - end) / 1e9 + "s");
            }
        }
    }
}
