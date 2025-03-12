package io.github.software.coursework.data;

public interface Document {

    interface Reader {
        long readInteger(String key);
        double readFloat(String key);
        String readString(String key);
        Reference<?> readReference(String key);
        Reader readCompound(String key);
        boolean isEnd();
        void readEnd();

        default long readInteger(int key) {
            return readInteger(Integer.toString(key));
        }
        default double readFloat(int key) {
            return readFloat(Integer.toString(key));
        }
        default String readString(int key) {
            return readString(Integer.toString(key));
        }
        default Reference<?> readReference(int key) {
            return readReference(Integer.toString(key));
        }
        default Reader readCompound(int key) {
            return readCompound(Integer.toString(key));
        }
    }

    interface Writer {
        void writeInteger(String key, long value);
        void writeFloat(String key, double value);
        void writeString(String key, String value);
        void writeReference(String key, Reference<?> value);
        Writer writeCompound(String key);
        void writeEnd();

        default void writeInteger(int key, long value) {
            writeInteger(Integer.toString(key), value);
        }
        default void writeFloat(int key, double value) {
            writeFloat(Integer.toString(key), value);
        }
        default void writeString(int key, String value) {
            writeString(Integer.toString(key), value);
        }
        default void writeReference(int key, Reference<?> value) {
            writeReference(Integer.toString(key), value);
        }
        default Writer writeCompound(int key) {
            return writeCompound(Integer.toString(key));
        }
    }

    Reader reader();
    Writer writer();
}
