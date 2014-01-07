package com.jakewharton.disklrucache;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AsyncDiskLruCache<Meta extends FileMeta> extends DiskLruCache<Meta> {

    @NotNull
    private final SyncMode mode;

    @NotNull
    private Executor executor = createExecutor();

    public AsyncDiskLruCache(@NotNull File cacheDir, long maxWeight, @NotNull SyncMode mode) {
        super(cacheDir, maxWeight);
        this.mode = mode;
    }

    @Override
    public void init() throws IOException {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    AsyncDiskLruCache.super.init();
                } catch (IOException e) {
                    onIOException(e);
                }
            }
        };
        executeAsync(task);
    }

    @Override
    public void close() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                AsyncDiskLruCache.super.close();
            }
        };
        executeAsync(task);
    }

    @Override
    protected void putJournal(@NotNull final String fileName, @NotNull final Meta meta, final boolean exists) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    AsyncDiskLruCache.super.putJournal(fileName, meta, exists);
                } catch (IOException e) {
                    onIOException(e);
                }
            }
        };
        if (mode.isSyncForPut()) {
            executeSync(task);
        } else {
            executeAsync(task);
        }
    }

    @Override
    protected void accessJournalDefaultRebuild(@NotNull final String fileName) {
        accessJournal(fileName, true);
    }

    @Override
    protected void accessJournal(@NotNull final String fileName, final boolean allowRebuild) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    AsyncDiskLruCache.super.accessJournal(fileName, allowRebuild);
                } catch (IOException e) {
                    onIOException(e);
                }
            }
        };
        executeAsync(task);
    }

    @Override
    protected void removeJournal(@NotNull final String fileName) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    AsyncDiskLruCache.super.removeJournal(fileName);
                } catch (IOException e) {
                    onIOException(e);
                }
            }
        };
        if (mode.isSyncForRemove()) {
            executeSync(task);
        } else {
            executeAsync(task);
        }
    }

    protected void executeAsync(Runnable task) {
        executor.execute(task);
    }

    protected void executeSync(final Runnable task) {
        final CountDownLatch done = new CountDownLatch(1);
        Runnable wrap = new Runnable() {
            @Override
            public void run() {
                task.run();
                done.countDown();
            }
        };
        executor.execute(wrap);
        try {
            done.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected void onIOException(@NotNull IOException e) {
        e.printStackTrace();
    }

    @NotNull
    Executor createExecutor() {
        return Executors.newSingleThreadExecutor();
    }

    public static enum SyncMode {
        Async(false, false),
        SyncForPut(true, false),
        SyncForModify(true, true);

        private final boolean syncForPut;
        private final boolean syncForRemove;

        SyncMode(boolean syncForPut, boolean syncForRemove) {
            this.syncForPut = syncForPut;
            this.syncForRemove = syncForRemove;
        }

        public boolean isSyncForPut() {
            return syncForPut;
        }

        public boolean isSyncForRemove() {
            return syncForRemove;
        }
    }
}
