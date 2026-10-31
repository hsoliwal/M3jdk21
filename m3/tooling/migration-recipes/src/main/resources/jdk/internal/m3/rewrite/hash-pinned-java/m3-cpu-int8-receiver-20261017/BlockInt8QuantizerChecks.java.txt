// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.model.quantize.tests;

import com.m3.tools.model.quantize.BlockInt8Quantizer;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.List;
import java.util.ArrayList;

/**
 * Independent, deterministic Java 21 oracle for bounded signed block INT8.
 * This tests a newly requested lossy contract, not equivalence to float32 APIs.
 */
public final class BlockInt8QuantizerChecks {
    private static long checks;
    private BlockInt8QuantizerChecks() { }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    @FunctionalInterface private interface Action { void apply(); }
    private static void refusal(Action action, String why) {
        checks++;
        try { action.apply(); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("no refusal: " + why);
    }
    private static byte referenceCode(float input, double scale) {
        double rounded = StrictMath.rint((double) input / scale);
        return (byte) Math.max(-127.0, Math.min(127.0, rounded));
    }
    private static void verify(float[] input, int blockSize) {
        float[] source = input.clone();
        var packed = BlockInt8Quantizer.pack(input, blockSize);
        require(Arrays.equals(input, source), "input changed");
        require(packed.length() == input.length, "wrong size");
        var codes = packed.codes();
        var scales = packed.scales();
        float[] restored = BlockInt8Quantizer.unpack(packed);
        require(scales.length == (input.length + (long) blockSize - 1) / blockSize,
                "wrong number of blocks");
        for (int block = 0; block < scales.length; block++) {
            double expectedMax = 0.0;
            int first = block * blockSize;
            int last = Math.min(input.length, first + blockSize);
            for (int i = first; i < last; i++) {
                expectedMax = Math.max(expectedMax, Math.abs((double) input[i]));
            }
            double expectedScale = expectedMax == 0 ? 1.0 : expectedMax / 127.0;
            require(Double.doubleToLongBits(scales[block]) == Double.doubleToLongBits(expectedScale),
                    "independent block scale");
            for (int i = first; i < last; i++) {
                require(codes[i] == referenceCode(input[i], expectedScale), "reference nearest-even code");
                require(codes[i] != -128, "reserved -128 code");
                require(Float.isFinite(restored[i]), "finite dequantization");
                require(Float.floatToIntBits(restored[i]) == Float.floatToIntBits(
                        (float) ((double) codes[i] * expectedScale)), "reconstruction differs");
                double error = Math.abs((double) input[i] - (double) restored[i]);
                double bound = expectedScale * 0.5 + 4 * Math.ulp(input[i]);
                require(error <= bound, "quantization error bound");
            }
        }
        // Defensive accessors must not permit external mutation of packed state.
        byte[] poisonedCodes = packed.codes();
        double[] poisonedScales = packed.scales();
        poisonedCodes[0] = -127;
        poisonedScales[0] = Double.NaN;
        require(Arrays.equals(codes, packed.codes()), "code alias leaked");
        require(Arrays.equals(scales, packed.scales()), "scale alias leaked");
        require(Arrays.equals(restored, BlockInt8Quantizer.unpack(packed)), "unpack became mutable");
    }

    public static void main(String[] args) throws Exception {
        for (int block = 32; block <= 4096; block *= 2) {
            int[] counts = {1, block - 1, block, block + 1, block * 2 + 1};
            for (int length : counts) {
                verify(new float[length], block);
                float[] wave = new float[length];
                for (int i = 0; i < length; i++) wave[i] = (i % 7) - 3.0f;
                verify(wave, block);
            }
        }
        verify(new float[]{Float.MAX_VALUE, Float.MIN_VALUE, -Float.MIN_NORMAL,
                127, -127, 64.5f, -64.5f, -0.0f}, 32);
        verify(new float[]{127, -127, 63.5f, -63.5f, 64.5f, -64.5f,
                0.0f, -0.0f, 1.0f, -1.0f}, 32);
        refusal(() -> BlockInt8Quantizer.pack(null, 32), "null input");
        refusal(() -> BlockInt8Quantizer.pack(new float[0], 32), "empty input");
        refusal(() -> BlockInt8Quantizer.pack(new float[]{1}, 31), "wrong block size");
        refusal(() -> BlockInt8Quantizer.pack(new float[]{1}, 48), "non-power-two block size");
        refusal(() -> BlockInt8Quantizer.pack(new float[]{1}, 8192), "oversized block");
        refusal(() -> BlockInt8Quantizer.pack(new float[]{Float.NaN}, 32), "NaN");
        refusal(() -> BlockInt8Quantizer.pack(new float[]{Float.POSITIVE_INFINITY}, 32), "Infinity");
        refusal(() -> BlockInt8Quantizer.pack(new float[]{Float.NEGATIVE_INFINITY}, 32), "-Infinity");
        refusal(() -> new BlockInt8Quantizer.Packed(new byte[]{-128}, new double[]{1}, 32), "-128 invalid");
        refusal(() -> new BlockInt8Quantizer.Packed(new byte[]{1}, new double[]{0}, 32), "zero scale");
        refusal(() -> new BlockInt8Quantizer.Packed(new byte[]{1}, new double[]{Double.NaN}, 32), "nan scale");
        refusal(() -> new BlockInt8Quantizer.Packed(new byte[]{1}, new double[]{1, 1}, 32), "scale mismatch");
        refusal(() -> new BlockInt8Quantizer.Packed(new byte[]{1}, new double[]{1}, 64 * 1024), "block invalid");
        // Native loading is never automatic, even when a file is present.
        try {
            BlockInt8Quantizer.verifiedNativePack(new float[]{1, 2}, 32);
            throw new AssertionError("native path must not activate without explicit admission");
        } catch (IllegalStateException expected) {
            // A native library never loads implicitly.
        }
        // Pure numeric seed corpus; preserve signed zero, extremes, and partial blocks.
        Random random = new Random(0x4D335155L);
        for (int repeat = 0; repeat < 480; repeat++) {
            int block = 1 << (5 + random.nextInt(8));
            int size = 1 + random.nextInt(1024);
            float[] sample = new float[size];
            for (int i = 0; i < sample.length; i++) {
                float v = (random.nextFloat() * 2.0f - 1.0f) * 3000f;
                sample[i] = i % 97 == 0 ? 0.0f : i % 89 == 0 ? -0.0f : v;
            }
            verify(sample, block);
        }
        try (var pool = Executors.newFixedThreadPool(4)) {
            List<Future<BlockInt8Quantizer.Packed>> jobs = new ArrayList<>();
            for (int i = 0; i < 32; i++) {
                jobs.add(pool.submit(() -> BlockInt8Quantizer.pack(new float[]{-2, 1, 0}, 32)));
            }
            var golden = jobs.getFirst().get();
            for (Future<BlockInt8Quantizer.Packed> job : jobs) {
                var answer = job.get();
                require(Arrays.equals(golden.codes(), answer.codes()), "thread code divergence");
                require(Arrays.equals(golden.scales(), answer.scales()), "thread scale divergence");
            }
        }
        // Wide execution / block-count correctness without a neural checkpoint.
        float[] wide = new float[1_000_003];
        for (int i = 0; i < wide.length; i++) wide[i] = (i % 251) / 251.0f;
        verify(wide, 256);
        System.out.println("M3_QUANTIZE_JAVA_PASS assertions=" + checks
                + " seed=1295200597 block_range=32..4096 lossy=true");
    }
}
