package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.*;

public class M3TextBlobBuilder extends M3Managed {
    static { M3Library.staticLoad(); }
    public M3TextBlobBuilder(long ptr) {
        super(ptr, _FinalizerHolder.PTR);
    }

    /**
     * Constructs empty TextBlobBuilder. By default, TextBlobBuilder has no runs.
     *
     * @see <a href="https://fiddle.skia.org/c/@TextBlobBuilder_empty_constructor">https://fiddle.skia.org/c/@TextBlobBuilder_empty_constructor</a>
     */
    public M3TextBlobBuilder() {
        this(_nMake());
        M3Stats.onNativeCall();
    }

    /**
     * <p>Returns TextBlob built from runs of glyphs added by builder. Returned
     * TextBlob is immutable; it may be copied, but its contents may not be altered.
     * Returns null if no runs of glyphs were added by builder.</p>
     *
     * <p>Resets TextBlobBuilder to its initial empty state, allowing it to be
     * reused to build a new set of runs.</p>
     *
     * @return  SkTextBlob or null
     *
     * @see <a href="https://fiddle.skia.org/c/@TextBlobBuilder_make">https://fiddle.skia.org/c/@TextBlobBuilder_make</a>
     */
    public M3TextBlob build() {
        try {
            M3Stats.onNativeCall();
            long ptr = _nBuild(_ptr);
            return ptr == 0 ? null : new M3TextBlob(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Glyphs are positioned on a baseline at (x, y), using font metrics to
     * determine their relative placement.
     *
     * @param font    Font used for this run
     * @param text    Text to append in this run
     * @param x       horizontal offset within the blob
     * @param y       vertical offset within the blob
     * @return        this
     */
    public M3TextBlobBuilder appendRun(M3Font font, String text, float x, float y) {
        return appendRun(font, font.getStringGlyphs(text), x, y, null);
    }

    /**
     * <p>Glyphs are positioned on a baseline at (x, y), using font metrics to
     * determine their relative placement.</p>
     *
     * <p>bounds defines an optional bounding box, used to suppress drawing when TextBlob
     * bounds does not intersect Surface bounds. If bounds is null, TextBlob bounds
     * is computed from (x, y) and glyphs metrics.</p>
     *
     * @param font    Font used for this run
     * @param text    Text to append in this run
     * @param x       horizontal offset within the blob
     * @param y       vertical offset within the blob
     * @param bounds  optional run bounding box
     * @return        this
     */
    public M3TextBlobBuilder appendRun(M3Font font, String text, float x, float y, M3Rect bounds) {
        return appendRun(font, font.getStringGlyphs(text), x, y, bounds);
    }

    /**
     * <p>Glyphs are positioned on a baseline at (x, y), using font metrics to
     * determine their relative placement.</p>
     *
     * <p>bounds defines an optional bounding box, used to suppress drawing when TextBlob
     * bounds does not intersect Surface bounds. If bounds is null, TextBlob bounds
     * is computed from (x, y) and glyphs metrics.</p>
     *
     * @param font    Font used for this run
     * @param glyphs  glyphs in this run
     * @param x       horizontal offset within the blob
     * @param y       vertical offset within the blob
     * @param bounds  optional run bounding box
     * @return        this
     */
    public M3TextBlobBuilder appendRun(M3Font font, short[] glyphs, float x, float y, M3Rect bounds) {
        try {
            M3Stats.onNativeCall();
            _nAppendRun(_ptr, M3Native.getPtr(font), glyphs, x, y, bounds);
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(font);
        }
    }

    /**
     * <p>Glyphs are positioned on a baseline at y, using x-axis positions from xs.</p>
     *
     * @param font    Font used for this run
     * @param glyphs  glyphs in this run
     * @param xs      horizontal positions on glyphs within the blob
     * @param y       vertical offset within the blob
     * @return        this
     */
    public M3TextBlobBuilder appendRunPosH(M3Font font, short[] glyphs, float[] xs, float y) {
        return appendRunPosH(font, glyphs, xs, y, null);
    }

    /**
     * <p>Glyphs are positioned on a baseline at y, using x-axis positions from xs.</p>
     *
     * <p>bounds defines an optional bounding box, used to suppress drawing when TextBlob
     * bounds does not intersect Surface bounds. If bounds is null, TextBlob bounds
     * is computed from (x, y) and glyphs metrics.</p>
     *
     * @param font    Font used for this run
     * @param glyphs  glyphs in this run
     * @param xs      horizontal positions of glyphs within the blob
     * @param y       vertical offset within the blob
     * @param bounds  optional run bounding box
     * @return        this
     */
    public M3TextBlobBuilder appendRunPosH(M3Font font, short[] glyphs, float[] xs, float y, M3Rect bounds) {
        try {
            assert glyphs.length == xs.length : "glyphs.length " + glyphs.length + " != xs.length " + xs.length;
            M3Stats.onNativeCall();
            _nAppendRunPosH(_ptr, M3Native.getPtr(font), glyphs, xs, y, bounds);
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(font);
        }
    }

    /**
     * <p>Glyphs are positioned at positions from pos.</p>
     *
     * @param font    Font used for this run
     * @param glyphs  glyphs in this run
     * @param pos     positions of glyphs within the blob
     * @return        this
     */
    public M3TextBlobBuilder appendRunPos(M3Font font, short[] glyphs, M3Point[] pos) {
        return appendRunPos(font, glyphs, pos, null);
    }

    /**
     * <p>Glyphs are positioned at positions from pos.</p>
     *
     * <p>bounds defines an optional bounding box, used to suppress drawing when TextBlob
     * bounds does not intersect Surface bounds. If bounds is null, TextBlob bounds
     * is computed from (x, y) and glyphs metrics.</p>
     *
     * @param font    Font used for this run
     * @param glyphs  glyphs in this run
     * @param pos     positions of glyphs within the blob
     * @param bounds  optional run bounding box
     * @return        this
     */
    public M3TextBlobBuilder appendRunPos(M3Font font, short[] glyphs, M3Point[] pos, M3Rect bounds) {
        try {
            assert glyphs.length == pos.length : "glyphs.length " + glyphs.length + " != pos.length " + pos.length;
            float[] floatPos = new float[pos.length * 2];
            for (int i = 0; i < pos.length; ++i) {
                floatPos[i * 2]     = pos[i]._x;
                floatPos[i * 2 + 1] = pos[i]._y;
            }
            M3Stats.onNativeCall();
            _nAppendRunPos(_ptr, M3Native.getPtr(font), glyphs, floatPos, bounds);
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(font);
        }
    }

    public M3TextBlobBuilder appendRunRSXform(M3Font font, short[] glyphs, M3RSXform[] xform) {
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
            _nAppendRunRSXform(_ptr, M3Native.getPtr(font), glyphs, floatXform);
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(font);
        }
    }

    public static class _FinalizerHolder {
        public static final long PTR = _nGetFinalizer();
    }

    public static native long _nGetFinalizer();
    public static native long _nMake();
    public static native long _nBuild(long ptr);
    public static native void _nAppendRun(long ptr, long fontPtr, short[] glyphs, float x, float y, M3Rect bounds);
    public static native void _nAppendRunPosH(long ptr, long fontPtr, short[] glyphs, float[] xs, float y, M3Rect bounds);
    public static native void _nAppendRunPos(long ptr, long fontPtr, short[] glyphs, float[] pos, M3Rect bounds);
    public static native void _nAppendRunRSXform(long ptr, long fontPtr, short[] glyphs, float[] xform);
}