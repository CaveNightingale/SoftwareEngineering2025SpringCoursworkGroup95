package io.github.software.coursework.gui;

import com.google.common.primitives.ImmutableDoubleArray;
import javafx.scene.paint.Color;

public final class ChartHeatmapView implements ChartController.View<ChartHeatmapModel> {

    private Color color(double percent) {
        if (!Double.isFinite(percent)) {
            return Color.LIGHTGRAY;
        }
        return percent > 0 ? Color.WHITE.interpolate(Color.RED, percent) : Color.WHITE.interpolate(Color.GREEN, -percent);
    }

    private double[] normalize(ImmutableDoubleArray data) {
        double[] normalized = new double[data.length()];
        double max = 0;
        double min = 0;
        for (int i = 0; i < data.length(); i++) {
            if (!Double.isFinite(data.get(i))) {
                normalized[i] = 0;
                continue;
            }
            max = Math.max(max, data.get(i));
            min = Math.min(min, data.get(i));
        }
        double scale = Math.max(Math.max(max, -min), 1e-3);
        for (int i = 0; i < data.length(); i++) {
            normalized[i] = data.get(i) / scale;
        }
        return normalized;
    }

    @Override
    public void onRender(ChartRendering rendering, ChartHeatmapModel model) {
        rendering.setXPadding(120, 50);
        rendering.setYInverted(false);
        rendering.setXCategoricalLimit(model.getXTicks().size());
        rendering.setYCategoricalLimit(model.getYTicks().size());
        for (int i = 0; i < model.getData().size(); i++) {
            ImmutableDoubleArray column = model.getData().get(i);
            double[] normalized = normalize(column);
            for (int j = 0; j < column.length(); j++) {
                rendering.fill(
                        new double[]{i - 0.5, i - 0.5, i + 0.5, i + 0.5},
                        new double[]{j - 0.5, j + 0.5, j + 0.5, j - 0.5},
                        color(normalized[j]).deriveColor(1, 0.75, 1, 0.5)
                );
                String text = Double.isFinite(column.get(j)) ? String.format("%.2f", column.get(j)) : "N/A";
                rendering.drawText(text, ChartRendering.ALIGN_CENTER, ChartRendering.ALIGN_CENTER, rendering.fromDataX(i), rendering.fromDataY(j));
            }
        }
        rendering.drawXAxis(model.getXTicks());
        rendering.drawYAxis(model.getYTicks());
    }

    @Override
    public void onHover(ChartRendering rendering, ChartHeatmapModel model, double screenX, double screenY) {
    }
}
