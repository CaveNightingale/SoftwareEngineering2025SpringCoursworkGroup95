package io.github.software.coursework.data.json;

import com.fasterxml.jackson.core.JsonGenerator;
import io.github.software.coursework.data.Document;
import io.github.software.coursework.data.Reference;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.stream.IntStream;

@ParametersAreNonnullByDefault
public class JsonWriter implements Document.Writer {
    private record Entry(String key, @Nullable Object value) {}

    private final Logger logger = Logger.getLogger("JsonWriter");
    private final JsonGenerator jsonGenerator;
    private final ArrayList<Entry> entries = new ArrayList<>();
    private boolean ended = false;

    private static void serialize(JsonGenerator jsonGenerator, @Nullable Object object) throws IOException {
        switch (object) {
            case Long value -> jsonGenerator.writeNumber(value);
            case Double value -> jsonGenerator.writeNumber(value);
            case String value -> jsonGenerator.writeString(value);
            case null -> jsonGenerator.writeNull();
            case Reference<?> value -> jsonGenerator.writeString(Long.toUnsignedString(value.id(), 16));
            case JsonWriter value -> {
                boolean isArray = IntStream.range(0, value.entries.size()).allMatch(i -> value.entries.get(i).key.equals(Integer.toString(i)));
                if (isArray) {
                    jsonGenerator.writeStartArray();
                    for (Entry entry : value.entries) {
                        serialize(jsonGenerator, entry.value());
                    }
                    jsonGenerator.writeEndArray();
                } else {
                    jsonGenerator.writeStartObject();
                    for (Entry entry : value.entries) {
                        jsonGenerator.writeFieldName(entry.key());
                        serialize(jsonGenerator, entry.value());
                    }
                    jsonGenerator.writeEndObject();
                }
            }
            default -> throw new Error();
        }
    }

    private JsonWriter(@Nullable JsonGenerator jsonGenerator) {
        this.jsonGenerator = jsonGenerator;
    }

    public static JsonWriter createWriter(JsonGenerator jsonGenerator) {
        return new JsonWriter(jsonGenerator);
    }

    @Override
    public void writeInteger(String key, long value) throws IOException {
        entries.add(new Entry(key, value));
    }

    @Override
    public void writeFloat(String key, double value) throws IOException {
        entries.add(new Entry(key, value));
    }

    @Override
    public void writeString(String key, String value) throws IOException {
        entries.add(new Entry(key, value));
    }

    @Override
    public void writeReference(String key, @Nullable Reference<?> value) throws IOException {
        entries.add(new Entry(key, value));
    }

    @Override
    public Document.Writer writeCompound(String key) {
        JsonWriter jsonWriter = new JsonWriter(null);
        entries.add(new Entry(key, jsonWriter));
        return jsonWriter;
    }

    @Override
    public void writeEnd() throws IOException {
        if (jsonGenerator != null) { // Root writer
            serialize(jsonGenerator, this);
        }
    }

    @Override
    public void close() throws IOException {
        if (!ended) {
            logger.info("Close the reader without writing an end of document symbol.");
        }
        if (jsonGenerator != null) {
            jsonGenerator.close();
        }
    }
}
