<<<<<<< HEAD
/*******************************************************************************
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
    @Test void allNativeBackendsAreRequiredByTheFullLane() {
        org.junit.jupiter.api.Assumptions.assumeTrue(Boolean.getBoolean("m3.collections.all.native.required"),
                "all-native qualification is an explicit, isolated lane");
        org.junit.jupiter.api.Assertions.assertAll(
                () -> org.junit.jupiter.api.Assertions.assertTrue(M3IndexMaskCompactor.nativeAvailable(), "mask"),
                () -> org.junit.jupiter.api.Assertions.assertTrue(M3IndexPathMasks.nativeAvailable(), "pathmask"),
                () -> org.junit.jupiter.api.Assertions.assertTrue(M3LongIndexPathMasksJNI.nativeAvailable(), "long-path-masks"),
                () -> org.junit.jupiter.api.Assertions.assertTrue(M3LongAddressKernel.nativeAvailable(), "address"),
                () -> org.junit.jupiter.api.Assertions.assertTrue(M3SpanIndex.nativeAvailable(), "span"),
                () -> org.junit.jupiter.api.Assertions.assertTrue(M3StateMaskHistogram.nativeAvailable(), "statehist"),
                () -> org.junit.jupiter.api.Assertions.assertTrue(M3LongRankBatchNative.nativeAvailable(), "rank"),
                () -> org.junit.jupiter.api.Assertions.assertTrue(M3SignalOrderNative.available(), "signal"),
                () -> org.junit.jupiter.api.Assertions.assertTrue(M3ResidencyStateMaskIndex.nativeAvailable(), "residency"));
        System.loadLibrary("m3bitlane");
        M3BitLane28 lane = new M3BitLane28();
        lane.set(0); lane.set(63); lane.set(4096);
        org.junit.jupiter.api.Assertions.assertEquals(lane.cardinality(0, 4097), lane.cardinalityNative(0, 4097));
    }
}
||||||| merged common ancestors
=======
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
    @Test void allNativeBackendsAreRequiredByTheFullLane() {
        org.junit.jupiter.api.Assumptions.assumeTrue(Boolean.getBoolean("m3.collections.all.native.required"),
                "all-native qualification is an explicit, isolated lane");
        org.junit.jupiter.api.Assertions.assertAll(
                () -> org.junit.jupiter.api.Assertions.assertTrue(M3IndexMaskCompactor.nativeAvailable(), "mask"),
                () -> org.junit.jupiter.api.Assertions.assertTrue(M3IndexPathMasks.nativeAvailable(), "pathmask"),
                () -> org.junit.jupiter.api.Assertions.assertTrue(M3LongAddressKernel.nativeAvailable(), "address"),
                () -> org.junit.jupiter.api.Assertions.assertTrue(M3SpanIndex.nativeAvailable(), "span"),
                () -> org.junit.jupiter.api.Assertions.assertTrue(M3StateMaskHistogram.nativeAvailable(), "statehist"),
                () -> org.junit.jupiter.api.Assertions.assertTrue(M3LongRankBatchNative.nativeAvailable(), "rank"),
                () -> org.junit.jupiter.api.Assertions.assertTrue(M3SignalOrderNative.available(), "signal"),
                () -> org.junit.jupiter.api.Assertions.assertTrue(M3ResidencyStateMaskIndex.nativeAvailable(), "residency"));
        System.loadLibrary("m3bitlane");
        M3BitLane28 lane = new M3BitLane28();
        lane.set(0); lane.set(63); lane.set(4096);
        org.junit.jupiter.api.Assertions.assertEquals(lane.cardinality(0, 4097), lane.cardinalityNative(0, 4097));
    }
}
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
