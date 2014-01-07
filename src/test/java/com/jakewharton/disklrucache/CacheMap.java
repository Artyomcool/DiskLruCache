package com.jakewharton.disklrucache;

import org.junit.Assert;

import java.util.Iterator;
import java.util.Map;

public class CacheMap<Meta> extends LinkedMapLruCache<Meta> {

    public void assertEquals(Object[][] expected) {
        Iterator<Map.Entry<String, Meta>> iterator = getEntriesIterator();
        for (Object[] expectedLine : expected) {
            Assert.assertTrue("Cache wasn't completely restored", iterator.hasNext());

            Map.Entry<String, Meta> next = iterator.next();
            Assert.assertEquals("Wrong order in cache", expectedLine[0], next.getKey());
            Assert.assertEquals("Wrong order in cache", expectedLine[1], next.getValue());
        }
        Assert.assertFalse("Cache has more records than was stored", iterator.hasNext());
    }

}
