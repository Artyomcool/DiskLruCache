package com.jakewharton.disklrucache;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

class LinkedMapLruCache<Meta> implements CacheInternalAccess<Meta> {

    public final LinkedHashMap<String, Meta> map = new LinkedHashMap<>(16, 0.75f, true);

    @Override
    public Meta putMetaInternal(@NotNull String fileName, @NotNull Meta meta) {
        return map.put(fileName, meta);
    }

    @Override
    public Meta accessInternal(@NotNull String fileName) {
        return map.get(fileName);
    }

    @Override
    public Meta removeInternal(@NotNull String fileName) {
        return map.remove(fileName);
    }

    @Override
    public @NotNull Iterator<Map.Entry<String, Meta>> getEntriesIterator() {
        return map.entrySet().iterator();
    }

    @Override
    public int getEntitiesCount() {
        return map.size();
    }

    @Override
    public boolean containsInternal(String fileName) {
        return map.containsKey(fileName);
    }

}