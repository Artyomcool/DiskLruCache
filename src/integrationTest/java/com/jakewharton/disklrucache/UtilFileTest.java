package com.jakewharton.disklrucache;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class UtilFileTest {

    private File base;
    private File tmp;

    @Before
    public void setUp() throws Exception {
        base = new File("tests");
        if (base.exists()) {
            FileUtils.forceDelete(base);
        }
        tmp = new File(base, "tmp");
    }

    @Test
    public void testCreateDirectory() throws Exception {
        Util.ensureExists(tmp);
        if (!tmp.exists()) {
            throw new IOException("Can't create directory");
        }
    }

    @Test
    public void testExistsDirectory() throws Exception {
        if (!tmp.mkdirs()) {
            throw new IOException("Can't create directory");
        }

        Util.ensureExists(tmp);

        if (!tmp.exists()) {
            throw new IOException("Can't create directory");
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test(expected = IOException.class)
    public void testFail() throws Exception {
        base.mkdirs();
        tmp.createNewFile();
        Util.ensureExists(tmp);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test(expected = IOException.class)
    public void testFail2() throws Exception {
        base.createNewFile();
        Util.ensureExists(tmp);
    }

    @After
    public void tearDown() throws Exception {
        if (base.exists()) {
            FileUtils.forceDelete(base);
        }
    }
}
