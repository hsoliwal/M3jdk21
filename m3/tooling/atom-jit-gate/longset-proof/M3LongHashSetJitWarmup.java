// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Test-only. This is not a new collection implementation or product class.
package jdk.m3.collections;

/** Execute the exact, source-sealed candidate long-set owner in the same JVM as its oracle. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/b65df53330e3d82f6b2d5509ded971f19883f4ca/synexia-openrewrite-recipes/crates/m3jdk21-atomize-jit-patternize-20261031/M3LongHashSetJitWarmup.java blob 216414c27c28; recipe m3/tooling/atom-jit-gate/recipe/atom-jit-gate-20261106/relocate.py v4
public final class M3LongHashSetJitWarmup {
    private static final int ROUNDS = 260;
    private static final int KEYS_PER_ROUND = 1_024;
    private static final int KEY_SHIFT = 17;

    private M3LongHashSetJitWarmup() { }

    public static void main(String[] args) {
        M3LongHashSetSingleProbeTest.main(args);
        long witnessed = 0L;
        for (int round = 0; round < ROUNDS; round++) {
            M3LongHashSet set = new M3LongHashSet();
            long prefix = (long) round << KEY_SHIFT;
            for (int index = 0; index < KEYS_PER_ROUND; index++) {
                long key = prefix | index;
                if (!set.add(key) || set.add(key)) {
                    throw new AssertionError("duplicate/insertion contract: " + round + ":" + index);
                }
            }
            for (int index = 0; index < KEYS_PER_ROUND; index++) {
                long key = prefix | index;
                if (!set.contains(key)) {
                    throw new AssertionError("missing member: " + round + ":" + index);
                }
                witnessed++;
            }
            if (set.size() != KEYS_PER_ROUND) {
                throw new AssertionError("wrong cardinality: " + set.size());
            }
        }
        if (witnessed != (long) ROUNDS * KEYS_PER_ROUND) {
            throw new AssertionError("warmup checksum mismatch");
        }
        System.out.println("JIT_WARMUP_PASS entries=" + witnessed);
    }
}
