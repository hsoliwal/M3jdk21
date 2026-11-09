package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;

public class M3PixelRef extends M3RefCnt {
    static { M3Library.staticLoad(); }
    public M3PixelRef(long ptr) {
        super(ptr);
    }

    public int getWidth() {
        try {
            M3Stats.onNativeCall();
            return _nGetWidth(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public int getHeight() {
        try {
            M3Stats.onNativeCall();
            return _nGetHeight(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public long getRowBytes() {
        try {
            M3Stats.onNativeCall();
            return _nGetRowBytes(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns a non-zero, unique value corresponding to the pixels in this
     * pixelref. Each time the pixels are changed (and notifyPixelsChanged is
     * called), a different generation ID will be returned.
     */
    public int getGenerationId() {
        try {
            M3Stats.onNativeCall();
            return _nGetGenerationId(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Call this if you have changed the contents of the pixels. This will in-
     * turn cause a different generation ID value to be returned from
     * getGenerationID().
     */
    public M3PixelRef notifyPixelsChanged() {
        M3Stats.onNativeCall();
        _nNotifyPixelsChanged(_ptr);
        return this;
    }

    /**
     * Returns true if this pixelref is marked as immutable, meaning that the
     * contents of its pixels will not change for the lifetime of the pixelref.
     */
    public boolean isImmutable() {
        try {
            M3Stats.onNativeCall();
            return _nIsImmutable(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Marks this pixelref is immutable, meaning that the contents of its
     * pixels will not change for the lifetime of the pixelref. This state can
     * be set on a pixelref, but it cannot be cleared once it is set.
     */
    public M3PixelRef setImmutable() {
        M3Stats.onNativeCall();
        _nSetImmutable(_ptr);
        return this;
    }

    public static native int  _nGetWidth(long ptr);
    public static native int  _nGetHeight(long ptr);
    public static native long _nGetRowBytes(long ptr);
    public static native int  _nGetGenerationId(long ptr);
    public static native void _nNotifyPixelsChanged(long ptr);
    public static native boolean _nIsImmutable(long ptr);
    public static native void _nSetImmutable(long ptr);
}