package io.github.software.coursework.data.text;

import io.github.software.coursework.data.Document;
import io.github.software.coursework.data.Reference;
import io.github.software.coursework.data.SyntaxException;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;

@ParametersAreNonnullByDefault
public class TextDocument implements Document {
    private final boolean pretty;
    private final StringBuilder text;

    public TextDocument() {
        this.text = new StringBuilder();
        this.pretty = false;
    }

    public TextDocument(CharSequence text, boolean pretty) {
        this.text = new StringBuilder(text);
        this.pretty = pretty;
    }

    public String toString() {
        return text.toString();
    }

    @Override
    public Reader reader() {
        return new TextReader(0, new Cursor());
    }

    @Override
    public Writer writer() {
        return new TextWriter(0);
    }

    private static class Cursor {
        int index;
    }

    private class TextReader implements Reader {
        private final int indent;
        private final Cursor cursor;

        public TextReader(int indent, Cursor cursor) {
            this.indent = indent;
            this.cursor = cursor;
        }

        public void skipWhitespace() {
            while (cursor.index < text.length() && Character.isWhitespace(text.charAt(cursor.index))) {
                if (cursor.index < text.length() && text.charAt(cursor.index) == '\n') {
                    cursor.index++;
                    break;
                }
                cursor.index++;
            }
        }

        public void readKey(String key) throws IOException {
            if (!pretty) {
                return;
            }
            for (int i = 0; i < indent; i++) {
                if (text.charAt(cursor.index) != '\t') {
                    throw new SyntaxException("Invalid indentation");
                }
                cursor.index++;
            }
            if (!text.substring(cursor.index, cursor.index + key.length()).equals(key)) {
                throw new SyntaxException("Invalid key");
            }
            cursor.index += key.length();
            if (text.charAt(cursor.index) != ':') {
                throw new SyntaxException("Invalid key-value separator");
            }
            cursor.index++;
            skipWhitespace();
        }

        public String readTokenValue() throws IOException {
            int start = cursor.index;
            while (cursor.index < text.length() && !Character.isWhitespace(text.charAt(cursor.index))) {
                cursor.index++;
            }
            String result = text.substring(start, cursor.index);
            skipWhitespace();
            return result;
        }

        public String readQuotedString() throws IOException {
            if (text.charAt(cursor.index) != '"') {
                return readTokenValue();
            }
            cursor.index++;
            StringBuilder result = new StringBuilder();
            while (cursor.index < text.length() && text.charAt(cursor.index) != '"') {
                if (text.charAt(cursor.index) == '\\') {
                    cursor.index++;
                    if (cursor.index == text.length()) {
                        throw new SyntaxException("Invalid escape sequence");
                    }
                    result.append(switch (text.charAt(cursor.index)) {
                        case 'n' -> '\n';
                        case 't' -> '\t';
                        case 'r' -> '\r';
                        case 'b' -> '\b';
                        case 'f' -> '\f';
                        case '"' -> '"';
                        case '\'' -> '\'';
                        case '\\' -> '\\';
                        case 'u' -> {
                            if (cursor.index + 4 >= text.length()) {
                                throw new SyntaxException("Invalid escape sequence");
                            }
                            int code;
                            try {
                                code = Integer.parseInt(text.substring(cursor.index + 1, cursor.index + 5), 16);
                            } catch (NumberFormatException ex) {
                                throw new SyntaxException("Invalid escape sequence");
                            }
                            cursor.index += 4;
                            yield (char) code;
                        }
                        default -> throw new SyntaxException("Invalid escape sequence");
                    });
                } else {
                    result.append(text.charAt(cursor.index));
                }
                cursor.index++;
            }
            if (cursor.index == text.length()) {
                throw new SyntaxException("Invalid string");
            }
            cursor.index++;
            skipWhitespace();
            return result.toString();
        }

        @Override
        public long readInteger(String key) throws IOException {
            readKey(key);
            try {
                return Long.parseLong(readTokenValue());
            } catch (NumberFormatException ex) {
                throw new SyntaxException("Invalid integer", ex);
            }
        }

        @Override
        public double readFloat(String key) throws IOException {
            readKey(key);
            try {
                return Double.parseDouble(readTokenValue());
            } catch (NumberFormatException ex) {
                throw new SyntaxException("Invalid float", ex);
            }
        }

        @Override
        public String readString(String key) throws IOException {
            readKey(key);
            return readQuotedString();
        }

        @Override
        public Reference<?> readReference(String key) throws IOException {
            readKey(key);
            String token = readTokenValue();
            if (token.equals("/")) { // null reference
                return null;
            }
            try {
                return new Reference<>(Long.parseUnsignedLong(token, 16));
            } catch (NumberFormatException ex) {
                throw new SyntaxException("Invalid reference", ex);
            }
        }

        @Override
        public Reader readCompound(String key) throws IOException {
            readKey(key);
            return new TextReader(indent + 1, cursor);
        }

        @Override
        public boolean isEnd() {
            int start = cursor.index;
            while (start < text.length() && Character.isWhitespace(text.charAt(start))) {
                start++;
            }
            return text.charAt(start) == '.' && (start == text.length() || Character.isWhitespace(text.charAt(start + 1)));
        }

        @Override
        public void readEnd() throws IOException {
            if (pretty) {
                for (int i = 0; i < indent; i++) {
                    if (text.charAt(cursor.index) != '\t') {
                        throw new SyntaxException("Invalid indentation");
                    }
                    cursor.index++;
                }
            }
            if (!isEnd()) {
                throw new SyntaxException("Invalid end of compound");
            }
            readTokenValue();
        }
    }

    private class TextWriter implements Writer {
        private final int indent;

        public TextWriter(int indent) {
            this.indent = indent;
        }

        public void writeKey(String key) {
            if (!pretty) {
                return;
            }
            text.append("\t".repeat(indent));
            text.append(key).append(": ");
        }

        public void writeEndOfMember() {
            if (pretty) {
                text.append("\n");
            } else {
                text.append(" ");
            }
        }

        public void writeQuotedString(String value) {
            if (value.chars().allMatch(c -> !Character.isWhitespace(c) && !Character.isISOControl(c)) && !value.isEmpty() && !value.startsWith("\"")) {
                text.append(value);
                return;
            }
            text.append('"');
            for (char c : value.toCharArray()) {
                switch (c) {
                    case '\n' -> text.append("\\n");
                    case '\t' -> text.append("\\t");
                    case '\r' -> text.append("\\r");
                    case '\b' -> text.append("\\b");
                    case '\f' -> text.append("\\f");
                    case '"' -> text.append("\\\"");
                    case '\\' -> text.append("\\\\");
                    default -> {
                        if (Character.isISOControl(c)) {
                            text.append("\\u").append(String.format("%04x", (int) c));
                        } else {
                            text.append(c);
                        }
                    }
                }
            }
            text.append('"');
        }

        @Override
        public void writeInteger(String key, long value) {
            writeKey(key);
            text.append(value);
            writeEndOfMember();
        }

        @Override
        public void writeFloat(String key, double value) {
            writeKey(key);
            text.append(value);
            writeEndOfMember();
        }

        @Override
        public void writeString(String key, String value) {
            writeKey(key);
            writeQuotedString(value);
            writeEndOfMember();
        }

        @Override
        public void writeReference(String key, @Nullable Reference<?> value) {
            writeKey(key);
            if (value == null) {
                text.append("/");
            } else {
                text.append(Long.toUnsignedString(value.id(), 16));
            }
            writeEndOfMember();
        }

        @Override
        public Writer writeCompound(String key) {
            writeKey(key);
            if (pretty) {
                text.append("\n");
            }
            return new TextWriter(indent + 1);
        }

        @Override
        public void writeEnd() {
            if (pretty) {
                text.append("\t".repeat(indent));
            }
            text.append(".");
            writeEndOfMember();
        }
    }
}
