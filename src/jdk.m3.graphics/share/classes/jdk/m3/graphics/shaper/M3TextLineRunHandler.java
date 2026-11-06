package jdk.m3.graphics.shaper;

import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.*;

public class M3TextLineRunHandler<T> extends M3Managed implements M3RunHandler {
    static { M3Library.staticLoad(); }

    public final M3ManagedString _text;

    public M3TextLineRunHandler(M3ManagedString text, boolean manageText) {
        super(_nMake(M3Native.getPtr(text)), _FinalizerHolder.PTR);
        _text = manageText ? text : null;
        M3ReferenceUtil.reachabilityFence(text);
    }

    public M3TextLineRunHandler(String text) {
        this(new M3ManagedString(text), true);
    }

    @Override
    public void close() {
        super.close();
        if (_text != null)
            _text.close();
    }

    @Override
    public void beginLine() {
        throw new UnsupportedOperationException("TextLineRunHandler::beginLine");
    }

    @Override
    public void runInfo(M3RunInfo info) {
        throw new UnsupportedOperationException("TextLineRunHandler::runInfo");
    }

    @Override
    public void commitRunInfo() {
        throw new UnsupportedOperationException("TextLineRunHandler::commitRunInfo");
    }

    @Override
    public M3Point runOffset(M3RunInfo info) {
        throw new UnsupportedOperationException("TextLineRunHandler::runOffset");
    }

    @Override
    public void commitRun(M3RunInfo info, short[] glyphs, M3Point[] positions, int[] clusters) {
        throw new UnsupportedOperationException("TextLineRunHandler::commitRun");
    }

    @Override
    public void commitLine() {
        throw new UnsupportedOperationException("TextLineRunHandler::commitLine");
    }

    public M3TextLine makeLine() {
        try {
            M3Stats.onNativeCall();
            long ptr = _nMakeLine(_ptr);
            return 0 == ptr ? null : new M3TextLine(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public static class _FinalizerHolder {
        public static final long PTR = _nGetFinalizer();
    }

    public static native long _nGetFinalizer();
    public static native long _nMake(long textPtr);
    public static native long _nMakeLine(long ptr);
}