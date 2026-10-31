package jdk.m3.graphics;

import java.io.*;
import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.*;

/**
 * <p>Base class for objects that draw into Canvas.</p>
 *
 * <p>The object has a generation id, which is guaranteed to be unique across all drawables. To
 * allow for clients of the drawable that may want to cache the results, the drawable must
 * change its generation id whenever its internal state changes such that it will draw differently.</p>
 */
public abstract class M3Drawable extends M3RefCnt implements M3Flattenable {
    static { M3Library.staticLoad(); }
    public M3Rect _bounds = null;

    public M3Drawable() {
        super(_nMake());
        M3Stats.onNativeCall();
        M3Stats.onNativeCall();
        _nInit(_ptr);
    }

    /**
     * Draws into the specified content. The drawing sequence will be balanced upon return
     * (i.e. the saveLevel() on the canvas will match what it was when draw() was called,
     * and the current matrix and clip settings will not be changed.
     */
    public M3Drawable draw(M3Canvas canvas) {
        return draw(canvas, null);
    }

    /**
     * Draws into the specified content. The drawing sequence will be balanced upon return
     * (i.e. the saveLevel() on the canvas will match what it was when draw() was called,
     * and the current matrix and clip settings will not be changed.
     */
    public M3Drawable draw(M3Canvas canvas, float x, float y) {
        return draw(canvas, M3Matrix33.makeTranslate(x, y));
    }

    /**
     * Draws into the specified content. The drawing sequence will be balanced upon return
     * (i.e. the saveLevel() on the canvas will match what it was when draw() was called,
     * and the current matrix and clip settings will not be changed.
     */
    public M3Drawable draw(M3Canvas canvas, M3Matrix33 matrix) {
        try {
            M3Stats.onNativeCall();
            _nDraw(_ptr, M3Native.getPtr(canvas), matrix == null ? null : matrix._mat);
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(canvas);
        }
    }

    public M3Picture makePictureSnapshot() {
        try {
            M3Stats.onNativeCall();
            return new M3Picture(_nMakePictureSnapshot(_ptr));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Return a unique value for this instance. If two calls to this return the same value,
     * it is presumed that calling the draw() method will render the same thing as well.</p>
     *
     * <p>Subclasses that change their state should call notifyDrawingChanged() to ensure that
     * a new value will be returned the next time it is called.</p>
     */
    public int getGenerationId() {
        try {
            M3Stats.onNativeCall();
            return _nGetGenerationId(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Return the (conservative) bounds of what the drawable will draw. If the drawable can
     * change what it draws (e.g. animation or in response to some external change), then this
     * must return a bounds that is always valid for all possible states.
     */
    public M3Rect getBounds() {
        if (_bounds == null)
            _bounds = onGetBounds();
        return _bounds;
    }

    /**
     * Calling this invalidates the previous generation ID, and causes a new one to be computed
     * the next time getGenerationId() is called. Typically this is called by the object itself,
     * in response to its internal state changing.
     */
    public M3Drawable notifyDrawingChanged() {
        M3Stats.onNativeCall();
        _nNotifyDrawingChanged(_ptr);
        _bounds = null;
        return this;
    }

    public abstract void onDraw(M3Canvas canvas);

    public abstract M3Rect onGetBounds();

    public void _onDraw(long canvasPtr) {
        onDraw(new M3Canvas(canvasPtr, false, this));
    }

    public static native long _nMake();
    public        native void _nInit(long ptr);
    public static native void _nDraw(long ptr, long canvasPtr, float[] matrix);
    public static native long _nMakePictureSnapshot(long ptr);
    public static native int  _nGetGenerationId(long ptr);
    public static native void _nNotifyDrawingChanged(long ptr);
}
