package io.github.software.coursework.algo;

import com.google.common.collect.ImmutableMap;
import io.github.software.coursework.data.Item;
import io.github.software.coursework.data.Reference;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

public record Update<T extends Item>(ImmutableMap<Reference<T>, T> oldItems, ImmutableMap<Reference<T>, T> newItems) {
    public Update<T> withOldItems(ImmutableMap<Reference<T>, T> oldItems) { return new Update<>(oldItems, newItems); }
    public Update<T> withNewItems(ImmutableMap<Reference<T>, T> newItems) { return new Update<>(oldItems, newItems); }

    public ImmutableMap<Reference<T>, T> removed() {
        return oldItems.entrySet().stream()
                .filter(entry -> !newItems.containsKey(entry.getKey()))
                .collect(ImmutableMap.toImmutableMap(ImmutableMap.Entry::getKey, ImmutableMap.Entry::getValue));
    }

    public ImmutableMap<Reference<T>, T> added() {
        return newItems.entrySet().stream()
                .filter(entry -> !oldItems.containsKey(entry.getKey()))
                .collect(ImmutableMap.toImmutableMap(ImmutableMap.Entry::getKey, ImmutableMap.Entry::getValue));
    }

    public ImmutableMap<Reference<T>, T> changed() {
        return newItems.entrySet().stream()
                .filter(entry -> oldItems.containsKey(entry.getKey()) && !Objects.equals(oldItems.get(entry.getKey()), entry.getValue()))
                .collect(ImmutableMap.toImmutableMap(ImmutableMap.Entry::getKey, ImmutableMap.Entry::getValue));
    }

    public static <T extends Item> Update<T> single(Reference<T> key, @Nullable T oldItem, @Nullable T newItem) {
        return new Update<>(
                oldItem != null ? ImmutableMap.of(key, oldItem) : ImmutableMap.of(),
                newItem != null ? ImmutableMap.of(key, newItem) : ImmutableMap.of()
        );
    }

    public static <T extends Item> Update<T> empty() {
        return new Update<>(ImmutableMap.of(), ImmutableMap.of());
    }
}
