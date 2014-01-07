package com.jakewharton.disklrucache;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Arrays;

public class TransactionalInMemoryFile extends TransactionalVirtualFile {

    private byte[] buf = new byte[4096];
    private int fileSize;
    private int pos;

    private DataInputStream inputStream = new DataInputStream(new SharedInputStream());
    private DataOutputStream outputStream = new DataOutputStream(new SharedOutputStream());

    protected void writeToBuf(byte b) {
        if (pos == buf.length) {
            buf = Arrays.copyOf(buf, pos * 3 / 2);
        }
        buf[pos++] = b;
        if (pos > fileSize) {
            fileSize = pos;
        }
    }

    @Override
    public @NotNull String readUTF() throws IOException {
        return inputStream.readUTF();
    }

    @Override
    public double readDouble() throws IOException {
        return inputStream.readDouble();
    }

    @Override
    public long readLong() throws IOException {
        return inputStream.readLong();
    }

    @Override
    public float readFloat() throws IOException {
        return inputStream.readFloat();
    }

    @Override
    public int readInt() throws IOException {
        return inputStream.readInt();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return inputStream.readUnsignedShort();
    }

    @Override
    public char readChar() throws IOException {
        return inputStream.readChar();
    }

    @Override
    public short readShort() throws IOException {
        return inputStream.readShort();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return inputStream.readUnsignedByte();
    }

    @Override
    public byte readByte() throws IOException {
        return inputStream.readByte();
    }

    @Override
    public boolean readBoolean() throws IOException {
        return inputStream.readBoolean();
    }

    @Override
    public int skipBytes(int n) throws IOException {
        return inputStream.skipBytes(n);
    }

    @Override
    public void readFully(@NotNull byte[] b, int off, int len) throws IOException {
        inputStream.readFully(b, off, len);
    }

    @Override
    public void readFully(@NotNull byte[] b) throws IOException {
        inputStream.readFully(b);
    }

    @SuppressWarnings("deprecation")
    @Override
    @Deprecated
    public String readLine() throws IOException {
        return inputStream.readLine();
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        outputStream.writeBoolean(v);
    }

    @Override
    public void writeByte(int v) throws IOException {
        outputStream.writeByte(v);
    }

    @Override
    public void writeShort(int v) throws IOException {
        outputStream.writeShort(v);
    }

    @Override
    public void writeChar(int v) throws IOException {
        outputStream.writeChar(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        outputStream.writeInt(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        outputStream.writeLong(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        outputStream.writeFloat(v);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        outputStream.writeDouble(v);
    }

    @Override
    public void writeBytes(String s) throws IOException {
        outputStream.writeBytes(s);
    }

    @Override
    public void writeChars(String s) throws IOException {
        outputStream.writeChars(s);
    }

    @Override
    public void writeUTF(String str) throws IOException {
        outputStream.writeUTF(str);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        outputStream.write(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
        outputStream.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        outputStream.write(b);
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public OutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    protected void seek(long pos) throws IOException {
        this.pos = (int) pos;
    }

    @Override
    protected long pos() throws IOException {
        return pos;
    }

    @Override
    protected void setLength(long length) throws IOException {
        fileSize = (int) length;
        if (pos > fileSize) {
            pos = fileSize;
        }
    }

    @Override
    public long length() throws IOException {
        return fileSize;
    }

    @Override
    public void close() {
    }

    private class SharedInputStream extends InputStream {
        @Override
        public int read() throws IOException {
            if (pos == fileSize) {
                return -1;
            }
            return buf[pos++] & 0xFF;
        }

        @Override
        public int available() throws IOException {
            return fileSize - pos;
        }
    }

    private class SharedOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
            writeToBuf((byte) b);
        }
    }
}
