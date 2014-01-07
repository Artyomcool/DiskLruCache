package com.jakewharton.disklrucache;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TransactionVirtualFileTest {

    private TransactionalInMemoryFile file;

    @Before
    public void setUp() throws Exception {
        file = new TransactionalInMemoryFile();
    }

    @Test
    public void testHeaderSize() throws Exception {
        file.createEmpty();
        Assert.assertEquals("Wrong header size", TransactionalVirtualFile.HEADER_SIZE, file.length());
    }

    @Test
    public void testCorruptedSize() throws Exception {
        for (int i = 0; i < TransactionalVirtualFile.HEADER_SIZE; i++) {
            file.write(i);
        }
        byte[] bytes = "File content".getBytes();
        file.write(bytes);
        file.seek(0);
        file.init();

        byte[] restoredBytes = new byte[bytes.length];
        file.readFully(restoredBytes);
        Assert.assertArrayEquals("Wrong content restored", bytes, restoredBytes);
        Assert.assertEquals("File has extra content", file.length(), file.pos());
    }

    @Test
    public void testCorruptedRecord() throws Exception {
        file.createEmpty();

        byte[] bytes = "File content".getBytes();
        file.write(bytes);
        file.commit();

        file.writeBytes("Trash");

        file.seek(0);
        file.init();

        byte[] restoredBytes = new byte[bytes.length];
        file.readFully(restoredBytes);
        Assert.assertArrayEquals("Wrong content restored", bytes, restoredBytes);
        Assert.assertEquals("File has extra content", file.length(), file.pos());
    }
}
