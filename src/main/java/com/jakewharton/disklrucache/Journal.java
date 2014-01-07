package com.jakewharton.disklrucache;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;
import com.esotericsoftware.kryo.util.MapReferenceResolver;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class Journal<Meta> {

    static final String JOURNAL_FILE_NAME = "journal";
    static final String JOURNAL_TMP_FILE_NAME = "journal.tmp";
    static final int MAX_REDUNDANT_OPERATION_COUNT = 2048;

    @NotNull
    private Kryo kryo;
    @NotNull
    private final VirtualFile journal;
    @NotNull
    private final CacheInternalAccess<Meta> cache;
    @NotNull
    private final VirtualFileSystem fileSystem;
    private TransactionalVirtualFile journalFile;
    private Output journalOutput;
    private int redundantOpCount;

    Journal(@NotNull CacheInternalAccess<Meta> cache, @NotNull VirtualFileSystem fileSystem) {
        this.cache = cache;
        this.fileSystem = fileSystem;
        kryo = createKryo();

        journal = fileSystem.get(JOURNAL_FILE_NAME);
    }

    public void close() {
        if (journalOutput != null) {
            try {
                journalOutput.close();
            } catch (KryoException e) {
                //e.printStackTrace();//TODO log
            }
        }
        if (journalFile != null) {
            journalFile.close();
        }
    }

    public int getRedundantOperations() {
        return redundantOpCount;
    }

    public void remove(String fileName) throws IOException {
        RemoveLine remove = new RemoveLine();
        logToJournal(remove, fileName);
        incrementRedundant(true);
    }

    public void access(String fileName, boolean allowRebuild) throws IOException {
        AccessLine read = new AccessLine();
        logToJournal(read, fileName);
        incrementRedundant(allowRebuild);
    }

    public void put(String fileName, Meta meta, boolean exists) throws IOException {
        PutLine putLine = new PutLine();
        putLine.meta = meta;
        logToJournal(putLine, fileName);
        if (exists) {
            incrementRedundant(true);
        }
    }

    public void initJournal() throws IOException {
        try {
            if (!journal.exists()){
                VirtualFile backup = fileSystem.get(JOURNAL_TMP_FILE_NAME);
                if (backup.exists()) {
                    backup.renameTo(journal);
                } else {
                    createEmptyJournal();
                    return;
                }
            }
            if (journal.length() < TransactionalFile.HEADER_SIZE) {
                createEmptyJournal();
                return;
            }
        } catch (IOException e) {
            close();
            throw e;
        }

        journalFile = journal.createTransactionalFile();
        try {
            init();
        } catch (@NotNull IOException | RuntimeException e) {
            //e.printStackTrace();    //TODO log
            createEmptyJournal();
        }
    }

    void createEmptyJournal() throws IOException {
        if (journalFile != null) {
            journalFile.close();
        }
        kryo.reset();
        createNewJournal(journal, kryo, false, true);
    }

    private void init() throws IOException, KryoException {
        journalFile.init();
        Input input = new Input(journalFile.getInputStream());
        try {
            try {
                MagicLine magicLine = kryo.readObject(input, MagicLine.class);
                magicLine.verify();
            } catch (RuntimeException e) {
                throw new IOException(e);
            }
            int linesCount = 0;
            while (!input.eof()) {
                @SuppressWarnings("unchecked")
                Line<Meta> line = (Line<Meta>) kryo.readClassAndObject(input);
                line.run(cache);
                linesCount++;
            }
            journalOutput = new Output(journalFile.getOutputStream());
            redundantOpCount = linesCount - cache.getEntitiesCount();
        } finally {
            Util.closeQuietly(input);
        }
    }

    private void createNewJournal(@NotNull VirtualFile file, @NotNull Kryo kryo, boolean writeLines, boolean assignOutput)
            throws IOException {

        recreate(file);
        TransactionalVirtualFile tmpJournal;
        Output output;
        if (assignOutput) {
            reopenJournal();
            tmpJournal = journalFile;
            output = journalOutput;
        } else {
            tmpJournal = file.createTransactionalFile();
            output = new Output(tmpJournal.getOutputStream());
        }
        try {
            try {
                tmpJournal.createEmpty();
                kryo.writeObject(output, MagicLine.createLine());
                if (writeLines) {
                    writeLines(kryo, output);
                }
                output.flush();
                tmpJournal.commit();
            } catch (KryoException e) {
                throwIOException(e);
            }
        } catch (IOException e) {
            try {
                Util.closeQuietly(output);
            } catch (KryoException ex) {
                //ex.printStackTrace();//TODO log
            }
            tmpJournal.close();
            file.delete();
            throw e;
        }
        if (!assignOutput) {
            output.close();
            tmpJournal.close();
        }
    }

    private void compactJournal() throws IOException {
        VirtualFile file = fileSystem.get(JOURNAL_TMP_FILE_NAME);
        Kryo kryo = createKryo();
        createNewJournal(file, kryo, true, false);

        this.kryo = kryo;
        journal.delete();
        file.renameTo(journal);
        reopenJournal();

        redundantOpCount = 0;
    }

    private static void recreate(@NotNull VirtualFile file) throws IOException {
        if (file.exists()) {
            file.delete();
        }
        file.create();
    }

    private void incrementRedundant(boolean allowRebuild) throws IOException {
        redundantOpCount++;
        if (allowRebuild && redundantOpCount > MAX_REDUNDANT_OPERATION_COUNT) {
            compactJournal();
        }
    }

    private void reopenJournal() throws IOException {
        journalFile = journal.createTransactionalFile();
        journalOutput = new Output(journalFile.getOutputStream());
    }

    private void writeLines(@NotNull Kryo kryo, Output output) {
        Iterator<Map.Entry<String, Meta>> iterator = cache.getEntriesIterator();
        PutLine<Meta> line = new PutLine<>();
        while(iterator.hasNext()) {
            Map.Entry<String, Meta> entry = iterator.next();
            line.fileName = entry.getKey();
            line.meta = entry.getValue();
            kryo.writeClassAndObject(output, line);
        }
    }

    private void logToJournal(@NotNull Line line, String fileName) throws IOException {
        line.fileName = fileName;
        try {
            kryo.writeClassAndObject(journalOutput, line);
            journalOutput.flush();
        } catch (KryoException e) {
            throwIOException(e);
        }
        journalFile.commit();
    }

    @NotNull
    static Kryo createKryo() {
        Kryo kryo = new Kryo(new MapReferenceResolver() {
            @Override
            public boolean useReferences(@NotNull Class type) {
                return super.useReferences(type) && !Line.class.isAssignableFrom(type);
            }
        });
        kryo.setDefaultSerializer(CompatibleFieldSerializer.class);
        kryo.register(MagicLine.class);
        kryo.register(Line.class);
        kryo.register(RemoveLine.class);
        kryo.register(PutLine.class);
        kryo.register(AccessLine.class);
        kryo.setAutoReset(false);
        return kryo;
    }

    static void throwIOException(@NotNull KryoException e) throws IOException {
        if (e.getCause() instanceof IOException) {
            throw (IOException)e.getCause();
        }
        throw new IOException(e);
    }

    static class MagicLine {

        private static final String MAGIC = "DiskLruCacheJournal";
        private static final int VERSION = 1;

        String magic;
        int version;

        public void verify() throws IOException {
            if (!MAGIC.equals(magic)) {
                throw new IOException("Wrong magic: " + magic);
            }
            if (version != VERSION) {
                throw new IOException("Wrong version: " + version);
            }
        }

        @NotNull
        public static MagicLine createLine() {
            MagicLine line = new MagicLine();
            line.version = VERSION;
            line.magic = MAGIC;
            return line;
        }
    }

    private static abstract class Line<Meta> {
        String fileName;
        public abstract void run(CacheInternalAccess<Meta> cache) throws IOException;
    }

    private static class RemoveLine<Meta> extends Line<Meta> {
        @Override
        public void run(@NotNull CacheInternalAccess<Meta> cache) throws IOException {
            cache.removeInternal(fileName);
        }
    }

    private static class AccessLine<Meta> extends Line<Meta> {
        @Override
        public void run(@NotNull CacheInternalAccess<Meta> cache) throws IOException {
            cache.accessInternal(fileName);
        }
    }

    private static class PutLine<Meta> extends Line<Meta> {
        private Meta meta;
        @Override
        public void run(@NotNull CacheInternalAccess<Meta> cache) throws IOException {
            cache.putMetaInternal(fileName, meta);
        }
    }

}
