package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.*;

public class M3PathMeasure extends M3Managed {
    static { M3Library.staticLoad(); }
    public M3PathMeasure(long ptr) {
        super(ptr, _FinalizerHolder.PTR);
    }

    public M3PathMeasure() {
        this(_nMake());
        M3Stats.onNativeCall();
    }

    /**
     * Initialize the pathmeasure with the specified path. The parts of the path that are needed
     * are copied, so the client is free to modify/delete the path after this call.
     */
    public M3PathMeasure(M3Path path) {
        this(path, false, 1f);
    }

    /**
     * Initialize the pathmeasure with the specified path. The parts of the path that are needed
     * are copied, so the client is free to modify/delete the path after this call.
     */
    public M3PathMeasure(M3Path path, boolean forceClosed) {
        this(path, forceClosed, 1f);
    }

    /**
     * <p>Initialize the pathmeasure with the specified path. The parts of the path that are needed
     * are copied, so the client is free to modify/delete the path after this call.</p>
     *
     * <p>resScale controls the precision of the measure. values &gt; 1 increase the
     * precision (and possible slow down the computation).</p>
     */
    public M3PathMeasure(M3Path path, boolean forceClosed, float resScale) {
        this(_nMakePath(M3Native.getPtr(path), forceClosed, resScale));
        M3Stats.onNativeCall();
        M3ReferenceUtil.reachabilityFence(path);
    }

    /**
     * Reset the pathmeasure with the specified path. The parts of the path that are needed
     * are copied, so the client is free to modify/delete the path after this call.
     */
    public M3PathMeasure setPath(M3Path path, boolean forceClosed) {
        try {
            M3Stats.onNativeCall();
            _nSetPath(_ptr, M3Native.getPtr(path), forceClosed);
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(path);
        }
    }

    /**
     * Return the total length of the current contour, or 0 if no path
     * is associated (e.g. resetPath(null))
     */
    public float getLength() {
        try {
            M3Stats.onNativeCall();
            return _nGetLength(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Pins distance to 0 &lt;= distance &lt;= getLength(), and then computes
     * the corresponding position.
     *
     * @return  null if there is no path, or a zero-length path was specified.
     */
    public M3Point getPosition(float distance) {
        try {
            M3Stats.onNativeCall();
            return _nGetPosition(_ptr, distance);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Pins distance to 0 &lt;= distance &lt;= getLength(), and then computes
     * the corresponding tangent.
     *
     * @return  null if there is no path, or a zero-length path was specified.
     */
    public M3Point getTangent(float distance) {
        try {
            M3Stats.onNativeCall();
            return _nGetTangent(_ptr, distance);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Pins distance to 0 &lt;= distance &lt;= getLength(), and then computes
     * the corresponding RSXform.
     *
     * @return  null if there is no path, or a zero-length path was specified.
     */
    public M3RSXform getRSXform(float distance) {
        try {
            M3Stats.onNativeCall();
            return _nGetRSXform(_ptr, distance);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Pins distance to 0 &lt;= distance &lt;= getLength(), and then computes
     * the corresponding matrix (by calling getPosition/getTangent).
     *
     * @return  null if there is no path, or a zero-length path was specified.
     */
    public M3Matrix33 getMatrix(float distance, boolean getPosition, boolean getTangent) {
        try {
            M3Stats.onNativeCall();
            float[] mat = _nGetMatrix(_ptr, distance, getPosition, getTangent);
            return mat == null ? null : new M3Matrix33(mat);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Given a start and stop distance, return in dst the intervening segment(s).
     * If the segment is zero-length, return false, else return true.
     * startD and stopD are pinned to legal values (0..getLength()). If startD &gt; stopD
     * then return false (and leave dst untouched).
     * Begin the segment with a moveTo if startWithMoveTo is true
    */
    public boolean getSegment(float startD, float endD, M3PathBuilder dst, boolean startWithMoveTo) {
        try {
            M3Stats.onNativeCall();
            return _nGetSegment(_ptr, startD, endD, M3Native.getPtr(dst), startWithMoveTo);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(dst);
        }
    }

    /**
     * @return  true if the current contour is closed.
     */
    public boolean isClosed() {
        try {
            M3Stats.onNativeCall();
            return _nIsClosed(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Move to the next contour in the path. Return true if one exists, or false if
     * we're done with the path.
     */
    public boolean nextContour() {
        try {
            M3Stats.onNativeCall();
            return _nNextContour(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public static class _FinalizerHolder {
        public static final long PTR = _nGetFinalizer();
    }

    public static native long    _nGetFinalizer();
    public static native long    _nMake();
    public static native long    _nMakePath(long pathPtr, boolean forceClosed, float resScale);
    public static native void    _nSetPath(long ptr, long pathPtr, boolean forceClosed);
    public static native float   _nGetLength(long ptr);
    public static native M3Point   _nGetPosition(long ptr, float distance);
    public static native M3Point   _nGetTangent(long ptr, float distance);
    public static native M3RSXform _nGetRSXform(long ptr, float distance);
    public static native float[] _nGetMatrix(long ptr, float distance, boolean getPosition, boolean getTangent);
    public static native boolean _nGetSegment(long ptr, float startD, float endD, long dstPtr, boolean startWithMoveTo);
    public static native boolean _nIsClosed(long ptr);
    public static native boolean _nNextContour(long ptr);
}
