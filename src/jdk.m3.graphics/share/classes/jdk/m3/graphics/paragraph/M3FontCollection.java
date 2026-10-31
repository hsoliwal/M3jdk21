package jdk.m3.graphics.paragraph;

import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.*;
import java.util.*;

public class M3FontCollection extends M3RefCnt {
    static { M3Library.staticLoad(); }
    public M3FontCollection() {
        this(_nMake());
        M3Stats.onNativeCall();
    }

    public long getFontManagersCount() {
        try {
            M3Stats.onNativeCall();
            return _nGetFontManagersCount(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }
    public M3FontCollection setAssetFontManager(M3FontMgr fontMgr) {
        try {
            M3Stats.onNativeCall();
            _nSetAssetFontManager(_ptr, M3Native.getPtr(fontMgr));
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(fontMgr);
        }
    }

    public M3FontCollection setDynamicFontManager(M3FontMgr fontMgr) {
        try {
            M3Stats.onNativeCall();
            _nSetDynamicFontManager(_ptr, M3Native.getPtr(fontMgr));
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(fontMgr);
        }
    }

    public M3FontCollection setTestFontManager(M3FontMgr fontMgr) {
        try {
            M3Stats.onNativeCall();
            _nSetTestFontManager(_ptr, M3Native.getPtr(fontMgr));
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(fontMgr);
        }
    }

    public M3FontCollection setDefaultFontManager(M3FontMgr fontMgr) {
        return setDefaultFontManager(fontMgr, null);
    }

    public M3FontCollection setDefaultFontManager(M3FontMgr fontMgr, String defaultFamilyName) {
        try {
            M3Stats.onNativeCall();
            _nSetDefaultFontManager(_ptr, M3Native.getPtr(fontMgr), defaultFamilyName);
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(fontMgr);
        }
    }
    public M3FontMgr getFallbackManager() {
        try {
            M3Stats.onNativeCall();
            long ptr = _nGetFallbackManager(_ptr);
            return ptr == 0 ? null : new M3FontMgr(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3Typeface[] findTypefaces(String[] familyNames, M3FontStyle style) {
        try {
            assert Arrays.stream(familyNames).allMatch(Objects::nonNull) : "Can't findTypefaces with null familyNames elements";
            M3Stats.onNativeCall();
            long[] ptrs = _nFindTypefaces(_ptr, familyNames, style._value);
            M3Typeface[] res = new M3Typeface[ptrs.length];
            for (int i = 0; i < ptrs.length; ++i) {
                res[i] = new M3Typeface(ptrs[i]);
            }
            return res;
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3Typeface defaultFallback(int unicode, M3FontStyle style, String locale) {
        return defaultFallback(unicode, style, locale, null);
    }

    public M3Typeface defaultFallback(int unicode, M3FontStyle style, String locale, M3FontArguments fontArguments) {
        try {
            assert locale != null : "Can't defaultFallback with locale == null";
            M3Stats.onNativeCall();
            long ptr = _nDefaultFallbackChar(_ptr, unicode, style._value, locale, fontArguments);
            return ptr == 0 ? null : new M3Typeface(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3Typeface defaultFallback() {
        try {
            M3Stats.onNativeCall();
            long ptr = _nDefaultFallback(_ptr);
            return ptr == 0 ? null : new M3Typeface(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3FontCollection setEnableFallback(boolean value) {
        M3Stats.onNativeCall();
        _nSetEnableFallback(_ptr, value);
        return this;
    }

    public M3ParagraphCache getParagraphCache() {
        try {
            M3Stats.onNativeCall();
            return new M3ParagraphCache(this, _nGetParagraphCache(_ptr));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3FontCollection(long ptr) {
        super(ptr);
    }

    public static native long   _nMake();
    public static native long   _nGetFontManagersCount(long ptr);
    public static native long   _nSetAssetFontManager(long ptr, long fontManagerPtr);
    public static native long   _nSetDynamicFontManager(long ptr, long fontManagerPtr);
    public static native long   _nSetTestFontManager(long ptr, long fontManagerPtr);
    public static native long   _nSetDefaultFontManager(long ptr, long fontManagerPtr, String defaultFamilyName);
    public static native long   _nGetFallbackManager(long ptr);
    public static native long[] _nFindTypefaces(long ptr, String[] familyNames, int fontStyle);
    public static native long   _nDefaultFallbackChar(long ptr, int unicode, int fontStyle, String locale, M3FontArguments fontArguments);
    public static native long   _nDefaultFallback(long ptr);
    public static native long   _nSetEnableFallback(long ptr, boolean value);
    public static native long   _nGetParagraphCache(long ptr);
}