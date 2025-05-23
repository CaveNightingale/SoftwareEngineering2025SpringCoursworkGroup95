package io.github.software.coursework.gui;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

final class MailLauncher {
    public static void openMailClient(String to, String subject, String body) {
        if (!Desktop.isDesktopSupported()) {
            System.err.println("Desktop is not supported on this system.");
            return;
        }

        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.MAIL)) {
            System.err.println("MAIL action is not supported on this system.");
            return;
        }

        try {
            // 构造 mailto URI，注意要进行 URL 编码处理
            String uriStr = String.format("mailto:%s?subject=%s&body=%s",
                    encode(to), encode(subject), encode(body));
            URI mailto = new URI(uriStr);
            desktop.mail(mailto);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private static String encode(String value) {
        return value.replace(" ", "%20").replace("\n", "%0A").replace("&", "%26");
        // 更严格可以用 URLEncoder.encode(value, StandardCharsets.UTF_8) 但会编码过度
    }
}
