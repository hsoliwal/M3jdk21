// Copyright 2026 Hitesh Soliwal and contributors.
// SPDX-License-Identifier: Apache-2.0
import com.synexia.mindex.ast.MIndexASTLimits;
import com.synexia.mindex.ast.MIndexASTPool;
import com.synexia.mindex.ast.partial.MIndexPartialAstCache;
import com.synexia.mindex.ast.partial.MIndexPartialAstKey;
import com.synexia.mindex.ast.partial.MIndexPartialAstMode;
import com.synexia.mindex.ast.partial.MIndexPartialAstProjection;
import com.synexia.mindex.ast.partial.MIndexPartialAstScope;
import java.util.List;
import java.util.Map;

/** One tiny immutable projection exposes a long arithmetic error without allocating a large cache. */
public final class PartialCacheLongOverflowProbe {
    private PartialCacheLongOverflowProbe() { }
    public static void main(String[] args) {
        var limits = MIndexASTLimits.defaults();
        var pool = new MIndexASTPool(limits);
        var root = pool.intern("PROBE", Map.of(), List.of());
        var key = new MIndexPartialAstKey("0".repeat(64), "1".repeat(64), "", 0,
                0, 2, 0, 1, MIndexPartialAstScope.FILE, MIndexPartialAstMode.SYNTAX, limits);
        var projection = new MIndexPartialAstProjection(key, pool.snapshot(), root.contentIndex(), 1L, 0);
        var cache = new MIndexPartialAstCache(1, Long.MAX_VALUE, 50);
        boolean admitted = cache.put(projection);
        var statistics = cache.stats();
        System.out.println("maxEntries=1 maxWeight=" + Long.MAX_VALUE + " projectionWeight=1 admitted="
                + admitted + " entries=" + statistics.entries() + " retainedWeight=" + statistics.retainedWeight());
        if (!admitted || statistics.entries() != 1 || statistics.retainedWeight() != 1L || cache.get(key) != projection) {
            throw new AssertionError("M3_PARTIAL_CACHE_LONG_BUDGET_COUNTEREXAMPLE: a valid positive budget rejected one tiny projection");
        }
        System.out.println("M3_PARTIAL_CACHE_LONG_BUDGET_PASS");
    }
}
