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
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * PrimitiveSortedGapSet and PrimitiveSortedGapMap clear only the slice a gap move vacated: against
 * master's classes (GapSetReference, GapMapReference), equal results and byte-identical backing
 * arrays, gap included, after every operation, for every primitive kind.
 */
//FLLDCIM: LEETCODIFY — differential proof for the gap-buffer slice clear
class PrimitiveSortedGapClearTest {

    private static final float[] LOADS = {0.5f, 0.75f, 0.95f};

    private static Object field(Object owner, String name) {
        try {
            Field f = owner.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(owner);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static void sameLanes(Object ref, Object lanes, String at, String... names) {
        for (String name : names) {
            assertTrue(Objects.deepEquals(field(ref, name), field(lanes, name)), at + " " + name);
        }
    }

    @Test
    void setsAgainstMaster() {
        for (PrimitiveKind kind : PrimitiveKind.values()) {
            for (int seed = 0; seed < 30; seed++) {
                Random random = new Random(seed * 31L + kind.ordinal());
                float load = LOADS[seed % LOADS.length];
                PrimitiveSortedGapSet set = new PrimitiveSortedGapSet(kind, seed % 7, load);
                GapSetReference ref = new GapSetReference(kind, seed % 7, load);
                for (int step = 0; step < 1_200; step++) {
                    long bits = random.nextInt(300) - 150;
                    String at = kind + " seed " + seed + " step " + step;
                    switch (random.nextInt(6)) {
                        case 0, 1, 2 -> assertEquals(ref.addBits(bits), set.addBits(bits), at);
                        case 3, 4 -> assertEquals(ref.removeBits(bits), set.removeBits(bits), at);
                        default -> {
                            if (random.nextInt(80) == 0) {
                                ref.clear();
                                set.clear();
                            }
                        }
                    }
                    sameLanes(ref, set, at, "elements", "gapStart", "gapEnd", "size");
                }
            }
        }
    }

    @Test
    void mapsAgainstMaster() {
        PrimitiveKind[] kinds = PrimitiveKind.values();
        for (int seed = 0; seed < 160; seed++) {
            Random random = new Random(seed);
            PrimitiveKind keyKind = kinds[seed % kinds.length];
            PrimitiveKind valueKind = kinds[(seed / kinds.length) % kinds.length];
            float load = LOADS[seed % LOADS.length];
            PrimitiveSortedGapMap map =
                    new PrimitiveSortedGapMap(keyKind, valueKind, seed % 5, load);
            GapMapReference ref = new GapMapReference(keyKind, valueKind, seed % 5, load);
            for (int step = 0; step < 1_200; step++) {
                long key = random.nextInt(300) - 150;
                long value = random.nextLong();
                String at = keyKind + "/" + valueKind + " seed " + seed + " step " + step;
                switch (random.nextInt(6)) {
                    case 0, 1, 2 -> assertEquals(ref.putBits(key, value, -7),
                            map.putBits(key, value, -7), at);
                    case 3, 4 -> assertEquals(ref.removeOrDefaultBits(key, -9),
                            map.removeOrDefaultBits(key, -9), at);
                    default -> assertEquals(ref.getOrDefaultBits(key, -3),
                            map.getOrDefaultBits(key, -3), at);
                }
                sameLanes(ref, map, at, "keys", "values", "gapStart", "gapEnd", "size");
            }
        }
    }
}
