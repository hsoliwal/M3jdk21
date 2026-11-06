package jdk.m3.graphics.impl;


public abstract class M3Native {
    public long _ptr;

    public static long getPtr(M3Native n) { return n == null ? 0 : n._ptr; }

    public M3Native(long ptr) {
        if (ptr == 0)
            throw new RuntimeException("Can't wrap nullptr");
        this._ptr = ptr;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(_ptr=0x" + Long.toString(_ptr, 16) + ")";
    }

    @Override
    public boolean equals(Object other) {
        try {
            if (this == other)
                return true;
            if (null == other)
                return false;
            if (!getClass().isInstance(other))
                return false;
            M3Native nOther = (M3Native) other;
            if (_ptr == nOther._ptr)
                return true;
            return _nativeEquals(nOther);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(other);
        }
    }

    public boolean _nativeEquals(M3Native other) {
        return false;
    }

    // FIXME two different pointers might point to equal objects
    @Override
    public int hashCode() {
        return Long.hashCode(_ptr);
    }
}