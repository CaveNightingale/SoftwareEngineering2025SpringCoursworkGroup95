package io.github.software.coursework.algo;

import com.google.common.collect.ImmutableMap;
import io.github.software.coursework.data.Item;
import io.github.software.coursework.data.Reference;

public record Update<T extends Item<T>>(ImmutableMap<Reference<T>, T> oldItems, ImmutableMap<Reference<T>, T> newItems) {
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
}
