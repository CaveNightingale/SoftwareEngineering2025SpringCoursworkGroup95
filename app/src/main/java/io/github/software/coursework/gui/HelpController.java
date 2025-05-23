package io.github.software.coursework.gui;

import javafx.fxml.FXML;
import javafx.scene.web.WebView;

public final class HelpController {
    @FXML
    private WebView root;

    private final HelpModel model = new HelpModel();

    @FXML
    public void initialize() {
        root.getEngine().loadContent(model.getHelpText());
    }
}
