package io.github.software.coursework.data;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;

interface DirectoryAccessor {
    <T extends Item<T>> @Nullable T get(String name, DeserializationConstructor<T> constructor) throws IOException;
    default <T extends Item<T>> @Nullable T get(Reference<T> name, DeserializationConstructor<T> constructor) throws IOException {
        return get(Long.toUnsignedString(name.id(), 16), constructor);
    }

    <T extends Item<T>> void put(String name, @Nullable T item) throws IOException;
    default <T extends Item<T>> void put(Reference<T> name, @Nullable T item) throws IOException {
        put(Long.toUnsignedString(name.id(), 16), item);
    }
}