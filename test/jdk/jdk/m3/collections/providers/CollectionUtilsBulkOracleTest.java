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
package jdk.m3.collections.providers;

import static jdk.m3.collections.providers.CollectionUtilsBulkFixtures.CONSISTENT;
import static jdk.m3.collections.providers.CollectionUtilsBulkFixtures.EXACT_ARRAY_LIST;
import static jdk.m3.collections.providers.CollectionUtilsBulkFixtures.F_BOTH;
import static jdk.m3.collections.providers.CollectionUtilsBulkFixtures.F_ITER1;
import static jdk.m3.collections.providers.CollectionUtilsBulkFixtures.F_NEXT1;
import static jdk.m3.collections.providers.CollectionUtilsBulkFixtures.F_NEXT2;
import static jdk.m3.collections.providers.CollectionUtilsBulkFixtures.MODES;
import static jdk.m3.collections.providers.CollectionUtilsBulkFixtures.RECLOG;
import static jdk.m3.collections.providers.CollectionUtilsBulkFixtures.RECLOG_VETO;
import static jdk.m3.collections.providers.CollectionUtilsBulkFixtures.THROW_HASH;
import static jdk.m3.collections.providers.CollectionUtilsBulkFixtures.attempt;
import static jdk.m3.collections.providers.CollectionUtilsBulkFixtures.checkBudget;
import static jdk.m3.collections.providers.CollectionUtilsBulkFixtures.checkContains;
import static jdk.m3.collections.providers.CollectionUtilsBulkFixtures.checkRemove;
import static jdk.m3.collections.providers.CollectionUtilsBulkFixtures.checkSameInstance;
import static jdk.m3.collections.providers.CollectionUtilsBulkFixtures.checkTargeted;
import static jdk.m3.collections.providers.CollectionUtilsBulkFixtures.containsCase;
import static jdk.m3.collections.providers.CollectionUtilsBulkFixtures.removeCase;
import static jdk.m3.collections.providers.CollectionUtilsBulkFixtures.runContains;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import jdk.m3.collections.apache.CollectionUtils;
import jdk.m3.collections.providers.CollectionUtilsBulkFixtures.Asym;
import jdk.m3.collections.providers.CollectionUtilsBulkFixtures.CollideCmp;
import jdk.m3.collections.providers.CollectionUtilsBulkFixtures.ContainsRun;
import jdk.m3.collections.providers.CollectionUtilsBulkFixtures.Fault;
import jdk.m3.collections.providers.CollectionUtilsBulkFixtures.NoHash;
import jdk.m3.collections.providers.CollectionUtilsBulkFixtures.RemoveCase;
import jdk.m3.collections.providers.CollectionUtilsBulkFixtures.ThrowHash;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * CollectionUtils.containsAll with one forward coll1 cursor, and removeCount/removeRange with a
 * bulk subList copy and clear on an exact ArrayList: in lockstep against the pre-patch code
 * path, equal results, exception classes and messages, coll2 traffic, call order up to the first
 * coll1.iterator(), input contents by identity, outstanding iterator and view outcomes, subclass
 * remove(int) logs and serialized bytes; plus the patched coll1 cost budget.
 */
//FLLDCIM: LEETCODIFY — differential proof for CollectionUtils.containsAll and removeCount
@Timeout(300)
class CollectionUtilsBulkOracleTest {

    private static final long SEED = 0x5EED_2026_1117L;
    private static final int CASES = 20_000;
    private static final int SAME_CASES = 2_000;
    private static final int LARGE = 200_000;
    private static final int HUGE = 300_000;

    private static long seed(RepetitionInfo info) {
        return SEED + info.getCurrentRepetition();
    }

    @RepeatedTest(3)
    void containsAllMatchesReference(RepetitionInfo info) {
        long seed = seed(info);
        Random r = new Random(seed);
        IntStream.range(0, CASES).forEach(i -> checkContains(
                containsCase(r, r.nextInt(MODES)), "seed " + seed + " case " + i));
    }

    @RepeatedTest(3)
    void containsAllStaysInBudget(RepetitionInfo info) {
        long seed = seed(info);
        Random r = new Random(seed);
        IntStream.range(0, CASES).forEach(i -> checkBudget(
                containsCase(r, CONSISTENT[r.nextInt(CONSISTENT.length)]).withoutFault(),
                "seed " + seed + " case " + i));
    }

    @Test
    void containsAllSameInstanceArguments() {
        Random r = new Random(SEED);
        IntStream.range(0, SAME_CASES).forEach(i -> checkSameInstance(
                containsCase(r, r.nextInt(THROW_HASH)), "case " + i));
    }

    @Test
    void containsAllRejectsNullArguments() {
        List<Object> one = List.of(1);
        assertNullRejected("coll1", null, null);
        assertNullRejected("coll1", null, one);
        assertNullRejected("coll2", one, null);
    }

    private static void assertNullRejected(String name, Collection<?> c1, Collection<?> c2) {
        String expected = "ex:java.lang.NullPointerException:" + name;
        assertEquals(expected, attempt(() -> CollectionUtilsBulkReference.containsAll(c1, c2)));
        assertEquals(expected, attempt(() -> CollectionUtils.containsAll(c1, c2)));
    }

    @Test
    void containsAllTargetedCases() {
        NoHash b = new NoHash(2);
        checkTargeted(List.of(new NoHash(1), b), List.of(b, new NoHash(1)), Fault.NONE, true);
        checkTargeted(List.of(new Asym(1)), List.of(1), Fault.NONE, false);
        checkTargeted(List.of(1), List.of(new Asym(1)), Fault.NONE, true);
        checkTargeted(List.of(0, new ThrowHash(2)), List.of(new ThrowHash(2)), Fault.NONE,
                "ex:java.lang.IllegalStateException:h2");
        List<Object> cmp = IntStream.range(0, 16).<Object>mapToObj(CollideCmp::new).toList();
        List<Object> wide = IntStream.range(15, 32).<Object>mapToObj(CollideCmp::new).toList();
        checkTargeted(cmp, wide, Fault.NONE, true);
        checkTargeted(List.of(1), List.of(), Fault.NONE, true);
    }

    @Test
    void containsAllInjectedFaults() {
        checkTargeted(List.of(1), List.of(1), new Fault(F_BOTH, 0),
                "ex:java.lang.IllegalStateException:it2");
        checkTargeted(List.of(1), List.of(1), new Fault(F_ITER1, 0),
                "ex:java.lang.IllegalStateException:it1");
        checkTargeted(List.of(0, 1, 2), List.of(0, 1, 2), new Fault(F_NEXT2, 2),
                "ex:java.lang.IllegalStateException:next2#2");
    }

    @Test
    void containsAllSameOrderIsLinear() {
        List<Object> xs = IntStream.range(0, LARGE).<Object>mapToObj(Integer::valueOf).toList();
        ContainsRun run = runContains(CollectionUtils::containsAll, new ArrayList<>(xs),
                new ArrayList<>(xs), new Fault(F_NEXT1, LARGE + 1));
        assertEquals(Boolean.TRUE, run.result());
        assertEquals(LARGE, run.coll1Nexts());
        assertEquals(1, run.coll1Iterators());
    }

    @RepeatedTest(3)
    void removeCountMatchesReference(RepetitionInfo info) {
        long seed = seed(info);
        Random r = new Random(seed);
        IntStream.range(0, CASES).forEach(i -> checkRemove(
                removeCase(r, false), "seed " + seed + " case " + i));
    }

    @RepeatedTest(3)
    void removeRangeMatchesReference(RepetitionInfo info) {
        long seed = seed(info);
        Random r = new Random(seed);
        IntStream.range(0, CASES).forEach(i -> checkRemove(
                removeCase(r, true), "seed " + seed + " case " + i));
    }

    @Test
    void removeCountTargetedCases() {
        List<Object> xs = List.of(0, 1, 2, 3, 4, 5);
        checkRemove(new RemoveCase(EXACT_ARRAY_LIST, xs, false, 2, 0), "count zero");
        checkRemove(new RemoveCase(EXACT_ARRAY_LIST, xs, false, Integer.MAX_VALUE - 1, 2),
                "overflow");
        checkRemove(new RemoveCase(RECLOG, xs, false, 1, 3), "subclass");
        checkRemove(new RemoveCase(RECLOG_VETO, xs, false, 1, 3), "veto");
        checkRemove(new RemoveCase(EXACT_ARRAY_LIST, xs, false, 1, 3), "bulk");
        checkRemove(new RemoveCase(EXACT_ARRAY_LIST, xs, true, 4, 2), "end before start");
        List<Object> list = new ArrayList<>(xs);
        assertEquals(List.of(1, 2, 3), CollectionUtils.removeCount(list, 1, 3));
        assertEquals(List.of(0, 4, 5), list);
        assertEquals("ex:java.util.NoSuchElementException:null", attempt(() ->
                CollectionUtils.removeCount(new ArrayList<>(xs), Integer.MAX_VALUE - 1, 2)));
        assertEquals("ex:java.lang.NullPointerException:input",
                attempt(() -> CollectionUtils.removeCount(null, 0, 0)));
    }

    @Test
    void removeCountLargeArrayList() {
        List<Integer> list = new ArrayList<>(IntStream.range(0, HUGE).boxed().toList());
        Collection<Integer> removed = CollectionUtils.removeCount(list, 1_000, HUGE / 2);
        assertEquals(ArrayList.class, removed.getClass());
        assertEquals(IntStream.range(1_000, 1_000 + HUGE / 2).boxed().toList(), removed);
        assertEquals(IntStream.concat(IntStream.range(0, 1_000),
                IntStream.range(1_000 + HUGE / 2, HUGE)).boxed().toList(), list);
    }
}
