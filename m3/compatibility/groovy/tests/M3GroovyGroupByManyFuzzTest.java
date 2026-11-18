// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import jdk.m3.collections.ObjectStream;

/** Deterministic randomized cross-check against a simple allocation-heavy reference. */
public final class M3GroovyGroupByManyFuzzTest {
    private M3GroovyGroupByManyFuzzTest() { }

    public static void main(String[] args) {
        Random random = new Random(0x47726f6f76793136L);
        for (int trial = 0; trial < 2500; trial++) {
            ArrayList<Integer> values = new ArrayList<>();
            int n = random.nextInt(140);
            for (int j = 0; j < n; j++) {
                int x = random.nextInt(15);
                values.add(x == 0 ? null : x - 7);
            }
            Function<Integer, Iterable<String>> keys = x -> {
                if (x == null) return null;
                String a = "r" + Math.floorMod(x, 4);
                if (Math.floorMod(x, 7) == 0) return Arrays.asList(a, a);
                return Math.floorMod(x, 3) == 0 ? Arrays.asList(a, null) : List.of(a);
            };
            Function<Integer, Integer> mapper = x -> x == null ? null : x * 2;
            Map<String, List<Integer>> expected = new LinkedHashMap<>();
            long memberships = 0L;
            for (Integer value : values) {
                Iterable<String> assigned = keys.apply(value);
                if (assigned == null) continue;
                for (String key : assigned) {
                    expected.computeIfAbsent(key, ignored -> new ArrayList<>()).add(mapper.apply(value));
                    memberships++;
                }
            }
            Map<String, List<Integer>> actual = ObjectStream.snapshot(values)
                    .groupByMany(mapper, keys, n, 5, memberships);
            if (!expected.equals(actual) || !expected.keySet().toString().equals(actual.keySet().toString())) {
                throw new AssertionError("random trial=" + trial + " source=" + values
                        + " expected=" + expected + " actual=" + actual);
            }
        }
        System.out.println("M3_GROOVY_GROUP_BY_MANY_FUZZ_PASS trials=2500 seed=0x47726f6f76793136");
    }
}
