package com.jakewharton.disklrucache;

import java.io.File;

public class SimpleMeta implements FileMeta {

    private final long weight;

    public SimpleMeta() {
        weight = 0;
    }

    public SimpleMeta(long weight) {
        this.weight = weight;
    }

    public SimpleMeta(File file) {
        this.weight = file.length();
    }

    @Override
    public long weight() {
        return weight;
    }
}
