package com.jakewharton.disklrucache;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public class UtilTest extends Util {
    @Test
    public void testClose() throws Exception {
        Closeable closeable = Mockito.mock(Closeable.class);
        Util.closeQuietly(closeable);

        Mockito.verify(closeable).close();
    }

    @Test
    public void testCloseNull() throws Exception {
        Util.closeQuietly(null);
    }

    @Test
    public void testCloseIO() throws Exception {
        Closeable closeable = Mockito.mock(Closeable.class);
        Mockito.doThrow(IOException.class).when(closeable).close();

        Util.closeQuietly(closeable);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRuntimeException() throws Exception {
        Closeable closeable = Mockito.mock(Closeable.class);
        Mockito.doThrow(IllegalArgumentException.class).when(closeable).close();

        Util.closeQuietly(closeable);
    }
}
