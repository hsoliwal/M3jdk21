package jdk.m3.graphics.sksg;

import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.*;

/**
 * <p>Receiver for invalidation events.</p>
 * <p>Tracks dirty regions for repaint.</p>
 */
public class M3InvalidationController extends M3Managed {
    static { M3Library.staticLoad(); }

    public M3InvalidationController(long ptr) {
        super(ptr, _FinalizerHolder.PTR);
    }

    public static class _FinalizerHolder {
        public static final long PTR = _nGetFinalizer();
    }

    public M3InvalidationController() {
        this(_nMake());
    }

    public M3InvalidationController invalidate(float left, float top, float right, float bottom, M3Matrix33 matrix) {
        M3Stats.onNativeCall();
        _nInvalidate(_ptr, left, top, right, bottom, matrix == null ? M3Matrix33.IDENTITY._mat : matrix._mat);
        return this;
    }

    public M3Rect getBounds() {
        try {
            M3Stats.onNativeCall();
            return _nGetBounds(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3InvalidationController reset() {
        M3Stats.onNativeCall();
        _nReset(_ptr);
        return this;
    }

    public static native long _nGetFinalizer();
    public static native long _nMake();
    public static native void _nInvalidate(long ptr, float left, float top, float right, float bottom, float[] matrix);
    public static native M3Rect _nGetBounds(long ptr);
    public static native void _nReset(long ptr);
}