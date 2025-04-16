package io.github.software.coursework.data.schema;

import com.google.common.collect.ImmutableList;
import io.github.software.coursework.data.Document;
import io.github.software.coursework.data.Item;
import org.apache.commons.lang3.tuple.ImmutableTriple;

import java.io.IOException;

public record Goal(
        long start,
        long end,
        long budget,
        long saving,
        ImmutableList<ImmutableTriple<String, Long, Long>> byCategory
) implements Item {
    public Goal withStart(long start) {
        return new Goal(start, end, budget, saving, byCategory);
    }

    public Goal withEnd(long end) {
        return new Goal(start, end, budget, saving, byCategory);
    }

    public Goal withBudget(long budget) {
        return new Goal(start, end, budget, saving, byCategory);
    }

    public Goal withSaving(long saving) {
        return new Goal(start, end, budget, saving, byCategory);
    }

    public Goal withByCategory(ImmutableList<ImmutableTriple<String, Long, Long>> byCategory) {
        return new Goal(start, end, budget, saving, byCategory);
    }

    public Goal withCategoryGoal(String category, long categoryBudget, long categorySaving) {
        ImmutableList.Builder<ImmutableTriple<String, Long, Long>> builder = ImmutableList.builder();
        for (ImmutableTriple<String, Long, Long> triple : byCategory) {
            if (!triple.getLeft().equals(category)) {
                builder.add(triple);
            }
        }
        builder.add(ImmutableTriple.of(category, categoryBudget, categorySaving));
        return new Goal(start, end, budget, saving, builder.build());
    }

    public Goal withoutCategoryGoal(String category) {
        ImmutableList.Builder<ImmutableTriple<String, Long, Long>> builder = ImmutableList.builder();
        for (ImmutableTriple<String, Long, Long> triple : byCategory) {
            if (!triple.getLeft().equals(category)) {
                builder.add(triple);
            }
        }
        return new Goal(start, end, budget, saving, builder.build());
    }

    @Override
    public void serialize(Document.Writer writer) throws IOException {
        writer.writeInteger("schema", 1);
        writer.writeInteger("start", start);
        writer.writeInteger("end", end);
        writer.writeInteger("budget", budget);
        writer.writeInteger("saving", saving);
        Document.Writer byCategoryWriter = writer.writeCompound("byCategory");
        for (int i = 0; i < byCategory.size(); i++) {
            ImmutableTriple<String, Long, Long> triple = byCategory.get(i);
            Document.Writer categoryWriter = byCategoryWriter.writeCompound(i);
            categoryWriter.writeString("category", triple.getLeft());
            categoryWriter.writeInteger("budget", triple.getMiddle());
            categoryWriter.writeInteger("saving", triple.getRight());
            categoryWriter.writeEnd();
        }
        byCategoryWriter.writeEnd();
        writer.writeEnd();
    }

    public static Goal deserialize(Document.Reader reader) throws IOException {
        long schema = reader.readInteger("schema");
        if (schema != 1) {
            throw new IOException("Unsupported schema version: " + schema);
        }
        Goal rval = new Goal(
                reader.readInteger("start"),
                reader.readInteger("end"),
                reader.readInteger("budget"),
                reader.readInteger("saving"),
                null
        );
        ImmutableList.Builder<ImmutableTriple<String, Long, Long>> byCategoryBuilder = ImmutableList.builder();
        Document.Reader byCategoryReader = reader.readCompound("byCategory");
        for (int i = 0; !byCategoryReader.isEnd(); i++) {
            Document.Reader categoryReader = byCategoryReader.readCompound(i);
            String category = categoryReader.readString("category");
            long budget = categoryReader.readInteger("budget");
            long saving = categoryReader.readInteger("saving");
            byCategoryBuilder.add(ImmutableTriple.of(category, budget, saving));
            categoryReader.readEnd();
        }
        byCategoryReader.readEnd();
        reader.readEnd();
        return rval.withByCategory(byCategoryBuilder.build());
    }

    public record Optional(Goal goal) implements Item {
        public Optional() {
            this(null);
        }

        @Override
        public void serialize(Document.Writer writer) throws IOException {
            if (goal == null) {
                writer.writeInteger("present", 0);
                writer.writeEnd();
            } else {
                writer.writeInteger("present", 1);
                goal.serialize(writer);
            }
        }

        public static Optional deserialize(Document.Reader reader) throws IOException {
            long present = reader.readInteger("present");
            if (present == 0) {
                reader.readEnd();
                return new Optional();
            } else {
                return new Optional(Goal.deserialize(reader));
            }
        }
    }
}
