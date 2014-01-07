package com.jakewharton.disklrucache;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class JournalMetaTest {

    private CacheMap<Meta> cache;
    private Journal<Meta> journal;
    private InMemoryFileSystem fileSystem;

    @Before
    public void setUp() throws Exception {
        cache = new CacheMap<>();
        fileSystem = new InMemoryFileSystem();
        journal = new Journal<>(cache, fileSystem);
        journal.initJournal();
    }

    @Test
    public void testPut() throws Exception {

        List<Meta> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Meta meta = new Meta();
            list.add(meta);
            journal.put("k" + i, meta, false);
        }

        for (int i = 0; i < 10; i++) {
            journal.put("k2_" + i, list.get(i), false);
        }

        journal.close();

        new Journal<>(cache, fileSystem).initJournal();

        List<Meta> loadedList = new ArrayList<>(cache.map.values());
        for (int i = 0; i < 10; i++) {
            Assert.assertNotSame("Looks like Kryo references wasn't cleared", list.get(i), loadedList.get(i));
            Assert.assertEquals("Wrong meta deserialized", list.get(i).tag, loadedList.get(i).tag);
            Assert.assertSame("Should be guaranteed if serialized objects the same, then deserialized also the same",
                    loadedList.get(i), loadedList.get(i + 10));
        }
    }

    private static class Meta {
        private static AtomicInteger counter = new AtomicInteger(0);
        private Integer tag = counter.getAndIncrement();
    }

}
