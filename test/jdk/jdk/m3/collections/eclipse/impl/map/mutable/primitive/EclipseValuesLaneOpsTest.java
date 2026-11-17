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
package jdk.m3.collections.eclipse.impl.map.mutable.primitive;

import static jdk.m3.collections.eclipse.impl.map.mutable.primitive.ValuesLaneLockstep.outcome;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.IntConsumer;
import jdk.m3.collections.eclipse.api.IntIterable;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.IntHashBag;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.IntArrayList;
import jdk.m3.collections.eclipse.impl.map.mutable.primitive.ValuesLaneSources.HookedList;
import jdk.m3.collections.eclipse.impl.map.mutable.primitive.ValuesLaneSources.LoggingView;
import jdk.m3.collections.eclipse.impl.map.mutable.primitive.ValuesLaneSources.TrueContainsMap;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.IntHashSet;
import org.junit.jupiter.api.Test;

/**
 * Differential proof for the gated value-lane fast paths of
 * AbstractMutableIntValuesMap.containsAll(IntIterable) and its values view's
 * removeAll(IntIterable). Patched maps and twins driven by the verbatim pre-patch bodies
 * (ValuesLaneRef) agree on results, exceptions with messages, every lane and counter, frozen
 * key-set aliasing and observable order, for all seven int-valued maps and each fallback case:
 * an aliasing source, a throwing source, a mutating source, a lazy tap, an overriding owner
 * subclass, a foreign values view, an owner's own nest-member view and a null source.
 */
//FLLDCIM: LEETCODIFY — differential proof for AbstractMutableIntValuesMap containsAll/removeAll
class EclipseValuesLaneOpsTest {

    private static final long SEED = 0x5EED_1A9EL;
    private static final int OPS = 200_000;

    @Test
    void lockstepAllSevenOwners() {
        for (ValuesLaneOwners owner : ValuesLaneOwners.ALL) {
            ValuesLaneLockstep run = new ValuesLaneLockstep(owner, SEED ^ owner.name.hashCode());
            for (int i = 0; i < OPS; i++) {
                run.op();
            }
            ValuesLaneState.assertSameOrder(run.p, run.t, owner, owner.name + " end");
            assertTrue(run.laneOps > OPS / 10, () -> owner.name + " lane ops " + run.laneOps);
        }
    }

    @Test
    void aliasingSourceKeepsPrePatchPath() {
        IntIntHashMap p = sixEntries(new IntIntHashMap());
        IntIntHashMap t = sixEntries(new IntIntHashMap());
        assertEquals(outcome(() -> ValuesLaneRef.removeAll(t.values(), t, t.values())),
                outcome(() -> p.values().removeAll(p.values())), "values().removeAll(values())");
        ValuesLaneState.assertSameLanes(p, t, "values alias");
        assertTrue(t.notEmpty(), "the pre-patch interleaving leaves a remainder");
        IntIntHashMap q = sixEntries(new IntIntHashMap());
        IntIntHashMap u = sixEntries(new IntIntHashMap());
        assertEquals(outcome(() -> ValuesLaneRef.removeAll(u.keySet(), u, u.values())),
                outcome(() -> q.values().removeAll(q.keySet())), "values().removeAll(keySet())");
        ValuesLaneState.assertSameLanes(q, u, "keySet alias");
    }

    @Test
    void throwingSourceKeepsPartialState() {
        IntIntHashMap p = sixEntries(new IntIntHashMap());
        IntIntHashMap t = sixEntries(new IntIntHashMap());
        String reference = outcome(() ->
                ValuesLaneRef.removeAll(new HookedList(boomAt(3), 1, 2, 3, 4, 5), t, t.values()));
        assertEquals(reference,
                outcome(() -> p.values().removeAll(new HookedList(boomAt(3), 1, 2, 3, 4, 5))));
        assertTrue(reference.endsWith("IllegalStateException: boom"), reference);
        ValuesLaneState.assertSameLanes(p, t, "after a throwing removeAll");
        HookedList present = new HookedList(boomAt(3), 0, 4, 5, 9);
        assertEquals(outcome(() -> ValuesLaneRef.containsAll(present, t)),
                outcome(() -> p.containsAll(present)), "containsAll");
    }

    @Test
    void mutatingSourceMatchesReference() {
        IntIntHashMap p = sixEntries(new IntIntHashMap());
        IntIntHashMap t = sixEntries(new IntIntHashMap());
        assertEquals(
                outcome(() -> ValuesLaneRef.removeAll(putting(t, 7, 7, 1, 8, 2), t, t.values())),
                outcome(() -> p.values().removeAll(putting(p, 7, 7, 1, 8, 2))), "removeAll");
        ValuesLaneState.assertSameLanes(p, t, "after a mutating removeAll");
        String reference = outcome(() ->
                ValuesLaneRef.containsAll(putting(t, 50, 50, 51, 52, 53), t));
        assertEquals(reference, outcome(() -> p.containsAll(putting(p, 50, 50, 51, 52, 53))));
        assertEquals("returned true", reference, "values put by the source are seen");
        ValuesLaneState.assertSameLanes(p, t, "after a mutating containsAll");
    }

    @Test
    void lazyTapSourceMatchesReference() {
        IntIntHashMap p = sixEntries(new IntIntHashMap());
        IntIntHashMap t = sixEntries(new IntIntHashMap());
        String reference = outcome(() -> ValuesLaneRef.containsAll(tapping(t, 1, 2, 70, 71), t));
        assertEquals(reference, outcome(() -> p.containsAll(tapping(p, 1, 2, 70, 71))));
        assertEquals("returned true", reference, "values put by the tap are seen");
        assertEquals(
                outcome(() -> ValuesLaneRef.removeAll(tapping(t, 1, 72, 3, 73), t, t.values())),
                outcome(() -> p.values().removeAll(tapping(p, 1, 72, 3, 73))), "removeAll");
        ValuesLaneState.assertSameLanes(p, t, "after tapping sources");
    }

    @Test
    void subclassOwnerKeepsPrePatchPath() {
        TrueContainsMap p = sixEntries(new TrueContainsMap());
        TrueContainsMap t = sixEntries(new TrueContainsMap());
        IntHashSet misses = IntHashSet.newSetWith(100, 101, 102, 103, 104);
        String reference = outcome(() -> ValuesLaneRef.containsAll(misses, t));
        assertEquals(reference, outcome(() -> p.containsAll(misses)), "containsValue override");
        assertEquals("returned true", reference, "the override answers every value");
        IntArrayList hits = IntArrayList.newListWith(3, 2, 1, 3, 9);
        assertEquals(outcome(() -> ValuesLaneRef.removeAll(hits, t, t.values())),
                outcome(() -> p.values().removeAll(hits)), "removeKey override");
        assertEquals(List.of(2, 1, 0), t.removedKeys, "source-order removeKey calls");
        ValuesLaneState.assertSameLanes(p, t, "subclass owner");
    }

    @Test
    void foreignViewKeepsPrePatchPath() {
        IntIntHashMap p = sixEntries(new IntIntHashMap());
        IntIntHashMap t = sixEntries(new IntIntHashMap());
        LoggingView viewP = new LoggingView(p);
        LoggingView viewT = new LoggingView(t);
        IntHashSet source = IntHashSet.newSetWith(1, 2, 3, 4);
        assertEquals(outcome(() -> ValuesLaneRef.removeAll(source, t, viewT)),
                outcome(() -> viewP.removeAll(source)), "foreign values view");
        assertEquals(viewT.log, viewP.log, "per-element remove(int) calls");
        assertEquals(4, viewT.log.size(), "one remove(int) call per source element");
        ValuesLaneState.assertSameLanes(p, t, "foreign values view");
    }

    @Test
    void ownViewKeepsPrePatchPath() {
        ValuesLaneOwnViewMap p = sixEntries(new ValuesLaneOwnViewMap());
        ValuesLaneOwnViewMap t = sixEntries(new ValuesLaneOwnViewMap());
        IntHashBag source = IntHashBag.newBagWith(3, 2, 1, 3, 9);
        assertEquals(outcome(() -> ValuesLaneRef.removeAll(source, t, t.values())),
                outcome(() -> p.values().removeAll(source)), "own nest-member view");
        assertEquals(5, t.removed.size(), "one remove(int) call per bag occurrence");
        ValuesLaneState.assertSameLanes(p, t, "own view subclass");
    }

    @Test
    void nullSourceThrowsSameNpe() {
        IntIntHashMap p = sixEntries(new IntIntHashMap());
        IntIntHashMap t = sixEntries(new IntIntHashMap());
        String reference = outcome(() -> ValuesLaneRef.containsAll(null, t));
        assertEquals(reference, outcome(() -> p.containsAll((IntIterable) null)), "map");
        assertEquals(reference, outcome(() -> p.values().containsAll((IntIterable) null)), "view");
        assertTrue(reference.contains("NullPointerException") && reference.contains("allSatisfy"),
                reference);
        String removal = outcome(() -> ValuesLaneRef.removeAll(null, t, t.values()));
        assertEquals(removal, outcome(() -> p.values().removeAll((IntIterable) null)), "removeAll");
        assertTrue(removal.contains("intIterator"), removal);
        ValuesLaneState.assertSameLanes(p, t, "null source");
    }

    @Test
    void sentinelOnlyValuesAreMembers() {
        IntIntHashMap p = fill(new IntIntHashMap(), 0, 777, 1, 888, 5, 1, 6, 2, 7, 3);
        IntIntHashMap t = fill(new IntIntHashMap(), 0, 777, 1, 888, 5, 1, 6, 2, 7, 3);
        IntArrayList source = IntArrayList.newListWith(777, 888, 1, 2, 777);
        String reference = outcome(() -> ValuesLaneRef.containsAll(source, t));
        assertEquals(reference, outcome(() -> p.containsAll(source)), "sentinel-slot values");
        assertEquals("returned true", reference, "zero and one key values are members");
        IntHashSet removal = IntHashSet.newSetWith(777, 888, 3, 4);
        assertEquals(outcome(() -> ValuesLaneRef.removeAll(removal, t, t.values())),
                outcome(() -> p.values().removeAll(removal)), "sentinel-slot removal");
        ValuesLaneState.assertSameLanes(p, t, "sentinel-slot removal");
    }

    @Test
    void fastPathPresentInBytecode() throws IOException {
        String outer = constants("AbstractMutableIntValuesMap.class");
        String view = constants("AbstractMutableIntValuesMap$AbstractIntValuesCollection.class");
        for (String name : List.of("isLaneSource", "isLaneOwner", "valueLaneSet")) {
            assertTrue(outer.contains(name), name);
        }
        for (String name : List.of("isLaneView", "removeAllByLane", "getNestHost")) {
            assertTrue(view.contains(name), name);
        }
    }

    private static String constants(String classFile) throws IOException {
        try (InputStream in = AbstractMutableIntValuesMap.class.getResourceAsStream(classFile)) {
            return new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }

    private static <M extends IntIntHashMap> M sixEntries(M map) {
        return fill(map, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 0);
    }

    private static <M extends IntIntHashMap> M fill(M map, int... keysAndValues) {
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }

    private static IntConsumer boomAt(int index) {
        return i -> {
            if (i == index) {
                throw new IllegalStateException("boom");
            }
        };
    }

    /** A source whose iterator puts base + index into the map before each element. */
    private static HookedList putting(IntIntHashMap map, int base, int... items) {
        return new HookedList(i -> map.put(100 + i, base + i), items);
    }

    private static IntIterable tapping(IntIntHashMap map, int... items) {
        return IntArrayList.newListWith(items).asLazy().tap(v -> map.put(300 + v, v));
    }
}
