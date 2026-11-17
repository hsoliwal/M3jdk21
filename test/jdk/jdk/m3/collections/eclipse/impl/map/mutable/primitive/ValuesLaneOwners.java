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

import java.util.List;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;

/**
 * The seven module maps with int values, each with typed put, removeKey, keySet().freeze() and
 * keysView adapters over an int key code. Codes 0 and 1 are the EMPTY_KEY and REMOVED_KEY
 * sentinel keys of every key type; codes at or above EXTREME pick a per-type extreme key
 * (MIN/MAX, NaN, a non-canonical NaN, -0.0, infinities, the smallest subnormal).
 */
//FLLDCIM: LEETCODIFY — lockstep owners for the values-lane differential
final class ValuesLaneOwners {

    static final int EXTREME = 1_000_000;
    private static final int[] INTS = {Integer.MIN_VALUE, Integer.MAX_VALUE, -1, 127, -128,
            Short.MAX_VALUE, Character.MAX_VALUE, 1 << 20};
    private static final long[] LONGS = {Long.MIN_VALUE, Long.MAX_VALUE, -1L, 1L << 40,
            Integer.MIN_VALUE, Integer.MAX_VALUE, 0x8000_0000L, -(1L << 33)};
    private static final float[] FLOATS = {Float.NaN, -0.0f, Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY, Float.MAX_VALUE, Float.MIN_VALUE, 0.5f,
            Float.intBitsToFloat(0x7fc00001)};
    private static final double[] DOUBLES = {Double.NaN, -0.0d, Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY, Double.MAX_VALUE, Double.MIN_VALUE, 0.5d,
            Double.longBitsToDouble(0x7ff8000000000001L)};
    static final List<ValuesLaneOwners> ALL = List.of(ofByte(), ofChar(), ofShort(), ofInt(),
            ofLong(), ofFloat(), ofDouble());

    final String name;
    final Supplier<AbstractMutableIntValuesMap> make;
    final ObjLongConsumer<AbstractMutableIntValuesMap> put;
    final ObjIntConsumer<AbstractMutableIntValuesMap> removeKey;
    final Function<AbstractMutableIntValuesMap, Object> freeze;
    final Function<AbstractMutableIntValuesMap, String> keys;

    private ValuesLaneOwners(String name, Supplier<AbstractMutableIntValuesMap> make,
            ObjLongConsumer<AbstractMutableIntValuesMap> put,
            ObjIntConsumer<AbstractMutableIntValuesMap> removeKey,
            Function<AbstractMutableIntValuesMap, Object> freeze,
            Function<AbstractMutableIntValuesMap, String> keys) {
        this.name = name;
        this.make = make;
        this.put = put;
        this.removeKey = removeKey;
        this.freeze = freeze;
        this.keys = keys;
    }

    /** Packs a key code and a value into the long the put adapter takes. */
    static long pack(int code, int value) {
        return ((long) code << 32) | (value & 0xffff_ffffL);
    }

    private static int code(long packed) {
        return (int) (packed >> 32);
    }

    private static int value(long packed) {
        return (int) packed;
    }

    private static int ik(int code) {
        return code >= EXTREME ? INTS[(code - EXTREME) % INTS.length] : code;
    }

    private static long lk(int code) {
        return code >= EXTREME ? LONGS[(code - EXTREME) % LONGS.length] : code;
    }

    private static float fk(int code) {
        return code >= EXTREME ? FLOATS[(code - EXTREME) % FLOATS.length] : code;
    }

    private static double dk(int code) {
        return code >= EXTREME ? DOUBLES[(code - EXTREME) % DOUBLES.length] : code;
    }

    private static ValuesLaneOwners ofByte() {
        return new ValuesLaneOwners("ByteIntHashMap", ByteIntHashMap::new,
                (m, kv) -> ((ByteIntHashMap) m).put((byte) ik(code(kv)), value(kv)),
                (m, c) -> ((ByteIntHashMap) m).removeKey((byte) ik(c)),
                m -> ((ByteIntHashMap) m).keySet().freeze(),
                m -> ((ByteIntHashMap) m).keysView().makeString());
    }

    private static ValuesLaneOwners ofChar() {
        return new ValuesLaneOwners("CharIntHashMap", CharIntHashMap::new,
                (m, kv) -> ((CharIntHashMap) m).put((char) ik(code(kv)), value(kv)),
                (m, c) -> ((CharIntHashMap) m).removeKey((char) ik(c)),
                m -> ((CharIntHashMap) m).keySet().freeze(),
                m -> ((CharIntHashMap) m).keysView().makeString());
    }

    private static ValuesLaneOwners ofShort() {
        return new ValuesLaneOwners("ShortIntHashMap", ShortIntHashMap::new,
                (m, kv) -> ((ShortIntHashMap) m).put((short) ik(code(kv)), value(kv)),
                (m, c) -> ((ShortIntHashMap) m).removeKey((short) ik(c)),
                m -> ((ShortIntHashMap) m).keySet().freeze(),
                m -> ((ShortIntHashMap) m).keysView().makeString());
    }

    private static ValuesLaneOwners ofInt() {
        return new ValuesLaneOwners("IntIntHashMap", IntIntHashMap::new,
                (m, kv) -> ((IntIntHashMap) m).put(ik(code(kv)), value(kv)),
                (m, c) -> ((IntIntHashMap) m).removeKey(ik(c)),
                m -> ((IntIntHashMap) m).keySet().freeze(),
                m -> ((IntIntHashMap) m).keysView().makeString());
    }

    private static ValuesLaneOwners ofLong() {
        return new ValuesLaneOwners("LongIntHashMap", LongIntHashMap::new,
                (m, kv) -> ((LongIntHashMap) m).put(lk(code(kv)), value(kv)),
                (m, c) -> ((LongIntHashMap) m).removeKey(lk(c)),
                m -> ((LongIntHashMap) m).keySet().freeze(),
                m -> ((LongIntHashMap) m).keysView().makeString());
    }

    private static ValuesLaneOwners ofFloat() {
        return new ValuesLaneOwners("FloatIntHashMap", FloatIntHashMap::new,
                (m, kv) -> ((FloatIntHashMap) m).put(fk(code(kv)), value(kv)),
                (m, c) -> ((FloatIntHashMap) m).removeKey(fk(c)),
                m -> ((FloatIntHashMap) m).keySet().freeze(),
                m -> ((FloatIntHashMap) m).keysView().makeString());
    }

    private static ValuesLaneOwners ofDouble() {
        return new ValuesLaneOwners("DoubleIntHashMap", DoubleIntHashMap::new,
                (m, kv) -> ((DoubleIntHashMap) m).put(dk(code(kv)), value(kv)),
                (m, c) -> ((DoubleIntHashMap) m).removeKey(dk(c)),
                m -> ((DoubleIntHashMap) m).keySet().freeze(),
                m -> ((DoubleIntHashMap) m).keysView().makeString());
    }
}
