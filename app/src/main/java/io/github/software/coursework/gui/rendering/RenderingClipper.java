package io.github.software.coursework.gui.rendering;

import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;

public final class RenderingClipper {
    private final RenderingState state;

    public RenderingClipper(RenderingState state) {
        this.state = state;
    }

    public @Nullable Pair<Pair<Double, Double>, Pair<Double, Double>> clipLine(Pair<Double, Double> a, Pair<Double, Double> b) {
        double x0 = a.getLeft();
        double y0 = a.getRight();
        double x1 = b.getLeft();
        double y1 = b.getRight();
        double dx = x1 - x0;
        double dy = y1 - y0;
        if (Math.abs(dx) < state.getXEps() && Math.abs(dy) < state.getYEps()) {
            return null;
        }
        if (x0 < state.getXMin()) {
            y0 += (state.getXMin() - x0) * dy / dx;
            x0 = state.getXMin();
        } else if (x0 > state.getXMax()) {
            y0 += (state.getXMax() - x0) * dy / dx;
            x0 = state.getXMax();
        }
        if (y0 < state.getYMin()) {
            x0 += (state.getYMin() - y0) * dx / dy;
            y0 = state.getYMin();
        } else if (y0 > state.getYMax()) {
            x0 += (state.getYMax() - y0) * dx / dy;
            y0 = state.getYMax();
        }
        if (x1 < state.getXMin()) {
            y1 += (state.getXMin() - x1) * dy / dx;
            x1 = state.getXMin();
        } else if (x1 > state.getXMax()) {
            y1 += (state.getXMax() - x1) * dy / dx;
            x1 = state.getXMax();
        }
        if (y1 < state.getYMin()) {
            x1 += (state.getYMin() - y1) * dx / dy;
            y1 = state.getYMin();
        } else if (y1 > state.getYMax()) {
            x1 += (state.getYMax() - y1) * dx / dy;
            y1 = state.getYMax();
        }
        if (x0 < state.getXMin() || x0 > state.getXMax() || y0 < state.getYMin() || y0 > state.getYMax() ||
                x1 < state.getXMin() || x1 > state.getXMax() || y1 < state.getYMin() || y1 > state.getYMax()) {
            return null;
        }
        return Pair.of(Pair.of(x0, y0), Pair.of(x1, y1));
    }

    /**
     * Calculate the intersection of a line segment and a line
     *
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

    public Pair<double[], double[]> clipPolygon(double[] x, double[] y) {
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
            if (a.getLeft() > state.getXMin()) {
                clipped.add(a);
            }
            Pair<Double, Double> c = segmentLineIntersection(a, b,
                    Pair.of(state.getXMin(), state.getYMin()), Pair.of(state.getXMin(), state.getYMax()));
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
            if (a.getLeft() < state.getXMax()) {
                clipped.add(a);
            }
            Pair<Double, Double> c = segmentLineIntersection(a, b,
                    Pair.of(state.getXMax(), state.getYMin()), Pair.of(state.getXMax(), state.getYMax()));
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
            if (a.getRight() < state.getYMax()) {
                clipped.add(a);
            }
            Pair<Double, Double> c = segmentLineIntersection(a, b,
                    Pair.of(state.getXMin(), state.getYMax()), Pair.of(state.getXMax(), state.getYMax()));
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
            if (a.getRight() > state.getYMin()) {
                clipped.add(a);
            }
            Pair<Double, Double> c = segmentLineIntersection(a, b,
                    Pair.of(state.getXMin(), state.getYMin()), Pair.of(state.getXMax(), state.getYMin()));
            if (c != null) {
                clipped.add(c);
            }
        }
        return clipped;
    }
}
