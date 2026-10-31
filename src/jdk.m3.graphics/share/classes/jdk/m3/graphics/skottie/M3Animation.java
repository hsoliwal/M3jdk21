package jdk.m3.graphics.skottie;

import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.sksg.M3InvalidationController;
import jdk.m3.graphics.*;

public class M3Animation extends M3Managed {
    static { M3Library.staticLoad(); }

    public M3Animation(long ptr) {
        super(ptr, _FinalizerHolder.PTR);
    }

    public static class _FinalizerHolder {
        public static final long PTR = _nGetFinalizer();
    }

    public static M3Animation makeFromString(String data) {
        assert data != null : "Can’t Animation::makeFromString with data == null";
        M3Stats.onNativeCall();
        long ptr = _nMakeFromString(data);
        if (ptr == 0)
            throw new IllegalArgumentException("Failed to create Animation from string=\"" + data.toString() + "\"");
        return new M3Animation(ptr);
    }

    public static M3Animation makeFromFile(String path) {
        assert path != null : "Can’t Animation::makeFromFile with path == null";
        M3Stats.onNativeCall();
        long ptr = _nMakeFromFile(path);
        if (ptr == 0)
            throw new IllegalArgumentException("Failed to create Animation from path=\"" + path + "\"");
        return new M3Animation(ptr);
    }

    public static M3Animation makeFromData(M3Data data) {
        assert data != null : "Can’t Animation::makeFromData with data == null";
        M3Stats.onNativeCall();
        long ptr = _nMakeFromData(M3Native.getPtr(data));
        if (ptr == 0)
            throw new IllegalArgumentException("Failed to create Animation from data.");
        return new M3Animation(ptr);
    }

    /**
     * <p>Draws the current animation frame</p>
     *
     * <p>It is undefined behavior to call render() on a newly created Animation
     * before specifying an initial frame via one of the seek() variants.</p>
     *
     * @param canvas  destination canvas
     * @return        this
     */
    public M3Animation render(M3Canvas canvas) {
        return render(canvas, M3Rect.makeXYWH(0, 0, getWidth(), getHeight()));
    }

    /**
     * <p>Draws the current animation frame</p>
     *
     * <p>It is undefined behavior to call render() on a newly created Animation
     * before specifying an initial frame via one of the seek() variants.</p>
     *
     * @param canvas  destination canvas
     * @param offset  destination offset
     * @return        this
     */
    public M3Animation render(M3Canvas canvas, M3Point offset) {
        assert offset != null : "Can’t Animation::render with offset == null";
        return render(canvas, offset._x, offset._y);
    }

    /**
     * <p>Draws the current animation frame</p>
     *
     * <p>It is undefined behavior to call render() on a newly created Animation
     * before specifying an initial frame via one of the seek() variants.</p>
     *
     * @param canvas  destination canvas
     * @param left    destination offset left
     * @param top     destination offset top
     * @return        this
     */
    public M3Animation render(M3Canvas canvas, float left, float top) {
        return render(canvas, M3Rect.makeXYWH(left, top, getWidth(), getHeight()));
    }

    /**
     * <p>Draws the current animation frame</p>
     *
     * <p>It is undefined behavior to call render() on a newly created Animation
     * before specifying an initial frame via one of the seek() variants.</p>
     *
     * @param canvas       destination canvas
     * @param dst          destination rect
     * @param renderFlags  render flags
     * @return             this
     */
    public M3Animation render(M3Canvas canvas, M3Rect dst, M3RenderFlag... renderFlags) {
        try {
            assert canvas != null : "Can’t Animation::render with canvas == null";
            assert dst != null : "Can’t Animation::render with dst == null";
            M3Stats.onNativeCall();
            int flags = 0;
            for (M3RenderFlag flag: renderFlags)
                flags |= flag._flag;
            _nRender(_ptr, M3Native.getPtr(canvas), dst._left, dst._top, dst._right, dst._bottom, flags);
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(canvas);
        }
    }

    /**
     * <p>Updates the animation state for |t|.</p>
     *
     * @param t   normalized [0..1] frame selector (0 → first frame, 1 → final frame)
     * @return    this
     */
    public M3Animation seek(float t) {
        return seek(t, null);
    }

    /**
     * <p>Updates the animation state for |t|.</p>
     *
     * @param t   normalized [0..1] frame selector (0 → first frame, 1 → final frame)
     * @param ic  invalidation controller (dirty region tracking)
     * @return    this
     */
    public M3Animation seek(float t, M3InvalidationController ic) {
        try {
            M3Stats.onNativeCall();
            _nSeek(_ptr, t, M3Native.getPtr(ic));
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(ic);
        }
    }

    /**
     * <p>Update the animation state to match |t|, specified as a frame index i.e.
     * relative to {@link getDuration()} * {@link getFPS()}.</p>
     *
     * <p>Fractional values are allowed and meaningful - e.g.
     * 0.0 → first frame 1.0 → second frame 0.5 → halfway between first and second frame</p>
     *
     * @param t   frame index
     * @return    this
     */
    public M3Animation seekFrame(float t) {
        return seekFrame(t, null);
    }

    /**
     * <p>Update the animation state to match |t|, specified as a frame index i.e.
     * relative to {@link getDuration()} * {@link getFPS()}.</p>
     *
     * <p>Fractional values are allowed and meaningful - e.g.
     * 0.0 → first frame 1.0 → second frame 0.5 → halfway between first and second frame</p>
     *
     * @param t   frame index
     * @param ic  invalidation controller (dirty region tracking)
     * @return    this
     */
    public M3Animation seekFrame(float t, M3InvalidationController ic) {
        try {
            M3Stats.onNativeCall();
            _nSeekFrame(_ptr, t, M3Native.getPtr(ic));
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(ic);
        }
    }

    /**
     * <p>Update the animation state to match t, specifed in frame time i.e.
     * relative to {@link getDuration()}.</p>
     *
     * @param t   frame time
     * @return    this
     */
    public M3Animation seekFrameTime(float t) {
        return seekFrameTime(t, null);
    }

    /**
     * <p>Update the animation state to match t, specifed in frame time i.e.
     * relative to {@link getDuration()}.</p>
     *
     * @param t   frame time
     * @param ic  invalidation controller (dirty region tracking)
     * @return    this
     */
    public M3Animation seekFrameTime(float t, M3InvalidationController ic) {
        try {
            M3Stats.onNativeCall();
            _nSeekFrameTime(_ptr, t, M3Native.getPtr(ic));
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(ic);
        }
    }

    /**
     * @return  the animation duration in seconds
     */
    public float getDuration() {
        try {
            M3Stats.onNativeCall();
            return _nGetDuration(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @return  the animation frame rate (frames / second)
     */
    public float getFPS() {
        try {
            M3Stats.onNativeCall();
            return _nGetFPS(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @return  Animation in point, in frame index units
     */
    public float getInPoint() {
        try {
            M3Stats.onNativeCall();
            return _nGetInPoint(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @return  Animation out point, in frame index units
     */
    public float getOutPoint() {
        try {
            M3Stats.onNativeCall();
            return _nGetOutPoint(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public String getVersion() {
        try {
            M3Stats.onNativeCall();
            return _nGetVersion(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3Point _size = null;

    public M3Point getSize() {
        if (_size == null) {
            _size = _nGetSize(_ptr);
        }
        return _size;
    }

    public float getWidth() {
        return getSize()._x;
    }

    public float getHeight() {
        return getSize()._y;
    }

    public static native long _nGetFinalizer();
    public static native long _nMakeFromString(String data);
    public static native long _nMakeFromFile(String path);
    public static native long _nMakeFromData(long dataPtr);
    public static native void _nRender(long ptr, long canvasPtr, float left, float top, float right, float bottom, int flags);
    public static native void _nSeek(long ptr, float t, long icPtr);
    public static native void _nSeekFrame(long ptr, float t, long icPtr);
    public static native void _nSeekFrameTime(long ptr, float t, long icPtr);
    public static native float _nGetDuration(long ptr);
    public static native float _nGetFPS(long ptr);
    public static native float _nGetInPoint(long ptr);
    public static native float _nGetOutPoint(long ptr);
    public static native String _nGetVersion(long ptr);
    public static native M3Point _nGetSize(long ptr);

}