// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.llm.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class M3NcmCheckpointTest {
    private static final int HEADER_BYTES = 20;

    private static M3ModelInput input() {
        return M3ModelInput.of("checkpoint-source",
                "a".repeat(64), "b".repeat(64), "c".repeat(64), "d".repeat(64));
    }

    private static M3NcmQuantizer plain() {
        return M3NcmQuantizer.of(4, 1, new int[]{1, 2, 4}, new int[]{2, 2, 2},
                new float[]{-0.0f, 2, -1, 1, -0.0f, 0.25f});
    }

    private static M3NcmQuantizer blend() {
        return M3NcmQuantizer.withResidualConvolution(3, 2,
                new int[]{1, 3}, new int[]{2, 2},
                new float[]{-0.0f, 0.0f, 1, 2, -1, 1, 0.25f, -0.5f},
                1, new float[]{-0.0f, 0.5f},
                new float[]{-0.0f, 1, -1, 0, 0.25f, -0.5f, 0.75f, 1},
                new float[]{-0.0f, 0, 0.5f, -1}, M3NcmQuantizer.Backend.JAVA);
    }

    private static ByteBuffer view(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
    }

    private static int dimensionsOffset(byte[] bytes) {
        return HEADER_BYTES + Integer.BYTES + view(bytes).getInt(HEADER_BYTES);
    }

    // Locate arrays in the documented wire format, without reading private owner fields.
    private static int countOffset(byte[] bytes, int array) {
        ByteBuffer input = view(bytes);
        input.position(dimensionsOffset(bytes) + 3 * Integer.BYTES);
        for (int i = 0; i < array; i++) {
            int count = input.getInt();
            input.position(input.position() + count * Integer.BYTES);
        }
        return input.position();
    }

    private static void rejectsIntMutation(M3NcmQuantizer model, byte[] bytes,
                                           int offset, int replacement) {
        byte[] altered = bytes.clone();
        view(altered).putInt(offset, replacement);
        assertThrows(IllegalArgumentException.class, () -> M3NcmQuantizer.fromCheckpoint(
                altered, model.codebookRoot(), altered.length, M3NcmQuantizer.Backend.JAVA));
    }

    @Test void versionOneWireAndExistingRootHaveAnIndependentGoldenFixture() {
        // Independently assembled big-endian fixture: one code containing negative zero.
        String root = "f88aeeced0837a2d527b5996527c7d251bb50357bbc8ec78927ee230b7066c0a";
        byte[] golden = HexFormat.of().parseHex(
                "4d334e434d43500000000001000000000000009d000000694d332d4e434d2d5256512f313b6633323b64697374616e63"
                + "652d6636342d6f7264657265643b617265612d666c6f6f726365696c3b6c696e6561722d68616c66706978656c2d6564"
                + "67653b636f6e762d73616d652d7a65726f3b7469652d6c6f776573742d636f6465000000010000000100000000000000"
                + "010000000100000001000000010000000180000000000000000000000000000000");
        M3NcmQuantizer model = M3NcmQuantizer.of(1, 1,
                new int[]{1}, new int[]{1}, new float[]{-0.0f});
        assertEquals(root, model.codebookRoot());
        assertEquals(177, golden.length);
        assertArrayEquals(golden, model.checkpointBytes(golden.length));
        assertArrayEquals(golden, M3NcmQuantizer.fromCheckpoint(golden, root,
                golden.length, M3NcmQuantizer.Backend.JAVA).checkpointBytes(golden.length));
    }

    @Test void allSupportedStateRoundTripsWithExactIdentityCodesAndReconstruction() {
        for (M3NcmQuantizer model : new M3NcmQuantizer[]{plain(), blend()}) {
            float[] latents = model.channels() == 1
                    ? new float[]{1, 2, 3, 2} : new float[]{1, 2, 0.25f, -0.5f, -1, 1};
            byte[] bytes = model.checkpointBytes(Long.MAX_VALUE);
            M3NcmQuantizer restored = M3NcmQuantizer.fromCheckpoint(bytes,
                    model.codebookRoot(), bytes.length, M3NcmQuantizer.Backend.JAVA);
            assertEquals(model.codebookRoot(), restored.codebookRoot());
            assertEquals(model.retainedBytes(), restored.retainedBytes());
            assertArrayEquals(model.copyLevelLengths(), restored.copyLevelLengths());
            assertArrayEquals(model.copyCodebookSizes(), restored.copyCodebookSizes());
            assertArrayEquals(bytes, restored.checkpointBytes(bytes.length));
            M3ModelInput source = input();
            M3NcmCodes codes = model.encode(source, latents);
            assertEquals(codes, restored.encode(source, latents));
            for (int levels = 0; levels <= model.levelCount(); levels++) {
                assertArrayEquals(model.reconstruct(codes, levels), restored.reconstruct(codes, levels));
            }
            assertArrayEquals(model.nextConceptInputs(codes), restored.nextConceptInputs(codes));
        }
    }

    @Test void signedZerosSubnormalsAndFiniteExtremesSurviveRawFloatStorage() {
        M3NcmQuantizer model = M3NcmQuantizer.withResidualConvolution(
                1, 1, new int[]{1}, new int[]{6},
                new float[]{0.0f, -0.0f, Float.MIN_VALUE, -Float.MIN_VALUE,
                        Float.MAX_VALUE, -Float.MAX_VALUE},
                1, new float[]{-0.0f}, new float[]{-Float.MIN_NORMAL},
                new float[]{-0.0f}, M3NcmQuantizer.Backend.JAVA);
        byte[] bytes = model.checkpointBytes(Long.MAX_VALUE);
        M3NcmQuantizer restored = M3NcmQuantizer.fromCheckpoint(bytes,
                model.codebookRoot(), bytes.length, M3NcmQuantizer.Backend.JAVA);
        assertArrayEquals(bytes, restored.checkpointBytes(bytes.length));
        assertEquals(model.codebookRoot(), restored.codebookRoot());
        byte[] positiveZero = bytes.clone();
        view(positiveZero).putInt(countOffset(bytes, 3) + Integer.BYTES, 0);
        assertThrows(IllegalArgumentException.class, () -> M3NcmQuantizer.fromCheckpoint(
                positiveZero, model.codebookRoot(), bytes.length, M3NcmQuantizer.Backend.JAVA));
    }

    @Test void budgetsAdmitTheExactBoundaryAndRefuseBeforeReadingInvalidBodies() {
        M3NcmQuantizer model = blend();
        byte[] bytes = model.checkpointBytes(Long.MAX_VALUE);
        assertArrayEquals(bytes, model.checkpointBytes(bytes.length));
        for (long budget : new long[]{-1, 0, bytes.length - 1L}) {
            assertThrows(IllegalArgumentException.class, () -> model.checkpointBytes(budget));
            assertThrows(IllegalArgumentException.class, () -> M3NcmQuantizer.fromCheckpoint(
                    bytes, model.codebookRoot(), budget, M3NcmQuantizer.Backend.JAVA));
        }
        assertEquals(model.codebookRoot(), M3NcmQuantizer.fromCheckpoint(bytes,
                model.codebookRoot(), bytes.length, M3NcmQuantizer.Backend.JAVA).codebookRoot());
        byte[] hostile = bytes.clone();
        view(hostile).putInt(countOffset(hostile, 0), Integer.MAX_VALUE);
        IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                () -> M3NcmQuantizer.fromCheckpoint(hostile, model.codebookRoot(),
                        bytes.length - 1L, M3NcmQuantizer.Backend.JAVA));
        assertTrue(refusal.getMessage().contains("budget"));
    }

    @Test void restoreDoesNotRetainCallerBytesOrSerializeAnExecutionBackend() {
        M3NcmQuantizer model = blend();
        byte[] expected = model.checkpointBytes(Long.MAX_VALUE);
        boolean loaded = M3NcmQuantizer.nativeAvailable();
        for (M3NcmQuantizer.Backend backend : M3NcmQuantizer.Backend.values()) {
            byte[] input = expected.clone();
            M3NcmQuantizer restored = M3NcmQuantizer.fromCheckpoint(input,
                    model.codebookRoot(), input.length, backend);
            Arrays.fill(input, (byte) 0);
            assertArrayEquals(expected, restored.checkpointBytes(expected.length));
            byte[] exported = restored.checkpointBytes(expected.length);
            Arrays.fill(exported, (byte) 0);
            assertArrayEquals(expected, restored.checkpointBytes(expected.length));
            assertEquals(loaded, M3NcmQuantizer.nativeAvailable());
        }
    }

    @Test void explicitlyEnabledJniRestoresBothArchitecturesAcrossEveryBackend() {
        String library = System.getProperty("m3.ncm.library");
        assumeTrue(library != null && !library.isBlank(), "explicit NCM native library not supplied");
        M3NcmQuantizer.loadNative(Path.of(library));
        assertTrue(M3NcmQuantizer.nativeAvailable());
        M3ModelInput source = input();
        for (M3NcmQuantizer model : new M3NcmQuantizer[]{plain(), blend()}) {
            float[] latents = model.channels() == 1
                    ? new float[]{1, 2, 3, 2} : new float[]{1, 2, 0.25f, -0.5f, -1, 1};
            byte[] bytes = model.checkpointBytes(Long.MAX_VALUE);
            M3NcmCodes expected = model.encode(source, latents);
            for (M3NcmQuantizer.Backend backend : M3NcmQuantizer.Backend.values()) {
                M3NcmQuantizer restored = M3NcmQuantizer.fromCheckpoint(bytes,
                        model.codebookRoot(), bytes.length, backend);
                assertEquals(model.codebookRoot(), restored.codebookRoot());
                assertArrayEquals(bytes, restored.checkpointBytes(bytes.length));
                assertEquals(expected, restored.encode(source, latents));
                assertArrayEquals(model.reconstruct(expected), restored.reconstruct(expected));
                assertArrayEquals(model.nextConceptInputs(expected), restored.nextConceptInputs(expected));
                for (int completed = 1; completed < model.levelCount(); completed++) {
                    assertArrayEquals(model.nextLevelInput(expected, completed),
                            restored.nextLevelInput(expected, completed));
                }
            }
        }
    }

    @Test void everyTruncationAndBothFormsOfTrailingDataAreRejected() {
        M3NcmQuantizer model = plain();
        byte[] bytes = model.checkpointBytes(Long.MAX_VALUE);
        for (int length = 0; length < bytes.length; length++) {
            byte[] prefix = Arrays.copyOf(bytes, length);
            assertThrows(IllegalArgumentException.class, () -> M3NcmQuantizer.fromCheckpoint(
                    prefix, model.codebookRoot(), bytes.length, M3NcmQuantizer.Backend.JAVA));
            if (prefix.length >= HEADER_BYTES) {
                view(prefix).putInt(16, prefix.length - HEADER_BYTES);
                assertThrows(IllegalArgumentException.class, () -> M3NcmQuantizer.fromCheckpoint(
                        prefix, model.codebookRoot(), bytes.length, M3NcmQuantizer.Backend.JAVA));
            }
        }
        byte[] trailing = Arrays.copyOf(bytes, bytes.length + 4);
        assertThrows(IllegalArgumentException.class, () -> M3NcmQuantizer.fromCheckpoint(
                trailing, model.codebookRoot(), trailing.length, M3NcmQuantizer.Backend.JAVA));
        view(trailing).putInt(16, trailing.length - HEADER_BYTES);
        IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                () -> M3NcmQuantizer.fromCheckpoint(trailing, model.codebookRoot(),
                        trailing.length, M3NcmQuantizer.Backend.JAVA));
        assertTrue(refusal.getMessage().contains("trailing"));
    }

    @Test void versionsArchitecturesSemanticsAndEnvelopeLengthsFailClosed() {
        M3NcmQuantizer model = plain();
        byte[] bytes = model.checkpointBytes(Long.MAX_VALUE);
        for (int offset : new int[]{0, 8, 12, 16, HEADER_BYTES}) {
            for (int value : new int[]{-1, Integer.MAX_VALUE, 2}) {
                rejectsIntMutation(model, bytes, offset, value);
            }
        }
        rejectsIntMutation(model, bytes, HEADER_BYTES + Integer.BYTES, 0);
        rejectsIntMutation(model, bytes, 12, 1); // Conv1d architecture with identity kernel.
        byte[] convolution = blend().checkpointBytes(Long.MAX_VALUE);
        rejectsIntMutation(blend(), convolution, 12, 0);
    }

    @Test void geometryAndDeclaredLengthsAreValidatedBeforeDerivedArrays() {
        M3NcmQuantizer model = plain();
        byte[] bytes = model.checkpointBytes(Long.MAX_VALUE);
        int dimensions = dimensionsOffset(bytes);
        for (int offset : new int[]{dimensions, dimensions + 4}) {
            rejectsIntMutation(model, bytes, offset, 0);
            rejectsIntMutation(model, bytes, offset, -1);
            rejectsIntMutation(model, bytes, offset, Integer.MAX_VALUE);
        }
        rejectsIntMutation(model, bytes, dimensions + 8, 3);
        for (int array = 0; array < 6; array++) {
            int count = countOffset(bytes, array);
            rejectsIntMutation(model, bytes, count, -1);
            rejectsIntMutation(model, bytes, count, Integer.MAX_VALUE);
            rejectsIntMutation(model, bytes, count, view(bytes).getInt(count) + 1);
        }
        int levels = countOffset(bytes, 0) + 4;
        rejectsIntMutation(model, bytes, levels, 0);
        rejectsIntMutation(model, bytes, levels + 4, 0);
        rejectsIntMutation(model, bytes, levels + 8, 3);
        int sizes = countOffset(bytes, 1) + 4;
        rejectsIntMutation(model, bytes, sizes, 0);
        rejectsIntMutation(model, bytes, sizes, Integer.MAX_VALUE);
        M3NcmQuantizer convolution = blend();
        byte[] convolutionBytes = convolution.checkpointBytes(Long.MAX_VALUE);
        for (int kernel : new int[]{-1, 0, 2, Integer.MAX_VALUE}) {
            rejectsIntMutation(convolution, convolutionBytes,
                    dimensionsOffset(convolutionBytes) + 8, kernel);
        }
    }

    @Test void nonfiniteWeightsAndInvalidRatiosRefuseBeforeRootVerification() {
        M3NcmQuantizer model = blend();
        byte[] bytes = model.checkpointBytes(Long.MAX_VALUE);
        for (int array = 2; array < 6; array++) {
            for (int bits : new int[]{0x7f800000, 0xff800000, 0x7fc00001, 0x7f800001}) {
                byte[] altered = bytes.clone();
                view(altered).putInt(countOffset(bytes, array) + 4, bits);
                IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                        () -> M3NcmQuantizer.fromCheckpoint(altered, model.codebookRoot(),
                                altered.length, M3NcmQuantizer.Backend.JAVA));
                assertTrue(refusal.getMessage().contains("nonfinite"));
            }
        }
        for (float ratio : new float[]{-0.01f, 1.01f}) {
            rejectsIntMutation(model, bytes, countOffset(bytes, 3) + 4,
                    Float.floatToRawIntBits(ratio));
        }
    }

    @Test void validButDifferentWeightsCannotPassTheExpectedRoot() {
        M3NcmQuantizer model = plain();
        byte[] bytes = model.checkpointBytes(Long.MAX_VALUE);
        byte[] changed = bytes.clone();
        view(changed).putInt(countOffset(bytes, 2) + 4, Float.floatToRawIntBits(0.5f));
        IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                () -> M3NcmQuantizer.fromCheckpoint(changed, model.codebookRoot(),
                        changed.length, M3NcmQuantizer.Backend.JAVA));
        assertTrue(refusal.getMessage().contains("expected codebook root"));
        for (String invalid : new String[]{null, "", "a".repeat(63), "A".repeat(64), "g".repeat(64)}) {
            assertThrows(IllegalArgumentException.class, () -> M3NcmQuantizer.fromCheckpoint(
                    bytes, invalid, bytes.length, M3NcmQuantizer.Backend.JAVA));
        }
        assertThrows(IllegalArgumentException.class, () -> M3NcmQuantizer.fromCheckpoint(
                bytes, "0".repeat(64), bytes.length, M3NcmQuantizer.Backend.JAVA));
        assertThrows(NullPointerException.class, () -> M3NcmQuantizer.fromCheckpoint(
                null, model.codebookRoot(), bytes.length, M3NcmQuantizer.Backend.JAVA));
        assertThrows(NullPointerException.class, () -> M3NcmQuantizer.fromCheckpoint(
                bytes, model.codebookRoot(), bytes.length, null));
    }

    @Test void generatedFiniteModelRoundTripsPreserveTheNumericalBoundary() {
        Random random = new Random(0x4e434d43504cL);
        M3ModelInput source = input();
        for (int trial = 0; trial < 48; trial++) {
            int channels = 1 + random.nextInt(4);
            float[] books = randomValues(random, 7 * channels);
            M3NcmQuantizer model = trial % 2 == 0
                    ? M3NcmQuantizer.of(5, channels, new int[]{1, 3, 5}, new int[]{2, 3, 2}, books)
                    : M3NcmQuantizer.withResidualConvolution(5, channels,
                        new int[]{1, 3, 5}, new int[]{2, 3, 2}, books, 3,
                        new float[]{0.25f, 0.5f, 0.75f},
                        randomValues(random, 9 * channels * channels),
                        randomValues(random, 3 * channels), M3NcmQuantizer.Backend.JAVA);
            float[] latents = randomValues(random, 5 * channels);
            byte[] bytes = model.checkpointBytes(Long.MAX_VALUE);
            M3NcmQuantizer restored = M3NcmQuantizer.fromCheckpoint(bytes,
                    model.codebookRoot(), bytes.length, M3NcmQuantizer.Backend.JAVA);
            M3NcmCodes codes = model.encode(source, latents);
            assertEquals(codes, restored.encode(source, latents));
            assertArrayEquals(model.reconstruct(codes), restored.reconstruct(codes));
            assertArrayEquals(model.nextConceptInputs(codes), restored.nextConceptInputs(codes));
            assertArrayEquals(bytes, restored.checkpointBytes(bytes.length));
        }
    }

    private static float[] randomValues(Random random, int count) {
        float[] values = new float[count];
        for (int i = 0; i < count; i++) values[i] = (random.nextInt(33) - 16) / 8.0f;
        return values;
    }
}
