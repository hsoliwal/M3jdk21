package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.shaper.*;

public class M3TextLine extends M3Managed {
    static { M3Library.staticLoad(); }
    public M3TextLine(long ptr) {
        super(ptr, _FinalizerHolder.PTR);
    }

    public static class _FinalizerHolder {
        public static final long PTR = _nGetFinalizer();
    }

    public static M3TextLine make(String text, M3Font font) {
        return make(text, font, M3ShapingOptions.DEFAULT);
    }

    public static M3TextLine make(String text, M3Font font, M3ShapingOptions opts) {
        try (M3Shaper shaper = M3Shaper.makeShapeDontWrapOrReorder();) {
            return shaper.shapeLine(text, font, opts);
        }
    }

    public float getAscent() {
        M3Stats.onNativeCall();
        try {
            return _nGetAscent(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public float getCapHeight() {
        M3Stats.onNativeCall();
        try {
            return _nGetCapHeight(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public float getXHeight() {
        M3Stats.onNativeCall();
        try {
            return _nGetXHeight(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public float getDescent() {
        M3Stats.onNativeCall();
        try {
            return _nGetDescent(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public float getLeading() {
        M3Stats.onNativeCall();
        try {
            return _nGetLeading(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public float getWidth() {
        M3Stats.onNativeCall();
        try {
            return _nGetWidth(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public float getHeight() {
        M3Stats.onNativeCall();
        try {
            return _nGetHeight(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3TextBlob getTextBlob() {
        M3Stats.onNativeCall();
        try {
            long res = _nGetTextBlob(_ptr);
            return res == 0 ? null : new M3TextBlob(res);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public short[] getGlyphs() {
        M3Stats.onNativeCall();
        try {
            return _nGetGlyphs(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @return  [x0, y0, x1, y1, ...]
     */
    public float[] getPositions() {
        M3Stats.onNativeCall();
        try {
            return _nGetPositions(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @param x  coordinate in px
     * @return   UTF-16 offset of glyph
     */
    public int getOffsetAtCoord(float x) {
        try {
            M3Stats.onNativeCall();
            return _nGetOffsetAtCoord(_ptr, x);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @param x  coordinate in px
     * @return   UTF-16 offset of glyph strictly left of coord
     */
    public int getLeftOffsetAtCoord(float x) {
        try {
            M3Stats.onNativeCall();
            return _nGetLeftOffsetAtCoord(_ptr, x);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @param offset  UTF-16 character offset
     * @return        glyph coordinate
     */
    public float getCoordAtOffset(int offset) {
        try {
            M3Stats.onNativeCall();
            return _nGetCoordAtOffset(_ptr, offset);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Returns the number of intervals that intersect bounds.
     * bounds describes a pair of lines parallel to the text advance.
     * The return array size is a multiple of two, and is at most twice the number of glyphs in
     * the the blob.</p>
     *
     * @param lowerBound lower line parallel to the advance
     * @param upperBound upper line parallel to the advance
     * @return           intersections; may be null
     */
    public float[] getIntercepts(float lowerBound, float upperBound) {
        return getIntercepts(lowerBound, upperBound, null);
    }

    /**
     * <p>Returns the number of intervals that intersect bounds.
     * bounds describes a pair of lines parallel to the text advance.
     * The return array size is a multiple of two, and is at most twice the number of glyphs in
     * the the blob.</p>
     *
     * @param lowerBound lower line parallel to the advance
     * @param upperBound upper line parallel to the advance
     * @param paint      specifies stroking, PathEffect that affects the result; may be null
     * @return           intersections; may be null
     */
    public float[] getIntercepts(float lowerBound, float upperBound, M3Paint paint) {
        try (M3TextBlob blob = getTextBlob()) {
            return blob == null ? null : blob.getIntercepts(lowerBound, upperBound, paint);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(paint);
        }
    }

    public static native long  _nGetFinalizer();
    public static native float _nGetAscent(long ptr);
    public static native float _nGetCapHeight(long ptr);
    public static native float _nGetXHeight(long ptr);
    public static native float _nGetDescent(long ptr);
    public static native float _nGetLeading(long ptr);
    public static native float _nGetWidth(long ptr);
    public static native float _nGetHeight(long ptr);
    public static native long  _nGetTextBlob(long ptr);
    public static native short[] _nGetGlyphs(long ptr);
    public static native float[] _nGetPositions(long ptr);
    public static native float[] _nGetRunPositions(long ptr);
    public static native float[] _nGetBreakPositions(long ptr);
    public static native int[]   _nGetBreakOffsets(long ptr);
    public static native int   _nGetOffsetAtCoord(long ptr, float x);
    public static native int   _nGetLeftOffsetAtCoord(long ptr, float x);
    public static native float _nGetCoordAtOffset(long ptr, int offset);
}