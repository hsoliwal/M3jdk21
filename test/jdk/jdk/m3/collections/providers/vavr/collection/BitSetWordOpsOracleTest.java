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
package jdk.m3.collections.vavr.collection;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import jdk.m3.collections.vavr.collection.BitSetOracleKit.Mapping;
import jdk.m3.collections.vavr.collection.BitSetOracleKit.Plan;
import jdk.m3.collections.vavr.collection.BitSetOracleKit.Side;
import jdk.m3.collections.vavr.collection.BitSetOracleOps.Named;
import jdk.m3.collections.vavr.collection.BitSetOracleOps.Outcome;
import org.junit.jupiter.api.Test;

/**
 * Leaf vavr-bitset-word-ops: the patched BitSet (word AND/ANDNOT/OR, PDEP rank select, highest
 * bit, int lane for addAll) against RefBitSetModule, the pre-patch module copy, driven in lockstep
 * with the same element mappings, words and argument sources. Every step compares the call log,
 * exception class and message, result class, words, identity, text and hash, and the receiver.
 */
//FLLDCIM: LEETCODIFY — differential proof for vavr BitSet word operations
@SuppressWarnings({"unchecked", "rawtypes", "auxiliaryclass"})
class BitSetWordOpsOracleTest {

    private static final int SEEDS = 64;
    private static final int STEPS = 2_000;
    private static final java.util.List<Named> OPS = BitSetOracleOps.ops();
    private static final String[] MAX_OPS = {"last", "lastOption", "take", "drop", "takeRight",
        "dropRight", "tail", "removeAll SAME", "retainAll SAME", "addAll SAME", "addAll JLIST",
        "union same", "intersect same", "removeAll JLIST"};
    private static final String[] EDGE_OPS = {"ofAll SPECIAL", "ofAll SPECIAL_LIST",
        "addAll SPECIAL", "addAll SPECIAL_LIST", "of special", "tabulate", "fill",
        "removeAll SPECIAL", "retainAll SPECIAL", "orElse SPECIAL", "ofAll LAZY", "ofAll VIEW",
        "ofAll SPECIAL_LAZY", "ofAll SPECIAL_VIEW", "addAll SPECIAL_VIEW", "orElse SPECIAL_VIEW"};

    private int serialTick;

    @Test
    void randomOpsMatchReference() {
        for (int seed = 0; seed < SEEDS; seed++) {
            Random r = new Random(seed);
            for (int k = 0; k < STEPS; k++) {
                Mapping m = BitSetOracleKit.ALL[r.nextInt(BitSetOracleKit.ALL.length)];
                Named op = OPS.get(r.nextInt(OPS.size()));
                lockstep(op, m, BitSetOracleKit.randomPlan(r, m, false), seed + "/" + k, true);
            }
        }
    }

    @Test
    void nonCanonicalShapesMatchReference() {
        Random r = new Random(-1);
        for (Mapping m : BitSetOracleKit.ALL) {
            for (Named op : OPS) {
                lockstep(op, m, BitSetOracleKit.randomPlan(r, m, true), "ragged", true);
            }
        }
    }

    @Test
    void builderEdgeCasesMatchReference() {
        for (Mapping m : BitSetOracleKit.ALL) {
            for (java.util.List<Object> special : edgeLists(m)) {
                Plan plan = BitSetOracleKit.fixedPlan(m, new int[] {0, 1}, new int[] {1}, special);
                for (String name : EDGE_OPS) {
                    lockstep(op(name), m, plan, "edge " + special, true);
                }
            }
        }
    }

    @Test
    void maxValueSetsMatchReference() {
        int[] recv = {1, Integer.MAX_VALUE};
        int[] arg = {Integer.MAX_VALUE};
        for (Mapping m : new Mapping[] {BitSetOracleKit.DEFAULT, BitSetOracleKit.LONGS}) {
            java.util.List<Object> special = java.util.List.of(m.elem.apply(Integer.MAX_VALUE));
            Plan plan = BitSetOracleKit.fixedPlan(m, recv, arg, special);
            for (String name : MAX_OPS) {
                lockstep(op(name), m, plan, "max", false);
            }
        }
    }

    @Test
    void rangeFactoriesMatchReference() {
        rangeSteps(BitSetOracleKit.DEFAULT, intRanges());
        rangeSteps(BitSetOracleKit.CHARS, charRanges());
        rangeSteps(BitSetOracleKit.LONGS, longRanges());
    }

    @Test
    void exactHoldsOnlyForBuiltIns() throws ReflectiveOperationException {
        Method exact = BitSetModule.AbstractBitSet.class.getDeclaredMethod("exact");
        exact.setAccessible(true);
        for (Mapping m : BitSetOracleKit.ALL) {
            assertEquals(m.exact, exact.invoke(BitSetOracleKit.emptyOf(m, false)), m.name);
        }
        BitSet<?>[] factories = {BitSet.of(1, 70), BitSet.ofAll('a', 'b'), BitSet.ofAll((byte) 1),
            BitSet.ofAll((short) 1), BitSet.ofAll(1L), BitSet.range('a', 'z'),
            BitSet.rangeClosed(1L, 9L), BitSet.ofAll(java.util.List.of(3, 200))};
        for (BitSet<?> set : factories) {
            assertEquals(true, exact.invoke(set), set.toString());
        }
        assertEquals(false, exact.invoke(deserialize(serialize(BitSet.of(1, 70)))), "read back");
        assertEquals(false, exact.invoke(BitSet.ofAll(true, false)), "booleans");
    }

    @Test
    void serialFormMatchesReference() {
        Class<?>[][] twins = {
            {BitSetModule.AbstractBitSet.class, RefBitSetModule.RefAbstractBitSet.class},
            {BitSetModule.BitSet1.class, RefBitSetModule.RefBitSet1.class},
            {BitSetModule.BitSet2.class, RefBitSetModule.RefBitSet2.class},
            {BitSetModule.BitSetN.class, RefBitSetModule.RefBitSetN.class}};
        for (Class<?>[] twin : twins) {
            assertEquals(fields(twin[1]), fields(twin[0]), twin[0].getName());
            assertEquals(1L, ObjectStreamClass.lookup(twin[0]).getSerialVersionUID());
        }
        assertEquals("fromInt:jdk.m3.collections.vavr.Function1 "
                + "toInt:jdk.m3.collections.vavr.Function1", fields(twins[0][0]));
        assertEquals("elements:long len:int", fields(twins[1][0]));
        assertEquals("elements1:long elements2:long len:int", fields(twins[2][0]));
        assertEquals("len:int elements:[J", fields(twins[3][0]));
    }

    private void lockstep(Named op, Mapping m, Plan plan, String at, boolean serial) {
        Side mine = new Side(false, m, plan);
        Side want = new Side(true, m, plan);
        Outcome got = BitSetOracleOps.run(op, mine);
        Outcome ref = BitSetOracleOps.run(op, want);
        String where = at + " " + m + " " + op.name + " recv=" + BitSetOracleOps.shape(mine.recv);
        assertEquals(ref.text, got.text, where);
        if (m.exact && got.result instanceof BitSet<?> a && ref.result instanceof BitSet<?> b) {
            assertTrue(a.equals(b) && b.equals(a), where + " equals");
            if (serial && serialTick++ % 8 == 0) {
                serialCheck((BitSet<Object>) a, where);
            }
        }
    }

    private static void serialCheck(BitSet<Object> res, String where) {
        byte[] bytes = serialize(res);
        assertEquals(res, deserialize(bytes), where + " round trip");
        BitSetModule.AbstractBitSet<Object> a = (BitSetModule.AbstractBitSet<Object>) res;
        if (a.getWordsNum() == 1 || a.getWord(a.getWordsNum() - 1) != 0) {
            BitSet<Object> rebuilt = a.createEmpty();
            for (Object x : res) {
                rebuilt = rebuilt.add(x);
            }
            assertArrayEquals(serialize(rebuilt), bytes, where + " serial form");
        }
    }

    private static Named op(String name) {
        return OPS.stream().filter(op -> op.name.equals(name)).findFirst().orElseThrow();
    }

    private static java.util.List<java.util.List<Object>> edgeLists(Mapping m) {
        Object a = m.elem.apply(0);
        Object b = m.elem.apply(Math.min(m.maxBit, 130));
        Object neg = m.negative.orElse(b);
        return java.util.List.of(nl((Object) null), nl(null, a), nl(a, null), nl(a, b, null),
                nl(b, a, null, a), nl(neg), nl(a, neg), nl(neg, null), nl(null, neg), nl(a, a, b),
                nl(b, b), nl(b, neg, a));
    }

    private static java.util.List<Object> nl(Object... xs) {
        return Arrays.asList(xs);
    }

    private void rangeSteps(Mapping m, java.util.List<Named> ranges) {
        Plan plan = BitSetOracleKit.fixedPlan(m, new int[] {0}, new int[] {0}, nl(m.elem.apply(0)));
        for (Named range : ranges) {
            lockstep(range, m, plan, "range", true);
        }
    }

    private static Named range(String name, Supplier<BitSet<?>> mine, Supplier<Iterable> source) {
        return new Named(name, s -> s.ref ? s.ofAll(source.get()) : mine.get());
    }

    private static java.util.List<Named> intRanges() {
        return java.util.List.of(
                range("range", () -> BitSet.range(0, 70), () -> Iterator.range(0, 70)),
                range("range neg", () -> BitSet.range(-1, 70), () -> Iterator.range(-1, 70)),
                range("range none", () -> BitSet.range(5, 5), () -> Iterator.range(5, 5)),
                range("rangeBy", () -> BitSet.rangeBy(0, 300, 7),
                        () -> Iterator.rangeBy(0, 300, 7)),
                range("rangeBy down", () -> BitSet.rangeBy(200, 0, -3),
                        () -> Iterator.rangeBy(200, 0, -3)),
                range("rangeBy zero", () -> BitSet.rangeBy(0, 5, 0),
                        () -> Iterator.rangeBy(0, 5, 0)),
                range("rangeClosed", () -> BitSet.rangeClosed(60, 200),
                        () -> Iterator.rangeClosed(60, 200)),
                range("rangeClosedBy", () -> BitSet.rangeClosedBy(200, -5, -9),
                        () -> Iterator.rangeClosedBy(200, -5, -9)));
    }

    private static java.util.List<Named> charRanges() {
        return java.util.List.of(
                range("chars", () -> BitSet.range('a', 'z'), () -> Iterator.range('a', 'z')),
                range("chars by", () -> BitSet.rangeBy('a', 'z', 3),
                        () -> Iterator.rangeBy('a', 'z', 3)),
                range("chars closed", () -> BitSet.rangeClosed('x', 'z'),
                        () -> Iterator.rangeClosed('x', 'z')),
                range("chars closed by", () -> BitSet.rangeClosedBy('z', 'a', -2),
                        () -> Iterator.rangeClosedBy('z', 'a', -2)));
    }

    private static java.util.List<Named> longRanges() {
        return java.util.List.of(
                range("longs", () -> BitSet.range(0L, 100L), () -> Iterator.range(0L, 100L)),
                range("longs by", () -> BitSet.rangeBy(0L, 1000L, 13L),
                        () -> Iterator.rangeBy(0L, 1000L, 13L)),
                range("longs closed", () -> BitSet.rangeClosed(-2L, 3L),
                        () -> Iterator.rangeClosed(-2L, 3L)),
                range("longs closed by", () -> BitSet.rangeClosedBy(100L, 0L, -7L),
                        () -> Iterator.rangeClosedBy(100L, 0L, -7L)));
    }

    private static String fields(Class<?> type) {
        return Arrays.stream(ObjectStreamClass.lookup(type).getFields())
                .map(f -> f.getName() + ":" + f.getType().getName())
                .collect(Collectors.joining(" "));
    }

    private static byte[] serialize(Object value) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    private static Object deserialize(byte[] bytes) {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return in.readObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
