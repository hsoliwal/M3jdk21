/* *****************************************************************************
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
package jdk.m3.collections.eclipse.impl.list.mutable;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.impl.parallel.BatchIterable;

/**
 * Reflective view of both twins: lane layout comparison including backing-array tails, the
 * white-box offset-tree invariants of the patched class, the get probe over every index and the
 * coverage counters of the lockstep proof.
 */
final class OffsetsState {

    /** How often the fast path and its edge cases were exercised on the patched twin. */
    static final class Coverage {
        long indexed;
        long served;
        long boundary;
        long emptyBefore;
        long stalePast;
        long stalePastTree;
        long reentrantRetain;
        long ops;
        long sizeSum;
    }

    /** A captured exception, compared by class and message. */
    record Fault(String text) {
    }

    private static final Class<?> LANE_OFFSETS = nested("CompositeFastList$LaneOffsets");
    private static final VarHandle P_LISTS = handle(CompositeFastList.class, "lists",
            FastList.class);
    private static final VarHandle P_SIZE = handle(CompositeFastList.class, "size", int.class);
    private static final VarHandle P_OFFSETS = handle(CompositeFastList.class, "offsets",
            LANE_OFFSETS);
    private static final VarHandle P_PRIVATE = handle(CompositeFastList.class, "privateLanes",
            boolean.class);
    private static final VarHandle P_BUSY = handle(CompositeFastList.class, "busyDepth",
            int.class);
    private static final VarHandle R_LISTS = handle(LegacyCompositeFastList.class, "lists",
            FastList.class);
    private static final VarHandle R_SIZE = handle(LegacyCompositeFastList.class, "size",
            int.class);
    private static final VarHandle TREE = handle(LANE_OFFSETS, "tree", long[].class);

    private OffsetsState() {
    }

    private static Class<?> nested(String name) {
        try {
            return Class.forName(CompositeFastList.class.getPackageName() + "." + name);
        } catch (ClassNotFoundException missing) {
            throw new IllegalStateException(missing);
        }
    }

    private static VarHandle handle(Class<?> owner, String name, Class<?> type) {
        try {
            return MethodHandles.privateLookupIn(owner, MethodHandles.lookup())
                    .findVarHandle(owner, name, type);
        } catch (ReflectiveOperationException missing) {
            throw new IllegalStateException(missing);
        }
    }

    /** Drops the patched twin's offset tree, as a stale or never-built tree would be. */
    static void dropTree(MutableList<Object> patched) {
        P_OFFSETS.set((CompositeFastList<?>) patched, null);
    }

    @SuppressWarnings("unchecked")
    static FastList<FastList<Object>> lanes(MutableList<Object> list) {
        if (list instanceof CompositeFastList<Object> patched) {
            return (FastList<FastList<Object>>) P_LISTS.get(patched);
        }
        return (FastList<FastList<Object>>) R_LISTS.get((LegacyCompositeFastList<?>) list);
    }

    static int sizeField(MutableList<Object> list) {
        if (list instanceof CompositeFastList<Object> patched) {
            return (int) P_SIZE.get(patched);
        }
        return (int) R_SIZE.get((LegacyCompositeFastList<?>) list);
    }

    static int laneCount(MutableList<Object> list) {
        return lanes(list).size();
    }

    static Optional<long[]> tree(MutableList<Object> patched) {
        Object offsets = P_OFFSETS.get((CompositeFastList<?>) patched);
        return offsets == null ? Optional.empty() : Optional.of((long[]) TREE.get(offsets));
    }

    static boolean treeLive(MutableList<Object> patched) {
        return P_OFFSETS.get((CompositeFastList<?>) patched) != null;
    }

    static boolean privateLanes(MutableList<Object> patched) {
        return (boolean) P_PRIVATE.get((CompositeFastList<?>) patched);
    }

    static int busyDepth(MutableList<Object> patched) {
        return (int) P_BUSY.get((CompositeFastList<?>) patched);
    }

    static BatchIterable<Object> batch(MutableList<Object> list) {
        if (list instanceof CompositeFastList<Object> patched) {
            return patched;
        }
        return (LegacyCompositeFastList<Object>) list;
    }

    static void addComposited(MutableList<Object> list, Collection<Object> lane) {
        if (list instanceof CompositeFastList<Object> patched) {
            patched.addComposited(lane);
        } else {
            ((LegacyCompositeFastList<Object>) list).addComposited(lane);
        }
    }

    static void resetSize(MutableList<Object> list) {
        if (list instanceof CompositeFastList<Object> patched) {
            patched.resetSize();
        } else {
            ((LegacyCompositeFastList<Object>) list).resetSize();
        }
    }

    /** The live elements of every lane, read without any callback. */
    static List<Object> elements(MutableList<Object> list) {
        List<Object> all = new ArrayList<>();
        for (FastList<Object> lane : lanes(list)) {
            for (int i = 0; i < lane.size; i++) {
                all.add(lane.items[i]);
            }
        }
        return all;
    }

    /** {@code starts[p]} is the total of lanes before p; the last entry is the live total. */
    static long[] starts(FastList<FastList<Object>> lanes) {
        long[] starts = new long[lanes.size() + 1];
        for (int p = 0; p < lanes.size(); p++) {
            starts[p + 1] = starts[p] + lanes.get(p).size;
        }
        return starts;
    }

    /** Fenwick tree from first principles: node i totals lanes (i - lowbit(i), i]. */
    static long[] fenwick(FastList<FastList<Object>> lanes) {
        long[] starts = starts(lanes);
        long[] tree = new long[starts.length];
        for (int i = 1; i < tree.length; i++) {
            tree[i] = starts[i] - starts[i - (i & -i)];
        }
        return tree;
    }

    /** Same element: equal Integer value, same probe id, or both null; never calls equals. */
    static boolean same(Object left, Object right) {
        if (left == right) {
            return true;
        }
        if (left instanceof Integer a && right instanceof Integer b) {
            return a.intValue() == b.intValue();
        }
        if (left instanceof Fault a && right instanceof Fault b) {
            return a.text().equals(b.text());
        }
        return left instanceof OffsetsCallbacks.Probe a && right instanceof OffsetsCallbacks.Probe b
                && a.id == b.id;
    }

    /** Outcome after every operation: fields, lanes, white-box tree, then every index. */
    static void compare(OffsetsTwin twin, String where) {
        assertEquals(sizeField(twin.legacy), sizeField(twin.patched), where + " size field");
        if (!sameLayout(twin)) {
            assertEquals(layout(twin, 1), layout(twin, 0), where + " lanes");
        }
        whiteBox(twin, where + " after op");
        probe(twin, where);
        whiteBox(twin, where + " after probe");
    }

    private static boolean sameLayout(OffsetsTwin twin) {
        FastList<FastList<Object>> mine = lanes(twin.patched);
        FastList<FastList<Object>> theirs = lanes(twin.legacy);
        if (mine.size() != theirs.size()) {
            return false;
        }
        for (int p = 0; p < mine.size(); p++) {
            if (!sameLane(mine.get(p), theirs.get(p))
                    || twin.handleOf(mine.get(p), 0) != twin.handleOf(theirs.get(p), 1)) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameLane(FastList<Object> mine, FastList<Object> theirs) {
        if (mine.getClass() != theirs.getClass() || mine.size != theirs.size
                || mine.items.length != theirs.items.length) {
            return false;
        }
        for (int i = 0; i < mine.items.length; i++) {
            if (!same(mine.items[i], theirs.items[i])) {
                return false;
            }
        }
        return true;
    }

    private static String layout(OffsetsTwin twin, int side) {
        StringBuilder text = new StringBuilder();
        for (FastList<Object> lane : lanes(twin.list(side))) {
            text.append("\n h").append(twin.handleOf(lane, side)).append(' ')
                    .append(lane.getClass().getSimpleName()).append(" n=").append(lane.size)
                    .append(' ').append(OffsetsCallbacks.renderArray(lane.items));
        }
        return text.toString();
    }

    /** I1, I2 and I5 on the patched twin. */
    static void whiteBox(OffsetsTwin twin, String where) {
        MutableList<Object> patched = twin.patched;
        assertEquals(0, busyDepth(patched), where + " busyDepth");
        boolean owned = privateLanes(patched);
        Optional<long[]> tree = tree(patched);
        if (tree.isPresent()) {
            assertTrue(owned, where + " offset tree over caller lanes");
            assertArrayEquals(fenwick(lanes(patched)), tree.get(), where + " offset tree drift");
        }
        if (owned) {
            assertTrue(ownedLanes(twin), where + " privateLanes with a caller lane");
        }
    }

    private static boolean ownedLanes(OffsetsTwin twin) {
        for (FastList<Object> lane : lanes(twin.patched)) {
            if (lane.getClass() != FastList.class || twin.handleOf(lane, 0) >= 0) {
                return false;
            }
        }
        return true;
    }

    /** After a successful sort or listIterator flatten the single fresh lane is private. */
    static void assertFlattened(OffsetsTwin twin, int side) {
        if (side == 0) {
            assertTrue(privateLanes(twin.patched), "flatten leaves privateLanes true");
            assertEquals(1, laneCount(twin.patched), "flatten leaves one lane");
        }
    }

    /** I9 on a freshly deserialized patched twin. */
    static void assertDeserialized(MutableList<Object> copy) {
        assertFalse(tree(copy).isPresent(), "deserialized offsets");
        assertFalse(privateLanes(copy), "deserialized privateLanes");
        assertEquals(0, busyDepth(copy), "deserialized busyDepth");
    }

    static Object getOrFault(MutableList<Object> list, int index) {
        try {
            return list.get(index);
        } catch (RuntimeException failure) {
            return new Fault(OffsetsCallbacks.fault(failure));
        }
    }

    /** get(i) on both twins for every i in [-2, size + 2]. */
    static void probe(OffsetsTwin twin, String where) {
        int size = twin.patched.size();
        long[] starts = starts(lanes(twin.patched));
        twin.cov.ops++;
        twin.cov.sizeSum += size;
        for (int i = -2; i <= Math.max(size, 0) + 2; i++) {
            boolean before = treeLive(twin.patched);
            Object mine = getOrFault(twin.patched, i);
            Object theirs = getOrFault(twin.legacy, i);
            if (!same(mine, theirs)) {
                fail(where + " get(" + i + ") " + text(mine) + " vs " + text(theirs));
            }
            count(twin.cov, i, size, starts, before, treeLive(twin.patched));
        }
    }

    private static String text(Object value) {
        return value instanceof Fault fault ? fault.text() : OffsetsCallbacks.render(value);
    }

    private static void count(Coverage cov, int index, int size, long[] starts, boolean before,
            boolean after) {
        if (index < 0 || index >= size) {
            return;
        }
        countServed(cov, after);
        if (index >= starts[starts.length - 1]) {
            cov.stalePast++;
            cov.stalePastTree += before ? 1 : 0;
            return;
        }
        int lane = 0;
        while (starts[lane + 1] <= index) {
            lane++;
        }
        cov.boundary += after && lane > 0 && starts[lane] == index ? 1 : 0;
        cov.emptyBefore += after && lane > 0 && starts[lane] == starts[lane - 1] ? 1 : 0;
    }

    static void countServed(Coverage cov, boolean served) {
        cov.indexed++;
        cov.served += served ? 1 : 0;
    }

    /** A callback read-back inside retainAll on a patched twin with nine or more lanes. */
    static void countReentrant(OffsetsCallbacks.Ctx ctx) {
        if (ctx.side == 0 && ctx.op.startsWith("retainAll") && laneCount(ctx.owner) >= 9) {
            ctx.cov.reentrantRetain++;
        }
    }
}
