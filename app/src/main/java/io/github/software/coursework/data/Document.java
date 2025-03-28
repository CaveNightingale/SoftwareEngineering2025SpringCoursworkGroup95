package io.github.software.coursework.data;

import org.checkerframework.checker.nullness.qual.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;

@ParametersAreNonnullByDefault
public interface Document {

    interface Reader extends AutoCloseable {
        long readInteger(String key) throws IOException;
        double readFloat(String key) throws IOException;
        String readString(String key) throws IOException;
        @Nullable Reference<?> readReference(String key) throws IOException;
        Reader readCompound(String key) throws IOException;
        boolean isEnd() throws IOException;
        void readEnd() throws IOException;

        default long readInteger(int key) throws IOException {
            return readInteger(Integer.toString(key));
        }
        default double readFloat(int key) throws IOException {
            return readFloat(Integer.toString(key));
        }
        default String readString(int key) throws IOException {
            return readString(Integer.toString(key));
        }
        default @Nullable Reference<?> readReference(int key) throws IOException {
            return readReference(Integer.toString(key));
        }
        default Reader readCompound(int key) throws IOException {
            return readCompound(Integer.toString(key));
        }
        default void close() throws IOException {}
    }

    interface Writer extends AutoCloseable {
        void writeInteger(String key, long value) throws IOException;
        void writeFloat(String key, double value) throws IOException;
        void writeString(String key, String value) throws IOException;
        void writeReference(String key, @Nullable Reference<?> value) throws IOException;
        Writer writeCompound(String key);
        void writeEnd() throws IOException;

        default void writeInteger(int key, long value) throws IOException {
            writeInteger(Integer.toString(key), value);
        }
        default void writeFloat(int key, double value) throws IOException {
            writeFloat(Integer.toString(key), value);
        }
        default void writeString(int key, String value) throws IOException {
            writeString(Integer.toString(key), value);
        }
        default void writeReference(int key, @Nullable Reference<?> value) throws IOException {
            writeReference(Integer.toString(key), value);
        }
        default Writer writeCompound(int key) throws IOException {
            return writeCompound(Integer.toString(key));
        }
        default void close() throws IOException {}
    }

    Reader reader();
    Writer writer();
}
