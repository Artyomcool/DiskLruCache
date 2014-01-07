package com.jakewharton.disklrucache;

import org.jetbrains.annotations.NotNull;

import java.io.*;

class TransactionalFile extends TransactionalVirtualFile {

    @NotNull
    private final RandomAccessFile file;
    @NotNull
    private final RandomAccessFileInputStream inputStream;
    @NotNull
    private final RandomAccessFileOutputStream outputStream;

    public TransactionalFile(File file) throws IOException {
        this.file = new RandomAccessFile(file, "rwd");
        this.inputStream = new RandomAccessFileInputStream(this.file);
        this.outputStream = new RandomAccessFileOutputStream(this.file);
    }

    public void close() {
        Util.closeQuietly(inputStream);
        Util.closeQuietly(outputStream);
        Util.closeQuietly(file);
    }

    @NotNull
    public InputStream getInputStream() {
        return inputStream;
    }

    @NotNull
    public OutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    public void readFully(byte[] b) throws IOException {
        file.readFully(b);
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        file.readFully(b, off, len);
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

    @Override
    public boolean readBoolean() throws IOException {
        return file.readBoolean();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return file.readUnsignedByte();
    }

    @Override
    public byte readByte() throws IOException {
        return file.readByte();
    }

    @Override
    public short readShort() throws IOException {
        return file.readShort();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return file.readUnsignedShort();
    }

    @Override
    public char readChar() throws IOException {
        return file.readChar();
    }

    @Override
    public int readInt() throws IOException {
        return file.readInt();
    }

    @Override
    public long readLong() throws IOException {
        return file.readLong();
    }

    @Override
    public float readFloat() throws IOException {
        return file.readFloat();
    }

    @Override
    public double readDouble() throws IOException {
        return file.readDouble();
    }

    @Override
    public String readLine() throws IOException {
        return file.readLine();
    }

    @NotNull
    @Override
    public String readUTF() throws IOException {
        return file.readUTF();
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        file.writeBoolean(v);
    }

    @Override
    public void writeByte(int v) throws IOException {
        file.writeByte(v);
    }

    @Override
    public void writeShort(int v) throws IOException {
        file.writeShort(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        file.writeInt(v);
    }

    @Override
    public int skipBytes(int n) throws IOException {
        return file.skipBytes(n);
    }

    @Override
    public void writeChar(int v) throws IOException {
        file.writeChar(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        file.writeLong(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        file.writeFloat(v);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        file.writeDouble(v);
    }

    @Override
    public void writeBytes(String s) throws IOException {
        file.writeBytes(s);
    }

    @Override
    public void writeChars(String s) throws IOException {
        file.writeChars(s);
    }

    @Override
    public void writeUTF(String str) throws IOException {
        file.writeUTF(str);
    }

    @Override
    public long length() throws IOException {
        return file.length();
    }

    @Override
    protected void seek(long pos) throws IOException {
        file.seek(pos);
    }

    @Override
    protected long pos() throws IOException {
        return file.getFilePointer();
    }

    @Override
    protected void setLength(long length) throws IOException {
        file.setLength(length);
    }
}
