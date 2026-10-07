// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synexia.mindex.MIndexString;
import org.junit.jupiter.api.Test;

/** Full-module gate; this class must run against real runtime owners, never link fixtures. */
final class MIndexArrayTest {
    @Test
    void nativeArrayContracts() throws Exception {
        MIndexArraySelfTest.main(new String[0]);
    }

    @Test
    void realStringDomainsAndShapedTableKeepTheirAuthority() {
        MIndexSpace<MIndexString> strings = MIndexSpaces.strings();
        int first = MIndexSpaces.stringId("mindex-array-first");
        int second = MIndexSpaces.stringId("mindex-array-second");
        MIndexCompositeIndex store = new MIndexCompositeIndex();
        MIndexShapeIndex shapes = new MIndexShapeIndex(store);
        int shape = shapes.intern(new int[]{first, second});
        MIndexTable<MIndexString> table = new MIndexTable<>(strings, shapes, shape, 4096);
        table.addRowsIds(new int[]{first, second, second, first}, 0, 4);
        MIndexFrozenTable<MIndexString> snapshot = table.freeze(store);
        table.removeRows(0, 1); table.compact();
        assertEquals(shape, table.shapeId());
        assertArrayEquals(new int[]{second, first}, table.snapshotIds());
        assertArrayEquals(new int[]{first, second, second, first}, snapshot.copyIds());
        assertEquals(second, table.idAtByNameId(0, first));
        MIndexBiMap<MIndexString, MIndexString> map = new MIndexBiMap<>(strings, strings, 0);
        map.putIds(first, second);
        assertSame(strings, map.inverse().keySpace());
        assertEquals(first, map.inverse().valueIdOrDefault(second, -1));
        assertEquals(strings.value(second), map.get(strings.value(first)));
        assertTrue(map.inverse().removeKeyId(second));
        assertTrue(map.isEmpty());
    }
}
