package jdk.m3.graphics.impl;


public abstract class M3Managed extends M3Native implements AutoCloseable {
    public M3Cleanable _cleanable;

    public M3Managed(long ptr, long finalizer) {
        this(ptr, finalizer, true);
    }

    public M3Managed(long ptr, long finalizer, boolean managed) {
        super(ptr);
        if (managed) {
            assert ptr != 0 : "Managed ptr is 0";
            assert finalizer != 0 : "Managed finalizer is 0";
            String className = getClass().getSimpleName();
            M3Stats.onAllocated(className);
            this._cleanable = M3Cleanable.register(this, new CleanerThunk(className, ptr, finalizer));
        }
    }

    @Override
    public void close() {
        if (0 != _ptr && null != _cleanable) {
            _cleanable.clean();
            _cleanable = null;
            _ptr = 0;
        }
    }

    public boolean isClosed() {
        return _ptr == 0;
    }

    public static class CleanerThunk implements Runnable {
        public String _className;
        public long _ptr;
        public long _finalizerPtr;

        public CleanerThunk(String className, long ptr, long finalizer) {
            this._className = className;
            this._ptr = ptr;
            this._finalizerPtr = finalizer;
        }

        public void run() {
            M3Log.trace(() -> "Cleaning " + _className + " " + Long.toString(_ptr, 16));
            M3Stats.onDeallocated(_className);
            M3Stats.onNativeCall();
            _nInvokeFinalizer(_finalizerPtr, _ptr);
        }
    }

    public static native void _nInvokeFinalizer(long finalizer, long ptr);
}