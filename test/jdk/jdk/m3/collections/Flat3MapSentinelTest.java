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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Flat3Map's hashed regime marks never-used and removed key slots with EMPTY/REMOVED sentinels
 * instead of a packed two-bit state lane: against the verbatim pre-change copy Flat3MapReference,
 * driven by the same puts, removes, lookups, slot-cursor walks with removeSlot/setSlotValue,
 * copies and clears over colliding keys, null keys and null values, equal results, exceptions,
 * slot order and the same sequence of user hashCode/equals calls (sentinels never reach them).
 */
//FLLDCIM: LEETCODIFY - differential proof for the Flat3Map EMPTY/REMOVED sentinels
class Flat3MapSentinelTest {
    /** A key with a chosen hash that logs every hashCode and equals call into its side's log. */
    static final class Key {
        final int id;
        final int hash;
        final List<String> log;

        Key(int id, int hash, List<String> log) {
            this.id = id;
            this.hash = hash;
            this.log = log;
        }

        @Override
        public int hashCode() {
            log.add("h" + id);
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            log.add("e" + id + ":" + (other instanceof Key k ? "k" + k.id : String.valueOf(other)));
            return other instanceof Key k && k.id == id;
        }

        @Override
        public String toString() {
            return "k" + id;
        }
    }

    private static String outcome(Supplier<Object> action) {
        try {
            return "=" + action.get();
        } catch (RuntimeException e) {
            return "!" + e.getClass().getName() + ": " + e.getMessage();
        }
    }

    private static final class Twin {
        Flat3Map<Object, Object> impl = new Flat3Map<>();
        Flat3MapReference<Object, Object> ref = new Flat3MapReference<>();
        final List<String> implLog = new ArrayList<>();
        final List<String> refLog = new ArrayList<>();

        Object key(int id, boolean forImpl) {
            return id < 0 ? null : new Key(id, Math.floorMod(id * 7, 5) * 8 + (id & 1),
                    forImpl ? implLog : refLog);
        }

        void step(String what, Supplier<Object> onRef, Supplier<Object> onImpl) {
            assertEquals(outcome(onRef), outcome(onImpl), what);
            assertEquals(refLog, implLog, what + " callbacks");
            assertEquals(walk(ref), walk(impl), what + " slots");
        }
    }

    private static String walk(Object map) {
        StringBuilder text = new StringBuilder();
        if (map instanceof Flat3Map<?, ?> m) {
            for (int s = m.nextSlot(0); s >= 0; s = m.nextSlot(s + 1)) {
                text.append(s).append(':').append(m.slotKey(s)).append('=').append(m.slotValue(s))
                        .append(' ');
            }
        } else {
            Flat3MapReference<?, ?> m = (Flat3MapReference<?, ?>) map;
            for (int s = m.nextSlot(0); s >= 0; s = m.nextSlot(s + 1)) {
                text.append(s).append(':').append(m.slotKey(s)).append('=').append(m.slotValue(s))
                        .append(' ');
            }
        }
        return text.toString();
    }

    private static void operate(Twin t, Random random) {
        int id = random.nextInt(40) - 2;
        Object value = random.nextInt(6) == 0 ? null : "v" + random.nextInt(4);
        int slot = random.nextInt(70) - 2;
        switch (random.nextInt(14)) {
            case 0, 1, 2, 3 -> t.step("put", () -> t.ref.put(t.key(id, false), value),
                    () -> t.impl.put(t.key(id, true), value));
            case 4, 5 -> t.step("remove", () -> t.ref.remove(t.key(id, false)),
                    () -> t.impl.remove(t.key(id, true)));
            case 6 -> t.step("get", () -> t.ref.get(t.key(id, false)), () -> t.impl.get(t.key(id,
                    true)));
            case 7 -> t.step("containsKey", () -> t.ref.containsKey(t.key(id, false)),
                    () -> t.impl.containsKey(t.key(id, true)));
            case 8 -> t.step("containsValue", () -> t.ref.containsValue(value),
                    () -> t.impl.containsValue(value));
            case 9 -> t.step("slotKey", () -> t.ref.slotKey(slot), () -> t.impl.slotKey(slot));
            case 10 -> t.step("setSlotValue", () -> t.ref.setSlotValue(slot, value),
                    () -> t.impl.setSlotValue(slot, value));
            case 11 -> t.step("removeSlot", () -> t.ref.removeSlot(slot), () -> t.impl.removeSlot(
                    slot));
            case 12 -> {
                if (random.nextInt(5) == 0) {
                    t.ref = t.ref.copy();
                    t.impl = t.impl.copy();
                    t.step("copy", t.ref::size, t.impl::size);
                }
            }
            default -> {
                if (random.nextInt(30) == 0) {
                    t.step("clear", () -> { t.ref.clear(); return null; },
                            () -> { t.impl.clear(); return null; });
                } else {
                    t.step("size", t.ref::size, t.impl::size);
                }
            }
        }
    }

    @Test
    @Timeout(value = 300, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void randomOperationsMatchMaster() {
        for (int seed = 0; seed < 300; seed++) {
            Random random = new Random(0xF1A73L * 1_000_003L + seed);
            Twin t = new Twin();
            for (int step = 0; step < 600; step++) {
                operate(t, random);
            }
        }
    }

    /** A cursor walk that removes every other live slot, with tombstones reused by later puts. */
    @Test
    @Timeout(value = 300, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void cursorRemovalReusesTombstones() {
        Random random = new Random(0xF1A74L);
        for (int trial = 0; trial < 200; trial++) {
            Twin t = new Twin();
            for (int i = 0; i < 4 + random.nextInt(40); i++) {
                int id = i;
                t.step("fill", () -> t.ref.put(t.key(id, false), id), () -> t.impl.put(t.key(id,
                        true), id));
            }
            int a = t.ref.nextSlot(0);
            int b = t.impl.nextSlot(0);
            for (int k = 0; a >= 0 && b >= 0; k++) {
                assertEquals(a, b, "cursor");
                int ra = k % 2 == 0 ? t.ref.removeSlot(a) : a + 1;
                int rb = k % 2 == 0 ? t.impl.removeSlot(b) : b + 1;
                a = t.ref.nextSlot(ra);
                b = t.impl.nextSlot(rb);
            }
            for (int i = 100; i < 100 + random.nextInt(30); i++) {
                int id = i;
                t.step("refill", () -> t.ref.put(t.key(id, false), null),
                        () -> t.impl.put(t.key(id, true), null));
            }
            t.step("null key", () -> t.ref.put(null, "n"), () -> t.impl.put(null, "n"));
            t.step("contains null value", () -> t.ref.containsValue(null), () ->
                    t.impl.containsValue(null));
        }
        assertTrue(true);
    }
}
