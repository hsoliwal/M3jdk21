// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class MIndexSetCapacityTest {
    @Test
    void wordCountWidensBeforeRounding() {
        assertEquals(0, IdSupport.bitWordCount(0));
        assertEquals(1, IdSupport.bitWordCount(1));
        assertEquals(1, IdSupport.bitWordCount(64));
        assertEquals(2, IdSupport.bitWordCount(65));
        assertEquals(33_554_432,
                IdSupport.bitWordCount(Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class,
                () -> IdSupport.bitWordCount(-1));
    }

    @Test
    void denseSetHandlesLastWordAndPromotesOutsideBound() {
        MIndexIdentitySpace<Object> space = MIndexSpaces.identities();
        for (int id = 0; id <= 65; id++) {
            assertEquals(id, space.id(new Object()));
        }
        MIndexSet<Object> set = MIndexSet.dense(space, 65);
        assertTrue(set.addId(0));
        assertTrue(set.addId(64));
        assertTrue(set.isDense());
        assertFalse(set.containsId(65));
        assertTrue(set.addId(65));
        assertFalse(set.isDense());
        assertTrue(set.containsId(0));
        assertTrue(set.containsId(64));
        assertTrue(set.containsId(65));
    }
}
