package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;

public class M3FontStyleSet extends M3RefCnt {
    static { M3Library.staticLoad(); }
    public static M3FontStyleSet makeEmpty() {
        M3Stats.onNativeCall();
        return new M3FontStyleSet(_nMakeEmpty());
    }

    public int count() {
        try {
            M3Stats.onNativeCall();
            return _nCount(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3FontStyle getStyle(int index) {
        try {
            M3Stats.onNativeCall();
            return new M3FontStyle(_nGetStyle(_ptr, index));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public String getStyleName(int index) {
        try {
            M3Stats.onNativeCall();
            return _nGetStyleName(_ptr, index);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3Typeface getTypeface(int index) {
        try {
            M3Stats.onNativeCall();
            long ptr = _nGetTypeface(_ptr, index);
            return ptr == 0 ? null : new M3Typeface(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3Typeface matchStyle(M3FontStyle style) {
        try {
            M3Stats.onNativeCall();
            long ptr = _nMatchStyle(_ptr, style._value);
            return ptr == 0 ? null : new M3Typeface(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3FontStyleSet(long ptr) {
        super(ptr);
    }

    public static native long _nMakeEmpty();
    public static native int _nCount(long ptr);
    public static native int _nGetStyle(long ptr, int index);
    public static native String _nGetStyleName(long ptr, int index);
    public static native long _nGetTypeface(long ptr, int index);
    public static native long _nMatchStyle(long ptr, int style);
}