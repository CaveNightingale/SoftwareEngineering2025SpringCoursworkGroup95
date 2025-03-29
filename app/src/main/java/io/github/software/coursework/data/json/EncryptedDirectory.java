package io.github.software.coursework.data.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.software.coursework.data.DeserializationConstructor;
import io.github.software.coursework.data.Directory;
import io.github.software.coursework.data.Document;
import io.github.software.coursework.data.Item;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class EncryptedDirectory implements Directory {
    private static final Logger logger = Logger.getLogger("EncryptedDirectory");
    private final Object none = new Object();
    private final File directory;
    private final byte[] key;
    private final String namespace;
    private final HashMap<String, Item<?>> buffer = new HashMap<>();
    private final Cache<String, Object> cache = CacheBuilder.newBuilder()
            .maximumSize(1024)
            .expireAfterAccess(Duration.ofMinutes(5))
            .build();
    private final JsonFactory jsonFactory = new JsonFactory();

    public EncryptedDirectory(File directory, byte[] key, String namespace) {
        this.directory = directory;
        this.key = key.clone();
        this.namespace = namespace;
    }

    public EncryptedDirectory(File directory, byte[] key) {
        this(directory, key, "");
    }

    public static String obfuscateFileName(String actualName, byte[] key) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        digest.update(actualName.getBytes(StandardCharsets.UTF_8));
        digest.update(key);
        byte[] hash = digest.digest();
        StringBuilder builder = new StringBuilder();
        for (byte b : hash) {
            builder.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        builder.append(".txt");
        return builder.toString();
    }

    public static String obfuscateFileName(String name, String namespace, byte[] key) {
        name = namespace + "/" + name;
        return obfuscateFileName(name, key);
    }

    private String obfuscateFileName(String name) {
        return obfuscateFileName(name, namespace, key);
    }

    public static void changeKey(byte[] oldKey, byte[] newKey, File path) {
        if (Arrays.equals(oldKey, newKey)) {
            return;
        }
        File[] list = path.listFiles(f -> f.getName().endsWith(".txt"));
        if (list == null) {
            return;
        }
        for (File file : list) {
            String actualName;
            try (JsonReader reader = JsonReader.createReader(new JsonFactory().createParser(
                    new DecryptingInputStream(new BufferedInputStream(new FileInputStream(file)), oldKey)))) {
                reader.suppressWarning();
                actualName = reader.readString("_filename");
                if (!obfuscateFileName(actualName, oldKey).equals(file.getName())) {
                    throw new IOException("Malformed file name: " + file.getName());
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Ignore non-recognized file: " + file, e);
                continue;
            }
            try (DecryptingInputStream decryptingInputStream = new DecryptingInputStream(new FileInputStream(file), oldKey);
                 EncryptingOutputStream encryptingOutputStream = new EncryptingOutputStream(new FileOutputStream(new File(path, obfuscateFileName(actualName, newKey))), newKey)) {
                decryptingInputStream.transferTo(encryptingOutputStream);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to change key for file: " + file, e);
                continue;
            }
            if (!file.delete()) {
                logger.log(Level.SEVERE, "Failed to delete old file: " + file);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Item<T>> @Nullable T get(String name, DeserializationConstructor<T> constructor) throws IOException {
        if (buffer.containsKey(name)) {
            return (T) buffer.get(name);
        }
        Object cached = cache.getIfPresent(name);
        if (cached == none) {
            return null;
        }
        if (cached != null) {
            return (T) cached;
        }
        File file = new File(directory, obfuscateFileName(name));
        if (!file.exists()) {
            cache.put(name, none);
            return null;
        }
        T output;
        try (Document.Reader reader = JsonReader.createReader(jsonFactory.createParser(
                        new DecryptingInputStream(new BufferedInputStream(new FileInputStream(file)), key)))) {
            String actualName = reader.readString("_filename");
            if (!actualName.equals(namespace + "/" + name)) {
                throw new IOException("Found " + actualName + " in location where " + namespace + "/" + name + " was expected");
            }
            output = constructor.deserialize(reader);
        }
        cache.put(name, output);
        return output;
    }

    @Override
    public <T extends Item<T>> void put(String name, @Nullable T item) {
        buffer.put(name, item);
    }

    @Override
    public void flush() throws IOException {
        for (Map.Entry<String, Item<?>> entry : buffer.entrySet()) {
            File file = new File(directory, obfuscateFileName(entry.getKey()));
            if (entry.getValue() == null) {
                if (file.exists() && !file.delete()) {
                    throw new IOException("Failed to delete file: " + file);
                }
                cache.put(entry.getKey(), none);
            } else {
                try (Document.Writer writer = JsonWriter.createWriter(jsonFactory.createGenerator(
                        new EncryptingOutputStream(new FileOutputStream(file), key)))) {
                    writer.writeString("_filename", namespace + "/" + entry.getKey());
                    entry.getValue().serialize(writer);
                }
                cache.put(entry.getKey(), entry.getValue());
            }
        }
        buffer.clear();
    }

    @Override
    public Directory withNamespace(String namespace) {
        return new EncryptedDirectory(directory, key, this.namespace + "-" + namespace);
    }
}
