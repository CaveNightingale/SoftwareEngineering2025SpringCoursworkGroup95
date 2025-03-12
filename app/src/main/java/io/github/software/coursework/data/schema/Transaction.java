package io.github.software.coursework.data.schema;

import com.google.common.collect.ImmutableList;
import io.github.software.coursework.data.Document;
import io.github.software.coursework.data.Item;
import io.github.software.coursework.data.Reference;

import java.util.function.Supplier;

public record Transaction(
        String title,
        String description,
        long time,
        long amount,
        String category,
        Reference<Entity> entity,
        ImmutableList<String> tags
) implements Item<Transaction> {

    @Override
    public void serialize(Document.Writer writer) {
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
    public static Transaction deserialize(Document.Reader reader) {
        return new Transaction(
                reader.readString("title"),
                reader.readString("description"),
                reader.readInteger("time"),
                reader.readInteger("amount"),
                reader.readString("category"),
                (Reference<Entity>) reader.readReference("entity"),
                ((Supplier<ImmutableList<String>>) () -> {
                    ImmutableList.Builder<String> tags = ImmutableList.builder();
                    Document.Reader tagsReader = reader.readCompound("tags");
                    for (int i = 0; !tagsReader.isEnd(); i++) {
                        tags.add(tagsReader.readString(i));
                    }
                    tagsReader.readEnd();
                    return tags.build();
                }).get()
        );
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
