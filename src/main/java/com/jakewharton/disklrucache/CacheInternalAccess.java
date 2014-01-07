package com.jakewharton.disklrucache;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;

interface CacheInternalAccess<Meta> {
    Meta putMetaInternal(@NotNull String fileName, @NotNull Meta meta);
    Meta accessInternal(@NotNull String fileName);
    Meta removeInternal(@NotNull String fileName);
    @NotNull Iterator<Map.Entry<String,Meta>> getEntriesIterator();
    int getEntitiesCount();
    boolean containsInternal(String fileName);
}
