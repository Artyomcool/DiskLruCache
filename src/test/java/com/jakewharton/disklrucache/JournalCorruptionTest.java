package com.jakewharton.disklrucache;

import com.esotericsoftware.kryo.io.Output;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;

import java.io.IOException;
import java.io.OutputStream;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class JournalCorruptionTest {

    private InMemoryFileSystem fileSystem;
    private CacheMap<String> cache;

    @Before
    public void setUp() throws Exception {
        fileSystem = Mockito.spy(new InMemoryFileSystem());
        cache = new CacheMap<>();
    }

    @Test(expected = IllegalStateException.class)
    public void testTrashSmallHeader() throws Exception {
        InMemoryFile file = fileSystem.get(Journal.JOURNAL_FILE_NAME);
        TransactionalVirtualFile transactionalFile = file.createTransactionalFile();
        transactionalFile.write(10);
        transactionalFile.close();

        new Journal<String>(cache, fileSystem) {
            @Override
            void createEmptyJournal() throws IOException {
                throw new IllegalStateException("Journal dropped");
            }
        }.initJournal();
    }

    @Test(expected = IllegalStateException.class)
    public void testTrashBytesForMagic() throws Exception {
        InMemoryFile file = fileSystem.get(Journal.JOURNAL_FILE_NAME);
        TransactionalVirtualFile transactionalFile = file.createTransactionalFile();
        transactionalFile.createEmpty();
        transactionalFile.write(0);
        transactionalFile.write(1);
        transactionalFile.write(2);
        transactionalFile.commit();
        transactionalFile.close();

        new Journal<String>(cache, fileSystem) {
            @Override
            void createEmptyJournal() throws IOException {
                throw new IllegalStateException("Journal dropped");
            }
        }.initJournal();
    }

    @Test(expected = IllegalStateException.class)
    public void testTrashObjectForMagic() throws Exception {
        testMagic("Trash");
    }

    @Test(expected = IllegalStateException.class)
    public void testCorruptedMagic() throws Exception {
        Journal.MagicLine line = Journal.MagicLine.createLine();
        line.magic = "What?";
        testMagic(line);
    }

    @Test(expected = IllegalStateException.class)
    public void testWrongMagicVersion() throws Exception {
        Journal.MagicLine line = Journal.MagicLine.createLine();
        line.version = 2;
        testMagic(line);
    }

    @Test(expected = CustomIOException.class)
    public void testIoFailOnEmptyJournal1() throws Exception {
        testIoFailOnEmptyJournal(true);
    }

    @Test(expected = CustomIOException.class)
    public void testIoFailOnEmptyJournal2() throws Exception {
        testIoFailOnEmptyJournal(false);
    }

    private void testIoFailOnEmptyJournal(final boolean throwOnce) throws Exception {
        new Journal<String>(cache, fileSystem) {
            @Override
            void createEmptyJournal() throws IOException {

                doAnswer(
                        new Answer() {
                            @Override
                            public Object answer(InvocationOnMock invocation) throws Throwable {
                                TransactionalVirtualFile file = (TransactionalVirtualFile) invocation.callRealMethod();
                                file = spy(file);

                                OutputStream outputStream = spy(new OutputStream() {
                                    @Override
                                    public void write(int b) throws IOException {
                                        throw new RuntimeException("Should never happen");
                                    }
                                });
                                Stubber stubber = doThrow(CustomIOException.class);
                                if (throwOnce) {
                                    stubber.doNothing();
                                } else {
                                    stubber.doThrow(IOException.class);
                                }
                                stubber.when(outputStream).write(anyInt());

                                doReturn(outputStream).when(file).getOutputStream();
                                return file;
                            }
                        })
                        .doCallRealMethod()
                        .when(fileSystem).createTransactionalFile(JOURNAL_FILE_NAME);
                super.createEmptyJournal();
            }
        }.initJournal();
    }

    @Test(expected = CustomIOException.class)
    public void testFailOnEmptyWhileOpening() throws Exception {
        fileSystem.clear();

        doThrow(CustomIOException.class)
                .doCallRealMethod()
                .when(fileSystem)
                .createTransactionalFile(Journal.JOURNAL_FILE_NAME);

        new Journal<>(cache, fileSystem).initJournal();
    }

    @Test
    //TODO simplify or split or do something with copy-paste
    public void testRestore() throws Exception {
        fileSystem.clear();
        try {
            testTrashBytesForMagic();
        } catch (IllegalStateException ignored) {
        }
        new Journal<>(cache, fileSystem).initJournal();

        fileSystem.clear();
        try {
            testTrashObjectForMagic();
        } catch (IllegalStateException ignored) {
        }
        new Journal<>(cache, fileSystem).initJournal();

        fileSystem.clear();
        try {
            testCorruptedMagic();
        } catch (IllegalStateException ignored) {
        }
        new Journal<>(cache, fileSystem).initJournal();

        fileSystem.clear();
        try {
            testWrongMagicVersion();
        } catch (IllegalStateException ignored) {
        }
        new Journal<>(cache, fileSystem).initJournal();

        fileSystem.clear();
        try {
            testIoFailOnEmptyJournal1();
        } catch (IOException ignored) {
        }
        new Journal<>(cache, fileSystem).initJournal();

        fileSystem.clear();
        try {
            testIoFailOnEmptyJournal2();
        } catch (IOException ignored) {
        }
        new Journal<>(cache, fileSystem).initJournal();

        fileSystem.clear();
        try {
            testFailOnEmptyWhileOpening();
        } catch (IOException ignored) {
        }
        new Journal<>(cache, fileSystem).initJournal();
    }

    @Test
    public void testRestoreFromBackup() throws Exception {
        Journal<String> journal = new Journal<>(cache, fileSystem);
        journal.initJournal();
        journal.put("k", "m", false);
        journal.close();

        assertTrue("Strange - cache should be empty", cache.map.isEmpty());

        fileSystem.get(Journal.JOURNAL_FILE_NAME).renameTo(fileSystem.get(Journal.JOURNAL_TMP_FILE_NAME));
        new Journal<>(cache, fileSystem).initJournal();

        cache.assertEquals(new String[][]{{"k", "m"}});
        assertEquals("Should be exactly one file on the FS", 1, fileSystem.count());
        assertFalse("Temp file shouldn't be presented anymore", fileSystem.get(Journal.JOURNAL_TMP_FILE_NAME).exists());
    }

    private void testMagic(Object magic) throws IOException {
        InMemoryFile file = fileSystem.get(Journal.JOURNAL_FILE_NAME);
        TransactionalVirtualFile transactionalFile = file.createTransactionalFile();
        transactionalFile.createEmpty();
        Output output = new Output(transactionalFile.getOutputStream());
        Journal.createKryo().writeObject(output, magic);
        output.close();
        transactionalFile.commit();
        transactionalFile.close();

        new Journal<String>(cache, fileSystem) {
            @Override
            void createEmptyJournal() throws IOException {
                super.createEmptyJournal();
                throw new IllegalStateException("Journal dropped");
            }
        }.initJournal();
    }

    private static class CustomIOException extends IOException {
    }
}
