package com.jakewharton.disklrucache;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

class RandomAccessFileOutputStream extends OutputStream {

    private final RandomAccessFile file;

    public RandomAccessFileOutputStream(RandomAccessFile file) {
        this.file = file;
    }

    @Override
    public void write(int b) throws IOException {
        file.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        file.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        file.write(b, off, len);
    }
}
