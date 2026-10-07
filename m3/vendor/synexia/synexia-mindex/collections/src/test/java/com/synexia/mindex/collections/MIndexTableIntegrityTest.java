// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synexia.mindex.MIndexString;
import org.junit.jupiter.api.Test;

final class MIndexTableIntegrityTest {
    @Test
    void frozenShapeSurvivesUpdatesAndMutableRoundTrip() {
        MIndexSpace<MIndexString> strings = MIndexSpaces.strings();
        MIndexCompositeIndex composites = new MIndexCompositeIndex();
        MIndexShapeIndex shapes = new MIndexShapeIndex(composites);
        int nameId = MIndexSpaces.stringId("table-name");
        int typeId = MIndexSpaces.stringId("table-type");
        int shapeId = shapes.intern(new int[] {nameId},
                new int[] {typeId});

        MIndexTable<MIndexString> table =
                new MIndexTable<>(strings, shapes, shapeId, 1);
        int alice = strings.id(MIndexString.literal("table-alice"));
        table.addRowIds(new int[] {alice});
        MIndexFrozenTable<MIndexString> frozen = table.freeze(composites);

        assertTrue(frozen.isShaped());
        assertEquals(1, frozen.rows());
        assertEquals(nameId, frozen.memberNameId(0));
        assertEquals(typeId, frozen.memberTypeId(0));
        assertEquals(alice, frozen.idAtByNameId(0, nameId));
        assertEquals(table.canonicalId(composites), frozen.canonicalId());

        MIndexFrozenTable<MIndexString> updated = frozen.withSet(
                0, 0, MIndexString.literal("table-bob"));
        assertEquals(alice, frozen.idAt(0, 0));
        assertEquals(MIndexString.literal("table-bob"),
                updated.getByNameId(0, nameId));
        assertEquals(nameId, updated.memberNameId(0));

        MIndexTable<MIndexString> mutable = frozen.mutableCopy();
        assertTrue(mutable.isShaped());
        assertEquals(frozen.canonicalId(),
                mutable.canonicalId(composites));

        assertEquals(frozen.canonicalId(),
                MIndexFrozenTable.ofIds(strings, composites, 1,
                        frozen.copyShapeLane(), frozen.copyIds())
                        .canonicalId());
        assertNotEquals(frozen.canonicalId(),
                MIndexFrozenTable.ofIds(
                        strings, composites, 1, frozen.copyIds())
                        .canonicalId());
        assertThrows(IllegalArgumentException.class,
                () -> MIndexFrozenTable.ofIds(strings, composites, 1,
                        new int[] {nameId, typeId, nameId, typeId},
                        new int[] {alice}));
    }

    @Test
    void shapeAndDequeWithIdenticalIdLaneNeverAlias() {
        MIndexSpace<MIndexString> strings = MIndexSpaces.strings();
        MIndexCompositeIndex composites = new MIndexCompositeIndex();
        int nameId = MIndexSpaces.stringId("shape-alias-name");
        int typeId = MIndexSpaces.stringId("shape-alias-type");
        MIndexShapeIndex shapes = new MIndexShapeIndex(composites);
        int shapeId = shapes.intern(
                new int[] {nameId}, new int[] {typeId});

        MIndexDeque<MIndexString> deque = new MIndexDeque<>(strings);
        deque.addLastId(nameId);
        deque.addLastId(typeId);
        int dequeId = deque.canonicalId(composites);

        assertNotEquals(MIndexCompositeIndex.KIND_SHAPE,
                MIndexCompositeIndex.KIND_DEQUE);
        assertNotEquals(shapeId, dequeId);
        assertEquals(MIndexCompositeIndex.KIND_SHAPE,
                composites.kind(shapeId));
        assertEquals(MIndexCompositeIndex.KIND_DEQUE,
                composites.kind(dequeId));
        assertThrows(IllegalArgumentException.class,
                () -> shapes.memberCount(dequeId));
    }
}
