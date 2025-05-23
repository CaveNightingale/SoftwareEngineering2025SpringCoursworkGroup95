package io.github.software.coursework.gui.rendering;

public final class RenderingState {
    // Screen size
    private double screenWidth;
    private double screenHeight;

    // The data coordinates
    private double dataPaddingLeft = 50;
    private double dataPaddingRight = 50;
    private double dataPaddingTop = 25;
    private double dataPaddingBottom = 25;
    private double xMin = 0;
    private double xMax = 1;
    private double yMin = 0;
    private double yMax = 1;
    private boolean invertY = true;

    public void setScreenWidth(double screenWidth) {
        this.screenWidth = screenWidth;
    }

    public void setScreenHeight(double screenHeight) {
        this.screenHeight = screenHeight;
    }

    public double getScreenWidth() {
        return screenWidth;
    }

    public double getScreenHeight() {
        return screenHeight;
    }

    public void setDataPaddingLeft(double dataPaddingLeft) {
        this.dataPaddingLeft = dataPaddingLeft;
    }

    public void setDataPaddingRight(double dataPaddingRight) {
        this.dataPaddingRight = dataPaddingRight;
    }

    public void setDataPaddingTop(double dataPaddingTop) {
        this.dataPaddingTop = dataPaddingTop;
    }

    public void setDataPaddingBottom(double dataPaddingBottom) {
        this.dataPaddingBottom = dataPaddingBottom;
    }

    public double getDataPaddingLeft() {
        return dataPaddingLeft;
    }

    public double getDataPaddingRight() {
        return dataPaddingRight;
    }

    public double getDataPaddingTop() {
        return dataPaddingTop;
    }

    public double getDataPaddingBottom() {
        return dataPaddingBottom;
    }

    public void setXMin(double xMin) {
        this.xMin = xMin;
    }

    public void setXMax(double xMax) {
        this.xMax = xMax;
    }

    public double getXMin() {
        return xMin;
    }

    public double getXMax() {
        return xMax;
    }

    public void setYMin(double yMin) {
        this.yMin = yMin;
    }

    public void setYMax(double yMax) {
        this.yMax = yMax;
    }

    public double getYMin() {
        return yMin;
    }

    public double getYMax() {
        return yMax;
    }

    public void setYInverted(boolean invertY) {
        this.invertY = invertY;
    }

    public boolean isYInverted() {
        return invertY;
    }

    public double getXEps() {
        return (getXMax() - getXMin()) / 1000;
    }

    public double getYEps() {
        return (getYMax() - getYMin()) / 1000;
    }
}
