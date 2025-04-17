package io.github.software.coursework.data.json;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import io.github.software.coursework.data.AsyncStorage;
import io.github.software.coursework.data.Document;
import io.github.software.coursework.data.Item;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.SequencedCollection;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class EncryptedLogger implements Closeable {
    private record Entry(Date time, AsyncStorage.Sensitivity sensitivity, String event, Item[] items) {}
    // Size padded to prevent inference of the length of the data
    private final int paddingChunkSize = 512;

    private final byte[] key;
    private final BufferedWriter writer;
    private final ConcurrentLinkedQueue<Entry> queue = new ConcurrentLinkedQueue<>();
    private final JsonFactory factory = JsonFactory.builder().build();

    public EncryptedLogger(File directory, byte[] key) throws IOException {
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        int i = 1;
        while (new File(directory, date + "_" + i + ".log").exists()) {
            i++;
        }
        this.key = key;
        writer = new BufferedWriter(new FileWriter(new File(directory, date + "_" + i + ".log"), StandardCharsets.UTF_8));
        writer.write(Encryption.LOG_START);
    }

    public void flush() throws IOException {
        Cipher cipher;
        SecureRandom random = new SecureRandom();
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        try {
            cipher = Cipher.getInstance("AES/CBC/NoPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
        for (Entry entry = queue.poll(); entry != null; entry = queue.poll()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (Document.Writer jsonWriter = JsonWriter.createWriter(factory.createGenerator(baos, JsonEncoding.UTF8))) {
                jsonWriter.writeString("time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(entry.time));
                jsonWriter.writeString("event", entry.event);
                jsonWriter.writeString("sensitivity", entry.sensitivity.toString());
                Document.Writer itemsWriter = jsonWriter.writeCompound("items");
                int i = 0;
                for (Item item : entry.items) {
                    item.serialize(itemsWriter.writeCompound(i++));
                }
                itemsWriter.writeEnd();
                jsonWriter.writeEnd();
            }
            byte[] data = baos.toByteArray();
            int contentLength = data.length + 4; // 4 bytes for the length prefix
            // So the encrypted data is padded to a multiple of paddingChunkSize. Iv is considered as part of the data.
            int paddedLength = ((contentLength + paddingChunkSize - 1 + cipher.getBlockSize()) / paddingChunkSize) * paddingChunkSize;
            byte[] paddedData = new byte[paddedLength];
            paddedData[0] = (byte) (0xff & (data.length >> 24));
            paddedData[1] = (byte) (0xff & (data.length >> 16));
            paddedData[2] = (byte) (0xff & (data.length >> 8));
            paddedData[3] = (byte) (0xff & data.length);
            System.arraycopy(data, 0, paddedData, 4, data.length);
            byte[] iv = new byte[cipher.getBlockSize()];
            byte[] encrypted;
            random.nextBytes(iv);
            try {
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));
                encrypted = cipher.doFinal(paddedData);
            } catch (InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException |
                     BadPaddingException e) {
                throw new RuntimeException(e);
            }
            byte[] encryptedWithIv = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, iv.length);
            System.arraycopy(encrypted, 0, encryptedWithIv, iv.length, encrypted.length);
            String base64 = Base64.getEncoder().encodeToString(encryptedWithIv);
            writer.write(base64 + "\n");
        }
    }

    public void log(String event, AsyncStorage.Sensitivity sensitivity, Item... items) {
        queue.add(new Entry(new Date(), sensitivity, event, items));
    }

    @Override
    public void close() throws IOException {
        try {
            flush();
            writer.write(Encryption.LOG_END);
        } finally {
            writer.close();
        }
    }

    public static SequencedCollection<String> decodeLog(File file, byte[] key) throws IOException {
        ArrayList<String> lines = new ArrayList<>();
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        Cipher cipher;
        try {
            cipher = Cipher.getInstance("AES/CBC/NoPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
        boolean firstLineRead = false;
        boolean finalLineRead = false;
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                if (line.startsWith(Encryption.LOG_START.trim())) {
                    if (!firstLineRead) {
                        firstLineRead = true;
                        continue;
                    } else {
                        throw new IOException("Multiple log start lines");
                    }
                }
                if (!firstLineRead) {
                    continue;
                }
                if (line.startsWith(Encryption.LOG_END.trim())) {
                    if (!finalLineRead) {
                        finalLineRead = true;
                        continue;
                    } else {
                        throw new IOException("Multiple log end lines");
                    }
                }
                if (finalLineRead) {
                    continue;
                }
                byte[] encrypted;
                try {
                    encrypted = Base64.getDecoder().decode(line);
                } catch (IllegalArgumentException e) {
                    throw new IOException("Invalid base64", e);
                }
                byte[] iv = new byte[16];
                System.arraycopy(encrypted, 0, iv, 0, iv.length);
                byte[] data = new byte[encrypted.length - iv.length];
                System.arraycopy(encrypted, iv.length, data, 0, data.length);
                byte[] decoded;
                try {
                    cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
                    decoded = cipher.doFinal(data);
                } catch (InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
                    throw new IOException("Invalid key", e);
                }
                if (decoded.length < 4) {
                    throw new IOException("Invalid data");
                }
                int size = ((decoded[0] & 0xff) << 24) | ((decoded[1] & 0xff) << 16) | ((decoded[2] & 0xff) << 8) | (decoded[3] & 0xff);
                lines.add(new String(decoded, 4, size, StandardCharsets.UTF_8));
            }
        }
        if (!firstLineRead) {
            throw new IOException("Cannot find log start line. This file does not look like a log file.");
        }
        return lines;
    }
}
