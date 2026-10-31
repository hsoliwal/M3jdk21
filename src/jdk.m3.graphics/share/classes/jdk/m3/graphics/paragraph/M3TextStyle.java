package jdk.m3.graphics.paragraph;

import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.*;
import java.util.*;

public class M3TextStyle extends M3Managed {
    static { M3Library.staticLoad(); }
    public M3TextStyle(long ptr) {
        super(ptr, _FinalizerHolder.PTR);
    }

    public M3TextStyle() {
        this(_nMake());
        M3Stats.onNativeCall();
    }

    @Override
    public boolean _nativeEquals(M3Native other) {
        try {
            M3Stats.onNativeCall();
            return _nEquals(_ptr, M3Native.getPtr(other));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(other);
        }
    }

    public boolean equals(M3TextStyleAttribute attribute, M3TextStyle other) {
        try {
            M3Stats.onNativeCall();
            return _nAttributeEquals(_ptr, attribute.ordinal(), M3Native.getPtr(other));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(other);
        }
    }

    public boolean equalsByFonts(M3TextStyle other) {
        try {
            M3Stats.onNativeCall();
            return _nEqualsByFonts(_ptr, M3Native.getPtr(other));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(other);
        }
    }

    public int getColor() {
        try {
            M3Stats.onNativeCall();
            return _nGetColor(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3TextStyle setColor(int color) {
        M3Stats.onNativeCall();
        _nSetColor(_ptr, color);
        return this;
    }

    public M3Paint getForeground() {
        try {
            M3Stats.onNativeCall();
            long ptr = _nGetForeground(_ptr);
            return ptr == 0 ? null : new M3Paint(ptr, true);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3TextStyle setForeground(M3Paint paint) {
        try {
            M3Stats.onNativeCall();
            _nSetForeground(_ptr, M3Native.getPtr(paint));
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(paint);
        }
    }

    public M3Paint getBackground() {
        try {
            M3Stats.onNativeCall();
            long ptr = _nGetBackground(_ptr);
            return ptr == 0 ? null : new M3Paint(ptr, true);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3TextStyle setBackground(M3Paint paint) {
        try {
            M3Stats.onNativeCall();
            _nSetBackground(_ptr, M3Native.getPtr(paint));
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(paint);
        }
    }

    public M3DecorationStyle getDecorationStyle() {
        try {
            M3Stats.onNativeCall();
            return _nGetDecorationStyle(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3TextStyle setDecorationStyle(M3DecorationStyle d) {
        M3Stats.onNativeCall();
        _nSetDecorationStyle(_ptr, d._underline, d._overline, d._lineThrough, d._gaps, d._color, d._lineStyle.ordinal(), d._thicknessMultiplier);
        return this;
    }

    public M3FontStyle getFontStyle() {
        try {
            M3Stats.onNativeCall();
            return new M3FontStyle(_nGetFontStyle(_ptr));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3TextStyle setFontStyle(M3FontStyle s) {
        M3Stats.onNativeCall();
        _nSetFontStyle(_ptr, s._value);
        return this;
    }

    public M3Shadow[] getShadows() {
        try {
            M3Stats.onNativeCall();
            return _nGetShadows(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3TextStyle addShadow(M3Shadow s) {
        M3Stats.onNativeCall();
        _nAddShadow(_ptr, s._color, s._offsetX, s._offsetY, s._blurSigma);
        return this;
    }

    public M3TextStyle addShadows(M3Shadow[] shadows) {
        for (M3Shadow s: shadows)
            addShadow(s);
        return this;
    }

    public M3TextStyle clearShadows() {
        M3Stats.onNativeCall();
        _nClearShadows(_ptr);
        return this;
    }

    public M3FontFeature[] getFontFeatures() {
        try {
            M3Stats.onNativeCall();
            return _nGetFontFeatures(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3TextStyle addFontFeature(M3FontFeature f) {
        M3Stats.onNativeCall();
        _nAddFontFeature(_ptr, f.getTag(), f._value);
        return this;
    }

    public M3TextStyle addFontFeatures(M3FontFeature[] FontFeatures) {
        for (M3FontFeature s: FontFeatures)
            addFontFeature(s);
        return this;
    }

    public M3TextStyle clearFontFeatures() {
        M3Stats.onNativeCall();
        _nClearFontFeatures(_ptr);
        return this;
    }

    public float getFontSize() {
        try {
            M3Stats.onNativeCall();
            return _nGetFontSize(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3TextStyle setFontSize(float size) {
        M3Stats.onNativeCall();
        _nSetFontSize(_ptr, size);
        return this;
    }

    public String[] getFontFamilies() {
        try {
            M3Stats.onNativeCall();
            return _nGetFontFamilies(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3TextStyle setFontFamily(String family) {
        return setFontFamilies(new String[] { family });
    }

    public M3TextStyle setFontFamilies(String[] families) {
        assert Arrays.stream(families).allMatch(Objects::nonNull) : "Can't setFontFamilies with null families elements";
        M3Stats.onNativeCall();
        _nSetFontFamilies(_ptr, families);
        return this;
    }

    public Float getHeight() {
        try {
            M3Stats.onNativeCall();
            return _nGetHeight(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3TextStyle setHeight(Float height) {
        M3Stats.onNativeCall();
        if (height == null)
            _nSetHeight(_ptr, false, 0);
        else
            _nSetHeight(_ptr, true, height);
        return this;
    }

    public float getLetterSpacing() {
        try {
            M3Stats.onNativeCall();
            return _nGetLetterSpacing(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3TextStyle setLetterSpacing(float letterSpacing) {
        M3Stats.onNativeCall();
        _nSetLetterSpacing(_ptr, letterSpacing);
        return this;
    }

    public float getWordSpacing() {
        try {
            M3Stats.onNativeCall();
            return _nGetWordSpacing(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3TextStyle setWordSpacing(float wordSpacing) {
        M3Stats.onNativeCall();
        _nSetWordSpacing(_ptr, wordSpacing);
        return this;
    }

    public M3Typeface getTypeface() {
        try {
            M3Stats.onNativeCall();
            long ptr = _nGetTypeface(_ptr);
            return ptr == 0 ? null : new M3Typeface(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3TextStyle setTypeface(M3Typeface typeface) {
        try {
            M3Stats.onNativeCall();
            _nSetTypeface(_ptr, M3Native.getPtr(typeface));
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(typeface);
        }
    }

    public String getLocale() {
        try {
            M3Stats.onNativeCall();
            return _nGetLocale(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3TextStyle setLocale(String locale) {
        assert locale != null : "Can't setLocale with locale == null";
        M3Stats.onNativeCall();
        _nSetLocale(_ptr, locale);
        return this;
    }

    public M3BaselineMode getBaselineMode() {
        try {
            M3Stats.onNativeCall();
            return M3BaselineMode._values[_nGetBaselineMode(_ptr)];
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3TextStyle setBaselineMode(M3BaselineMode baseline) {
        M3Stats.onNativeCall();
        _nSetBaselineMode(_ptr, baseline.ordinal());
        return this;
    }

    public M3FontMetrics getFontMetrics() {
        try {
            M3Stats.onNativeCall();
            return _nGetFontMetrics(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public boolean isPlaceholder() {
        try {
            M3Stats.onNativeCall();
            return _nIsPlaceholder(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3TextStyle setPlaceholder() {
        M3Stats.onNativeCall();
        _nSetPlaceholder(_ptr);
        return this;
    }

    public static class _FinalizerHolder {
        public static final long PTR = _nGetFinalizer();
    }

    public static native long  _nGetFinalizer();
    public static native long  _nMake();
    public static native boolean _nEquals(long ptr, long otherPtr);
    public static native boolean _nAttributeEquals(long ptr, int attribute, long otherPtr);
    public static native boolean _nEqualsByFonts(long ptr, long otherPtr);
    public static native int   _nGetColor(long ptr);
    public static native void  _nSetColor(long ptr,int color);
    public static native long  _nGetForeground(long ptr);
    public static native void  _nSetForeground(long ptr, long paintPtr);
    public static native long  _nGetBackground(long ptr);
    public static native void  _nSetBackground(long ptr, long paintPtr);
    public static native M3DecorationStyle _nGetDecorationStyle(long ptr);
    public static native void  _nSetDecorationStyle(long ptr, boolean underline, boolean overline, boolean lineThrough, boolean gaps, int color, int style, float thicknessMultiplier);
    public static native int   _nGetFontStyle(long ptr);
    public static native void  _nSetFontStyle(long ptr, int fontStyle);
    public static native M3Shadow[] _nGetShadows(long ptr);
    public static native void  _nAddShadow(long ptr, int color, float offsetX, float offsetY, double blurSigma);
    public static native void  _nClearShadows(long ptr);
    public static native M3FontFeature[] _nGetFontFeatures(long ptr);
    public static native void  _nAddFontFeature(long ptr, String name, int value);
    public static native void  _nClearFontFeatures(long ptr);
    public static native float _nGetFontSize(long ptr);
    public static native void  _nSetFontSize(long ptr, float size);
    public static native String[] _nGetFontFamilies(long ptr);
    public static native void  _nSetFontFamilies(long ptr, String[] families);
    public static native Float _nGetHeight(long ptr);
    public static native void  _nSetHeight(long ptr, boolean override, float height);
    public static native float _nGetLetterSpacing(long ptr);
    public static native void  _nSetLetterSpacing(long ptr, float letterSpacing);
    public static native float _nGetWordSpacing(long ptr);
    public static native void  _nSetWordSpacing(long ptr, float wordSpacing);
    public static native long  _nGetTypeface(long ptr);
    public static native void  _nSetTypeface(long ptr, long typefacePtr);
    public static native String _nGetLocale(long ptr);
    public static native void  _nSetLocale(long ptr, String locale);
    public static native int   _nGetBaselineMode(long ptr);
    public static native void  _nSetBaselineMode(long ptr, int mode);
    public static native M3FontMetrics _nGetFontMetrics(long ptr);
    public static native boolean _nIsPlaceholder(long ptr);
    public static native void  _nSetPlaceholder(long ptr);
}