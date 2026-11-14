/*
 * SPDX-License-Identifier: MIT
 * Numerical routines derived from HazyResearch/ncm, models/quant.py and
 * models/next_concept.py at c70274273f489abc73832b2b98584a8483b6b203.
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

/**
 * Scalar reference kernels for {@value #PROFILE}. Tensors are contiguous
 * batch/length/channel FP32 arrays; codebooks are vocabulary/channel arrays.
 * Results own their storage; inputs are never modified. Callers must not
 * concurrently modify primitive inputs during a call.
 *
 * <p>Java 21 ordinary float operations are strict. Reductions run left to right
 * without FMA. The donor's expanded distance formula is preserved, without a
 * promise of bitwise agreement with every PyTorch BLAS backend. Rounding can
 * collapse near-boundary distances; equal computed distances choose the lowest
 * ID. Nonfinite inputs and computed values are rejected.</p>
 *
 * <p>These kernels do not implement learned down/up-sampling, text tokenization,
 * transformer inference, or pretrained text generation.</p>
 */
public final class M3NcmOps {
    public static final String PROFILE = "AREA_LINEAR_RVQ_V1";
    public static final String DONOR_REVISION = "c70274273f489abc73832b2b98584a8483b6b203";
    public static final Budget DEFAULT_BUDGET = new Budget(16_777_216L, 1_000_000_000L);
    static final int MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

    private M3NcmOps() { }

    /**
     * Per-call limits. Elements are cumulative newly allocated array slots,
     * including temporary arrays, rather than peak live storage. Work counts
     * conservative scalar operation/validation units. Hierarchy calls share
     * one ledger across levels. Use cancellation for wall-clock deadlines.
     */
    public record Budget(long maxElements, long maxWork) {
        public Budget {
            if (maxElements <= 0 || maxWork <= 0)
                throw new IllegalArgumentException("budget limits must be positive");
        }
        public void check(long elements, long work) {
            if (elements < 0 || work < 0 || elements > maxElements || work > maxWork)
                throw new IllegalArgumentException("NCM allocation/work budget exceeded");
        }
    }

    /** Throw, for example, CancellationException to stop; exceptions propagate. */
    @FunctionalInterface
    public interface Cancellation {
        Cancellation NONE = () -> { };
        void checkpoint();
    }

    public static int[] nearest(float[] rows, int channels, float[] codebook) {
        return nearest(rows, channels, codebook, DEFAULT_BUDGET, Cancellation.NONE);
    }

    public static int[] nearest(float[] rows, int channels, float[] codebook,
                                Budget budget, Cancellation cancellation) {
        return nearest(rows, channels, codebook, new Work(budget, cancellation));
    }

    static int[] nearest(float[] rows, int channels, float[] codebook, Work work) {
        int rowCount = checkNearest(rows, channels, codebook, work);
        int vocabulary = codebook.length / channels;
        float[] norms = codeNorms(codebook, channels, work.cancellation);
        int[] result = new int[rowCount];
        for (int r = 0; r < rowCount; r++) {
            work.cancellation.checkpoint();
            int row = r * channels;
            float rowNorm = 0;
            for (int c = 0; c < channels; c++) rowNorm += rows[row + c] * rows[row + c];
            finite(rowNorm);
            float best = Float.POSITIVE_INFINITY;
            int bestId = 0;
            for (int j = 0; j < vocabulary; j++) {
                float dot = 0;
                int entry = j * channels;
                for (int c = 0; c < channels; c++) dot += rows[row + c] * codebook[entry + c];
                float distance = finite((rowNorm + norms[j]) - 2.0f * dot);
                if (distance < best) { best = distance; bestId = j; }
            }
            result[r] = bestId;
        }
        return result;
    }

    public static float[] areaResize(float[] values, int batch, int fromLength,
                                     int channels, int toLength) {
        return areaResize(values, batch, fromLength, channels, toLength,
                DEFAULT_BUDGET, Cancellation.NONE);
    }

    public static float[] areaResize(float[] values, int batch, int fromLength,
                                     int channels, int toLength, Budget budget,
                                     Cancellation cancellation) {
        return areaResize(values, batch, fromLength, channels, toLength,
                new Work(budget, cancellation));
    }

    static float[] areaResize(float[] values, int batch, int fromLength,
                              int channels, int toLength, Work work) {
        int count = checkResize(values, batch, fromLength, channels, toLength, work);
        float[] result = new float[count];
        for (int b = 0; b < batch; b++) {
            work.cancellation.checkpoint();
            for (int j = 0; j < toLength; j++) {
                int start = (int) ((long) j * fromLength / toLength);
                int end = (int) (((long) (j + 1) * fromLength + toLength - 1) / toLength);
                for (int c = 0; c < channels; c++) {
                    float sum = 0;
                    for (int i = start; i < end; i++)
                        sum += values[(b * fromLength + i) * channels + c];
                    result[(b * toLength + j) * channels + c] = finite(sum / (end - start));
                }
            }
        }
        return result;
    }

    public static float[] linearResize(float[] values, int batch, int fromLength,
                                       int channels, int toLength) {
        return linearResize(values, batch, fromLength, channels, toLength,
                DEFAULT_BUDGET, Cancellation.NONE);
    }

    public static float[] linearResize(float[] values, int batch, int fromLength,
                                       int channels, int toLength, Budget budget,
                                       Cancellation cancellation) {
        return linearResize(values, batch, fromLength, channels, toLength,
                new Work(budget, cancellation));
    }

    static float[] linearResize(float[] values, int batch, int fromLength,
                                int channels, int toLength, Work work) {
        int count = checkResize(values, batch, fromLength, channels, toLength, work);
        float[] result = new float[count];
        float scale = (float) fromLength / toLength;
        for (int b = 0; b < batch; b++) {
            work.cancellation.checkpoint();
            for (int j = 0; j < toLength; j++) {
                float source = Math.max(0.0f, scale * ((float) j + 0.5f) - 0.5f);
                int lo = Math.min((int) source, fromLength - 1);
                int hi = Math.min(lo + 1, fromLength - 1);
                float alpha = Math.max(0.0f, Math.min(source - lo, 1.0f));
                for (int c = 0; c < channels; c++) {
                    float left = values[(b * fromLength + lo) * channels + c];
                    float right = values[(b * fromLength + hi) * channels + c];
                    result[(b * toLength + j) * channels + c] =
                            finite((1.0f - alpha) * left + alpha * right);
                }
            }
        }
        return result;
    }

    /** Weights are [outputChannel,inputChannel,3], bias is [outputChannel]. */
    public static float[] phi(float[] values, int batch, int length, int channels,
                               float[] weights, float[] bias, float ratio) {
        return phi(values, batch, length, channels, weights, bias, ratio,
                DEFAULT_BUDGET, Cancellation.NONE);
    }

    public static float[] phi(float[] values, int batch, int length, int channels,
                               float[] weights, float[] bias, float ratio,
                               Budget budget, Cancellation cancellation) {
        return phi(values, batch, length, channels, weights, bias, ratio,
                new Work(budget, cancellation));
    }

    static float[] phi(float[] values, int batch, int length, int channels,
                       float[] weights, float[] bias, float ratio, Work work) {
        int count = checkPhi(values, batch, length, channels, weights, bias, ratio, work);
        float[] result = new float[count];
        for (int b = 0; b < batch; b++) {
            work.cancellation.checkpoint();
            for (int t = 0; t < length; t++) {
                for (int out = 0; out < channels; out++) {
                    float sum = 0;
                    for (int in = 0; in < channels; in++) {
                        int weight = (out * channels + in) * 3;
                        for (int tap = 0; tap < 3; tap++) {
                            int at = t + tap - 1;
                            if (at >= 0 && at < length)
                                sum += values[(b * length + at) * channels + in] * weights[weight + tap];
                        }
                    }
                    float conv = finite(sum + bias[out]);
                    int at = (b * length + t) * channels + out;
                    result[at] = finite(values[at] * (1.0f - ratio) + conv * ratio);
                }
            }
        }
        return result;
    }

    /** Input rows are already projected into codebook channel space. */
    public static float[] tiedLogits(float[] projectedRows, int channels,
                                      float[] codebook, float logInverseTemperature,
                                      float[] bias) {
        return tiedLogits(projectedRows, channels, codebook, logInverseTemperature,
                bias, DEFAULT_BUDGET, Cancellation.NONE);
    }

    public static float[] tiedLogits(float[] projectedRows, int channels,
                                      float[] codebook, float logInverseTemperature,
                                      float[] bias, Budget budget, Cancellation cancellation) {
        Work work = new Work(budget, cancellation);
        int count = checkTiedLogits(projectedRows, channels, codebook,
                logInverseTemperature, bias, work);
        int vocabulary = codebook.length / channels;
        int rowCount = projectedRows.length / channels;
        float[] norms = codeNorms(codebook, channels, cancellation);
        float normSum = 0;
        for (float norm : norms) normSum += norm;
        float denominator = Math.max(finite(normSum / vocabulary), 1.0e-12f);
        float inverseTemperature = finite((float) StrictMath.exp(logInverseTemperature));
        float[] result = new float[count];
        for (int r = 0; r < rowCount; r++) {
            cancellation.checkpoint();
            for (int j = 0; j < vocabulary; j++) {
                float dot = 0;
                for (int c = 0; c < channels; c++)
                    dot += projectedRows[r * channels + c] * codebook[j * channels + c];
                float value = finite(((2.0f * dot - norms[j]) / denominator) * inverseTemperature);
                result[r * vocabulary + j] = bias == null ? value : finite(value + bias[j]);
            }
        }
        return result;
    }

    private static float[] codeNorms(float[] codebook, int channels, Cancellation cancellation) {
        float[] norms = new float[codebook.length / channels];
        for (int j = 0; j < norms.length; j++) {
            cancellation.checkpoint();
            float sum = 0;
            for (int c = 0; c < channels; c++) {
                float value = codebook[j * channels + c];
                sum += value * value;
            }
            norms[j] = finite(sum);
        }
        return norms;
    }

    // Shared with JNI: validate defaults and return output length without doing
    // the kernel. Native code must also reject computed nonfinite values.
    static int checkNearest(float[] rows, int channels, float[] codebook) {
        return checkNearest(rows, channels, codebook, new Work(DEFAULT_BUDGET, Cancellation.NONE));
    }

    private static int checkNearest(float[] rows, int channels, float[] codebook, Work work) {
        int rowCount = rows(rows, channels, "rows");
        int vocabulary = rows(codebook, channels, "codebook");
        long cost = add(mul(4, add(rows.length, codebook.length)),
                mul(mul(rowCount, vocabulary), add(mul(2, channels), 6)));
        work.consume(add(rowCount, vocabulary), cost);
        checkFinite(rows, work.cancellation); checkFinite(codebook, work.cancellation);
        return rowCount;
    }

    static int checkResize(float[] values, int batch, int fromLength, int channels, int toLength) {
        return checkResize(values, batch, fromLength, channels, toLength,
                new Work(DEFAULT_BUDGET, Cancellation.NONE));
    }

    private static int checkResize(float[] values, int batch, int fromLength,
                                    int channels, int toLength, Work work) {
        int input = tensorSize(batch, fromLength, channels);
        int output = tensorSize(batch, toLength, channels);
        length(values, input, "values");
        work.consume(output, add(mul(2, input), mul(12, output)));
        checkFinite(values, work.cancellation);
        return output;
    }

    static int checkPhi(float[] values, int batch, int length, int channels,
                         float[] weights, float[] bias, float ratio) {
        return checkPhi(values, batch, length, channels, weights, bias, ratio,
                new Work(DEFAULT_BUDGET, Cancellation.NONE));
    }

    private static int checkPhi(float[] values, int batch, int length, int channels,
                                 float[] weights, float[] bias, float ratio, Work work) {
        int output = tensorSize(batch, length, channels);
        length(values, output, "values");
        int weightCount = arraySize(mul(mul(channels, channels), 3));
        length(weights, weightCount, "weights"); length(bias, channels, "bias");
        finite(ratio);
        if (ratio < 0 || ratio > 1) throw new IllegalArgumentException("Phi ratio must be in [0,1]");
        long scan = add(add(output, weightCount), channels);
        work.consume(output, add(scan, mul(output, add(mul(6, channels), 6))));
        checkFinite(values, work.cancellation); checkFinite(weights, work.cancellation);
        checkFinite(bias, work.cancellation);
        return output;
    }

    static int checkTiedLogits(float[] projectedRows, int channels, float[] codebook,
                                float logInverseTemperature, float[] bias) {
        return checkTiedLogits(projectedRows, channels, codebook, logInverseTemperature,
                bias, new Work(DEFAULT_BUDGET, Cancellation.NONE));
    }

    private static int checkTiedLogits(float[] projectedRows, int channels, float[] codebook,
                                        float logInverseTemperature, float[] bias, Work work) {
        int rowCount = rows(projectedRows, channels, "projectedRows");
        int vocabulary = rows(codebook, channels, "codebook");
        int output = arraySize(mul(rowCount, vocabulary));
        if (bias != null) length(bias, vocabulary, "bias");
        finite(logInverseTemperature);
        finite((float) StrictMath.exp(logInverseTemperature));
        long cost = add(mul(4, add(projectedRows.length, codebook.length)),
                mul(output, add(mul(2, channels), 10)));
        work.consume(add(output, vocabulary), add(cost, vocabulary));
        checkFinite(projectedRows, work.cancellation); checkFinite(codebook, work.cancellation);
        if (bias != null) checkFinite(bias, work.cancellation);
        return output;
    }

    static void checkFinite(float[] values) { checkFinite(values, Cancellation.NONE); }

    static void checkFinite(float[] values, Cancellation cancellation) {
        require(values, "values");
        cancellation.checkpoint();
        for (int i = 0; i < values.length; i++) {
            if ((i & 65535) == 65535) cancellation.checkpoint();
            finite(values[i]);
        }
    }

    static float finite(float value) {
        if (!Float.isFinite(value))
            throw new IllegalArgumentException("nonfinite NCM input or computed value");
        return value;
    }

    static int rows(float[] values, int channels, String name) {
        require(values, name); positive(channels, "channels");
        if (values.length == 0 || values.length % channels != 0)
            throw new IllegalArgumentException(name + " must contain complete nonempty rows");
        return values.length / channels;
    }

    static void length(float[] values, int expected, String name) {
        require(values, name);
        if (values.length != expected) throw new IllegalArgumentException(name + " length does not match shape");
    }

    static int tensorSize(int batch, int length, int channels) {
        positive(batch, "batch"); positive(length, "length"); positive(channels, "channels");
        return arraySize(mul(mul(batch, length), channels));
    }

    static int arraySize(long value) {
        if (value < 0 || value > MAX_ARRAY_LENGTH)
            throw new IllegalArgumentException("NCM array length overflow");
        return (int) value;
    }

    static void positive(int value, String name) {
        if (value <= 0) throw new IllegalArgumentException(name + " must be positive");
    }

    static void require(Object value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " must not be null");
    }

    static long add(long a, long b) {
        try { return Math.addExact(a, b); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("NCM size/work overflow", e); }
    }

    static long mul(long a, long b) {
        try { return Math.multiplyExact(a, b); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("NCM size/work overflow", e); }
    }

    /** Mutable only within one call; hierarchy levels do not reset the ledger. */
    static final class Work {
        final Budget budget;
        final Cancellation cancellation;
        private long elements;
        private long work;
        Work(Budget budget, Cancellation cancellation) {
            require(budget, "budget"); require(cancellation, "cancellation");
            this.budget = budget; this.cancellation = cancellation;
        }
        void consume(long moreElements, long moreWork) {
            if (moreElements < 0 || moreWork < 0)
                throw new IllegalArgumentException("negative NCM resource charge");
            long nextElements = add(elements, moreElements);
            long nextWork = add(work, moreWork);
            budget.check(nextElements, nextWork);
            cancellation.checkpoint();
            elements = nextElements; work = nextWork;
        }
    }
}
