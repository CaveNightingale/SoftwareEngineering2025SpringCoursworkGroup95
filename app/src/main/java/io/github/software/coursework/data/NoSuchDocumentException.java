package io.github.software.coursework.data;

/**
 * Exception thrown when a document is not found.
 */
public class NoSuchDocumentException extends RuntimeException {
    public NoSuchDocumentException(String message) {
        super(message);
    }

    public NoSuchDocumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
