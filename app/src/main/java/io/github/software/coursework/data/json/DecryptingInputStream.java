package io.github.software.coursework.data.json;

import com.google.common.annotations.VisibleForTesting;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

@VisibleForTesting
public final class DecryptingInputStream extends InputStream {
    private final InputStream backing;
    private final Cipher cipher;
    private int base64Value;
    private int base64Ungrouped;
    private boolean base64Ended;
    private final byte[] decryptedBuffer;
    private int decryptedBufferOffset = 0;
    private boolean decryptionEnded;

    public DecryptingInputStream(InputStream backing, byte[] key) throws IOException {
        this.backing = backing;
        if (!Arrays.equals(this.backing.readNBytes(Encryption.DATA_START.length()), Encryption.DATA_START.getBytes(StandardCharsets.UTF_8))) {
            throw new IOException("Invalid data start");
        }
        try {
            this.cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = new byte[cipher.getBlockSize()];
            for (int i = 0; i < iv.length;) {
                int read = base64Read(iv, i, iv.length - i);
                if (read == -1) {
                    throw new IOException("Invalid data start");
                }
                i += read;
            }
            this.cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException |
                 InvalidKeyException e) {
            throw new IOException(e);
        }
        decryptedBuffer = new byte[cipher.getBlockSize()];
    }

    public int readInternal(byte @NonNull [] bytes, int offset, int length) throws IOException {
        if (decryptedBufferOffset != 0) {
            System.arraycopy(decryptedBuffer, decryptedBufferOffset, bytes, offset, length);
            decryptedBufferOffset = 0;
            return length;
        }
        if (decryptionEnded) {
            return -1;
        }
        int blockWanted = (length + cipher.getBlockSize() - 1) / cipher.getBlockSize();
        byte[] blocks = new byte[blockWanted * cipher.getBlockSize()];
        int read = base64Read(blocks, 0, blocks.length);
        byte[] decrypted;
        try {
            decrypted = read == -1 ? cipher.doFinal() : cipher.update(blocks, 0, read);
            if (read == -1) {
                decryptionEnded = true;
            }
            if (decrypted == null) { // Too short for a block?
                return 0; // Skip this round
            }
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new IOException(e);
        }
        if (decrypted.length > length) {
            System.arraycopy(decrypted, 0, bytes, offset, length);
            decryptedBufferOffset = decrypted.length - length;
            System.arraycopy(decrypted, length, decryptedBuffer, 0, decrypted.length - length);
            return length;
        } else {
            System.arraycopy(decrypted, 0, bytes, offset, decrypted.length);
            return decrypted.length;
        }
    }

    @Override
    public int read(byte @NonNull [] b, int off, int len) throws IOException {
        int n = 0;
        while (n == 0) { // Retry until making progress or reaching EOF
            n = readInternal(b, off, len);
        }
        return n;
    }

    @Override
    public int read() throws IOException {
        byte[] buffer = new byte[1];
        int read = read(buffer, 0, 1);
        return read == -1 ? -1 : (buffer[0] & 0xFF);
    }

    private int base64Read(byte[] bytes, int offset, int length) throws IOException {
        if (base64Ended) {
            return -1;
        }
        int maxSize = (length * 8 - base64Ungrouped + 6 - 1) / 6;
        byte[] buffer = new byte[maxSize];
        int read = backing.read(buffer);
        int ptr = offset;
        for (int i = 0; i < read; i++) {
            int decoded = base64Decode(buffer[i]);
            if (decoded == -1) {
                base64Ended = true;
                int padding = switch (base64Ungrouped) {
                    case 0 -> 0;
                    case 2 -> 1;
                    case 4 -> 2;
                    default -> throw new AssertionError(); // Unreachable
                };
                int rest = padding + Encryption.DATA_END.length();
                byte[] restBytes = new byte[rest];
                if (i + rest <= read) {
                    System.arraycopy(buffer, i, restBytes, 0, rest);
                } else {
                    int right = read - i;
                    int rest2 = rest - right;
                    byte[] buffer2 = backing.readNBytes(rest2);
                    if (buffer2.length < rest2) {
                        throw new IOException("Invalid data end");
                    }
                    System.arraycopy(buffer, i, restBytes, 0, right);
                    System.arraycopy(buffer2, 0, restBytes, right, rest2);
                }
                int paddingPtr = 0;
                while (paddingPtr < restBytes.length && restBytes[paddingPtr] == '=') {
                    paddingPtr++;
                }
                if (!Arrays.equals(restBytes, paddingPtr, restBytes.length, Encryption.DATA_END.getBytes(StandardCharsets.UTF_8), 0, Encryption.DATA_END.length())) {
                    throw new IOException("Invalid data end");
                }
                break;
            }
            base64Value = base64Value << 6 | decoded;
            base64Ungrouped += 6;
            if (base64Ungrouped >= 8) {
                bytes[ptr++] = (byte) (base64Value >> (base64Ungrouped - 8));
                base64Ungrouped -= 8;
            }
        }
        return ptr - offset;
    }

    private static int base64Decode(byte data) {
        if (data >= 'A' && data <= 'Z') {
            return data - 'A';
        } else if (data >= 'a' && data <= 'z') {
            return data - 'a' + 26;
        } else if (data >= '0' && data <= '9') {
            return data - '0' + 52;
        } else if (data == '+') {
            return 62;
        } else if (data == '/') {
            return 63;
        } else {
            return -1;
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
        backing.close();
    }
}
