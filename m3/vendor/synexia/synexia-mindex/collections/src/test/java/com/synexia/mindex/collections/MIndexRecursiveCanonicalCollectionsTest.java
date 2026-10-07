// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synexia.mindex.MIndexString;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class MIndexRecursiveCanonicalCollectionsTest {
    @Test
    void builtInSemanticKindsAreUnique() {
        int[] kinds = MIndexCompositeIndex.builtInKinds();
        Set<Integer> unique = new HashSet<>();
        for (int kind : kinds) {
            assertTrue(kind > 0);
            assertTrue(unique.add(kind), () -> "duplicate kind " + kind);
            assertTrue(MIndexCompositeIndex.isBuiltInKind(kind));
        }
        assertEquals(kinds.length, unique.size());
        assertNotEquals(
                MIndexCompositeIndex.KIND_DEQUE,
                MIndexCompositeIndex.KIND_SHAPE);
        assertEquals(
                "SHAPE",
                MIndexCompositeIndex.kindName(MIndexCompositeIndex.KIND_SHAPE));
    }

    @Test
    void shapeAndDequeCannotAliasOnIdenticalStringLane() {
        MIndexCompositeIndex composites = new MIndexCompositeIndex();
        MIndexSpace<MIndexString> strings = MIndexSpaces.strings();
        MIndexString field = MIndexString.literal("recursive-field");
        MIndexString type = MIndexString.literal("java.lang.String");

        MIndexShapeIndex shapes = new MIndexShapeIndex(composites);
        int shapeId = shapes.intern(
                new int[] {field.contentIndex()},
                new int[] {type.contentIndex()});

        MIndexDeque<MIndexString> deque = new MIndexDeque<>(strings);
        deque.addLast(field);
        deque.addLast(type);
        int dequeId = deque.canonicalId(composites);

        assertNotEquals(shapeId, dequeId);
        assertEquals(MIndexCompositeIndex.KIND_SHAPE, composites.kind(shapeId));
        assertEquals(MIndexCompositeIndex.KIND_DEQUE, composites.kind(dequeId));
    }

    @Test
    void canonicalCollectionsCanBeNestedAsPrimitiveChildIds() {
        MIndexCompositeIndex composites = new MIndexCompositeIndex();
        MIndexSpace<MIndexString> strings = MIndexSpaces.strings();
        MIndexString alpha = MIndexString.literal("nested-alpha");
        MIndexString beta = MIndexString.literal("nested-beta");

        MIndexFrozenSet<MIndexString> set = MIndexFrozenSet.ofIds(
                strings,
                composites,
                new int[] {alpha.contentIndex(), beta.contentIndex()});
        MIndexFrozenTuple<MIndexString> tuple = MIndexFrozenTuple.ofIds(
                strings,
                composites,
                new int[] {alpha.contentIndex(), beta.contentIndex()});

        MIndexCompositeSpace compositeSpace = composites.space();
        assertSame(compositeSpace, MIndexSpaces.composites(composites));

        MIndexList<MIndexCompositeRef> children = new MIndexList<>(compositeSpace);
        children.add(compositeSpace.ref(set));
        children.add(compositeSpace.ref(tuple));
        MIndexFrozenList<MIndexCompositeRef> parent = children.freeze(composites);

        assertEquals(set.canonicalId(), parent.idAt(0));
        assertEquals(tuple.canonicalId(), parent.idAt(1));
        assertEquals(MIndexCompositeIndex.KIND_SET, parent.get(0).kind());
        assertEquals(MIndexCompositeIndex.KIND_TUPLE, parent.get(1).kind());

        int parentId = parent.canonicalId();
        MIndexFrozenTuple<MIndexString> laterChild = MIndexFrozenTuple.ofIds(
                strings,
                composites,
                new int[] {beta.contentIndex()});
        assertTrue(laterChild.canonicalId() > parentId);

        MIndexList<MIndexCompositeRef> sameChildren = new MIndexList<>(compositeSpace);
        sameChildren.add(compositeSpace.ref(set));
        sameChildren.add(compositeSpace.ref(tuple));
        assertEquals(parent, sameChildren.freeze(composites));
        assertEquals(parentId, parent.canonicalId());
    }

    @Test
    void compositeSpaceRejectsForeignStoreCoordinates() {
        MIndexCompositeIndex left = new MIndexCompositeIndex();
        MIndexCompositeIndex right = new MIndexCompositeIndex();
        MIndexSpace<MIndexString> strings = MIndexSpaces.strings();
        MIndexString value = MIndexString.literal("foreign-coordinate");

        MIndexFrozenTuple<MIndexString> tuple = MIndexFrozenTuple.ofIds(
                strings,
                right,
                new int[] {value.contentIndex()});

        assertEquals(-1, left.space().findId(tuple));
        assertThrows(
                IllegalArgumentException.class,
                () -> left.space().id(tuple));
    }

    @Test
    void graphCanonicalIdentityIgnoresAdmissionOrderAndDuplicates() {
        TestSpace nodes = new TestSpace();
        TestSpace relations = new TestSpace();
        MIndexCompositeIndex composites = new MIndexCompositeIndex();

        MIndexGraph<String, String> first = new MIndexGraph<>(nodes, relations);
        first.addEdge("b", "depends", "c");
        first.addEdge("a", "calls", "b");
        first.addEdge("a", "calls", "b");

        MIndexGraph<String, String> second = new MIndexGraph<>(nodes, relations);
        second.addEdge("a", "calls", "b");
        second.addEdge("b", "depends", "c");

        MIndexFrozenGraph<String, String> left = first.freeze(composites);
        MIndexFrozenGraph<String, String> right = second.freeze(composites);

        assertEquals(left, right);
        assertEquals(2, left.edgeCount());
        assertEquals(1, left.outDegree("a"));
        assertTrue(left.containsEdge("a", "calls", "b"));
        assertFalse(left.containsEdge("a", "depends", "c"));

        int[] visited = {0};
        left.forEachEdgeId((sourceId, relationId, targetId) -> visited[0]++);
        assertEquals(left.edgeCount(), visited[0]);

        MIndexFrozenGraph<String, String> extended =
                left.withEdge("a", "depends", "c");
        assertEquals(2, left.edgeCount());
        assertEquals(3, extended.edgeCount());
        assertEquals(left, extended.withoutEdge("a", "depends", "c"));

        MIndexGraph.Frozen<String, String> csr = first.freeze();
        assertEquals(left, csr.canonical(composites));
        assertEquals(left.canonicalId(), csr.canonicalEdgeSetId(composites));
    }

    private static final class TestSpace implements MIndexSpace<String> {
        private final List<String> values = new ArrayList<>();

        @Override
        public int id(String value) {
            int existing = values.indexOf(value);
            if (existing >= 0) {
                return existing;
            }
            values.add(value);
            return values.size() - 1;
        }

        @Override
        public int findId(String value) {
            return values.indexOf(value);
        }

        @Override
        public String value(int id) {
            return values.get(id);
        }

        @Override
        public int size() {
            return values.size();
        }
    }
}
