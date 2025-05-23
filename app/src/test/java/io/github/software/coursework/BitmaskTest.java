package io.github.software.coursework;

import com.google.common.primitives.ImmutableLongArray;
import io.github.software.coursework.util.Bitmask;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BitmaskTest {

    @Test
    public void testGetWithImmutableLongArray() {
        ImmutableLongArray array = ImmutableLongArray.of(0L, 2L, 0L);

        // Bit at index 65 (in the second long) should be set (2L = ...10 in binary)
        assertTrue(Bitmask.get(array, 65, 0));

        // Bits at indices 0, 64, and 128 should not be set
        assertFalse(Bitmask.get(array, 0, 0));
        assertFalse(Bitmask.get(array, 64, 0));
        assertFalse(Bitmask.get(array, 128, 0));

        // Test with offset
        assertTrue(Bitmask.get(array, 1, 1));
        assertFalse(Bitmask.get(array, 1, 0));
    }

    @Test
    public void testGetWithLongArray() {
        long[] array = new long[]{0L, 2L, 0L};

        // Bit at index 65 (in the second long) should be set (2L = ...10 in binary)
        assertTrue(Bitmask.get(array, 65, 0));

        // Bits at indices 0, 64, and 128 should not be set
        assertFalse(Bitmask.get(array, 0, 0));
        assertFalse(Bitmask.get(array, 64, 0));
        assertFalse(Bitmask.get(array, 128, 0));

        // Test with offset
        assertTrue(Bitmask.get(array, 1, 1));
        assertFalse(Bitmask.get(array, 1, 0));
    }

    @Test
    public void testSetWithLongArray() {
        long[] array = new long[3];

        // Initially all bits should be unset
        assertFalse(Bitmask.get(array, 65, 0));

        // Set bit at index 65 and verify the return value (should be false initially)
        boolean oldValue = Bitmask.set(array, 65, true, 0);
        assertFalse(oldValue);

        // Verify the bit is now set
        assertTrue(Bitmask.get(array, 65, 0));

        // Set the same bit again and verify the return value (should be true now)
        oldValue = Bitmask.set(array, 65, true, 0);
        assertTrue(oldValue);

        // Set the bit to false and verify the return value and the new state
        oldValue = Bitmask.set(array, 65, false, 0);
        assertTrue(oldValue);
        assertFalse(Bitmask.get(array, 65, 0));

        // Test with offset
        assertFalse(Bitmask.set(array, 1, true, 1));
        assertTrue(Bitmask.get(array, 1, 1));
    }

    @Test
    public void testView2D() {
        ImmutableLongArray array = ImmutableLongArray.of(1L, 2L, 4L, 8L);
        Bitmask.View2D view = Bitmask.view2D(array, 128);

        // With width=128, each row takes 2 longs (128 bits)
        // First row: 1L (bit 0 set)
        // Second row: 2L (bit 1 set)
        assertTrue(view.get(0, 0));
        assertTrue(view.get(0, 65));
        assertTrue(view.get(1, 2));
        assertFalse(view.get(0, 1));
        assertFalse(view.get(1, 0));

        // Test with a different width
        view = Bitmask.view2D(array, 64);
        // With width=64, each row takes 1 long (64 bits)
        assertTrue(view.get(0, 0));
        assertTrue(view.get(1, 1));
        assertTrue(view.get(2, 2));
        assertTrue(view.get(3, 3));
        assertFalse(view.get(0, 1));
    }

    @Test
    public void testView2DMutable() {
        long[] array = new long[]{1L, 2L, 4L, 8L};
        Bitmask.View2DMutable view = Bitmask.view2DMutable(array, 128);

        // Test get method
        assertTrue(view.get(0, 0));
        assertTrue(view.get(0, 65));
        assertFalse(view.get(0, 1));

        // Test set method
        assertFalse(view.set(0, 1, true));
        assertTrue(view.get(0, 1));
        assertTrue(view.set(0, 1, false));
        assertFalse(view.get(0, 1));

        // Test creating an immutable view
        Bitmask.View2D immutableView = view.view();
        assertTrue(immutableView.get(0, 0));
        assertTrue(immutableView.get(0, 65));

        // Modify the mutable view and verify the immutable view is not affected
        view.set(0, 0, false);
        assertFalse(view.get(0, 0));
        assertTrue(immutableView.get(0, 0));
    }

    @Test
    public void testSize2d() {
        // Test various dimensions
        assertEquals(1, Bitmask.size2d(1, 1));
        assertEquals(1, Bitmask.size2d(1, 64));
        assertEquals(2, Bitmask.size2d(1, 65));
        assertEquals(2, Bitmask.size2d(1, 128));
        assertEquals(2, Bitmask.size2d(2, 64));
        assertEquals(4, Bitmask.size2d(2, 65));
        assertEquals(4, Bitmask.size2d(2, 128));

        // Test edge cases
        assertEquals(0, Bitmask.size2d(0, 10));
        assertEquals(0, Bitmask.size2d(10, 0));
        assertEquals(16, Bitmask.size2d(1, 1000));
        assertEquals(160, Bitmask.size2d(10, 1000));
    }
}