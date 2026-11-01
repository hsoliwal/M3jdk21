package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;

/**
 * A utility proxy base class for implementing draw/paint filters.
 */
public abstract class M3PaintFilterCanvas extends M3Canvas {
    static { M3Library.staticLoad(); }
    /**
     * @param unrollDrawable if needed to filter nested drawable content using this canvas (for drawables there is no paint to filter)
     */
    public M3PaintFilterCanvas(M3Canvas canvas, boolean unrollDrawable) {
        super(_nMake(M3Native.getPtr(canvas), unrollDrawable), true, canvas);
        M3Stats.onNativeCall();
        _nAttachToJava(_ptr);
        M3Stats.onNativeCall();
        M3ReferenceUtil.reachabilityFence(canvas);
    }

    /**
     *  Called with the paint that will be used to draw the specified type.
     *  The implementation may modify the paint as they wish.
     *
     *  The result boolean is used to determine whether the draw op is to be
     *  executed (true) or skipped (false).
     *
     *  Note: The base implementation calls onFilter() for top-level/explicit paints only.
     */
    public abstract boolean onFilter(M3Paint paint);

    public final boolean onFilter(long paintPtr) {
        M3Paint paint = new M3Paint(paintPtr, false);
        return onFilter(paint);
    }

    public        native void _nAttachToJava(long canvasPtr);
    public static native long _nMake(long canvasPtr, boolean unrollDrawable);
}
