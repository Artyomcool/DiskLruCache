package com.jakewharton.disklrucache;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.*;

public class TransactionFileTest extends FileTest {

    private TransactionalVirtualFile transactionalFile;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        transactionalFile = realFile.createTransactionalFile();
    }

    @Test
    public void testWriteBytes() throws Exception {
        byte[] bytes = "Content".getBytes();

        transactionalFile.write(bytes);
        transactionalFile.close();

        byte[] buf = new byte[bytes.length];
        transactionalFile = realFile.createTransactionalFile();
        transactionalFile.readFully(buf);

        assertEquals("File has more content", transactionalFile.length(), transactionalFile.pos());
        assertArrayEquals(bytes, buf);
    }

    @Test
    public void testWriteBytes2() throws Exception {
        byte[] bytes = "Content".getBytes();

        transactionalFile.write(bytes, 1, bytes.length - 2);
        transactionalFile.close();

        byte[] buf = new byte[bytes.length];
        buf[0] = '!';
        buf[bytes.length - 1] = '!';
        transactionalFile = realFile.createTransactionalFile();
        transactionalFile.readFully(buf, 1, bytes.length - 2);

        assertEquals("File has more content", transactionalFile.length(), transactionalFile.pos());
        assertArrayEquals("!onten!".getBytes(), buf);
    }

    @Test
    public void testPrimitives() throws Exception {
        byte b = 1;
        int ub = 0xf1;
        short s = 2;
        int us = 0xff0f;
        int i = 3;
        long l = 4;
        float f = 5;
        double d = 6;
        char c = '7';
        String u = "u";
        String chrs = "abc";

        transactionalFile.writeBoolean(true);
        transactionalFile.writeBoolean(false);
        transactionalFile.writeByte(b);
        transactionalFile.write(ub);
        transactionalFile.writeShort(s);
        transactionalFile.writeShort(us);
        transactionalFile.writeInt(i);
        transactionalFile.writeLong(l);
        transactionalFile.writeFloat(f);
        transactionalFile.writeDouble(d);
        transactionalFile.writeChar(c);
        transactionalFile.writeUTF(u);
        transactionalFile.writeChars(chrs);
        transactionalFile.writeBytes(u + "\r");
        transactionalFile.writeBytes(u + "\n");
        transactionalFile.writeBytes(u + "\r\n");
        transactionalFile.writeBytes(u);
        transactionalFile.close();

        transactionalFile = realFile.createTransactionalFile();
        assertTrue(transactionalFile.readBoolean());
        assertFalse(transactionalFile.readBoolean());
        assertEquals(b, transactionalFile.readByte());
        assertEquals(ub, transactionalFile.readUnsignedByte());
        assertEquals(s, transactionalFile.readShort());
        assertEquals(us, transactionalFile.readUnsignedShort());
        assertEquals(i, transactionalFile.readInt());
        assertEquals(l, transactionalFile.readLong());
        assertEquals(f, transactionalFile.readFloat(), 0.000000001);
        assertEquals(d, transactionalFile.readDouble(), 0.000000001);
        assertEquals(c, transactionalFile.readChar());
        assertEquals(u, transactionalFile.readUTF());
        assertEquals(chrs, transactionalFile.readChar() + "" +
                transactionalFile.readChar() + "" +
                transactionalFile.readChar());
        assertEquals(u, transactionalFile.readLine());
        assertEquals(u, transactionalFile.readLine());
        assertEquals(u, transactionalFile.readLine());
        assertEquals(u, transactionalFile.readLine());

        assertEquals("File has more content", transactionalFile.length(), transactionalFile.pos());
    }

    @Test
    public void testLength() throws Exception {
        transactionalFile.write(new byte[]{1, 2, 3, 4});
        assertEquals(4, transactionalFile.length());
        assertEquals(4, file.length());

        transactionalFile.setLength(2);
        assertEquals(2, transactionalFile.length());
        assertEquals(2, file.length());

        transactionalFile.write(new byte[]{5, 6});
        assertEquals(4, transactionalFile.length());
        assertEquals(4, file.length());

        transactionalFile.seek(0);
        byte[] bytes = new byte[4];
        transactionalFile.readFully(bytes);
        assertArrayEquals(new byte[]{1, 2, 5, 6}, bytes);
    }

    @Test
    public void testSeek() throws Exception {
        transactionalFile.write(0);
        transactionalFile.write(1);
        transactionalFile.write(2);
        transactionalFile.write(3);

        assertEquals(4, transactionalFile.length());

        transactionalFile.seek(1);
        assertEquals(1, transactionalFile.readByte());

        assertEquals(1, transactionalFile.skipBytes(1));
        assertEquals(3, transactionalFile.readByte());
    }

    @Test
    public void testStream() throws Exception {
        InputStream inputStream = transactionalFile.getInputStream();
        OutputStream outputStream = transactionalFile.getOutputStream();

        outputStream.write(1);
        byte[] b = {2, 3, 4, 5};
        outputStream.write(b);
        outputStream.write(new byte[]{5, 6, 7}, 1, 2);

        transactionalFile.seek(0);
        assertEquals(7, inputStream.available());

        assertEquals(1, inputStream.read());
        assertEquals(6, inputStream.available());

        byte[] r = new byte[4];
        assertEquals(4, inputStream.read(r));
        assertEquals(2, inputStream.available());
        assertArrayEquals(b, r);

        assertEquals(2, inputStream.read(r, 1, 2));
        assertEquals(0, inputStream.available());
        assertArrayEquals(new byte[]{2, 6, 7, 5}, r);

        transactionalFile.seek(0);
        assertEquals(2, inputStream.skip(2));
        assertEquals(5, inputStream.available());
        assertEquals(3, inputStream.read());
    }

    @Override
    @After
    public void tearDown() throws Exception {
        transactionalFile.close();
        super.tearDown();
    }
}
