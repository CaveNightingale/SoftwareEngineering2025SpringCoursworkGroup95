package io.github.software.coursework.data.memory;


import io.github.software.coursework.data.NoSuchDocumentException;
import io.github.software.coursework.data.Reference;
import io.github.software.coursework.data.Storage;
import io.github.software.coursework.data.schema.Entity;
import io.github.software.coursework.data.schema.Transaction;

import java.util.*;

public class MemoryStorage implements Storage {
    private record NamedEntity(String name, Reference<Entity> reference) implements Comparable<NamedEntity> {
        @Override
        public int compareTo(NamedEntity namedEntity) {
            int rl =  name.compareTo(namedEntity.name);
            return rl != 0 ? rl : Long.compare(reference.id(), namedEntity.reference.id());
        }
    }

    private record TimestampedTransaction(long timestamp, Reference<Transaction> reference) implements Comparable<TimestampedTransaction> {
        @Override
        public int compareTo(TimestampedTransaction timestampedTransaction) {
            if (timestamp != timestampedTransaction.timestamp) {
                return Long.compare(timestamp, timestampedTransaction.timestamp);
            } else {
                return Long.compare(reference.id(), timestampedTransaction.reference.id());
            }
        }
    }

    private final HashMap<Reference<Entity>, Entity> entities = new HashMap<>();
    private final HashMap<Reference<Transaction>, Transaction> transactions = new HashMap<>();
    private final TreeSet<NamedEntity> namedEntities = new TreeSet<>();
    private final TreeSet<TimestampedTransaction> timestampedTransactions = new TreeSet<>();

    @Override
    public Entity getEntity(Reference<Entity> reference) throws NoSuchDocumentException {
        return entities.get(reference);
    }

    @Override
    public void putEntity(Reference<Entity> reference, Entity entity) throws NoSuchDocumentException {
        Entity old = entities.put(reference, entity);
        if (old != null && !old.name().equals(entity.name())) {
            namedEntities.remove(new NamedEntity(old.name(), reference));
        }
        if (old == null || !old.name().equals(entity.name())) {
            namedEntities.add(new NamedEntity(entity.name(), reference));
        }
    }

    @Override
    public void removeEntity(Reference<Entity> reference) throws NoSuchDocumentException {
        Entity old = entities.remove(reference);
        if (old != null) {
            namedEntities.remove(new NamedEntity(old.name(), reference));
        }
    }

    @Override
    public SequencedCollection<Reference<Entity>> getEntities() throws NoSuchDocumentException {
        return namedEntities.stream().map(NamedEntity::reference).toList();
    }

    @Override
    public Transaction getTransaction(Reference<Transaction> reference) throws NoSuchDocumentException {
        return transactions.get(reference);
    }

    @Override
    public void putTransaction(Reference<Transaction> reference, Transaction transaction) throws NoSuchDocumentException {
        Transaction old = transactions.put(reference, transaction);
        if (old != null && old.time() != transaction.time()) {
            timestampedTransactions.remove(new TimestampedTransaction(old.time(), reference));
        }
        if (old == null || old.time() != transaction.time()) {
            timestampedTransactions.add(new TimestampedTransaction(transaction.time(), reference));
        }
    }

    @Override
    public void removeTransaction(Reference<Transaction> reference) throws NoSuchDocumentException {
        Transaction transaction = transactions.remove(reference);
        if (transaction != null) {
            timestampedTransactions.remove(new TimestampedTransaction(transaction.time(), reference));
        }
    }

    @Override
    public SequencedCollection<Reference<Transaction>> getTransactions() throws NoSuchDocumentException {
        return timestampedTransactions.stream().map(TimestampedTransaction::reference).toList();
    }
}
