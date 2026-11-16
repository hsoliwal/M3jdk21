/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections.sketch;

import java.lang.reflect.Field;

/** Candidate-only cache transition witness; never a public API requirement. */
final class HllEstimateCacheStructural {
    private static final int PRECISION = 6;
    private static final int MERGE_ATTEMPTS = 10_000;

    private HllEstimateCacheStructural() { }

    /** The two private fields of the estimate cache. */
    private record Cache(Field known, Field estimate) {
        boolean known(LongHyperLogLog hll) {
            try { return known.getBoolean(hll); }
            catch (IllegalAccessException e) { throw new AssertionError(e); }
        }

        long estimate(LongHyperLogLog hll) {
            try { return estimate.getLong(hll); }
            catch (IllegalAccessException e) { throw new AssertionError(e); }
        }
    }

    private static Field field(String name) {
        try {
            Field f = LongHyperLogLog.class.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static int check(boolean holds, String message) {
        if (!holds) throw new AssertionError(message);
        return 1;
    }

    private static int emptyAndFirstAdd(Cache cache, LongHyperLogLog hll) {
        int checks = check(cache.known(hll), "empty source must be cached");
        checks += check(cache.estimate(hll) == 0, "empty cache != 0");
        checks += check(hll.estimate() == 0, "initial estimate");
        checks += check(hll.add(0L), "initial add should change a register");
        return checks + check(!cache.known(hll), "add did not invalidate");
    }

    private static int cachedAndUnchanged(Cache cache, LongHyperLogLog hll) {
        long answer = hll.estimate();
        boolean kept = cache.known(hll) && cache.estimate(hll) == answer;
        int checks = check(kept, "estimate not cached");
        checks += check(!hll.add(0L), "duplicate add changed");
        checks += check(cache.known(hll), "unchanged add invalidated");
        hll.merge(hll);
        return checks + check(cache.known(hll), "self merge invalidated");
    }

    private static int merges(Cache cache, LongHyperLogLog hll) {
        LongHyperLogLog other = new LongHyperLogLog(PRECISION);
        other.add(0L);
        hll.merge(other);
        int checks = check(cache.known(hll), "identical merge invalidated");
        int attempts = 0;
        for (int i = 1; i < MERGE_ATTEMPTS && cache.known(hll); i++) {
            other.add(i);
            hll.merge(other);
            attempts++;
        }
        boolean invalidated = attempts != 0 && !cache.known(hll);
        return checks + check(invalidated, "changed merge did not invalidate");
    }

    private static int afterChange(Cache cache, LongHyperLogLog hll) {
        hll.estimate();
        int checks = check(cache.known(hll), "post-merge cache missing");
        hll.clear();
        return checks + check(cache.known(hll) && cache.estimate(hll) == 0L && hll.estimate() == 0L,
                "clear cache wrong");
    }

    static int run() {
        Cache cache = new Cache(field("estimateKnown"), field("cachedEstimate"));
        LongHyperLogLog hll = new LongHyperLogLog(PRECISION);
        int checks = emptyAndFirstAdd(cache, hll);
        checks += cachedAndUnchanged(cache, hll);
        checks += merges(cache, hll);
        return checks + afterChange(cache, hll);
    }
}
