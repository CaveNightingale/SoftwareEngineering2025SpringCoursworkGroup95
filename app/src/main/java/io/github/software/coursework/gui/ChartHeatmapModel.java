package io.github.software.coursework.gui;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.ImmutableDoubleArray;
import javafx.scene.paint.Color;

public final class ChartHeatmapModel {
    private final ImmutableList<String> xTicks;
    private final ImmutableList<String> yTicks;
    private final ImmutableList<ImmutableDoubleArray> data; // column major

    public ChartHeatmapModel(ImmutableList<String> xTicks, ImmutableList<String> yTicks, ImmutableList<ImmutableDoubleArray> data) {
        this.xTicks = xTicks;
        this.yTicks = yTicks;
        this.data = data;
    }

    public ImmutableList<String> getXTicks() {
        return xTicks;
    }

    public ImmutableList<String> getYTicks() {
        return yTicks;
    }

    public ImmutableList<ImmutableDoubleArray> getData() {
        return data;
    }
}
