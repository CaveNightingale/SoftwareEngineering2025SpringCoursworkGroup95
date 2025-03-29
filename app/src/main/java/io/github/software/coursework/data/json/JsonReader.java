package io.github.software.coursework.data.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.github.software.coursework.data.Document;
import io.github.software.coursework.data.Reference;
import io.github.software.coursework.data.SyntaxException;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

@ParametersAreNonnullByDefault
public class JsonReader implements Document.Reader {
    private final Logger logger = Logger.getLogger("JsonReader");

    private final JsonParser jsonParser;
    private final boolean array;
    private final boolean root;
    private int index;
    private JsonReader childReader;
    private boolean exhausted;
    private boolean suppressWarning = false;

    private JsonReader(JsonParser jsonParser, boolean array, boolean root) {
        this.jsonParser = jsonParser;
        this.array = array;
        this.root = root;
    }

    private void checkField(String key, JsonToken... type) throws IOException {
        if (exhausted) {
            throw new IllegalStateException("You have reach the end of the document.");
        }
        if (childReader != null && !childReader.exhausted) {
            throw new IllegalStateException("You should exhaust previous field before continuing.");
        }
        if (array) {
            if (!key.equals(Integer.toString(index))) {
                throw new SyntaxException("Expected " + index + ", found " + key);
            }
            index++;
        } else {
            if (jsonParser.currentToken() != JsonToken.FIELD_NAME || !key.equals(jsonParser.getText())) {
                throw new SyntaxException("Expected field name, found " + jsonParser.currentToken());
            }
            jsonParser.nextToken();
        }
        JsonToken token = jsonParser.currentToken();
        for (JsonToken t : type) {
            if (token == t) {
                return;
            }
        }
        throw new SyntaxException("Expected " + Arrays.toString(type) + ", found " + token);
    }

    public static JsonReader createReader(JsonParser parser) throws IOException {
        JsonReader jsonReader = new JsonReader(parser, switch (parser.nextToken()) {
            case START_ARRAY -> true;
            case START_OBJECT -> false;
            default -> throw new SyntaxException("Expected start of compound, found " + parser.currentToken());
        }, true);
        parser.nextToken();
        return jsonReader;
    }

    @Override
    public long readInteger(String key) throws IOException {
        checkField(key, JsonToken.VALUE_NUMBER_INT);
        long value = jsonParser.getLongValue();
        jsonParser.nextToken();
        return value;
    }

    @Override
    public double readFloat(String key) throws IOException {
        checkField(key, JsonToken.VALUE_NUMBER_FLOAT, JsonToken.VALUE_NUMBER_INT);
        double value = jsonParser.getValueAsDouble();
        jsonParser.nextToken();
        return value;
    }

    @Override
    public String readString(String key) throws IOException {
        checkField(key, JsonToken.VALUE_STRING);
        String value = jsonParser.getText();
        jsonParser.nextToken();
        return value;
    }

    @Override
    @SuppressWarnings({"rawtypes"})
    public @Nullable Reference<?> readReference(String key) throws IOException {
        checkField(key, JsonToken.VALUE_STRING, JsonToken.VALUE_NULL);
        Reference<?> ref = switch (jsonParser.currentToken()) {
            case JsonToken.VALUE_STRING -> new Reference(Long.parseUnsignedLong(jsonParser.getText(), 16));
            case JsonToken.VALUE_NULL -> null;
            default -> throw new Error();
        };
        jsonParser.nextToken();
        return ref;
    }

    @Override
    public Document.Reader readCompound(String key) throws IOException {
        checkField(key, JsonToken.START_ARRAY, JsonToken.START_OBJECT);
        this.childReader = switch (jsonParser.currentToken()) {
            case START_ARRAY -> new JsonReader(jsonParser, true, false);
            case START_OBJECT -> new JsonReader(jsonParser, false, false);
            default -> throw new Error();
        };
        jsonParser.nextToken();
        return childReader;
    }

    @Override
    public boolean isEnd() throws IOException {
        if (exhausted) {
            throw new IllegalStateException("You have reach the end of the document.");
        }
        if (childReader != null && !childReader.exhausted) {
            throw new IllegalStateException("You should exhaust previous field before continuing.");
        }
        return jsonParser.currentToken() == (array ? JsonToken.END_ARRAY : JsonToken.END_OBJECT);
    }

    @Override
    public void readEnd() throws IOException {
        if (exhausted) {
            throw new IllegalStateException("You have reach the end of the document.");
        }
        if (childReader != null && !childReader.exhausted) {
            throw new IllegalStateException("You should exhaust previous field before continuing.");
        }
        if (!isEnd()) {
            throw new SyntaxException("Expected end of compound, found " + jsonParser.currentToken());
        }
        jsonParser.nextToken();
        this.exhausted = true;
    }

    void suppressWarning() {
        this.suppressWarning = true;
    }

    @Override
    public void close() throws IOException {
        if (!exhausted && !suppressWarning) {
            logger.info("Close the reader before reaching the end of the document.");
        }
        if (root) {
            jsonParser.close();
        }
    }
}
