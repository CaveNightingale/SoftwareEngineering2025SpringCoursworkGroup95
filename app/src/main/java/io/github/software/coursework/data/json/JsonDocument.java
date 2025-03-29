package io.github.software.coursework.data.json;

import com.fasterxml.jackson.core.JsonFactory;
import io.github.software.coursework.data.Document;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class JsonDocument implements Document {
    private final ByteArrayOutputStream bytes;
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();

    public JsonDocument() {
        this.bytes = new ByteArrayOutputStream();
        try {
            this.bytes.write("[]".getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new Error(e); // Unreachable
        }
    }

    @Override
    public Reader reader() {
        try {
            return JsonReader.createReader(JSON_FACTORY.createParser(bytes.toByteArray()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Writer writer() {
        bytes.reset();
        try {
            return JsonWriter.createWriter(JSON_FACTORY.createGenerator(bytes));
        } catch (IOException ex) {
            throw new Error(ex); // Unreachable
        }
    }

    @Override
    public String toString() {
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
