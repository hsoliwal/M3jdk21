package jdk.m3.graphics.paragraph;

import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.*;

public class M3ParagraphStyle extends M3Managed {
    static { M3Library.staticLoad(); }
    public M3ParagraphStyle() {
        super(_nMake(), _FinalizerHolder.PTR);
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

    public M3StrutStyle getStrutStyle() {
        try {
            M3Stats.onNativeCall();
            return new M3StrutStyle(_nGetStrutStyle(_ptr));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3ParagraphStyle setStrutStyle(M3StrutStyle s) {
        try {
            M3Stats.onNativeCall();
            _nSetStrutStyle(_ptr, M3Native.getPtr(s));
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(s);
        }
    }

    public M3TextStyle getTextStyle() {
        try {
            M3Stats.onNativeCall();
            return new M3TextStyle(_nGetTextStyle(_ptr));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3ParagraphStyle setTextStyle(M3TextStyle style) {
        try {
            M3Stats.onNativeCall();
            _nSetTextStyle(_ptr, M3Native.getPtr(style));
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(style);
        }
    }

    public M3Direction getDirection() {
        try {
            M3Stats.onNativeCall();
            return M3Direction._values[_nGetDirection(_ptr)];
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3ParagraphStyle setDirection(M3Direction style) {
        M3Stats.onNativeCall();
        _nSetDirection(_ptr, style.ordinal());
        return this;
    }

    public M3Alignment getAlignment() {
        try {
            M3Stats.onNativeCall();
            return M3Alignment._values[_nGetAlignment(_ptr)];
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3ParagraphStyle setAlignment(M3Alignment alignment) {
        M3Stats.onNativeCall();
        _nSetAlignment(_ptr, alignment.ordinal());
        return this;
    }

    public long getMaxLinesCount() {
        try {
            M3Stats.onNativeCall();
            return _nGetMaxLinesCount(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3ParagraphStyle setMaxLinesCount(long count) {
        M3Stats.onNativeCall();
        _nSetMaxLinesCount(_ptr, count);
        return this;
    }

    public String getEllipsis() {
        try {
            M3Stats.onNativeCall();
            return _nGetEllipsis(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3ParagraphStyle setEllipsis(String ellipsis) {
        assert ellipsis != null : "Can't setEllipsis with ellipsis == null";
        M3Stats.onNativeCall();
        _nSetEllipsis(_ptr, ellipsis);
        return this;
    }

    public float getHeight() {
        try {
            M3Stats.onNativeCall();
            return _nGetHeight(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3ParagraphStyle setHeight(float height) {
        M3Stats.onNativeCall();
        _nSetHeight(_ptr, height);
        return this;
    }

    public M3HeightMode getHeightMode() {
        try {
            M3Stats.onNativeCall();
            return M3HeightMode._values[_nGetHeightMode(_ptr)];
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3ParagraphStyle setHeightMode(M3HeightMode behavior) {
        M3Stats.onNativeCall();
        _nSetHeightMode(_ptr, behavior.ordinal());
        return this;
    }

    public M3Alignment getEffectiveAlignment() {
        try {
            M3Stats.onNativeCall();
            return M3Alignment._values[_nGetEffectiveAlignment(_ptr)];
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public boolean isHintingEnabled() {
        try {
            M3Stats.onNativeCall();
            return _nIsHintingEnabled(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3ParagraphStyle disableHinting() {
        M3Stats.onNativeCall();
        _nDisableHinting(_ptr);
        return this;
    }

    public static class _FinalizerHolder {
        public static final long PTR = _nGetFinalizer();
    }

    public static native long    _nGetFinalizer();
    public static native long    _nMake();
    public static native boolean _nEquals(long ptr, long otherPtr);
    public static native long    _nGetStrutStyle(long ptr);
    public static native void    _nSetStrutStyle(long ptr, long stylePtr);
    public static native long    _nGetTextStyle(long ptr);
    public static native void    _nSetTextStyle(long ptr, long textStylePtr);
    public static native int     _nGetDirection(long ptr);
    public static native void    _nSetDirection(long ptr, int direction);
    public static native int     _nGetAlignment(long ptr);
    public static native void    _nSetAlignment(long ptr, int align);
    public static native long    _nGetMaxLinesCount(long ptr);
    public static native void    _nSetMaxLinesCount(long ptr, long maxLines);
    public static native String  _nGetEllipsis(long ptr);
    public static native void    _nSetEllipsis(long ptr, String ellipsis);
    public static native float   _nGetHeight(long ptr);
    public static native void    _nSetHeight(long ptr, float height);
    public static native int     _nGetHeightMode(long ptr);
    public static native void    _nSetHeightMode(long ptr, int v);
    public static native int     _nGetEffectiveAlignment(long ptr);
    public static native boolean _nIsHintingEnabled(long ptr);
    public static native void    _nDisableHinting(long ptr);
}