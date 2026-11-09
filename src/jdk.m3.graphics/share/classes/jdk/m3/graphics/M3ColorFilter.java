package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;

public class M3ColorFilter extends M3RuntimeEffectChild {
    static { M3Library.staticLoad(); }
    public static M3ColorFilter makeComposed(M3ColorFilter outer, M3ColorFilter inner) {
        try {
            M3Stats.onNativeCall();
            return new M3ColorFilter(_nMakeComposed(M3Native.getPtr(outer), M3Native.getPtr(inner)));
        } finally {
            M3ReferenceUtil.reachabilityFence(outer);
            M3ReferenceUtil.reachabilityFence(inner);
        }
    }

    public static M3ColorFilter makeBlend(int color, M3BlendMode mode) {
        M3Stats.onNativeCall();
        return new M3ColorFilter(_nMakeBlend(color, mode.ordinal()));
    }

    public static M3ColorFilter makeMatrix(M3ColorMatrix matrix) {
        M3Stats.onNativeCall();
        return new M3ColorFilter(_nMakeMatrix(matrix.getMat()));
    }

    public static M3ColorFilter makeHSLAMatrix(M3ColorMatrix matrix) {
        M3Stats.onNativeCall();
        return new M3ColorFilter(_nMakeHSLAMatrix(matrix.getMat()));
    }

    public static class _LinearToSRGBGammaHolder {
        static { M3Stats.onNativeCall(); }
        public static final M3ColorFilter INSTANCE = new M3ColorFilter(_nGetLinearToSRGBGamma(), false);
    }

    public static M3ColorFilter getLinearToSRGBGamma() {
        return _LinearToSRGBGammaHolder.INSTANCE;
    }

    public static class _SRGBToLinearGammaHolder {
        static { M3Stats.onNativeCall(); }
        public static final M3ColorFilter INSTANCE = new M3ColorFilter(_nGetSRGBToLinearGamma(), false);
    }

    public static M3ColorFilter getSRGBToLinearGamma() {
        return _SRGBToLinearGammaHolder.INSTANCE;
    }

    public static M3ColorFilter makeLerp(M3ColorFilter dst, M3ColorFilter src, float t) {
        try {
            return new M3ColorFilter(_nMakeLerp(t, M3Native.getPtr(dst), M3Native.getPtr(src)));
        } finally {
            M3ReferenceUtil.reachabilityFence(dst);
            M3ReferenceUtil.reachabilityFence(src);
        }
    }

    public static M3ColorFilter makeLighting(int colorMul, int colorAdd) {
        return new M3ColorFilter(_nMakeLighting(colorMul, colorAdd));
    }

    public static M3ColorFilter makeHighContrast(boolean grayscale, M3InversionMode mode, float contrast) {
        return new M3ColorFilter(_nMakeHighContrast(grayscale, mode.ordinal(), contrast));
    }

    public static M3ColorFilter makeTable(byte[] table) {
        assert table.length == 256 : "Expected 256 elements, got " + table.length;
        return new M3ColorFilter(_nMakeTable(table));
    }

    public static M3ColorFilter makeTableARGB(byte[] a, byte[] r, byte[] g, byte[] b) {
        assert a == null || a.length == 256 : "Expected 256 elements in a[], got " + a.length;
        assert r == null || r.length == 256 : "Expected 256 elements in r[], got " + r.length;
        assert g == null || g.length == 256 : "Expected 256 elements in g[], got " + g.length;
        assert b == null || b.length == 256 : "Expected 256 elements in b[], got " + b.length;
        return new M3ColorFilter(_nMakeTableARGB(a, r, g, b));
    }

    public static M3ColorFilter makeOverdraw(int[] colors) {
        assert colors.length == 6 : "Expected 6 elements, got " + colors.length;
        return new M3ColorFilter(_nMakeOverdraw(colors[0], colors[1], colors[2], colors[3], colors[4], colors[5]));
    }

    public static class _LumaHolder {
        static { M3Stats.onNativeCall(); }
        public static final M3ColorFilter INSTANCE = new M3ColorFilter(_nGetLuma(), false);
    }

    public static M3ColorFilter getLuma() {
        return _LumaHolder.INSTANCE;
    }

    public M3ColorFilter(long ptr) {
        super(ptr);
    }

    public M3ColorFilter(long ptr, boolean allowClose) {
        super(ptr, allowClose);
    }
    public static native long _nMakeComposed(long outer, long inner);
    public static native long _nMakeBlend(int color, int blendMode);
    public static native long _nMakeMatrix(float[] rowMajor);
    public static native long _nMakeHSLAMatrix(float[] rowMajor);
    public static native long _nGetLinearToSRGBGamma();
    public static native long _nGetSRGBToLinearGamma();
    public static native long _nMakeLerp(float t, long dstPtr, long srcPtr);
    public static native long _nMakeLighting(int colorMul, int colorAdd);
    public static native long _nMakeHighContrast(boolean grayscale, int inversionMode, float contrast);
    public static native long _nMakeTable(byte[] table);
    public static native long _nMakeTableARGB(byte[] a, byte[] r, byte[] g, byte[] b);
    public static native long _nMakeOverdraw(int c0, int c1, int c2, int c3, int c4, int c5);
    public static native long _nGetLuma();
}