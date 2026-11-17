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
package jdk.m3.collections.apache.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/**
 * PassiveExpiringMap skips its expiry sweep while the clock is below a watermark of the earliest
 * expiration time: against the verbatim pre-change copy RefPassiveExpiringMap, on a phased clock
 * (expiration times sit on phase boundaries, operations run inside phases), equal results, the same
 * decorated-map remove order, equal contents after faults in the decorated map, re-entrant puts
 * during a sweep and serialization round trips; and an entry is never visible from the
 * millisecond it expires.
 */
//FLLDCIM: LEETCODIFY - phased-clock proof for the PassiveExpiringMap sweep watermark
class PassiveExpiringWatermarkTest {
    private static final long PHASE = 400;

    /** Expiration times on phase boundaries, never-expire (-1) or already past. */
    static final class Script implements PassiveExpiringMap.ExpirationPolicy<Integer, String>,
            RefPassiveExpiringMap.ExpirationPolicy<Integer, String>, Serializable {
        private static final long serialVersionUID = 1L;
        final long base;

        Script(long base) {
            this.base = base;
        }

        @Override
        public long expirationTime(Integer key, String value) {
            int pick = Math.floorMod(key * 31 + value.hashCode(), 11);
            return pick == 0 ? -1L : pick == 1 ? base - 1_000_000L : base + pick * PHASE;
        }
    }

    /** A decorated map that logs removes, can fail once, and can re-enter the outer map. */
    static final class Decorated extends LinkedHashMap<Integer, String> {
        private static final long serialVersionUID = 1L;
        transient List<Object> log = new ArrayList<>();
        transient boolean failRemove;
        transient boolean failClear;
        transient Map<Integer, String> reenter;

        @Override
        public String remove(Object key) {
            if (log != null) {
                log.add(key);
            }
            if (failRemove) {
                failRemove = false;
                throw new IllegalStateException("remove fault " + key);
            }
            if (reenter != null) {
                Map<Integer, String> outer = reenter;
                reenter = null;
                outer.put(1000 + (Integer) key, "r");
            }
            return super.remove(key);
        }

        @Override
        public void clear() {
            if (failClear) {
                failClear = false;
                throw new IllegalStateException("clear fault");
            }
            super.clear();
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
        Decorated implMap = new Decorated();
        Decorated refMap = new Decorated();
        PassiveExpiringMap<Integer, String> impl;
        RefPassiveExpiringMap<Integer, String> ref;

        Twin(Script script) {
            impl = new PassiveExpiringMap<>((PassiveExpiringMap.ExpirationPolicy<Integer, String>)
                    script,
                    implMap);
            ref = new RefPassiveExpiringMap<>(
                    (RefPassiveExpiringMap.ExpirationPolicy<Integer, String>) script, refMap);
        }

        void step(String what, Supplier<Object> onRef, Supplier<Object> onImpl) {
            assertEquals(outcome(onRef), outcome(onImpl), what);
            assertEquals(refMap.log, implMap.log, what + " remove order");
            assertEquals(new TreeMap<>(refMap), new TreeMap<>(implMap), what + " contents");
        }

        @SuppressWarnings("unchecked")
        void roundTrip() {
            impl = (PassiveExpiringMap<Integer, String>) copy(impl);
            ref = (RefPassiveExpiringMap<Integer, String>) copy(ref);
            implMap = decorated(impl);
            refMap = decorated(ref);
        }
    }

    private static Object copy(Object value) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
                out.writeObject(value);
            }
            try (ObjectInputStream in = new ObjectInputStream(
                    new ByteArrayInputStream(bytes.toByteArray()))) {
                return in.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    private static Decorated decorated(Object map) {
        try {
            java.lang.reflect.Field field = AbstractMapDecorator.class.getDeclaredField("map");
            field.setAccessible(true);
            Decorated d = (Decorated) field.get(map);
            d.log = new ArrayList<>();
            return d;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static void operate(Twin t, Random random) {
        int key = random.nextInt(24);
        String value = "v" + random.nextInt(5);
        switch (random.nextInt(12)) {
            case 0, 1, 2 -> t.step("put", () -> t.ref.put(key, value), () -> t.impl.put(key, value))
                    ;
            case 3 -> t.step("get", () -> t.ref.get(key), () -> t.impl.get(key));
            case 4 -> t.step("size", t.ref::size, t.impl::size);
            case 5 -> t.step("containsKey", () -> t.ref.containsKey(key), () -> t.impl.containsKey(
                    key));
            case 6 -> t.step("containsValue", () -> t.ref.containsValue(value),
                    () -> t.impl.containsValue(value));
            case 7 -> t.step("keySet", () -> new TreeMap<>(t.ref).keySet().toString(),
                    () -> new TreeMap<>(t.impl).keySet().toString());
            case 8 -> t.step("remove", () -> t.ref.remove(key), () -> t.impl.remove(key));
            case 9 -> {
                t.refMap.failRemove = t.implMap.failRemove = random.nextInt(4) == 0;
                t.refMap.reenter = random.nextInt(4) == 0 ? t.ref : null;
                t.implMap.reenter = t.refMap.reenter == null ? null : t.impl;
                t.step("isEmpty", t.ref::isEmpty, t.impl::isEmpty);
                t.refMap.failRemove = t.implMap.failRemove = false;
                t.refMap.reenter = t.implMap.reenter = null;
            }
            case 10 -> {
                boolean fail = random.nextInt(3) == 0;
                t.refMap.failClear = t.implMap.failClear = fail;
                if (random.nextInt(4) == 0) {
                    t.step("clear", () -> { t.ref.clear(); return null; },
                            () -> { t.impl.clear(); return null; });
                }
                t.refMap.failClear = t.implMap.failClear = false;
            }
            default -> t.step("values", () -> new TreeMap<>(t.ref).values().toString(),
                    () -> new TreeMap<>(t.impl).values().toString());
        }
    }

    private static void sleepUntil(long at) throws InterruptedException {
        long wait = at - System.currentTimeMillis();
        if (wait > 0) {
            Thread.sleep(wait);
        }
    }

    /** Phase k runs in [base + kP + 80, base + (k+1)P - 120]; no expiration time falls inside. */
    @Test
    void phasedClockMatchesOldMap() throws InterruptedException {
        Random random = new Random(0x9A55L);
        long base = System.currentTimeMillis() + 300;
        Twin t = new Twin(new Script(base));
        int phasesRun = 0;
        for (int phase = 0; phase < 14; phase++) {
            long start = base + phase * PHASE + 80;
            long end = base + (phase + 1) * PHASE - 120;
            sleepUntil(start);
            for (int op = 0; op < 40 && System.currentTimeMillis() < end; op++) {
                operate(t, random);
            }
            if (phase % 4 == 3 && System.currentTimeMillis() < end) {
                t.roundTrip();
            }
            phasesRun++;
        }
        assertEquals(14, phasesRun);
    }

    /** Fixed times per (key, value): 1/"b" at phase 5, 2/"a" at phase 1, 1/"r" at phase 3. */
    static final class Fixed implements PassiveExpiringMap.ExpirationPolicy<Integer, String>,
            RefPassiveExpiringMap.ExpirationPolicy<Integer, String>, Serializable {
        private static final long serialVersionUID = 1L;
        final long base;

        Fixed(long base) {
            this.base = base;
        }

        @Override
        public long expirationTime(Integer key, String value) {
            int phase = value.equals("b") ? 5 : value.equals("a") ? 1 : 3;
            return base + phase * PHASE;
        }
    }

    /** Removing key 2 during a sweep re-puts key 1 (no structural change) with an earlier time. */
    static final class Racing extends LinkedHashMap<Integer, String> {
        private static final long serialVersionUID = 1L;
        transient Map<Integer, String> outer;

        @Override
        public String remove(Object key) {
            if (outer != null && Integer.valueOf(2).equals(key)) {
                Map<Integer, String> target = outer;
                outer = null;
                target.put(1, "r");
            }
            return super.remove(key);
        }
    }

    /**
     * Key 1 is visited first (phase 5), key 2 expires and its removal re-puts key 1 at phase 3:
     * the sweep was raced by a write, so the watermark must not rise to phase 5.
     */
    @Test
    void racedSweepKeepsWatermark() throws InterruptedException {
        long base = System.currentTimeMillis() + 300;
        Fixed policy = new Fixed(base);
        Racing implMap = new Racing();
        Racing refMap = new Racing();
        PassiveExpiringMap<Integer, String> impl = new PassiveExpiringMap<>(
                (PassiveExpiringMap.ExpirationPolicy<Integer, String>) policy, implMap);
        RefPassiveExpiringMap<Integer, String> ref = new RefPassiveExpiringMap<>(
                (RefPassiveExpiringMap.ExpirationPolicy<Integer, String>) policy, refMap);
        sleepUntil(base + 80);
        for (Map<Integer, String> map : List.<Map<Integer, String>>of(impl, ref)) {
            map.put(1, "b");
            map.put(2, "a");
        }
        implMap.outer = impl;
        refMap.outer = ref;
        sleepUntil(base + 2 * PHASE + 80);
        assertEquals(ref.size(), impl.size(), "sweep raced by the re-put");
        sleepUntil(base + 4 * PHASE + 80);
        // whole-map reads go through the sweep (containsKey checks one key and skips the watermark)
        assertEquals(ref.containsValue("r"), impl.containsValue("r"), "re-put entry after expiry");
        assertEquals(ref.size(), impl.size(), "size after the re-put expiry");
        assertFalse(impl.containsValue("r"), "key 1 expired at phase 3");
    }

    /** An entry expiring at the current millisecond is gone at the next read in it. */
    @Test
    void entryGoneFromItsExpiryMillisecond() {
        PassiveExpiringMap<Integer, String> map = new PassiveExpiringMap<>(
                (PassiveExpiringMap.ExpirationPolicy<Integer, String>) (k, v) ->
                        System.currentTimeMillis());
        for (int i = 0; i < 20_000; i++) {
            map.put(i, "x");
            assertFalse(map.containsValue("x"), "entry visible from its expiry millisecond");
        }
        assertTrue(map.isEmpty());
    }
}
