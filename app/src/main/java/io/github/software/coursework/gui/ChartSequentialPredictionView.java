package io.github.software.coursework.gui;


import com.google.common.primitives.Doubles;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.apache.commons.lang3.tuple.Pair;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.stream.IntStream;

public final class ChartSequentialPredictionView implements ChartController.View<ChartSequentialPredictionModel> {

    private double mouseX = Double.NaN;

    @Override
    public void onRender(ChartRendering rendering, ChartSequentialPredictionModel model) {
        double cent = 0.01;
        rendering.setYLimits(model.getBottom() * cent, model.getTop() * cent);
        rendering.setXLimits(0, model.getSequenceLength() - 1);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy/M/d");
        ArrayList<Pair<Double, String>> xTicks = new ArrayList<>();
        if (model.getSequenceLength() >= 10) {
            for (int i = 0; i <= 5; i++) {
                int idx = (i * (model.getSequenceLength() - 1)) / 5;
                long time = model.getStart() + idx * MainPage.DAY;
                String date = LocalDate.ofEpochDay(Math.ceilDiv(time, MainPage.DAY)).format(formatter);
                xTicks.add(Pair.of((double) idx, date));
            }
        } else {
            for (int i = 0; i < model.getSequenceLength(); i++) {
                long time = model.getStart() + i * MainPage.DAY;
                String date = LocalDate.ofEpochDay(Math.ceilDiv(time, MainPage.DAY)).format(formatter);
                xTicks.add(Pair.of((double) i, date));
            }
        }
        rendering.drawXAxis(xTicks);

        double scale = Math.abs((model.getTop() - model.getBottom()) * cent);
        if (Double.isFinite(model.getReference())) {
            rendering.drawYAxis(0, Math.max(Math.max(Math.abs(model.getReference() * cent) / 4, 0.2), scale / 8));
        } else {
            rendering.drawYAxis(0, scale / 4);
        }

        double[] trainX = IntStream.range(-1, model.getTrainingSamples().length()).asDoubleStream().toArray();
        double[] trainY = addReference(model.getTrainingSamples().stream().map(x -> x * cent).toArray(), 0);
        rendering.plot(trainX, trainY, Color.GREEN, 2);

        double[] predX= IntStream.range(model.getTrainingSamples().length() - 1, model.getSequenceLength()).asDoubleStream().toArray();
        double[] predY = addReference(model.getPredictedMean().stream().map(x -> x * cent).toArray(), trainY[trainY.length - 1]);
        rendering.plot(predX, predY, Color.BLUE, 2);

        double[] predLowerX = IntStream.range(model.getTrainingSamples().length() - 1, model.getSequenceLength()).asDoubleStream().toArray();
        double[] predLowerY = addReference(model.getPredictedLowerBound().stream().map(x -> x * cent).toArray(), trainY[trainY.length - 1]);
        double[] predUpperX = predLowerX.clone();
        double[] predUpperY = addReference(model.getPredictedUpperBound().stream().map(x -> x * cent).toArray(), trainY[trainY.length - 1]);
        Doubles.reverse(predLowerX);
        Doubles.reverse(predLowerY);
        double[] confidenceX = Doubles.concat(predUpperX, predLowerX);
        double[] confidenceY = Doubles.concat(predUpperY, predLowerY);
        rendering.fill(confidenceX, confidenceY, Color.BLUE.deriveColor(1, 1, 1, 0.25));

        if (Double.isFinite(model.getReference())) {
            double y = model.getReference() * cent;
            drawReference(rendering, rendering.fracXToDataX(0), y, rendering.fracXToDataX(1), y, Color.RED.deriveColor(1, 1, 1, 0.5));
            rendering.save();
            rendering.setStroke(Color.RED.deriveColor(1, 1, 1, 0.5));
            rendering.drawText(model.getReferenceName(), ChartRendering.ALIGN_START, ChartRendering.ALIGN_END, rendering.fromFracX(0) + 5, rendering.fromDataY(y) - 5);
            rendering.restore();
        }

        if (model.getToday() >= model.getStart() && model.getToday() <= model.getEnd()) {
            long x = (model.getToday() - model.getStart()) / MainPage.DAY;
            drawReference(rendering, x, model.getBottom() * cent, x, model.getTop() * cent, Color.DARKCYAN.deriveColor(1, 1, 1, 0.5));
            rendering.save();
            rendering.setStroke(Color.DARKCYAN.deriveColor(1, 1, 1, 0.5));
            rendering.drawText("Today", ChartRendering.ALIGN_CENTER, ChartRendering.ALIGN_END, rendering.fromDataX(x), rendering.fromDataY(model.getTop() * cent) - 5);
            rendering.restore();
        }

        if (Double.isFinite(mouseX)) {
            drawReference(rendering, mouseX, rendering.fracYToDataY(0), mouseX, rendering.fracYToDataY(1), Color.BLACK.deriveColor(1, 1, 1, 0.5));
            rendering.save();
            rendering.setStroke(Color.BLACK.deriveColor(1, 1, 1, 0.5));
            String date = LocalDate.ofEpochDay(model.getStart() / MainPage.DAY + (int) mouseX).format(formatter);
            rendering.drawText(date, ChartRendering.ALIGN_CENTER, ChartRendering.ALIGN_START, rendering.fromDataX(mouseX), rendering.fromDataY(0) + 7.5);
            rendering.restore();
            String text = getMouseHoverText(model);
            rendering.save();
            rendering.setStroke(Color.BLACK.deriveColor(1, 1, 1, 0.5));
            rendering.drawText(text, ChartRendering.ALIGN_CENTER, ChartRendering.ALIGN_END, rendering.fromDataX(mouseX), rendering.fromFracY(1) - 5);
            rendering.restore();
        }
    }

    @Override
    public void onHover(ChartRendering rendering, ChartSequentialPredictionModel model, double screenX, double screenY) {
        mouseX = Double.isFinite(screenX) ? Math.round(rendering.toDataX(screenX)) : Double.NaN;
        if (mouseX < 0 || mouseX >= model.getSequenceLength()) {
            mouseX = Double.NaN;
        }
        rendering.clearRect(0, 0, rendering.getScreenWidth(), rendering.getScreenHeight());
        onRender(rendering, model);
    }

    private void drawConfidenceInterval(ChartRendering rendering, ChartSequentialPredictionModel model, double base, double cent) {
        double[] predLowerX = generateXValues(model.getTrainingSamples().length() - 1, model.getSequenceLength());
        double[] predLowerY = addReference(model.getPredictedLowerBound().stream().map(x -> x * cent).toArray(), base);
        double[] predUpperX = predLowerX.clone();
        double[] predUpperY = addReference(model.getPredictedUpperBound().stream().map(x -> x * cent).toArray(), base);
        rendering.fill(
                combineArrays(predUpperX, reverseArray(predLowerX)),
                combineArrays(predUpperY, reverseArray(predLowerY)),
                Color.BLUE.deriveColor(1, 1, 1, 0.25));
    }

    private void drawMouseHover(ChartRendering rendering, ChartSequentialPredictionModel model, double cent) {
        rendering.setStroke(Color.BLACK.deriveColor(1, 1, 1, 0.5));
        String hoverText = getMouseHoverText(model);
        rendering.drawText(
                hoverText,
                ChartRendering.ALIGN_CENTER,
                ChartRendering.ALIGN_END,
                rendering.fromDataX(mouseX),
                rendering.fromFracY(1) - 5
        );
    }

    private String getMouseHoverText(ChartSequentialPredictionModel model) {
        int index = (int) mouseX;
        if (index < model.getTrainingSamples().length()) {
            return String.format("%.2f", model.getTrainingSamples().get(index));
        } else {
            double referenceValue = model.getTrainingSamples().get(model.getTrainingSamples().length() - 1);
            double value = model.getPredictedMean().get(index - model.getTrainingSamples().length()) + referenceValue;
            double upper = model.getPredictedUpperBound().get(index - model.getTrainingSamples().length()) + referenceValue;
            double lower = model.getPredictedLowerBound().get(index - model.getTrainingSamples().length()) + referenceValue;
            return String.format("%.2f / %.2f / %.2f", lower, value, upper);
        }
    }

    private double[] generateXValues(int start, int end) {
        return IntStream.range(start, end).asDoubleStream().toArray();
    }

    private double[] reverseArray(double[] array) {
        double[] reversed = array.clone();
        for (int i = 0, j = array.length - 1; i < j; i++, j--) {
            double temp = reversed[i];
            reversed[i] = reversed[j];
            reversed[j] = temp;
        }
        return reversed;
    }

    private double[] combineArrays(double[] array1, double[] array2) {
        double[] combined = new double[array1.length + array2.length];
        System.arraycopy(array1, 0, combined, 0, array1.length);
        System.arraycopy(array2, 0, combined, array1.length, array2.length);
        return combined;
    }

    private double[] addReference(double[] array, double reference) {
        double[] newArray = new double[array.length + 1];
        newArray[0] = reference;
        for (int i = 0; i < array.length; i++) {
            newArray[i + 1] = array[i] + reference;
        }
        return newArray;
    }

    private void drawReference(ChartRendering rendering, double x0, double y0, double x1, double y1, Paint paint) {
        rendering.save();
        rendering.setLineDashes(10, 5);
        rendering.plot(new double[]{x0, x1}, new double[]{y0, y1}, paint, 2);
        rendering.restore();
    }
}