package io.github.software.coursework;

import io.github.software.coursework.data.json.DecryptingInputStream;
import io.github.software.coursework.data.json.EncryptingOutputStream;
import io.github.software.coursework.data.json.Encryption;
import io.github.software.coursework.util.RandomChunkingInputStream;
import io.github.software.coursework.util.RandomString;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;

public class EncryptionTest {
    @Test
    public void testEncryption() {
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 16; i++) {
            byte[] key = new byte[256 / 8];
            random.nextBytes(key);
            StringBuilder password = new StringBuilder();
            for (int j = random.nextInt(20); j > 0; j--) {
                password.append((char) random.nextInt('z' - 'a' + 1) + 'a');
            }
            String keyFile = Encryption.writeKeyFile(password.toString(), key);
            byte[] key2 = Encryption.readKeyFile(password.toString(), keyFile);
            assertArrayEquals(key, key2);
        }
    }

    @Test
    public void testStream() throws IOException, InvalidKeyException {
        for (int i = 0; i < 64; i++) {
            SecureRandom random = new SecureRandom();
            byte[] key = new byte[256 / 8];
            random.nextBytes(key);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            String stored = RandomString.generateGeometricLength(16, random);
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new EncryptingOutputStream(outputStream, key), StandardCharsets.UTF_8))) {
                writer.write(stored);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new DecryptingInputStream(new RandomChunkingInputStream(
                            new ByteArrayInputStream(outputStream.toByteArray())), key), StandardCharsets.UTF_8))) {
                assertEquals(stored, reader.readLine());
            }
        }
    }
}
