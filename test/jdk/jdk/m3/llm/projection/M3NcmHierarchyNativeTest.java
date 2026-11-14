// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.projection;

import static org.junit.jupiter.api.Assertions.*;

import jdk.m3.llm.projection.M3NcmNativeOps.Mode;
import jdk.m3.llm.projection.M3NcmOps.Budget;
import jdk.m3.llm.projection.M3NcmOps.Cancellation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/** Whole-codec proofs for the declared FP32 profile and its selected primitive backend. */
final class M3NcmHierarchyNativeTest {
    private static final Budget DEFAULT = M3NcmOps.DEFAULT_BUDGET;

    @Test void requestedNativeModeIsObservableAndCannotSilentlyFallback() {
        if (Boolean.getBoolean("ncm.test.native.required")) {
            assertTrue(M3NcmNativeOps.available());
        }
        if (Boolean.getBoolean("ncm.test.controlled.required")) {
            assertTrue(M3NcmNativeOps.controlledAvailable());
        }
        M3NcmHierarchy hierarchy = toy();
        if (!M3NcmNativeOps.available()) {
            assertThrows(UnsupportedOperationException.class,
                    () -> hierarchy.encode(new float[]{0,0,2,2}, 1, Mode.NATIVE_REQUIRED));
            assertThrows(UnsupportedOperationException.class,
                    () -> hierarchy.decode(new int[]{0,1,1,0,2,1}, 1, Mode.NATIVE_REQUIRED));
        }
        Cancellation custom = () -> { };
        if (!M3NcmNativeOps.controlledAvailable()) {
            assertThrows(UnsupportedOperationException.class,
                    () -> hierarchy.encode(new float[]{0,0,2,2}, 1, DEFAULT, custom, Mode.NATIVE_REQUIRED));
            assertArrayEquals(new int[]{0,1,1,0,2,1},
                    hierarchy.encode(new float[]{0,0,2,2}, 1, DEFAULT, custom, Mode.AUTO));
        }
    }

    @Test void completeCodecMatchesIndependentFixtureInEveryAvailableMode() {
        M3NcmHierarchy hierarchy = toy();
        String fingerprint = hierarchy.fingerprint();
        float[] source = {0,0,2,2};
        int[] expected = {0,1,1,0,2,1};
        for (Mode mode : ordinaryModes()) {
            int[] actual = hierarchy.encode(source, 1, mode);
            assertArrayEquals(expected, actual);
            assertBits(source, hierarchy.decode(actual, 1, mode));
            assertBits(new float[]{0,.5f,1.5f,2},
                    hierarchy.decodePrefix(new int[]{0,1}, 1, 1, mode));
            assertBits(new float[]{0,.5f,1.5f,2},
                    hierarchy.nextCondition(new int[]{0,1}, 1, 1, mode));
            actual[0] = -1;
            assertArrayEquals(expected, hierarchy.encode(source, 1, mode));
            assertEquals(fingerprint, hierarchy.fingerprint());
        }
        assertBits(new float[]{0,0,2,2}, source);
        assertArrayEquals(expected, hierarchy.encode(source, 1));
        assertArrayEquals(expected, hierarchy.encode(source, 1, DEFAULT, Cancellation.NONE));
    }

    @Test void packedBatchPrefixesApplyPhiBeforeNextScaleReduction() {
        M3NcmHierarchy hierarchy = new M3NcmHierarchy("prefix-phi", 1,
                new int[]{1,2,4}, new float[][]{{0,2},{-1,0,1},{-.5f,0,.5f}},
                new float[][]{{1,0,-1},null,null}, new float[][]{{.25f},null,null},
                new float[]{.5f,0,0}, DEFAULT);
        for (Mode mode : ordinaryModes()) {
            assertBits(new float[]{.125f,1.125f,1.125f,2.125f,.125f,.125f,.125f,.125f},
                    hierarchy.decodePrefix(new int[]{1,0}, 2, 1, mode));
            assertBits(new float[]{.625f,1.625f,.125f,.125f},
                    hierarchy.nextCondition(new int[]{1,0}, 2, 1, mode));
        }
    }

    @Test void hierarchyKeepsExpandedFp32RoundingInsteadOfSquaredDifferenceSearch() {
        M3NcmHierarchy hierarchy = new M3NcmHierarchy("rounding", 1,
                new int[]{1}, new float[][]{{8193,8192}});
        for (Mode mode : ordinaryModes()) {
            assertArrayEquals(new int[]{0}, hierarchy.encode(new float[]{8192}, 1, mode));
            assertBits(new float[]{8193}, hierarchy.decode(new int[]{0}, 1, mode));
        }
    }

    @Test void randomizedDensePhiCodecsAgreeOnCodesReconstructionAndAllPrefixes() {
        Random random = new Random(0x4e434d48494552L);
        for (int sample = 0; sample < 16; sample++) {
            int channels = 1 + random.nextInt(4);
            int batch = 1 + random.nextInt(3);
            int[] levels = {2,5,7 + random.nextInt(5)};
            float[][] books = new float[3][], weights = new float[3][], biases = new float[3][];
            for (int level = 0; level < levels.length; level++) {
                books[level] = values(random, (3 + level) * channels, 16);
                weights[level] = values(random, channels * channels * 3, 128);
                biases[level] = values(random, channels, 64);
            }
            M3NcmHierarchy hierarchy = new M3NcmHierarchy("random-" + sample, channels,
                    levels, books, weights, biases, new float[]{.25f,.5f,.75f}, DEFAULT);
            float[] source = values(random, batch * hierarchy.fullLength() * channels, 16);
            float[] snapshot = source.clone();
            int[] expected = hierarchy.encode(source, batch);
            float[] reconstructed = hierarchy.decode(expected, batch);
            for (Mode mode : ordinaryModes()) {
                int[] actual = hierarchy.encode(source, batch, mode);
                assertArrayEquals(expected, actual);
                assertBits(reconstructed, hierarchy.decode(actual, batch, mode));
                for (int completed = 1; completed < levels.length; completed++) {
                    int width = hierarchy.prefixOffsets()[completed];
                    int[] prefix = new int[batch * width];
                    for (int row = 0; row < batch; row++)
                        System.arraycopy(expected, row * hierarchy.totalCodes(), prefix, row * width, width);
                    assertBits(hierarchy.decodePrefix(prefix, batch, completed),
                            hierarchy.decodePrefix(prefix, batch, completed, mode));
                    assertBits(hierarchy.nextCondition(prefix, batch, completed),
                            hierarchy.nextCondition(prefix, batch, completed, mode));
                }
            }
            assertBits(snapshot, source);
        }
    }

    @Test void everyModeUsesOneCumulativeLedgerAtExactIndependentToyBoundaries() {
        // Independent count of the existing declared charges: initial snapshot/output,
        // reduction, two searches, local gathers, expansion, copies and residual updates.
        M3NcmHierarchy hierarchy = toy();
        float[] source = {0,0,2,2};
        int[] codes = {0,1,1,0,2,1};
        int[] prefix = {0,1};
        for (Mode mode : ordinaryModes()) {
            assertArrayEquals(codes, hierarchy.encode(source, 1, new Budget(33,308), Cancellation.NONE, mode));
            assertThrows(IllegalArgumentException.class,
                    () -> hierarchy.encode(source, 1, new Budget(32,308), Cancellation.NONE, mode));
            assertThrows(IllegalArgumentException.class,
                    () -> hierarchy.encode(source, 1, new Budget(33,307), Cancellation.NONE, mode));
            assertBits(source, hierarchy.decode(codes, 1, new Budget(20,96), Cancellation.NONE, mode));
            assertThrows(IllegalArgumentException.class,
                    () -> hierarchy.decode(codes, 1, new Budget(19,96), Cancellation.NONE, mode));
            assertThrows(IllegalArgumentException.class,
                    () -> hierarchy.decode(codes, 1, new Budget(20,95), Cancellation.NONE, mode));
            assertBits(new float[]{0,.5f,1.5f,2},
                    hierarchy.decodePrefix(prefix, 1, 1, new Budget(12,72), Cancellation.NONE, mode));
            assertThrows(IllegalArgumentException.class,
                    () -> hierarchy.decodePrefix(prefix, 1, 1, new Budget(12,71), Cancellation.NONE, mode));
            assertBits(new float[]{0,.5f,1.5f,2},
                    hierarchy.nextCondition(prefix, 1, 1, new Budget(16,128), Cancellation.NONE, mode));
            assertThrows(IllegalArgumentException.class,
                    () -> hierarchy.nextCondition(prefix, 1, 1, new Budget(15,128), Cancellation.NONE, mode));
        }
    }

    @Test void largeCustomLimitSelectsControlsWithoutAllocatingALargeFixture() {
        Budget larger = new Budget(DEFAULT.maxElements() + 1, DEFAULT.maxWork() + 1);
        for (Mode mode : controlledModes()) {
            assertArrayEquals(new int[]{0,1,1,0,2,1},
                    toy().encode(new float[]{0,0,2,2}, 1, larger, Cancellation.NONE, mode));
        }
        if (!M3NcmNativeOps.controlledAvailable()) {
            assertThrows(UnsupportedOperationException.class, () -> toy().encode(
                    new float[]{0,0,2,2}, 1, larger, Cancellation.NONE, Mode.NATIVE_REQUIRED));
        }
    }

    @Test void constructorAndFinalTransformOverflowSemanticsRemainUnchanged() {
        M3NcmHierarchy finiteBook = new M3NcmHierarchy("finite-book", 1,
                new int[]{1}, new float[][]{{Float.MAX_VALUE}});
        M3NcmHierarchy zeroRatio = new M3NcmHierarchy("zero-ratio", 1,
                new int[]{1}, new float[][]{{2}}, new float[][]{{0,Float.MAX_VALUE,0}},
                new float[][]{{0}}, new float[]{0}, DEFAULT);
        for (Mode mode : ordinaryModes()) {
            assertBits(new float[]{Float.MAX_VALUE}, finiteBook.decode(new int[]{0}, 1, mode));
            assertThrows(IllegalArgumentException.class,
                    () -> finiteBook.encode(new float[]{0}, 1, mode));
            assertThrows(IllegalArgumentException.class,
                    () -> zeroRatio.decode(new int[]{0}, 1, mode));
            assertThrows(IllegalArgumentException.class,
                    () -> zeroRatio.encode(new float[]{2}, 1, mode));
        }
    }

    @Test void cancellationPropagatesWithoutPublishingPartialArraysAndRecoveryIsClean() {
        float[] book = new float[32];
        for (int i = 0; i < book.length; i++) book[i] = (i - 16) / 16f;
        M3NcmHierarchy hierarchy = new M3NcmHierarchy("cancel", 1,
                new int[]{4,16}, new float[][]{book,book});
        float[] source = new float[32];
        for (int i = 0; i < source.length; i++) source[i] = (i % 9 - 4) / 8f;
        float[] snapshot = source.clone();
        int[] expected = hierarchy.encode(source, 2);
        for (Mode mode : controlledModes()) {
            AtomicInteger checkpoints = new AtomicInteger();
            CancellationException requested = new CancellationException("stop after work began");
            Cancellation late = () -> { if (checkpoints.incrementAndGet() == 16) throw requested; };
            assertSame(requested, assertThrows(CancellationException.class,
                    () -> hierarchy.encode(source, 2, DEFAULT, late, mode)));
            assertTrue(checkpoints.get() >= 16);
            assertBits(snapshot, source);
            assertArrayEquals(expected, hierarchy.encode(source, 2, mode));
            Cancellation stop = () -> { throw requested; };
            assertSame(requested, assertThrows(CancellationException.class,
                    () -> hierarchy.decode(expected, 2, DEFAULT, stop, mode)));
            assertBits(hierarchy.decode(expected, 2), hierarchy.decode(expected, 2, mode));
        }
    }

    @Test void malformedShapesIdsAndControlsRefuseInAllAvailableModes() {
        for (Mode mode : ordinaryModes()) {
            assertThrows(IllegalArgumentException.class,
                    () -> toy().encode(new float[]{0}, 1, mode));
            assertThrows(IllegalArgumentException.class,
                    () -> toy().encode(new float[]{0,0,Float.NaN,0}, 1, mode));
            assertThrows(IllegalArgumentException.class,
                    () -> toy().decode(new int[]{0,1,1,0,3,1}, 1, mode));
            assertThrows(IllegalArgumentException.class,
                    () -> toy().nextCondition(new int[]{0,1}, 1, 0, mode));
            assertThrows(IllegalArgumentException.class,
                    () -> toy().encode(new float[]{0,0,0,0}, 1, null, Cancellation.NONE, mode));
            assertThrows(IllegalArgumentException.class,
                    () -> toy().encode(new float[]{0,0,0,0}, 1, DEFAULT, null, mode));
        }
        assertThrows(NullPointerException.class,
                () -> toy().encode(new float[]{0,0,0,0}, 1, (Mode) null));
    }

    @Test @Timeout(30) void concurrentCallsShareNoMutableLedgerOrResultStorage() throws Exception {
        M3NcmHierarchy hierarchy = toy();
        float[] source = {0,0,2,2};
        for (Mode mode : controlledModes()) {
            try (var executor = Executors.newFixedThreadPool(4)) {
                CountDownLatch start = new CountDownLatch(1);
                ArrayList<Future<Void>> tasks = new ArrayList<>();
                for (int i = 0; i < 24; i++) tasks.add(executor.submit(() -> {
                    start.await();
                    int[] codes = hierarchy.encode(source, 1, new Budget(33,308), Cancellation.NONE, mode);
                    assertArrayEquals(new int[]{0,1,1,0,2,1}, codes);
                    float[] decoded = hierarchy.decode(codes, 1, new Budget(20,96), Cancellation.NONE, mode);
                    assertBits(source, decoded);
                    codes[0] = -1;
                    decoded[0] = Float.NaN;
                    return null;
                }));
                start.countDown();
                for (Future<Void> task : tasks) task.get(10, TimeUnit.SECONDS);
            }
        }
        assertBits(new float[]{0,0,2,2}, source);
    }

    @Test @Timeout(30) void oldJavaOverloadsDoNotInitializeTheNativeLoader(@TempDir Path directory) throws Exception {
        Path output = directory.resolve("java-defaults.log");
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Xlog:class+init=info", "-cp", System.getProperty("java.class.path"),
                JavaDefaultsProbe.class.getName()).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertTrue(process.waitFor(20, TimeUnit.SECONDS), "default Java probe timed out");
            String log = Files.readString(output);
            assertEquals(0, process.exitValue(), log);
            assertTrue(log.contains("JAVA_DEFAULTS_COMPLETE"), log);
            String nativeOwner = M3NcmNativeOps.class.getName().replace('.', '/');
            assertFalse(log.contains("Initializing '" + nativeOwner + "'"), log);
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }

    /** A fresh process makes class initialization observable without a product test hook. */
    public static final class JavaDefaultsProbe {
        private JavaDefaultsProbe() { }
        public static void main(String[] args) {
            M3NcmHierarchy hierarchy = toy();
            int[] codes = hierarchy.encode(new float[]{0,0,2,2}, 1);
            hierarchy.encode(new float[]{0,0,2,2}, 1, DEFAULT, Cancellation.NONE);
            hierarchy.decode(codes, 1);
            hierarchy.decode(codes, 1, DEFAULT, Cancellation.NONE);
            hierarchy.decodePrefix(new int[]{0,1}, 1, 1);
            hierarchy.nextCondition(new int[]{0,1}, 1, 1);
            System.out.println("JAVA_DEFAULTS_COMPLETE");
        }
    }

    private static Mode[] ordinaryModes() {
        return M3NcmNativeOps.available() ? Mode.values() : new Mode[]{Mode.JAVA,Mode.AUTO};
    }
    private static Mode[] controlledModes() {
        return M3NcmNativeOps.controlledAvailable() ? Mode.values() : new Mode[]{Mode.JAVA,Mode.AUTO};
    }
    private static M3NcmHierarchy toy() {
        return new M3NcmHierarchy("toy", 1, new int[]{2,4}, new float[][]{{0,2},{-.5f,0,.5f}});
    }
    private static float[] values(Random random, int count, int divisor) {
        float[] result = new float[count];
        for (int i = 0; i < result.length; i++) result[i] = (random.nextInt(33) - 16) / (float) divisor;
        return result;
    }
    private static void assertBits(float[] expected, float[] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++)
            assertEquals(Float.floatToRawIntBits(expected[i]), Float.floatToRawIntBits(actual[i]), "FP32 slot " + i);
    }
}
