package jdk.m3.graphics;

import java.nio.ByteBuffer;
import jdk.m3.graphics.impl.*;

/**
 *  Data holds an immutable data buffer.
 */
public class M3Data extends M3Managed {
    static { M3Library.staticLoad(); }

    public long getSize() {
        try {
            M3Stats.onNativeCall();
            return _nSize(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public byte[] getBytes() {
        return getBytes(0, getSize());
    }

    public byte[] getBytes(long offset, long length) {
        try {
            M3Stats.onNativeCall();
            return _nBytes(_ptr, offset, length);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     *  Returns true if these two objects have the same length and contents,
     *  effectively returning 0 == memcmp(...)
     */
    @Override
    public boolean _nativeEquals(M3Native other) {
        try {
            M3Stats.onNativeCall();
            return _nEquals(_ptr, M3Native.getPtr(other));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(other);
        }
    }

    public static M3Data makeFromBytes(byte[] bytes) {
        return makeFromBytes(bytes, 0, bytes.length);
    }

    public static M3Data makeFromBytes(byte[] bytes, long offset, long length) {
        M3Stats.onNativeCall();
        return new M3Data(_nMakeFromBytes(bytes, offset, length));
    }

    /**
     *  Create a new dataref the file with the specified path.
     *  If the file cannot be opened, this returns null.
     */
    public static M3Data makeFromFileName(String path) {
        assert path != null : "Can’t makeFromFileName with path == null";
        M3Stats.onNativeCall();
        long ptr = _nMakeFromFileName(path);
        return ptr == 0 ? null : new M3Data(ptr);
    }

    /**
     *  Create a new dataref using a subset of the data in the specified
     *  src dataref.
     */
    public M3Data makeSubset(long offset, long length) {
        try {
            M3Stats.onNativeCall();
            return new M3Data(_nMakeSubset(_ptr, offset, length));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3Data makeCopy() {
        try {
            M3Stats.onNativeCall();
            return new M3Data(_nMakeSubset(_ptr, 0, getSize()));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public ByteBuffer toByteBuffer() {
        try {
            M3Stats.onNativeCall();
            return _nToByteBuffer(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     *  Returns a new empty dataref (or a reference to a shared empty dataref).
     *  New or shared, the caller must see that {@link #close()} is eventually called.
     */
    public static M3Data makeEmpty() {
        M3Stats.onNativeCall();
        return new M3Data(_nMakeEmpty());
    }

    public M3Data(long ptr) {
        super(ptr, _FinalizerHolder.PTR);
    }

    public static class _FinalizerHolder {
        public static final long PTR = _nGetFinalizer();
    }

    public static native long    _nGetFinalizer();
    public static native long    _nSize(long ptr);
    public static native byte[]  _nBytes(long ptr, long offset, long length);
    public static native boolean _nEquals(long ptr, long otherPtr);
    public static native ByteBuffer _nToByteBuffer(long ptr);
    public static native long    _nMakeFromBytes(byte[] bytes, long offset, long length);
    public static native long    _nMakeFromFileName(String path);
    public static native long    _nMakeSubset(long ptr, long offset, long length);
    public static native long    _nMakeEmpty();
}