package jdk.m3.graphics.paragraph;

import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.*;

public class M3TypefaceFontProvider extends M3FontMgr {
    static { M3Library.staticLoad(); }
    public M3TypefaceFontProvider() {
        super(_nMake());
        M3Stats.onNativeCall();
    }

    public M3TypefaceFontProvider registerTypeface(M3Typeface typeface) {
        return registerTypeface(typeface, null);
    }

    public M3TypefaceFontProvider registerTypeface(M3Typeface typeface, String alias) {
        try {
            M3Stats.onNativeCall();
            _nRegisterTypeface(_ptr, M3Native.getPtr(typeface), alias);
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(typeface);
        }
    }

    public static native long _nMake();
    public static native long _nRegisterTypeface(long ptr, long typefacePtr, String alias);
}