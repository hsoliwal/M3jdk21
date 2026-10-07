// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synexia.mindex.MIndexObjects;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class MIndexCollectionEnhancementTest {
    private record ShapeRecord(String name, int count) { }

    @Test
    void denseMapUsesDirectAddressingAndPromotesSafely() {
        TestSpace keys = new TestSpace();
        TestSpace values = new TestSpace();
        MIndexMap<String, String> map =
                MIndexMap.dense(keys, values, 64);

        for (int index = 0; index < 32; index++) {
            assertTrue(map.put("k" + index, "v" + index));
        }
        assertTrue(map.isDense());
        assertEquals("v7", map.get("k7"));
        assertFalse(map.put("k7", "updated"));
        assertEquals("updated", map.get("k7"));

        for (int index = 32; index <= 64; index++) {
            map.put("k" + index, "v" + index);
        }
        assertFalse(map.isDense());
        assertEquals("updated", map.get("k7"));
        assertEquals("v64", map.get("k64"));
        assertTrue(map.remove("k12"));
        assertFalse(map.containsKey("k12"));
    }

    @Test
    void factorySelectsDirectMapOnlyForDenseBoundedDomains() {
        MIndexCollectionFactory factory =
                new MIndexCollectionFactory();
        MIndexCollectionFactory.Hints dense =
                MIndexCollectionFactory.Hints.membership(32, 64);
        MIndexCollectionFactory.Hints sparse =
                MIndexCollectionFactory.Hints.membership(
                        8, 65_536);

        assertEquals(
                "dense-direct-map",
                factory.mapDecision(dense).shape());
        assertEquals(
                "sparse-open-addressed-map",
                factory.mapDecision(sparse).shape());

        TestSpace keys = new TestSpace();
        TestSpace values = new TestSpace();
        assertTrue(factory.map(keys, values, dense).isDense());
        assertFalse(factory.map(keys, values, sparse).isDense());
    }

    @Test
    void frozenSnapshotsDetachPrimitiveState() {
        TestSpace space = new TestSpace();

        MIndexCompositeIndex canonical =
                new MIndexCompositeIndex();

        MIndexList<String> list = new MIndexList<>(space);
        list.add("a");
        list.add("b");
        MIndexFrozenList<String> frozenList =
                list.freeze(canonical);
        list.set(0, "changed");
        assertEquals("a", frozenList.get(0));
        assertEquals(2, frozenList.size());

        MIndexSet<String> set = new MIndexSet<>(space);
        set.add("a");
        set.add("b");
        MIndexFrozenSet<String> frozenSet =
                set.freeze(canonical);
        set.remove("a");
        assertTrue(frozenSet.contains("a"));
        assertFalse(set.contains("a"));

        MIndexMap<String, String> map =
                new MIndexMap<>(space, space);
        map.put("a", "b");
        MIndexFrozenMap<String, String> frozenMap =
                map.freeze(canonical);
        map.put("a", "changed");
        assertEquals("b", frozenMap.get("a"));

        assertEquals(
                frozenList.canonicalId(),
                MIndexFrozenList.ofIds(
                        space,
                        canonical,
                        frozenList.copyIds())
                        .canonicalId());
    }

    @Test
    void javaViewsAreExplicitCompatibilityBoundaries() {
        TestSpace space = new TestSpace();

        MIndexList<String> list = new MIndexList<>(space);
        List<String> listView =
                MIndexJavaViews.mutableList(list);
        listView.add("a");
        listView.add("b");
        listView.set(1, "c");
        assertEquals("c", list.get(1));

        MIndexCompositeIndex canonical =
                new MIndexCompositeIndex();
        List<String> readOnlyList =
                MIndexJavaViews.readOnlyList(
                        list.freeze(canonical));
        assertEquals(List.of("a", "c"), readOnlyList);
        assertThrows(
                UnsupportedOperationException.class,
                () -> readOnlyList.add("x"));

        MIndexSet<String> set = new MIndexSet<>(space);
        Set<String> setView =
                MIndexJavaViews.mutableSet(set);
        assertTrue(setView.add("a"));
        assertTrue(setView.contains("a"));
        assertTrue(setView.remove("a"));

        MIndexMap<String, String> map =
                new MIndexMap<>(space, space);
        Map<String, String> mapView =
                MIndexJavaViews.mutableMap(map);
        assertNull(mapView.put("k", "v1"));
        assertEquals("v1", mapView.put("k", "v2"));
        assertEquals("v2", map.get("k"));

        Map<String, String> readOnlyMap =
                MIndexJavaViews.readOnlyMap(
                        map.freeze(canonical));
        assertEquals("v2", readOnlyMap.get("k"));
        assertThrows(
                UnsupportedOperationException.class,
                () -> readOnlyMap.put("x", "y"));
    }

    @Test
    void shapeIdentityIsExactAcrossIndependentRegistries() {
        int nameId = MIndexSpaces.stringId("name");
        int countId = MIndexSpaces.stringId("count");
        int otherId = MIndexSpaces.stringId("other");

        MIndexShapeIndex firstShapes =
                new MIndexShapeIndex();
        MIndexShapeIndex secondShapes =
                new MIndexShapeIndex();

        int firstTypeId =
                MIndexSpaces.stringId("first-type");
        int secondTypeId =
                MIndexSpaces.stringId("second-type");
        int firstShape = firstShapes.intern(
                new int[] {nameId, countId},
                new int[] {firstTypeId, secondTypeId});
        int sameShape = firstShapes.intern(
                new int[] {nameId, countId},
                new int[] {firstTypeId, secondTypeId});
        int unrelatedLocalZero = secondShapes.intern(
                new int[] {otherId, countId},
                new int[] {firstTypeId, secondTypeId});

        assertEquals(firstShape, sameShape);
        assertEquals(0, firstShape);
        assertEquals(0, unrelatedLocalZero);
        assertEquals(
                0,
                firstShapes.ordinalOfNameId(
                        firstShape, nameId));
        assertEquals(
                1,
                firstShapes.ordinalOfNameId(
                        firstShape, countId));
        assertThrows(
                IllegalArgumentException.class,
                () -> firstShapes.intern(
                        new int[] {nameId, nameId}));

        int objectShape = firstShapes.intern(
                MIndexObjects.index(
                        new ShapeRecord("alice", 7))
                        .shape());
        assertEquals(2, firstShapes.memberCount(objectShape));
        int objectNameOrdinal =
                firstShapes.ordinalOfNameId(
                        objectShape, nameId);
        int objectCountOrdinal =
                firstShapes.ordinalOfNameId(
                        objectShape, countId);
        assertTrue(objectNameOrdinal >= 0);
        assertTrue(objectCountOrdinal >= 0);
        assertEquals(
                MIndexSpaces.stringId(
                        String.class.getName()),
                firstShapes.typeId(
                        objectShape, objectNameOrdinal));
        assertEquals(
                MIndexSpaces.stringId(
                        int.class.getName()),
                firstShapes.typeId(
                        objectShape, objectCountOrdinal));

        TestSpace values = new TestSpace();
        MIndexTable<String> first = new MIndexTable<>(
                values, firstShapes, firstShape, 1);
        first.addRowIds(new int[] {
                values.id("alice"),
                values.id("7")
        });
        assertEquals(
                "alice",
                first.getByNameId(0, nameId));

        MIndexTable<String> second = new MIndexTable<>(
                values,
                secondShapes,
                unrelatedLocalZero,
                1);
        second.addRowIds(new int[] {
                values.id("alice"),
                values.id("7")
        });

        MIndexCompositeIndex canonical =
                new MIndexCompositeIndex();
        assertNotEquals(
                first.canonicalId(canonical),
                second.canonicalId(canonical));

        MIndexFrozenTable<String> frozen =
                first.freeze(canonical);
        assertTrue(frozen.isShaped());
        assertEquals(
                "7",
                frozen.getByNameId(0, countId));

        TestSpace independentValues = new TestSpace();
        MIndexTable<String> sameNumericLaneDifferentSpace =
                new MIndexTable<>(
                        independentValues,
                        firstShapes,
                        firstShape,
                        1);
        sameNumericLaneDifferentSpace.addRowIds(
                new int[] {
                        independentValues.id("alice"),
                        independentValues.id("7")
                });
        assertNotEquals(
                first.canonicalId(canonical),
                sameNumericLaneDifferentSpace
                        .canonicalId(canonical));
    }

    @Test
    void randomizedDenseMapMatchesHashMap() {
        TestSpace keys = new TestSpace();
        TestSpace values = new TestSpace();
        for (int index = 0; index < 128; index++) {
            keys.id("k" + index);
        }
        for (int index = 0; index < 64; index++) {
            values.id("v" + index);
        }

        MIndexMap<String, String> indexed =
                MIndexMap.dense(keys, values, 128);
        Map<String, String> reference = new HashMap<>();
        Random random = new Random(0x4d_49_4e_44L);

        for (int step = 0; step < 20_000; step++) {
            String key = "k" + random.nextInt(128);
            String value = "v" + random.nextInt(64);
            switch (random.nextInt(4)) {
                case 0 -> {
                    boolean absent =
                            !reference.containsKey(key);
                    reference.put(key, value);
                    assertEquals(
                            absent,
                            indexed.put(key, value));
                }
                case 1 -> assertEquals(
                        reference.remove(key) != null,
                        indexed.remove(key));
                case 2 -> assertEquals(
                        reference.containsKey(key),
                        indexed.containsKey(key));
                default -> assertEquals(
                        reference.get(key),
                        indexed.get(key));
            }
            assertEquals(
                    reference.size(),
                    indexed.size());
        }
        assertTrue(indexed.isDense());
    }

    @Test
    void newResidentArrayFieldsRemainPrimitive() {
        assertPrimitiveArrayFields(MIndexMap.class);
        assertPrimitiveArrayFields(MIndexFrozenList.class);
        assertPrimitiveArrayFields(MIndexFrozenSet.class);
        assertPrimitiveArrayFields(MIndexFrozenMap.class);
        assertPrimitiveArrayFields(MIndexFrozenTable.class);
        assertPrimitiveArrayFields(MIndexTable.class);
    }

    private static void assertPrimitiveArrayFields(
            Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            Class<?> fieldType = field.getType();
            if (fieldType.isArray()) {
                assertTrue(
                        fieldType.getComponentType().isPrimitive(),
                        () -> type.getName()
                                + "."
                                + field.getName()
                                + " retains non-primitive array "
                                + fieldType.getTypeName());
            }
        }
    }

    private static final class TestSpace
            implements MIndexSpace<String> {
        private final List<String> values =
                new ArrayList<>();
        private final Map<String, Integer> ids =
                new HashMap<>();

        @Override
        public int id(String value) {
            Integer existing = ids.get(value);
            if (existing != null) {
                return existing;
            }
            int id = values.size();
            values.add(value);
            ids.put(value, id);
            return id;
        }

        @Override
        public int findId(String value) {
            return ids.getOrDefault(value, -1);
        }

        @Override
        public String value(int id) {
            return values.get(id);
        }

        @Override
        public int size() {
            return values.size();
        }

        @Override
        public boolean javaEqualityCompatible() {
            return true;
        }
    }
}
