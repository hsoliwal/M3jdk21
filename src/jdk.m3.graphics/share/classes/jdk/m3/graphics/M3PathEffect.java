package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;

public class M3PathEffect extends M3RefCnt implements M3Flattenable {
    static { M3Library.staticLoad(); }
    public M3PathEffect makeSum(M3PathEffect second) {
        try {
            M3Stats.onNativeCall();
            return new M3PathEffect(_nMakeSum(_ptr, M3Native.getPtr(second)));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(second);
        }
    }
    public M3PathEffect makeCompose(M3PathEffect inner) {
        try {
            M3Stats.onNativeCall();
            return new M3PathEffect(_nMakeCompose(_ptr, M3Native.getPtr(inner)));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(inner);
        }
    }
    public static M3PathEffect makePath1D(M3Path path, float advance, float phase, M3PathEffect1DStyle style) {
        try {
            M3Stats.onNativeCall();
            return new M3PathEffect(_nMakePath1D(M3Native.getPtr(path), advance, phase, style.ordinal()));
        } finally {
            M3ReferenceUtil.reachabilityFence(path);
        }
    }

    public static M3PathEffect makePath2D(M3Matrix33 matrix, M3Path path) {
        try {
            M3Stats.onNativeCall();
            return new M3PathEffect(_nMakePath2D(matrix.getMat(), M3Native.getPtr(path)));
        } finally {
            M3ReferenceUtil.reachabilityFence(path);
        }
    }

    public static M3PathEffect makeLine2D(float width, M3Matrix33 matrix) {
        M3Stats.onNativeCall();
        return new M3PathEffect(_nMakeLine2D(width, matrix.getMat()));
    }

    public static M3PathEffect makeCorner(float radius) {
        M3Stats.onNativeCall();
        return new M3PathEffect(_nMakeCorner(radius));
    }

    public static M3PathEffect makeDash(float[] intervals, float phase) {
        M3Stats.onNativeCall();
        return new M3PathEffect(_nMakeDash(intervals, phase));
    }

    public static M3PathEffect makeDiscrete(float segLength, float dev, int seed) {
        M3Stats.onNativeCall();
        return new M3PathEffect(_nMakeDiscrete(segLength, dev, seed));
    }

    public M3PathEffect(long ptr) {
        super(ptr);
    }

    public static native long _nMakeSum(long firstPtr, long secondPtr);
    public static native long _nMakeCompose(long outerPtr, long innerPtr);
    public static native long _nMakePath1D(long pathPtr, float advance, float phase, int style);
    public static native long _nMakePath2D(float[] matrix, long pathPtr);
    public static native long _nMakeLine2D(float width, float[] matrix);
    public static native long _nMakeCorner(float radius);
    public static native long _nMakeDash(float[] intervals, float phase);
    public static native long _nMakeDiscrete(float segLength, float dev, int seed);
}