/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

/** Checks the installed scaffold source signatures and unchanged refusal boundary. */
final class M3LongPatriciaTrieCompatibilityTest {
    @Test void installedScaffoldMethodsRemainSourceCompatibleAndRefuseWithoutMutation() {
        M3LongPatriciaTrie trie = new M3LongPatriciaTrie();
        assertTrue(trie.putKey(7L, 11L));
        assertThrows(M3NotYetReceived.class, () -> { long value = trie.put(7L, 99L); });
        assertThrows(M3NotYetReceived.class, () -> { long value = trie.remove(7L); });
        assertThrows(M3NotYetReceived.class, () -> { long value = trie.selectKey(7L); });
        assertEquals(11L, trie.getOrDefault(7L, -1L));
        assertEquals(1, trie.size());
        assertFalse(trie.putKey(7L, 19L));
        assertEquals(19L, trie.getOrDefault(7L, -1L));
        assertTrue(trie.removeKey(7L));
        assertFalse(trie.removeKey(7L));
        assertTrue(trie.isEmpty());
    }

    @Test void everyInstalledPublicSignatureAndHistoricalMetadataIsRetained() throws Exception {
        signature("scaffoldDescriptor", String.class);
        signature("put", long.class, long.class, long.class);
        signature("remove", long.class, long.class);
        signature("selectKey", long.class, long.class);
        signature("getOrDefault", long.class, long.class, long.class);
        signature("containsKey", boolean.class, long.class);
        signature("prefixCount", int.class, long.class, int.class);
        signature("firstKey", long.class);
        signature("lastKey", long.class);
        signature("size", int.class);
        signature("isEmpty", boolean.class);
        signature("clear", void.class);
        signature("putKey", boolean.class, long.class, long.class);
        signature("removeKey", boolean.class, long.class);
        assertNotNull(M3LongPatriciaTrie.class.getConstructor());
        assertEquals("M3LongPatriciaTrie|LONG_PATRICIA_TRIE|LONG|OCCUPIED,DELETED,LEAF|long[] keyLane;long[] valueLane;int[] bitIndexLane;int[] leftLane;int[] rightLane",
                M3LongPatriciaTrie.scaffoldDescriptor());
        assertEquals(1L, M3LongPatriciaTrie.STATE_OCCUPIED);
        assertEquals(2L, M3LongPatriciaTrie.STATE_DELETED);
        assertEquals(4L, M3LongPatriciaTrie.STATE_LEAF);
    }

    private static void signature(String name, Class<?> result, Class<?>... parameters) throws Exception {
        Method method = M3LongPatriciaTrie.class.getMethod(name, parameters);
        assertEquals(result, method.getReturnType(), name);
    }
}
