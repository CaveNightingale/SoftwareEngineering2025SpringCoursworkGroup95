package io.github.software.coursework;

import io.github.software.coursework.gui.rendering.RenderingClipper;
import io.github.software.coursework.gui.rendering.RenderingState;
import io.github.software.coursework.gui.rendering.RenderingTransformer;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class RenderingTest {
    @Test
    public void testDefaultValues() {
        RenderingState renderingState = new RenderingState();
        // Test default values
        assertEquals(50, renderingState.getDataPaddingLeft());
        assertEquals(50, renderingState.getDataPaddingRight());
        assertEquals(25, renderingState.getDataPaddingTop());
        assertEquals(25, renderingState.getDataPaddingBottom());
        assertEquals(0, renderingState.getXMin());
        assertEquals(1, renderingState.getXMax());
        assertEquals(0, renderingState.getYMin());
        assertEquals(1, renderingState.getYMax());
        assertTrue(renderingState.isYInverted());
    }

    @Test
    public void testScreenDimensions() {
        RenderingState renderingState = new RenderingState();
        // Test screen dimensions
        renderingState.setScreenWidth(800);
        renderingState.setScreenHeight(600);

        assertEquals(800, renderingState.getScreenWidth());
        assertEquals(600, renderingState.getScreenHeight());
    }

    @Test
    public void testDataPadding() {
        RenderingState renderingState = new RenderingState();
        // Test data padding setters and getters
        renderingState.setDataPaddingLeft(60);
        renderingState.setDataPaddingRight(70);
        renderingState.setDataPaddingTop(35);
        renderingState.setDataPaddingBottom(40);

        assertEquals(60, renderingState.getDataPaddingLeft());
        assertEquals(70, renderingState.getDataPaddingRight());
        assertEquals(35, renderingState.getDataPaddingTop());
        assertEquals(40, renderingState.getDataPaddingBottom());
    }

    @Test
    public void testDataBounds() {
        RenderingState renderingState = new RenderingState();
        // Test data bounds setters and getters
        renderingState.setXMin(-10);
        renderingState.setXMax(10);
        renderingState.setYMin(-5);
        renderingState.setYMax(5);

        assertEquals(-10, renderingState.getXMin());
        assertEquals(10, renderingState.getXMax());
        assertEquals(-5, renderingState.getYMin());
        assertEquals(5, renderingState.getYMax());
    }

    @Test
    public void testEpsilonValues() {
        RenderingState renderingState = new RenderingState();
        // Test epsilon calculations
        renderingState.setXMin(0);
        renderingState.setXMax(10);
        renderingState.setYMin(0);
        renderingState.setYMax(5);

        assertEquals(0.01, renderingState.getXEps());
        assertEquals(0.005, renderingState.getYEps());

        // Test with different bounds
        renderingState.setXMin(-100);
        renderingState.setXMax(100);
        renderingState.setYMin(-50);
        renderingState.setYMax(50);

        assertEquals(0.2, renderingState.getXEps());
        assertEquals(0.1, renderingState.getYEps());
    }

    @Test
    public void testDataPaddingLeft() {
        RenderingState renderingState = new RenderingState();

        // Test default value
        assertEquals(50.0, renderingState.getDataPaddingLeft());

        // Test setting new value
        renderingState.setDataPaddingLeft(75.0);
        assertEquals(75.0, renderingState.getDataPaddingLeft());

        // Test setting to zero
        renderingState.setDataPaddingLeft(0.0);
        assertEquals(0.0, renderingState.getDataPaddingLeft());

        // Test setting to negative (should be allowed in this implementation)
        renderingState.setDataPaddingLeft(-10.0);
        assertEquals(-10.0, renderingState.getDataPaddingLeft());
    }

    @Test
    public void testDataPaddingRight() {
        RenderingState renderingState = new RenderingState();

        // Test default value
        assertEquals(50.0, renderingState.getDataPaddingRight());

        // Test setting new value
        renderingState.setDataPaddingRight(65.0);
        assertEquals(65.0, renderingState.getDataPaddingRight());
    }

    @Test
    public void testDataPaddingTop() {
        RenderingState renderingState = new RenderingState();

        // Test default value
        assertEquals(25.0, renderingState.getDataPaddingTop());

        // Test setting new value
        renderingState.setDataPaddingTop(35.0);
        assertEquals(35.0, renderingState.getDataPaddingTop());
    }

    @Test
    public void testDataPaddingBottom() {
        RenderingState renderingState = new RenderingState();

        // Test default value
        assertEquals(25.0, renderingState.getDataPaddingBottom());

        // Test setting new value
        renderingState.setDataPaddingBottom(40.0);
        assertEquals(40.0, renderingState.getDataPaddingBottom());
    }

    @Test
    public void testXBounds() {
        RenderingState renderingState = new RenderingState();

        // Test default values
        assertEquals(0.0, renderingState.getXMin());
        assertEquals(1.0, renderingState.getXMax());

        // Test setting new values
        renderingState.setXMin(-10.0);
        renderingState.setXMax(10.0);
        assertEquals(-10.0, renderingState.getXMin());
        assertEquals(10.0, renderingState.getXMax());

        // Test xMin > xMax (allowed in this implementation)
        renderingState.setXMin(20.0);
        renderingState.setXMax(10.0);
        assertEquals(20.0, renderingState.getXMin());
        assertEquals(10.0, renderingState.getXMax());
    }

    @Test
    public void testYBounds() {
        RenderingState renderingState = new RenderingState();

        // Test default values
        assertEquals(0.0, renderingState.getYMin());
        assertEquals(1.0, renderingState.getYMax());

        // Test setting new values
        renderingState.setYMin(-5.0);
        renderingState.setYMax(5.0);
        assertEquals(-5.0, renderingState.getYMin());
        assertEquals(5.0, renderingState.getYMax());
    }

    @Test
    public void testYInversion() {
        RenderingState renderingState = new RenderingState();

        // Test default value
        assertTrue(renderingState.isYInverted());

        // Test toggling the value
        renderingState.setYInverted(false);
        assertFalse(renderingState.isYInverted());

        renderingState.setYInverted(true);
        assertTrue(renderingState.isYInverted());
    }

    @Test
    public void testXEpsilon() {
        RenderingState renderingState = new RenderingState();

        // Test default epsilon (1-0)/1000 = 0.001
        assertEquals(0.001, renderingState.getXEps());

        // Test with different bounds
        renderingState.setXMin(0.0);
        renderingState.setXMax(10.0);
        assertEquals(0.01, renderingState.getXEps());

        // Test with negative range
        renderingState.setXMin(10.0);
        renderingState.setXMax(0.0);
        assertEquals(-0.01, renderingState.getXEps());

        // Test with larger range
        renderingState.setXMin(-500.0);
        renderingState.setXMax(500.0);
        assertEquals(1.0, renderingState.getXEps());
    }

    @Test
    public void testYEpsilon() {
        RenderingState renderingState = new RenderingState();

        // Test default epsilon (1-0)/1000 = 0.001
        assertEquals(0.001, renderingState.getYEps());

        // Test with different bounds
        renderingState.setYMin(0.0);
        renderingState.setYMax(5.0);
        assertEquals(0.005, renderingState.getYEps());

        // Test with zero range (edge case)
        renderingState.setYMin(5.0);
        renderingState.setYMax(5.0);
        assertEquals(0.0, renderingState.getYEps());
    }

    @Test
    public void testMultiplePropertyChanges() {
        RenderingState renderingState = new RenderingState();

        // Setup a complete configuration
        renderingState.setScreenWidth(1920.0);
        renderingState.setScreenHeight(1080.0);
        renderingState.setDataPaddingLeft(100.0);
        renderingState.setDataPaddingRight(100.0);
        renderingState.setDataPaddingTop(50.0);
        renderingState.setDataPaddingBottom(50.0);
        renderingState.setXMin(-100.0);
        renderingState.setXMax(100.0);
        renderingState.setYMin(-50.0);
        renderingState.setYMax(50.0);
        renderingState.setYInverted(false);

        // Verify all properties
        assertEquals(1920.0, renderingState.getScreenWidth());
        assertEquals(1080.0, renderingState.getScreenHeight());
        assertEquals(100.0, renderingState.getDataPaddingLeft());
        assertEquals(100.0, renderingState.getDataPaddingRight());
        assertEquals(50.0, renderingState.getDataPaddingTop());
        assertEquals(50.0, renderingState.getDataPaddingBottom());
        assertEquals(-100.0, renderingState.getXMin());
        assertEquals(100.0, renderingState.getXMax());
        assertEquals(-50.0, renderingState.getYMin());
        assertEquals(50.0, renderingState.getYMax());
        assertFalse(renderingState.isYInverted());
        assertEquals(0.2, renderingState.getXEps());
        assertEquals(0.1, renderingState.getYEps());
    }

    @Test
    public void testFromDataX() {
        // Create a RenderingState with specific values
        RenderingState state = new RenderingState();
        state.setScreenWidth(800);
        state.setDataPaddingLeft(50);
        state.setDataPaddingRight(50);
        state.setXMin(0);
        state.setXMax(100);

        RenderingTransformer transformer = new RenderingTransformer(state);

        // Test conversion from data coordinates to screen coordinates
        // At xMin (0), should be at left padding (50)
        assertEquals(50, transformer.fromDataX(0), 0.001);

        // At xMax (100), should be at (screenWidth - rightPadding) = 750
        assertEquals(750, transformer.fromDataX(100), 0.001);

        // At midpoint (50), should be at midpoint of data area = 400
        assertEquals(400, transformer.fromDataX(50), 0.001);

        // Test value outside range
        assertEquals(-300, transformer.fromDataX(-50), 0.001);
        assertEquals(1100, transformer.fromDataX(150), 0.001);
    }

    @Test
    public void testFromDataY_YInverted() {
        // Create a RenderingState with specific values and Y-inverted (default)
        RenderingState state = new RenderingState();
        state.setScreenHeight(600);
        state.setDataPaddingTop(40);
        state.setDataPaddingBottom(60);
        state.setYMin(0);
        state.setYMax(10);
        state.setYInverted(true);

        RenderingTransformer transformer = new RenderingTransformer(state);

        // With Y-inverted:
        // At yMin (0), should be at (screenHeight - bottomPadding) = 540
        assertEquals(540, transformer.fromDataY(0), 0.001);

        // At yMax (10), should be at top padding (40)
        assertEquals(40, transformer.fromDataY(10), 0.001);

        // At midpoint (5), should be at 290
        assertEquals(290, transformer.fromDataY(5), 0.001);
    }

    @Test
    public void testFromDataY_YNotInverted() {
        // Create a RenderingState with Y not inverted
        RenderingState state = new RenderingState();
        state.setScreenHeight(600);
        state.setDataPaddingTop(40);
        state.setDataPaddingBottom(60);
        state.setYMin(0);
        state.setYMax(10);
        state.setYInverted(false);

        RenderingTransformer transformer = new RenderingTransformer(state);

        // With Y not inverted:
        // At yMin (0), should be at top padding (40)
        assertEquals(40, transformer.fromDataY(0), 0.001);

        // At yMax (10), should be at (screenHeight - bottomPadding) = 540
        assertEquals(540, transformer.fromDataY(10), 0.001);

        // At midpoint (5), should be at 290
        assertEquals(290, transformer.fromDataY(5), 0.001);
    }

    @Test
    public void testToDataX() {
        // Create a RenderingState with specific values
        RenderingState state = new RenderingState();
        state.setScreenWidth(800);
        state.setDataPaddingLeft(50);
        state.setDataPaddingRight(50);
        state.setXMin(0);
        state.setXMax(100);

        RenderingTransformer transformer = new RenderingTransformer(state);

        // Test conversion from screen coordinates to data coordinates
        // At left padding (50), should be at xMin (0)
        assertEquals(0, transformer.toDataX(50), 0.001);

        // At (screenWidth - rightPadding) = 750, should be at xMax (100)
        assertEquals(100, transformer.toDataX(750), 0.001);

        // At midpoint of data area (400), should be at data midpoint (50)
        assertEquals(50, transformer.toDataX(400), 0.001);
    }

    @Test
    public void testToDataY_YInverted() {
        // Create a RenderingState with Y-inverted (default)
        RenderingState state = new RenderingState();
        state.setScreenHeight(600);
        state.setDataPaddingTop(40);
        state.setDataPaddingBottom(60);
        state.setYMin(0);
        state.setYMax(10);
        state.setYInverted(true);

        RenderingTransformer transformer = new RenderingTransformer(state);

        // With Y-inverted:
        // At top padding (40), should be at yMax (10)
        assertEquals(10, transformer.toDataY(40), 0.001);

        // At (screenHeight - bottomPadding) = 540, should be at yMin (0)
        assertEquals(0, transformer.toDataY(540), 0.001);

        // At 290, should be at data midpoint (5)
        assertEquals(5, transformer.toDataY(290), 0.001);
    }

    @Test
    public void testToDataY_YNotInverted() {
        // Create a RenderingState with Y not inverted
        RenderingState state = new RenderingState();
        state.setScreenHeight(600);
        state.setDataPaddingTop(40);
        state.setDataPaddingBottom(60);
        state.setYMin(0);
        state.setYMax(10);
        state.setYInverted(false);

        RenderingTransformer transformer = new RenderingTransformer(state);

        // With Y not inverted:
        // At top padding (40), should be at yMin (0)
        assertEquals(0, transformer.toDataY(40), 0.001);

        // At (screenHeight - bottomPadding) = 540, should be at yMax (10)
        assertEquals(10, transformer.toDataY(540), 0.001);

        // At 290, should be at data midpoint (5)
        assertEquals(5, transformer.toDataY(290), 0.001);
    }

    @Test
    public void testFracXToDataX() {
        RenderingState state = new RenderingState();
        state.setXMin(-100);
        state.setXMax(100);

        RenderingTransformer transformer = new RenderingTransformer(state);

        // 0.0 should map to xMin
        assertEquals(-100, transformer.fracXToDataX(0.0), 0.001);

        // 1.0 should map to xMax
        assertEquals(100, transformer.fracXToDataX(1.0), 0.001);

        // 0.5 should map to midpoint
        assertEquals(0, transformer.fracXToDataX(0.5), 0.001);

        // Values outside [0,1] range
        assertEquals(-200, transformer.fracXToDataX(-0.5), 0.001);
        assertEquals(200, transformer.fracXToDataX(1.5), 0.001);
    }

    @Test
    public void testFracYToDataY() {
        RenderingState state = new RenderingState();
        state.setYMin(0);
        state.setYMax(10);

        RenderingTransformer transformer = new RenderingTransformer(state);

        // 0.0 should map to yMin
        assertEquals(0, transformer.fracYToDataY(0.0), 0.001);

        // 1.0 should map to yMax
        assertEquals(10, transformer.fracYToDataY(1.0), 0.001);

        // 0.5 should map to midpoint
        assertEquals(5, transformer.fracYToDataY(0.5), 0.001);
    }

    @Test
    public void testDataXToFracX() {
        RenderingState state = new RenderingState();
        state.setXMin(-50);
        state.setXMax(50);

        RenderingTransformer transformer = new RenderingTransformer(state);

        // xMin should map to 0.0
        assertEquals(0.0, transformer.dataXToFracX(-50), 0.001);

        // xMax should map to 1.0
        assertEquals(1.0, transformer.dataXToFracX(50), 0.001);

        // midpoint should map to 0.5
        assertEquals(0.5, transformer.dataXToFracX(0), 0.001);

        // Values outside range
        assertEquals(-0.5, transformer.dataXToFracX(-100), 0.001);
        assertEquals(1.5, transformer.dataXToFracX(100), 0.001);
    }

    @Test
    public void testDataYToFracY() {
        RenderingState state = new RenderingState();
        state.setYMin(10);
        state.setYMax(20);

        RenderingTransformer transformer = new RenderingTransformer(state);

        // yMin should map to 0.0
        assertEquals(0.0, transformer.dataYToFracY(10), 0.001);

        // yMax should map to 1.0
        assertEquals(1.0, transformer.dataYToFracY(20), 0.001);

        // midpoint should map to 0.5
        assertEquals(0.5, transformer.dataYToFracY(15), 0.001);
    }

    @Test
    public void testFromFracX() {
        // This combines fracXToDataX and fromDataX
        RenderingState state = new RenderingState();
        state.setScreenWidth(1000);
        state.setDataPaddingLeft(100);
        state.setDataPaddingRight(100);
        state.setXMin(0);
        state.setXMax(100);

        RenderingTransformer transformer = new RenderingTransformer(state);

        // 0.0 should map to left edge of data area (100)
        assertEquals(100, transformer.fromFracX(0.0), 0.001);

        // 1.0 should map to right edge of data area (900)
        assertEquals(900, transformer.fromFracX(1.0), 0.001);

        // 0.5 should map to center of data area (500)
        assertEquals(500, transformer.fromFracX(0.5), 0.001);
    }

    @Test
    public void testFromFracY_YInverted() {
        // This combines fracYToDataY and fromDataY with Y inverted
        RenderingState state = new RenderingState();
        state.setScreenHeight(800);
        state.setDataPaddingTop(100);
        state.setDataPaddingBottom(100);
        state.setYMin(0);
        state.setYMax(10);
        state.setYInverted(true);

        RenderingTransformer transformer = new RenderingTransformer(state);

        // 0.0 should map to bottom edge of data area (700)
        assertEquals(700, transformer.fromFracY(0.0), 0.001);

        // 1.0 should map to top edge of data area (100)
        assertEquals(100, transformer.fromFracY(1.0), 0.001);

        // 0.5 should map to center of data area (400)
        assertEquals(400, transformer.fromFracY(0.5), 0.001);
    }

    @Test
    public void testToFracX() {
        // This combines toDataX and dataXToFracX
        RenderingState state = new RenderingState();
        state.setScreenWidth(1000);
        state.setDataPaddingLeft(100);
        state.setDataPaddingRight(100);
        state.setXMin(0);
        state.setXMax(100);

        RenderingTransformer transformer = new RenderingTransformer(state);

        // At left edge of data area (100), should be 0.0
        assertEquals(0.0, transformer.toFracX(100), 0.001);

        // At right edge of data area (900), should be 1.0
        assertEquals(1.0, transformer.toFracX(900), 0.001);

        // At center of data area (500), should be 0.5
        assertEquals(0.5, transformer.toFracX(500), 0.001);
    }

    @Test
    public void testToFracY_YInverted() {
        // This combines toDataY and dataYToFracY with Y inverted
        RenderingState state = new RenderingState();
        state.setScreenHeight(800);
        state.setDataPaddingTop(100);
        state.setDataPaddingBottom(100);
        state.setYMin(0);
        state.setYMax(10);
        state.setYInverted(true);

        RenderingTransformer transformer = new RenderingTransformer(state);

        // At top edge of data area (100), should be 1.0
        assertEquals(1.0, transformer.toFracY(100), 0.001);

        // At bottom edge of data area (700), should be 0.0
        assertEquals(0.0, transformer.toFracY(700), 0.001);

        // At center of data area (400), should be 0.5
        assertEquals(0.5, transformer.toFracY(400), 0.001);
    }

    @Test
    public void testEdgeCasesWithNegativeRange() {
        // Test edge case where xMax < xMin or yMax < yMin
        RenderingState state = new RenderingState();
        state.setScreenWidth(800);
        state.setScreenHeight(600);
        state.setDataPaddingLeft(50);
        state.setDataPaddingRight(50);
        state.setDataPaddingTop(50);
        state.setDataPaddingBottom(50);
        state.setXMin(100);
        state.setXMax(0);  // Inverted X range
        state.setYMin(10);
        state.setYMax(0);  // Inverted Y range

        RenderingTransformer transformer = new RenderingTransformer(state);

        // X transformations with inverted range
        assertEquals(50, transformer.fromDataX(100), 0.001);
        assertEquals(750, transformer.fromDataX(0), 0.001);
        assertEquals(100, transformer.toDataX(50), 0.001);
        assertEquals(0, transformer.toDataX(750), 0.001);

        // Y transformations with inverted range (and Y-inversion)
        state.setYInverted(true);
        assertEquals(550, transformer.fromDataY(10), 0.001);
        assertEquals(50, transformer.fromDataY(0), 0.001);
        assertEquals(10, transformer.toDataY(550), 0.001);
        assertEquals(0, transformer.toDataY(50), 0.001);
    }

    @Test
    public void testRoundTrip() {
        // Test that converting from data to screen and back gives original value
        RenderingState state = new RenderingState();
        state.setScreenWidth(1000);
        state.setScreenHeight(800);
        state.setDataPaddingLeft(100);
        state.setDataPaddingRight(100);
        state.setDataPaddingTop(80);
        state.setDataPaddingBottom(80);
        state.setXMin(-10);
        state.setXMax(10);
        state.setYMin(-5);
        state.setYMax(5);

        RenderingTransformer transformer = new RenderingTransformer(state);

        // X round trip
        double originalX = 5.0;
        double screenX = transformer.fromDataX(originalX);
        double roundTripX = transformer.toDataX(screenX);
        assertEquals(originalX, roundTripX, 0.001);

        // Y round trip with Y-inversion
        state.setYInverted(true);
        double originalY = 2.5;
        double screenY = transformer.fromDataY(originalY);
        double roundTripY = transformer.toDataY(screenY);
        assertEquals(originalY, roundTripY, 0.001);

        // Y round trip without Y-inversion
        state.setYInverted(false);
        screenY = transformer.fromDataY(originalY);
        roundTripY = transformer.toDataY(screenY);
        assertEquals(originalY, roundTripY, 0.001);

        // Fractional coordinates round trip
        double fracX = 0.75;
        screenX = transformer.fromFracX(fracX);
        double roundTripFracX = transformer.toFracX(screenX);
        assertEquals(fracX, roundTripFracX, 0.001);

        double fracY = 0.25;
        screenY = transformer.fromFracY(fracY);
        double roundTripFracY = transformer.toFracY(screenY);
        assertEquals(fracY, roundTripFracY, 0.001);
    }

    @Test
    public void testClipLineCompletelyInside() {
        RenderingState state = new RenderingState();
        state.setXMin(0);
        state.setXMax(100);
        state.setYMin(0);
        state.setYMax(100);

        RenderingClipper clipper = new RenderingClipper(state);

        // Line completely inside the bounds
        Pair<Double, Double> a = Pair.of(25.0, 25.0);
        Pair<Double, Double> b = Pair.of(75.0, 75.0);

        Pair<Pair<Double, Double>, Pair<Double, Double>> result = clipper.clipLine(a, b);

        assertNotNull(result);
        assertEquals(25.0, result.getLeft().getLeft(), 0.001);
        assertEquals(25.0, result.getLeft().getRight(), 0.001);
        assertEquals(75.0, result.getRight().getLeft(), 0.001);
        assertEquals(75.0, result.getRight().getRight(), 0.001);
    }

    @Test
    public void testClipLineCompletelyOutside() {
        RenderingState state = new RenderingState();
        state.setXMin(0);
        state.setXMax(100);
        state.setYMin(0);
        state.setYMax(100);

        RenderingClipper clipper = new RenderingClipper(state);

        // Line completely outside the bounds (to the left)
        Pair<Double, Double> a = Pair.of(-20.0, 25.0);
        Pair<Double, Double> b = Pair.of(-10.0, 75.0);

        Pair<Pair<Double, Double>, Pair<Double, Double>> result = clipper.clipLine(a, b);

        assertNull(result);
    }

    @Test
    public void testClipLineHorizontalCrossing() {
        RenderingState state = new RenderingState();
        state.setXMin(0);
        state.setXMax(100);
        state.setYMin(0);
        state.setYMax(100);

        RenderingClipper clipper = new RenderingClipper(state);

        // Horizontal line crossing left boundary
        Pair<Double, Double> a = Pair.of(-50.0, 50.0);
        Pair<Double, Double> b = Pair.of(50.0, 50.0);

        Pair<Pair<Double, Double>, Pair<Double, Double>> result = clipper.clipLine(a, b);

        assertNotNull(result);
        assertEquals(0.0, result.getLeft().getLeft(), 0.001);
        assertEquals(50.0, result.getLeft().getRight(), 0.001);
        assertEquals(50.0, result.getRight().getLeft(), 0.001);
        assertEquals(50.0, result.getRight().getRight(), 0.001);
    }

    @Test
    public void testClipLineVerticalCrossing() {
        RenderingState state = new RenderingState();
        state.setXMin(0);
        state.setXMax(100);
        state.setYMin(0);
        state.setYMax(100);

        RenderingClipper clipper = new RenderingClipper(state);

        // Vertical line crossing bottom boundary
        Pair<Double, Double> a = Pair.of(50.0, -50.0);
        Pair<Double, Double> b = Pair.of(50.0, 150.0);

        Pair<Pair<Double, Double>, Pair<Double, Double>> result = clipper.clipLine(a, b);

        assertNotNull(result);
        assertEquals(50.0, result.getLeft().getLeft(), 0.001);
        assertEquals(0.0, result.getLeft().getRight(), 0.001);
        assertEquals(50.0, result.getRight().getLeft(), 0.001);
        assertEquals(100.0, result.getRight().getRight(), 0.001);
    }

    @Test
    public void testClipLineDiagonalCrossing() {
        RenderingState state = new RenderingState();
        state.setXMin(0);
        state.setXMax(100);
        state.setYMin(0);
        state.setYMax(100);

        RenderingClipper clipper = new RenderingClipper(state);

        // Diagonal line crossing two boundaries
        Pair<Double, Double> a = Pair.of(-50.0, -50.0);
        Pair<Double, Double> b = Pair.of(150.0, 150.0);

        Pair<Pair<Double, Double>, Pair<Double, Double>> result = clipper.clipLine(a, b);

        assertNotNull(result);
        assertEquals(0.0, result.getLeft().getLeft(), 0.001);
        assertEquals(0.0, result.getLeft().getRight(), 0.001);
        assertEquals(100.0, result.getRight().getLeft(), 0.001);
        assertEquals(100.0, result.getRight().getRight(), 0.001);
    }

    @Test
    public void testClipLineZeroLength() {
        RenderingState state = new RenderingState();
        state.setXMin(0);
        state.setXMax(100);
        state.setYMin(0);
        state.setYMax(100);

        RenderingClipper clipper = new RenderingClipper(state);

        // Zero-length line (a point)
        Pair<Double, Double> a = Pair.of(50.0, 50.0);
        Pair<Double, Double> b = Pair.of(50.0, 50.0);

        // Should return null for zero-length line
        Pair<Pair<Double, Double>, Pair<Double, Double>> result = clipper.clipLine(a, b);

        assertNull(result);
    }

    @Test
    public void testClipLineNearlyZeroLength() {
        RenderingState state = new RenderingState();
        state.setXMin(0);
        state.setXMax(100);
        state.setYMin(0);
        state.setYMax(100);

        RenderingClipper clipper = new RenderingClipper(state);

        // Nearly zero-length line (shorter than epsilon)
        Pair<Double, Double> a = Pair.of(50.0, 50.0);
        Pair<Double, Double> b = Pair.of(50.0000001, 50.0000001);

        // Should return null for nearly zero-length line
        Pair<Pair<Double, Double>, Pair<Double, Double>> result = clipper.clipLine(a, b);

        assertNull(result);
    }

    @Test
    public void testClipPolygonCompletelyInside() {
        RenderingState state = new RenderingState();
        state.setXMin(0);
        state.setXMax(100);
        state.setYMin(0);
        state.setYMax(100);

        RenderingClipper clipper = new RenderingClipper(state);

        // Square completely inside the bounds
        double[] x = {25, 75, 75, 25};
        double[] y = {25, 25, 75, 75};

        Pair<double[], double[]> result = clipper.clipPolygon(x, y);

        assertNotNull(result);
        double[] resultX = result.getLeft();
        double[] resultY = result.getRight();

        // Should maintain the same polygon
        assertEquals(4, resultX.length);
        assertEquals(4, resultY.length);

        // Check if all points are in the result (may be in different order)
        assertTrue(containsPoint(resultX, resultY, 25, 25));
        assertTrue(containsPoint(resultX, resultY, 75, 25));
        assertTrue(containsPoint(resultX, resultY, 75, 75));
        assertTrue(containsPoint(resultX, resultY, 25, 75));
    }

    @Test
    public void testClipPolygonCompletelyOutside() {
        RenderingState state = new RenderingState();
        state.setXMin(0);
        state.setXMax(100);
        state.setYMin(0);
        state.setYMax(100);

        RenderingClipper clipper = new RenderingClipper(state);

        // Square completely outside the bounds (to the left)
        double[] x = {-75, -25, -25, -75};
        double[] y = {25, 25, 75, 75};

        Pair<double[], double[]> result = clipper.clipPolygon(x, y);

        assertNotNull(result);
        double[] resultX = result.getLeft();
        double[] resultY = result.getRight();

        // Should return an empty polygon
        assertEquals(0, resultX.length);
        assertEquals(0, resultY.length);
    }

    @Test
    public void testClipPolygonPartiallyInside() {
        RenderingState state = new RenderingState();
        state.setXMin(0);
        state.setXMax(100);
        state.setYMin(0);
        state.setYMax(100);

        RenderingClipper clipper = new RenderingClipper(state);

        // Square partially inside the bounds (crossing left boundary)
        double[] x = {-25, 25, 25, -25};
        double[] y = {25, 25, 75, 75};

        Pair<double[], double[]> result = clipper.clipPolygon(x, y);

        assertNotNull(result);
        double[] resultX = result.getLeft();
        double[] resultY = result.getRight();

        // Result should be a clipped polygon
        assertTrue(resultX.length > 0);
        assertTrue(containsPoint(resultX, resultY, 0, 25));
        assertTrue(containsPoint(resultX, resultY, 0, 75));
        assertTrue(containsPoint(resultX, resultY, 25, 25));
        assertTrue(containsPoint(resultX, resultY, 25, 75));
    }

    @Test
    public void testSegmentLineIntersection() {
        // Test the segment-line intersection method using clipLine method indirectly
        RenderingState state = new RenderingState();
        state.setXMin(0);
        state.setXMax(100);
        state.setYMin(0);
        state.setYMax(100);

        RenderingClipper clipper = new RenderingClipper(state);

        // Diagonal line crossing the bottom boundary
        Pair<Double, Double> a = Pair.of(0.0, -25.0);
        Pair<Double, Double> b = Pair.of(100.0, 75.0);

        Pair<Pair<Double, Double>, Pair<Double, Double>> result = clipper.clipLine(a, b);

        assertNotNull(result);

        // Intersection with bottom boundary should be at (25, 0)
        assertEquals(25.0, result.getLeft().getLeft(), 0.001);
        assertEquals(0.0, result.getLeft().getRight(), 0.001);
    }

    @Test
    public void testWithCustomBounds() {
        RenderingState state = new RenderingState();
        state.setXMin(-10);
        state.setXMax(10);
        state.setYMin(-5);
        state.setYMax(5);

        RenderingClipper clipper = new RenderingClipper(state);

        // Line crossing the bounds in custom coordinate system
        Pair<Double, Double> a = Pair.of(-15.0, 0.0);
        Pair<Double, Double> b = Pair.of(15.0, 0.0);

        Pair<Pair<Double, Double>, Pair<Double, Double>> result = clipper.clipLine(a, b);

        assertNotNull(result);
        assertEquals(-10.0, result.getLeft().getLeft(), 0.001);
        assertEquals(0.0, result.getLeft().getRight(), 0.001);
        assertEquals(10.0, result.getRight().getLeft(), 0.001);
        assertEquals(0.0, result.getRight().getRight(), 0.001);
    }

    @Test
    public void testClipPolygonWithCustomBounds() {
        RenderingState state = new RenderingState();
        state.setXMin(-50);
        state.setXMax(50);
        state.setYMin(-50);
        state.setYMax(50);

        RenderingClipper clipper = new RenderingClipper(state);

        // Diamond that should be clipped to a square
        double[] x = {0, 100, 0, -100};
        double[] y = {-100, 0, 100, 0};

        Pair<double[], double[]> result = clipper.clipPolygon(x, y);

        assertNotNull(result);
        double[] resultX = result.getLeft();
        double[] resultY = result.getRight();

        System.out.println("X: " + Arrays.toString(resultX));
        System.out.println("Y: " + Arrays.toString(resultX));
        // Should clip to a square with corners at (+/-50, +/-50)
        assertTrue(containsPoint(resultX, resultY, 50, 50));
        assertTrue(containsPoint(resultX, resultY, -50, 50));
        assertTrue(containsPoint(resultX, resultY, 50, -50));
        assertTrue(containsPoint(resultX, resultY, -50, -50));
    }

    // Helper method to check if a point exists in the result arrays
    private boolean containsPoint(double[] x, double[] y, double pointX, double pointY) {
        for (int i = 0; i < x.length; i++) {
            if (Math.abs(x[i] - pointX) < 0.001 && Math.abs(y[i] - pointY) < 0.001) {
                return true;
            }
        }
        return false;
    }
}
