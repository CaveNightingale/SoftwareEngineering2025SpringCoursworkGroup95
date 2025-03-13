package io.github.software.coursework.data;

import io.github.software.coursework.data.schema.Entity;
import io.github.software.coursework.data.schema.Transaction;

import java.io.IOException;
import java.util.SequencedCollection;

public interface Storage extends AutoCloseable {
    Entity getEntity(Reference<Entity> reference) throws NoSuchDocumentException;
    void putEntity(Reference<Entity> reference, Entity entity) throws NoSuchDocumentException;
    void removeEntity(Reference<Entity> reference) throws NoSuchDocumentException;
    SequencedCollection<Reference<Entity>> getEntities() throws NoSuchDocumentException;

    Transaction getTransaction(Reference<Transaction> reference) throws NoSuchDocumentException;
    void putTransaction(Reference<Transaction> reference, Transaction transaction) throws NoSuchDocumentException;
    void removeTransaction(Reference<Transaction> reference) throws NoSuchDocumentException;
    SequencedCollection<Reference<Transaction>> getTransactions() throws NoSuchDocumentException;

    default void close() throws IOException {
        flush();
    }
    default void flush() throws IOException {
    }
}
