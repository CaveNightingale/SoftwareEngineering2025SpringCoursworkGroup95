package io.github.software.coursework.gui.rendering;

public final class RenderingTransformer {
    private final RenderingState state;
    public RenderingTransformer(RenderingState state) {
        this.state = state;
    }

    public double fromDataX(double x) {
        return (x - state.getXMin()) / (state.getXMax() - state.getXMin()) * (state.getScreenWidth() - state.getDataPaddingLeft() - state.getDataPaddingRight()) + state.getDataPaddingLeft();
    }

    public double fromDataY(double y) {
        double frac = (y - state.getYMin()) / (state.getYMax() - state.getYMin());
        return (state.isYInverted() ? 1 - frac : frac) * (state.getScreenHeight() - state.getDataPaddingTop() - state.getDataPaddingBottom()) + state.getDataPaddingTop();
    }

    public double toDataX(double x) {
        return (x - state.getDataPaddingLeft()) / (state.getScreenWidth() - state.getDataPaddingLeft() - state.getDataPaddingRight()) * (state.getXMax() - state.getXMin()) + state.getXMin();
    }

    // Notice the direction of the Y axis is inverted, to follow the convention of data visualization
    public double toDataY(double y) {
        double frac = (y - state.getDataPaddingTop()) / (state.getScreenHeight() - state.getDataPaddingTop() - state.getDataPaddingBottom());
        return (state.isYInverted() ? 1 - frac : frac) * (state.getYMax() - state.getYMin()) + state.getYMin();
    }

    public double fracXToDataX(double x) {
        return x * (state.getXMax() - state.getXMin()) + state.getXMin();
    }

    public double fracYToDataY(double y) {
        return y * (state.getYMax() - state.getYMin()) + state.getYMin();
    }

    public double dataXToFracX(double x) {
        return (x - state.getXMin()) / (state.getXMax() - state.getXMin());
    }

    public double dataYToFracY(double y) {
        return (y - state.getYMin()) / (state.getYMax() - state.getYMin());
    }

    public double fromFracX(double x) {
        return fromDataX(fracXToDataX(x));
    }

    public double fromFracY(double y) {
        return fromDataY(fracYToDataY(y));
    }

    public double toFracX(double x) {
        return dataXToFracX(toDataX(x));
    }

    public double toFracY(double y) {
        return dataYToFracY(toDataY(y));
    }
}
