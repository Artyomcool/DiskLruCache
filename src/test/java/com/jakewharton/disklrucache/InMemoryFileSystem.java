package com.jakewharton.disklrucache;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class InMemoryFileSystem implements VirtualFileSystem {

    private Map<String, InMemoryFileRecord> files = new HashMap<>();
    private Map<String, InMemoryFile> filesToReuse = new HashMap<>();

    @NotNull
    @Override
    public InMemoryFile get(String name) {
        return createFile(name);
    }

    public void rename(InMemoryFile file, InMemoryFile other) throws IOException {
        InMemoryFileRecord record = files.remove(file.getName());
        if (record == null) {
            throw new IOException("File not found");
        }
        file.setName(other.getName());
        files.put(file.getName(), record);
    }

    public void delete(InMemoryFile file) throws IOException {
        if (files.remove(file.getName()) == null) {
            throw new IOException("File doesn't exist: " + file.getName());
        }
    }

    protected InMemoryFile createFile(String name) {
        return new InMemoryFile(this, name);
    }

    public int count() {
        return files.size();
    }

    public void clear() {
        files.clear();
    }

    public void put(InMemoryFile file) throws IOException {
        if (files.containsKey(file.getName())) {
            throw new IOException("File already exists");
        }
        files.put(file.getName(), new InMemoryFileRecord());
    }

    public TransactionalVirtualFile createTransactionalFile(String name) throws IOException {
        InMemoryFileRecord record = files.get(name);
        if (record == null) {
            put(createFile(name));
            record = files.get(name);
        }
        if (record.file == null) {
            record.file = new TransactionalInMemoryFile();
        }
        record.file.seek(0);
        return record.file;
    }

    public boolean fileExists(String name) {
        return files.containsKey(name);
    }

    private static class InMemoryFileRecord {
        TransactionalInMemoryFile file;
    }
}
