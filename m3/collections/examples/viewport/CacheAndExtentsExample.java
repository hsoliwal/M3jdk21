/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
import com.m3.collections.M3BoundedCache;
import com.m3.collections.M3SparseExtents;

/** Variable row heights over an estimate (Fenwick offsets) and a CLOCK cache of row renderings. */
public final class CacheAndExtentsExample {
    public static void main(String[] args) {
        M3SparseExtents extents = new M3SparseExtents(20);       // unmeasured rows count as 20 px
        extents.reset(10_000_000);
        extents.setExtent(5, 60);                                // two measured rows
        extents.setExtent(1_000, 5);
        long y = extents.offsetOf(2_000);
        int row = extents.rowAt(y + 1);
        System.out.println("total=" + extents.totalExtent() + " y(2000)=" + y + " rowAt=" + row);
        if (y != 2_000L * 20 + 40 - 15 || row != 2_000) throw new AssertionError("extents example");
        M3BoundedCache<Integer, String> rendered = new M3BoundedCache<>(256);
        int misses = 0;
        for (int pass = 0; pass < 3; pass++) for (int r = 0; r < 200; r++) if (rendered.get(r) == null) {
            misses++;
            rendered.put(r, "rendered row " + r);
        }
        System.out.println("cache size=" + rendered.size() + " misses=" + misses + " hits=" + rendered.hits());
        if (misses != 200) throw new AssertionError("cache example");
    }
}
