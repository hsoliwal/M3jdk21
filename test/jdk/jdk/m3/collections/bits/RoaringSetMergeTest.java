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
package jdk.m3.collections.bits;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/**
 * RoaringIntSet/RoaringLongSet union and intersection merge container by container: against the
 * verbatim pre-change copies RoaringIntSetReference/RoaringLongSetReference, built by the same
 * adds and removes, equal private lanes (keys, types, cardinalities, containers with their
 * capacity tails, counts), equal public projections and equal behaviour of later adds/removes
 * (promotion and demotion hysteresis), with the operands left unchanged.
 */
//FLLDCIM: LEETCODIFY - differential proof for container-wise Roaring union/intersection
class RoaringSetMergeTest {
    private static Object read(Object owner, String name) {
        try {
            Field field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(owner);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static String slot(Object container) {
        if (container == null) {
            return "null";
        }
        if (container instanceof char[] chars) {
            return "c" + chars.length + new String(chars).hashCode() + Arrays.toString(chars);
        }
        if (container instanceof long[] words) {
            return "w" + words.length + Arrays.toString(words);
        }
        return "set{" + intLanes(container) + "}";
    }

    private static String intLanes(Object set) {
        StringBuilder lanes = new StringBuilder();
        lanes.append(Arrays.toString((int[]) read(set, "highKeys")));
        lanes.append(Arrays.toString((byte[]) read(set, "types")));
        lanes.append(Arrays.toString((int[]) read(set, "cardinalities")));
        Object[] containers = (Object[]) read(set, "containers");
        lanes.append(containers.length);
        for (Object container : containers) {
            lanes.append('|').append(slot(container));
        }
        return lanes + "#" + read(set, "containerCount") + "#" + read(set, "cardinality");
    }

    private static String longLanes(Object set) {
        StringBuilder lanes = new StringBuilder(Arrays.toString((int[]) read(set, "highKeys")));
        Object[] containers = (Object[]) read(set, "containers");
        lanes.append(containers.length);
        for (Object container : containers) {
            lanes.append('|').append(slot(container));
        }
        return lanes + "#" + read(set, "containerCount") + "#" + read(set, "cardinality");
    }

    private static String outcome(Supplier<String> action) {
        try {
            return action.get();
        } catch (RuntimeException e) {
            return "!" + e.getClass().getName() + ": " + e.getMessage();
        }
    }

    /** Values that cross the 2048/4096 thresholds, chunk edges and the sign bit. */
    private static int value(Random random, int[] chunks) {
        int chunk = chunks[random.nextInt(chunks.length)];
        int low = random.nextInt(8) == 0 ? (random.nextBoolean() ? 0 : 65535) : random.nextInt(65536
                );
        return (chunk << 16) | low;
    }

    private static final class IntPair {
        final RoaringIntSet impl;
        final RoaringIntSetReference ref;

        IntPair(RoaringIntSet impl, RoaringIntSetReference ref) {
            this.impl = impl;
            this.ref = ref;
        }

        void add(int v) {
            assertEquals(ref.add(v), impl.add(v));
        }

        void remove(int v) {
            assertEquals(ref.remove(v), impl.remove(v));
        }

        void same(String where) {
            assertEquals(intLanes(ref), intLanes(impl), where);
            assertEquals(ref.containerReferenceSlots(), impl.containerReferenceSlots(), where);
            assertEquals(ref.primitivePayloadBytes(), impl.primitivePayloadBytes(), where);
            StringBuilder a = new StringBuilder();
            StringBuilder b = new StringBuilder();
            ref.forEach(v -> a.append(v).append(','));
            impl.forEach(v -> b.append(v).append(','));
            assertEquals(a.toString(), b.toString(), where);
        }
    }

    private static IntPair intPair(Random random, int[] chunks, int adds, int removes) {
        IntPair pair = new IntPair(new RoaringIntSet(), new RoaringIntSetReference());
        for (int i = 0; i < adds; i++) {
            pair.add(value(random, chunks));
        }
        for (int i = 0; i < removes; i++) {
            pair.remove(value(random, chunks));
        }
        return pair;
    }

    private static int[] chunks(Random random) {
        int[] chunks = new int[1 + random.nextInt(6)];
        for (int i = 0; i < chunks.length; i++) {
            chunks[i] = random.nextInt(4) == 0 ? random.nextInt() >> 16 : random.nextInt(9) - 4;
        }
        return chunks;
    }

    private static void mergeInts(Random random, int scale) {
        int[] chunks = chunks(random);
        IntPair a = intPair(random, chunks, random.nextInt(scale), random.nextInt(scale / 2 + 1));
        IntPair b = intPair(random, chunks(random).length > 3 ? chunks(random) : chunks,
                random.nextInt(scale), random.nextInt(scale / 2 + 1));
        String aBefore = intLanes(a.impl);
        String bBefore = intLanes(b.impl);
        IntPair union = new IntPair(a.impl.union(b.impl), a.ref.union(b.ref));
        IntPair meet = new IntPair(a.impl.intersection(b.impl), a.ref.intersection(b.ref));
        union.same("union");
        meet.same("intersection");
        assertEquals(aBefore, intLanes(a.impl), "left operand changed");
        assertEquals(bBefore, intLanes(b.impl), "right operand changed");
        for (int i = 0; i < scale / 4; i++) {
            union.remove(value(random, chunks));
            meet.add(value(random, chunks));
        }
        union.same("union after edits");
        meet.same("intersection after edits");
    }

    @Test
    void intMergesMatchReference() {
        Random random = new Random(0x80A41L);
        for (int trial = 0; trial < 1200; trial++) {
            mergeInts(random, trial % 6 == 0 ? 14_000 : trial % 3 == 0 ? 5000 : 300);
        }
    }

    @Test
    void intEdgeOperandsMatchReference() {
        Random random = new Random(0x80A42L);
        IntPair empty = new IntPair(new RoaringIntSet(), new RoaringIntSetReference());
        for (int trial = 0; trial < 60; trial++) {
            IntPair a = intPair(random, chunks(random), random.nextInt(9000), random.nextInt(3000));
            new IntPair(a.impl.union(a.impl), a.ref.union(a.ref)).same("self union");
            new IntPair(a.impl.intersection(a.impl), a.ref.intersection(a.ref)).same("self meet");
            new IntPair(a.impl.union(empty.impl), a.ref.union(empty.ref)).same("union empty");
            new IntPair(empty.impl.union(a.impl), empty.ref.union(a.ref)).same("empty union");
            new IntPair(a.impl.intersection(empty.impl), a.ref.intersection(empty.ref)).same("meet")
                    ;
        }
        assertEquals(outcome(() -> String.valueOf(empty.ref.union(null))),
                outcome(() -> String.valueOf(empty.impl.union(null))));
        assertEquals(outcome(() -> String.valueOf(empty.ref.intersection(null))),
                outcome(() -> String.valueOf(empty.impl.intersection(null))));
    }

    /** count values of chunk 3 at stride 1 from start; promoted first (5000) when shrink is set. */
    private static IntPair chunkOf(int start, int count, boolean shrink) {
        IntPair pair = new IntPair(new RoaringIntSet(), new RoaringIntSetReference());
        int fill = shrink ? Math.max(count, 5000) : count;
        for (int i = 0; i < fill; i++) {
            pair.add((3 << 16) | ((start + i) & 0xFFFF));
        }
        for (int i = count; i < fill; i++) {
            pair.remove((3 << 16) | ((start + i) & 0xFFFF));
        }
        return pair;
    }

    /** Counts at the 2048/4096 hysteresis edges, shrunk bitmaps, and edits after the merge. */
    @Test
    void thresholdShapesMatchReference() {
        int[] counts = {1, 2047, 2048, 2049, 3000, 4095, 4096, 4097, 6000};
        int[] offsets = {0, 1, 2047, 2048, 4096, 30000};
        for (int left : counts) {
            for (int right : counts) {
                for (int offset : offsets) {
                    for (int shape = 0; shape < 4; shape++) {
                        IntPair a = chunkOf(0, left, (shape & 1) != 0);
                        IntPair b = chunkOf(offset, right, (shape & 2) != 0);
                        String aLanes = intLanes(a.impl);
                        String bLanes = intLanes(b.impl);
                        IntPair union = new IntPair(a.impl.union(b.impl), a.ref.union(b.ref));
                        IntPair meet = new IntPair(a.impl.intersection(b.impl),
                                a.ref.intersection(b.ref));
                        union.same("union " + left + "/" + right + "@" + offset + " " + shape);
                        meet.same("meet " + left + "/" + right + "@" + offset + " " + shape);
                        for (int v = 0; v < 70; v++) {
                            union.remove((3 << 16) | (offset + v * 37));
                            meet.add((3 << 16) | (v * 41));
                        }
                        union.same("union edited");
                        meet.same("meet edited");
                        assertEquals(aLanes, intLanes(a.impl), "left operand after edits");
                        assertEquals(bLanes, intLanes(b.impl), "right operand after edits");
                    }
                }
            }
        }
    }

    /** Several chunks whose merges all come out as bitmaps must not share a word buffer. */
    @Test
    void bitmapChunksOwnTheirWords() {
        int[][] counts = {{3000, 3000}, {2500, 5000}, {5000, 5000}, {4000, 1000}};
        for (int[] pairCounts : counts) {
            IntPair a = new IntPair(new RoaringIntSet(), new RoaringIntSetReference());
            IntPair b = new IntPair(new RoaringIntSet(), new RoaringIntSetReference());
            for (int chunk = 2; chunk <= 5; chunk++) {
                for (int i = 0; i < pairCounts[0]; i++) {
                    a.add((chunk << 16) | (2 * i));
                }
                for (int i = 0; i < pairCounts[1]; i++) {
                    b.add((chunk << 16) | (2 * i + 1));
                }
            }
            IntPair union = new IntPair(a.impl.union(b.impl), a.ref.union(b.ref));
            union.same("multi-chunk union " + pairCounts[0] + "+" + pairCounts[1]);
            for (int chunk = 2; chunk <= 5; chunk++) {
                union.remove((chunk << 16) | (chunk * 7));
                union.add((chunk << 16) | 60001);
            }
            union.same("multi-chunk union edited");
        }
    }

    /** A bitmap chunk only the right operand has is copied, so editing the union leaves it. */
    @Test
    void rightOnlyBitmapIsNotShared() {
        for (int count : new int[] {2049, 4096, 4097, 9000}) {
            IntPair a = new IntPair(new RoaringIntSet(), new RoaringIntSetReference());
            IntPair b = new IntPair(new RoaringIntSet(), new RoaringIntSetReference());
            a.add(3 << 16);
            for (int i = 0; i < count; i++) {
                b.add((4 << 16) | i);
            }
            String bLanes = intLanes(b.impl);
            IntPair union = new IntPair(a.impl.union(b.impl), a.ref.union(b.ref));
            union.same("right-only union " + count);
            for (int i = 0; i < 50; i++) {
                union.remove((4 << 16) | (i * 13));
                union.add((4 << 16) | (60000 + i));
            }
            union.same("right-only union edited " + count);
            assertEquals(bLanes, intLanes(b.impl), "right operand shares its words, " + count);
        }
    }

    private static long longValue(Random random, int[] highs) {
        long high = highs[random.nextInt(highs.length)];
        int low = random.nextInt(4) == 0 ? random.nextInt(70_000) : random.nextInt();
        return (high << 32) | (low & 0xFFFF_FFFFL);
    }

    private static void sameLongs(RoaringLongSet impl, RoaringLongSetReference ref, String where) {
        assertEquals(longLanes(ref), longLanes(impl), where);
        assertEquals(ref.containerReferenceSlots(), impl.containerReferenceSlots(), where);
        StringBuilder a = new StringBuilder();
        StringBuilder b = new StringBuilder();
        ref.forEach(v -> a.append(v).append(','));
        impl.forEach(v -> b.append(v).append(','));
        assertEquals(a.toString(), b.toString(), where);
    }

    @Test
    void longMergesMatchReference() {
        Random random = new Random(0x80A43L);
        for (int trial = 0; trial < 600; trial++) {
            int[] highs = {0, -1, 1, Integer.MIN_VALUE, Integer.MAX_VALUE, random.nextInt(), -2};
            RoaringLongSet[] impl = {new RoaringLongSet(), new RoaringLongSet()};
            RoaringLongSetReference[] ref = {new RoaringLongSetReference(), new
                    RoaringLongSetReference()};
            for (int side = 0; side < 2; side++) {
                int adds = random.nextInt(trial % 5 == 0 ? 9000 : 400);
                for (int i = 0; i < adds; i++) {
                    long v = longValue(random, highs);
                    assertEquals(ref[side].add(v), impl[side].add(v));
                }
                for (int i = 0; i < adds / 3; i++) {
                    long v = longValue(random, highs);
                    assertEquals(ref[side].remove(v), impl[side].remove(v));
                }
            }
            sameLongs(impl[0].union(impl[1]), ref[0].union(ref[1]), "long union");
            sameLongs(impl[0].intersection(impl[1]), ref[0].intersection(ref[1]), "long meet");
            sameLongs(impl[0].union(impl[0]), ref[0].union(ref[0]), "long self union");
        }
        assertTrue(true);
    }
}
