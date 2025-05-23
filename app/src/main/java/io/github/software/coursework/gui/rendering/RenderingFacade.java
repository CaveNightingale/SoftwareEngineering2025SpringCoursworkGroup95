package io.github.software.coursework.gui.rendering;

import com.google.common.annotations.VisibleForTesting;
import io.github.software.coursework.gui.ChartRendering;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Paint;
import org.apache.commons.lang3.tuple.Pair;

import java.util.SequencedCollection;

public final class RenderingFacade implements ChartRendering {
    private final RenderingState state = new RenderingState();
    private final RenderingTransformer transformer = new RenderingTransformer(state);
    private final RenderingRenderer renderer;

    public RenderingFacade(Canvas canvas) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        this.renderer = new RenderingRenderer(state, transformer, gc);
    }

    public void setScreenWidth(double screenWidth) {
        state.setScreenWidth(screenWidth);
    }

    public void setScreenHeight(double screenHeight) {
        state.setScreenHeight(screenHeight);
    }

    @Override
    public double getScreenWidth() {
        return state.getScreenWidth();
    }

    @Override
    public double getScreenHeight() {
        return state.getScreenHeight();
    }

    @Override
    public void setYInverted(boolean invertY) {
        state.setYInverted(invertY);
    }

    @Override
    public void setXPadding(double left, double right) {
        state.setDataPaddingLeft(left);
        state.setDataPaddingRight(right);
    }

    @Override
    public void setYPadding(double top, double bottom) {
        state.setDataPaddingTop(top);
        state.setDataPaddingBottom(bottom);
    }

    @Override
    public void setXLimits(double left, double right) {
        state.setXMin(left);
        state.setXMax(right);
    }

    @Override
    public void setYLimits(double bottom, double top) {
        state.setYMin(bottom);
        state.setYMax(top);
    }

    @Override
    public void setXCategoricalLimit(int categoryCount) {
        setXLimits(-0.5, categoryCount - 0.5); // Translate to a 0.5 to center the category
    }

    @Override
    public void setYCategoricalLimit(int categoryCount) {
        setYLimits(-0.5, categoryCount - 0.5);
    }

    @Override
    public double fromDataX(double x) {
        return transformer.fromDataX(x);
    }

    @Override
    public double fromDataY(double y) {
        return transformer.fromDataY(y);
    }

    @Override
    public double toDataX(double x) {
        return transformer.toDataX(x);
    }

    // Notice the direction of the Y axis is inverted, to follow the convention of data visualization
    @Override
    public double toDataY(double y) {
        return transformer.toDataY(y);
    }

    @Override
    public double fracXToDataX(double x) {
        return transformer.fracXToDataX(x);
    }

    @Override
    public double fracYToDataY(double y) {
        return transformer.fracYToDataY(y);
    }

    @Override
    public double dataXToFracX(double x) {
        return transformer.dataXToFracX(x);
    }

    @Override
    public double dataYToFracY(double y) {
        return transformer.dataYToFracY(y);
    }

    @Override
    public double fromFracX(double x) {
        return transformer.fromFracX(x);
    }

    @Override
    public double fromFracY(double y) {
        return transformer.fromFracY(y);
    }

    @Override
    public double toFracX(double x) {
        return transformer.toFracX(x);
    }

    @Override
    public double toFracY(double y) {
        return transformer.toFracY(y);
    }

    @Override
    public void save() {
        renderer.save();
    }

    @Override
    public void restore() {
        renderer.restore();
    }

    @Override
    public void clearRect(double x, double y, double width, double height) {
        renderer.clearRect(x, y, width, height);
    }

    @Override
    public void setStroke(Paint paint) {
        renderer.setStroke(paint);
    }

    @Override
    public void setFill(Paint paint) {
        renderer.setFill(paint);
    }

    @Override
    public void setLineDashes(double... dashes) {
        renderer.setLineDashes(dashes);
    }

    @Override
    public void drawText(String text, int alignX, int alignY, double screenX, double screenY) {
        renderer.drawText(text, alignX, alignY, screenX, screenY);
    }

    @Override
    public void drawXAxis(Iterable<Pair<Double, String>> ticks) {
        renderer.drawXAxis(ticks);
    }

    @Override
    public void drawXAxis(SequencedCollection<String> ticks) {
        renderer.drawXAxis(ticks);
    }

    @Override
    public void drawYAxis(double offset, double step) {
        renderer.drawYAxis(offset, step);
    }

    @Override
    public void drawYAxis(SequencedCollection<String> ticks) {
        renderer.drawYAxis(ticks);
    }

    @Override
    public void plot(double[] x, double[] y, Paint paint, double width) {
        renderer.plot(x, y, paint, width);
    }

    @Override
    public void scatter(double[] x, double[] y, Paint paint, double width) {
        renderer.scatter(x, y, paint, width);
    }

    @Override
    public void fill(double[] x, double[] y, Paint paint) {
        renderer.fill(x, y, paint);
    }
}
