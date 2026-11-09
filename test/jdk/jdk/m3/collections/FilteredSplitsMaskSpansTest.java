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
package jdk.m3.collections;

import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.LongIndexPathMasksJNI;
import jdk.internal.m3.collections.LongRankBatchNative;
import jdk.internal.m3.collections.ResidencyStateMaskIndex;
import jdk.internal.m3.collections.sketch.StateMaskHistogram;

final class FilteredSplitsMaskSpansTest {
    @Test void filteredRecursiveSplits() { FilteredSplitContract.main(new String[0]); }
    @Test void maskSpanJavaOracle() { MaskSpansContract.main(new String[0]); }
    @Test void maskSpanCheckedJniOracle() {
        org.junit.jupiter.api.Assumptions.assumeTrue(Boolean.getBoolean("m3.mask.spans.native.required"),
                "native proof requires the explicit checked-JNI runner");
        MaskSpansContract.main(new String[] {"native"});
    }
    @Test void allNativeBackendsAreRequiredByTheFullLane() {
        org.junit.jupiter.api.Assumptions.assumeTrue(Boolean.getBoolean("m3.collections.all.native.required"),
                "all-native qualification is an explicit, isolated lane");
        org.junit.jupiter.api.Assertions.assertAll(
                () -> org.junit.jupiter.api.Assertions.assertTrue(IndexMaskCompactor.nativeAvailable(), "mask"),
                () -> org.junit.jupiter.api.Assertions.assertTrue(IndexPathMasks.nativeAvailable(), "pathmask"),
                () -> org.junit.jupiter.api.Assertions.assertTrue(LongIndexPathMasksJNI.nativeAvailable(), "long-path-masks"),
                () -> org.junit.jupiter.api.Assertions.assertTrue(LongAddressKernel.nativeAvailable(), "address"),
                () -> org.junit.jupiter.api.Assertions.assertTrue(SpanIndex.nativeAvailable(), "span"),
                () -> org.junit.jupiter.api.Assertions.assertTrue(StateMaskHistogram.nativeAvailable(), "statehist"),
                () -> org.junit.jupiter.api.Assertions.assertTrue(LongRankBatchNative.nativeAvailable(), "rank"),
                () -> org.junit.jupiter.api.Assertions.assertTrue(SignalOrderNative.available(), "signal"),
                () -> org.junit.jupiter.api.Assertions.assertTrue(ResidencyStateMaskIndex.nativeAvailable(), "residency"));
        System.loadLibrary("m3bitlane");
        BitLane28 lane = new BitLane28();
        lane.set(0); lane.set(63); lane.set(4096);
        org.junit.jupiter.api.Assertions.assertEquals(lane.cardinality(0, 4097), lane.cardinalityNative(0, 4097));
    }
}
