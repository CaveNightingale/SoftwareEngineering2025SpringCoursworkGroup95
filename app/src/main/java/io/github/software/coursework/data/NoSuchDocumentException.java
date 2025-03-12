package io.github.software.coursework.data;

public class NoSuchDocumentException extends RuntimeException {
    public NoSuchDocumentException(String message) {
        super(message);
    }

    public NoSuchDocumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
