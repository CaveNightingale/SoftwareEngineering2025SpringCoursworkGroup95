package io.github.software.coursework.algo;

import com.google.common.collect.ImmutableMap;
import io.github.software.coursework.data.Item;
import io.github.software.coursework.data.Reference;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

/**
 * A class that represents an update to a set of items.
 * @param oldItems The old items.
 * @param newItems The new items.
 * @param <T> The type of the items.
 */
public record Update<T extends Item>(ImmutableMap<Reference<T>, T> oldItems, ImmutableMap<Reference<T>, T> newItems) {
    /**
     * Create a new update with the given old and new items.
     * @param oldItems The old items.
     * @return The new update.
     */
    public Update<T> withOldItems(ImmutableMap<Reference<T>, T> oldItems) { return new Update<>(oldItems, newItems); }

    /**
     * Create a new update with the given new items.
     * @param newItems The new items.
     * @return The new update.
     */
    public Update<T> withNewItems(ImmutableMap<Reference<T>, T> newItems) { return new Update<>(oldItems, newItems); }

    /**
     * Get the old items that were removed.
     * A reference is removed if a reference is present in the old items but not in the new items.
     * @return The removed items.
     */
    public ImmutableMap<Reference<T>, T> removed() {
        return oldItems.entrySet().stream()
                .filter(entry -> !newItems.containsKey(entry.getKey()))
                .collect(ImmutableMap.toImmutableMap(ImmutableMap.Entry::getKey, ImmutableMap.Entry::getValue));
    }

    /**
     * Get the new items that were added.
     * A reference is added if a reference is present in the new items but not in the old items.
     * @return The added items.
     */
    public ImmutableMap<Reference<T>, T> added() {
        return newItems.entrySet().stream()
                .filter(entry -> !oldItems.containsKey(entry.getKey()))
                .collect(ImmutableMap.toImmutableMap(ImmutableMap.Entry::getKey, ImmutableMap.Entry::getValue));
    }

    /**
     * Get the items that were changed.
     * A reference is changed if a reference is present in both the old and new items but the values are different.
     * @return The changed items.
     */
    public ImmutableMap<Reference<T>, T> changed() {
        return newItems.entrySet().stream()
                .filter(entry -> oldItems.containsKey(entry.getKey()) && !Objects.equals(oldItems.get(entry.getKey()), entry.getValue()))
                .collect(ImmutableMap.toImmutableMap(ImmutableMap.Entry::getKey, ImmutableMap.Entry::getValue));
    }

    /**
     * Create a new update with a single item.
     * @param key The key of the item.
     * @param oldItem The old item, or null if it was added.
     * @param newItem The new item, or null if it was removed.
     * @return The new update.
     * @param <T> The type of the item.
     */
    public static <T extends Item> Update<T> single(Reference<T> key, @Nullable T oldItem, @Nullable T newItem) {
        return new Update<>(
                oldItem != null ? ImmutableMap.of(key, oldItem) : ImmutableMap.of(),
                newItem != null ? ImmutableMap.of(key, newItem) : ImmutableMap.of()
        );
    }

    /**
     * Create an update that nothing has changed.
     * @return The empty update.
     * @param <T> The type of the item.
     */
    public static <T extends Item> Update<T> empty() {
        return new Update<>(ImmutableMap.of(), ImmutableMap.of());
    }
}
