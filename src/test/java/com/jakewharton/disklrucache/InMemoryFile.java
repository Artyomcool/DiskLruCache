package com.jakewharton.disklrucache;

import java.io.IOException;

public class InMemoryFile implements VirtualFile {

    private final InMemoryFileSystem fileSystem;
    private String name;

    protected InMemoryFile(InMemoryFileSystem fileSystem, String name) {
        this.fileSystem = fileSystem;
        this.name = name;
    }

    @Override
    public boolean exists() {
        return fileSystem.fileExists(this.getName());
    }

    @Override
    public void create() throws IOException {
        fileSystem.put(this);
    }

    @Override
    public void renameTo(VirtualFile other) throws IOException {
        if (other instanceof InMemoryFile) {
            fileSystem.rename(this, (InMemoryFile)other);
        } else {
            throw new IllegalArgumentException("File should be InMemoryFile");
        }
    }

    @Override
    public void delete() throws IOException {
        fileSystem.delete(this);
    }

    @Override
    public long length() throws IOException {
        return createTransactionalFile().length();
    }

    @Override
    public TransactionalVirtualFile createTransactionalFile() throws IOException {
        return fileSystem.createTransactionalFile(this.getName());
    }

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }
}
