package jdk.m3.graphics.shaper;

import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.*;

public class M3TextBlobBuilderRunHandler<T> extends M3Managed implements M3RunHandler {
    static { M3Library.staticLoad(); }

    public final M3ManagedString _text;

    public M3TextBlobBuilderRunHandler(M3ManagedString text, boolean manageText, float offsetX, float offsetY) {
        super(_nMake(M3Native.getPtr(text), offsetX, offsetY), _FinalizerHolder.PTR);
        _text = manageText ? text : null;
        M3ReferenceUtil.reachabilityFence(text);
    }

    public M3TextBlobBuilderRunHandler(String text) {
        this(new M3ManagedString(text), true, 0, 0);
    }

    public M3TextBlobBuilderRunHandler(String text, M3Point offset) {
        this(new M3ManagedString(text), true, offset._x, offset._y);
    }

    @Override
    public void close() {
        super.close();
        if (_text != null)
            _text.close();
    }

    @Override
    public void beginLine() {
        throw new UnsupportedOperationException("beginLine");
    }

    @Override
    public void runInfo(M3RunInfo info) {
        throw new UnsupportedOperationException("runInfo");
    }

    @Override
    public void commitRunInfo() {
        throw new UnsupportedOperationException("commitRunInfo");
    }

    @Override
    public M3Point runOffset(M3RunInfo info) {
        throw new UnsupportedOperationException("runOffset");
    }

    @Override
    public void commitRun(M3RunInfo info, short[] glyphs, M3Point[] positions, int[] clusters) {
        throw new UnsupportedOperationException("commitRun");
    }

    @Override
    public void commitLine() {
        throw new UnsupportedOperationException("commitLine");
    }

    public M3TextBlob makeBlob() {
        try {
            M3Stats.onNativeCall();
            long ptr = _nMakeBlob(_ptr);
            return 0 == ptr ? null : new M3TextBlob(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public static class _FinalizerHolder {
        public static final long PTR = _nGetFinalizer();
    }

    public static native long _nGetFinalizer();
    public static native long _nMake(long textPtr, float offsetX, float offsetY);
    public static native long _nMakeBlob(long ptr);
}