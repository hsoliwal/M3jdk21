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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/**
 * LFUCache marks unused key slots with an EMPTY sentinel instead of a byte state lane: against the
 * verbatim pre-change copy LFUCacheReference, the same getOrCreate/removeIdentity/clear calls with
 * identity keys (null included), failing and null-returning factories and forced timestamp
 * rebases give equal results, exceptions, factory calls and private lanes (live slots, keys,
 * frequencies, stamps, clock, size, identity hint).
 */
//FLLDCIM: LEETCODIFY - differential proof for the LFUCache EMPTY key slots
class LFUCacheSentinelTest {
    private static final Object[] POOL = new Object[9];

    static {
        for (int i = 0; i < POOL.length; i++) {
            POOL[i] = new Object();
        }
    }

    private static Object read(Object owner, String name) {
        try {
            Field field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(owner);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static void write(Object owner, String name, long value) {
        try {
            Field field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.setLong(owner, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static String keyName(Object key) {
        for (int i = 0; i < POOL.length; i++) {
            if (POOL[i] == key) {
                return "k" + i;
            }
        }
        return key == null ? "null" : "other";
    }

    /** Live slots and their keys: the reference reads its state byte, the patched lane its
     * sentinel. */
    private static String lanes(Object cache, boolean patched) {
        Object[] keys = (Object[]) read(cache, "keys");
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < keys.length; i++) {
            boolean live = patched ? !"other".equals(keyName(keys[i])) || keys[i] == null
                    : ((byte[]) read(cache, "states"))[i] != 0;
            text.append(live ? keyName(keys[i]) : "-").append(' ');
        }
        return text + Arrays.toString((int[]) read(cache, "frequencies"))
                + Arrays.toString((long[]) read(cache, "accessStamps"))
                + read(cache, "size") + "/" + read(cache, "clock") + "/" + read(cache,
                        "lastIdentitySlot");
    }

    private static String outcome(Supplier<Object> action) {
        try {
            return "=" + action.get();
        } catch (RuntimeException e) {
            return "!" + e.getClass().getName() + ": " + e.getMessage();
        }
    }

    private static Function<Object, String> factory(int kind, int[] counter) {
        return key -> {
            counter[0]++;
            if (kind == 1) {
                throw new IllegalStateException("factory fault " + keyName(key));
            }
            return kind == 2 ? null : keyName(key) + "#" + counter[0];
        };
    }

    /** LFUCache allocates through ArrayNative; without the M3 array JNI library it cannot be built.
     * */
    private static boolean arraysNativeLoaded() {
        try {
            new LFUCache<Object, Object>(1);
            return true;
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    @Test
    void randomOperationsMatchMaster() {
        assumeTrue(arraysNativeLoaded(), "M3 array JNI library (m3.arrays.native.path) not loaded");
        for (int seed = 0; seed < 400; seed++) {
            Random random = new Random(0x1F0CL * 1_000_003L + seed);
            int capacity = random.nextInt(7);
            LFUCache<Object, String> impl = new LFUCache<>(capacity);
            LFUCacheReference<Object, String> ref = new LFUCacheReference<>(capacity);
            int[] implCalls = {0};
            int[] refCalls = {0};
            for (int step = 0; step < 500; step++) {
                Object key = random.nextInt(10) == 0 ? null : POOL[random.nextInt(POOL.length)];
                int op = random.nextInt(20);
                if (op < 12) {
                    int kind = random.nextInt(12) == 0 ? 1 + random.nextInt(2) : 0;
                    assertEquals(outcome(() -> ref.getOrCreate(key, factory(kind, refCalls))),
                            outcome(() -> impl.getOrCreate(key, factory(kind, implCalls))), "get");
                } else if (op < 16) {
                    ref.removeIdentity(key);
                    impl.removeIdentity(key);
                } else if (op < 18) {
                    long clock = random.nextBoolean() ? Long.MAX_VALUE : Long.MAX_VALUE - 1;
                    write(ref, "clock", clock);
                    write(impl, "clock", clock);
                } else if (op == 18 && random.nextInt(4) == 0) {
                    ref.clear();
                    impl.clear();
                }
                assertEquals(refCalls[0], implCalls[0], "factory calls");
                assertEquals(ref.size(), impl.size(), "size");
                assertEquals(lanes(ref, false), lanes(impl, true), "lanes at " + step);
            }
        }
    }
}
