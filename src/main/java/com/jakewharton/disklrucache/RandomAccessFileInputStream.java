package com.jakewharton.disklrucache;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

class RandomAccessFileInputStream extends InputStream {

    @NotNull
    private final RandomAccessFile file;

    public RandomAccessFileInputStream(@NotNull RandomAccessFile file) {
        this.file = file;
    }

    @Override
    public int read() throws IOException {
        return file.read();
    }

    @Override
    public int read(@NotNull byte[] b) throws IOException {
        return file.read(b);
    }

    @Override
    public int read(@NotNull byte[] b, int off, int len) throws IOException {
        return file.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return file.skipBytes((int) (n & 0x7FFFFFFF));
    }

    @Override
    public int available() throws IOException {
        return (int) (file.length() - file.getFilePointer());
    }
}
