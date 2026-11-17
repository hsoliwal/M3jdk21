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
package jdk.m3.collections.range;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * LongRangeSet.addAll merges in one pass: against adding the source ranges one by one on a twin
 * built by the same operations, equal ranges, capacity, payload and revision change, and no new
 * lanes when nothing is added.
 */
//FLLDCIM: LEETCODIFY — differential proof for LongRangeSet.addAll
class LongRangeSetAddAllMergeTest {

    private static final Field STARTS = field("starts");
    private static final Field REVISION = field("revision");

    private static Field field(String name) {
        try {
            Field f = LongRangeSet.class.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
    }

    /** Two sets built by the same random operations, so their lanes and capacities are equal. */
    private static LongRangeSet[] twins(Random random, int operations, int span, int expected) {
        LongRangeSet a = new LongRangeSet(expected);
        LongRangeSet b = new LongRangeSet(expected);
        for (int i = 0; i < operations; i++) {
            long from = random.nextInt(span);
            long to = from + random.nextInt(1 + span / 20);
            boolean remove = random.nextInt(5) == 0;
            for (LongRangeSet set : new LongRangeSet[] {a, b}) {
                if (remove) set.remove(from, to);
                else set.add(from, to);
            }
        }
        return new LongRangeSet[] {a, b};
    }

    private static void compare(LongRangeSet target, LongRangeSet twin, LongRangeSet source,
            String at) throws IllegalAccessException {
        Object lanes = STARTS.get(target);
        int revision = REVISION.getInt(target);
        int twinRevision = REVISION.getInt(twin);
        target.addAll(source);
        for (int i = 0; i < source.rangeCount(); i++) {
            twin.add(source.startAt(i), source.endAt(i));
        }
        assertArrayEquals(twin.startsToArray(), target.startsToArray(), at + " starts");
        assertArrayEquals(twin.endsToArray(), target.endsToArray(), at + " ends");
        assertEquals(twin.capacity(), target.capacity(), at + " capacity");
        assertEquals(twin.payloadBytes(), target.payloadBytes(), at + " payload");
        boolean changed = REVISION.getInt(twin) != twinRevision;
        assertEquals(changed, REVISION.getInt(target) != revision,
                at + " revision changes iff ranges change");
        if (!changed) {
            assertSame(lanes, STARTS.get(target), at + " nothing added: the lanes are kept");
        }
        target.verifyInvariants();
    }

    @Test
    void randomBatchesAgainstOneByOne() throws IllegalAccessException {
        int[] expected = {0, 1, 3, 8, 100};
        for (int seed = 0; seed < 3_000; seed++) {
            Random random = new Random(seed);
            int span = 20 + random.nextInt(400);
            int capacity = expected[seed % expected.length];
            LongRangeSet[] t = twins(random, random.nextInt(80), span, capacity);
            LongRangeSet source = twins(random, random.nextInt(40), span, 0)[0];
            compare(t[0], t[1], source, "seed " + seed);
        }
    }

    @Test
    void sourcesInsideTargetsChangeNothing() throws IllegalAccessException {
        LongRangeSet target = new LongRangeSet();
        LongRangeSet twin = new LongRangeSet();
        LongRangeSet source = new LongRangeSet();
        for (int i = 0; i < 20; i++) {
            target.add(i * 10L, i * 10L + 8);
            twin.add(i * 10L, i * 10L + 8);
            source.add(i * 10L + 1, i * 10L + 7);
        }
        compare(target, twin, source, "contained");
    }

    @Test
    void bridgingTouchingAndCovering() throws IllegalAccessException {
        long[][] sources = {
            {8, 10, 18, 20, 28, 30, 38, 40},      // touching both neighbours: bridges every gap
            {-5, 1000, 2000, 2001, 3000, 3001, 4000, 4001}, // covers all, appends
            {-100, -90, -80, -70, -60, -50, -40, -30},      // before the targets
            {1, 2, 11, 12, 15, 25, 33, 34},       // inside, inside, bridging, inside
        };
        for (long[] bounds : sources) {
            LongRangeSet target = new LongRangeSet(4);
            LongRangeSet twin = new LongRangeSet(4);
            for (int i = 0; i < 5; i++) {
                target.add(i * 10L, i * 10L + 8);
                twin.add(i * 10L, i * 10L + 8);
            }
            LongRangeSet source = new LongRangeSet();
            for (int i = 0; i < bounds.length; i += 2) source.add(bounds[i], bounds[i + 1]);
            compare(target, twin, source, java.util.Arrays.toString(bounds));
        }
    }

    @Test
    void selfEmptyAndNull() throws IllegalAccessException {
        LongRangeSet set = new LongRangeSet();
        for (int i = 0; i < 10; i++) set.add(i * 3L, i * 3L + 1);
        Object lanes = STARTS.get(set);
        int revision = REVISION.getInt(set);
        set.addAll(set);
        set.addAll(new LongRangeSet());
        assertSame(lanes, STARTS.get(set));
        assertEquals(revision, REVISION.getInt(set));
        assertEquals(10, set.rangeCount());
        assertThrows(NullPointerException.class, () -> set.addAll(null));
    }

    @Test
    void largeInterleavedBatch() throws IllegalAccessException {
        LongRangeSet target = new LongRangeSet();
        LongRangeSet twin = new LongRangeSet();
        LongRangeSet source = new LongRangeSet();
        for (int i = 0; i < 20_000; i++) {
            target.add(i * 4L, i * 4L + 1);
            twin.add(i * 4L, i * 4L + 1);
            source.add(i * 4L + 2, i * 4L + 3);
        }
        compare(target, twin, source, "interleaved 20,000 + 20,000");
        assertEquals(40_000, target.rangeCount());
    }
}
