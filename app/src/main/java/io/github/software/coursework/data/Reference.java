package io.github.software.coursework.data;

import java.security.SecureRandom;

public record Reference<T extends Item<T>>(long id) {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public Reference() {
        this(SECURE_RANDOM.nextLong());
    }
}
