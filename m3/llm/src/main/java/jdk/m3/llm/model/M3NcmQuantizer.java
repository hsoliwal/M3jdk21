/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-FileCopyrightText: 2025 hermann kumbong
 * SPDX-License-Identifier: Apache-2.0 AND MIT
 * Modified 2026-11-14: Java scalar adaptation with immutable primitive storage,
 * explicit numerical semantics, source namespaces and optional validated JNI.
 * Donor: HazyResearch/ncm@c70274273f489abc73832b2b98584a8483b6b203,
 * models/quant.py. Retain native/NCM_DONOR_NOTICE.md with distributions.
 */
package jdk.m3.llm.model;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Immutable multilevel residual vector quantization over supplied latent vectors.
 *
 * <p>This Java scalar adaptation follows the residual-codebook
 * design reviewed in HazyResearch/ncm at commit
 * c70274273f489abc73832b2b98584a8483b6b203. It is not a trained NCM encoder, decoder,
 * transformer or a lossless text codec. Callers supply latents from a separately
 * validated encoder; text identities alone cannot produce those latents.
 *
 * <p>Storage is position-major/channel-minor float32. Each level uses an independent
 * flattened codebook. Downsampling uses floor/ceiling area-pooling bins; upsampling
 * uses half-pixel linear coordinates with edge clamping (align_corners=false).
 * Optional residual Conv1d uses zero padding, an odd kernel, output/input/kernel
 * weight order and (1-ratio)*input + ratio*convolution. Learned resampling and GELU
 * convolution stacks are not represented by this API and must not be silently omitted
 * when importing a checkpoint.
 *
 * <p>Arithmetic is specified independently of PyTorch kernels: double intermediates,
 * ascending accumulation order and float rounding at each produced component.
 * Numerical overflow refuses the operation. Java is the oracle; the optional native
 * kernel accelerates nearest-code search only. No checkpoint parity or speedup is
 * implied by that component boundary. Instances are immutable and thread safe;
 * callers must not mutate input arrays concurrently with an invocation copying them.
 */
public final class M3NcmQuantizer {
    /** AUTO chooses at call entry; errors from an available native kernel propagate. */
    public enum Backend { JAVA, AUTO, NATIVE }

    private static final String SEMANTICS = "M3-NCM-RVQ/1;f32;distance-f64-ordered;"
            + "area-floorceil;linear-halfpixel-edge;conv-same-zero;tie-lowest-code";

    private final int fullLength;
    private final int channels;
    private final int vectorLength;
    private final int[] levels;
    private final int[] sizes;
    private final int[] levelOffsets;
    private final int[] bookOffsets;
    private final float[] books;
    private final int kernelSize;
    private final float[] ratios;
    private final float[] weights;
    private final float[] biases;
    private final Backend backend;
    private final String root;

    private M3NcmQuantizer(int fullLength, int channels, int[] levels, int[] sizes,
                              float[] books, int kernelSize, float[] ratios,
                              float[] weights, float[] biases, Backend backend) {
        this.backend = Objects.requireNonNull(backend, "backend");
        this.levels = Objects.requireNonNull(levels, "levelLengths").clone();
        this.sizes = Objects.requireNonNull(sizes, "codebookSizes").clone();
        this.books = Objects.requireNonNull(books, "codebooks").clone();
        this.ratios = Objects.requireNonNull(ratios, "residualRatios").clone();
        this.weights = Objects.requireNonNull(weights, "convolutionWeights").clone();
        this.biases = Objects.requireNonNull(biases, "convolutionBias").clone();
        if (fullLength <= 0 || channels <= 0 || this.levels.length == 0
                || this.levels.length != this.sizes.length) {
            throw new IllegalArgumentException("positive dimensions and matching nonempty levels required");
        }
        this.fullLength = fullLength;
        this.channels = channels;
        this.vectorLength = product(fullLength, channels);
        this.levelOffsets = new int[this.levels.length + 1];
        this.bookOffsets = new int[this.levels.length + 1];
        int previous = 0;
        for (int i = 0; i < this.levels.length; i++) {
            if (this.levels[i] < previous || this.levels[i] <= 0
                    || this.levels[i] > fullLength || this.sizes[i] <= 0) {
                throw new IllegalArgumentException("invalid level length or codebook size");
            }
            previous = this.levels[i];
            this.levelOffsets[i + 1] = sum(this.levelOffsets[i], this.levels[i]);
            this.bookOffsets[i + 1] = sum(this.bookOffsets[i], product(this.sizes[i], channels));
        }
        if (previous != fullLength || this.bookOffsets[this.levels.length] != this.books.length) {
            throw new IllegalArgumentException("final level must cover full length; codebooks must match shape");
        }
        this.kernelSize = kernelSize;
        if (kernelSize == 0) {
            if (this.ratios.length != 0 || this.weights.length != 0 || this.biases.length != 0) {
                throw new IllegalArgumentException("identity residual transform has no weights");
            }
        } else {
            if (kernelSize < 0 || (kernelSize & 1) == 0
                    || this.ratios.length != this.levels.length
                    || this.biases.length != product(this.levels.length, channels)
                    || this.weights.length != product(product(this.levels.length, channels),
                                                      product(channels, kernelSize))) {
                throw new IllegalArgumentException("invalid residual convolution dimensions");
            }
            for (float ratio : this.ratios) {
                if (!Float.isFinite(ratio) || ratio < 0.0f || ratio > 1.0f) {
                    throw new IllegalArgumentException("residual ratio must be in [0,1]");
                }
            }
        }
        requireFinite(this.books, "codebooks");
        requireFinite(this.weights, "convolutionWeights");
        requireFinite(this.biases, "convolutionBias");
        this.root = computeRoot();
    }

    /** Constructs an identity-residual quantizer using the Java oracle. */
    public static M3NcmQuantizer of(int fullLength, int channels, int[] levelLengths,
                                       int[] codebookSizes, float[] codebooks) {
        return of(fullLength, channels, levelLengths, codebookSizes, codebooks, Backend.JAVA);
    }

    public static M3NcmQuantizer of(int fullLength, int channels, int[] levelLengths,
                                       int[] codebookSizes, float[] codebooks, Backend backend) {
        return new M3NcmQuantizer(fullLength, channels, levelLengths, codebookSizes,
                codebooks, 0, new float[0], new float[0], new float[0], backend);
    }

    /**
     * Constructs frozen per-level residual convolutions. Weights are flattened as
     * [level][output channel][input channel][kernel]; biases as [level][output channel].
     * A fixed finite ratio in [0,1] is required for every level. Copying learned weights
     * into this representation still requires independent encoder/checkpoint validation.
     */
    public static M3NcmQuantizer withResidualConvolution(
            int fullLength, int channels, int[] levelLengths, int[] codebookSizes,
            float[] codebooks, int kernelSize, float[] residualRatios,
            float[] convolutionWeights, float[] convolutionBias, Backend backend) {
        if (kernelSize <= 0) {
            throw new IllegalArgumentException("positive odd convolution kernel required");
        }
        return new M3NcmQuantizer(fullLength, channels, levelLengths, codebookSizes,
                codebooks, kernelSize, residualRatios, convolutionWeights, convolutionBias, backend);
    }

    /** Loads exactly the caller-selected native library; load and ABI failures propagate. */
    public static void loadNative(Path library) {
        M3NcmNative.load(library);
    }

    public static boolean nativeAvailable() {
        return M3NcmNative.available();
    }

    public int fullLength() { return fullLength; }
    public int channels() { return channels; }
    public int levelCount() { return levels.length; }
    public String codebookRoot() { return root; }
    public int[] copyLevelLengths() { return levels.clone(); }
    public int[] copyCodebookSizes() { return sizes.clone(); }

    /** Primitive array payload bytes, excluding object headers, root string and transient work. */
    public long retainedBytes() {
        return 4L * (levels.length + (long) sizes.length + levelOffsets.length + bookOffsets.length
                + books.length + (long) ratios.length + weights.length + biases.length);
    }

    /**
     * Quantizes supplied encoder latents and attaches the caller's exact input namespace.
     * This does not verify that a supplied latent array was produced from that text; the
     * encoder/provenance boundary must establish that relationship before this call.
     */
    public M3NcmCodes encode(M3ModelInput input, float[] latents) {
        Objects.requireNonNull(input, "input");
        float[] residual = Objects.requireNonNull(latents, "latents").clone();
        if (residual.length != vectorLength) {
            throw new IllegalArgumentException("latent length does not match quantizer shape");
        }
        requireFinite(residual, "latents");
        boolean useNative = backend != Backend.JAVA && M3NcmNative.available();
        if (backend == Backend.NATIVE && !useNative) {
            throw new IllegalStateException("NCM native backend has not been explicitly loaded");
        }
        int[] allCodes = new int[levelOffsets[levels.length]];
        float[] reconstruction = new float[vectorLength];
        for (int level = 0; level < levels.length; level++) {
            float[] reduced = downsample(residual, levels[level]);
            int[] selected = useNative
                    ? M3NcmNative.nearest0(reduced, levels[level], channels,
                                              books, bookOffsets[level], sizes[level])
                    : M3NcmNative.nearestJava(reduced, levels[level], channels,
                                                 books, bookOffsets[level], sizes[level]);
            System.arraycopy(selected, 0, allCodes, levelOffsets[level], selected.length);
            float[] contribution = contribution(allCodes, level, levelOffsets[level]);
            for (int i = 0; i < vectorLength; i++) {
                reconstruction[i] = finiteResult((double) reconstruction[i] + contribution[i]);
                residual[i] = finiteResult((double) residual[i] - contribution[i]);
            }
        }
        return M3NcmCodes.of(input, root, levelOffsets, sizes, allCodes);
    }

    public float[] reconstruct(M3NcmCodes codes) {
        return reconstruct(codes, levels.length);
    }

    /** Returns the cumulative latent reconstruction after 0..levelCount completed levels. */
    public float[] reconstruct(M3NcmCodes codes, int completedLevels) {
        validateCodes(codes);
        if (completedLevels < 0 || completedLevels > levels.length) {
            throw new IllegalArgumentException("completedLevels outside quantizer");
        }
        int[] values = codes.copyCodes();
        float[] result = new float[vectorLength];
        for (int level = 0; level < completedLevels; level++) {
            float[] contribution = contribution(values, level, levelOffsets[level]);
            for (int i = 0; i < result.length; i++) {
                result[i] = finiteResult((double) result[i] + contribution[i]);
            }
        }
        return result;
    }

    private void validateCodes(M3NcmCodes codes) {
        Objects.requireNonNull(codes, "codes");
        if (!root.equals(codes.codebookRoot())
                || !Arrays.equals(levelOffsets, codes.copyLevelOffsets())
                || !Arrays.equals(sizes, codes.copyCodebookSizes())) {
            throw new IllegalArgumentException("codes belong to another codebook or shape");
        }
    }

    private float[] downsample(float[] source, int targetLength) {
        if (targetLength == fullLength) {
            return source;
        }
        float[] result = new float[product(targetLength, channels)];
        for (int position = 0; position < targetLength; position++) {
            int first = (int) ((long) position * fullLength / targetLength);
            int end = (int) (((long) (position + 1) * fullLength + targetLength - 1) / targetLength);
            for (int channel = 0; channel < channels; channel++) {
                double total = 0.0;
                for (int input = first; input < end; input++) {
                    total = total + source[input * channels + channel];
                }
                result[position * channels + channel] = finiteResult(total / (end - first));
            }
        }
        return result;
    }

    private float[] contribution(int[] codes, int level, int codeOffset) {
        int length = levels[level];
        float[] expanded = new float[vectorLength];
        for (int position = 0; position < fullLength; position++) {
            double coordinate = Math.max(0.0, (position + 0.5) * length / fullLength - 0.5);
            int left = Math.min((int) coordinate, length - 1);
            int right = Math.min(left + 1, length - 1);
            double fraction = coordinate - left;
            int leftBase = bookOffsets[level] + codes[codeOffset + left] * channels;
            int rightBase = bookOffsets[level] + codes[codeOffset + right] * channels;
            for (int channel = 0; channel < channels; channel++) {
                expanded[position * channels + channel] = fraction == 0.0 || left == right
                        ? books[leftBase + channel]
                        : finiteResult((1.0 - fraction) * books[leftBase + channel]
                                       + fraction * books[rightBase + channel]);
            }
        }
        if (kernelSize == 0 || ratios[level] == 0.0f) {
            return expanded;
        }
        float[] result = new float[vectorLength];
        int padding = kernelSize / 2;
        double ratio = ratios[level];
        for (int position = 0; position < fullLength; position++) {
            for (int out = 0; out < channels; out++) {
                double convolution = biases[level * channels + out];
                for (int in = 0; in < channels; in++) {
                    int weightBase = ((level * channels + out) * channels + in) * kernelSize;
                    for (int kernel = 0; kernel < kernelSize; kernel++) {
                        long input = (long) position + kernel - padding;
                        if (input >= 0 && input < fullLength) {
                            double term = (double) expanded[(int) input * channels + in]
                                    * weights[weightBase + kernel];
                            convolution = convolution + term;
                        }
                    }
                }
                int index = position * channels + out;
                result[index] = finiteResult((1.0 - ratio) * expanded[index] + ratio * convolution);
            }
        }
        return result;
    }

    private String computeRoot() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] semantics = SEMANTICS.getBytes(StandardCharsets.US_ASCII);
            updateInt(digest, semantics.length);
            digest.update(semantics);
            updateInt(digest, fullLength);
            updateInt(digest, channels);
            updateInt(digest, kernelSize);
            updateInts(digest, levels);
            updateInts(digest, sizes);
            updateFloats(digest, books);
            updateFloats(digest, ratios);
            updateFloats(digest, weights);
            updateFloats(digest, biases);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError("SHA-256 is required by Java", impossible);
        }
    }

    private static void updateInts(MessageDigest digest, int[] values) {
        updateInt(digest, values.length);
        for (int value : values) {
            updateInt(digest, value);
        }
    }

    private static void updateFloats(MessageDigest digest, float[] values) {
        updateInt(digest, values.length);
        for (float value : values) {
            updateInt(digest, Float.floatToRawIntBits(value));
        }
    }

    private static void updateInt(MessageDigest digest, int value) {
        digest.update((byte) (value >>> 24));
        digest.update((byte) (value >>> 16));
        digest.update((byte) (value >>> 8));
        digest.update((byte) value);
    }

    private static float finiteResult(double value) {
        float result = (float) value;
        if (!Float.isFinite(result)) {
            throw new ArithmeticException("nonfinite residual quantization result");
        }
        return result;
    }

    private static void requireFinite(float[] values, String name) {
        for (float value : values) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException(name + " contains a nonfinite value");
            }
        }
    }

    private static int product(int a, int b) {
        long value = (long) a * b;
        if (value < 0 || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("quantizer shape overflows a Java array");
        }
        return (int) value;
    }

    private static int sum(int a, int b) {
        long value = (long) a + b;
        if (value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("quantizer offsets overflow a Java array");
        }
        return (int) value;
    }
}
