package io.github.software.coursework.data;

import java.io.IOException;

public class SyntaxException extends IOException {
    public SyntaxException(String message) {
        super(message);
    }

    public SyntaxException(String message, Throwable cause) {
        super(message, cause);
    }
}
