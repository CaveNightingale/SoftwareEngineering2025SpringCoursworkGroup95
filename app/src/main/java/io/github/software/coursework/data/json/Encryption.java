package io.github.software.coursework.data.json;

import org.checkerframework.checker.nullness.qual.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;

@ParametersAreNonnullByDefault
public final class Encryption {
    private Encryption() {}

    static final String KEY_START = "--- START OF BUPT-QMUL 2025 SPRING SOFTWARE ENGINEERING COURSEWORK 95 SUBMISSION SECRET KEY ---\n";
    static final String KEY_END = "\n--- END OF BUPT-QMUL 2025 SPRING SOFTWARE ENGINEERING COURSEWORK 95 SUBMISSION SECRET KEY ---\n";
    static final String DATA_START = "--- START OF BUPT-QMUL 2025 SPRING SOFTWARE ENGINEERING COURSEWORK 95 SUBMISSION AES ENCRYPTED DATA ---\n";
    static final String DATA_END = "\n--- END OF BUPT-QMUL 2025 SPRING SOFTWARE ENGINEERING COURSEWORK 95 SUBMISSION AES ENCRYPTED DATA ---\n";

    private static final int SALT_LENGTH = 16;
    private static final int ITERATION_COUNT = 65536;
    private static final int KEY_LENGTH = 256;
    private static final int IV_LENGTH = 16;

    private static byte[] generateSalt(Random random) {
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }

    private static SecretKeySpec generateKey(String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
        byte[] secretKey = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(secretKey, "AES");
    }

    private static byte[] encrypt(String password, byte[] data, Random random) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        byte[] salt = generateSalt(random);
        SecretKeySpec key = generateKey(password, salt);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] encrypted = cipher.doFinal(data);
        byte[] result = new byte[salt.length + iv.length + encrypted.length];
        System.arraycopy(salt, 0, result, 0, salt.length);
        System.arraycopy(iv, 0, result, salt.length, iv.length);
        System.arraycopy(encrypted, 0, result, salt.length + iv.length, encrypted.length);
        return result;
    }

    private static byte @Nullable [] decrypt(String password, byte[] data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[IV_LENGTH];
        byte[] encrypted = new byte[data.length - salt.length - iv.length];
        System.arraycopy(data, 0, salt, 0, salt.length);
        System.arraycopy(data, salt.length, iv, 0, iv.length);
        System.arraycopy(data, salt.length + iv.length, encrypted, 0, encrypted.length);
        try {
            SecretKeySpec key = generateKey(password, salt);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            return cipher.doFinal(encrypted);
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeySpecException e) {
            return null;
        }
    }

    public static String writeKeyFile(String password, byte @Nullable[] key) {
        Random random = new SecureRandom();
        if (key == null) {
            key = new byte[KEY_LENGTH / 8];
            random.nextBytes(key);
        }
        try {
            byte[] encrypted = encrypt(password, key, random);
            String encoded = Base64.getEncoder().encodeToString(encrypted);
            return KEY_START + encoded + KEY_END;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte @Nullable [] readKeyFile(String password, String file) {
        if (!file.startsWith(KEY_START) || !file.endsWith(KEY_END)) {
            return null;
        }
        String strippedKey = file.substring(KEY_START.length(), file.length() - KEY_END.length());
        byte[] encrypted = Base64.getDecoder().decode(strippedKey);
        try {
            return decrypt(password, encrypted);
        } catch (NoSuchPaddingException | InvalidKeyException | NoSuchAlgorithmException |
                 InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean lookLikeKey(File file) {
        if (!Files.isRegularFile(file.toPath())) {
            return false;
        }
        try {
            long size = Files.size(file.toPath());
            if (size > 4096) {
                return false;
            }
            String fileData = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            return fileData.startsWith(KEY_START) && fileData.endsWith(KEY_END);
        } catch (IOException e) {
            return false;
        }
    }
}
