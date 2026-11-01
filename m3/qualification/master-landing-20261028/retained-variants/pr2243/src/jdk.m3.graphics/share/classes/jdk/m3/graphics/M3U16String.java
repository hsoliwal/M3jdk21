package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;

/**
 * Java mirror of std::vector&lt;jchar&gt; (UTF-16)
 */
public class M3U16String extends M3Managed {
    static { M3Library.staticLoad(); }
    public M3U16String(long ptr) {
        super(ptr, _FinalizerHolder.PTR);
    }

    public M3U16String(String s) {
        this(_nMake(s));
        M3Stats.onNativeCall();
    }

    @Override
    public String toString() {
        try {
            M3Stats.onNativeCall();
            return _nToString(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public static class _FinalizerHolder {
        public static final long PTR = _nGetFinalizer();
    }

    public static native long _nMake(String s);
    public static native long _nGetFinalizer();
    public static native String _nToString(long ptr);
}