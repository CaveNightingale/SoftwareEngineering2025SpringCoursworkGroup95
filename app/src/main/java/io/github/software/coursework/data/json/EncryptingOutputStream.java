package io.github.software.coursework.data.json;

import com.google.common.annotations.VisibleForTesting;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

@VisibleForTesting
@ParametersAreNonnullByDefault
public final class EncryptingOutputStream extends OutputStream {
    private final OutputStream backing;
    private final Cipher cipher;
    private int base64Value = 0;
    private int base64Ungrouped = 0;

    public EncryptingOutputStream(OutputStream backing, byte[] key) throws IOException {
        this.backing = backing;
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        try {
            this.cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            this.cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(iv));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new IOException(e);
        }
        backing.write(Encryption.DATA_START.getBytes(StandardCharsets.UTF_8));
        base64Write(iv);
    }

    private byte base64Byte(int value) {
        if (value < 26) {
            return (byte) (value + 'A');
        } else if (value < 52) {
            return (byte) (value - 26 + 'a');
        } else if (value < 62) {
            return (byte) (value - 52 + '0');
        } else if (value == 62) {
            return '+';
        } else {
            return '/';
        }
    }

    private void flushBase64() throws IOException {
        switch (base64Ungrouped) {
            case 0 -> {}
            case 1 -> {
                byte[] bytes = new byte[4];
                bytes[0] = base64Byte(base64Value >> 2);
                bytes[1] = base64Byte((base64Value & 0b11) << 4);
                bytes[2] = '=';
                bytes[3] = '=';
                backing.write(bytes);
            }
            case 2 -> {
                byte[] bytes = new byte[4];
                bytes[0] = base64Byte(base64Value >> 10);
                bytes[1] = base64Byte((base64Value >> 4) & 0b111111);
                bytes[2] = base64Byte((base64Value & 0b1111) << 2);
                bytes[3] = '=';
                backing.write(bytes);
            }
        }
    }

    public void base64Write(byte[] bytes) throws IOException{
        int ungrouped = base64Ungrouped;
        int value = base64Value;
        int encodedSize = (ungrouped + bytes.length) / 3 * 4;
        byte[] encoded = new byte[encodedSize];
        int ptr = 0;
        while (ptr < encoded.length && ungrouped < 3) {
            value = (value << 8) | (bytes[ptr] & 0xFF);
            ptr++;
            ungrouped++;
        }
        if (ungrouped < 3) {
            base64Ungrouped = ungrouped;
            base64Value = value;
            return;
        }
        encoded[0] = base64Byte(value >> 18);
        encoded[1] = base64Byte((value >> 12) & 0b111111);
        encoded[2] = base64Byte((value >> 6) & 0b111111);
        encoded[3] = base64Byte(value & 0b111111);
        for (int j = 4; ptr + 3 <= bytes.length; j += 4) {
            value = (bytes[ptr] & 0xFF) << 16 | (bytes[ptr + 1] & 0xFF) << 8 | (bytes[ptr + 2] & 0xFF);
            encoded[j] = base64Byte(value >> 18);
            encoded[j + 1] = base64Byte((value >> 12) & 0b111111);
            encoded[j + 2] = base64Byte((value >> 6) & 0b111111);
            encoded[j + 3] = base64Byte(value & 0b111111);
            ptr += 3;
        }
        ungrouped = 0;
        value = 0;
        while (ptr < bytes.length) {
            value = (value << 8) | (bytes[ptr] & 0xFF);
            ptr++;
            ungrouped++;
        }
        base64Ungrouped = ungrouped;
        base64Value = value;
        backing.write(encoded);
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        byte[] encrypted = cipher.update(bytes);
        base64Write(encrypted);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        byte[] encrypted = cipher.update(b, off, len);
        base64Write(encrypted);
    }

    @Override
    public void write(int b) throws IOException {
        write(new byte[]{(byte) b});
    }

    @Override
    public void close() throws IOException {
        try {
            byte[] encrypted;
            try {
                encrypted = cipher.doFinal();
            } catch (IllegalBlockSizeException | BadPaddingException e) {
                throw new IOException(e);
            }
            base64Write(encrypted);
            flushBase64();
            backing.write(Encryption.DATA_END.getBytes(StandardCharsets.UTF_8));
        } finally {
            super.close();
            backing.close();
        }
    }
}
