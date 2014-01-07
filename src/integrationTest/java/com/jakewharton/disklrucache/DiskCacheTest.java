package com.jakewharton.disklrucache;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class DiskCacheTest {

    private DiskLruCache<SimpleMeta> cache;
    private File base;

    @Before
    public void setUp() throws Exception {
        base = new File("tests");
        cache = new DiskLruCache<>(base, 100);
        FileUtils.deleteDirectory(base);
    }

    @Test
    public void testFillAndReload() throws Exception {
        cache.init();

        for (int i = 10; i <= 20; i++) {
            putString("c" + i, "Content " + i);
        }

        verifyNotExists("c10");
        for (int i = 11; i <= 20; i++) {
            verifyExists("c" + i, "Content " + i);
        }

        cache.close();

        cache = new DiskLruCache<>(base, 90);
        cache.init();

        verifyNotExists("c10");
        verifyNotExists("c11");
        for (int i = 12; i <= 20; i++) {
            verifyExists("c" + i, "Content " + i);
        }
    }

    @Test
    public void testDoublePutAndReload() throws Exception {
        cache.init();

        for (int i = 10; i <= 20; i++) {
            putString("c" + i, "Content " + i);
        }
        for (int i = 10; i <= 20; i++) {
            putString("c" + i, "Content " + i);
        }

        verifyNotExists("c10");
        for (int i = 11; i <= 20; i++) {
            verifyExists("c" + i, "Content " + i);
        }

        cache.close();

        cache = new DiskLruCache<>(base, 100);
        cache.init();

        verifyNotExists("c10");
        for (int i = 11; i <= 20; i++) {
            verifyExists("c" + i, "Content " + i);
        }
    }

    @Test
    public void testPutRemoveAndReload() throws Exception {
        cache.init();

        for (int i = 10; i <= 20; i++) {
            putString("c" + i, "Content " + i);
        }

        cache.remove("c12");
        putString("c21", "Content 21");

        verifyNotExists("c10");
        verifyExists("c11", "Content 11");
        verifyNotExists("c12");
        for (int i = 13; i <= 21; i++) {
            verifyExists("c" + i, "Content " + i);
        }

        cache.close();

        cache = new DiskLruCache<>(base, 100);
        cache.init();

        verifyNotExists("c10");
        verifyExists("c11", "Content 11");
        verifyNotExists("c12");
        for (int i = 13; i <= 21; i++) {
            verifyExists("c" + i, "Content " + i);
        }
    }

    private void putString(String name, String content) throws IOException {
        File tmp = new File("tmp");
        FileUtils.writeStringToFile(tmp, content);
        cache.put(tmp, name, new SimpleMeta(content.length()));
    }

    private void verifyNotExists(String name) throws IOException {
        File file = cache.find(name);
        assertNull("File exists, but shouldn't", file);
    }

    private void verifyExists(String name, String content) throws IOException {
        File file = cache.find(name);
        assertNotNull("File not exists", file);
        assertEquals("Wrong file content", content, FileUtils.readFileToString(file));
    }

    @After
    public void tearDown() throws Exception {
        cache.close();
        FileUtils.deleteDirectory(base);
    }
}
