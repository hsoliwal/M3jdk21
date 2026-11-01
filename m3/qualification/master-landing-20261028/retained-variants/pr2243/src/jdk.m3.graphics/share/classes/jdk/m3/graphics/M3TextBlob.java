package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.*;

public class M3TextBlob extends M3Managed {
    static { M3Library.staticLoad(); }
    public M3TextBlob(long ptr) {
        super(ptr, _FinalizerHolder.PTR);
    }

    /**
     * Returns conservative bounding box. Uses Paint associated with each glyph to
     * determine glyph bounds, and unions all bounds. Returned bounds may be
     * larger than the bounds of all glyphs in runs.
     *
     * @return  conservative bounding box
     */
    public M3Rect getBounds() {
        try {
            M3Stats.onNativeCall();
            return _nBounds(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns a non-zero value unique among all text blobs.
     *
     * @return  identifier for TextBlob
     */
    public int getUniqueId() {
        try {
            M3Stats.onNativeCall();
            return _nGetUniqueId(_ptr);
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
     * <p>Runs within the blob that contain SkRSXform are ignored when computing intercepts.</p>
     *
     * @param lowerBound lower line parallel to the advance
     * @param upperBound upper line parallel to the advance
     * @return           intersections; may be null
     */
    public float[] getIntercepts(float lowerBound, float upperBound) {
        return getIntercepts(lowerBound, upperBound);
    }

    /**
     * <p>Returns the number of intervals that intersect bounds.
     * bounds describes a pair of lines parallel to the text advance.
     * The return array size is a multiple of two, and is at most twice the number of glyphs in
     * the the blob.</p>
     *
     * <p>Runs within the blob that contain SkRSXform are ignored when computing intercepts.</p>
     *
     * @param lowerBound lower line parallel to the advance
     * @param upperBound upper line parallel to the advance
     * @param paint      specifies stroking, PathEffect that affects the result; may be null
     * @return           intersections; may be null
     */
    public float[] getIntercepts(float lowerBound, float upperBound, M3Paint paint) {
        try {
            M3Stats.onNativeCall();
            return _nGetIntercepts(_ptr, lowerBound, upperBound, M3Native.getPtr(paint));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(paint);
        }
    }

    /**
     * Returns a TextBlob built from a single run of text with x-positions and a single y value.
     * Returns null if glyphs is empty.
     *
     * @param glyphs  glyphs drawn
     * @param xpos    array of x-positions, must contain values for all of the glyphs.
     * @param ypos    shared y-position for each glyph, to be paired with each xpos.
     * @param font    Font used for this run
     * @return        new TextBlob or null
     */
    public static M3TextBlob makeFromPosH(short[] glyphs, float[] xpos, float ypos, M3Font font) {
        try {
            assert glyphs.length == xpos.length : "glyphs.length " + glyphs.length + " != xpos.length " + xpos.length;
            M3Stats.onNativeCall();
            long ptr = _nMakeFromPosH(glyphs, xpos, ypos, M3Native.getPtr(font));
            return ptr == 0 ? null : new M3TextBlob(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(font);
        }
    }

    /**
     * Returns a TextBlob built from a single run of text with positions.
     * Returns null if glyphs is empty.
     *
     * @param glyphs  glyphs drawn
     * @param pos     array of positions, must contain values for all of the glyphs.
     * @param font    Font used for this run
     * @return        new TextBlob or null
     */
    public static M3TextBlob makeFromPos(short[] glyphs, M3Point[] pos, M3Font font) {
        try {
            assert glyphs.length == pos.length : "glyphs.length " + glyphs.length + " != pos.length " + pos.length;
            float[] floatPos = new float[pos.length * 2];
            for (int i = 0; i < pos.length; ++i) {
                floatPos[i * 2]     = pos[i]._x;
                floatPos[i * 2 + 1] = pos[i]._y;
            }
            M3Stats.onNativeCall();
            long ptr = _nMakeFromPos(glyphs, floatPos, M3Native.getPtr(font));
            return ptr == 0 ? null : new M3TextBlob(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(font);
        }
    }

    public static M3TextBlob makeFromRSXform(short[] glyphs, M3RSXform[] xform, M3Font font) {
        try {
            assert glyphs.length == xform.length : "glyphs.length " + glyphs.length + " != xform.length " + xform.length;
            float[] floatXform = new float[xform.length * 4];
            for (int i = 0; i < xform.length; ++i) {
                floatXform[i * 4]     = xform[i]._scos;
                floatXform[i * 4 + 1] = xform[i]._ssin;
                floatXform[i * 4 + 2] = xform[i]._tx;
                floatXform[i * 4 + 3] = xform[i]._ty;
            }
            M3Stats.onNativeCall();
            long ptr = _nMakeFromRSXform(glyphs, floatXform, M3Native.getPtr(font));
            return ptr == 0 ? null : new M3TextBlob(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(font);
        }
    }

    public M3Data serializeToData() {
        try {
            M3Stats.onNativeCall();
            return new M3Data(_nSerializeToData(_ptr));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public static M3TextBlob makeFromData(M3Data data) {
        try {
            M3Stats.onNativeCall();
            long ptr = _nMakeFromData(M3Native.getPtr(data));
            return ptr == 0 ? null : new M3TextBlob(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(data);
        }
    }

    /**
     * @return  glyph indices for the whole blob
     */
    public short[] getGlyphs() {
        try {
            M3Stats.onNativeCall();
            return _nGetGlyphs(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Return result depends on how blob was constructed.</p>
     *
     * <ul><li>makeFromPosH returns 1 float per glyph (x pos)
     * <li>makeFromPos returns 2 floats per glyph (x, y pos)
     * <li>makeFromRSXform returns 4 floats per glyph
     * </ul>
     *
     * <p>Blobs constructed by TextBlobBuilderRunHandler/Shaper default handler have 2 floats per glyph.</p>
     *
     * @return  glyph positions for the blob if it was made with makeFromPos, null otherwise
     */
    public float[] getPositions() {
        try {
            M3Stats.onNativeCall();
            return _nGetPositions(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Only works on TextBlobs that come from TextBlobBuilderRunHandler/Shaper default handler.
     *
     * @return  utf-16 offsets of clusters that start the glyph
     * @throws  IllegalArgumentException if TextBlob doesn’t have this information
     */
    public int[] getClusters() {
        try {
            M3Stats.onNativeCall();
            int[] res = _nGetClusters(_ptr);
            if (res == null)
                throw new IllegalArgumentException();
            return res;
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Only works on TextBlobs that come from TextBlobBuilderRunHandler/Shaper default handler.
     *
     * @return  tight bounds around all the glyphs in the TextBlob
     * @throws  IllegalArgumentException if TextBlob doesn’t have this information
     */
    public M3Rect getTightBounds() {
        try {
            M3Stats.onNativeCall();
            M3Rect res = _nGetTightBounds(_ptr);
            if (res == null)
                throw new IllegalArgumentException();
            return res;
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

   /**
     * Only works on TextBlobs that come from TextBlobBuilderRunHandler/Shaper default handler.
     *
     * @return  tight bounds around all the glyphs in the TextBlob
     * @throws  IllegalArgumentException if TextBlob doesn’t have this information
     */
    public M3Rect getBlockBounds() {
        try {
            M3Stats.onNativeCall();
            M3Rect res = _nGetBlockBounds(_ptr);
            if (res == null)
                throw new IllegalArgumentException();
            return res;
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

   /**
     * Only works on TextBlobs that come from TextBlobBuilderRunHandler/Shaper default handler.
     *
     * @return  first baseline in TextBlob
     * @throws  IllegalArgumentException if TextBlob doesn’t have this information
     */
    public Float getFirstBaseline() {
        try {
            M3Stats.onNativeCall();
            Float res = _nGetFirstBaseline(_ptr);
            if (res == null)
                throw new IllegalArgumentException();
            return res;
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

   /**
     * Only works on TextBlobs that come from TextBlobBuilderRunHandler/Shaper default handler.
     *
     * @return  last baseline in TextBlob
     * @throws  IllegalArgumentException if TextBlob doesn’t have this information
     */
    public Float getLastBaseline() {
        try {
            M3Stats.onNativeCall();
            Float res = _nGetLastBaseline(_ptr);
            if (res == null)
                throw new IllegalArgumentException();
            return res;
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public static class _FinalizerHolder {
        public static final long PTR = _nGetFinalizer();
    }

    public static native long _nGetFinalizer();
    public static native M3Rect _nBounds(long ptr);
    public static native int  _nGetUniqueId(long ptr);
    public static native float[]  _nGetIntercepts(long ptr, float lower, float upper, long paintPtr);
    public static native long _nMakeFromPosH(short[] glyphs, float[] xpos, float ypos, long fontPtr);
    public static native long _nMakeFromPos(short[] glyphs, float[] pos, long fontPtr);
    public static native long _nMakeFromRSXform(short[] glyphs, float[] xform, long fontPtr);
    public static native long _nSerializeToData(long ptr /*, SkSerialProcs */);
    public static native long _nMakeFromData(long dataPtr /*, SkDeserialProcs */);
    public static native short[] _nGetGlyphs(long ptr);
    public static native float[] _nGetPositions(long ptr);
    public static native int[] _nGetClusters(long ptr);
    public static native M3Rect _nGetTightBounds(long ptr);
    public static native M3Rect _nGetBlockBounds(long ptr);
    public static native Float _nGetFirstBaseline(long ptr);
    public static native Float _nGetLastBaseline(long ptr);
}