package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;

public class M3ManagedString extends M3Managed {
    static { M3Library.staticLoad(); }
    public M3ManagedString(long ptr) {
        super(ptr, _FinalizerHolder.PTR);
    }

    public M3ManagedString(String s) {
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

    public M3ManagedString insert(int offset, String s) {
        assert s != null : "Can't insrert with s == null";
        M3Stats.onNativeCall();
        _nInsert(_ptr, offset, s);
        return this;
    }

    public M3ManagedString append(String s) {
        assert s != null : "Can't append with s == null";
        M3Stats.onNativeCall();
        _nAppend(_ptr, s);
        return this;
    }

    public M3ManagedString remove(int from) {
        M3Stats.onNativeCall();
        _nRemoveSuffix(_ptr, from);
        return this;
    }

    public M3ManagedString remove(int from, int length) {
        M3Stats.onNativeCall();
        _nRemove(_ptr, from, length);
        return this;
    }

    public static class _FinalizerHolder {
        public static final long PTR = _nGetFinalizer();
    }

    public static native long _nMake(String s);
    public static native long _nGetFinalizer();
    public static native String _nToString(long ptr);
    public static native void _nInsert(long ptr, int offset, String s);
    public static native void _nAppend(long ptr, String s);
    public static native void _nRemoveSuffix(long ptr, int from);
    public static native void _nRemove(long ptr, int from, int length);
}