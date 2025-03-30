package io.github.software.coursework.data.schema;

import com.google.common.collect.ImmutableList;
import io.github.software.coursework.data.Document;
import io.github.software.coursework.data.Item;
import io.github.software.coursework.data.Reference;

import java.io.IOException;

public record Transaction(
        String title,
        String description,
        long time,
        long amount,
        String category,
        Reference<Entity> entity,
        ImmutableList<String> tags
) implements Item {

    @Override
    public void serialize(Document.Writer writer) throws IOException{
        writer.writeInteger("schema", 1);
        writer.writeString("title", title);
        writer.writeString("description", description);
        writer.writeInteger("time", time);
        writer.writeInteger("amount", amount);
        writer.writeString("category", category);
        writer.writeReference("entity", entity);
        Document.Writer tagsWriter = writer.writeCompound("tags");
        for (int i = 0; i < tags.size(); i++) {
            tagsWriter.writeString(i, tags.get(i));
        }
        tagsWriter.writeEnd();
        writer.writeEnd();
    }

    @SuppressWarnings("unchecked")
    public static Transaction deserialize(Document.Reader reader) throws IOException {
        long schema = reader.readInteger("schema");
        if (schema != 1) {
            throw new IOException("Unsupported schema version: " + schema);
        }
        Transaction rval = new Transaction(
                reader.readString("title"),
                reader.readString("description"),
                reader.readInteger("time"),
                reader.readInteger("amount"),
                reader.readString("category"),
                (Reference<Entity>) reader.readReference("entity"),
                null
        );
        ImmutableList.Builder<String> tags1 = ImmutableList.builder();
        Document.Reader tagsReader = reader.readCompound("tags");
        for (int i = 0; !tagsReader.isEnd(); i++) {
            tags1.add(tagsReader.readString(i));
        }
        tagsReader.readEnd();
        reader.readEnd();
        return rval.withTags(tags1.build());
    }

    public Transaction withTitle(String title) {
        return new Transaction(title, description, time, amount, category, entity, tags);
    }

    public Transaction withDescription(String description) {
        return new Transaction(title, description, time, amount, category, entity, tags);
    }

    public Transaction withTime(long time) {
        return new Transaction(title, description, time, amount, category, entity, tags);
    }

    public Transaction withAmount(long amount) {
        return new Transaction(title, description, time, amount, category, entity, tags);
    }

    public Transaction withCategory(String category) {
        return new Transaction(title, description, time, amount, category, entity, tags);
    }

    public Transaction withEntity(Reference<Entity> entity) {
        return new Transaction(title, description, time, amount, category, entity, tags);
    }

    public Transaction withTags(ImmutableList<String> tags) {
        return new Transaction(title, description, time, amount, category, entity, tags);
    }
}
