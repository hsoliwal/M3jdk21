/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import org.junit.jupiter.api.Test;

class M3ArrayListTrimEpochTest {
    private static Object backing(M3ArrayList<?> list) throws ReflectiveOperationException {
        Field field = M3ArrayList.class.getDeclaredField("elements");
        field.setAccessible(true);
        return field.get(list);
    }

    @Test void explicitNoOpTrimInvalidatesIteratorAndSublistWithoutAllocation() throws Exception {
        var list = new M3ArrayList<Integer>(Arrays.asList(null, 1, 1, 2));
        var iterator = list.iterator();
        var sublist = list.subList(0, 2);
        Object storage = backing(list);
        list.trimToSize(true);
        assertSame(storage, backing(list));
        assertThrows(ConcurrentModificationException.class, iterator::next);
        assertThrows(ConcurrentModificationException.class, sublist::size);
        assertEquals(Arrays.asList(null, 1, 1, 2), list);
    }

    @Test void existingNoArgumentAndFalseNoOpKeepTheirOldEpochContract() throws Exception {
        var list = new M3ArrayList<Integer>(List.of(1, 2));
        var iterator = list.iterator();
        var sublist = list.subList(0, 1);
        Object storage = backing(list);
        list.trimToSize();
        list.trimToSize(false);
        assertSame(storage, backing(list));
        assertEquals(1, iterator.next());
        assertEquals(List.of(1), sublist);
    }

    @Test void eitherModeActuallyReleasesUnusedCapacityAndPreservesValues() throws Exception {
        for (boolean invalidate : new boolean[]{false, true}) {
            var list = new M3ArrayList<Integer>(64);
            list.addAll(Arrays.asList(null, 7, 7));
            Object storage = backing(list);
            var iterator = list.iterator();
            var snapshot = list.snapshot();
            list.trimToSize(invalidate);
            assertEquals(3, list.capacity());
            assertNotSame(storage, backing(list));
            assertEquals(Arrays.asList(null, 7, 7), list);
            assertThrows(ConcurrentModificationException.class, iterator::next);
            list.set(1, 8);
            assertEquals(Arrays.asList(null, 7, 7), snapshot);
        }
    }

    @Test void explicitEmptyNoOpInvalidatesExistingEmptyIteratorWithoutAllocation() throws Exception {
        var list = new M3ArrayList<Object>(0);
        Object storage = backing(list);
        var iterator = list.iterator();
        list.trimToSize(true);
        assertSame(storage, backing(list));
        assertEquals(0, list.capacity());
        assertThrows(ConcurrentModificationException.class, iterator::next);
        assertTrue(list.isEmpty());
    }
}
