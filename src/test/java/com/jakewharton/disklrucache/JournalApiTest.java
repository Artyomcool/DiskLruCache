package com.jakewharton.disklrucache;

import com.esotericsoftware.kryo.KryoException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.ProtocolException;

public class JournalApiTest {

    private CacheMap<String> cache;
    private InMemoryFileSystem fileSystem;
    private Journal<String> journal;

    @Before
    public void setUp() throws Exception {
        cache = new CacheMap<>();
        fileSystem = new InMemoryFileSystem();
        journal = new Journal<>(cache, fileSystem);
        journal.initJournal();
    }

    @Test
    public void testEmptyJournal() throws Exception {
        journal.close();

        Assert.assertEquals("Wrong file count in the FS", 1, fileSystem.count());

        long length = fileSystem.get(Journal.JOURNAL_FILE_NAME).length();

        new Journal<>(cache, fileSystem).close();

        Assert.assertEquals("Wrong file count in the FS", 1, fileSystem.count());

        long length2 = fileSystem.get(Journal.JOURNAL_FILE_NAME).length();
        Assert.assertEquals("File changed after reopening", length, length2);
    }

    @Test
    public void testReCreateEmptyJournal() throws Exception {
        journal.createEmptyJournal();
        testPut();
    }

    @Test
    public void testPut() throws Exception {
        for (int i = 0; i < 10; i++) {
            journal.put("k" + i, "m" + i, false);
        }

        journal.put("k3", "m32", true);
        journal.put("k4", "m4", true);

        journal.close();

        new Journal<>(cache, fileSystem).initJournal();

        String[][] expected = {
                {"k0", "m0"},
                {"k1", "m1"},
                {"k2", "m2"},
                {"k5", "m5"},
                {"k6", "m6"},
                {"k7", "m7"},
                {"k8", "m8"},
                {"k9", "m9"},
                {"k3", "m32"},
                {"k4", "m4"},
        };

        cache.assertEquals(expected);
    }

    @Test
    public void testCompactJournal() throws Exception {
        cache.map.put("k", "m");
        journal.put("k", "m", false);

        cache.map.put("a", "m1");
        journal.put("a", "m1", false);

        cache.map.put("b", "m2");
        journal.put("b", "m2", false);

        for (int i = 0; i < Journal.MAX_REDUNDANT_OPERATION_COUNT; i++) {
            cache.map.put("k", "m");
            journal.put("k", "m", true);
        }
        Assert.assertEquals("There should be exactly " + Journal.MAX_REDUNDANT_OPERATION_COUNT +
                " redundant operations", Journal.MAX_REDUNDANT_OPERATION_COUNT, journal.getRedundantOperations());

        journal.put("k", "m", true);

        Assert.assertEquals("There shouldn't be any redundant operations", 0, journal.getRedundantOperations());

        journal.close();

        journal = new Journal<>(cache, fileSystem);
        Assert.assertEquals("There shouldn't be any redundant operations", 0, journal.getRedundantOperations());

        String[][] expected = {
                {"a", "m1"},
                {"b", "m2"},
                {"k", "m"},
        };
        cache.assertEquals(expected);
    }

    @Test
    public void testAccess() throws Exception {
        for (int i = 0; i < 10; i++) {
            journal.put("k" + i, "m" + i, false);
        }

        journal.access("k0", true);
        journal.access("k1", false);

        String[][] expected = {
                {"k2", "m2"},
                {"k3", "m3"},
                {"k4", "m4"},
                {"k5", "m5"},
                {"k6", "m6"},
                {"k7", "m7"},
                {"k8", "m8"},
                {"k9", "m9"},
                {"k0", "m0"},
                {"k1", "m1"},
        };

        journal.close();

        new Journal<>(cache, fileSystem).initJournal();

        cache.assertEquals(expected);
    }

    @Test
    public void testRemove() throws Exception {
        for (int i = 0; i < 10; i++) {
            journal.put("k" + i, "m" + i, false);
        }

        journal.remove("k5");
        journal.remove("k0");
        journal.remove("k9");

        String[][] expected = {
                {"k1", "m1"},
                {"k2", "m2"},
                {"k3", "m3"},
                {"k4", "m4"},
                {"k6", "m6"},
                {"k7", "m7"},
                {"k8", "m8"},
        };

        journal.close();

        new Journal<>(cache, fileSystem).initJournal();

        cache.assertEquals(expected);
    }

    @Test(expected = IOException.class)
    public void testThrowCause() throws Exception {
        KryoException e = new KryoException(new RuntimeException());
        Journal.throwIOException(e);
    }

    @Test(expected = ProtocolException.class)
    public void testThrowCause2() throws Exception {
        KryoException e = new KryoException(new ProtocolException());
        Journal.throwIOException(e);
    }
}
