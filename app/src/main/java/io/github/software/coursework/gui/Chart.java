package io.github.software.coursework.gui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.SequencedCollection;

@SuppressWarnings("SuspiciousNameCombination")
public class Chart extends Region {
    private final Canvas canvas = new Canvas();
    public Chart() {
        getChildren().add(canvas);
        layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
            canvas.setWidth(newValue.getWidth());
            canvas.setHeight(newValue.getHeight());
            render();
        });
        canvas.setOnMouseMoved(event -> {
            Renderer renderer = getRenderer();
            if (renderer != null) {
                renderer.onHover(event.getX(), event.getY());
            }
        });
        canvas.setOnMouseEntered(event -> {
            Renderer renderer = getRenderer();
            if (renderer != null) {
                renderer.onHover(event.getX(), event.getY());
            }
        });
        canvas.setOnMouseExited(event -> {
            Renderer renderer = getRenderer();
            if (renderer != null) {
                renderer.onHover(Double.NaN, Double.NaN);
            }
        });

        rendererProperty().addListener((observable, oldValue, newValue) -> render());
    }

    private final SimpleObjectProperty<Renderer> renderer = new SimpleObjectProperty<>();
    public final ObjectProperty<Renderer> rendererProperty() {
        return renderer;
    }

    public final void setRenderer(Renderer onRender) {
        rendererProperty().set(onRender);
    }

    public final Renderer getRenderer() {
        return rendererProperty().get();
    }

    public void render() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        Renderer renderer = getRenderer();
        if (renderer != null) {
            renderer.setCanvas(canvas);
            renderer.setGraphicsContext(gc);
            renderer.setScreenWidth(canvas.getWidth());
            renderer.setScreenHeight(canvas.getHeight());
            renderer.render();
        }
    }

    public static abstract class Renderer {

        public abstract void render();

        private Canvas canvas;
        private GraphicsContext gc;
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

        public Canvas getCanvas() {
            return canvas;
        }

        public GraphicsContext getGraphicsContext() {
            return gc;
        }

        public double getScreenWidth() {
            return screenWidth;
        }

        public double getScreenHeight() {
            return screenHeight;
        }

        public void setCanvas(Canvas canvas) {
            this.canvas = canvas;
        }

        public void setGraphicsContext(GraphicsContext gc) {
            this.gc = gc;
        }

        public void setScreenWidth(double screenWidth) {
            this.screenWidth = screenWidth;
        }

        public void setScreenHeight(double screenHeight) {
            this.screenHeight = screenHeight;
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

        public void setXMin(double xMin) {
            this.xMin = xMin;
        }

        public void setXMax(double xMax) {
            this.xMax = xMax;
        }

        public void setYMin(double yMin) {
            this.yMin = yMin;
        }

        public void setYMax(double yMax) {
            this.yMax = yMax;
        }

        public boolean isYInverted() {
            return invertY;
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

        public double getXMin() {
            return xMin;
        }

        public double getXMax() {
            return xMax;
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

        public void setXPadding(double left, double right) {
            this.dataPaddingLeft = left;
            this.dataPaddingRight = right;
        }

        public void setYPadding(double top, double bottom) {
            this.dataPaddingTop = top;
            this.dataPaddingBottom = bottom;
        }

        public void setXLimits(double left, double right) {
            this.xMin = left;
            this.xMax = right;
        }

        public void setYLimits(double bottom, double top) {
            this.yMax = top;
            this.yMin = bottom;
        }

        public void setXCategoricalLimit(int categoryCount) {
            setXLimits(-0.5, categoryCount - 0.5); // Translate to a 0.5 to center the category
        }

        public void setYCategoricalLimit(int categoryCount) {
            setYLimits(-0.5, categoryCount - 0.5);
        }

        public double fromDataX(double x) {
            return (x - xMin) / (xMax - xMin) * (screenWidth - dataPaddingLeft - dataPaddingRight) + dataPaddingLeft;
        }

        public double fromDataY(double y) {
            double frac = (y - yMin) / (yMax - yMin);
            return (invertY ? 1 - frac : frac) * (screenHeight - dataPaddingTop - dataPaddingBottom) + dataPaddingTop;
        }

        public double toDataX(double x) {
            return (x - dataPaddingLeft) / (screenWidth - dataPaddingLeft - dataPaddingRight) * (xMax - xMin) + xMin;
        }

        // Notice the direction of the Y axis is inverted, to follow the convention of data visualization
        public double toDataY(double y) {
            double frac = (y - dataPaddingTop) / (screenHeight - dataPaddingTop - dataPaddingBottom);
            return (invertY ? 1 - frac : frac) * (yMax - yMin) + yMin;
        }

        public double fracXToDataX(double x) {
            return x * (xMax - xMin) + xMin;
        }

        public double fracYToDataY(double y) {
            return y * (yMax - yMin) + yMin;
        }

        public double dataXToFracX(double x) {
            return (x - xMin) / (xMax - xMin);
        }

        public double dataYToFracY(double y) {
            return (y - yMin) / (yMax - yMin);
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

        public static final int ALIGN_START = 1;
        public static final int ALIGN_CENTER = 2;
        public static final int ALIGN_END = 3;

        private static String formatDouble(double value, int n) {
            DecimalFormat decimalFormat = new DecimalFormat("#." + "#".repeat(Math.max(0, n)));
            return decimalFormat.format(value);
        }

        public double getXEps() {
            return (xMax - xMin) / 1000;
        }

        public double getYEps() {
            return (yMax - yMin) / 1000;
        }

        public void save() {
            gc.save();
        }

        public void restore() {
            gc.restore();
        }

        public String formatXDouble(double value) {
            if (Math.abs(value) < getXEps()) {
                return "0";
            }
            double scale = Math.log10(xMax - xMin);
            int reserved = (int) Math.ceil(-scale) + 3;
            return formatDouble(value, reserved);
        }

        public String formatYDouble(double value) {
            if (Math.abs(value) < getYEps()) {
                return "0";
            }
            double scale = Math.log10(yMax - yMin);
            int reserved = (int) Math.ceil(-scale) + 3;
            return formatDouble(value, reserved);
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
                    fromFracX(0),
                    fromFracY(0),
                    fromFracX(1),
                    fromFracY(0)
            );
            // Draw X axis ticks
            int textYAlignment = invertY ? ALIGN_START : ALIGN_END;
            int sign = invertY ? 1 : -1;
            for (Pair<Double, String> tick : ticks) {
                gc.strokeLine(fromDataX(tick.getKey()), fromFracY(0), fromDataX(tick.getKey()), fromFracY(0) + 5 * sign);
                drawText(tick.getValue(), ALIGN_CENTER, textYAlignment, fromDataX(tick.getKey()), fromFracY(0) + 7.5 * sign);
            }
            restore();
        }

        public void drawXAxis(double offset, double step) {
            ArrayList<Pair<Double, String>> ticks = new ArrayList<>();
            double start = Math.ceil((xMin - offset) / step) * step + offset;
            if (Math.abs(start - step - xMin) < getXEps()) {
                start -= step;
            }
            for (double i = start; i - xMax < getYEps(); i += step) {
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
                    fromFracX(0),
                    fromFracY(0),
                    fromFracX(0),
                    fromFracY(1)
            );
            // Draw Y axis ticks
            for (Pair<Double, String> tick : ticks) {
                gc.strokeLine(fromFracX(0), fromDataY(tick.getKey()), fromFracX(0) - 5, fromDataY(tick.getKey()));
                drawText(tick.getValue(), ALIGN_END, ALIGN_CENTER, fromFracX(0) - 7.5, fromDataY(tick.getKey()));
            }
            restore();
        }

        public void drawYAxis(double offset, double step) {
            ArrayList<Pair<Double, String>> ticks = new ArrayList<>();
            double start = Math.ceil((yMin - offset) / step) * step + offset;
            if (Math.abs(start - step - yMin) < getYEps()) {
                start -= step;
            }
            for (double i = start; i - yMax < getYEps(); i += step) {
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

        private @Nullable Pair<Pair<Double, Double>, Pair<Double, Double>> clipLine(Pair<Double, Double> a, Pair<Double, Double> b) {
            double x0 = a.getLeft();
            double y0 = a.getRight();
            double x1 = b.getLeft();
            double y1 = b.getRight();
            double dx = x1 - x0;
            double dy = y1 - y0;
            if (Math.abs(dx) < getXEps() && Math.abs(dy) < getYEps()) {
                return null;
            }
            if (x0 < xMin) {
                y0 += (xMin - x0) * dy / dx;
                x0 = xMin;
            } else if (x0 > xMax) {
                y0 += (xMax - x0) * dy / dx;
                x0 = xMax;
            }
            if (y0 < yMin) {
                x0 += (yMin - y0) * dx / dy;
                y0 = yMin;
            } else if (y0 > yMax) {
                x0 += (yMax - y0) * dx / dy;
                y0 = yMax;
            }
            if (x1 < xMin) {
                y1 += (xMin - x1) * dy / dx;
                x1 = xMin;
            } else if (x1 > xMax) {
                y1 += (xMax - x1) * dy / dx;
                x1 = xMax;
            }
            if (y1 < yMin) {
                x1 += (yMin - y1) * dx / dy;
                y1 = yMin;
            } else if (y1 > yMax) {
                x1 += (yMax - y1) * dx / dy;
                y1 = yMax;
            }
            if (x0 < xMin || x0 > xMax || y0 < yMin || y0 > yMax ||
                    x1 < xMin || x1 > xMax || y1 < yMin || y1 > yMax) {
                return null;
            }
            return Pair.of(Pair.of(x0, y0), Pair.of(x1, y1));
        }

        public void plot(double[] x, double[] y, Paint paint, double width) {
            if (x.length != y.length) {
                throw new IllegalArgumentException("x and y must have the same length");
            }
            save();
            gc.setStroke(paint);
            gc.setLineWidth(width);
            for (int i = 0; i < x.length - 1; i++) {
                Pair<Pair<Double, Double>, Pair<Double, Double>> clipped = clipLine(Pair.of(x[i], y[i]), Pair.of(x[i + 1], y[i + 1]));
                if (clipped == null) {
                    continue;
                }
                gc.strokeLine(
                        fromDataX(clipped.getLeft().getLeft()),
                        fromDataY(clipped.getLeft().getRight()),
                        fromDataX(clipped.getRight().getLeft()),
                        fromDataY(clipped.getRight().getRight())
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
                if (x[i] < xMin || x[i] > xMax || y[i] < yMin || y[i] > yMax) {
                    continue;
                }
                gc.strokeOval(
                        fromDataX(x[i]) - width / 2,
                        fromDataY(y[i]) - width / 2,
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
            Pair<double[], double[]> clipped = clipPolygon(x, y);
            x = clipped.getLeft();
            y = clipped.getRight();
            double[] screenX = new double[x.length];
            double[] screenY = new double[y.length];
            for (int i = 0; i < x.length; i++) {
                screenX[i] = fromDataX(x[i]);
                screenY[i] = fromDataY(y[i]);
            }
            gc.setFill(paint);
            gc.fillPolygon(screenX, screenY, x.length);
            restore();
        }

        /**
         * Calculate the intersection of a line segment and a line
         * @param a the first point of the line segment
         * @param b the second point of the line segment
         * @param c the first point of the line
         * @param d the second point of the line
         * @return the intersection point, or null if there is no intersection
         */
        private static @Nullable Pair<Double, Double> segmentLineIntersection(Pair<Double, Double> a, Pair<Double, Double> b, Pair<Double, Double> c, Pair<Double, Double> d) {
            double x1 = a.getLeft();
            double y1 = a.getRight();
            double x2 = b.getLeft();
            double y2 = b.getRight();
            double x3 = c.getLeft();
            double y3 = c.getRight();
            double x4 = d.getLeft();
            double y4 = d.getRight();

            double denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
            if (Math.abs(denom) < 1e-10) {
                return null;
            }
            double t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denom;
            if (t < 0 || t > 1) {
                return null;
            }
            return Pair.of(x1 + t * (x2 - x1), y1 + t * (y2 - y1));
        }
        
        private Pair<double[],double[]> clipPolygon(double[] x, double[] y) {
            ArrayList<Pair<Double, Double>> points = new ArrayList<>();
            for (int i = 0; i < x.length; i++) {
                points.add(Pair.of(x[i], y[i]));
            }
            points = clipLeft(points);
            points = clipRight(points);
            points = clipTop(points);
            points = clipBottom(points);
            double[] clippedX = new double[points.size()];
            double[] clippedY = new double[points.size()];
            for (int i = 0; i < points.size(); i++) {
                clippedX[i] = points.get(i).getLeft();
                clippedY[i] = points.get(i).getRight();
            }
            return Pair.of(clippedX, clippedY);
        }

        private ArrayList<Pair<Double, Double>> clipLeft(ArrayList<Pair<Double, Double>> list) {
            ArrayList<Pair<Double, Double>> clipped = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                Pair<Double, Double> a = list.get(i);
                Pair<Double, Double> b = list.get((i + 1) % list.size());
                if (a.getLeft() > xMin) {
                    clipped.add(a);
                }
                Pair<Double, Double> c = segmentLineIntersection(a, b,
                        Pair.of(xMin, yMin), Pair.of(xMin, yMax));
                if (c != null) {
                    clipped.add(c);
                }
            }
            return clipped;
        }

        private ArrayList<Pair<Double, Double>> clipRight(ArrayList<Pair<Double, Double>> list) {
            ArrayList<Pair<Double, Double>> clipped = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                Pair<Double, Double> a = list.get(i);
                Pair<Double, Double> b = list.get((i + 1) % list.size());
                if (a.getLeft() < xMax) {
                    clipped.add(a);
                }
                Pair<Double, Double> c = segmentLineIntersection(a, b,
                        Pair.of(xMax, yMin), Pair.of(xMax, yMax));
                if (c != null) {
                    clipped.add(c);
                }
            }
            return clipped;
        }

        private ArrayList<Pair<Double, Double>> clipTop(ArrayList<Pair<Double, Double>> list) {
            ArrayList<Pair<Double, Double>> clipped = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                Pair<Double, Double> a = list.get(i);
                Pair<Double, Double> b = list.get((i + 1) % list.size());
                if (a.getRight() < yMax) {
                    clipped.add(a);
                }
                Pair<Double, Double> c = segmentLineIntersection(a, b,
                        Pair.of(xMin, yMax), Pair.of(xMax, yMax));
                if (c != null) {
                    clipped.add(c);
                }
            }
            return clipped;
        }

        private ArrayList<Pair<Double, Double>> clipBottom(ArrayList<Pair<Double, Double>> list) {
            ArrayList<Pair<Double, Double>> clipped = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                Pair<Double, Double> a = list.get(i);
                Pair<Double, Double> b = list.get((i + 1) % list.size());
                if (a.getRight() > yMin) {
                    clipped.add(a);
                }
                Pair<Double, Double> c = segmentLineIntersection(a, b,
                        Pair.of(xMin, yMin), Pair.of(xMax, yMin));
                if (c != null) {
                    clipped.add(c);
                }
            }
            return clipped;
        }

        public void onHover(double screenX, double screenY) {
            // Override this method to handle hover events
        }
    }
}
