package jdk.m3.graphics.shaper;

import java.util.*;
import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.*;

/**
 * Shapes text using HarfBuzz and places the shaped text into a
 * client-managed buffer.
 */
public class M3Shaper extends M3Managed {
    static { M3Library.staticLoad(); }
    public static M3Shaper makePrimitive() {
        M3Stats.onNativeCall();
        return new M3Shaper(_nMakePrimitive());
    }

    public static M3Shaper makeShaperDrivenWrapper() {
        return makeShaperDrivenWrapper(null);
    }

    public static M3Shaper makeShaperDrivenWrapper(M3FontMgr fontMgr) {
        try {
            M3Stats.onNativeCall();
            return new M3Shaper(_nMakeShaperDrivenWrapper(M3Native.getPtr(fontMgr)));
        } finally {
            M3ReferenceUtil.reachabilityFence(fontMgr);
        }
    }

    public static M3Shaper makeShapeThenWrap() {
        return makeShapeThenWrap(null);
    }

    public static M3Shaper makeShapeThenWrap(M3FontMgr fontMgr) {
        try {
            M3Stats.onNativeCall();
            return new M3Shaper(_nMakeShapeThenWrap(M3Native.getPtr(fontMgr)));
        } finally {
            M3ReferenceUtil.reachabilityFence(fontMgr);
        }
    }

    public static M3Shaper makeShapeDontWrapOrReorder() {
        return makeShapeDontWrapOrReorder(null);
    }

    public static M3Shaper makeShapeDontWrapOrReorder(M3FontMgr fontMgr) {
        try {
            M3Stats.onNativeCall();
            return new M3Shaper(_nMakeShapeDontWrapOrReorder(M3Native.getPtr(fontMgr)));
        } finally {
            M3ReferenceUtil.reachabilityFence(fontMgr);
        }
    }

    /**
     * <p>Only works on macOS</p>
     *
     * <p>WARN broken in m87 https://bugs.chromium.org/p/skia/issues/detail?id=10897</p>
     *
     * @return  Shaper on macOS, throws UnsupportedOperationException elsewhere
     */
    public static M3Shaper makeCoreText() {
        M3Stats.onNativeCall();
        long ptr = _nMakeCoreText();
        if (ptr == 0)
            throw new UnsupportedOperationException("CoreText not available");
        return new M3Shaper(ptr);
    }

    public static M3Shaper make() {
        return make(null);
    }

    public static M3Shaper make(M3FontMgr fontMgr) {
        try {
            M3Stats.onNativeCall();
            return new M3Shaper(_nMake(M3Native.getPtr(fontMgr)));
        } finally {
            M3ReferenceUtil.reachabilityFence(fontMgr);
        }
    }

    public static M3Shaper makeBestAvailable() {
        M3Stats.onNativeCall();
        return new M3Shaper(_nMakeBestAvailable(0));
    }

    public static M3Shaper makeBestAvailable(M3FontMgr fontMgr) {
        try {
            M3Stats.onNativeCall();
            return new M3Shaper(_nMakeBestAvailable(M3Native.getPtr(fontMgr)));
        } finally {
            M3ReferenceUtil.reachabilityFence(fontMgr);
        }
    }

    public M3TextBlob shape(String text, M3Font font) {
        return shape(text, font, M3ShapingOptions.DEFAULT, Float.POSITIVE_INFINITY, M3Point.ZERO);
    }

    public M3TextBlob shape(String text, M3Font font, float width) {
        return shape(text, font, M3ShapingOptions.DEFAULT, width, M3Point.ZERO);
    }

    public M3TextBlob shape(String text, M3Font font, float width, M3Point offset) {
        return shape(text, font, M3ShapingOptions.DEFAULT, width, offset);
    }

    public M3TextBlob shape(String text, M3Font font, M3ShapingOptions opts, float width, M3Point offset) {
        try {
            assert text != null : "Can’t Shaper::shape with text == null";
            assert opts != null : "Can’t Shaper::shape with opts == null";
            assert font != null : "Can’t Shaper::shape with font == null";
            M3Stats.onNativeCall();
            long ptr = _nShapeBlob(_ptr, text, M3Native.getPtr(font), opts, width, offset._x, offset._y);
            return 0 == ptr ? null : new M3TextBlob(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(font);
        }
    }

    public M3TextLine shapeLine(String text, M3Font font, M3ShapingOptions opts) {
        try {
            assert text != null : "Can’t Shaper::shapeLine with text == null";
            assert opts != null : "Can’t Shaper::shapeLine with opts == null";
            assert font != null : "Can’t Shaper::shapeLine with font == null";
            M3Stats.onNativeCall();
            return new M3TextLine(_nShapeLine(_ptr, text, M3Native.getPtr(font), opts));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(font);
        }
    }

    public M3TextLine shapeLine(String text, M3Font font) {
        return shapeLine(text, font, M3ShapingOptions.DEFAULT);
    }

    public M3Shaper shape(String text,
                        M3Font font,
                        M3ShapingOptions opts,
                        float width,
                        M3RunHandler runHandler)
    {
        try (M3ManagedString textUtf8 = new M3ManagedString(text);
             M3FontMgrRunIterator fontIter = new M3FontMgrRunIterator(textUtf8, false, font, opts);
             M3IcuBidiRunIterator bidiIter = new M3IcuBidiRunIterator(textUtf8, false, opts._leftToRight ? java.text.Bidi.DIRECTION_LEFT_TO_RIGHT : java.text.Bidi.DIRECTION_RIGHT_TO_LEFT);
             M3HbIcuScriptRunIterator scriptIter = new M3HbIcuScriptRunIterator(textUtf8, false);)
        {
            Iterator<M3LanguageRun> langIter = new M3TrivialLanguageRunIterator(text, Locale.getDefault().toLanguageTag());
            return shape(textUtf8, fontIter, bidiIter, scriptIter, langIter, opts, width, runHandler);
        }
    }

    public M3Shaper shape(String text,
                        Iterator<M3FontRun> fontIter,
                        Iterator<M3BidiRun> bidiIter,
                        Iterator<M3ScriptRun> scriptIter,
                        Iterator<M3LanguageRun> langIter,
                        M3ShapingOptions opts,
                        float width,
                        M3RunHandler runHandler)
    {
        try (M3ManagedString textUtf8 = new M3ManagedString(text);) {
            return shape(textUtf8, fontIter, bidiIter, scriptIter, langIter, opts, width, runHandler);
        }
    }

    public M3Shaper shape(M3ManagedString textUtf8,
                        Iterator<M3FontRun> fontIter,
                        Iterator<M3BidiRun> bidiIter,
                        Iterator<M3ScriptRun> scriptIter,
                        Iterator<M3LanguageRun> langIter,
                        M3ShapingOptions opts,
                        float width,
                        M3RunHandler runHandler)
    {
        try {
            assert fontIter != null : "FontRunIterator == null";
            assert bidiIter != null : "BidiRunIterator == null";
            assert scriptIter != null : "ScriptRunIterator == null";
            assert langIter != null : "LanguageRunIterator == null";
            assert opts != null : "Can’t Shaper::shape with opts == null";
            M3Stats.onNativeCall();
            _nShape(_ptr, M3Native.getPtr(textUtf8), fontIter, bidiIter, scriptIter, langIter, opts, width, runHandler);
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(textUtf8);
        }
    }

    public M3Shaper(long ptr) {
        super(ptr, _FinalizerHolder.PTR);
    }

    public static class _FinalizerHolder {
        public static final long PTR = _nGetFinalizer();
    }

    public static native long _nGetFinalizer();
    public static native long _nMakePrimitive();
    public static native long _nMakeShaperDrivenWrapper(long fontMgrPtr);
    public static native long _nMakeShapeThenWrap(long fontMgrPtr);
    public static native long _nMakeShapeDontWrapOrReorder(long fontMgrPtr);
    public static native long _nMakeCoreText();
    public static native long _nMake(long fontMgrPtr);
    public static native long _nMakeBestAvailable(long fontMgrPtr);
    public static native long _nShapeBlob(long ptr, String text, long fontPtr, M3ShapingOptions opts, float width, float offsetX, float offsetY);
    public static native long _nShapeLine(long ptr, String text, long fontPtr, M3ShapingOptions opts);
    public static native void _nShape(long ptr, long textPtr, Iterator<M3FontRun> fontIter, Iterator<M3BidiRun> bidiIter, Iterator<M3ScriptRun> scriptIter, Iterator<M3LanguageRun> langIter, M3ShapingOptions opts, float width, M3RunHandler runHandler);
}
