package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;
import java.util.*;

public class M3FontMgr extends M3RefCnt {
    static { M3Library.staticLoad(); }
    public int getFamiliesCount() {
        try {
            M3Stats.onNativeCall();
            return _nGetFamiliesCount(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public String getFamilyName(int index) {
        try {
            M3Stats.onNativeCall();
            return _nGetFamilyName(_ptr, index);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3FontStyleSet makeStyleSet(int index) {
        try {
            M3Stats.onNativeCall();
            long ptr = _nMakeStyleSet(_ptr, index);
            return ptr == 0 ? null : new M3FontStyleSet(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * The caller must call {@link #close()} on the returned object.
     * Never returns null; will return an empty set if the name is not found.
     *
     * Passing null as the parameter will return the default system family.
     * Note that most systems don't have a default system family, so passing null will often
     * result in the empty set.
     *
     * It is possible that this will return a style set not accessible from
     * {@link #makeStyleSet(int)} due to hidden or auto-activated fonts.
     */
    public M3FontStyleSet matchFamily(String familyName) {
        try {
            M3Stats.onNativeCall();
            return new M3FontStyleSet(_nMatchFamily(_ptr, familyName));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Find the closest matching typeface to the specified familyName and style
     * and return a ref to it. The caller must call {@link #close()} on the returned
     * object. Will return null if no 'good' match is found.
     *
     * Passing null as the parameter for `familyName` will return the
     * default system font.
     *
     * It is possible that this will return a style set not accessible from
     * {@link #makeStyleSet(int)} or {@link #matchFamily(String)} due to hidden or
     * auto-activated fonts.
     */
    public M3Typeface matchFamilyStyle(String familyName, M3FontStyle style) {
        try {
            M3Stats.onNativeCall();
            long ptr = _nMatchFamilyStyle(_ptr, familyName, style._value);
            return ptr == 0 ? null : new M3Typeface(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3Typeface matchFamiliesStyle(String[] families, M3FontStyle style) {
        for (String family: families) {
            M3Typeface typeface = matchFamilyStyle(family, style);
            if (typeface != null) {
                return typeface;
            }
        }
        return null;
    }

    /**
     * Use the system fallback to find a typeface for the given character.
     * Note that bcp47 is a combination of ISO 639, 15924, and 3166-1 codes,
     * so it is fine to just pass a ISO 639 here.
     *
     * Will return null if no family can be found for the character
     * in the system fallback.
     *
     * Passing `null` as the parameter for `familyName` will return the
     * default system font.
     *
     * bcp47[0] is the least significant fallback, bcp47[bcp47.length-1] is the
     * most significant. If no specified bcp47 codes match, any font with the
     * requested character will be matched.
     */
    public M3Typeface matchFamilyStyleCharacter(String familyName, M3FontStyle style, String[] bcp47, int character) {
        try {
            M3Stats.onNativeCall();
            long ptr = _nMatchFamilyStyleCharacter(_ptr, familyName, style._value, bcp47, character);
            return ptr == 0 ? null : new M3Typeface(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3Typeface matchFamiliesStyleCharacter(String[] families, M3FontStyle style, String[] bcp47, int character) {
        assert Arrays.stream(bcp47).allMatch(Objects::nonNull) : "Can't matchFamiliesStyleCharacter with null bcp47 elements";
        for (String family: families) {
            M3Typeface typeface = matchFamilyStyleCharacter(family, style, bcp47, character);
            if (typeface != null) {
                return typeface;
            }
        }
        return null;
    }

    /**
     * Create a typeface for the specified file name or null if the data is not recognized.
     * The caller must call {@link #close()} on the returned object if it is not null.
     */
    public M3Typeface makeFromFile(String path) {
        return makeFromFile(path, 0);
    }

    /**
     * Create a typeface for the specified file name and TTC index (pass 0 for none)
     * or null if the data is not recognized. The caller must call {@link #close()} on
     * the returned object if it is not null.
     */
    public M3Typeface makeFromFile(String path, int ttcIndex) {
        try {
            assert path != null : "Can’t makeFromFile with path == null";
            M3Stats.onNativeCall();
            long ptr = _nMakeFromFile(_ptr, path, ttcIndex);
            return ptr == 0 ? null : new M3Typeface(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Create a typeface for the specified data or null if the data is not recognized.
     * The caller must call {@link #close()} on the returned object if it is not null.
     */
    public M3Typeface makeFromData(M3Data data) {
        return makeFromData(data, 0);
    }

    /**
     * Create a typeface for the specified data and TTC index (pass 0 for none)
     * or null if the data is not recognized. The caller must call {@link #close()} on
     * the returned object if it is not null.
     */
    public M3Typeface makeFromData(M3Data data, int ttcIndex) {
        try {
            assert data != null : "Can’t makeFromData with data == null";
            M3Stats.onNativeCall();
            long ptr = _nMakeFromData(_ptr, M3Native.getPtr(data), ttcIndex);
            return ptr == 0 ? null : new M3Typeface(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(data);
        }
    }

    /**
     * Controls whether Skia reads gamma and contrast at runtime from Windows ClearType settings.
     * Windows DirectWrite only
     *
     * @param value true to match Windows native text rendering, false to consistent rendering across machines
     * @see <a href="https://github.com/HumbleUI/SkiaBuild/pull/14">https://github.com/HumbleUI/SkiaBuild/pull/14</a>
     * @see <a href="https://github.com/HumbleUI/Skija/pull/107">https://github.com/HumbleUI/Skija/pull/107</a>
     */
    public static void useSystemRenderingParams(boolean value) {
        M3Stats.onNativeCall();
        _nUseSystemRenderingParams(value ? 1 : 0);
    }

    public static class _DefaultHolder {
        static { M3Stats.onNativeCall(); }
        public static final M3FontMgr INSTANCE = new M3FontMgr(_nDefault(), false);
    }

    /**
     * Return the default fontmgr.
     */
    public static M3FontMgr getDefault() {
        return _DefaultHolder.INSTANCE;
    }

    public M3FontMgr(long ptr) {
        super(ptr);
    }

    public M3FontMgr(long ptr, boolean allowClose) {
        super(ptr, allowClose);
    }

    public static native int _nGetFamiliesCount(long ptr);
    public static native String _nGetFamilyName(long ptr, int index);
    public static native long _nMakeStyleSet(long ptr, int index);
    public static native long _nMatchFamily(long ptr, String familyName);
    public static native long _nMatchFamilyStyle(long ptr, String familyName, int fontStyle);
    public static native long _nMatchFamilyStyleCharacter(long ptr, String familyName, int fontStyle, String[] bcp47, int character);
    public static native long _nMakeFromFile(long ptr, String path, int ttcIndex);
    public static native long _nMakeFromData(long ptr, long dataPtr, int ttcIndex);
    public static native void _nUseSystemRenderingParams(int value);
    public static native long _nDefault();
}