package jdk.m3.graphics.shaper;

import java.util.*;
import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.*;

public abstract class M3ManagedRunIterator<T> extends M3Managed implements Iterator<T> {
    static { M3Library.staticLoad(); }

    public final M3ManagedString _text;

    public M3ManagedRunIterator(long ptr, M3ManagedString text, boolean manageText) {
        super(ptr, _FinalizerHolder.PTR);
        _text = manageText ? text : null;
    }

    @Override
    public void close() {
        super.close();
        if (_text != null)
            _text.close();
    }

    public int _getEndOfCurrentRun() {
        try {
            return _nGetEndOfCurrentRun(_ptr, M3Native.getPtr(_text));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(_text);
        }
    }

    @Override
    public boolean hasNext() {
        try {
            return !_nIsAtEnd(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public static class _FinalizerHolder {
        public static final long PTR = _nGetFinalizer();
    }

    public static native long _nGetFinalizer();
    public static native void _nConsume(long ptr);
    public static native int  _nGetEndOfCurrentRun(long ptr, long textPtr);
    public static native boolean _nIsAtEnd(long ptr);
}