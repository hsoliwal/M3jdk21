package jdk.m3.graphics.paragraph;

import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.*;

public class M3ParagraphBuilder extends M3Managed {
    static { M3Library.staticLoad(); }

    public M3ManagedString _text;
    public M3ParagraphBuilder(M3ParagraphStyle style, M3FontCollection fc) {
        super(_nMake(M3Native.getPtr(style), M3Native.getPtr(fc)), _FinalizerHolder.PTR);
        M3Stats.onNativeCall();
        M3ReferenceUtil.reachabilityFence(style);
        M3ReferenceUtil.reachabilityFence(fc);
    }

    public M3ParagraphBuilder pushStyle(M3TextStyle style) {
        try {
            M3Stats.onNativeCall();
            _nPushStyle(_ptr, M3Native.getPtr(style));
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(style);
        }
    }

    public M3ParagraphBuilder popStyle() {
        M3Stats.onNativeCall();
        _nPopStyle(_ptr);
        return this;
    }

    public M3ParagraphBuilder addText(String text) {
        assert text != null : "Can't addText with text == null";
        M3Stats.onNativeCall();
        _nAddText(_ptr, text);
        if (_text == null) {
            _text = new M3ManagedString(text);
        } else {
            _text.append(text);
        }
        return this;
    }

    public M3ParagraphBuilder addPlaceholder(M3PlaceholderStyle style) {
        M3Stats.onNativeCall();
        _nAddPlaceholder(_ptr, style.getWidth(), style.getHeight(), style.getAlignment().ordinal(), style.getBaselineMode().ordinal(), style.getBaseline());
        return this;
    }

    public M3Paragraph build() {
        try {
            M3Stats.onNativeCall();
            M3Paragraph paragraph = new M3Paragraph(_nBuild(_ptr), _text);
            _text = null;
            return paragraph;
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public static class _FinalizerHolder {
        public static final long PTR = _nGetFinalizer();
    }

    public static native long _nMake(long paragraphStylePtr, long fontCollectionPtr);
    public static native long _nGetFinalizer();
    public static native void _nPushStyle(long ptr, long textStylePtr);
    public static native void _nPopStyle(long ptr);
    public static native void _nAddText(long ptr, String text);
    public static native void _nAddPlaceholder(long ptr, float width, float height, int alignment, int baselineMode, float baseline);
    public static native long _nBuild(long ptr);
}