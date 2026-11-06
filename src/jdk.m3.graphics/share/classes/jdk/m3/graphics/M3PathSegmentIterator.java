package jdk.m3.graphics;

import java.util.*;
import jdk.m3.graphics.impl.*;


public class M3PathSegmentIterator extends M3Managed implements Iterator<M3PathSegment> {
    static { M3Library.staticLoad(); }
    public final M3Path _path;
    public M3PathSegment _nextSegment;

    @Override
    public M3PathSegment next() {
        try {
            if (_nextSegment._verb == M3PathVerb.DONE)
                throw new NoSuchElementException();
            M3PathSegment res = _nextSegment;
            _nextSegment = _nNext(_ptr);
            return res;
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    @Override
    public boolean hasNext() {
        return _nextSegment._verb != M3PathVerb.DONE;
    }

    public M3PathSegmentIterator(M3Path path, long ptr) {
        super(ptr, _nGetFinalizer());
        this._path = path;
        M3Stats.onNativeCall();
    }

    public static M3PathSegmentIterator make(M3Path path, boolean forceClose) {
        try {
            long ptr = _nMake(M3Native.getPtr(path), forceClose);
            M3PathSegmentIterator i = new M3PathSegmentIterator(path, ptr);
            i._nextSegment = _nNext(ptr);
            return i;
        } finally {
            M3ReferenceUtil.reachabilityFence(path);
        }
    }

    public static class _FinalizerHolder {
        public static final long PTR = _nGetFinalizer();
    }

    public static native long _nMake(long pathPtr, boolean forceClose);
    public static native long _nGetFinalizer();
    public static native M3PathSegment _nNext(long ptr);
}
