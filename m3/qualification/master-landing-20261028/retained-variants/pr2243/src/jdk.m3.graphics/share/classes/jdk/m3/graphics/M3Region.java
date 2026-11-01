package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.*;

public class M3Region extends M3Managed {
    static { M3Library.staticLoad(); }

    public M3Region() {
        super(_nMake(), _FinalizerHolder.PTR);
        M3Stats.onNativeCall();
    }

    public boolean set(M3Region r) {
        try {
            M3Stats.onNativeCall();
            return _nSet(_ptr, M3Native.getPtr(r));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(r);
        }
    }

    public boolean isEmpty() {
        try {
            M3Stats.onNativeCall();
            return _nIsEmpty(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public boolean isRect() {
        try {
            M3Stats.onNativeCall();
            return _nIsRect(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public boolean isComplex() {
        try {
            M3Stats.onNativeCall();
            return _nIsComplex(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3IRect getBounds() {
        try {
            M3Stats.onNativeCall();
            return _nGetBounds(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public int computeRegionComplexity() {
        try {
            M3Stats.onNativeCall();
            return _nComputeRegionComplexity(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3Path getBoundaryPath() {
        try {
            M3Stats.onNativeCall();
            return new M3Path(_nGetBoundaryPath(_ptr));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public boolean setEmpty() {
        try {
            M3Stats.onNativeCall();
            return _nSetEmpty(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public boolean setRect(M3IRect rect) {
        try {
            M3Stats.onNativeCall();
            return _nSetRect(_ptr, rect._left, rect._top, rect._right, rect._bottom);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public boolean setRects(M3IRect[] rects) {
        try {
            int[] arr = new int[rects.length * 4];
            for (int i = 0; i < rects.length; ++i) {
                arr[i * 4]     = rects[i]._left;
                arr[i * 4 + 1] = rects[i]._top;
                arr[i * 4 + 2] = rects[i]._right;
                arr[i * 4 + 3] = rects[i]._bottom;
            }
            M3Stats.onNativeCall();
            return _nSetRects(_ptr, arr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public boolean setRegion(M3Region r) {
        try {
            M3Stats.onNativeCall();
            return _nSetRegion(_ptr, M3Native.getPtr(r));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(r);
        }
    }

    public boolean setPath(M3Path path, M3Region clip) {
        try {
            M3Stats.onNativeCall();
            return _nSetPath(_ptr, M3Native.getPtr(path), M3Native.getPtr(clip));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(path);
            M3ReferenceUtil.reachabilityFence(clip);
        }
    }

    public boolean intersects(M3IRect rect) {
        try {
            M3Stats.onNativeCall();
            return _nIntersectsIRect(_ptr, rect._left, rect._top, rect._right, rect._bottom);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public boolean intersects(M3Region r) {
        try {
            M3Stats.onNativeCall();
            return _nIntersectsRegion(_ptr, M3Native.getPtr(r));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(r);
        }
    }

    public boolean contains(int x, int y) {
        try {
            M3Stats.onNativeCall();
            return _nContainsIPoint(_ptr, x, y);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public boolean contains(M3IRect rect) {
        try {
            M3Stats.onNativeCall();
            return _nContainsIRect(_ptr, rect._left, rect._top, rect._right, rect._bottom);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public boolean contains(M3Region r) {
        try {
            M3Stats.onNativeCall();
            return _nContainsRegion(_ptr, M3Native.getPtr(r));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(r);
        }
    }

    public boolean quickContains(M3IRect rect) {
        try {
            M3Stats.onNativeCall();
            return _nQuickContains(_ptr, rect._left, rect._top, rect._right, rect._bottom);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public boolean quickReject(M3IRect rect) {
        try {
            M3Stats.onNativeCall();
            return _nQuickRejectIRect(_ptr, rect._left, rect._top, rect._right, rect._bottom);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public boolean quickReject(M3Region r) {
        try {
            M3Stats.onNativeCall();
            return _nQuickRejectRegion(_ptr, M3Native.getPtr(r));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(r);
        }
    }

    public void translate(int dx, int dy) {
        try {
            M3Stats.onNativeCall();
            _nTranslate(_ptr, dx, dy);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public boolean op(M3IRect rect, M3RegionOp op) {
        try {
            M3Stats.onNativeCall();
            return _nOpIRect(_ptr, rect._left, rect._top, rect._right, rect._bottom, op.ordinal());
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public boolean op(M3Region r, M3RegionOp op) {
        try {
            M3Stats.onNativeCall();
            return _nOpRegion(_ptr, M3Native.getPtr(r), op.ordinal());
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(r);
        }
    }

    public boolean op(M3IRect rect, M3Region r, M3RegionOp op) {
        try {
            M3Stats.onNativeCall();
            return _nOpIRectRegion(_ptr, rect._left, rect._top, rect._right, rect._bottom, M3Native.getPtr(r), op.ordinal());
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(r);
        }
    }

    public boolean op(M3Region r, M3IRect rect, M3RegionOp op) {
        try {
            M3Stats.onNativeCall();
            return _nOpRegionIRect(_ptr, M3Native.getPtr(r), rect._left, rect._top, rect._right, rect._bottom, op.ordinal());
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(r);
        }
    }

    public boolean op(M3Region a, M3Region b, M3RegionOp op) {
        try {
            M3Stats.onNativeCall();
            return _nOpRegionRegion(_ptr, M3Native.getPtr(a), M3Native.getPtr(b), op.ordinal());
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(a);
            M3ReferenceUtil.reachabilityFence(b);
        }
    }

    public static class _FinalizerHolder {
        public static final long PTR = _nGetFinalizer();
    }

    public static native long    _nMake();
    public static native long    _nGetFinalizer();
    public static native boolean _nSet(long ptr, long regoinPtr);
    public static native boolean _nIsEmpty(long ptr);
    public static native boolean _nIsRect(long ptr);
    public static native boolean _nIsComplex(long ptr);
    public static native M3IRect   _nGetBounds(long ptr);
    public static native int     _nComputeRegionComplexity(long ptr);
    public static native long    _nGetBoundaryPath(long ptr);
    public static native boolean _nSetEmpty(long ptr);
    public static native boolean _nSetRect(long ptr, int left, int top, int right, int bottom);
    public static native boolean _nSetRects(long ptr, int[] rects);
    public static native boolean _nSetRegion(long ptr, long regionPtr);
    public static native boolean _nSetPath(long ptr, long pathPtr, long regionPtr);
    public static native boolean _nIntersectsIRect(long ptr, int left, int top, int right, int bottom);
    public static native boolean _nIntersectsRegion(long ptr, long regionPtr);
    public static native boolean _nContainsIPoint(long ptr, int x, int y);
    public static native boolean _nContainsIRect(long ptr, int left, int top, int right, int bottom);
    public static native boolean _nContainsRegion(long ptr, long regionPtr);
    public static native boolean _nQuickContains(long ptr, int left, int top, int right, int bottom);
    public static native boolean _nQuickRejectIRect(long ptr, int left, int top, int right, int bottom);
    public static native boolean _nQuickRejectRegion(long ptr, long regionPtr);
    public static native void    _nTranslate(long ptr, int dx, int dy);
    public static native boolean _nOpIRect(long ptr, int left, int top, int right, int bottom, int op);
    public static native boolean _nOpRegion(long ptr, long regionPtr, int op);
    public static native boolean _nOpIRectRegion(long ptr, int left, int top, int right, int bottom, long regionPtr, int op);
    public static native boolean _nOpRegionIRect(long ptr, long regionPtr, int left, int top, int right, int bottom, int op);
    public static native boolean _nOpRegionRegion(long ptr, long regionPtrA, long regionPtrB, int op);
}