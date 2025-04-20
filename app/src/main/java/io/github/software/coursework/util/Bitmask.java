package io.github.software.coursework.util;

import com.google.common.primitives.ImmutableLongArray;

public final class Bitmask {
    private Bitmask() {}

    public static boolean get(ImmutableLongArray set, int index, int offset) {
        return (set.get(index / 64 + offset) & (1L << (index % 64))) != 0;
    }

    public static boolean get(long[] set, int index, int offset) {
        return (set[index / 64 + offset] & (1L << (index % 64))) != 0;
    }

    public static boolean set(long[] set, int index, boolean value, int offset) {
        int i = index / 64 + offset;
        long mask = 1L << (index % 64);
        boolean old = (set[i] & mask) != 0;
        if (value) {
            set[i] |= mask;
        } else {
            set[i] &= ~mask;
        }
        return old;
    }

    public record View2D(ImmutableLongArray data, int width) {
        public boolean get(int x, int y) {
            return Bitmask.get(data, y, x * ((width + 63) / 64));
        }
    }

    public record View2DMutable(long[] data, int width) {
        public boolean get(int x, int y) {
            return Bitmask.get(data, y, x * ((width + 63) / 64));
        }

        public boolean set(int x, int y, boolean value) {
            return Bitmask.set(data, y, value, x * ((width + 63) / 64));
        }

        public View2D view() {
            return new View2D(ImmutableLongArray.copyOf(data), width);
        }
    }

    public static View2D view2D(ImmutableLongArray data, int width) {
        return new View2D(data, width);
    }

    public static View2DMutable view2DMutable(long[] data, int width) {
        return new View2DMutable(data, width);
    }

    public static int size2d(int height, int width) {
        return height * ((width + 63) / 64);
    }
}
