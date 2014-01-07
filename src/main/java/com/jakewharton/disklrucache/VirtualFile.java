package com.jakewharton.disklrucache;

import java.io.IOException;

interface VirtualFile {
    boolean exists();
    void create() throws IOException;
    void renameTo(VirtualFile other) throws IOException;
    void delete() throws IOException;
    long length() throws IOException;
    TransactionalVirtualFile createTransactionalFile() throws IOException;
}
