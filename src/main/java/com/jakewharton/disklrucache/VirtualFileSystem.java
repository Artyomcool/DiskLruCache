package com.jakewharton.disklrucache;

import org.jetbrains.annotations.NotNull;

interface VirtualFileSystem {
    @NotNull VirtualFile get(String name);
}
