/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.llm.model;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class M3NcmQuantizerTest {
    static M3ModelInput input() {
        return M3ModelInput.of("ncm-source",
                "a".repeat(64), "b".repeat(64), "c".repeat(64), "d".repeat(64));
    }

    private static M3NcmQuantizer example(M3NcmQuantizer.Backend backend) {
        return M3NcmQuantizer.of(4, 1, new int[]{1, 4}, new int[]{3, 3},
                new float[]{0, 2, 4, -1, 0, 1}, backend);
    }

    @Test void residualLevelsAndPartialReconstructionsHaveKnownValues() {
        M3NcmQuantizer quantizer = example(M3NcmQuantizer.Backend.JAVA);
        float[] latent = {1, 2, 3, 2};
        M3ModelInput source = input();
        M3NcmCodes codes = quantizer.encode(source, latent);
        assertArrayEquals(new int[]{1, 0, 1, 2, 1}, codes.copyCodes());
        assertArrayEquals(latent, quantizer.reconstruct(codes));
        assertArrayEquals(new float[]{2, 2, 2, 2}, quantizer.reconstruct(codes, 1));
        assertArrayEquals(new float[4], quantizer.reconstruct(codes, 0));
        assertSame(source, codes.input());
        assertTrue(codes.matches(source, quantizer.codebookRoot()));
        assertEquals(4, quantizer.fullLength());
        assertEquals(1, quantizer.channels());
        assertEquals(2, quantizer.levelCount());
        assertEquals(64, quantizer.codebookRoot().length());
        assertThrows(IllegalArgumentException.class, () -> quantizer.reconstruct(codes, -1));
        assertThrows(IllegalArgumentException.class, () -> quantizer.reconstruct(codes, 3));
    }

    @Test void unevenAreaBinsAndHalfPixelInterpolationAreExplicit() {
        M3NcmQuantizer quantizer = M3NcmQuantizer.of(5, 1,
                new int[]{2, 5}, new int[]{3, 1}, new float[]{0, 3, 6, 0});
        M3NcmCodes codes = quantizer.encode(input(), new float[]{0, 0, 3, 6, 6});
        assertEquals(0, codes.codeAt(0, 0));
        assertEquals(2, codes.codeAt(0, 1));
        assertArrayEquals(new float[]{0, 0.6f, 3, 5.4f, 6}, quantizer.reconstruct(codes, 1));
    }

    @Test void sameResolutionResidualLevelsAndMultipleChannelsAreAccepted() {
        M3NcmQuantizer quantizer = M3NcmQuantizer.of(2, 2,
                new int[]{2, 2}, new int[]{2, 2}, new float[]{1, 2, 3, 4, 0, 0, 1, -1});
        M3NcmCodes codes = quantizer.encode(input(), new float[]{2, 1, 3, 4});
        assertArrayEquals(new int[]{0, 1, 1, 0}, codes.copyCodes());
        assertArrayEquals(new float[]{2, 1, 3, 4}, quantizer.reconstruct(codes));
    }

    @Test void nearestTiesSignedZerosExtremesAndEmptyBatchAreDefined() {
        assertArrayEquals(new int[]{0}, M3NcmNative.nearestJava(
                new float[]{0}, 1, 1, new float[]{-1, 1}, 0, 2));
        assertArrayEquals(new int[]{0, 0}, M3NcmNative.nearestJava(
                new float[]{-0.0f, 0.0f}, 2, 1, new float[]{0.0f, -0.0f}, 0, 2));
        assertArrayEquals(new int[]{1}, M3NcmNative.nearestJava(
                new float[]{Float.MAX_VALUE}, 1, 1,
                new float[]{-Float.MAX_VALUE, Float.MAX_VALUE}, 0, 2));
        assertArrayEquals(new int[0], M3NcmNative.nearestJava(
                new float[0], 0, 1, new float[]{0}, 0, 1));
        assertArrayEquals(new int[]{0}, M3NcmNative.nearestJava(
                new float[]{0}, 1, 1, new float[]{Float.NaN, 0, Float.POSITIVE_INFINITY}, 1, 1));
    }

    @Test void factoriesAndResultsDoNotRetainMutableArrayAliases() {
        int[] levels = {1, 4};
        int[] sizes = {3, 3};
        float[] books = {0, 2, 4, -1, 0, 1};
        M3NcmQuantizer quantizer = M3NcmQuantizer.of(4, 1, levels, sizes, books);
        String root = quantizer.codebookRoot();
        levels[0] = 99;
        sizes[0] = 99;
        books[1] = Float.NaN;
        quantizer.copyLevelLengths()[0] = 99;
        quantizer.copyCodebookSizes()[0] = 99;
        float[] latent = {1, 2, 3, 2};
        M3NcmCodes codes = quantizer.encode(input(), latent);
        latent[0] = 777;
        codes.copyCodes()[0] = 2;
        quantizer.reconstruct(codes)[0] = 777;
        assertArrayEquals(new float[]{1, 2, 3, 2}, quantizer.reconstruct(codes));
        assertEquals(root, quantizer.codebookRoot());
        assertArrayEquals(new int[]{1, 4}, quantizer.copyLevelLengths());
        assertEquals(4L * (2 + 2 + 3 + 3 + 6), quantizer.retainedBytes());
    }

    @Test void rootBindsExactFloatBitsShapeAndTransformButNotExecutionBackend() {
        assertEquals(example(M3NcmQuantizer.Backend.JAVA).codebookRoot(),
                example(M3NcmQuantizer.Backend.AUTO).codebookRoot());
        assertNotEquals(M3NcmQuantizer.of(1, 1, new int[]{1}, new int[]{1},
                        new float[]{0.0f}).codebookRoot(),
                M3NcmQuantizer.of(1, 1, new int[]{1}, new int[]{1},
                        new float[]{-0.0f}).codebookRoot());
        assertNotEquals(M3NcmQuantizer.of(1, 1, new int[]{1}, new int[]{1},
                        new float[]{1}).codebookRoot(),
                M3NcmQuantizer.withResidualConvolution(1, 1, new int[]{1}, new int[]{1},
                        new float[]{1}, 1, new float[]{1}, new float[]{1}, new float[]{0},
                        M3NcmQuantizer.Backend.JAVA).codebookRoot());
    }

    @Test void residualConvolutionUsesZeroPaddingBlendAndChannelOrder() {
        M3NcmQuantizer quantizer = M3NcmQuantizer.withResidualConvolution(
                3, 1, new int[]{3}, new int[]{3}, new float[]{1, 2, 3},
                3, new float[]{0.5f}, new float[]{1, 1, 1}, new float[]{0},
                M3NcmQuantizer.Backend.JAVA);
        assertArrayEquals(new float[]{2, 4, 4},
                quantizer.reconstruct(quantizer.encode(input(), new float[]{1, 2, 3})));
        M3NcmQuantizer channels = M3NcmQuantizer.withResidualConvolution(
                2, 2, new int[]{2}, new int[]{2}, new float[]{1, 2, 3, 4},
                1, new float[]{1}, new float[]{0, 1, 1, 0}, new float[]{0, 0},
                M3NcmQuantizer.Backend.JAVA);
        assertArrayEquals(new float[]{2, 1, 4, 3},
                channels.reconstruct(channels.encode(input(), new float[]{1, 2, 3, 4})));
    }

    @Test void transformConfigurationIsCopiedAndZeroRatioIsIdentity() {
        float[] ratio = {0};
        float[] weights = {50};
        float[] bias = {100};
        M3NcmQuantizer quantizer = M3NcmQuantizer.withResidualConvolution(
                1, 1, new int[]{1}, new int[]{1}, new float[]{2}, 1, ratio, weights, bias,
                M3NcmQuantizer.Backend.JAVA);
        ratio[0] = 1;
        weights[0] = Float.NaN;
        bias[0] = Float.NaN;
        assertArrayEquals(new float[]{2}, quantizer.reconstruct(quantizer.encode(input(), new float[]{2})));
    }

    @Test void dimensionsAndNonfiniteModelDataFailBeforeInference() {
        assertThrows(NullPointerException.class,
                () -> M3NcmQuantizer.of(1, 1, null, new int[]{1}, new float[]{0}));
        assertThrows(IllegalArgumentException.class,
                () -> M3NcmQuantizer.of(0, 1, new int[]{1}, new int[]{1}, new float[]{0}));
        assertThrows(IllegalArgumentException.class,
                () -> M3NcmQuantizer.of(1, 0, new int[]{1}, new int[]{1}, new float[]{0}));
        assertThrows(IllegalArgumentException.class,
                () -> M3NcmQuantizer.of(1, 1, new int[0], new int[0], new float[0]));
        assertThrows(IllegalArgumentException.class,
                () -> M3NcmQuantizer.of(4, 1, new int[]{2, 1, 4}, new int[]{1, 1, 1}, new float[3]));
        assertThrows(IllegalArgumentException.class,
                () -> M3NcmQuantizer.of(4, 1, new int[]{2}, new int[]{1}, new float[]{0}));
        assertThrows(IllegalArgumentException.class,
                () -> M3NcmQuantizer.of(1, 1, new int[]{1}, new int[]{0}, new float[0]));
        assertThrows(IllegalArgumentException.class,
                () -> M3NcmQuantizer.of(Integer.MAX_VALUE, 2,
                        new int[]{Integer.MAX_VALUE}, new int[]{1}, new float[2]));
        assertThrows(IllegalArgumentException.class,
                () -> M3NcmQuantizer.of(Integer.MAX_VALUE, 1,
                        new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE}, new int[]{1, 1}, new float[2]));
        assertThrows(IllegalArgumentException.class,
                () -> M3NcmQuantizer.of(1, 2, new int[]{1},
                        new int[]{Integer.MAX_VALUE}, new float[0]));
        for (float invalid : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,
                    () -> M3NcmQuantizer.of(1, 1, new int[]{1}, new int[]{1}, new float[]{invalid}));
            assertThrows(IllegalArgumentException.class,
                    () -> example(M3NcmQuantizer.Backend.JAVA).encode(input(), new float[]{0, invalid, 0, 0}));
        }
        assertThrows(IllegalArgumentException.class,
                () -> example(M3NcmQuantizer.Backend.JAVA).encode(input(), new float[3]));
    }

    @Test void convolutionConfigurationAndFiniteOverflowAreRefused() {
        for (int invalidKernel : new int[]{-1, 0, 2, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class,
                    () -> M3NcmQuantizer.withResidualConvolution(1, 2, new int[]{1},
                            new int[]{1}, new float[2], invalidKernel, new float[]{1},
                            new float[4], new float[2], M3NcmQuantizer.Backend.JAVA));
        }
        for (float invalidRatio : new float[]{-1, 2, Float.NaN, Float.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,
                    () -> M3NcmQuantizer.withResidualConvolution(1, 1, new int[]{1},
                            new int[]{1}, new float[]{1}, 1, new float[]{invalidRatio},
                            new float[]{1}, new float[]{0}, M3NcmQuantizer.Backend.JAVA));
        }
        M3NcmQuantizer overflow = M3NcmQuantizer.withResidualConvolution(
                1, 1, new int[]{1}, new int[]{1}, new float[]{Float.MAX_VALUE},
                1, new float[]{1}, new float[]{2}, new float[]{0}, M3NcmQuantizer.Backend.JAVA);
        assertThrows(ArithmeticException.class, () -> overflow.encode(input(), new float[]{Float.MAX_VALUE}));
        M3NcmQuantizer subtraction = M3NcmQuantizer.of(1, 1,
                new int[]{1}, new int[]{1}, new float[]{-Float.MAX_VALUE});
        assertThrows(ArithmeticException.class, () -> subtraction.encode(input(), new float[]{Float.MAX_VALUE}));
    }

    @Test void reconstructionRejectsForeignCodebookOrShape() {
        M3NcmQuantizer quantizer = example(M3NcmQuantizer.Backend.JAVA);
        M3NcmCodes foreign = M3NcmCodes.of(input(), "f".repeat(64),
                new int[]{0, 1, 5}, new int[]{3, 3}, new int[5]);
        assertThrows(IllegalArgumentException.class, () -> quantizer.reconstruct(foreign));
        M3NcmCodes wrongShape = M3NcmCodes.of(input(), quantizer.codebookRoot(),
                new int[]{0, 2, 5}, new int[]{3, 3}, new int[5]);
        assertThrows(IllegalArgumentException.class, () -> quantizer.reconstruct(wrongShape));
    }

    @Test void autoDoesNotLoadLibrariesAndNativeModeFailsWhenUnavailable() {
        M3ModelInput source = input();
        boolean before = M3NcmQuantizer.nativeAvailable();
        int[] expected = example(M3NcmQuantizer.Backend.JAVA)
                .encode(source, new float[]{1, 2, 3, 2}).copyCodes();
        assertArrayEquals(expected, example(M3NcmQuantizer.Backend.AUTO)
                .encode(source, new float[]{1, 2, 3, 2}).copyCodes());
        assertEquals(before, M3NcmQuantizer.nativeAvailable());
        if (!before) {
            assertThrows(IllegalStateException.class, () -> example(M3NcmQuantizer.Backend.NATIVE)
                    .encode(source, new float[]{1, 2, 3, 2}));
        }
    }

    @Test void concurrentIndependentCallsPreserveExactCodesAndInputs() throws Exception {
        M3NcmQuantizer quantizer = example(M3NcmQuantizer.Backend.JAVA);
        M3ModelInput source = input();
        float[] latent = {1, 2, 3, 2};
        String expected = quantizer.encode(source, latent).rootHash();
        ArrayList<Callable<String>> jobs = new ArrayList<>();
        for (int i = 0; i < 128; i++) {
            jobs.add(() -> quantizer.encode(source, latent).rootHash());
        }
        try (var executor = Executors.newFixedThreadPool(8)) {
            for (var result : executor.invokeAll(jobs)) assertEquals(expected, result.get());
        }
        assertArrayEquals(new float[]{1, 2, 3, 2}, latent);
    }
}
