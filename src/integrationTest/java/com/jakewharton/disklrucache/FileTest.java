package com.jakewharton.disklrucache;

import com.jakewharton.disklrucache.RealFile;
import com.jakewharton.disklrucache.VirtualFile;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.IOException;

public class FileTest {

    File testDir;
    File file;
    VirtualFile realFile;

    @Before
    public void setUp() throws Exception {
        testDir = new File("testDir");
        if (!testDir.exists() && !testDir.mkdirs()) {
            throw new IOException("Can't create directory for tests");
        }
        clean();

        file = new File(testDir, "test");
        realFile = new RealFile(file);
    }

    @After
    public void tearDown() throws Exception {
        clean();
    }

    private void clean() throws IOException {
        File[] files = testDir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (!f.delete()) {
                throw new IOException("Can't delete file " + f);
            }
        }
    }
}
