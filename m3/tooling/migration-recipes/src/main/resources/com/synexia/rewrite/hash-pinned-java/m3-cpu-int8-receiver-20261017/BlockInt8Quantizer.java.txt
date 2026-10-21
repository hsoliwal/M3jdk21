// SPDX-FileCopyrightText: 2026 Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.model.quantize;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Random;

/**
 * Independently packaged, CPU-authoritative symmetric signed INT8 block codec.
 * This is NOT GGUF/Q8_0, a Chrome weight converter, a model trainer, or an
 * implicit optimization of floating-point APIs. It is deliberately lossy.
 *
 * <p>Input and output arrays are defensively owned. Blocks are 32..4096
 * elements, with partial final blocks and one finite positive double scale
 * per block. No external data, GPU or model weights are read implicitly.</p>
 */
public final class BlockInt8Quantizer {
    public static final int MAX_ELEMENTS = 4_194_304;
    private static final long MAX_NATIVE_BYTES = 64L << 20;
    private static final Object NATIVE_LOCK = new Object();
    private static String installedNativePath;
    private static String installedNativeHash;
    private static volatile boolean nativeAdmitted;

    private BlockInt8Quantizer() { }

    /** Immutable published representation, including its exact scale vector. */
    public record Packed(byte[] codes, double[] scales, int blockSize) {
        public Packed {
            requireBlockSize(blockSize);
            Objects.requireNonNull(codes, "codes");
            Objects.requireNonNull(scales, "scales");
            if (codes.length < 1 || codes.length > MAX_ELEMENTS
                    || scales.length != blocks(codes.length, blockSize))
                throw new IllegalArgumentException("packed shape/budget");
            for (double scale : scales)
                if (!(scale > 0.0) || !Double.isFinite(scale)
                        || scale > Float.MAX_VALUE / 127.0)
                    throw new IllegalArgumentException("invalid or float32-overflowing scale");
            for (byte code : codes)
                if (code == Byte.MIN_VALUE)
                    throw new IllegalArgumentException("INT8 -128 reserved for invalid code");
            codes = codes.clone();
            scales = scales.clone();
        }
        @Override public byte[] codes() { return codes.clone(); }
        @Override public double[] scales() { return scales.clone(); }
        public int length() { return codes.length; }
    }

    private static void requireBlockSize(int size) {
        if (size < 32 || size > 4096 || (size & (size - 1)) != 0)
            throw new IllegalArgumentException("block size must be a power of two in [32,4096]");
    }
    private static int blocks(int size, int blockSize) {
        return (int) ((size + (long) blockSize - 1) / blockSize);
    }
    private static void validate(float[] input, int blockSize) {
        requireBlockSize(blockSize);
        if (input == null || input.length == 0 || input.length > MAX_ELEMENTS)
            throw new IllegalArgumentException("input size/budget");
        for (float value : input)
            if (!Float.isFinite(value)) throw new IllegalArgumentException("nonfinite input");
    }

    /**
     * O(N) symmetric per-block absolute-maximum quantization, without bias.
     * scale = max(abs(x))/127, and q = rint(x/scale), clamped to [-127,127].
     * For all-zero blocks, scale is exactly 1 and all codes are zero.
     */
    public static Packed pack(float[] input, int blockSize) {
        validate(input, blockSize);
        byte[] codes = new byte[input.length];
        double[] scales = new double[blocks(input.length, blockSize)];
        for (int start = 0, block = 0; start < input.length; start += blockSize, block++) {
            int end = (int) Math.min(input.length, (long) start + blockSize);
            double maxAbs = 0.0;
            for (int i = start; i < end; i++) maxAbs = Math.max(maxAbs, Math.abs(input[i]));
            double scale = maxAbs == 0.0 ? 1.0 : maxAbs / 127.0;
            scales[block] = scale;
            for (int i = start; i < end; i++) {
                double rounded = Math.rint(input[i] / scale);
                codes[i] = (byte) Math.max(-127.0, Math.min(127.0, rounded));
            }
        }
        return new Packed(codes, scales, blockSize);
    }

    /** Reconstructs an independent finite float32 vector; loss is intentional. */
    public static float[] unpack(Packed packed) {
        Objects.requireNonNull(packed, "packed");
        float[] result = new float[packed.codes.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = (float) (packed.codes[i] * packed.scales[i / packed.blockSize]);
        }
        return result;
    }

    /**
     * Optional pinned JNI candidate. Always returns the independently computed
     * Java answer, and rejects even a one-bit native scale or code mismatch.
     */
    public static Packed verifiedNativePack(float[] input, int blockSize) {
        Packed expected = pack(input, blockSize);
        if (!nativeAdmitted) throw new IllegalStateException("explicit native admission required");
        Object[] nativeResult = packNative(input.clone(), blockSize);
        if (nativeResult == null || nativeResult.length != 2
                || !(nativeResult[0] instanceof byte[] codes)
                || !(nativeResult[1] instanceof double[] scales)
                || !Arrays.equals(codes, expected.codes)
                || scales.length != expected.scales.length)
            throw new IllegalStateException("native quantization shape/code mismatch");
        for (int i = 0; i < scales.length; i++) {
            if (Double.doubleToLongBits(scales[i]) != Double.doubleToLongBits(expected.scales[i]))
                throw new IllegalStateException("native quantization scale mismatch block " + i);
        }
        return expected;
    }

    /** Single SHA-256-pinned native image per process. No automatic loading. */
    public static void admitNative(Path library, String expectedSha256) throws IOException {
        Objects.requireNonNull(library, "library");
        if (!library.isAbsolute() || expectedSha256 == null
                || !expectedSha256.matches("[a-f0-9]{64}"))
            throw new IllegalArgumentException("absolute path and lowercase SHA-256 required");
        Path path = library.normalize();
        for (Path part = path; part != null; part = part.getParent())
            if (Files.isSymbolicLink(part)) throw new IOException("symlink path refused");
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                || Files.size(path) > MAX_NATIVE_BYTES)
            throw new IOException("missing or oversized library");
        synchronized (NATIVE_LOCK) {
            String name = path.toString();
            if (installedNativePath != null && (!installedNativePath.equals(name)
                    || !installedNativeHash.equals(expectedSha256)))
                throw new IOException("native library already pinned to different identity");
            if (!sha256(path).equals(expectedSha256)) throw new IOException("native library SHA mismatch");
            if (installedNativePath == null) {
                installedNativePath = name;
                installedNativeHash = expectedSha256;
                System.load(name);
                if (nativeAbi() != 1 || !sha256(path).equals(expectedSha256))
                    throw new IOException("native ABI/hash postload mismatch; restart process");
                nativeAdmitted = true;
            }
            if (!nativeAdmitted) throw new IOException("native admission failed; restart process");
        }
    }

    private static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream stream = Files.newInputStream(path)) {
                byte[] chunk = new byte[65_536];
                for (int n; (n = stream.read(chunk)) != -1; ) digest.update(chunk, 0, n);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) { throw new AssertionError(exception); }
    }

    private static native int nativeAbi();
    private static native Object[] packNative(float[] input, int blockSize);

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && "--self-test".equals(args[0])) {
            selfTest(false);
            System.out.println("M3_JMOD_TEST_PASS model.quantize");
        } else if (args.length == 3 && "--jni-test".equals(args[0])) {
            admitNative(Path.of(args[1]), args[2]);
            selfTest(true);
            System.out.println("M3_JNI_TEST_PASS model.quantize");
        } else throw new IllegalArgumentException("--self-test | --jni-test absolute_library sha256");
    }

    private static void selfTest(boolean nativeEnabled) {
        Packed zero = pack(new float[65], 32);
        if (zero.scales[0] != 1.0 || zero.scales[2] != 1.0
                || !Arrays.equals(unpack(zero), new float[65]))
            throw new AssertionError("all-zero/partial block");
        Packed fixed = pack(new float[]{-127, 0, 127}, 32);
        if (!Arrays.equals(fixed.codes, new byte[]{-127, 0, 127}))
            throw new AssertionError("basic symmetric quantization");
        Random random = new Random(0x4D3351);
        for (int trial = 0; trial < 128; trial++) {
            int count = 1 + random.nextInt(2048);
            float[] input = new float[count];
            for (int i = 0; i < count; i++) input[i] = random.nextFloat() * 20 - 10;
            int blockSize = 1 << (5 + random.nextInt(8));
            Packed packed = pack(input, blockSize);
            float[] decoded = unpack(packed);
            double[] scale = packed.scales();
            for (int i = 0; i < count; i++) {
                double bound = scale[i / blockSize] * 0.5 + Math.ulp(input[i]) * 2.0;
                if (Math.abs((double) input[i] - decoded[i]) > bound)
                    throw new AssertionError("quantization error bound " + i);
            }
            if (nativeEnabled) verifiedNativePack(input, blockSize);
        }
        if (nativeEnabled) {
            verifiedNativePack(new float[]{Float.MAX_VALUE, Float.MIN_VALUE, -Float.MIN_NORMAL}, 32);
            verifiedNativePack(new float[65], 32);
        }
    }
}
