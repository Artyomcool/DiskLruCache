package com.jakewharton.disklrucache;

import java.io.*;

abstract class TransactionalVirtualFile implements DataInput, DataOutput {

    public static final long HEADER_SIZE = 16;

    public abstract InputStream getInputStream();
    public abstract OutputStream getOutputStream();

    protected abstract void seek(long pos) throws IOException;
    protected abstract long pos() throws IOException;
    protected abstract void setLength(long length) throws IOException;
    public abstract long length() throws IOException;

    public abstract void close();

    private long saveFileSize(boolean usePointer) throws IOException {
        long pos = usePointer ? pos() : -1;
        seek(0);

        long size = usePointer ? pos : length();
        writeLong(size);
        writeLong(size);

        if (usePointer) {
            seek(pos);
        }

        return size;
    }

    private long restoreFileSize() throws IOException {
        long size1 = readLong();
        long size2 = readLong();
        return size1 == size2 ? size1 : saveFileSize(false);
    }

    public void createEmpty() throws IOException {
        saveFileSize(false);
    }

    public void init() throws IOException {
        long size = restoreFileSize();
        if (size < length()) {
            setLength(size);
            seek(HEADER_SIZE);
        }
    }

    public void commit() throws IOException {
        saveFileSize(true);
    }
}
