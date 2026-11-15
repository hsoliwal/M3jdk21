/*
 * Copyright 2026 Hitesh Soliwal and Synexia contributors.
 * Licensed under the Apache License, Version 2.0.
 * You may obtain a copy at https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package jdk.m3.collections.apache.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

import org.junit.jupiter.api.Test;

/** Preserve M3-specific failure and virtual dispatch while converging bulk algorithms. */
class TreeListConvergenceCompatibilityTest {
    private static final class SizeObservedList extends TreeList<Integer> {
        private int sizeCalls;
        @Override
        public int size() {
            sizeCalls++;
            return super.size();
        }
    }

    @Test
    void invalidIndexedMutationsRetainExistingIteratorInvalidation() {
        final TreeList<Integer> list = new TreeList<>(Arrays.asList(1, 2, 3));
        Iterator<Integer> cursor = list.iterator();
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(-1, 4));
        assertThrows(ConcurrentModificationException.class, cursor::next);
        cursor = list.iterator();
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(3));
        assertThrows(ConcurrentModificationException.class, cursor::next);
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    void subclassSizeCallbacksKeepTheirOriginalCountsAndFailureMessage() {
        final SizeObservedList list = new SizeObservedList();
        list.addAll(Arrays.asList(1, 2, 3));
        list.sizeCalls = 0;
        assertEquals(Integer.valueOf(1), list.get(0));
        assertEquals(1, list.sizeCalls);
        list.sizeCalls = 0;
        list.listIterator(0);
        assertEquals(1, list.sizeCalls);
        list.sizeCalls = 0;
        final IndexOutOfBoundsException failure =
                assertThrows(IndexOutOfBoundsException.class, () -> list.get(3));
        assertEquals("Invalid index:3, size=3", failure.getMessage());
        assertEquals(2, list.sizeCalls);
        list.sizeCalls = 0;
        assertEquals(Integer.valueOf(2), list.remove(1));
        assertEquals(2, list.sizeCalls);
    }

    @Test
    void inheritedAndBulkRangeContractsRetainStorageViewsAndCallbacks() throws Exception {
        TreeListBulkRangeContract.verify(24);
    }
}
