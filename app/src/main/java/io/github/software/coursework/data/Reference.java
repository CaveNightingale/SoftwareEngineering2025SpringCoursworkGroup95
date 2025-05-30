package io.github.software.coursework.data;

import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * A reference to an item.
 * @param id The id of the item.
 * @param <T> The type of the item.
 */
public record Reference<T extends Item>(long id) {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public Reference() {
        this(SECURE_RANDOM.nextLong());
    }

    public Reference(String string) {
        this(Hashing.murmur3_128().hashString(string, StandardCharsets.UTF_8).asLong());
    }

}
