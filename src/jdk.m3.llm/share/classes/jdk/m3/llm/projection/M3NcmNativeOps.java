// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.projection;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Coarse, bounded JNI batches for the explicit NCM numerical profile.
 * Java remains the semantic oracle. No model, tokenizer, callback or native
 * memory is retained. Callers must not mutate inputs until a call returns.
 *
 * <p>The single native owner is library {@code m3_ncm}, loaded from the
 * explicit {@code m3.ncm.library} path when supplied, otherwise by name.
 * The existing expanded-FP32 and ordered-FP64 numerical profiles remain distinct
 * within that shared owner. The FP32 nearest operation uses its shared kernel.</p>
 *
 * <p>ABI 1 remains available independently of the controlled capability. Custom
 * cancellation or an allocation budget above the legacy ceiling requires the
 * controlled capability; AUTO uses Java when that capability is absent and
 * NATIVE_REQUIRED refuses. Available-native numerical failures propagate.</p>
 *
 * <p>A hierarchy passes one Work ledger through every primitive. Cancellation
 * callbacks run on the calling thread during validation and native norm/row/batch
 * boundaries. A callback exception stops the call and propagates unchanged after
 * native cleanup. Cancellation.NONE has no Java callback from native code.</p>
 *
 * <p>The allocation budget admits cumulative result/scratch dimensions. JNI may
 * copy input arrays and native scratch coexists with the Java result; the budget
 * is not a hard bound on VM-dependent peak native memory.</p>
 */
public final class M3NcmNativeOps {
    /** Backend choice is observable through availability and required-mode refusal. */
    public enum Mode { JAVA, AUTO, NATIVE_REQUIRED }
    private static final boolean AVAILABLE = load();
    private static final boolean CONTROLLED_AVAILABLE = loadControlled();

    private M3NcmNativeOps() { }

    private static boolean load() {
        if (Boolean.getBoolean("m3.llm.ncm.disableNative")) return false;
        try {
            String library = System.getProperty("m3.ncm.library");
            if (library == null) System.loadLibrary("m3_ncm");
            else System.load(Path.of(library).toAbsolutePath().normalize().toString());
            return abiVersionNative() == 1;
        } catch (UnsatisfiedLinkError | SecurityException | InvalidPathException absentFeature) {
            return false;
        }
    }

    private static boolean loadControlled() {
        if (!AVAILABLE) return false;
        try { return controlledAbiVersionNative() == 1; }
        catch (UnsatisfiedLinkError absentFeature) { return false; }
    }

    /** Availability of the original numerical ABI, including older native libraries. */
    public static boolean available() { return AVAILABLE; }
    /** Availability of custom-budget and cooperative-cancellation native entrypoints. */
    public static boolean controlledAvailable() { return CONTROLLED_AVAILABLE; }

    public static int[] nearest(float[] rows, int channels, float[] codebook, Mode mode) {
        return nearest(rows, channels, codebook, mode,
                M3NcmOps.DEFAULT_BUDGET, M3NcmOps.Cancellation.NONE);
    }

    public static int[] nearest(float[] rows, int channels, float[] codebook, Mode mode,
                                M3NcmOps.Budget budget, M3NcmOps.Cancellation cancellation) {
        return nearest(rows, channels, codebook, mode, new M3NcmOps.Work(budget, cancellation));
    }

    static int[] nearest(float[] rows, int channels, float[] codebook, Mode mode, M3NcmOps.Work work) {
        int length = M3NcmOps.checkNearest(rows, channels, codebook, work);
        if (resolveMode(mode, work.budget, work.cancellation) == Mode.JAVA)
            return M3NcmOps.nearestValidated(rows, channels, codebook, work);
        int[] result = CONTROLLED_AVAILABLE
                ? nearestControlledNative(rows, channels, codebook, work.budget.maxElements(), callback(work))
                : nearestNative(rows, channels, codebook);
        work.cancellation.checkpoint();
        if (result == null || result.length != length) throw invalidNative();
        int vocabulary = codebook.length / channels;
        for (int i = 0; i < result.length; i++) {
            if ((i & 65535) == 65535) work.cancellation.checkpoint();
            if (result[i] < 0 || result[i] >= vocabulary) throw invalidNative();
        }
        return result;
    }

    public static float[] areaResize(float[] values, int batch, int fromLength, int channels,
                                     int toLength, Mode mode) {
        return areaResize(values, batch, fromLength, channels, toLength, mode,
                M3NcmOps.DEFAULT_BUDGET, M3NcmOps.Cancellation.NONE);
    }

    public static float[] areaResize(float[] values, int batch, int fromLength, int channels,
                                     int toLength, Mode mode, M3NcmOps.Budget budget,
                                     M3NcmOps.Cancellation cancellation) {
        return areaResize(values, batch, fromLength, channels, toLength, mode,
                new M3NcmOps.Work(budget, cancellation));
    }

    static float[] areaResize(float[] values, int batch, int fromLength, int channels,
                              int toLength, Mode mode, M3NcmOps.Work work) {
        int length = M3NcmOps.checkResize(values, batch, fromLength, channels, toLength, work);
        if (resolveMode(mode, work.budget, work.cancellation) == Mode.JAVA)
            return M3NcmOps.areaResizeValidated(values, batch, fromLength, channels, toLength, work);
        return checked(CONTROLLED_AVAILABLE
                ? resizeControlledNative(values, batch, fromLength, channels, toLength, false,
                        work.budget.maxElements(), callback(work))
                : resizeNative(values, batch, fromLength, channels, toLength, false), length, work);
    }

    public static float[] linearResize(float[] values, int batch, int fromLength, int channels,
                                       int toLength, Mode mode) {
        return linearResize(values, batch, fromLength, channels, toLength, mode,
                M3NcmOps.DEFAULT_BUDGET, M3NcmOps.Cancellation.NONE);
    }

    public static float[] linearResize(float[] values, int batch, int fromLength, int channels,
                                       int toLength, Mode mode, M3NcmOps.Budget budget,
                                       M3NcmOps.Cancellation cancellation) {
        return linearResize(values, batch, fromLength, channels, toLength, mode,
                new M3NcmOps.Work(budget, cancellation));
    }

    static float[] linearResize(float[] values, int batch, int fromLength, int channels,
                                int toLength, Mode mode, M3NcmOps.Work work) {
        int length = M3NcmOps.checkResize(values, batch, fromLength, channels, toLength, work);
        if (resolveMode(mode, work.budget, work.cancellation) == Mode.JAVA)
            return M3NcmOps.linearResizeValidated(values, batch, fromLength, channels, toLength, work);
        return checked(CONTROLLED_AVAILABLE
                ? resizeControlledNative(values, batch, fromLength, channels, toLength, true,
                        work.budget.maxElements(), callback(work))
                : resizeNative(values, batch, fromLength, channels, toLength, true), length, work);
    }

    public static float[] phi(float[] values, int batch, int length, int channels,
                              float[] weights, float[] bias, float ratio, Mode mode) {
        return phi(values, batch, length, channels, weights, bias, ratio, mode,
                M3NcmOps.DEFAULT_BUDGET, M3NcmOps.Cancellation.NONE);
    }

    public static float[] phi(float[] values, int batch, int length, int channels,
                              float[] weights, float[] bias, float ratio, Mode mode,
                              M3NcmOps.Budget budget, M3NcmOps.Cancellation cancellation) {
        return phi(values, batch, length, channels, weights, bias, ratio, mode,
                new M3NcmOps.Work(budget, cancellation));
    }

    static float[] phi(float[] values, int batch, int length, int channels,
                       float[] weights, float[] bias, float ratio, Mode mode, M3NcmOps.Work work) {
        int size = M3NcmOps.checkPhi(values, batch, length, channels, weights, bias, ratio, work);
        if (resolveMode(mode, work.budget, work.cancellation) == Mode.JAVA)
            return M3NcmOps.phiValidated(values, batch, length, channels, weights, bias, ratio, work);
        return checked(CONTROLLED_AVAILABLE
                ? phiControlledNative(values, batch, length, channels, weights, bias, ratio,
                        work.budget.maxElements(), callback(work))
                : phiNative(values, batch, length, channels, weights, bias, ratio), size, work);
    }

    public static float[] tiedLogits(float[] rows, int channels, float[] codebook,
                                     float logInverseTemperature, float[] bias, Mode mode) {
        return tiedLogits(rows, channels, codebook, logInverseTemperature, bias, mode,
                M3NcmOps.DEFAULT_BUDGET, M3NcmOps.Cancellation.NONE);
    }

    public static float[] tiedLogits(float[] rows, int channels, float[] codebook,
                                     float logInverseTemperature, float[] bias, Mode mode,
                                     M3NcmOps.Budget budget, M3NcmOps.Cancellation cancellation) {
        return tiedLogits(rows, channels, codebook, logInverseTemperature, bias, mode,
                new M3NcmOps.Work(budget, cancellation));
    }

    static float[] tiedLogits(float[] rows, int channels, float[] codebook,
                              float logInverseTemperature, float[] bias, Mode mode, M3NcmOps.Work work) {
        int length = M3NcmOps.checkTiedLogits(rows, channels, codebook, logInverseTemperature, bias, work);
        if (resolveMode(mode, work.budget, work.cancellation) == Mode.JAVA)
            return M3NcmOps.tiedLogitsValidated(rows, channels, codebook, logInverseTemperature, bias, work);
        return checked(CONTROLLED_AVAILABLE
                ? tiedLogitsControlledNative(rows, channels, codebook, logInverseTemperature, bias,
                        work.budget.maxElements(), callback(work))
                : tiedLogitsNative(rows, channels, codebook, logInverseTemperature, bias), length, work);
    }

    static Mode resolveMode(Mode mode, M3NcmOps.Budget budget, M3NcmOps.Cancellation cancellation) {
        Objects.requireNonNull(mode, "mode");
        M3NcmOps.require(budget, "budget"); M3NcmOps.require(cancellation, "cancellation");
        if (mode == Mode.JAVA) return Mode.JAVA;
        boolean needsControl = cancellation != M3NcmOps.Cancellation.NONE
                || budget.maxElements() > M3NcmOps.DEFAULT_BUDGET.maxElements();
        boolean supported = AVAILABLE && (!needsControl || CONTROLLED_AVAILABLE);
        if (mode == Mode.NATIVE_REQUIRED && !supported)
            throw new UnsupportedOperationException(needsControl
                    ? "NCM controlled numerical JNI capability 1 is unavailable"
                    : "NCM numerical JNI ABI 1 is unavailable");
        return supported ? Mode.NATIVE_REQUIRED : Mode.JAVA;
    }

    private static M3NcmOps.Cancellation callback(M3NcmOps.Work work) {
        return work.cancellation == M3NcmOps.Cancellation.NONE ? null : work.cancellation;
    }

    private static float[] checked(float[] result, int length, M3NcmOps.Work work) {
        work.cancellation.checkpoint();
        if (result == null || result.length != length) throw invalidNative();
        M3NcmOps.checkFinite(result, work.cancellation);
        return result;
    }

    private static IllegalStateException invalidNative() {
        return new IllegalStateException("NCM JNI returned an invalid result shape or code");
    }

    private static native int abiVersionNative();
    private static native int controlledAbiVersionNative();
    private static native int[] nearestNative(float[] rows, int channels, float[] codebook);
    private static native float[] resizeNative(float[] values, int batch, int fromLength, int channels,
                                               int toLength, boolean linear);
    private static native float[] phiNative(float[] values, int batch, int length, int channels,
                                            float[] weights, float[] bias, float ratio);
    private static native float[] tiedLogitsNative(float[] rows, int channels, float[] codebook,
                                                   float logInverseTemperature, float[] bias);
    private static native int[] nearestControlledNative(float[] rows, int channels, float[] codebook,
                                                         long maxElements, M3NcmOps.Cancellation cancellation);
    private static native float[] resizeControlledNative(float[] values, int batch, int fromLength,
            int channels, int toLength, boolean linear, long maxElements, M3NcmOps.Cancellation cancellation);
    private static native float[] phiControlledNative(float[] values, int batch, int length, int channels,
            float[] weights, float[] bias, float ratio, long maxElements, M3NcmOps.Cancellation cancellation);
    private static native float[] tiedLogitsControlledNative(float[] rows, int channels, float[] codebook,
            float logInverseTemperature, float[] bias, long maxElements, M3NcmOps.Cancellation cancellation);
}
