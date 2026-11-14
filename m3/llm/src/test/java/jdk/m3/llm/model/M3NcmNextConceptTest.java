// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.llm.model;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class M3NcmNextConceptTest {
    private static M3ModelInput input() {
        return M3ModelInput.of("next-concept-source",
                "a".repeat(64), "b".repeat(64), "c".repeat(64), "d".repeat(64));
    }

    private static M3NcmQuantizer model() {
        return M3NcmQuantizer.of(4, 1, new int[]{1, 2, 4}, new int[]{2, 2, 2},
                new float[]{0, 2, -1, 1, 100, -100});
    }

    private static M3NcmCodes codes(M3NcmQuantizer model, int... values) {
        int[] levels = model.copyLevelLengths();
        int[] offsets = new int[levels.length + 1];
        for (int i = 0; i < levels.length; i++) offsets[i + 1] = offsets[i] + levels[i];
        return M3NcmCodes.of(input(), model.codebookRoot(), offsets,
                model.copyCodebookSizes(), values);
    }

    @Test void donorSyntheticMultiscaleBoundaryUsesCumulativeNextScaleInputs() {
        // Hand-calculated fixture for pinned quant.py::idx_to_ncm_input, not trained weights.
        M3NcmQuantizer model = model();
        M3NcmCodes codes = codes(model, 1, 0, 1, 0, 0, 0, 0);
        assertArrayEquals(new float[]{2, 2}, model.nextLevelInput(codes, 1));
        assertArrayEquals(new float[]{1, 1.5f, 2.5f, 3}, model.nextLevelInput(codes, 2));
        assertArrayEquals(new float[]{2, 2, 1, 1.5f, 2.5f, 3}, model.nextConceptInputs(codes));
        M3NcmCodes differentFinal = codes(model, 1, 0, 1, 1, 1, 1, 1);
        assertArrayEquals(model.nextConceptInputs(codes), model.nextConceptInputs(differentFinal));
        assertNotEquals(model.reconstruct(codes)[0], model.reconstruct(differentFinal)[0]);
    }

    @Test void unevenAreaBinsPoolTheReconstructionAtTheNextResolution() {
        M3NcmQuantizer model = M3NcmQuantizer.of(5, 1,
                new int[]{2, 3, 5}, new int[]{2, 1, 1}, new float[]{0, 6, 0, 0});
        M3NcmCodes codes = codes(model, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0);
        assertArrayEquals(new float[]{0.3f, 3, 5.7f}, model.nextLevelInput(codes, 1), 0.000001f);
        assertArrayEquals(new float[]{0, 0.6f, 3, 5.4f, 6}, model.nextLevelInput(codes, 2));
        assertArrayEquals(new float[]{0.3f, 3, 5.7f, 0, 0.6f, 3, 5.4f, 6},
                model.nextConceptInputs(codes), 0.000001f);
    }

    @Test void equalResolutionsAndMultipleChannelsKeepPositionMajorOrdering() {
        M3NcmQuantizer model = M3NcmQuantizer.of(2, 2,
                new int[]{1, 2, 2}, new int[]{1, 2, 1}, new float[]{1, 10, 0, -2, 2, 0, 99, 99});
        M3NcmCodes codes = codes(model, 0, 0, 1, 0, 0);
        assertArrayEquals(new float[]{1, 10, 1, 10}, model.nextLevelInput(codes, 1));
        assertArrayEquals(new float[]{1, 8, 3, 10}, model.nextLevelInput(codes, 2));
        assertArrayEquals(new float[]{1, 10, 1, 10, 1, 8, 3, 10}, model.nextConceptInputs(codes));
    }

    @Test void donorBlendTransformsApplyBeforePoolingAndUnusedFinalTransformIsSkipped() {
        M3NcmQuantizer model = M3NcmQuantizer.withResidualConvolution(4, 1,
                new int[]{1, 2, 4}, new int[]{1, 2, 1}, new float[]{2, -1, 1, Float.MAX_VALUE},
                3, new float[]{0.5f, 1, 1},
                new float[]{1, 1, 1, 0, 1, 0, 0, 2, 0}, new float[]{0, 1, 0},
                M3NcmQuantizer.Backend.JAVA);
        M3NcmCodes codes = codes(model, 0, 0, 1, 0, 0, 0, 0);
        assertArrayEquals(new float[]{3.5f, 3.5f}, model.nextLevelInput(codes, 1));
        assertArrayEquals(new float[]{3, 4.5f, 5.5f, 5}, model.nextLevelInput(codes, 2));
        assertArrayEquals(new float[]{3.5f, 3.5f, 3, 4.5f, 5.5f, 5}, model.nextConceptInputs(codes));
        assertThrows(ArithmeticException.class, () -> model.reconstruct(codes));
    }

    @Test void oneLevelHasNoNextInputAndDoesNotEvaluateAnyContribution() {
        M3NcmQuantizer model = M3NcmQuantizer.withResidualConvolution(
                1, 1, new int[]{1}, new int[]{1}, new float[]{Float.MAX_VALUE},
                1, new float[]{1}, new float[]{2}, new float[]{0}, M3NcmQuantizer.Backend.JAVA);
        M3NcmCodes codes = codes(model, 0);
        assertArrayEquals(new float[0], model.nextConceptInputs(codes));
        assertThrows(IllegalArgumentException.class, () -> model.nextLevelInput(codes, 0));
        assertThrows(IllegalArgumentException.class, () -> model.nextLevelInput(codes, 1));
        assertThrows(ArithmeticException.class, () -> model.reconstruct(codes));
        M3NcmCodes foreign = M3NcmCodes.of(input(), "f".repeat(64),
                new int[]{0, 1}, new int[]{1}, new int[]{0});
        assertThrows(IllegalArgumentException.class, () -> model.nextConceptInputs(foreign));
    }

    @Test void allNextInputPathsValidateBoundsCodebookAndGeometry() {
        M3NcmQuantizer model = model();
        M3NcmCodes codes = codes(model, 1, 0, 1, 0, 0, 0, 0);
        for (int completed : new int[]{Integer.MIN_VALUE, -1, 0, 3, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> model.nextLevelInput(codes, completed));
        }
        M3NcmCodes[] incompatible = {
            M3NcmCodes.of(input(), "f".repeat(64), new int[]{0, 1, 3, 7},
                    new int[]{2, 2, 2}, new int[7]),
            M3NcmCodes.of(input(), model.codebookRoot(), new int[]{0, 2, 4, 7},
                    new int[]{2, 2, 2}, new int[7]),
            M3NcmCodes.of(input(), model.codebookRoot(), new int[]{0, 1, 3, 7},
                    new int[]{3, 2, 2}, new int[7])
        };
        for (M3NcmCodes wrong : incompatible) {
            assertThrows(IllegalArgumentException.class, () -> model.nextLevelInput(wrong, 1));
            assertThrows(IllegalArgumentException.class, () -> model.nextConceptInputs(wrong));
        }
        assertThrows(NullPointerException.class, () -> model.nextLevelInput(null, 1));
        assertThrows(NullPointerException.class, () -> model.nextConceptInputs(null));
    }

    @Test void cumulativeOverflowIsRefusedAtTheFirstAffectedNextInput() {
        M3NcmQuantizer model = M3NcmQuantizer.of(1, 1,
                new int[]{1, 1, 1}, new int[]{1, 1, 1}, new float[]{Float.MAX_VALUE, Float.MAX_VALUE, 0});
        M3NcmCodes codes = codes(model, 0, 0, 0);
        assertArrayEquals(new float[]{Float.MAX_VALUE}, model.nextLevelInput(codes, 1));
        assertThrows(ArithmeticException.class, () -> model.nextLevelInput(codes, 2));
        assertThrows(ArithmeticException.class, () -> model.nextConceptInputs(codes));
    }

    @Test void concatenatedOutputOverflowIsRefusedBeforeHugeLatentAllocation() {
        // Model/code storage is below 2 MiB; a latent vector would require 4.8 GB.
        M3NcmQuantizer model = M3NcmQuantizer.of(40_000, 30_000,
                new int[]{40_000, 40_000, 40_000}, new int[]{1, 1, 1}, new float[90_000]);
        M3NcmCodes codes = M3NcmCodes.of(input(), model.codebookRoot(),
                new int[]{0, 40_000, 80_000, 120_000}, new int[]{1, 1, 1}, new int[120_000]);
        assertThrows(IllegalArgumentException.class, () -> model.nextConceptInputs(codes));
    }

    @Test void returnedArraysAreIndependentAcrossCallsScalesAndThreads() throws Exception {
        M3NcmQuantizer model = model();
        M3NcmCodes codes = codes(model, 1, 0, 1, 0, 0, 0, 0);
        float[] expected = {2, 2, 1, 1.5f, 2.5f, 3};
        model.nextLevelInput(codes, 1)[0] = 999;
        model.nextLevelInput(codes, 2)[0] = 999;
        model.nextConceptInputs(codes)[0] = 999;
        assertArrayEquals(expected, model.nextConceptInputs(codes));
        ArrayList<Callable<float[]>> tasks = new ArrayList<>();
        for (int i = 0; i < 48; i++) tasks.add(() -> model.nextConceptInputs(codes));
        try (var executor = Executors.newFixedThreadPool(4)) {
            for (var result : executor.invokeAll(tasks)) assertArrayEquals(expected, result.get());
        }
    }
}
