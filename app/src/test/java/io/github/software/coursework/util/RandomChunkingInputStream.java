package io.github.software.coursework.util;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public class RandomChunkingInputStream extends InputStream {
    private final InputStream in;
    private final Random random = new Random();

    public RandomChunkingInputStream(InputStream in) {
        this.in = in;
    }

    @Override
    public int read(byte @NonNull [] b, int off, int len) throws IOException {
        return in.read(b, off, Math.min(len, random.nextInt(5) + 1));
    }

    @Override
    public int read() throws IOException {
        byte[] buf = new byte[1];
        int read = read(buf, 0, 1);
        return read == -1 ? -1 : buf[0] & 0xFF;
    }
}
