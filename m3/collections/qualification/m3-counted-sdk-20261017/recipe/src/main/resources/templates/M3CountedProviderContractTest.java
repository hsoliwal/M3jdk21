/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import org.apache.commons.collections4.multiset.HashMultiSet;
import org.junit.jupiter.api.Test;

/** Real Guava + Apache Commons libraries, not compile-only substitute APIs. */
public final class M3CountedProviderContractTest {
    @Test void realProvidersDifferentialWithM3CountedStorage() {
        M3GuavaMultiset<String> m3Guava = M3GuavaMultiset.create();
        M3ApacheMultiSet<String> m3Apache = M3ApacheMultiSet.create();
        Multiset<String> guava = HashMultiset.create();
        org.apache.commons.collections4.MultiSet<String> apache = new HashMultiSet<>();
        String[] keys = {null, "alpha", "beta", "gamma", "delta", "epsilon"};
        Random random = new Random(0x17AB20261017L);
        for (int step = 0; step < 3200; step++) {
            String key = keys[random.nextInt(keys.length)];
            int qty = random.nextInt(6);
            switch (random.nextInt(5)) {
                case 0, 1 -> {
                    assertEquals(guava.add(key, qty), m3Guava.add(key, qty));
                    assertEquals(apache.add(key, qty), m3Apache.add(key, qty));
                }
                case 2, 3 -> {
                    assertEquals(guava.remove(key, qty), m3Guava.remove(key, qty));
                    assertEquals(apache.remove(key, qty), m3Apache.remove(key, qty));
                }
                default -> {
                    assertEquals(guava.setCount(key, qty), m3Guava.setCount(key, qty));
                    assertEquals(apache.setCount(key, qty), m3Apache.setCount(key, qty));
                }
            }
            assertEquals(guava.size(), m3Guava.size());
            assertEquals(apache.size(), m3Apache.size());
            assertEquals(guava.elementSet(), m3Guava.elementSet());
            assertEquals(apache.uniqueSet(), m3Apache.uniqueSet());
            assertEquals(guava.entrySet(), m3Guava.entrySet());
            assertEquals(apache.entrySet(), m3Apache.entrySet());
            assertEquals(guava, m3Guava);
            assertEquals(m3Guava, guava);
            assertEquals(apache, m3Apache);
            assertEquals(m3Apache, apache);
            assertEquals(guava.hashCode(), m3Guava.hashCode());
            assertEquals(apache.hashCode(), m3Apache.hashCode());
            for (String element : keys) {
                assertEquals(guava.count(element), m3Guava.count(element));
                assertEquals(apache.getCount(element), m3Apache.getCount(element));
            }
        }
    }
    @Test void backedViewsRemoveAllWhileOccurrenceIteratorRemovesOne() {
        M3GuavaMultiset<String> guava = M3GuavaMultiset.create();
        M3ApacheMultiSet<String> apache = M3ApacheMultiSet.create();
        for (String element : List.of("a", "a", "b", "a", "b")) {
            guava.add(element);
            apache.add(element);
        }
        var guavaIterator = guava.iterator();
        assertEquals("a", guavaIterator.next());
        guavaIterator.remove();
        assertEquals(2, guava.count("a"));
        assertTrue(guava.elementSet().remove("b"));
        assertEquals(0, guava.count("b"));
        assertEquals(2, guava.size());
        var apacheIterator = apache.iterator();
        assertEquals("a", apacheIterator.next());
        apacheIterator.remove();
        assertEquals(2, apache.getCount("a"));
        assertTrue(apache.uniqueSet().remove("b"));
        assertEquals(0, apache.getCount("b"));
        assertEquals(2, apache.size());
        assertTrue(guava.entrySet().remove(com.google.common.collect.Multisets.immutableEntry("a", 2)));
        assertEquals(0, guava.size());
        apache.entrySet().clear();
        assertEquals(0, apache.size());
    }
    @Test void sourceBoundsNullsAndCounters() {
        var guava = M3GuavaMultiset.<String>create();
        var apache = M3ApacheMultiSet.<String>create();
        guava.add(null, 4);
        apache.add(null, 4);
        assertEquals(4, guava.count(null));
        assertEquals(4, apache.getCount(null));
        assertTrue(guava.setCount(null, 4, 2));
        assertFalse(guava.setCount(null, 4, 1));
        assertEquals(2, guava.count(null));
        assertThrows(IllegalArgumentException.class, () -> guava.add("x", -1));
        assertThrows(IllegalArgumentException.class, () -> apache.add("x", -1));
        assertThrows(IllegalArgumentException.class, () -> guava.setCount("x", -1));
        assertThrows(IllegalArgumentException.class, () -> apache.setCount("x", -1));
        var large = M3GuavaMultiset.<String>create();
        large.add("x", Integer.MAX_VALUE);
        large.add("y", Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, large.size());
        assertEquals(2L * Integer.MAX_VALUE, large.totalOccurrences());
        assertThrows(IllegalArgumentException.class, () -> large.add("x", 1));
        assertEquals(Integer.MAX_VALUE, large.count("x"));
        var apacheLarge = M3ApacheMultiSet.<String>create();
        apacheLarge.add("x", Integer.MAX_VALUE);
        apacheLarge.add("y", Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, apacheLarge.size());
        assertEquals(2L * Integer.MAX_VALUE, apacheLarge.totalOccurrences());
    }
}
