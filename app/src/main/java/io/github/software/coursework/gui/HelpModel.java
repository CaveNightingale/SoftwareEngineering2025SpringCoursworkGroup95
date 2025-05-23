package io.github.software.coursework.gui;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class HelpModel {

    private String toHtml(String markdown) {
        Parser parser = Parser.builder().build();
        org.commonmark.node.Node document = parser.parse(markdown);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        String html = renderer.render(document);

        return "<html><head><style>"
                + "body { font-family: sans-serif; margin: 10px; line-height: 1.5; }"
                + "h1, h2, h3 { color: #333; }"
                + "code { background: #f0f0f0; padding: 2px 5px; border-radius: 3px; }"
                + "pre { background: #f8f8f8; padding: 10px; border-radius: 5px; }"
                + "a { color: #0066cc; text-decoration: none; }"
                + "</style></head><body>" + html + "</body></html>";
    }

    public String getHelpText() {
        try {
            return toHtml(Files.readString(Path.of(Objects.requireNonNull(HelpModel.class.getResource("_help.md")).toURI())));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
