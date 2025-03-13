package io.github.software.coursework.data.text;

import io.github.software.coursework.data.Document;
import io.github.software.coursework.data.Reference;

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

        public void readKey(String key) {
            if (!pretty) {
                return;
            }
            for (int i = 0; i < indent; i++) {
                if (text.charAt(cursor.index) != '\t') {
                    throw new IllegalStateException("Invalid indentation");
                }
                cursor.index++;
            }
            if (!text.substring(cursor.index, cursor.index + key.length()).equals(key)) {
                throw new IllegalStateException("Invalid key");
            }
            cursor.index += key.length();
            if (text.charAt(cursor.index) != ':') {
                throw new IllegalStateException("Invalid key-value separator");
            }
            cursor.index++;
            skipWhitespace();
        }

        public String readTokenValue() {
            int start = cursor.index;
            while (cursor.index < text.length() && !Character.isWhitespace(text.charAt(cursor.index))) {
                cursor.index++;
            }
            String result = text.substring(start, cursor.index);
            skipWhitespace();
            return result;
        }

        public String readQuotedString() {
            if (text.charAt(cursor.index) != '"') {
                return readTokenValue();
            }
            cursor.index++;
            StringBuilder result = new StringBuilder();
            while (cursor.index < text.length() && text.charAt(cursor.index) != '"') {
                if (text.charAt(cursor.index) == '\\') {
                    cursor.index++;
                    if (cursor.index == text.length()) {
                        throw new IllegalStateException("Invalid escape sequence");
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
                                throw new IllegalStateException("Invalid escape sequence");
                            }
                            int code = Integer.parseInt(text.substring(cursor.index + 1, cursor.index + 5), 16);
                            cursor.index += 4;
                            yield (char) code;
                        }
                        default -> throw new IllegalStateException("Invalid escape sequence");
                    });
                } else {
                    result.append(text.charAt(cursor.index));
                }
                cursor.index++;
            }
            if (cursor.index == text.length()) {
                throw new IllegalStateException("Invalid string");
            }
            cursor.index++;
            skipWhitespace();
            return result.toString();
        }

        @Override
        public long readInteger(String key) {
            readKey(key);
            return Long.parseLong(readTokenValue());
        }

        @Override
        public double readFloat(String key) {
            readKey(key);
            return Double.parseDouble(readTokenValue());
        }

        @Override
        public String readString(String key) {
            readKey(key);
            return readQuotedString();
        }

        @Override
        public Reference<?> readReference(String key) {
            readKey(key);
            String token = readTokenValue();
            if (token.equals("/")) { // null reference
                return null;
            }
            return new Reference<>(Long.parseUnsignedLong(token, 16));
        }

        @Override
        public Reader readCompound(String key) {
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
        public void readEnd() {
            if (pretty) {
                for (int i = 0; i < indent; i++) {
                    if (text.charAt(cursor.index) != '\t') {
                        throw new IllegalStateException("Invalid indentation");
                    }
                    cursor.index++;
                }
            }
            if (!isEnd()) {
                throw new IllegalStateException("Invalid end of compound");
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
        public void writeReference(String key, Reference<?> value) {
            writeKey(key);
            if (value == null) {
                text.append("/");
            } else {
                text.append(Long.toHexString(value.id()));
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
