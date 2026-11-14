// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.projection;

import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Behavioral fixtures independently derived from the donor equations. */
final class M3NcmOpsTest {
    private static final float[] BOOK = {0, 0, 2, 0, 0, 2};

    @Test void nearestUsesExpandedDistancesAndFirstIdTies() {
        assertArrayEquals(new int[]{0, 0, 1},
                M3NcmOps.nearest(new float[]{0, 0, 1, 1, 2, 0}, 2, BOOK));
    }

    @Test void expandedFp32DistanceCanCollapseNearBoundary() {
        // A squared-difference rewrite would choose ID 1 and violate this profile.
        assertArrayEquals(new int[]{0},
                M3NcmOps.nearest(new float[]{8192}, 1, new float[]{8193, 8192}));
    }

    @Test void areaResizeUsesOverlappingAdaptiveBins() {
        assertArrayEquals(new float[]{1, 4, 7},
                M3NcmOps.areaResize(new float[]{0, 2, 4, 6, 8}, 1, 5, 1, 3));
    }

    @Test void areaResizePreservesBatchAndChannelStrides() {
        assertArrayEquals(new float[]{1, 11, 4, 14, 7, 17, 21, 31, 24, 34, 27, 37},
                M3NcmOps.areaResize(new float[]{0,10, 2,12, 4,14, 6,16, 8,18,
                        20,30, 22,32, 24,34, 26,36, 28,38}, 2, 5, 2, 3));
    }

    @Test void linearResizeUsesHalfPixelCoordinates() {
        assertArrayEquals(new float[]{0, 1, 3, 4},
                M3NcmOps.linearResize(new float[]{0, 4}, 1, 2, 1, 4));
        assertArrayEquals(new float[]{3},
                M3NcmOps.linearResize(new float[]{0, 2, 4, 6}, 1, 4, 1, 1));
        assertArrayEquals(new float[]{3, 3, 3},
                M3NcmOps.linearResize(new float[]{3}, 1, 1, 1, 3));
    }

    @Test void resizeNeverMutatesOrAliasesInputs() {
        float[] input = {2, 4};
        float[] a = M3NcmOps.areaResize(input, 1, 2, 1, 2);
        float[] b = M3NcmOps.linearResize(input, 1, 2, 1, 2);
        a[0] = 99;
        b[1] = 99;
        assertArrayEquals(new float[]{2, 4}, input);
    }

    @Test void phiUsesCrossCorrelationPaddingBiasAndBlend() {
        assertArrayEquals(new float[]{-.375f, -.375f, 3.125f},
                M3NcmOps.phi(new float[]{1, 2, 4}, 1, 3, 1,
                        new float[]{1, 0, -1}, new float[]{.25f}, .5f));
    }

    @Test void phiMixesChannelsWithoutMixingBatches() {
        float[] weights = {0,0,0, 0,2,0, 0,-1,0, 0,0,0};
        assertArrayEquals(new float[]{20,-1, 40,-2, 60,-3, 80,-4},
                M3NcmOps.phi(new float[]{1,10, 2,20, 3,30, 4,40}, 2, 2, 2,
                        weights, new float[2], 1));
    }

    @Test void tiedHeadUsesMeanSquaredNormScaleAndOptionalBias() {
        float logTwo = (float) StrictMath.log(2);
        assertArrayEquals(new float[]{.5f, 3, -3.5f},
                M3NcmOps.tiedLogits(new float[]{2, 0}, 2, BOOK, logTwo,
                        new float[]{.5f, 0, -.5f}), 1e-6f);
        assertArrayEquals(new float[]{0, 3, -3},
                M3NcmOps.tiedLogits(new float[]{2, 0}, 2, BOOK, logTwo, null), 1e-6f);
        assertArrayEquals(new float[]{0, 0},
                M3NcmOps.tiedLogits(new float[]{2}, 1, new float[]{0, 0}, 0, null));
    }

    @Test void completeResidualCodecMatchesIndependentToyFixture() {
        M3NcmHierarchy h = toy();
        float[] source = {0, 0, 2, 2};
        int[] codes = h.encode(source, 1);
        assertArrayEquals(new int[]{0, 1, 1, 0, 2, 1}, codes);
        assertArrayEquals(source, h.decode(codes, 1));
        assertArrayEquals(new float[]{0, .5f, 1.5f, 2}, h.decodePrefix(new int[]{0, 1}, 1, 1));
        assertArrayEquals(new float[]{0, 0, 2, 2}, source);
    }

    @Test void nextConditionConsumesPackedPerBatchPrefixes() {
        assertArrayEquals(new float[]{0, .5f, 1.5f, 2, 2, 1.5f, .5f, 0},
                toy().nextCondition(new int[]{0, 1, 1, 0}, 2, 1));
    }

    @Test void nextConditionResizesTheAccumulatedMultilevelPrefix() {
        M3NcmHierarchy h = new M3NcmHierarchy("condition", 1, new int[]{1,2,3,5},
                new float[][]{{0,2},{-1,0,1},{0},{0}});
        assertArrayEquals(new float[]{1.1f,2,2.9f}, h.nextCondition(new int[]{1,0,2}, 1, 2), 1e-6f);
    }

    @Test void providedPhiIsAppliedAtTheFinalLevel() {
        M3NcmHierarchy h = new M3NcmHierarchy("phi-fixture", 1, new int[]{3},
                new float[][]{{1, 2, 4}}, new float[][]{{1, 0, -1}},
                new float[][]{{.25f}}, new float[]{.5f}, M3NcmOps.DEFAULT_BUDGET);
        assertArrayEquals(new float[]{-.375f, -.375f, 3.125f}, h.decode(new int[]{0, 1, 2}, 1));
    }

    @Test void exactFinalResidualGridReconstructsRandomIntegerLatents() {
        // Every integer input is representable: coarse outputs are half-integers,
        // and the final Cartesian codebook contains every possible residual.
        float[] coarse = cartesianGrid(-2, 2, 2);
        float[] fine = cartesianGrid(-4, 4, .5f);
        M3NcmHierarchy h = new M3NcmHierarchy("exact-grid", 2, new int[]{2, 4},
                new float[][]{coarse, fine});
        Random random = new Random(0x4e434d);
        for (int sample = 0; sample < 30; sample++) {
            int batch = 1 + random.nextInt(4);
            float[] input = new float[batch * 4 * 2];
            for (int i = 0; i < input.length; i++) input[i] = random.nextInt(5) - 2;
            int[] codes = h.encode(input, batch);
            assertArrayEquals(input, h.decode(codes, batch));
            assertArrayEquals(codes, h.encode(input, batch));
        }
    }

    @Test void resizingPreservesRandomConstantsAndConvexBounds() {
        Random random = new Random(74);
        for (int sample = 0; sample < 40; sample++) {
            int n = 1 + random.nextInt(19), m = 1 + random.nextInt(19);
            float[] input = new float[n * 3];
            float[] constant = {(random.nextInt(17) - 8) * .25f, 2.25f, -3.5f};
            for (int i = 0; i < n; i++) System.arraycopy(constant, 0, input, i * 3, 3);
            float[][] outputs = {M3NcmOps.areaResize(input, 1, n, 3, m),
                    M3NcmOps.linearResize(input, 1, n, 3, m)};
            for (float[] output : outputs) {
                assertEquals(m * 3, output.length);
                for (int i = 0; i < output.length; i++) assertEquals(constant[i % 3], output[i], 1e-6f);
            }
        }
        float[] bounded = M3NcmOps.linearResize(new float[]{-3, 8, -1, 4}, 1, 4, 1, 37);
        assertEquals(37, bounded.length);
        for (float value : bounded) assertTrue(value >= -3 && value <= 8);
    }

    @Test void hierarchyCopiesParametersAndMetadataDefensively() {
        int[] lengths = {2, 4};
        float[][] books = {{0, 2}, {-.5f, 0, .5f}};
        M3NcmHierarchy h = new M3NcmHierarchy("copy", 1, lengths, books);
        String fingerprint = h.fingerprint();
        lengths[0] = 99; books[0][1] = 99;
        h.levelLengths()[0] = 99; h.prefixOffsets()[1] = 99; h.codebook(0)[1] = 99;
        assertArrayEquals(new int[]{0, 1, 1, 0, 2, 1}, h.encode(new float[]{0, 0, 2, 2}, 1));
        assertEquals(fingerprint, h.fingerprint());
        assertArrayEquals(new int[]{0, 2, 6}, h.prefixOffsets());
        assertEquals(4, h.fullLength()); assertEquals(6, h.totalCodes());
        assertEquals(2, h.levelCount()); assertEquals(1, h.channels());
        assertEquals(2, h.vocabularySize(0)); assertEquals(3, h.vocabularySize(1));
        assertThrows(IllegalArgumentException.class, () -> h.vocabularySize(-1));
        assertThrows(IllegalArgumentException.class, () -> h.vocabularySize(2));
    }

    @Test void fingerprintHasAStableCrossPackageWireIdentity() {
        // Independently serialized with Python struct/hashlib using the documented fields.
        assertEquals("3bd17bbbb6c8d479742596729d9d6b45f1acd49a97ee7dcd8479ae68e54bb43e",
                toy().fingerprint());
    }

    @Test void phiParametersAreCopiedAndDecodedSnapshotsAreIndependent() {
        float[][] weights = {{1,0,-1}};
        float[][] biases = {{.25f}};
        float[] ratios = {.5f};
        M3NcmHierarchy h = new M3NcmHierarchy("snapshot", 1, new int[]{3},
                new float[][]{{1,2,4}}, weights, biases, ratios, M3NcmOps.DEFAULT_BUDGET);
        String hash = h.fingerprint();
        weights[0][0] = 99; biases[0][0] = 99; ratios[0] = 0;
        float[] first = h.decode(new int[]{0,1,2}, 1);
        assertArrayEquals(new float[]{-.375f,-.375f,3.125f}, first);
        first[0] = 100;
        assertArrayEquals(new float[]{-.375f,-.375f,3.125f}, h.decode(new int[]{0,1,2}, 1));
        assertEquals(hash, h.fingerprint());
    }

    @Test void fingerprintBindsVersionCodebooksTransformsAndLevelSchedule() {
        M3NcmHierarchy a = toy();
        assertEquals(a.fingerprint(), toy().fingerprint());
        assertNotEquals(a.fingerprint(), new M3NcmHierarchy("other", 1, new int[]{2,4},
                new float[][]{{0,2},{-.5f,0,.5f}}).fingerprint());
        assertNotEquals(a.fingerprint(), new M3NcmHierarchy("toy", 1, new int[]{2,4},
                new float[][]{{0,3},{-.5f,0,.5f}}).fingerprint());
        assertNotEquals(a.fingerprint(), new M3NcmHierarchy("toy", 1, new int[]{1,4},
                new float[][]{{0,2},{-.5f,0,.5f}}).fingerprint());
        M3NcmHierarchy p1 = phiHierarchy(1, .5f, 0);
        M3NcmHierarchy p2 = phiHierarchy(2, .5f, 0);
        M3NcmHierarchy p3 = phiHierarchy(1, .25f, 0);
        M3NcmHierarchy p4 = phiHierarchy(1, .5f, 1);
        assertNotEquals(p1.fingerprint(), p2.fingerprint());
        assertNotEquals(p1.fingerprint(), p3.fingerprint());
        assertNotEquals(p1.fingerprint(), p4.fingerprint());
    }

    @Test void primitiveValidationRejectsMalformedShapesAndNonfiniteInputs() {
        assertThrows(IllegalArgumentException.class, () -> M3NcmOps.nearest(new float[]{1}, 0, new float[]{1}));
        assertThrows(IllegalArgumentException.class, () -> M3NcmOps.nearest(new float[]{1}, 2, BOOK));
        assertThrows(IllegalArgumentException.class, () -> M3NcmOps.nearest(new float[]{Float.NaN}, 1, new float[]{0}));
        assertThrows(IllegalArgumentException.class, () -> M3NcmOps.nearest(new float[]{0}, 1, new float[]{Float.POSITIVE_INFINITY}));
        assertThrows(IllegalArgumentException.class, () -> M3NcmOps.areaResize(new float[]{1}, 2, 1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> M3NcmOps.linearResize(new float[]{1}, 1, 1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> M3NcmOps.phi(new float[]{1}, 1, 1, 1, new float[2], new float[]{0}, .5f));
        assertThrows(IllegalArgumentException.class, () -> M3NcmOps.phi(new float[]{1}, 1, 1, 1, new float[3], new float[]{0}, 2));
        assertThrows(IllegalArgumentException.class, () -> M3NcmOps.phi(new float[]{1}, 1, 1, 1, new float[3], new float[]{Float.NaN}, .5f));
        assertThrows(IllegalArgumentException.class, () -> M3NcmOps.tiedLogits(new float[]{0}, 1, new float[]{1}, 0, new float[2]));
        assertThrows(IllegalArgumentException.class, () -> M3NcmOps.tiedLogits(new float[]{0}, 1, new float[]{1}, Float.NaN, null));
    }

    @Test void computedNonfiniteValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> M3NcmOps.nearest(new float[]{Float.MAX_VALUE}, 1, new float[]{0}));
        assertThrows(IllegalArgumentException.class, () -> M3NcmOps.areaResize(new float[]{Float.MAX_VALUE,Float.MAX_VALUE}, 1, 2, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> M3NcmOps.phi(new float[]{Float.MAX_VALUE}, 1, 1, 1, new float[]{0,2,0}, new float[]{0}, 1));
        assertThrows(IllegalArgumentException.class, () -> M3NcmOps.tiedLogits(new float[]{1}, 1, new float[]{1}, 1000, null));
        assertThrows(IllegalArgumentException.class, () -> M3NcmOps.tiedLogits(new float[]{Float.MAX_VALUE}, 1, new float[]{2}, 0, null));
    }

    @Test void hierarchyRejectsInvalidSchedulesTransformsAndCodes() {
        assertThrows(IllegalArgumentException.class, () -> new M3NcmHierarchy("x", 1, new int[]{2,2}, new float[][]{{0},{0}}));
        assertThrows(IllegalArgumentException.class, () -> new M3NcmHierarchy("x", 1, new int[]{2,1}, new float[][]{{0},{0}}));
        assertThrows(IllegalArgumentException.class, () -> new M3NcmHierarchy("x", 2, new int[]{1}, new float[][]{{0}}));
        assertThrows(IllegalArgumentException.class, () -> new M3NcmHierarchy("x", 1, new int[]{1}, new float[][]{{Float.NaN}}));
        assertThrows(IllegalArgumentException.class, () -> new M3NcmHierarchy("x", 1, new int[]{1}, new float[][]{{0}}, new float[][]{null}, new float[][]{{0}}, new float[]{0}, M3NcmOps.DEFAULT_BUDGET));
        assertThrows(IllegalArgumentException.class, () -> toy().decode(new int[]{0,1,1,0,3,1}, 1));
        assertThrows(IllegalArgumentException.class, () -> toy().decode(new int[]{-1,1,1,0,2,1}, 1));
        assertThrows(IllegalArgumentException.class, () -> toy().decode(new int[]{0,1}, 1));
        assertThrows(IllegalArgumentException.class, () -> toy().nextCondition(new int[]{0,1}, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> toy().nextCondition(new int[]{0,1,1,0,2,1}, 1, 2));
    }

    @Test void budgetsBoundAllocationWorkAndWholeCodecCalls() {
        assertThrows(IllegalArgumentException.class, () -> new M3NcmOps.Budget(0, 1));
        M3NcmOps.Budget noAllocation = new M3NcmOps.Budget(1, 10000);
        assertThrows(IllegalArgumentException.class, () -> M3NcmOps.nearest(new float[]{0,0}, 2, BOOK, noAllocation, M3NcmOps.Cancellation.NONE));
        M3NcmOps.Budget noWork = new M3NcmOps.Budget(1000, 1);
        assertThrows(IllegalArgumentException.class, () -> M3NcmOps.areaResize(new float[]{1,2}, 1, 2, 1, 1, noWork, M3NcmOps.Cancellation.NONE));
        assertThrows(IllegalArgumentException.class, () -> toy().encode(new float[]{0,0,2,2}, 1, new M3NcmOps.Budget(20,100000), M3NcmOps.Cancellation.NONE));
        assertThrows(IllegalArgumentException.class, () -> M3NcmOps.linearResize(new float[]{1}, 1, 1, 1, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> M3NcmOps.areaResize(new float[]{1}, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 1));
    }

    @Test void cancellationStopsKernelsAndCodecWithoutMutatingInputs() {
        M3NcmOps.Cancellation stop = () -> { throw new CancellationException("requested"); };
        assertThrows(CancellationException.class, () -> M3NcmOps.nearest(new float[]{0,0}, 2, BOOK, M3NcmOps.DEFAULT_BUDGET, stop));
        float[] source = {0,0,2,2};
        AtomicInteger checks = new AtomicInteger();
        M3NcmOps.Cancellation later = () -> { if (checks.incrementAndGet() >= 3) throw new CancellationException("requested"); };
        assertThrows(CancellationException.class, () -> toy().encode(source, 1, M3NcmOps.DEFAULT_BUDGET, later));
        assertArrayEquals(new float[]{0,0,2,2}, source);
        assertArrayEquals(source, toy().decode(toy().encode(source, 1), 1));
    }

    @Test void sharedNativeValidationReturnsCheckedOutputLengths() {
        assertEquals(3, M3NcmOps.checkNearest(new float[]{0,0,1,1,2,0}, 2, BOOK));
        assertEquals(3, M3NcmOps.checkResize(new float[]{0,2,4,6,8}, 1, 5, 1, 3));
        assertEquals(3, M3NcmOps.checkPhi(new float[]{1,2,4}, 1, 3, 1, new float[]{1,0,-1}, new float[]{0}, .5f));
        assertEquals(3, M3NcmOps.checkTiedLogits(new float[]{2,0}, 2, BOOK, 0, null));
        assertThrows(IllegalArgumentException.class, () -> M3NcmOps.checkFinite(new float[]{Float.NaN}));
    }

    private static M3NcmHierarchy toy() {
        return new M3NcmHierarchy("toy", 1, new int[]{2, 4}, new float[][]{{0, 2}, {-.5f, 0, .5f}});
    }

    private static M3NcmHierarchy phiHierarchy(float weight, float ratio, float bias) {
        return new M3NcmHierarchy("p", 1, new int[]{1}, new float[][]{{1}},
                new float[][]{{0,weight,0}}, new float[][]{{bias}}, new float[]{ratio}, M3NcmOps.DEFAULT_BUDGET);
    }

    private static float[] cartesianGrid(float min, float max, float step) {
        int n = Math.round((max - min) / step) + 1;
        float[] result = new float[n * n * 2];
        int at = 0;
        for (int a = 0; a < n; a++) for (int b = 0; b < n; b++) {
            result[at++] = min + a * step; result[at++] = min + b * step;
        }
        return result;
    }
}
