package jdk.m3.graphics;

import java.util.*;
import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.*;

public class M3Typeface extends M3RefCnt {
    static { M3Library.staticLoad(); }
    /**
     * @return  the typeface’s intrinsic style attributes
     */
    public M3FontStyle getFontStyle() {
        try {
            M3Stats.onNativeCall();
            return new M3FontStyle(_nGetFontStyle(_ptr));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @return  true if {@link #getFontStyle()} has the bold bit set
     */
    public boolean isBold() {
        return getFontStyle().getWeight() >= M3FontWeight.SEMI_BOLD;
    }

    /**
     * @return  true if {@link #getFontStyle()} has the italic bit set
     */
    public boolean isItalic() {
        return getFontStyle().getSlant() != M3FontSlant.UPRIGHT;
    }

    /**
     * This is a style bit, advance widths may vary even if this returns true.
     *
     * @return  true if the typeface claims to be fixed-pitch
     */
    public boolean isFixedPitch() {
        try {
            M3Stats.onNativeCall();
            return _nIsFixedPitch(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * It is possible the number of axes can be retrieved but actual position cannot.
     *
     * @return  the variation coordinates describing the position of this typeface in design variation space, null if there’s no variations
     */
    public M3FontVariation[] getVariations() {
        try {
            M3Stats.onNativeCall();
            return _nGetVariations(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

        /**
     * It is possible the number of axes can be retrieved but actual position cannot.
     *
     * @return  the variation coordinates describing the position of this typeface in design variation space, null if there’s no variations
     */
    public M3FontVariationAxis[] getVariationAxes() {
        try {
            M3Stats.onNativeCall();
            return _nGetVariationAxes(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @return  a 32bit value for this typeface, unique for the underlying font data. Never 0
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
     * @return  true if the two typefaces reference the same underlying font, treating null as the default font
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

    /**
     * Return a new typeface based on this typeface but parameterized as specified in the
     * variation. If the variation does not supply an argument for a parameter
     * in the font then the value from this typeface will be used as the value for that argument.
     *
     * @param   variation  axis position
     * @return  same typeface if variation already matches, new typeface otherwise
     * @throws IllegalArgumentException  on failure
     */
    public M3Typeface makeClone(M3FontVariation variation) {
        return makeClone(M3FontArguments.DEFAULT.withVariations(new M3FontVariation[] { variation }));
    }

    /**
     * Return a new typeface based on this typeface but parameterized as specified in the
     * variations. If the variations does not supply an argument for a parameter
     * in the font then the value from this typeface will be used as the value for that argument.
     *
     * @param  variations  axis positions
     * @return  same typeface if all variation already match, new typeface otherwise
     * @throws IllegalArgumentException  on failure
     */
    public M3Typeface makeClone(M3FontVariation[] variations) {
        return makeClone(M3FontArguments.DEFAULT.withVariations(variations));
    }

    /**
     * Return a new typeface based on this typeface but parameterized as specified in the
     * font arguments. If the arguments does not supply a value for a parameter
     * in the font then the value from this typeface will be used as the value for that argument.
     *
     * @param   fontArguments  new typeface parameters
     * @return  same typeface if all arguments already match, new typeface otherwise
     * @throws IllegalArgumentException  on failure
     */
    public M3Typeface makeClone(M3FontArguments fontArguments) {
        try {
            if (fontArguments.equals(M3FontArguments.DEFAULT))
                return this;
            M3Stats.onNativeCall();
            long ptr = _nMakeClone(_ptr, fontArguments);
            if (ptr == 0)
                throw new IllegalArgumentException("Failed to clone Typeface " + this + " with " + fontArguments);
            return new M3Typeface(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(fontArguments);
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     *  Given a string, returns corresponding glyph ids.
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
     *  This is a short-cut for calling {@link #getUTF32Glyphs(int[])}.
     *  @return  the glyph that corresponds to the specified unicode code-point (in UTF32 encoding). If the unichar is not supported, returns 0
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
     * @return  the number of glyphs in the typeface
     */
    public int getGlyphsCount() {
        try {
            M3Stats.onNativeCall();
            return _nGetGlyphsCount(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @return  the number of tables in the font
     */
    public int getTablesCount() {
        try {
            M3Stats.onNativeCall();
            return _nGetTablesCount(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @return  the list of table tags in the font
     */
    public String[] getTableTags() {
        try {
            M3Stats.onNativeCall();
            return Arrays.stream(_nGetTableTags(_ptr)).mapToObj(M3FourByteTag::toString).toArray(String[]::new);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Given a table tag, return the size of its contents, or 0 if not present
     */
    public long getTableSize(String tag) {
        try {
            M3Stats.onNativeCall();
            return _nGetTableSize(_ptr, M3FourByteTag.fromString(tag));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Return an immutable copy of the requested font table, or null if that table was
     * not found.
     *
     * @param tag  The table tag whose contents are to be copied
     * @return     an immutable copy of the table's data, or null
     */
    public M3Data getTableData(String tag) {
        try {
            M3Stats.onNativeCall();
            long ptr = _nGetTableData(_ptr, M3FourByteTag.fromString(tag));
            return ptr == 0 ? null : new M3Data(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @return  the units-per-em value for this typeface, or zero if there is an error
     */
    public int getUnitsPerEm() {
        try {
            M3Stats.onNativeCall();
            return _nGetUnitsPerEm(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Given a run of glyphs, return the associated horizontal adjustments.
     * Adjustments are in "design units", which are integers relative to the
     * typeface's units per em (see {@link #getUnitsPerEm()}).
     *
     * Some typefaces are known to never support kerning. Calling this with null,
     * if it returns null then it will always return null (no kerning) for all
     * possible glyph runs. If it returns int[0], then it *may* return non-null
     * adjustments for some glyph runs.
     *
     * @return  adjustment array (one less than glyphs), or null if no kerning should be applied
     */
    public int[] getKerningPairAdjustments(short[] glyphs) {
        try {
            M3Stats.onNativeCall();
            return _nGetKerningPairAdjustments(_ptr, glyphs);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @return  all of the family names specified by the font
     */
    public M3FontFamilyName[] getFamilyNames() {
        try {
            M3Stats.onNativeCall();
            return _nGetFamilyNames(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @return  the family name for this typeface. The language of the name is whatever the host platform chooses
     */
    public String getFamilyName() {
        try {
            M3Stats.onNativeCall();
            return _nGetFamilyName(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Return a rectangle (scaled to 1-pt) that represents the union of the bounds of all
     * of the glyphs, but each one positioned at (0,). This may be conservatively large, and
     * will not take into account any hinting or other size-specific adjustments.
     */
    public M3Rect getBounds() {
        try {
            M3Stats.onNativeCall();
            return _nGetBounds(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Opens a stream for reading the data of this typeface.
     * @return  a StreamAsset for the typeface data, or null if not available
     */
    public M3StreamAsset openStream(){
        return openStream(null);
    }

   /**
     * Opens a stream for reading the data of this typeface.
     * @param ttcIndex  if not null, will be filled with the TTC index (for TrueType Collections)
     * @return  a StreamAsset for the typeface data, or null if not available
     */
    public M3StreamAsset openStream(int[] ttcIndex) {
        try {
            M3Stats.onNativeCall();
            long ptr = _nOpenStream(_ptr, ttcIndex);
            return ptr == 0 ? null : new M3StreamAsset(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3Typeface(long ptr) {
        super(ptr);
    }

    public static native int      _nGetFontStyle(long ptr);
    public static native boolean  _nIsFixedPitch(long ptr);
    public static native M3FontVariation[] _nGetVariations(long ptr);
    public static native M3FontVariationAxis[] _nGetVariationAxes(long ptr);
    public static native int      _nGetUniqueId(long ptr);
    public static native boolean  _nEquals(long ptr, long otherPtr);
    public static native long     _nMakeFromName(String name, int fontStyle);
    public static native long     _nMakeFromFile(String path, int index);
    public static native long     _nMakeFromData(long dataPtr, int index);
    public static native long     _nMakeClone(long ptr, M3FontArguments fontArguments);
    public static native short[]  _nGetUTF32Glyphs(long ptr, int[] uni);
    public static native short    _nGetUTF32Glyph(long ptr, int unichar);
    public static native int      _nGetGlyphsCount(long ptr);
    public static native int      _nGetTablesCount(long ptr);
    public static native int[]    _nGetTableTags(long ptr);
    public static native long     _nGetTableSize(long ptr, int tag);
    public static native long     _nGetTableData(long ptr, int tag);
    public static native int      _nGetUnitsPerEm(long ptr);
    public static native int[]    _nGetKerningPairAdjustments(long ptr, short[] glyphs);
    public static native M3FontFamilyName[] _nGetFamilyNames(long ptr);
    public static native String   _nGetFamilyName(long ptr);
    public static native M3Rect     _nGetBounds(long ptr);
    public static native long     _nOpenStream(long ptr, int[] ttcIndex);
}