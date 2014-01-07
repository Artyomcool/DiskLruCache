package com.jakewharton.disklrucache;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.jakewharton.disklrucache.RealFile.*;
import static com.jakewharton.disklrucache.Util.ensureExists;

public class DiskLruCache<Meta extends FileMeta> {

    @NotNull
    private final Journal<Meta> journal;
    @NotNull
    final LinkedMapLruCache<Meta> cache;
    @NotNull
    private final File baseDir;
    @NotNull
    private final File backupDir;
    @NotNull
    private final File journalDir;

    private final long maxWeight;
    final AtomicLong weight = new AtomicLong(0);

    public DiskLruCache(@NotNull File cacheDir, long maxWeight) {
        this(new File(cacheDir, "journal"),
                new File(cacheDir, "files"),
                new File(cacheDir, "backup"),
                maxWeight);
    }

    DiskLruCache(@NotNull File journalDir, @NotNull File baseDir,
                 @NotNull File backupDir, long maxWeight) {
        this.journalDir = journalDir;
        this.baseDir = baseDir;
        this.backupDir = backupDir;
        this.cache = new LinkedMapLruCache<>();
        this.journal = new Journal<>(cache, new DirectoryFileSystem(journalDir));
        this.maxWeight = maxWeight;
    }

    public synchronized void init() throws IOException {
        initDirectories();
        journal.initJournal();
        initFiles();
        initSize();
        checkSize();
    }

    public synchronized void close() {
        journal.close();
    }

    public synchronized void renewMeta(@NotNull String fileName, @NotNull Meta meta) throws IOException {
        Meta old = cache.putMetaInternal(fileName, meta);
        if (old == null) {
            throw new IllegalArgumentException("It is supposed, that file exists in cache: " + fileName);
        }
        changeSize(meta, old);
        putJournal(fileName, meta, false);
    }

    public synchronized void put(@NotNull File file, @NotNull String newName, @NotNull Meta meta) throws IOException {
        File newFile = new File(baseDir, newName);
        File backupFile = null;
        if (newFile.exists()) {
            backupFile = new File(backupDir, newName);
            rename(newFile, backupFile);
        }

        Meta old = cache.putMetaInternal(newName, meta);
        changeSize(meta, old);
        putJournal(newName, meta, old != null);

        //TODO we have inconsistent result here (meta and file doesn't match)
        //(if the following line will not be executed)
        //looks like he only thing we can do - document it
        //another way - use last modified file timestamp as version marker
        rename(file, newFile);
        deleteIfExists(backupFile);
    }

    public synchronized @Nullable File find(@NotNull String fileName) throws IOException {
        if (cache.containsInternal(fileName)) {
            cache.accessInternal(fileName);
            accessJournalDefaultRebuild(fileName);
            return new File(baseDir, fileName);
        } else {
            return null;
        }
    }

    public synchronized boolean remove(@NotNull String fileName) throws IOException {
        Meta old = cache.removeInternal(fileName);
        if (old != null) {
            removeJournal(fileName);
            deleteIfExists(new File(baseDir, fileName));
            changeSize(null, old);
            return true;
        }
        return false;
    }

    public synchronized boolean contains(@NotNull String fileName) {
        return cache.containsInternal(fileName);
    }

    //TODO document that you can't use journal operations without synchronization
    protected void putJournal(@NotNull String fileName, @NotNull Meta meta, boolean exists) throws IOException {
        journal.put(fileName, meta, exists);
        checkSize();
    }

    protected void accessJournalDefaultRebuild(@NotNull String fileName) throws IOException {
        accessJournal(fileName, false);
        checkSize();
    }

    protected void accessJournal(@NotNull String fileName, boolean allowRebuild) throws IOException {
        journal.access(fileName, allowRebuild);
        checkSize();
    }

    protected void removeJournal(@NotNull String fileName) throws IOException {
        journal.remove(fileName);
        checkSize();
    }

    private void initDirectories() throws IOException {
        ensureExists(journalDir);
        ensureExists(baseDir);
        ensureExists(backupDir);
    }

    private void changeSize(@Nullable Meta newMeta, @Nullable Meta oldMeta) {
        long dif = 0;
        if (newMeta != null) {
            dif += newMeta.weight();
        }
        if (oldMeta != null) {
            dif -= oldMeta.weight();
        }
        weight.addAndGet(dif);
    }

    private void initFiles() throws IOException {
        File[] files = backupDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!contains(file.getName())) {
                    delete(file);
                }
            }
        }

        files = backupDir.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if (contains(name)) {
                    File brokenFile = new File(baseDir, name);
                    if (brokenFile.exists()) {
                        delete(brokenFile);
                    }
                    rename(file, brokenFile);
                } else {
                    delete(file);
                }
            }
        }
    }

    protected void initSize() {
        Iterator<Map.Entry<String, Meta>> iterator = cache.getEntriesIterator();
        while (iterator.hasNext()) {
            weight.addAndGet(iterator.next().getValue().weight());
        }
    }

    protected void checkSize() throws IOException {
        long currentWeight = weight.get();
        if (currentWeight > maxWeight) {
            evict();
        }
    }

    private void evict() throws IOException {
        Iterator<Map.Entry<String, Meta>> iterator = cache.getEntriesIterator();
        while (iterator.hasNext() && weight.get() > maxWeight) {
            Map.Entry<String, Meta> next = iterator.next();
            weight.addAndGet(-next.getValue().weight());
            iterator.remove();
            journal.remove(next.getKey());
            deleteIfExists(new File(baseDir, next.getKey()));
        }
    }

}
