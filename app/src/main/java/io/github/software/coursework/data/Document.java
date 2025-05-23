package io.github.software.coursework.data;

import org.checkerframework.checker.nullness.qual.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;

/**
 * An interface for reading and writing documents.
 * Notice that a document may or may not record the key, if the key is not stored, it will
 * rely on the order of the fields to read and write.
 */
@ParametersAreNonnullByDefault
public interface Document {

    /**
     * A reader for reading documents.
     * Since most text-based file format stores 32-bit integers and 64-bit integers in completely
     * same way, we only provide 64-bit integer reader. And the case for float is the same.
     */
    interface Reader extends AutoCloseable {
        /**
         * Read a 64-bit integer from the document.
         * @param key the key to read
         * @return the integer value
         * @throws IOException if an I/O error occurs
         */
        long readInteger(String key) throws IOException;

        /**
         * Read a 64-bit  float from the document.
         * @param key the key to read
         * @return the float value
         * @throws IOException if an I/O error occurs
         */
        double readFloat(String key) throws IOException;

        /**
         * Read a string from the document.
         * @param key the key to read
         * @return the string value
         * @throws IOException if an I/O error occurs
         */
        String readString(String key) throws IOException;

        /**
         * Read a reference from the document.
         * @param key the key to read
         * @return the reference value
         * @throws IOException if an I/O error occurs
         */
        @Nullable Reference<?> readReference(String key) throws IOException;

        /**
         * Read a compound from the document. It returns a new reader for the compound.
         * You must exhaust the reader before reading the next field. (i.e. cursor must
         * move over the END_OF_COMPOUND token in that compound and points to the next field)
         * @param key the key to read
         * @return the compound value
         * @throws IOException if an I/O error occurs
         */
        Reader readCompound(String key) throws IOException;

        /**
         * Check if the end of the compound is reached.
         * @return true if the end of the document is reached, false otherwise
         * @throws IOException if an I/O error occurs
         */
        boolean isEnd() throws IOException;

        /**
         * This will consume the END_OF_COMPOUND token.
         * @throws IOException if an I/O error occurs
         */
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

    /**
     * A writer for writing documents.
     */
    interface Writer extends AutoCloseable {
        /**
         * Write a 64-bit integer to the document.
         * @param key the key to write
         * @param value the integer value
         * @throws IOException if an I/O error occurs
         */
        void writeInteger(String key, long value) throws IOException;

        /**
         * Write a 64-bit float to the document.
         * @param key the key to write
         * @param value the float value
         * @throws IOException if an I/O error occurs
         */
        void writeFloat(String key, double value) throws IOException;

        /**
         * Write a string to the document.
         * @param key the key to write
         * @param value the string value
         * @throws IOException if an I/O error occurs
         */
        void writeString(String key, String value) throws IOException;

        /**
         * Write a reference to the document.
         * @param key the key to write
         * @param value the reference value
         * @throws IOException if an I/O error occurs
         */
        void writeReference(String key, @Nullable Reference<?> value) throws IOException;

        /**
         * Write a compound to the document. It returns a new writer for the compound.
         * You must call writeEnd() before writing the next field. (i.e. END_OF_COMPOUND token
         * must be written at the end of the compound)
         * @param key the key to write
         * @return the compound writer
         */
        Writer writeCompound(String key);

        /**
         * Write the end of the compound to the document.
         * @throws IOException if an I/O error occurs
         */
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

    /**
     * Create a new reader for the document.
     * @return the reader
     */
    Reader reader();

    /**
     * Create a new writer for the document.
     * @return the writer
     */
    Writer writer();
}
