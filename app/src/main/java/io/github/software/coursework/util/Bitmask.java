package io.github.software.coursework.util;

import com.google.common.primitives.ImmutableLongArray;

/**
 * A utility class for manipulating bitmasks.
 */
public final class Bitmask {
    private Bitmask() {}

    /**
     * Get the value of a bit at the specified index in the bitmask.
     * @param set the bitmask
     * @param index the index of the bit to get
     * @param offset the offset to the start of the bitmask
     * @return true if the bit is set, false otherwise
     */
    public static boolean get(ImmutableLongArray set, int index, int offset) {
        return (set.get(index / 64 + offset) & (1L << (index % 64))) != 0;
    }

    /**
     * Get the value of a bit at the specified index in the bitmask.
     * @param set the bitmask
     * @param index the index of the bit to get
     * @param offset the offset to the start of the bitmask
     * @return true if the bit is set, false otherwise
     */
    public static boolean get(long[] set, int index, int offset) {
        return (set[index / 64 + offset] & (1L << (index % 64))) != 0;
    }

    /**
     * Set the value of a bit at the specified index in the bitmask.
     * @param set the bitmask
     * @param index the index of the bit to set
     * @param value the value to set the bit to
     * @param offset the offset to the start of the bitmask
     * @return the old value of the bit
     */
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

    /**
     * A view of a 2D bitmask.
     * @param data The data of the bitmask.
     * @param width The width of the bitmask.
     */
    public record View2D(ImmutableLongArray data, int width) {
        /**
         * Get the value of a bit at the specified index in the bitmask.
         * @param x The x coordinate
         * @param y The y coordinate
         * @return true if the bit is set, false otherwise
         */
        public boolean get(int x, int y) {
            return Bitmask.get(data, y, x * ((width + 63) / 64));
        }
    }

    /**
     * A mutable view of a 2D bitmask.
     * @param data The data of the bitmask.
     * @param width The width of the bitmask.
     */
    public record View2DMutable(long[] data, int width) {
        /**
         * Get the value of a bit at the specified index in the bitmask.
         * @param x The x coordinate
         * @param y The y coordinate
         * @return true if the bit is set, false otherwise
         */
        public boolean get(int x, int y) {
            return Bitmask.get(data, y, x * ((width + 63) / 64));
        }

        /**
         * Set the value of a bit at the specified index in the bitmask.
         * @param x The x coordinate
         * @param y The y coordinate
         * @param value The value to set the bit to
         * @return the old value of the bit
         */
        public boolean set(int x, int y, boolean value) {
            return Bitmask.set(data, y, value, x * ((width + 63) / 64));
        }

        /**
         * Transform the mutable view into an immutable view.
         * @return the immutable view
         */
        public View2D view() {
            return new View2D(ImmutableLongArray.copyOf(data), width);
        }
    }

    /**
     * Create a 2D view of a bitmask.
     * @param data The data of the bitmask.
     * @param width The width of the bitmask.
     * @return the 2D view of the bitmask
     */
    public static View2D view2D(ImmutableLongArray data, int width) {
        return new View2D(data, width);
    }

    /**
     * Create a mutable 2D view of a bitmask.
     * @param data The data of the bitmask.
     * @param width The width of the bitmask.
     * @return the mutable 2D view of the bitmask
     */
    public static View2DMutable view2DMutable(long[] data, int width) {
        return new View2DMutable(data, width);
    }

    /**
     * Estimate the size of a 2D bitmask in number of 64-bit integers.
     * @param height the height of the bitmask (i.e. the first dimension)
     * @param width the width of the bitmask (i.e. the second dimension)
     * @return the size of the bitmask in number of 64-bit integers
     */
    public static int size2d(int height, int width) {
        return height * ((width + 63) / 64);
    }
}
