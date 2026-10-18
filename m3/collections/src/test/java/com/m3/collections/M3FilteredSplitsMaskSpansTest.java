/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

final class M3FilteredSplitsMaskSpansTest {
    @Test void filteredRecursiveSplits() { M3FilteredSplitContract.main(new String[0]); }
    @Test void maskSpanJavaOracle() { M3MaskSpansContract.main(new String[0]); }
    @Test void maskSpanCheckedJniOracle() {
        org.junit.jupiter.api.Assumptions.assumeTrue(Boolean.getBoolean("m3.mask.spans.native.required"),
                "native proof requires the explicit checked-JNI runner");
        M3MaskSpansContract.main(new String[] {"native"});
    }
}
