package io.github.software.coursework.data;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;

/**
 * An interface for accessing key-value pairs in a directory.
 */
interface DirectoryAccessor {
    /**
     * Get an item from the directory.
     * @param name the name of the item
     * @param constructor the constructor to use to deserialize the item
     * @return the item, or null if it does not exist. Notice the returned value may either be same instance as you put() or a deserialized copy.
     * @param <T> the type of the item
     * @throws IOException if an I/O error occurs
     */
    <T extends Item> @Nullable T get(String name, Deserialize<T> constructor) throws IOException;

    /**
     * @see #get(String, Deserialize)
     */
    default <T extends Item> @Nullable T get(Reference<T> name, Deserialize<T> constructor) throws IOException {
        return get(Long.toUnsignedString(name.id(), 16), constructor);
    }

    /**
     * Put an item into the directory.
     * @param name the name of the item
     * @param item the item to put
     * @param <T> the type of the item
     * @throws IOException if an I/O error occurs
     */
    <T extends Item> void put(String name, @Nullable T item) throws IOException;

    /**
     * @see #put(String, Item)
     */
    default <T extends Item> void put(Reference<T> name, @Nullable T item) throws IOException {
        put(Long.toUnsignedString(name.id(), 16), item);
    }
}