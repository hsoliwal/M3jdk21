package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;

/**
 * SkBlender represents a custom blend function in the Skia pipeline.
 * A blender combines a source color (the result of our paint) and destination
 * color (from the canvas) into a final color.
 */
public class M3Blender extends M3RuntimeEffectChild {
    static { M3Library.staticLoad(); }

    /**
     * Create a blender that implements the specified BlendMode.
     *
     * @param mode  The blend mode to use
     * @return      A new Blender that implements the specified blend mode
     */
    public static M3Blender makeWithMode(M3BlendMode mode) {
        M3Stats.onNativeCall();
        return new M3Blender(_nMakeWithMode(mode.ordinal()));
    }

    /**
     * Create a blender that implements the following arithmetic formula:
     * <pre>
     * k1 * src * dst + k2 * src + k3 * dst + k4
     * </pre>
     *
     * @param k1              First coefficient (src * dst factor)
     * @param k2              Second coefficient (src factor)
     * @param k3              Third coefficient (dst factor)
     * @param k4              Fourth coefficient (constant)
     * @param enforcePremul   If true, the RGB channels will be clamped to the calculated alpha
     * @return                A new Blender that implements arithmetic blending
     */
    public static M3Blender makeArithmetic(float k1, float k2, float k3, float k4, boolean enforcePremul) {
        M3Stats.onNativeCall();
        return new M3Blender(_nMakeArithmetic(k1, k2, k3, k4, enforcePremul));
    }

    public static <T extends M3RuntimeEffectChild> M3Blender makeRuntime(M3RuntimeEffect effect, M3Data uniforms, T[] children) {
        M3Stats.onNativeCall();
        int childCount = children == null ? 0 : children.length;
        long[] childrenPtrs = new long[childCount];
        for (int i = 0; i < childCount; i++) {
            childrenPtrs[i] = M3Native.getPtr(children[i]);
        }
        return new M3Blender(_nMakeRuntime(M3Native.getPtr(effect), M3Native.getPtr(uniforms), childrenPtrs));
    }

    public M3Blender(long ptr) {
        super(ptr);
    }

    public static native long _nMakeWithMode(int blendMode);
    public static native long _nMakeArithmetic(float k1, float k2, float k3, float k4, boolean enforcePremul);
    public static native long _nMakeRuntime(long effectPtr, long uniformsPtr, long[] childrenPtrs);
}
