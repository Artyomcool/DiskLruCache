package com.jakewharton.disklrucache;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

class RealFile implements VirtualFile {

    @NotNull
    private final File file;

    public RealFile(@NotNull File file) {
        this.file = file;
    }

    @Override
    public boolean exists() {
        return file.exists();
    }

    @Override
    public void create() throws IOException {
        if (!file.createNewFile()) {
            throw new IOException("Can't create file: " + file.getName());
        }
    }

    @Override
    public void renameTo(VirtualFile dest) throws IOException {
        if (dest instanceof RealFile) {
            File file = ((RealFile) dest).file;
            rename(this.file, file);
        } else {
            throw new IllegalArgumentException("Only RealFile allowed");
        }
    }

    @Override
    public void delete() throws IOException {
        delete(file);
    }

    @Override
    public long length() {
        return file.length();
    }

    @Override
    public @NotNull TransactionalFile createTransactionalFile() throws IOException {
        return new TransactionalFile(file);
    }

    public static void delete(@NotNull File file) throws IOException {
        if (!file.delete()) {
            throw new IOException("Can't delete file: " + file.getName());
        }
    }

    public static void deleteIfExists(@Nullable File file) throws IOException {
        if (file == null || !file.exists()) {
            return;
        }
        delete(file);
    }

    public static void rename(@NotNull File file1, @NotNull File file2) throws IOException {
        if (!file1.renameTo(file2)) {
            throw new IOException("Can't rename file " + file1.getName() + " to " +file2.getName());
        }
    }

}
