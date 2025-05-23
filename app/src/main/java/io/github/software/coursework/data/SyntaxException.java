package io.github.software.coursework.data;

import java.io.IOException;

/**
 * Exception thrown when a document is malformed (i.e. has invalid syntax).
 */
public class SyntaxException extends IOException {
    public SyntaxException(String message) {
        super(message);
    }

    public SyntaxException(String message, Throwable cause) {
        super(message, cause);
    }
}
