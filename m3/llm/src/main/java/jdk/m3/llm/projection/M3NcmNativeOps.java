// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.projection;

import java.util.Objects;

/** Coarse, bounded JNI batches for the explicit NCM numerical profile.
 * Java remains the semantic oracle. No model, tokenizer or native memory is retained.
 * Callers must not mutate input arrays until a call returns. Native-required mode
 * fails visibly if the versioned feature is unavailable, including older libraries.
 * The Java validation budget limits admitted work and result/scratch dimensions.
 * JNI may copy input arrays, and native scratch temporarily coexists with the Java
 * result; VM-dependent peak native memory is a separate qualification measure.
 */
public final class M3NcmNativeOps {
    /** Backend choice is observable through availability and required-mode refusal. */
    public enum Mode { JAVA, AUTO, NATIVE_REQUIRED }
    private static final boolean AVAILABLE = load();

    private M3NcmNativeOps() { }

    private static boolean load() {
        if (Boolean.getBoolean("m3.llm.ncm.disableNative")) return false;
        try {
            System.loadLibrary("m3_llm_ncm");
            return abiVersionNative() == 1;
        } catch (UnsatisfiedLinkError | SecurityException absentFeature) { return false; }
    }

    public static boolean available() { return AVAILABLE; }

    public static int[] nearest(float[] rows, int channels, float[] codebook, Mode mode) {
        int length = M3NcmOps.checkNearest(rows,channels,codebook);
        if (!useNative(mode)) return M3NcmOps.nearest(rows,channels,codebook);
        int[] result = nearestNative(rows,channels,codebook);
        if (result == null || result.length != length) throw invalidNative();
        int vocabulary = codebook.length/channels;
        for (int id : result) if (id < 0 || id >= vocabulary) throw invalidNative();
        return result;
    }

    public static float[] areaResize(float[] values,int batch,int fromLength,int channels,
                                     int toLength,Mode mode) {
        int length = M3NcmOps.checkResize(values,batch,fromLength,channels,toLength);
        if (!useNative(mode)) return M3NcmOps.areaResize(values,batch,fromLength,channels,toLength);
        return checked(resizeNative(values,batch,fromLength,channels,toLength,false),length);
    }

    public static float[] linearResize(float[] values,int batch,int fromLength,int channels,
                                       int toLength,Mode mode) {
        int length = M3NcmOps.checkResize(values,batch,fromLength,channels,toLength);
        if (!useNative(mode)) return M3NcmOps.linearResize(values,batch,fromLength,channels,toLength);
        return checked(resizeNative(values,batch,fromLength,channels,toLength,true),length);
    }

    public static float[] phi(float[] values,int batch,int length,int channels,
                              float[] weights,float[] bias,float ratio,Mode mode) {
        int size = M3NcmOps.checkPhi(values,batch,length,channels,weights,bias,ratio);
        if (!useNative(mode)) return M3NcmOps.phi(values,batch,length,channels,weights,bias,ratio);
        return checked(phiNative(values,batch,length,channels,weights,bias,ratio),size);
    }

    public static float[] tiedLogits(float[] rows,int channels,float[] codebook,
                                     float logInverseTemperature,float[] bias,Mode mode) {
        int length = M3NcmOps.checkTiedLogits(rows,channels,codebook,logInverseTemperature,bias);
        if (!useNative(mode)) return M3NcmOps.tiedLogits(rows,channels,codebook,logInverseTemperature,bias);
        return checked(tiedLogitsNative(rows,channels,codebook,logInverseTemperature,bias),length);
    }

    private static boolean useNative(Mode mode) {
        Objects.requireNonNull(mode,"mode");
        if (mode == Mode.NATIVE_REQUIRED && !AVAILABLE)
            throw new UnsupportedOperationException("NCM numerical JNI ABI 1 is unavailable");
        return mode != Mode.JAVA && AVAILABLE;
    }

    private static float[] checked(float[] result,int length) {
        if (result == null || result.length != length) throw invalidNative();
        M3NcmOps.checkFinite(result);
        return result;
    }

    private static IllegalStateException invalidNative() {
        return new IllegalStateException("NCM JNI returned an invalid result shape or code");
    }

    private static native int abiVersionNative();
    private static native int[] nearestNative(float[] rows,int channels,float[] codebook);
    private static native float[] resizeNative(float[] values,int batch,int fromLength,int channels,
                                               int toLength,boolean linear);
    private static native float[] phiNative(float[] values,int batch,int length,int channels,
                                            float[] weights,float[] bias,float ratio);
    private static native float[] tiedLogitsNative(float[] rows,int channels,float[] codebook,
                                                   float logInverseTemperature,float[] bias);
}
