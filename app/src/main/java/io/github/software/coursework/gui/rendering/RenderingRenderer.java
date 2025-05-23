package io.github.software.coursework.gui.rendering;

import com.google.common.annotations.VisibleForTesting;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.apache.commons.lang3.tuple.Pair;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.SequencedCollection;

import static io.github.software.coursework.gui.ChartRendering.ALIGN_END;
import static io.github.software.coursework.gui.ChartRendering.ALIGN_CENTER;
import static io.github.software.coursework.gui.ChartRendering.ALIGN_START;

@VisibleForTesting
public final class RenderingRenderer {
    private final RenderingState state;
    private final RenderingTransformer transformer;
    private final RenderingClipper clipper;
    private final GraphicsContext gc;

    public RenderingRenderer(RenderingState state, RenderingTransformer transformer, GraphicsContext gc) {
        this.state = state;
        this.transformer = transformer;
        this.clipper = new RenderingClipper(state);
        this.gc = gc;
    }

    public void save() {
        gc.save();
    }

    public void restore() {
        gc.restore();
    }

    private static String formatDouble(double value, int n) {
        DecimalFormat decimalFormat = new DecimalFormat("#." + "#".repeat(Math.max(0, n)));
        return decimalFormat.format(value);
    }

    private String formatXDouble(double value) {
        if (Math.abs(value) < state.getXEps()) {
            return "0";
        }
        double scale = Math.log10(state.getXMax() - state.getXMin());
        int reserved = (int) Math.ceil(-scale) + 3;
        return formatDouble(value, reserved);
    }

    private String formatYDouble(double value) {
        if (Math.abs(value) < state.getYEps()) {
            return "0";
        }
        double scale = Math.log10(state.getYMax() - state.getYMin());
        int reserved = (int) Math.ceil(-scale) + 3;
        return formatDouble(value, reserved);
    }

    public void clearRect(double x, double y, double width, double height) {
        gc.clearRect(x, y, width, height);
    }

    public void setStroke(Paint paint) {
        gc.setStroke(paint);
    }

    public void setFill(Paint paint) {
        gc.setFill(paint);
    }

    public void setLineDashes(double... dashes) {
        gc.setLineDashes(dashes);
    }

    public void drawText(String text, int alignX, int alignY, double screenX, double screenY) {
        Font font = gc.getFont();
        Text textNode = new Text(text);
        textNode.setFont(font);

        double textWidth = textNode.getLayoutBounds().getWidth();
        double textHeight = textNode.getLayoutBounds().getHeight();

        double x = switch (alignX) {
            case ALIGN_START -> 0;
            case ALIGN_CENTER -> -textWidth / 2;
            case ALIGN_END -> -textWidth;
            default -> throw new IllegalArgumentException("Invalid alignment: " + alignX);
        };

        double y = switch (alignY) {
            case ALIGN_START -> textHeight * 3 / 4;
            case ALIGN_CENTER -> textHeight / 4;
            case ALIGN_END -> -textHeight / 4;
            default -> throw new IllegalArgumentException("Invalid alignment: " + alignY);
        };
        gc.strokeText(text, x + screenX, y + screenY);
    }
    
    public void drawXAxis(Iterable<Pair<Double, String>> ticks) {
        save();
        gc.setLineWidth(1);
        // Draw X axis
        gc.strokeLine(
                transformer.fromFracX(0),
                transformer.fromFracY(0),
                transformer.fromFracX(1),
                transformer.fromFracY(0)
        );
        // Draw X axis ticks
        int textYAlignment = state.isYInverted() ? ALIGN_START : ALIGN_END;
        int sign = state.isYInverted() ? 1 : -1;
        for (Pair<Double, String> tick : ticks) {
            gc.strokeLine(transformer.fromDataX(tick.getKey()), transformer.fromFracY(0), transformer.fromDataX(tick.getKey()), transformer.fromFracY(0) + 5 * sign);
            drawText(tick.getValue(), ALIGN_CENTER, textYAlignment, transformer.fromDataX(tick.getKey()), transformer.fromFracY(0) + 7.5 * sign);
        }
        restore();
    }
    
    public void drawXAxis(double offset, double step) {
        ArrayList<Pair<Double, String>> ticks = new ArrayList<>();
        double start = Math.ceil((state.getXMin() - offset) / step) * step + offset;
        if (Math.abs(start - step - state.getXMin()) < state.getXEps()) {
            start -= step;
        }
        for (double i = start; i - state.getXMax() < state.getYEps(); i += step) {
            ticks.add(Pair.of(i, formatXDouble(i)));
        }
        drawXAxis(ticks);
    }
    
    public void drawXAxis(SequencedCollection<String> ticks) {
        ArrayList<Pair<Double, String>> ticks2 = new ArrayList<>();
        for (String tick : ticks) {
            ticks2.add(Pair.of((double) ticks2.size(), tick));
        }
        drawXAxis(ticks2);
    }
    
    public void drawYAxis(Iterable<Pair<Double, String>> ticks) {
        save();
        gc.setLineWidth(1);
        // Draw Y axis
        gc.strokeLine(
                transformer.fromFracX(0),
                transformer.fromFracY(0),
                transformer.fromFracX(0),
                transformer.fromFracY(1)
        );
        // Draw Y axis ticks
        for (Pair<Double, String> tick : ticks) {
            gc.strokeLine(transformer.fromFracX(0), transformer.fromDataY(tick.getKey()), transformer.fromFracX(0) - 5, transformer.fromDataY(tick.getKey()));
            drawText(tick.getValue(), ALIGN_END, ALIGN_CENTER, transformer.fromFracX(0) - 7.5, transformer.fromDataY(tick.getKey()));
        }
        restore();
    }
    
    public void drawYAxis(double offset, double step) {
        ArrayList<Pair<Double, String>> ticks = new ArrayList<>();
        double start = Math.ceil((state.getYMin() - offset) / step) * step + offset;
        if (Math.abs(start - step - state.getYMin()) < state.getYEps()) {
            start -= step;
        }
        for (double i = start; i - state.getYMax() < state.getYEps(); i += step) {
            ticks.add(Pair.of(i, formatYDouble(i)));
        }
        drawYAxis(ticks);
    }
    
    public void drawYAxis(SequencedCollection<String> ticks) {
        ArrayList<Pair<Double, String>> ticks2 = new ArrayList<>();
        for (String tick : ticks) {
            ticks2.add(Pair.of((double) ticks2.size(), tick));
        }
        drawYAxis(ticks2);
    }
    
    public void plot(double[] x, double[] y, Paint paint, double width) {
        if (x.length != y.length) {
            throw new IllegalArgumentException("x and y must have the same length");
        }
        save();
        gc.setStroke(paint);
        gc.setLineWidth(width);
        for (int i = 0; i < x.length - 1; i++) {
            Pair<Pair<Double, Double>, Pair<Double, Double>> clipped = clipper.clipLine(Pair.of(x[i], y[i]), Pair.of(x[i + 1], y[i + 1]));
            if (clipped == null) {
                continue;
            }
            gc.strokeLine(
                    transformer.fromDataX(clipped.getLeft().getLeft()),
                    transformer.fromDataY(clipped.getLeft().getRight()),
                    transformer.fromDataX(clipped.getRight().getLeft()),
                    transformer.fromDataY(clipped.getRight().getRight())
            );
        }
        restore();
    }
    
    public void scatter(double[] x, double[] y, Paint paint, double width) {
        if (x.length != y.length) {
            throw new IllegalArgumentException("x and y must have the same length");
        }
        save();
        gc.setStroke(paint);
        gc.setLineWidth(width);
        for (int i = 0; i < x.length; i++) {
            if (x[i] < state.getXMin() || x[i] > state.getXMax() || y[i] < state.getYMin() || y[i] > state.getYMax()) {
                continue;
            }
            gc.strokeOval(
                    transformer.fromDataX(x[i]) - width / 2,
                    transformer.fromDataY(y[i]) - width / 2,
                    width,
                    width
            );
        }
        restore();
    }
    
    public void fill(double[] x, double[] y, Paint paint) {
        if (x.length != y.length) {
            throw new IllegalArgumentException("x and y must have the same length");
        }
        save();
        Pair<double[], double[]> clipped = clipper.clipPolygon(x, y);
        x = clipped.getLeft();
        y = clipped.getRight();
        double[] screenX = new double[x.length];
        double[] screenY = new double[y.length];
        for (int i = 0; i < x.length; i++) {
            screenX[i] = transformer.fromDataX(x[i]);
            screenY[i] = transformer.fromDataY(y[i]);
        }
        gc.setFill(paint);
        gc.fillPolygon(screenX, screenY, x.length);
        restore();
    }
}
