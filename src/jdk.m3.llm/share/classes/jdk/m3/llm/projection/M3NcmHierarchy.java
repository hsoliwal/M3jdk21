/*
 * SPDX-License-Identifier: MIT
 * Residual hierarchy operations derived from HazyResearch/ncm, models/quant.py,
 * revision c70274273f489abc73832b2b98584a8483b6b203.
 * https://github.com/HazyResearch/ncm
 *
 * MIT License
 *
 * Copyright (c) 2025 hermann kumbong
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package jdk.m3.llm.projection;

import jdk.m3.llm.projection.M3NcmNativeOps.Mode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

import static jdk.m3.llm.projection.M3NcmOps.*;

/**
 * Immutable numerical residual codec for AREA_LINEAR_RVQ_V1.
 *
 * <p>Each level owns a separate vocabulary of C-channel FP32 vectors. Codes are
 * packed [batch,sum(levelLengths)] in level order, with level-local IDs.
 * Latents and reconstructions are [batch,fullLength,channels]. Model parameters
 * are copied defensively; returned arrays own their storage.</p>
 *
 * <p>This profile uses adaptive area reduction, half-pixel linear upsampling,
 * and optional supplied 3-wide Phi residual filters. It does not support the
 * pretrained donor's learned down/up-sampling cascades or transformer inference.
 * Encoding/decoding latent vectors is generally lossy. Code IDs are model-local
 * derived data and cannot replace canonical M3String lexical identities.</p>
 *
 * <p>Construction budgets cover copying, validation, and fingerprint work.
 * Operational overloads take independent per-call budgets; their ledgers cover
 * the entire multilevel call, including transient arrays. Cancellation is
 * checked during validation, between levels, and between batches/rows.</p>
 *
 * <p>Existing overloads always execute in Java without initializing the native
 * loader. Mode-aware overloads select their primitive backend once per call;
 * gather and residual accumulation remain Java. The selected backend shares
 * the same cumulative ledger and does not change the model fingerprint.</p>
 */
public final class M3NcmHierarchy {
    private final String modelVersion;
    private final int channels;
    private final int[] levels;
    private final int[] offsets;
    private final float[][] codebooks;
    private final float[][] weights;
    private final float[][] biases;
    private final float[] ratios;
    private final String fingerprint;

    public M3NcmHierarchy(String modelVersion, int channels,
                               int[] levelLengths, float[][] codebooks) {
        this(modelVersion, channels, levelLengths, codebooks, null, null, null, DEFAULT_BUDGET);
    }

    /**
     * Null transform arrays select identity transforms at every level. Otherwise
     * all three arrays have one entry per level; a null weight/bias pair with
     * ratio zero selects identity at that level. Nonidentity weights have shape
     * [channels,channels,3], biases [channels], and ratios are in [0,1].
     */
    public M3NcmHierarchy(String modelVersion, int channels, int[] levelLengths,
                               float[][] codebooks, float[][] phiWeights,
                               float[][] phiBiases, float[] phiRatios, Budget budget) {
        require(modelVersion, "modelVersion");
        if (modelVersion.isEmpty() || modelVersion.length() > 1024 || modelVersion.isBlank())
            throw new IllegalArgumentException("modelVersion must contain 1..1024 nonblank characters");
        positive(channels, "channels");
        require(levelLengths, "levelLengths"); require(codebooks, "codebooks");
        positive(levelLengths.length, "level count");
        if (codebooks.length != levelLengths.length)
            throw new IllegalArgumentException("one codebook is required per level");
        int count = levelLengths.length;
        require(budget, "budget");
        boolean transforms = phiWeights != null || phiBiases != null || phiRatios != null;
        if (transforms) {
            require(phiWeights, "phiWeights"); require(phiBiases, "phiBiases"); require(phiRatios, "phiRatios");
            if (phiWeights.length != count || phiBiases.length != count || phiRatios.length != count)
                throw new IllegalArgumentException("transform arrays must have one entry per level");
        }
        long storage = add(mul(6, count), 1);
        checkConstructionBudget(budget, storage, modelVersion.length());
        long totalCodes = 0;
        int previous = 0;
        for (int k = 0; k < count; k++) {
            int length = levelLengths[k];
            if (length <= previous) throw new IllegalArgumentException("level lengths must be positive and strictly increasing");
            tensorSize(1, length, channels);
            previous = length;
            totalCodes = add(totalCodes, length);
            arraySize(totalCodes);
            rows(codebooks[k], channels, "codebook");
            storage = add(storage, codebooks[k].length);
            if (transforms) {
                finite(phiRatios[k]);
                if (phiRatios[k] < 0 || phiRatios[k] > 1)
                    throw new IllegalArgumentException("Phi ratio must be in [0,1]");
                if (phiWeights[k] == null || phiBiases[k] == null) {
                    if (phiWeights[k] != null || phiBiases[k] != null || phiRatios[k] != 0)
                        throw new IllegalArgumentException("identity Phi requires null weights/bias and ratio zero");
                } else {
                    length(phiWeights[k], arraySize(mul(mul(channels, channels), 3)), "phi weights");
                    length(phiBiases[k], channels, "phi bias");
                    storage = add(storage, add(phiWeights[k].length, channels));
                }
            }
            checkConstructionBudget(budget, storage, modelVersion.length());
        }
        Work work = new Work(budget, Cancellation.NONE);
        // Fixed overhead covers digest/hex storage and bounded metadata strings.
        work.consume(add(storage, 256), add(mul(storage, 8), add(mul(modelVersion.length(), 8), 4096)));
        this.modelVersion = modelVersion;
        this.channels = channels;
        this.levels = levelLengths.clone();
        this.offsets = new int[count + 1];
        this.codebooks = new float[count][];
        this.weights = new float[count][];
        this.biases = new float[count][];
        this.ratios = transforms ? phiRatios.clone() : new float[count];
        for (int k = 0; k < count; k++) {
            offsets[k + 1] = arraySize(add(offsets[k], levels[k]));
            this.codebooks[k] = codebooks[k].clone();
            checkFinite(this.codebooks[k], work.cancellation);
            if (transforms && phiWeights[k] != null) {
                weights[k] = phiWeights[k].clone();
                biases[k] = phiBiases[k].clone();
                checkFinite(weights[k], work.cancellation); checkFinite(biases[k], work.cancellation);
            }
        }
        fingerprint = computeFingerprint();
    }

    public String profile() { return PROFILE; }
    public String modelVersion() { return modelVersion; }
    public int channels() { return channels; }
    public int levelCount() { return levels.length; }
    public int fullLength() { return levels[levels.length - 1]; }
    public int totalCodes() { return offsets[levels.length]; }
    public int vocabularySize(int level) { checkLevel(level); return codebooks[level].length / channels; }
    public int[] levelLengths() { return levels.clone(); }
    public int[] prefixOffsets() { return offsets.clone(); }
    public String fingerprint() { return fingerprint; }

    public float[] codebook(int level) {
        checkLevel(level);
        DEFAULT_BUDGET.check(codebooks[level].length, codebooks[level].length);
        return codebooks[level].clone();
    }

    public int[] encode(float[] latents, int batch) {
        return encode(latents, batch, DEFAULT_BUDGET, Cancellation.NONE);
    }

    /** Produces all level-local codes for each batch member without modifying latents. */
    public int[] encode(float[] latents, int batch, Budget budget, Cancellation cancellation) {
        return encode(latents, batch, budget, cancellation, Mode.JAVA);
    }

    public int[] encode(float[] latents, int batch, Mode mode) {
        return encode(latents, batch, DEFAULT_BUDGET, Cancellation.NONE, mode);
    }

    /** Selects the primitive backend once; controls apply to the entire hierarchy call. */
    public int[] encode(float[] latents, int batch, Budget budget, Cancellation cancellation, Mode mode) {
        Work work = new Work(budget, cancellation);
        Mode selected = selectMode(mode, work);
        int full = fullLength();
        int inputCount = tensorSize(batch, full, channels);
        length(latents, inputCount, "latents");
        int codeCount = arraySize(mul(batch, totalCodes()));
        work.consume(add(inputCount, codeCount), add(mul(3, inputCount), codeCount));
        float[] residual = latents.clone();
        checkFinite(residual, cancellation);
        int[] result = new int[codeCount];
        for (int k = 0; k < levels.length; k++) {
            cancellation.checkpoint();
            float[] rows = k == levels.length - 1 ? residual
                    : resizeArea(residual, batch, full, channels, levels[k], work, selected);
            int[] local = findNearest(rows, channels, codebooks[k], work, selected);
            work.consume(0, local.length);
            for (int b = 0; b < batch; b++) {
                cancellation.checkpoint();
                System.arraycopy(local, b * levels[k], result,
                        b * totalCodes() + offsets[k], levels[k]);
            }
            float[] contribution = contribution(local, batch, levels[k], 0, k, work, selected);
            work.consume(0, mul(2, inputCount));
            for (int b = 0; b < batch; b++) {
                cancellation.checkpoint();
                int start = b * full * channels;
                int end = start + full * channels;
                for (int i = start; i < end; i++) residual[i] = finite(residual[i] - contribution[i]);
            }
        }
        return result;
    }

    public float[] decode(int[] flatCodes, int batch) {
        return decode(flatCodes, batch, DEFAULT_BUDGET, Cancellation.NONE);
    }

    public float[] decode(int[] flatCodes, int batch, Budget budget, Cancellation cancellation) {
        return decode(flatCodes, batch, budget, cancellation, Mode.JAVA);
    }

    public float[] decode(int[] flatCodes, int batch, Mode mode) {
        return decode(flatCodes, batch, DEFAULT_BUDGET, Cancellation.NONE, mode);
    }

    public float[] decode(int[] flatCodes, int batch, Budget budget, Cancellation cancellation, Mode mode) {
        Work work = new Work(budget, cancellation);
        return reconstruct(flatCodes, batch, levels.length, work, selectMode(mode, work));
    }

    public float[] decodePrefix(int[] prefixCodes, int batch, int completedLevels) {
        return decodePrefix(prefixCodes, batch, completedLevels, DEFAULT_BUDGET, Cancellation.NONE);
    }

    /**
     * Prefix codes are packed per batch with width prefixOffsets[completedLevels].
     * The result has the full latent length even for an incomplete hierarchy.
     */
    public float[] decodePrefix(int[] prefixCodes, int batch, int completedLevels,
                                Budget budget, Cancellation cancellation) {
        return decodePrefix(prefixCodes, batch, completedLevels, budget, cancellation, Mode.JAVA);
    }

    public float[] decodePrefix(int[] prefixCodes, int batch, int completedLevels, Mode mode) {
        return decodePrefix(prefixCodes, batch, completedLevels, DEFAULT_BUDGET, Cancellation.NONE, mode);
    }

    public float[] decodePrefix(int[] prefixCodes, int batch, int completedLevels,
                                Budget budget, Cancellation cancellation, Mode mode) {
        Work work = new Work(budget, cancellation);
        return reconstruct(prefixCodes, batch, completedLevels, work, selectMode(mode, work));
    }

    public float[] nextCondition(int[] prefixCodes, int batch, int completedLevels) {
        return nextCondition(prefixCodes, batch, completedLevels, DEFAULT_BUDGET, Cancellation.NONE);
    }

    /** Returns [batch,levelLengths[completedLevels],channels] for the next level. */
    public float[] nextCondition(int[] prefixCodes, int batch, int completedLevels,
                                 Budget budget, Cancellation cancellation) {
        return nextCondition(prefixCodes, batch, completedLevels, budget, cancellation, Mode.JAVA);
    }

    public float[] nextCondition(int[] prefixCodes, int batch, int completedLevels, Mode mode) {
        return nextCondition(prefixCodes, batch, completedLevels, DEFAULT_BUDGET, Cancellation.NONE, mode);
    }

    public float[] nextCondition(int[] prefixCodes, int batch, int completedLevels,
                                 Budget budget, Cancellation cancellation, Mode mode) {
        if (completedLevels < 1 || completedLevels >= levels.length)
            throw new IllegalArgumentException("nextCondition requires a nonempty, incomplete prefix");
        Work work = new Work(budget, cancellation);
        Mode selected = selectMode(mode, work);
        float[] cumulative = reconstruct(prefixCodes, batch, completedLevels, work, selected);
        return resizeArea(cumulative, batch, fullLength(), channels, levels[completedLevels], work, selected);
    }

    private float[] reconstruct(int[] sourceCodes, int batch, int completedLevels, Work work, Mode selected) {
        if (completedLevels < 1 || completedLevels > levels.length)
            throw new IllegalArgumentException("invalid completedLevels");
        positive(batch, "batch"); require(sourceCodes, "codes");
        int stride = offsets[completedLevels];
        int expectedCodes = arraySize(mul(batch, stride));
        if (sourceCodes.length != expectedCodes)
            throw new IllegalArgumentException("packed codes length does not match prefix/batch shape");
        int outputCount = tensorSize(batch, fullLength(), channels);
        work.consume(add(expectedCodes, outputCount), add(mul(2, expectedCodes), outputCount));
        int[] codes = sourceCodes.clone();
        for (int k = 0; k < completedLevels; k++) {
            work.cancellation.checkpoint();
            int vocabulary = codebooks[k].length / channels;
            for (int b = 0; b < batch; b++) {
                work.cancellation.checkpoint();
                for (int t = 0; t < levels[k]; t++) {
                    int code = codes[b * stride + offsets[k] + t];
                    if (code < 0 || code >= vocabulary)
                        throw new IllegalArgumentException("code ID is outside its level-local vocabulary");
                }
            }
        }
        float[] result = new float[outputCount];
        for (int k = 0; k < completedLevels; k++) {
            work.cancellation.checkpoint();
            float[] contribution = contribution(codes, batch, stride, offsets[k], k, work, selected);
            work.consume(0, mul(2, outputCount));
            for (int b = 0; b < batch; b++) {
                work.cancellation.checkpoint();
                int start = b * fullLength() * channels;
                int end = start + fullLength() * channels;
                for (int i = start; i < end; i++) result[i] = finite(result[i] + contribution[i]);
            }
        }
        return result;
    }

    private float[] contribution(int[] codes, int batch, int codeStride,
                                  int codeOffset, int level, Work work, Mode selected) {
        int localLength = levels[level];
        int count = tensorSize(batch, localLength, channels);
        work.consume(count, mul(mul(batch, localLength), add(channels, 1)));
        float[] local = new float[count];
        float[] book = codebooks[level];
        for (int b = 0; b < batch; b++) {
            work.cancellation.checkpoint();
            for (int t = 0; t < localLength; t++) {
                int entry = codes[b * codeStride + codeOffset + t] * channels;
                System.arraycopy(book, entry, local, (b * localLength + t) * channels, channels);
            }
        }
        float[] full = level == levels.length - 1 ? local
                : resizeLinear(local, batch, localLength, channels, fullLength(), work, selected);
        return weights[level] == null ? full
                : applyPhi(full, batch, fullLength(), channels, weights[level], biases[level], ratios[level], work, selected);
    }

    private static Mode selectMode(Mode mode, Work work) {
        Objects.requireNonNull(mode, "mode");
        // Do not touch NativeOps in the default path: even class initialization may load JNI.
        return mode == Mode.JAVA ? Mode.JAVA
                : M3NcmNativeOps.resolveMode(mode, work.budget, work.cancellation);
    }

    private static int[] findNearest(float[] rows, int channels, float[] codebook,
                                     Work work, Mode selected) {
        return selected == Mode.JAVA ? nearest(rows, channels, codebook, work)
                : M3NcmNativeOps.nearest(rows, channels, codebook, selected, work);
    }

    private static float[] resizeArea(float[] values, int batch, int fromLength,
                                      int channels, int toLength, Work work, Mode selected) {
        return selected == Mode.JAVA ? areaResize(values, batch, fromLength, channels, toLength, work)
                : M3NcmNativeOps.areaResize(values, batch, fromLength, channels, toLength, selected, work);
    }

    private static float[] resizeLinear(float[] values, int batch, int fromLength,
                                        int channels, int toLength, Work work, Mode selected) {
        return selected == Mode.JAVA ? linearResize(values, batch, fromLength, channels, toLength, work)
                : M3NcmNativeOps.linearResize(values, batch, fromLength, channels, toLength, selected, work);
    }

    private static float[] applyPhi(float[] values, int batch, int length, int channels,
                                    float[] weights, float[] bias, float ratio, Work work, Mode selected) {
        return selected == Mode.JAVA ? phi(values, batch, length, channels, weights, bias, ratio, work)
                : M3NcmNativeOps.phi(values, batch, length, channels, weights, bias, ratio, selected, work);
    }

    private void checkLevel(int level) {
        if (level < 0 || level >= levels.length) throw new IllegalArgumentException("invalid level");
    }

    /**
     * Domain-separated SHA-256, big-endian 32-bit fields, UTF-16 code units,
     * explicit lengths/presence tags, and raw IEEE-754 float bits. Budgets and
     * cancellation are execution policies and intentionally do not enter it.
     */
    private String computeFingerprint() {
        final MessageDigest digest;
        try { digest = MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 unavailable", e); }
        putString(digest, "M3_NCM_HIERARCHY_V1");
        putString(digest, PROFILE);
        putString(digest, DONOR_REVISION);
        putString(digest, modelVersion);
        putInt(digest, channels); putInt(digest, levels.length);
        for (int k = 0; k < levels.length; k++) {
            putInt(digest, levels[k]); putFloats(digest, codebooks[k]);
            putInt(digest, weights[k] == null ? 0 : 1);
            putInt(digest, Float.floatToRawIntBits(ratios[k]));
            if (weights[k] != null) { putFloats(digest, weights[k]); putFloats(digest, biases[k]); }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void putFloats(MessageDigest digest, float[] values) {
        putInt(digest, values.length);
        for (float value : values) putInt(digest, Float.floatToRawIntBits(value));
    }

    private static void checkConstructionBudget(Budget budget, long storage, int versionLength) {
        budget.check(add(storage, 256), add(mul(storage, 8), add(mul(versionLength, 8), 4096)));
    }

    private static void putString(MessageDigest digest, String value) {
        putInt(digest, value.length());
        for (int i = 0; i < value.length(); i++) putInt(digest, value.charAt(i));
    }

    private static void putInt(MessageDigest digest, int value) {
        digest.update((byte) (value >>> 24)); digest.update((byte) (value >>> 16));
        digest.update((byte) (value >>> 8)); digest.update((byte) value);
    }
}
