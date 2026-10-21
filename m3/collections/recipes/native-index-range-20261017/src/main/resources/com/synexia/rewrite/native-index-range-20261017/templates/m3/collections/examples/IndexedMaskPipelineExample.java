/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
import com.m3.collections.M3IndexMaskCompactor;
import com.m3.collections.M3LongIndexProjection;
import java.util.Arrays;

/** No per-row wrappers: immutable payload + ordered physical IDs + logical offsets + flag bytes. */
public final class IndexedMaskPipelineExample {
    public static void main(String[] args) {
        var backend = args.length > 0 && args[0].equals("native")
                ? M3IndexMaskCompactor.Backend.NATIVE_REQUIRED : M3IndexMaskCompactor.Backend.JAVA;
        var ranked = M3LongIndexProjection.snapshot(new long[]{40,10,10,30,20}).sortedAscending();
        var page = ranked.slice(1,3); // values [10,20,30]; physical IDs [2,4,3]
        byte[] logicalState = {(byte)0x81, 1, (byte)0x80};
        var selected = page.filtered(logicalState, 0x80, backend).sortedAscending();
        if (!Arrays.equals(selected.physicalIndices(),new int[]{2,3})
                || !Arrays.equals(selected.toArray(),new long[]{10,30})) throw new AssertionError();
        System.out.println("physical="+Arrays.toString(selected.physicalIndices())
                +" values="+Arrays.toString(selected.toArray()));
    }
}
