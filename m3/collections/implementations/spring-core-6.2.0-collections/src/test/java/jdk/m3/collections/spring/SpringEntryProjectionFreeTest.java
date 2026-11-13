/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Contract regression for Spring's cached, projection-free entry traversals. */
final class SpringEntryProjectionFreeTest {
    private static final class MultiSpy extends LinkedMultiValueMap<String, String> {
        private static final long serialVersionUID = 1L;
        private int entryCalls;
        @Override
        public Set<Entry<String, List<String>>> entrySet() {
            entryCalls++;
            return super.entrySet();
        }
    }

    private static final class SingleSpy extends LinkedHashMap<String, String> {
        private static final long serialVersionUID = 1L;
        private int entryCalls;
        @Override
        public Set<Map.Entry<String, String>> entrySet() {
            entryCalls++;
            return super.entrySet();
        }
    }

    @Test
    void multiToSinglePreservesNullsBackedViewsAndExceptionTypes() {
        MultiSpy backing = new MultiSpy();
        backing.add("a", "alpha");
        backing.add("b", null);
        MultiToSingleValueMapAdapter<String, String> adapter =
                new MultiToSingleValueMapAdapter<>(backing);
        int before = backing.entryCalls;

        assertTrue(adapter.containsValue("alpha"));
        assertTrue(adapter.containsValue(null));
        assertFalse(adapter.containsValue("absent"));
        assertEquals(before + 1, backing.entryCalls);
        Set<Map.Entry<String, String>> publicView = adapter.entrySet();
        assertSame(publicView, adapter.entrySet());

        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "alpha");
        expected.put("b", null);
        assertTrue(adapter.equals(expected));
        assertEquals(expected, adapter);
        assertEquals(before + 1, backing.entryCalls);

        backing.put("c", new ArrayList<>());
        assertThrows(IndexOutOfBoundsException.class,
                () -> adapter.containsValue("not-present"));
    }

    @Test
    void singleToMultiPreservesSingletonListsAndCachedBackingEntrySet() {
        SingleSpy backing = new SingleSpy();
        backing.put("a", "alpha");
        backing.put("b", null);
        SingleToMultiValueMapAdapter<String, String> adapter =
                new SingleToMultiValueMapAdapter<>(backing);
        int before = backing.entryCalls;

        assertTrue(adapter.containsValue(Collections.singletonList("alpha")));
        assertTrue(adapter.containsValue(Collections.singletonList(null)));
        assertFalse(adapter.containsValue(null));
        assertFalse(adapter.containsValue(List.of()));
        assertEquals(before + 1, backing.entryCalls);
        Set<Map.Entry<String, List<String>>> publicView = adapter.entrySet();
        assertSame(publicView, adapter.entrySet());

        Map<String, List<String>> expected = new LinkedHashMap<>();
        expected.put("a", Collections.singletonList("alpha"));
        expected.put("b", Collections.singletonList(null));
        // Preserve the existing null-backed asymmetry: get() returns null while
        // entrySet() projects a singleton null list. This optimization must not change it.
        assertNull(adapter.get("b"));
        assertTrue(adapter.containsKey("b"));
        assertTrue(adapter.equals(expected));
        assertFalse(expected.equals(adapter));
        backing.remove("b");
        expected.remove("b");
        assertTrue(adapter.equals(expected));
        assertEquals(expected, adapter);
        assertEquals(before + 1, backing.entryCalls);

        backing.put("c", "gamma");
        assertTrue(adapter.containsValue(Collections.singletonList("gamma")));
        assertEquals(before + 1, backing.entryCalls);
    }
}
