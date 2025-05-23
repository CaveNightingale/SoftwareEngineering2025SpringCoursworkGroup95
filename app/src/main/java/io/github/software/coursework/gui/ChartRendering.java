package io.github.software.coursework.gui;

import javafx.scene.paint.Paint;
import org.apache.commons.lang3.tuple.Pair;

import java.util.SequencedCollection;

/**
 * An interface allow rendering item on a canvas.
 * This is a flattened facade of the rendering system.
 * We have 3 coordinate systems:
 * 1. Data coordinate system: the coordinate system of the data points.
 * 2. Screen coordinate system: the coordinate system of the screen in pixels.
 * 3. Fractional coordinate system: the min-max normalized coordinate system of the data.
 */
public interface ChartRendering {
    int ALIGN_START = 1;
    int ALIGN_CENTER = 2;
    int ALIGN_END = 3;

    /**
     * Get the width of the canvas in the screen coordinate system.
     * @return the width of the canvas in the screen coordinate system in pixels
     */
    double getScreenWidth();

    /**
     * Get the height of the canvas in the screen coordinate system.
     * @return the height of the canvas in the screen coordinate system in pixels
     */
    double getScreenHeight();

    /**
     * Set if the Y axis is inverted. Not inverted means the Y axis is increasing from bottom to top.
     * @param invertY true if the Y axis is inverted, false otherwise
     */
    void setYInverted(boolean invertY);

    /**
     * Set the padding of the X axis in screen coordinate system.
     * @param left the padding on the left side of the X axis
     * @param right the padding on the right side of the X axis
     */
    void setXPadding(double left, double right);

    /**
     * Set the padding of the Y axis in screen coordinate system.
     * @param top the padding on the top side of the Y axis
     * @param bottom the padding on the bottom side of the Y axis
     */
    void setYPadding(double top, double bottom);

    /**
     * Set the limits of the X axis in data coordinate system. Data points outside this range will be clipped.
     * @param left the left limit of the X axis in data coordinate system
     * @param right the right limit of the X axis in data coordinate system
     */
    void setXLimits(double left, double right);

    /**
     * Set the limits of the Y axis in data coordinate system. Data points outside this range will be clipped.
     * @param bottom the bottom limit of the Y axis in data coordinate system
     * @param top the top limit of the Y axis in data coordinate system
     */
    void setYLimits(double bottom, double top);

    /**
     * Set the limits of the X axis in data coordinate system. Data points outside this range will be clipped.
     * We will make sure that data points with X in [0, categoryCount - 1] will be placed at the center of the chart.
     * @param categoryCount the number of categories in the X axis
     */
    void setXCategoricalLimit(int categoryCount);

    /**
     * Set the limits of the Y axis in data coordinate system. Data points outside this range will be clipped.
     * @param categoryCount the number of categories in the Y axis
     */
    void setYCategoricalLimit(int categoryCount);

    /**
     * Convert a data coordinate to a screen coordinate.
     * @param x the X coordinate in data coordinate system
     * @return the X coordinate in screen coordinate system
     */
    double fromDataX(double x);

    /**
     * Convert a data coordinate to a screen coordinate.
     * @param y the Y coordinate in data coordinate system
     * @return the Y coordinate in screen coordinate system
     */
    double fromDataY(double y);

    /**
     * Convert a screen coordinate to a data coordinate.
     * @param x the X coordinate in screen coordinate system
     * @return the X coordinate in data coordinate system
     */
    double toDataX(double x);

    /**
     * Convert a screen coordinate to a data coordinate.
     * @param y the Y coordinate in screen coordinate system
     * @return the Y coordinate in data coordinate system
     */
    double toDataY(double y);

    /**
     * Convert a fractional coordinate to a data coordinate.
     * @param x the X coordinate in fractional coordinate system
     * @return the X coordinate in data coordinate system
     */
    double fracXToDataX(double x);

    /**
     * Convert a fractional coordinate to a data coordinate.
     * @param y the Y coordinate in fractional coordinate system
     * @return the Y coordinate in data coordinate system
     */
    double fracYToDataY(double y);

    /**
     * Convert a data coordinate to a fractional coordinate.
     * @param x the X coordinate in data coordinate system
     * @return the X coordinate in fractional coordinate system
     */
    double dataXToFracX(double x);

    /**
     * Convert a data coordinate to a fractional coordinate.
     * @param y the Y coordinate in data coordinate system
     * @return the Y coordinate in fractional coordinate system
     */
    double dataYToFracY(double y);

    /**
     * Convert a fractional coordinate to a screen coordinate.
     * @param x the X coordinate in fractional coordinate system
     * @return the X coordinate in screen coordinate system
     */
    double fromFracX(double x);

    /**
     * Convert a fractional coordinate to a screen coordinate.
     * @param y the Y coordinate in fractional coordinate system
     * @return the Y coordinate in screen coordinate system
     */
    double fromFracY(double y);

    /**
     * Convert a screen coordinate to a fractional coordinate.
     * @param x the X coordinate in screen coordinate system
     * @return the X coordinate in fractional coordinate system
     */
    double toFracX(double x);

    /**
     * Convert a screen coordinate to a fractional coordinate.
     * @param y the Y coordinate in screen coordinate system
     * @return the Y coordinate in fractional coordinate system
     */
    double toFracY(double y);;

    /**
     * Save the current state of the rendering context.
     */
    void save();

    /**
     * Restore the last saved state of the rendering context.
     */
    void restore();

    /**
     * Clear an area of the canvas.
     * @param x the X coordinate of the top left corner of the area to clear (screen coordinate system)
     * @param y the Y coordinate of the top left corner of the area to clear (screen coordinate system)
     * @param width the width of the area to clear (screen coordinate system)
     * @param height the height of the area to clear (screen coordinate system)
     */
    void clearRect(double x, double y, double width, double height);

    /**
     * Set the stroke color of the rendering context.
     * @param paint the color to set
     */
    void setStroke(Paint paint);

    /**
     * Set the fill color of the rendering context.
     * @param paint the color to set
     */
    void setFill(Paint paint);

    /**
     * Set the line dashes of the rendering context.
     * @param dashes the dashes to set
     */
    void setLineDashes(double... dashes);

    /**
     * Draw a line from (x0, y0) to (x1, y1).
     * @param text the text to draw
     * @param alignX the alignment of the text in the X direction (i.e. one of ALIGN_START, ALIGN_CENTER, ALIGN_END)
     * @param alignY the alignment of the text in the Y direction (i.e. one of ALIGN_START, ALIGN_CENTER, ALIGN_END)
     * @param screenX the X coordinate of the text in screen coordinate system
     * @param screenY the Y coordinate of the text in screen coordinate system
     */
    void drawText(String text, int alignX, int alignY, double screenX, double screenY);

    /**
     * Draw the X axis with ticks.
     * @param ticks the ticks to draw
     */
    void drawXAxis(Iterable<Pair<Double, String>> ticks);

    /**
     * Draw the X axis with ticks.
     * @param ticks the ticks to draw
     */
    void drawXAxis(SequencedCollection<String> ticks);

    /**
     * Draw the Y axis with ticks.
     * @param offset the offset of the Y axis in data coordinate system
     * @param step the step of the Y axis in data coordinate system
     */
    void drawYAxis(double offset, double step);

    /**
     * Draw the Y axis with ticks.
     * @param ticks the ticks to draw
     */
    void drawYAxis(SequencedCollection<String> ticks);

    /**
     * Plot a curve. Content falling out of the X and Y limits will be clipped.
     * @param x the X coordinates of the points to plot
     * @param y the Y coordinates of the points to plot
     * @param paint the color to plot
     * @param width the width of the line to plot
     */
    void plot(double[] x, double[] y, Paint paint, double width);

    /**
     * Scatter points. Content falling out of the X and Y limits will be clipped.
     * @param x the X coordinates of the points to scatter
     * @param y the Y coordinates of the points to scatter
     * @param paint the color to scatter
     * @param width the width of the line to scatter
     */
    void scatter(double[] x, double[] y, Paint paint, double width);

    /**
     * Fill a polygon. Content falling out of the X and Y limits will be clipped.
     * @param x the X coordinates of the points to fill
     * @param y the Y coordinates of the points to fill
     * @param paint the color to fill
     */
    void fill(double[] x, double[] y, Paint paint);
}
