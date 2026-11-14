/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-FileCopyrightText: 2025 hermann kumbong
 * SPDX-License-Identifier: Apache-2.0 AND MIT
 * Modified 2026-11-14: Java scalar adaptation with immutable primitive storage,
 * explicit numerical semantics, source namespaces and optional validated JNI.
 * Completion: bounded frozen-model checkpoints and next-concept input reconstruction.
 * Donor: HazyResearch/ncm@c70274273f489abc73832b2b98584a8483b6b203,
 * models/quant.py. Retain src/main/native/NCM_DONOR_NOTICE.md with distributions.
 */
package jdk.m3.llm.model;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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

    // Portable envelope; the body is exactly the existing codebookRoot preimage.
    private static final long CHECKPOINT_MAGIC = 0x4d334e434d435000L; // M3NCMCP + NUL
    private static final int CHECKPOINT_VERSION = 1;
    private static final int CHECKPOINT_HEADER_BYTES = 20;
    private static final int CHECKPOINT_BODY_FIXED_BYTES = 40;
    private static final int IDENTITY_RESIDUAL = 0;
    private static final int BLEND_CONVOLUTION = 1;

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

    /**
     * Encodes this frozen model in the portable M3 NCM checkpoint version 1 format.
     * The body contains the numerical semantics, dimensions, codebooks and residual
     * transform, with raw float bits preserved. Backend choice is not serialized.
     * This is a model-specific payload for existing artifact/checkpoint stores, not
     * a PyTorch or Safetensors importer. Only identity and blend Conv1d are supported.
     *
     * <p>The nonnegative budget bounds the complete encoded byte array, including
     * its header. Admission precedes allocation. This method allocates one result
     * byte array; object headers and fixed-size digest/encoding state are excluded.
     */
    public byte[] checkpointBytes(long maxBytes) {
        long payloadBytes = 4L * (levels.length + (long) sizes.length + books.length
                + (long) ratios.length + weights.length + biases.length);
        long encodedBytes = CHECKPOINT_HEADER_BYTES + CHECKPOINT_BODY_FIXED_BYTES
                + (long) SEMANTICS.length() + payloadBytes;
        if (maxBytes < 0 || encodedBytes > maxBytes || encodedBytes > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("checkpoint exceeds the supplied byte budget or array limit");
        }
        byte[] result = new byte[(int) encodedBytes];
        ByteBuffer output = ByteBuffer.wrap(result).order(ByteOrder.BIG_ENDIAN);
        output.putLong(CHECKPOINT_MAGIC).putInt(CHECKPOINT_VERSION);
        output.putInt(kernelSize == 0 ? IDENTITY_RESIDUAL : BLEND_CONVOLUTION);
        output.putInt(result.length - CHECKPOINT_HEADER_BYTES);
        output.putInt(SEMANTICS.length());
        for (int i = 0; i < SEMANTICS.length(); i++) output.put((byte) SEMANTICS.charAt(i));
        output.putInt(fullLength).putInt(channels).putInt(kernelSize);
        putInts(output, levels);
        putInts(output, sizes);
        putFloats(output, books);
        putFloats(output, ratios);
        putFloats(output, weights);
        putFloats(output, biases);
        return result;
    }

    /**
     * Restores a frozen model only when the entire bounded payload validates and
     * its semantic SHA-256 equals the supplied lowercase codebook root. Version 1
     * accepts identity or blend Conv1d only; other architectures/semantics refuse.
     * No library is loaded, inference performed or filesystem state changed.
     *
     * <p>The budget bounds encoded bytes, not heap usage. The input is snapshotted
     * after budget admission. Full geometry, finite values, exact EOF and the root
     * are checked before model-array allocation. If E is encoded bytes, P is the
     * six arrays' payload bytes and N is the level count, additional peak primitive
     * payload is bounded by E + 2P + 8(N+1): snapshot, decoded arrays, constructor
     * copies and two offset arrays. This excludes the caller's input, object headers
     * and fixed-size state. Caller mutation during the snapshot is unsupported.
     */
    public static M3NcmQuantizer fromCheckpoint(
            byte[] checkpoint, String expectedCodebookRoot, long maxBytes, Backend backend) {
        Objects.requireNonNull(checkpoint, "checkpoint");
        Objects.requireNonNull(backend, "backend");
        if (expectedCodebookRoot == null || !expectedCodebookRoot.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("expectedCodebookRoot must be a lowercase SHA-256");
        }
        if (maxBytes < 0 || checkpoint.length > maxBytes
                || checkpoint.length < CHECKPOINT_HEADER_BYTES) {
            throw new IllegalArgumentException("checkpoint exceeds budget or is shorter than its header");
        }
        byte[] snapshot = checkpoint.clone();
        ByteBuffer input = ByteBuffer.wrap(snapshot).order(ByteOrder.BIG_ENDIAN);
        try {
            validateCheckpoint(input);
        } catch (BufferUnderflowException truncated) {
            throw new IllegalArgumentException("truncated checkpoint", truncated);
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(snapshot, CHECKPOINT_HEADER_BYTES,
                    snapshot.length - CHECKPOINT_HEADER_BYTES);
            if (!HexFormat.of().formatHex(digest.digest()).equals(expectedCodebookRoot)) {
                throw new IllegalArgumentException("checkpoint does not match expected codebook root");
            }
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError("SHA-256 is required by Java", impossible);
        }
        input.position(CHECKPOINT_HEADER_BYTES + Integer.BYTES + SEMANTICS.length());
        int fullLength = input.getInt();
        int channels = input.getInt();
        int kernelSize = input.getInt();
        int[] levels = readInts(input);
        int[] sizes = readInts(input);
        float[] books = readFloats(input);
        float[] ratios = readFloats(input);
        float[] weights = readFloats(input);
        float[] biases = readFloats(input);
        M3NcmQuantizer result = new M3NcmQuantizer(fullLength, channels, levels,
                sizes, books, kernelSize, ratios, weights, biases, backend);
        if (!result.root.equals(expectedCodebookRoot)) {
            throw new AssertionError("validated checkpoint and model identity disagree");
        }
        return result;
    }

    private static void validateCheckpoint(ByteBuffer input) {
        if (input.getLong() != CHECKPOINT_MAGIC || input.getInt() != CHECKPOINT_VERSION) {
            throw new IllegalArgumentException("unsupported checkpoint magic or version");
        }
        int architecture = input.getInt();
        if (architecture != IDENTITY_RESIDUAL && architecture != BLEND_CONVOLUTION) {
            throw new IllegalArgumentException("unsupported checkpoint residual architecture");
        }
        if (input.getInt() != input.remaining()) {
            throw new IllegalArgumentException("checkpoint body length does not match exact input length");
        }
        if (input.getInt() != SEMANTICS.length()) {
            throw new IllegalArgumentException("unsupported checkpoint numerical semantics");
        }
        for (int i = 0; i < SEMANTICS.length(); i++) {
            if (input.get() != (byte) SEMANTICS.charAt(i)) {
                throw new IllegalArgumentException("unsupported checkpoint numerical semantics");
            }
        }
        int fullLength = input.getInt();
        int channels = input.getInt();
        int kernelSize = input.getInt();
        if (fullLength <= 0 || channels <= 0) {
            throw new IllegalArgumentException("checkpoint requires positive dimensions");
        }
        product(fullLength, channels);
        if ((architecture == IDENTITY_RESIDUAL && kernelSize != 0)
                || (architecture == BLEND_CONVOLUTION
                    && (kernelSize <= 0 || (kernelSize & 1) == 0))) {
            throw new IllegalArgumentException("checkpoint residual architecture and kernel disagree");
        }
        int levelCount = checkedCount(input);
        if (levelCount == 0 || levelCount == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("checkpoint requires nonempty bounded levels");
        }
        int previous = 0;
        int totalCodes = 0;
        for (int i = 0; i < levelCount; i++) {
            int length = input.getInt();
            if (length <= 0 || length < previous || length > fullLength) {
                throw new IllegalArgumentException("invalid checkpoint level length");
            }
            previous = length;
            totalCodes = sum(totalCodes, length);
        }
        if (previous != fullLength || checkedCount(input) != levelCount) {
            throw new IllegalArgumentException("checkpoint levels and codebooks do not match shape");
        }
        int bookValues = 0;
        for (int i = 0; i < levelCount; i++) {
            int size = input.getInt();
            if (size <= 0) throw new IllegalArgumentException("checkpoint codebook must be nonempty");
            bookValues = sum(bookValues, product(size, channels));
        }
        validateFloatArray(input, bookValues, false);
        int ratioCount = kernelSize == 0 ? 0 : levelCount;
        int biasCount = kernelSize == 0 ? 0 : product(levelCount, channels);
        int weightCount = kernelSize == 0 ? 0 : product(biasCount, product(channels, kernelSize));
        validateFloatArray(input, ratioCount, true);
        validateFloatArray(input, weightCount, false);
        validateFloatArray(input, biasCount, false);
        if (input.hasRemaining()) throw new IllegalArgumentException("trailing checkpoint data");
    }

    private static int checkedCount(ByteBuffer input) {
        int count = input.getInt();
        if (count < 0 || count > input.remaining() / Integer.BYTES) {
            throw new IllegalArgumentException("checkpoint array count exceeds bounded payload");
        }
        return count;
    }

    private static void validateFloatArray(ByteBuffer input, int expectedCount, boolean ratio) {
        if (checkedCount(input) != expectedCount) {
            throw new IllegalArgumentException("checkpoint float array does not match shape");
        }
        for (int i = 0; i < expectedCount; i++) {
            float value = Float.intBitsToFloat(input.getInt());
            if (!Float.isFinite(value) || (ratio && (value < 0.0f || value > 1.0f))) {
                throw new IllegalArgumentException("checkpoint contains nonfinite or out-of-range weights");
            }
        }
    }

    private static void putInts(ByteBuffer output, int[] values) {
        output.putInt(values.length);
        for (int value : values) output.putInt(value);
    }

    private static void putFloats(ByteBuffer output, float[] values) {
        output.putInt(values.length);
        for (float value : values) output.putInt(Float.floatToRawIntBits(value));
    }

    // These second-pass readers run only on the fully validated immutable snapshot.
    private static int[] readInts(ByteBuffer input) {
        int[] result = new int[input.getInt()];
        for (int i = 0; i < result.length; i++) result[i] = input.getInt();
        return result;
    }

    private static float[] readFloats(ByteBuffer input) {
        float[] result = new float[input.getInt()];
        for (int i = 0; i < result.length; i++) result[i] = Float.intBitsToFloat(input.getInt());
        return result;
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
        return reconstructValues(codes.copyCodes(), completedLevels);
    }

    /**
     * Returns the cumulative reconstruction after the specified completed levels,
     * area-pooled to the next level's resolution, in position-major/channel-minor
     * order. The count must be in [1, levelCount()-1]; no next scale exists at either
     * endpoint. This is a numerical input to a separate next-concept model, not a
     * prediction from such a model. Returned arrays are caller owned.
     */
    public float[] nextLevelInput(M3NcmCodes codes, int completedLevels) {
        validateCodes(codes);
        if (completedLevels <= 0 || completedLevels >= levels.length) {
            throw new IllegalArgumentException("completedLevels must precede an existing next level");
        }
        return downsample(reconstructValues(codes.copyCodes(), completedLevels),
                levels[completedLevels]);
    }

    /**
     * Concatenates next-level inputs in increasing level order, following the donor's
     * idx_to_ncm_input boundary. Output contains channels * sum(levelLengths[1:])
     * floats; overflow is refused before vector allocation. A one-level model returns
     * an empty array after validating codes. One cumulative pass shares completed work
     * between scales and never evaluates a final-level contribution that is not used.
     */
    public float[] nextConceptInputs(M3NcmCodes codes) {
        validateCodes(codes);
        int count = product(levelOffsets[levels.length] - levels[0], channels);
        if (count == 0) return new float[0];
        float[] result = new float[count];
        int[] values = codes.copyCodes();
        float[] cumulative = new float[vectorLength];
        int outputOffset = 0;
        for (int completed = 1; completed < levels.length; completed++) {
            accumulate(cumulative, values, completed - 1);
            float[] next = downsample(cumulative, levels[completed]);
            System.arraycopy(next, 0, result, outputOffset, next.length);
            outputOffset += next.length;
        }
        return result;
    }

    private float[] reconstructValues(int[] values, int completedLevels) {
        float[] result = new float[vectorLength];
        for (int level = 0; level < completedLevels; level++) accumulate(result, values, level);
        return result;
    }

    private void accumulate(float[] cumulative, int[] values, int level) {
        float[] contribution = contribution(values, level, levelOffsets[level]);
        for (int i = 0; i < cumulative.length; i++) {
            cumulative[i] = finiteResult((double) cumulative[i] + contribution[i]);
        }
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
