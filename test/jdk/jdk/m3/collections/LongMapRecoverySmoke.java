/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** Independent model-only execution on exact Java21 candidate source. */
//FLLDCIM: RELOCATE - donor blob eb68a0be3fa5 at ca1aafc4d94f; recipe make/m3/collections/qualification/census-g6-20261106/relocate.py v4
public final class LongMapRecoverySmoke {
    private static long checked;
    private LongMapRecoverySmoke() { }

    private static void check(boolean condition, String reason) {
        checked++;
        if (!condition) throw new AssertionError(reason);
    }

    public static void main(String[] arguments) {
        for (int seed = 0; seed < 12; seed++) {
            Random random = new Random(0x5ee7fa71L + seed * 1907);
            LongLongHashMap map = new LongLongHashMap();
            Map<Long, Long> reference = new HashMap<>();
            for (int step = 0; step < 10000; step++) {
                long key = step % 17 == 0 ? Long.MIN_VALUE : step % 19 == 0
                        ? Long.MAX_VALUE : step % 23 == 0 ? 0 : random.nextInt(150) - 75;
                long amount = step % 59 == 0 ? Long.MAX_VALUE : random.nextInt(2001) - 1000;
                switch (random.nextInt(7)) {
                    case 0, 1, 2 -> {
                        long expected = reference.getOrDefault(key, 0L) + amount;
                        check(map.addTo(key, amount) == expected, "addTo");
                        reference.put(key, expected);
                    }
                    case 3 -> {
                        boolean inserted = !reference.containsKey(key);
                        check(map.put(key, amount) == inserted, "put");
                        reference.put(key, amount);
                    }
                    case 4 -> check(map.remove(key) == (reference.remove(key) != null), "remove");
                    case 5 -> check(map.containsKey(key) == reference.containsKey(key), "containsKey");
                    case 6 -> check(map.getOrDefault(key, Long.MIN_VALUE + 7)
                            == reference.getOrDefault(key, Long.MIN_VALUE + 7), "getOrDefault");
                    default -> throw new AssertionError("unreachable");
                }
                check(map.size() == reference.size(), "size");
            }
            check(map.keysToArray().length == reference.size(), "key array length");
        }
        System.out.printf("M3_LONGMAP_HISTORY_RECOVERY_SMOKE_PASS checks=%d%n", checked);
    }
}
