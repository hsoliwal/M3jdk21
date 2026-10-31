package jdk.m3.graphics;

import java.util.function.*;
import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.*;

public class M3Font extends M3Managed {
    static { M3Library.staticLoad(); }

    public M3FontMetrics _metrics = null;
    public Float _spacing = null;
    public M3Font(long ptr) {
        super(ptr, _FinalizerHolder.PTR);
    }

    public M3Font(long ptr, boolean managed) {
        super(ptr, _FinalizerHolder.PTR, managed);
    }

    /**
     * Returns Font initialized with default values
     */
    public M3Font() {
        this(_nMakeDefault());
        M3Stats.onNativeCall();
    }

    /**
     * Returns Font with Typeface and default size
     *
     * @param typeface  typeface and style used to draw and measure text. Pass null for default
     */
    public M3Font(M3Typeface typeface) {
        this(_nMakeTypeface(M3Native.getPtr(typeface)));
        M3Stats.onNativeCall();
        M3ReferenceUtil.reachabilityFence(typeface);
    }

    /**
     * @param typeface  typeface and style used to draw and measure text. Pass null for default
     * @param size      typographic size of the text
     */
    public M3Font(M3Typeface typeface, float size) {
        this(_nMakeTypefaceSize(M3Native.getPtr(typeface), size));
        M3Stats.onNativeCall();
        M3ReferenceUtil.reachabilityFence(typeface);
    }

    /**
     * Constructs Font with default values with Typeface and size in points,
     * horizontal scale, and horizontal skew. Horizontal scale emulates condensed
     * and expanded fonts. Horizontal skew emulates oblique fonts.
     *
     * @param typeface  typeface and style used to draw and measure text. Pass null for default
     * @param size      typographic size of the text
     * @param scaleX    text horizonral scale
     * @param skewX     additional shear on x-axis relative to y-axis
     */
    public M3Font(M3Typeface typeface, float size, float scaleX, float skewX) {
        this(_nMakeTypefaceSizeScaleSkew(M3Native.getPtr(typeface), size, scaleX, skewX));
        M3Stats.onNativeCall();
        M3ReferenceUtil.reachabilityFence(typeface);
    }

    /**
     * Compares fonts, and returns true if they are equivalent.
     * May return false if Typeface has identical contents but different pointers.
     */
    @Override
    public boolean _nativeEquals(M3Native other) {
        try {
            return _nEquals(_ptr, M3Native.getPtr(other));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(other);
        }
    }

    public void _resetMetrics() {
        _metrics = null;
        _spacing = null;
    }

    public static M3Font makeClone(long ptr) {
        M3Stats.onNativeCall();
        return new M3Font(_nMakeClone(ptr));
    }

    /**
     * If true, instructs the font manager to always hint glyphs.
     * Returned value is only meaningful if platform uses FreeType as the font manager.
     *
     * @return  true if all glyphs are hinted
    */
    public boolean isAutoHintingForced() {
        try {
            M3Stats.onNativeCall();
            return _nIsAutoHintingForced(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @return  true if font engine may return glyphs from font bitmaps instead of from outlines
     */
    public boolean areBitmapsEmbedded() {
        try {
            M3Stats.onNativeCall();
            return _nAreBitmapsEmbedded(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @return  true if glyphs may be drawn at sub-pixel offsets
     */
    public boolean isSubpixel() {
        try {
            M3Stats.onNativeCall();
            return _nIsSubpixel(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @return  true if font and glyph metrics are requested to be linearly scalable
     */
    public boolean areMetricsLinear() {
        try {
            M3Stats.onNativeCall();
            return _nAreMetricsLinear(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns true if bold is approximated by increasing the stroke width when creating glyph
     * bitmaps from outlines.
     *
     * @return  true if bold is approximated through stroke width
     */
    public boolean isEmboldened() {
        try {
            M3Stats.onNativeCall();
            return _nIsEmboldened(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns true if baselines will be snapped to pixel positions when the current transformation
     * matrix is axis aligned.
     *
     * @return  true if baselines may be snapped to pixels
     */
    public boolean isBaselineSnapped() {
        try {
            M3Stats.onNativeCall();
            return _nIsBaselineSnapped(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Sets whether to always hint glyphs. If forceAutoHinting is set, instructs the font manager to always hint glyphs. Only affects platforms that use FreeType as the font manager.
     *
     * @param value  setting to always hint glyphs
     * @return       this
     */
    public M3Font setAutoHintingForced(boolean value) {
        _resetMetrics();
        M3Stats.onNativeCall();
        _nSetAutoHintingForced(_ptr, value);
        return this;
    }

    /**
     * Requests, but does not require, to use bitmaps in fonts instead of outlines.
     *
     * @param value  setting to use bitmaps in fonts
     * @return       this
     */
    public M3Font setBitmapsEmbedded(boolean value) {
        _resetMetrics();
        M3Stats.onNativeCall();
        _nSetBitmapsEmbedded(_ptr, value);
        return this;
    }

    /**
     * Requests, but does not require, that glyphs respect sub-pixel positioning.
     *
     * @param value  setting for sub-pixel positioning
     * @return       this
     */
    public M3Font setSubpixel(boolean value) {
        _resetMetrics();
        M3Stats.onNativeCall();
        _nSetSubpixel(_ptr, value);
        return this;
    }

    /**
     * Requests, but does not require, linearly scalable font and glyph metrics.
     *
     * For outline fonts 'true' means font and glyph metrics should ignore hinting and rounding.
     * Note that some bitmap formats may not be able to scale linearly and will ignore this flag.
     *
     * @param value  setting for linearly scalable font and glyph metrics.
     * @return       this
     */
    public M3Font setMetricsLinear(boolean value) {
        _resetMetrics();
        M3Stats.onNativeCall();
        _nSetMetricsLinear(_ptr, value);
        return this;
    }

    /**
     * Increases stroke width when creating glyph bitmaps to approximate a bold typeface.
     *
     * @param value  setting for bold approximation
     * @return       this
     */
    public M3Font setEmboldened(boolean value) {
        _resetMetrics();
        M3Stats.onNativeCall();
        _nSetEmboldened(_ptr, value);
        return this;
    }

    /**
     * Requests that baselines be snapped to pixels when the current transformation matrix is axis
     * aligned.
     *
     * @param value  setting for baseline snapping to pixels
     * @return       this
     */
    public M3Font setBaselineSnapped(boolean value) {
        _resetMetrics();
        M3Stats.onNativeCall();
        _nSetBaselineSnapped(_ptr, value);
        return this;
    }

    /**
     * Whether edge pixels draw opaque or with partial transparency.
     */
    public M3FontEdging getEdging() {
        try {
            M3Stats.onNativeCall();
            return M3FontEdging._values[_nGetEdging(_ptr)];
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Requests, but does not require, that edge pixels draw opaque or with
     * partial transparency.
     */
    public M3Font setEdging(M3FontEdging value) {
        _resetMetrics();
        M3Stats.onNativeCall();
        _nSetEdging(_ptr, value.ordinal());
        return this;
    }

    /**
     * @return  level of glyph outline adjustment
     */
    public M3FontHinting getHinting() {
        try {
            M3Stats.onNativeCall();
            return M3FontHinting._values[_nGetHinting(_ptr)];
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Sets level of glyph outline adjustment. Does not check for valid values of hintingLevel.
     */
    public M3Font setHinting(M3FontHinting value) {
        _resetMetrics();
        M3Stats.onNativeCall();
        _nSetHinting(_ptr, value.ordinal());
        return this;
    }

    /**
     * Returns a font with the same attributes of this font, but with the specified size.
     */
    public M3Font makeWithSize(float size) {
        return new M3Font(getTypeface(), size, getScaleX(), getSkewX());
    }

    /**
     * @return {@link Typeface} if set, or null
     */
    public M3Typeface getTypeface() {
        try {
            M3Stats.onNativeCall();
            long ptr = _nGetTypeface(_ptr);
            return ptr == 0 ? null : new M3Typeface(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @return  text size in points
     */
    public float getSize() {
        try {
            M3Stats.onNativeCall();
            return _nGetSize(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @return  text scale on x-axis. Default value is 1
     */
    public float getScaleX() {
        try {
            M3Stats.onNativeCall();
            return _nGetScaleX(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @return  text skew on x-axis. Default value is 0
     */
    public float getSkewX() {
        try {
            M3Stats.onNativeCall();
            return _nGetSkewX(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Sets Typeface to typeface. Pass null to use the default typeface.
     */
    public M3Font setTypeface(M3Typeface typeface) {
        try {
            _resetMetrics();
            M3Stats.onNativeCall();
            _nSetTypeface(_ptr, M3Native.getPtr(typeface));
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(typeface);
        }
    }

    /**
     * Sets text size in points. Has no effect if value is not greater than or equal to zero.
     */
    public M3Font setSize(float value) {
        _resetMetrics();
        M3Stats.onNativeCall();
        _nSetSize(_ptr, value);
        return this;
    }

    /**
     * Sets text scale on x-axis. Default value is 1.
     */
    public M3Font setScaleX(float value) {
        _resetMetrics();
        M3Stats.onNativeCall();
        _nSetScaleX(_ptr, value);
        return this;
    }

    /**
     * Sets text skew on x-axis. Default value is 0.
     */
    public M3Font setSkewX(float value) {
        _resetMetrics();
        M3Stats.onNativeCall();
        _nSetSkewX(_ptr, value);
        return this;
    }

    /**
     *  Converts text into glyph indices.
     *
     *  @return  the corresponding glyph ids for each character.
     */
    public short[] getStringGlyphs(String s) {
        return getUTF32Glyphs(s.codePoints().toArray());
    }

    /**
     *  Given an array of UTF32 character codes, return their corresponding glyph IDs.
     *
     *  @return  the corresponding glyph IDs for each character.
     */
    public short[] getUTF32Glyphs(int[] uni) {
        try {
            M3Stats.onNativeCall();
            return _nGetUTF32Glyphs(_ptr, uni);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @return  the glyph that corresponds to the specified unicode code-point (in UTF32 encoding). If the unichar is not supported, returns 0
     */
    public short getUTF32Glyph(int unichar) {
        try {
            M3Stats.onNativeCall();
            return _nGetUTF32Glyph(_ptr, unichar);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @return  number of glyphs represented by text
     */
    public int getStringGlyphsCount(String s) {
        try {
            M3Stats.onNativeCall();
            return _nGetStringGlyphsCount(_ptr, s);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @return  the bounding box of text
     */
    public M3Rect measureText(String s) {
        return measureText(s, null);
    }

    /**
     * @param p  stroke width or PathEffect may modify the advance with
     * @return   the bounding box of text
     */
    public M3Rect measureText(String s, M3Paint p) {
        try {
            M3Stats.onNativeCall();
            return _nMeasureText(_ptr, s, M3Native.getPtr(p));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(p);
        }
    }

    public float measureTextWidth(String s) {
        M3Stats.onNativeCall();
        return measureTextWidth(s, null);
    }

    public float measureTextWidth(String s, M3Paint p) {
        try {
            M3Stats.onNativeCall();
            return _nMeasureTextWidth(_ptr, s, M3Native.getPtr(p));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(p);
        }
    }

    /**
     * Retrieves the advances for each glyph
     */
    public float[] getWidths(short[] glyphs) {
        try {
            M3Stats.onNativeCall();
            return _nGetWidths(_ptr, glyphs);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Retrieves the bounds for each glyph
     */
    public M3Rect[] getBounds(short[] glyphs) {
        return getBounds(glyphs, null);
    }

    /**
     * Retrieves the bounds for each glyph
     */
    public M3Rect[] getBounds(short[] glyphs, M3Paint p) {
        try {
            M3Stats.onNativeCall();
            return _nGetBounds(_ptr, glyphs, M3Native.getPtr(p));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(p);
        }
    }

    /**
     * Retrieves the positions for each glyph.
     */
    public M3Point[] getPositions(short[] glyphs) {
        try {
            M3Stats.onNativeCall();
            return _nGetPositions(_ptr, glyphs, 0, 0);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Retrieves the positions for each glyph, beginning at the specified origin.
     */
    public M3Point[] getPositions(short[] glyphs, M3Point offset) {
        try {
            M3Stats.onNativeCall();
            return _nGetPositions(_ptr, glyphs, offset._x, offset._y);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Retrieves the x-positions for each glyph.
     */
    public float[] getXPositions(short[] glyphs) {
        try {
            M3Stats.onNativeCall();
            return _nGetXPositions(_ptr, glyphs, 0);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Retrieves the x-positions for each glyph, beginning at the specified origin.
     */
    public float[] getXPositions(short[] glyphs, float offset) {
        try {
            M3Stats.onNativeCall();
            return _nGetXPositions(_ptr, glyphs, offset);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * If the glyph has an outline, returns it. The glyph outline may be empty.
     * Degenerate contours in the glyph outline will be skipped. If glyph is described by a bitmap, returns null.
     */
    public M3Path getPath(short glyph) {
        try {
            M3Stats.onNativeCall();
            long ptr = _nGetPath(_ptr, glyph);
            return ptr == 0 ? null : new M3Path(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Return glyph outlines, some of which might be null.
     */
    public M3Path[] getPaths(short[] glyphs) {
        try {
            M3Stats.onNativeCall();
            return _nGetPaths(_ptr, glyphs);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns FontMetrics associated with Typeface. Results are scaled by text size but does not take into account
     * dimensions required by text scale, text skew, fake bold, style stroke, and {@link PathEffect}.
     */
    public M3FontMetrics getMetrics() {
        try {
            if (_metrics == null) {
                M3Stats.onNativeCall();
                _metrics = _nGetMetrics(_ptr);
            }
            return _metrics;
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns the recommended spacing between lines: the sum of metrics descent, ascent, and leading.
     * Result is scaled by text size but does not take into account dimensions required by stroking and SkPathEffect.
     */
    public float getSpacing() {
        try {
            if (_spacing == null) {
                M3Stats.onNativeCall();
                _spacing = _nGetSpacing(_ptr);
            }
            return _spacing;
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public static class _FinalizerHolder {
        public static final long PTR = _nGetFinalizer();
    }

    public static native long    _nGetFinalizer();
    public static native long    _nMakeDefault();
    public static native long    _nMakeTypeface(long typefacePtr);
    public static native long    _nMakeTypefaceSize(long typefacePtr, float size);
    public static native long    _nMakeTypefaceSizeScaleSkew(long typefacePtr, float size, float scaleX, float skewX);
    public static native long    _nMakeClone(long ptr);
    public static native boolean _nEquals(long ptr, long otherPtr);
    public static native boolean _nIsAutoHintingForced(long ptr);
    public static native boolean _nAreBitmapsEmbedded(long ptr);
    public static native boolean _nIsSubpixel(long ptr);
    public static native boolean _nAreMetricsLinear(long ptr);
    public static native boolean _nIsEmboldened(long ptr);
    public static native boolean _nIsBaselineSnapped(long ptr);
    public static native void    _nSetAutoHintingForced(long ptr, boolean value);
    public static native void    _nSetBitmapsEmbedded(long ptr, boolean value);
    public static native void    _nSetSubpixel(long ptr, boolean value);
    public static native void    _nSetMetricsLinear(long ptr, boolean value);
    public static native void    _nSetEmboldened(long ptr, boolean value);
    public static native void    _nSetBaselineSnapped(long ptr, boolean value);
    public static native int     _nGetEdging(long ptr);
    public static native void    _nSetEdging(long ptr, int value);
    public static native int     _nGetHinting(long ptr);
    public static native void    _nSetHinting(long ptr, int value);
    public static native long    _nGetTypeface(long ptr);
    public static native float   _nGetSize(long ptr);
    public static native float   _nGetScaleX(long ptr);
    public static native float   _nGetSkewX(long ptr);
    public static native void    _nSetTypeface(long ptr, long typefacePtr);
    public static native void    _nSetSize(long ptr, float value);
    public static native void    _nSetScaleX(long ptr, float value);
    public static native void    _nSetSkewX(long ptr, float value);
    public static native short[] _nGetStringGlyphs(long ptr, String str);
    public static native short   _nGetUTF32Glyph(long ptr, int uni);
    public static native short[] _nGetUTF32Glyphs(long ptr, int[] uni);
    public static native int     _nGetStringGlyphsCount(long ptr, String str);
    public static native M3Rect    _nMeasureText(long ptr, String str, long paintPtr);
    public static native float   _nMeasureTextWidth(long ptr, String str, long paintPtr);
    public static native float[] _nGetWidths(long ptr, short[] glyphs);
    public static native M3Rect[]  _nGetBounds(long ptr, short[] glyphs, long paintPtr);
    public static native M3Point[] _nGetPositions(long ptr, short[] glyphs, float x, float y);
    public static native float[] _nGetXPositions(long ptr, short[] glyphs, float x);
    public static native long    _nGetPath(long ptr, short glyph);
    public static native M3Path[]  _nGetPaths(long ptr, short[] glyphs);
    public static native M3FontMetrics _nGetMetrics(long ptr);
    public static native float   _nGetSpacing(long ptr);
}