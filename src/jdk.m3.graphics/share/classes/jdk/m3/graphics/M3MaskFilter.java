package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;

public class M3MaskFilter extends M3RefCnt implements M3Flattenable {
    static { M3Library.staticLoad(); }
    public static M3MaskFilter makeBlur(M3FilterBlurMode mode, float sigma) {
        return makeBlur(mode, sigma, true);
    }

    public static M3MaskFilter makeBlur(M3FilterBlurMode mode, float sigma, boolean respectCTM) {
        M3Stats.onNativeCall();
        return new M3MaskFilter(_nMakeBlur(mode.ordinal(), sigma, respectCTM));
    }

    public static M3MaskFilter makeShader(M3Shader s) {
        try {
            M3Stats.onNativeCall();
            return new M3MaskFilter(_nMakeShader(M3Native.getPtr(s)));
        } finally {
            M3ReferenceUtil.reachabilityFence(s);
        }
    }

    public static M3MaskFilter makeTable(byte[] table) {
        M3Stats.onNativeCall();
        return new M3MaskFilter(_nMakeTable(table));
    }

    public static M3MaskFilter makeGamma(float gamma) {
        M3Stats.onNativeCall();
        return new M3MaskFilter(_nMakeGamma(gamma));
    }

    public static M3MaskFilter makeClip(int min, int max) {
        M3Stats.onNativeCall();
        return new M3MaskFilter(_nMakeClip((byte) min, (byte) max));
    }

    // public MaskFilter makeEmboss(float blurSigma, float lightDirectionX, float lightDirectionY, float lightDirectionZ, int lightPad, int lightAmbient, int lightSpecular) {
    //     Native.onNativeCall();
    //     return new MaskFilter(_nMakeEmboss(blurSigma, lightDirectionX, lightDirectionY, lightDirectionZ, lightPad, lightAmbient, lightSpecular));
    // }

    public M3MaskFilter(long ptr) {
        super(ptr);
    }
    public static native long _nMakeBlur(int mode, float sigma, boolean respectCTM);
    public static native long _nMakeShader(long shaderPtr);
    // public static native long _nMakeEmboss(float sigma, float x, float y, float z, int pad, int ambient, int specular);
    public static native long _nMakeTable(byte[] table);
    public static native long _nMakeGamma(float gamma);
    public static native long _nMakeClip(byte min, byte max);
}