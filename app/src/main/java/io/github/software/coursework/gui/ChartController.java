package io.github.software.coursework.gui;

import io.github.software.coursework.gui.rendering.RenderingFacade;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;

public final class ChartController<T> {
    public interface View<T> {
        void onRender(ChartRendering rendering, T model);
        void onHover(ChartRendering rendering, T model, double x, double y);
    }

    @FXML
    private Canvas canvas;

    @FXML
    private AnchorPane root;

    private RenderingFacade rendering;

    private final DoubleProperty canvasWidth = new SimpleDoubleProperty();
    private final DoubleProperty canvasHeight = new SimpleDoubleProperty();

    private T model;
    private View<T> view;

    public void setModel(T model) {
        this.model = model;
        render();
    }

    public void setView(View<T> view) {
        this.view = view;
        render();
    }

    @FXML
    public void initialize() {
        canvasWidth.bind(root.widthProperty());
        canvasHeight.bind(root.heightProperty());
        canvas.widthProperty().bind(canvasWidth);
        canvas.heightProperty().bind(canvasHeight);

        canvasWidth.addListener((observable, oldValue, newValue) -> render());
        canvasHeight.addListener((observable, oldValue, newValue) -> render());

        canvas.setOnMouseMoved(this::handleMouseHover);
        canvas.setOnMouseEntered(this::handleMouseHover);
        canvas.setOnMouseExited(this::handleMouseLeave);

        rendering = new RenderingFacade(canvas);
    }

    private void handleMouseHover(MouseEvent event) {
        if (view != null && model != null) {
            view.onHover(rendering, model, event.getX(), event.getY());
        }
    }

    private void handleMouseLeave(MouseEvent event) {
        if (view != null && model != null) {
            view.onHover(rendering, model, Double.NaN, Double.NaN);
        }
    }

    public void render() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        rendering.setScreenWidth(canvas.getWidth());
        rendering.setScreenHeight(canvas.getHeight());

        if (view != null && model != null) {
            view.onRender(rendering, model);
        }
    }

}