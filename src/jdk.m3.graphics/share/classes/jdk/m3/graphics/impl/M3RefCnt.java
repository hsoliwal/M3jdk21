package jdk.m3.graphics.impl;


public abstract class M3RefCnt extends M3Managed {
    protected M3RefCnt(long ptr) {
        super(ptr, _FinalizerHolder.PTR);
    }

    protected M3RefCnt(long ptr, boolean allowClose) {
        super(ptr, _FinalizerHolder.PTR, allowClose);
    }

    public int getRefCount() {
        try {
            M3Stats.onNativeCall();
            return _nGetRefCount(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    @Override
    public String toString() {
        String s = super.toString();
        String base = s.substring(0, s.length() - 1);
        return base + (isClosed()
                ? ", object is closed)"
                : ", refCount=" + getRefCount() + ")");
    }

    public static class _FinalizerHolder {
        public static final long PTR = _nGetFinalizer();
    }

    public static native long _nGetFinalizer();
    public static native int  _nGetRefCount(long ptr);
}