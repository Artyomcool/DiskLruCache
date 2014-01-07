package com.jakewharton.disklrucache;

import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.Assert.*;

public class RealFileTest extends FileTest {

    @Test
    public void testCreateFile() throws Exception {
        assertFalse(file.exists());
        assertFalse(realFile.exists());

        realFile.create();

        assertTrue(file.exists());
        assertTrue(realFile.exists());
    }

    @Test(expected = IOException.class)
    public void testFailCreateFile() throws Exception {
        if (file.mkdir()) {
            realFile.create();
        }
    }

    @Test(expected = IOException.class)
    public void testDoubleCreate() throws Exception {
        realFile.create();
        realFile.create();
    }

    @Test
    public void testRename() throws Exception {
        realFile.create();
        testRenameNotExists();
    }

    @Test(expected = IOException.class)
    public void testRenameNotExists() throws Exception {
        realFile.renameTo(new RealFile(new File(testDir, "test2")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRenameToStrangeFile() throws Exception {
        realFile.renameTo(Mockito.mock(VirtualFile.class));
    }

    @Test(expected = IOException.class)
    public void testDeleteNotExists() throws Exception {
        realFile.delete();
    }

    @Test
    public void testDelete() throws Exception {
        realFile.create();
        realFile.delete();

        assertFalse(file.exists());
        assertFalse(realFile.exists());
    }

    @Test
    public void testLength() throws Exception {
        realFile.create();
        assertEquals(0, realFile.length());

        byte[] bytes = "Test".getBytes();

        FileOutputStream stream = new FileOutputStream(file);
        stream.write(bytes);
        stream.close();

        assertEquals(bytes.length, realFile.length());
    }

    @Test
    public void testCreateTransactionalAndClose() throws Exception {
        realFile.createTransactionalFile().close();
    }

    @Test(expected = IOException.class)
    public void testCreateTransactionalFail() throws Exception {
        if (file.mkdir()) {
            realFile.createTransactionalFile().close();
        }
    }
}
