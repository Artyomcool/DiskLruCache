package com.jakewharton.disklrucache;

import org.jetbrains.annotations.NotNull;

import java.io.File;

class DirectoryFileSystem implements VirtualFileSystem {

    @NotNull
    private final File baseDir;

    public DirectoryFileSystem(@NotNull File baseDir) {
        this.baseDir = baseDir;
    }

    @NotNull
    @Override
    public VirtualFile get(@NotNull String name) {
        return new RealFile(new File(baseDir, name));
    }
}
