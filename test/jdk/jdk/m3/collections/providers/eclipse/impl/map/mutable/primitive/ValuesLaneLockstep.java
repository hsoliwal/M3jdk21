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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;
import java.util.function.BooleanSupplier;
import jdk.m3.collections.eclipse.api.IntIterable;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableIntCollection;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.IntHashBag;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.IntArrayList;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.IntHashSet;

/**
 * Lockstep driver: a patched map P and a twin T built by the same owner take the same seeded
 * stream of puts (sentinel keys, small range, wide range, extremes; a small value alphabet
 * forcing duplicates), removeKeys, keySet().freeze() calls, put bursts (rehash) and
 * containsAll/removeAll calls. P runs the patched methods, T the verbatim pre-patch bodies;
 * sources of 0..64 elements straddle the threshold of 4 and mix hits, sentinel-slot values and
 * misses. After every op the results, exceptions and twin state must agree.
 */
//FLLDCIM: LEETCODIFY — seeded lockstep driver: patched map against pre-patch twin
final class ValuesLaneLockstep {

    private static final int[] VALUES = {-3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 31, 32, 63,
            64, Integer.MIN_VALUE, Integer.MAX_VALUE};
    private static final int[] KINDS = {0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 4,
            5};
    private static final int LANE_KINDS = 3;
    private static final int MIN_LANE_SOURCE = 4;
    private static final int KEPT_FROZEN = 3;

    final ValuesLaneOwners owner;
    final AbstractMutableIntValuesMap p;
    final AbstractMutableIntValuesMap t;
    private final List<Object> frozenP = new ArrayList<>();
    private final List<Object> frozenT = new ArrayList<>();
    private final SplittableRandom random;
    private int step;
    int laneOps;

    ValuesLaneLockstep(ValuesLaneOwners owner, long seed) {
        this.owner = owner;
        this.p = owner.make.get();
        this.t = owner.make.get();
        this.random = new SplittableRandom(seed);
    }

    /** Result text of a call: its return value, or its exception class and message. */
    static String outcome(BooleanSupplier call) {
        try {
            return "returned " + call.getAsBoolean();
        } catch (RuntimeException e) {
            return "threw " + e.getClass().getName() + ": " + helpfulNpeFree(e.getMessage());
        }
    }

    /** Lane sources (kinds 0..2), lazy and unmodifiable wrappers (3, 4), the map's own view (5). */
    static IntIterable source(int kind, int[] elements, AbstractMutableIntValuesMap map) {
        return switch (kind) {
            case 0 -> IntHashSet.newSetWith(elements);
            case 1 -> IntArrayList.newListWith(elements);
            case 2 -> IntHashBag.newBagWith(elements);
            case 3 -> IntArrayList.newListWith(elements).asLazy();
            case 4 -> IntHashSet.newSetWith(elements).asUnmodifiable();
            default -> map.values();
        };
    }

    void op() {
        int r = this.random.nextInt(100);
        String at = this.owner.name + " step " + this.step++;
        if (r < 34) this.put(this.key(), this.value());
        else if (r < 44) this.removeKey();
        else if (r < 45) this.freeze();
        else if (r < 47) this.burst();
        else if (r < 72) this.containsAll(r < 60, at);
        else this.removeAll(at);
        this.check(at);
    }

    private int key() {
        int r = this.random.nextInt(20);
        if (r < 12) {
            return this.random.nextInt(-4, 41);
        }
        return r < 18 ? this.random.nextInt(0, 240)
                : ValuesLaneOwners.EXTREME + this.random.nextInt(8);
    }

    private int value() {
        return this.random.nextInt(10) == 0 ? this.random.nextInt()
                : VALUES[this.random.nextInt(VALUES.length)];
    }

    private void put(int code, int value) {
        long packed = ValuesLaneOwners.pack(code, value);
        this.owner.put.accept(this.p, packed);
        this.owner.put.accept(this.t, packed);
    }

    private void removeKey() {
        int code = this.key();
        this.owner.removeKey.accept(this.p, code);
        this.owner.removeKey.accept(this.t, code);
    }

    private void freeze() {
        this.frozenP.add(this.owner.freeze.apply(this.p));
        this.frozenT.add(this.owner.freeze.apply(this.t));
        if (this.frozenP.size() > KEPT_FROZEN) {
            this.frozenP.remove(0);
            this.frozenT.remove(0);
        }
    }

    private void burst() {
        for (int i = 0; i < 24; i++) {
            this.put(this.key(), this.value());
        }
    }

    private int[] elements() {
        boolean wide = this.random.nextInt(8) == 0;
        int[] elements = new int[this.random.nextInt(wide ? 65 : 9)];
        int[] live = this.p.toArray();
        Arrays.setAll(elements, i -> this.element(live));
        return elements;
    }

    private int element(int[] live) {
        int r = this.random.nextInt(10);
        if (live.length > 0 && r < 2) {
            return live[this.random.nextInt(Math.min(2, live.length))];
        }
        if (live.length > 0 && r < 6) {
            return live[this.random.nextInt(live.length)];
        }
        return r < 9 ? VALUES[this.random.nextInt(VALUES.length)] : this.random.nextInt();
    }

    private IntIterable patchedSource(int kind, int[] elements) {
        IntIterable source = source(kind, elements, this.p);
        if (kind < LANE_KINDS && source.size() >= MIN_LANE_SOURCE) {
            this.laneOps++;
        }
        return source;
    }

    private void containsAll(boolean viaMap, String at) {
        int kind = KINDS[this.random.nextInt(KINDS.length)];
        int[] e = this.elements();
        IntIterable source = this.patchedSource(kind, e);
        String patched = outcome(() -> viaMap ? this.p.containsAll(source)
                : this.p.values().containsAll(source));
        String reference = outcome(() ->
                ValuesLaneRef.containsAll(source(kind, e.clone(), this.t), this.t));
        assertEquals(reference, patched,
                () -> at + " containsAll kind " + kind + " " + Arrays.toString(e));
    }

    private void removeAll(String at) {
        int kind = KINDS[this.random.nextInt(KINDS.length)];
        int[] e = this.elements();
        IntIterable source = this.patchedSource(kind, e);
        String patched = outcome(() -> this.p.values().removeAll(source));
        MutableIntCollection view = this.t.values();
        String reference = outcome(() ->
                ValuesLaneRef.removeAll(source(kind, e.clone(), this.t), this.t, view));
        assertEquals(reference, patched,
                () -> at + " removeAll kind " + kind + " " + Arrays.toString(e));
    }

    private void check(String at) {
        ValuesLaneState.assertSameLanes(this.p, this.t, at);
        assertArrayEquals(this.t.toArray(), this.p.toArray(), at + " toArray");
        boolean deep = this.step % 16 == 0;
        ValuesLaneState.assertSameAliasing(this.p, this.t, this.frozenP, this.frozenT, deep, at);
        if (deep) {
            ValuesLaneState.assertSameOrder(this.p, this.t, this.owner, at);
        }
    }

    /** Helpful NPE text names the null variable only with -g; blank that name so builds compare. */
    private static String helpfulNpeFree(String message) {
        return message == null ? null : message.replaceAll("because \"[^\"]*\"", "because \"?\"");
    }
}
