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
import java.util.HashMap;
import java.util.Map;

public class EncryptedDirectory implements Directory {
    private final Object none = new Object();
    protected final File directory;
    protected final byte[] key;
    protected final boolean obfuscateNamespace;
    protected final String namespace;
    protected final HashMap<String, Item<?>> buffer = new HashMap<>();
    protected final Cache<String, Object> cache = CacheBuilder.newBuilder()
            .maximumSize(1024)
            .expireAfterAccess(Duration.ofMinutes(5))
            .build();
    protected final JsonFactory jsonFactory = new JsonFactory();

    public EncryptedDirectory(File directory, byte[] key, String namespace, boolean obfuscateNamespace) {
        this.directory = directory;
        this.key = key.clone();
        this.namespace = namespace;
        this.obfuscateNamespace = obfuscateNamespace;
    }

    public EncryptedDirectory(File directory, byte[] key) {
        this(directory, key, "", true);
    }

    protected String obfuscateFileName(String name) {
        if (obfuscateNamespace) {
            name = namespace + "-" + name;
        }
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        digest.update(name.getBytes(StandardCharsets.UTF_8));
        digest.update(key);
        byte[] hash = digest.digest();
        StringBuilder builder = new StringBuilder();
        for (byte b : hash) {
            builder.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        builder.append(".txt");
        if (obfuscateNamespace) {
            return builder.toString();
        } else {
            return namespace + "/" + builder;
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
                    entry.getValue().serialize(writer);
                }
                cache.put(entry.getKey(), entry.getValue());
            }
        }
        buffer.clear();
    }

    @Override
    public Directory withNamespace(String namespace) {
        return new EncryptedDirectory(directory, key, this.namespace + "/" + namespace, obfuscateNamespace);
    }
}
