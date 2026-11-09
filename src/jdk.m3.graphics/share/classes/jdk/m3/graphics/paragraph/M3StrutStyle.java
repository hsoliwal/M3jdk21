package jdk.m3.graphics.paragraph;

import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.*;
import java.util.*;

public class M3StrutStyle extends M3Managed {
    static { M3Library.staticLoad(); }
    public M3StrutStyle(long ptr) {
        super(ptr, _FinalizerHolder.PTR);
    }

    public M3StrutStyle() {
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

    public String[] getFontFamilies() {
        try {
            M3Stats.onNativeCall();
            return _nGetFontFamilies(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3StrutStyle setFontFamilies(String[] families) {
        assert Arrays.stream(families).allMatch(Objects::nonNull) : "Can't setFontFamilies with null families elements";
        M3Stats.onNativeCall();
        _nSetFontFamilies(_ptr, families);
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

    public M3StrutStyle setFontStyle(M3FontStyle style) {
        M3Stats.onNativeCall();
        _nSetFontStyle(_ptr, style._value);
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

    public M3StrutStyle setFontSize(float value) {
        M3Stats.onNativeCall();
        _nSetFontSize(_ptr, value);
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

    public M3StrutStyle setHeight(float value) {
        M3Stats.onNativeCall();
        _nSetHeight(_ptr, value);
        return this;
    }

    public float getLeading() {
        try {
            M3Stats.onNativeCall();
            return _nGetLeading(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3StrutStyle setLeading(float value) {
        M3Stats.onNativeCall();
        _nSetLeading(_ptr, value);
        return this;
    }

    public boolean isEnabled() {
        try {
            M3Stats.onNativeCall();
            return _nIsEnabled(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3StrutStyle setEnabled(boolean value) {
        M3Stats.onNativeCall();
        _nSetEnabled(_ptr, value);
        return this;
    }

    public boolean isHeightForced() {
        try {
            M3Stats.onNativeCall();
            return _nIsHeightForced(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3StrutStyle setHeightForced(boolean value) {
        M3Stats.onNativeCall();
        _nSetHeightForced(_ptr, value);
        return this;
    }

    public boolean isHeightOverridden() {
        try {
            M3Stats.onNativeCall();
            return _nIsHeightOverridden(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3StrutStyle setHeightOverridden(boolean value) {
        M3Stats.onNativeCall();
        _nSetHeightOverridden(_ptr, value);
        return this;
    }

    public static class _FinalizerHolder {
        public static final long PTR = _nGetFinalizer();
    }

    public static native long     _nGetFinalizer();
    public static native long     _nMake();
    public static native boolean  _nEquals(long ptr, long otherPtr);
    public static native String[] _nGetFontFamilies(long ptr);
    public static native void     _nSetFontFamilies(long ptr, String[] families);
    public static native int      _nGetFontStyle(long ptr);
    public static native void     _nSetFontStyle(long ptr, int value);
    public static native float    _nGetFontSize(long ptr);
    public static native void     _nSetFontSize(long ptr, float value);
    public static native float    _nGetHeight(long ptr);
    public static native void     _nSetHeight(long ptr, float value);
    public static native float    _nGetLeading(long ptr);
    public static native void     _nSetLeading(long ptr, float value);
    public static native boolean  _nIsEnabled(long ptr);
    public static native void     _nSetEnabled(long ptr, boolean value);
    public static native boolean  _nIsHeightForced(long ptr);
    public static native void     _nSetHeightForced(long ptr, boolean value);
    public static native boolean  _nIsHeightOverridden(long ptr);
    public static native void     _nSetHeightOverridden(long ptr, boolean value);
}