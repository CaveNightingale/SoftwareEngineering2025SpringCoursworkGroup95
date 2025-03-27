package io.github.software.coursework.data.json;

import io.github.software.coursework.data.*;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;

public class ChunkedIndex<T extends Item<T>> implements AutoCloseable {
    private int splitThreshold = 512;
    private int mergeThreshold = 186;
    private final ArrayList<ChunkDescription<T>> chunkDescriptions;
    private final Directory directory;
    private final Comparator<T> comparator;
    private final DeserializationConstructor<T> deserializationConstructor;
    private enum Sentinel {
        POSITIVE_INFINITY,
        NEGATIVE_INFINITY
    }

    @SuppressWarnings("unchecked")
    private int compare(Object a, Object b) {
        if (a == b) {
            return 0;
        }
        if (a == Sentinel.NEGATIVE_INFINITY || b == Sentinel.POSITIVE_INFINITY) {
            return -1;
        }
        if (a == Sentinel.POSITIVE_INFINITY || b == Sentinel.NEGATIVE_INFINITY) {
            return 1;
        }
        return comparator.compare((T) a, (T) b);
    }

    public ChunkedIndex(Directory directory, Comparator<T> comparator, DeserializationConstructor<T> deserializationConstructor) throws IOException {
        this.directory = directory;
        this.comparator = comparator;
        this.deserializationConstructor = deserializationConstructor;
        ChunkIndexWrapper<T> wrapper = directory.get("index", reader -> ChunkIndexWrapper.deserialize(reader, deserializationConstructor));
        chunkDescriptions = wrapper == null ? new ArrayList<>() : wrapper.chunkDescriptions;
    }

    public void setSplitThreshold(int splitThreshold) {
        this.splitThreshold = splitThreshold;
    }

    public void setMergeThreshold(int mergeThreshold) {
        this.mergeThreshold = mergeThreshold;
    }

    public int lookForChunk(Object item) {
        int left = 0;
        int right = chunkDescriptions.size() - 1;
        while (left < right) {
            int mid = (left + right) / 2;
            if (compare(chunkDescriptions.get(mid).max, item) < 0) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }
        return left;
    }

    public void addSample(T item) throws IOException {
        if (chunkDescriptions.isEmpty()) {
            Reference<Chunk<T>> reference = new Reference<>();
            Chunk<T> chunk = new Chunk<>(new ArrayList<T>());
            chunk.items.add(item);
            directory.put(reference, chunk);
            chunkDescriptions.add(new ChunkDescription<>(1, item, item, reference));
        } else {
            int chunkIndex = lookForChunk(item);
            ChunkDescription<T> chunkDescription = chunkDescriptions.get(chunkIndex);
            Chunk<T> chunk = directory.get(chunkDescription.reference(), reader -> Chunk.deserialize(reader, deserializationConstructor));
            if (chunk == null) {
                throw new IOException("Chunk not found");
            }
            chunk.add(item, comparator);
            if (chunk.items.size() > splitThreshold) {
                ArrayList<T> splitItems = new ArrayList<>(chunk.items.subList(chunk.items.size() / 2, chunk.items.size()));
                chunk = new Chunk<>(new ArrayList<>(chunk.items.subList(0, chunk.items.size() / 2)));
                ChunkDescription<T> splitDescription = new ChunkDescription<>(splitItems.size(), splitItems.getFirst(), splitItems.getLast(), new Reference<>());
                directory.put(splitDescription.reference, new Chunk<>(splitItems));
                chunkDescriptions.add(chunkIndex + 1, splitDescription);
            }
            directory.put(chunkDescription.reference(), chunk);
            chunkDescriptions.set(chunkIndex, new ChunkDescription<>(chunk.items.size(), chunk.items.getFirst(), chunk.items.getLast(), chunkDescription.reference));
        }
    }

    public void removeSample(T item) throws IOException {
        if (chunkDescriptions.isEmpty()) {
            return;
        }
        int chunkIndex = lookForChunk(item);
        ChunkDescription<T> chunkDescription = chunkDescriptions.get(chunkIndex);
        Chunk<T> chunk = directory.get(chunkDescription.reference(), reader -> Chunk.deserialize(reader, deserializationConstructor));
        if (chunk == null) {
            throw new IOException("Chunk not found");
        }
        chunk.remove(item);
        directory.put(chunkDescription.reference, chunk);
        if (chunk.items.size() <= mergeThreshold && chunkDescriptions.size() > 1) {
            ChunkDescription<T> nextChunkDescription = chunkIndex == chunkDescriptions.size() - 1 ? null : chunkDescriptions.get(chunkIndex + 1);
            ChunkDescription<T> prevChunkDescription = chunkIndex == 0 ? null : chunkDescriptions.get(chunkIndex - 1);
            if (chunkIndex == 0 || (nextChunkDescription != null && prevChunkDescription.count > nextChunkDescription.count)) {
                assert nextChunkDescription != null;
                Chunk<T> nextChunk = directory.get(nextChunkDescription.reference(), reader -> Chunk.deserialize(reader, deserializationConstructor));
                if (nextChunk == null) {
                    throw new IOException("Chunk not found");
                }
                chunk.items.addAll(nextChunk.items);
                directory.put(chunkDescription.reference, chunk);
                directory.put(nextChunkDescription.reference, null);
                chunkDescriptions.remove(chunkIndex + 1);
                chunkDescriptions.set(chunkIndex, new ChunkDescription<>(chunk.items.size(), chunk.items.getFirst(), chunk.items.getLast(), chunkDescription.reference));
            } else if (chunkIndex == chunkDescriptions.size() - 1) {
                assert prevChunkDescription != null;
                Chunk<T> prevChunk = directory.get(prevChunkDescription.reference(), reader -> Chunk.deserialize(reader, deserializationConstructor));
                if (prevChunk == null) {
                    throw new IOException("Chunk not found");
                }
                prevChunk.items.addAll(chunk.items);
                directory.put(prevChunkDescription.reference, prevChunk);
                directory.put(chunkDescription.reference, null);
                chunkDescriptions.remove(chunkIndex);
                chunkDescriptions.set(chunkIndex - 1, new ChunkDescription<>(prevChunk.items.size(), prevChunk.items.getFirst(), prevChunk.items.getLast(), prevChunkDescription.reference));
            }
        }
    }

    public ArrayList<T> querySamples(@Nullable T min1, @Nullable T max1, int skip, int limit) throws IOException {
        Object min = min1 == null ? Sentinel.NEGATIVE_INFINITY : min1;
        Object max = max1 == null ? Sentinel.POSITIVE_INFINITY : max1;
        ArrayList<T> result = new ArrayList<>();
        if (chunkDescriptions.isEmpty()) {
            return result;
        }
        int left = lookForChunk(min);
        ChunkDescription<T> leftChunkDescription = chunkDescriptions.get(left);
        Chunk<T> leftChunk = directory.get(leftChunkDescription.reference(), reader -> Chunk.deserialize(reader, deserializationConstructor));
        if (leftChunk == null) {
            throw new IOException("Chunk not found in query");
        }
        for (T element : leftChunk.items) {
            if (compare(element, max) >= 0) {
                return result;
            }
            if (compare(element, min) < 0) {
                continue;
            }
            if (skip > 0) {
                skip--;
            } else {
                result.add(element);
                if (result.size() >= limit) {
                    return result;
                }
            }
        }
        for (int i = left + 1; i < chunkDescriptions.size(); i++) {
            ChunkDescription<T> chunkDescription = chunkDescriptions.get(i);
            if (compare(chunkDescription.min, max) >= 0) {
                return result;
            }
            if (chunkDescription.count <= skip) {
                skip -= chunkDescription.count;
                continue;
            }
            Chunk<T> chunk = directory.get(chunkDescription.reference(), reader -> Chunk.deserialize(reader, deserializationConstructor));
            if (chunk == null) {
                throw new IOException("Chunk not found in query");
            }
            for (T element : chunk.items) {
                if (compare(element, max) >= 0) {
                    return result;
                }
                if (skip > 0) {
                    skip--;
                } else {
                    result.add(element);
                    if (result.size() >= limit) {
                        return result;
                    }
                }
            }
        }
        return result;
    }

    public void flush() throws IOException {
        directory.put("index", new ChunkIndexWrapper<>(chunkDescriptions));
        directory.flush();
    }

    @Override
    public void close() throws IOException {
        flush();
        directory.close();
    }

    private record ChunkDescription<T extends Item<T>>(int count, T min, T max, Reference<Chunk<T>> reference) {
    }

    private record ChunkIndexWrapper<T extends Item<T>>(ArrayList<ChunkDescription<T>> chunkDescriptions) implements Item<ChunkIndexWrapper<T>> {

        @Override
        public void serialize(Document.Writer writer) throws IOException {
            for (int i = 0; i < chunkDescriptions.size(); i++) {
                ChunkDescription<T> chunkDescription = chunkDescriptions.get(i);
                Document.Writer chunkWriter = writer.writeCompound(i);
                chunkWriter.writeInteger("count", chunkDescription.count);
                chunkDescription.min.serialize(chunkWriter.writeCompound("min"));
                chunkDescription.max.serialize(chunkWriter.writeCompound("max"));
                chunkWriter.writeReference("reference", chunkDescription.reference);
                chunkWriter.writeEnd();
            }
            writer.writeEnd();
        }

        @SuppressWarnings("unchecked")
        public static <T extends Item<T>> ChunkIndexWrapper<T> deserialize(Document.Reader reader, DeserializationConstructor<T> constructor) throws IOException {
            ArrayList<ChunkDescription<T>> chunkDescriptions = new ArrayList<>();
            for (int i = 0; !reader.isEnd(); i++) {
                Document.Reader chunkReader = reader.readCompound(i);
                int count = (int) chunkReader.readInteger("count");
                T min = constructor.deserialize(chunkReader.readCompound("min"));
                T max = constructor.deserialize(chunkReader.readCompound("max"));
                Reference<Chunk<T>> reference = (Reference<Chunk<T>>) chunkReader.readReference("reference");
                chunkDescriptions.add(new ChunkDescription<>(count, min, max, reference));
                chunkReader.readEnd();
            }
            reader.readEnd();
            return new ChunkIndexWrapper<>(chunkDescriptions);
        }
    }

    private record Chunk<T extends Item<T>>(ArrayList<T> items) implements Item<Chunk<T>> {
        @Override
        public void serialize(Document.Writer writer) throws IOException {
            for (int i = 0; i < items.size(); i++) {
                items.get(i).serialize(writer.writeCompound(i));
            }
            writer.writeEnd();
        }

        public static <T extends Item<T>> Chunk<T> deserialize(Document.Reader reader, DeserializationConstructor<T> constructor) throws IOException {
            ArrayList<T> items = new ArrayList<>();
            for (int i = 0; !reader.isEnd(); i++) {
                items.add(constructor.deserialize(reader.readCompound(i)));
            }
            reader.readEnd();
            return new Chunk<>(items);
        }

        public void add(T item, Comparator<T> comparator) {
            int index = 0;
            while (index < items.size() && comparator.compare(items.get(index), item) < 0) {
                index++;
            }
            items.add(index, item);
        }

        public void remove(T item) {
            items.remove(item);
        }
    }
}
