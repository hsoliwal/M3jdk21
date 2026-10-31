package jdk.m3.graphics.paragraph;

import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.*;

public class M3Paragraph extends M3Managed {
    static { M3Library.staticLoad(); }

    public M3ManagedString _text;

    @Override
    public void close() {
        if (_text != null) {
            _text.close();
            _text = null;
        }
        super.close();
    }
    public float getMaxWidth() {
        try {
            M3Stats.onNativeCall();
            return _nGetMaxWidth(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public float getHeight() {
        try {
            M3Stats.onNativeCall();
            return _nGetHeight(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public float getMinIntrinsicWidth() {
        try {
            M3Stats.onNativeCall();
            return _nGetMinIntrinsicWidth(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public float getMaxIntrinsicWidth() {
        try {
            M3Stats.onNativeCall();
            return _nGetMaxIntrinsicWidth(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public float getAlphabeticBaseline() {
        try {
            M3Stats.onNativeCall();
            return _nGetAlphabeticBaseline(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public float getIdeographicBaseline() {
        try {
            M3Stats.onNativeCall();
            return _nGetIdeographicBaseline(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public float getLongestLine() {
        try {
            M3Stats.onNativeCall();
            return _nGetLongestLine(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public boolean didExceedMaxLines() {
        try {
            M3Stats.onNativeCall();
            return _nDidExceedMaxLines(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3Paragraph layout(float width) {
        M3Stats.onNativeCall();
        _nLayout(_ptr, width);
        return this;
    }

    public M3Paragraph paint(M3Canvas canvas, float x, float y) {
        try {
            M3Stats.onNativeCall();
            _nPaint(_ptr, M3Native.getPtr(canvas), x, y);
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(canvas);
        }
    }

    /**
     * Returns a vector of bounding boxes that enclose all text between
     * start and end char indices, including start and excluding end.
     */
    public M3TextBox[] getRectsForRange(int start, int end, M3RectHeightMode rectHeightMode, M3RectWidthMode rectWidthMode) {
        try {
            M3Stats.onNativeCall();
            return _nGetRectsForRange(_ptr, start, end, rectHeightMode.ordinal(), rectWidthMode.ordinal());
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3TextBox[] getRectsForPlaceholders() {
        try {
            M3Stats.onNativeCall();
            return _nGetRectsForPlaceholders(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3PositionWithAffinity getGlyphPositionAtCoordinate(float dx, float dy) {
        try {
            M3Stats.onNativeCall();
            int res = _nGetGlyphPositionAtCoordinate(_ptr, dx, dy);
            if (res >= 0)
                return new M3PositionWithAffinity(res, M3Affinity.DOWNSTREAM);
            else
                return new M3PositionWithAffinity(-res-1, M3Affinity.UPSTREAM);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3IRange getWordBoundary(int offset) {
        try {
            M3Stats.onNativeCall();
            return M3IRange._makeFromLong(_nGetWordBoundary(_ptr, offset));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3LineMetrics[] getLineMetrics() {
        try {
            if (_text == null)
                return new M3LineMetrics[0];
            M3Stats.onNativeCall();
            return _nGetLineMetrics(_ptr, M3Native.getPtr(_text));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(_text);
        }
    }

    public long getLineNumber() {
        try {
            M3Stats.onNativeCall();
            return _nGetLineNumber(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3Paragraph markDirty() {
        M3Stats.onNativeCall();
        _nMarkDirty(_ptr);
        return this;
    }

    public int getUnresolvedGlyphsCount() {
        try {
            M3Stats.onNativeCall();
            return _nGetUnresolvedGlyphsCount(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3Paragraph updateAlignment(M3Alignment alignment) {
        M3Stats.onNativeCall();
        _nUpdateAlignment(_ptr, alignment.ordinal());
        return this;
    }

    // Skia now does not provide Paragraph::updateText().
    // To change text, rebuild Paragraph via ParagraphBuilder.

    public M3Paragraph updateFontSize(int from, int to, float size) {
        try {
            if (_text != null) {
                M3Stats.onNativeCall();
                _nUpdateFontSize(_ptr, from, to, size, M3Native.getPtr(_text));
            }
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(_text);
        }
    }

    public M3Paragraph updateForegroundPaint(int from, int to, M3Paint paint) {
        try {
            if (_text != null) {
                M3Stats.onNativeCall();
                _nUpdateForegroundPaint(_ptr, from, to, M3Native.getPtr(paint), M3Native.getPtr(_text));
            }
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(paint);
            M3ReferenceUtil.reachabilityFence(_text);
        }
    }

    public M3Paragraph updateBackgroundPaint(int from, int to, M3Paint paint) {
        try {
            if (_text != null) {
                M3Stats.onNativeCall();
                _nUpdateBackgroundPaint(_ptr, from, to, M3Native.getPtr(paint), M3Native.getPtr(_text));
            }
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(paint);
            M3ReferenceUtil.reachabilityFence(_text);
        }
    }

    public M3Paragraph(long ptr, M3ManagedString text) {
        super(ptr, _FinalizerHolder.PTR);
        M3Stats.onNativeCall();
        _text = text;
    }

    public static class _FinalizerHolder {
        public static final long PTR = _nGetFinalizer();
    }

    public static native long  _nGetFinalizer();
    public static native float _nGetMaxWidth(long ptr);
    public static native float _nGetHeight(long ptr);
    public static native float _nGetMinIntrinsicWidth(long ptr);
    public static native float _nGetMaxIntrinsicWidth(long ptr);
    public static native float _nGetAlphabeticBaseline(long ptr);
    public static native float _nGetIdeographicBaseline(long ptr);
    public static native float _nGetLongestLine(long ptr);
    public static native boolean _nDidExceedMaxLines(long ptr);
    public static native void  _nLayout(long ptr, float width);
    public static native long  _nPaint(long ptr, long canvasPtr, float x, float y);
    public static native M3TextBox[] _nGetRectsForRange(long ptr, int start, int end, int rectHeightMode, int rectWidthMode);
    public static native M3TextBox[] _nGetRectsForPlaceholders(long ptr);
    public static native int   _nGetGlyphPositionAtCoordinate(long ptr, float dx, float dy);
    public static native long  _nGetWordBoundary(long ptr, int offset);
    public static native M3LineMetrics[] _nGetLineMetrics(long ptr, long textPtr);
    public static native long  _nGetLineNumber(long ptr);
    public static native void  _nMarkDirty(long ptr);
    public static native int   _nGetUnresolvedGlyphsCount(long ptr);
    public static native void  _nUpdateAlignment(long ptr, int Align);
    public static native void  _nUpdateFontSize(long ptr, int from, int to, float size, long textPtr);
    public static native void  _nUpdateForegroundPaint(long ptr, int from, int to, long paintPtr, long textPtr);
    public static native void  _nUpdateBackgroundPaint(long ptr, int from, int to, long paintPtr, long textPtr);
}
