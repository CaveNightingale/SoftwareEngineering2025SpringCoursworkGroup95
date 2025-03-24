package io.github.software.coursework.data.csv;

import io.github.software.coursework.data.Document;
import io.github.software.coursework.data.Reference;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;

@ParametersAreNonnullByDefault
public class CSVDocument implements Document {
    final ArrayList<ImmutablePair<String, String>> data = new ArrayList<>();
    final ArrayList<ImmutablePair<String, Reference<?>>> refs = new ArrayList<>();

    @Override
    public Reader reader() {
        throw new UnsupportedOperationException("CSV is flattened format. It's not possible to deserialize document from it.");
    }

    @Override
    public Writer writer() {
        return new CSVWriter("");
    }

    private class CSVWriter implements Writer {
        private final String path;
        private CSVWriter(String path) {
            this.path = path;
        }

        @Override
        public void writeInteger(String key, long value) {
            data.add(ImmutablePair.of(path + "." + key, Long.toString(value)));
        }

        @Override
        public void writeFloat(String key, double value) {
            data.add(ImmutablePair.of(path + "." + key, Double.toString(value)));
        }

        @Override
        public void writeString(String key, String value) {
            data.add(ImmutablePair.of(path + "." + key, value));
        }

        @Override
        public void writeReference(String key, @Nullable Reference<?> value) {
            if (value != null) {
                refs.add(ImmutablePair.of(path + "." + key, value));
                data.add(ImmutablePair.of(path + "." + key, Long.toString(value.id(), 16)));
            } else {
                data.add(ImmutablePair.of(path + "." + key, ""));
            }
        }

        @Override
        public Writer writeCompound(String key) {
            return new CSVWriter(path + "." + key);
        }

        @Override
        public void writeEnd() {
        }
    }
}
