package io.github.software.coursework.data.text;

import com.google.common.io.Files;
import io.github.software.coursework.data.Document;
import io.github.software.coursework.data.Reference;
import io.github.software.coursework.data.memory.MemoryStorage;
import io.github.software.coursework.data.schema.Entity;
import io.github.software.coursework.data.schema.Transaction;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TextFormat {
    public static void exportTo(MemoryStorage storage, File file, boolean pretty) throws IOException {
        TextDocument document = new TextDocument("", pretty);
        Document.Writer writer = document.writer();
        Document.Writer entities = writer.writeCompound("entities");
        for (Reference<Entity> reference : storage.getEntities()) {
            Entity entity = storage.getEntity(reference);
            Document.Writer writerEntity = entities.writeCompound("entity");
            writerEntity.writeReference("reference", reference);
            entity.serialize(writerEntity);
        }
        entities.writeEnd();
        Document.Writer transactions = writer.writeCompound("transactions");
        for (Reference<Transaction> reference : storage.getTransactions()) {
            Transaction transaction = storage.getTransaction(reference);
            Document.Writer writerTransaction = transactions.writeCompound("transaction");
            writerTransaction.writeReference("reference", reference);
            transaction.serialize(writerTransaction);
        }
        transactions.writeEnd();
        writer.writeEnd();
        Files.write(document.toString().getBytes(StandardCharsets.UTF_8), file);
    }

    @SuppressWarnings("unchecked")
    public static void importFrom(MemoryStorage storage, File file, boolean pretty) throws IOException {
        TextDocument document = new TextDocument(new String(Files.toByteArray(file), StandardCharsets.UTF_8), pretty);
        Document.Reader reader = document.reader();
        Document.Reader entities = reader.readCompound("entities");
        while (!entities.isEnd()) {
            Document.Reader entity = entities.readCompound("entity");
            Reference<Entity> reference = (Reference<Entity>) entity.readReference("reference");
            storage.putEntity(reference, Entity.deserialize(entity));
        }
        entities.readEnd();
        Document.Reader transactions = reader.readCompound("transactions");
        while (!transactions.isEnd()) {
            Document.Reader transaction = transactions.readCompound("transaction");
            Reference<Transaction> reference = (Reference<Transaction>) transaction.readReference("reference");
            storage.putTransaction(reference, Transaction.deserialize(transaction));
        }
        transactions.readEnd();
    }
}
