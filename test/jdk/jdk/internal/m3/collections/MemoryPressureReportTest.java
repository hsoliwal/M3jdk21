// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.util.HashMap;
import java.util.Map;

/** Direct JVM proof: Xmx/current heap are observed, not passed by the programmer. */
//FLLDCIM: RELOCATE - donor blob f8abd3252b7b at 165ceb5a0202; recipe make/m3/collections/recipe/residency-paged-array-20261107/relocate.py v4
public final class MemoryPressureReportTest {
    private MemoryPressureReportTest() { }

    public static void main(String[] args) {
        var data = new HashMap<String, String>();
        for (String field : MemoryPressure.describe().split(" ")) {
            String[] pair = field.split("=", -1);
            if (pair.length != 2 || data.putIfAbsent(pair[0], pair[1]) != null)
                throw new AssertionError("invalid or duplicate pressure field: " + field);
        }
        var heap = MemoryPressure.current();
        if (Long.parseLong(data.get("xmxBytes")) != Runtime.getRuntime().maxMemory()
                || heap.maxHeap() != Runtime.getRuntime().maxMemory())
            throw new AssertionError("Xmx differs from actual JVM");
        long used = Long.parseLong(data.get("heapUsedBytes"));
        long committed = Long.parseLong(data.get("heapCommittedBytes"));
        long headroom = Long.parseLong(data.get("heapHeadroomBytes"));
        if (used < 0 || used > committed || committed > Runtime.getRuntime().maxMemory()
                || headroom != Math.max(0L, Runtime.getRuntime().maxMemory() - used))
            throw new AssertionError("heap arithmetic");
        if (!Map.of("true", true, "false", false).containsKey(data.get("nativeSamplerLoaded")))
            throw new AssertionError("native status missing");
        long physical = Long.parseLong(data.get("physicalAvailableBytes"));
        long total = Long.parseLong(data.get("physicalTotalBytes"));
        if (physical < -1 || total < -1 || (physical >= 0 && total >= 0 && physical > total))
            throw new AssertionError("physical sampling bounds");
        if (Boolean.getBoolean("m3.residency.native.required") &&
                (!MemoryPressure.nativeAvailable() || physical < 0 || total <= 0))
            throw new AssertionError("required native sampler did not return usable data");
        System.out.println("PASS " + MemoryPressure.describe());
    }
}
