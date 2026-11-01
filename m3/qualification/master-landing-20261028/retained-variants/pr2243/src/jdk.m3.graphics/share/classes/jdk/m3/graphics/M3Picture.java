package jdk.m3.graphics;

import java.util.function.*;
import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.*;

public class M3Picture extends M3RefCnt {
    static { M3Library.staticLoad(); }
    public M3Picture(long ptr) {
        super(ptr);
    }

    /**
     * Recreates Picture that was serialized into data. Returns constructed Picture
     * if successful; otherwise, returns null. Fails if data does not permit
     * constructing valid Picture.
     */
    public static M3Picture makeFromData(M3Data data) {
        try {
            M3Stats.onNativeCall();
            long ptr = _nMakeFromData(M3Native.getPtr(data));
            return ptr == 0 ? null : new M3Picture(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(data);
        }
    }

    /**
     * <p>Replays the drawing commands on the specified canvas. In the case that the
     * commands are recorded, each command in the Picture is sent separately to canvas.</p>
     *
     * <p>To add a single command to draw Picture to recording canvas, call
     * {@link Canvas#drawPicture} instead.</p>
     *
     * @param canvas  receiver of drawing commands
     * @return        this
     *
     * @see <a href="https://fiddle.skia.org/c/@Picture_playback">https://fiddle.skia.org/c/@Picture_playback</a>
     */
    public M3Picture playback(M3Canvas canvas) {
        return playback(canvas, null);
    }

    /**
     * <p>Replays the drawing commands on the specified canvas. In the case that the
     * commands are recorded, each command in the Picture is sent separately to canvas.</p>
     *
     * <p>To add a single command to draw Picture to recording canvas, call
     * {@link Canvas#drawPicture} instead.</p>
     *
     * @param canvas  receiver of drawing commands
     * @param abort   return true to interrupt the playback
     * @return        this
     *
     * @see <a href="https://fiddle.skia.org/c/@Picture_playback">https://fiddle.skia.org/c/@Picture_playback</a>
     */
    public M3Picture playback(M3Canvas canvas, BooleanSupplier abort) {
        try {
            M3Stats.onNativeCall();
            _nPlayback(_ptr, M3Native.getPtr(canvas), abort);
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(canvas);
        }
    }

    /**
     * <p>Returns cull Rect for this picture, passed in when Picture was created.
     * Returned Rect does not specify clipping Rect for Picture; cull is hint
     * of Picture bounds.</p>
     *
     * <p>Picture is free to discard recorded drawing commands that fall outside cull.</p>
     *
     * @return  bounds passed when Picture was created
     *
     * @see <a href="https://fiddle.skia.org/c/@Picture_cullRect">https://fiddle.skia.org/c/@Picture_cullRect</a>
     */
    public M3Rect getCullRect() {
        try {
            M3Stats.onNativeCall();
            return _nGetCullRect(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns a non-zero value unique among Picture in Skia process.
     *
     * @return  identifier for Picture
     */
    public int getUniqueId() {
        try {
            M3Stats.onNativeCall();
            return _nGetUniqueId(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @return  storage containing Data describing Picture.
     *
     * @see  <a href="https://fiddle.skia.org/c/@Picture_serialize">https://fiddle.skia.org/c/@Picture_serialize</a>
     */
    public M3Data serializeToData() {
        try {
            M3Stats.onNativeCall();
            return new M3Data(_nSerializeToData(_ptr));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Returns a placeholder Picture. Result does not draw, and contains only
     * cull Rect, a hint of its bounds. Result is immutable; it cannot be changed
     * later. Result identifier is unique.</p>
     *
     * <p>Returned placeholder can be intercepted during playback to insert other
     * commands into Canvas draw stream.</p>
     *
     * @param cull  placeholder dimensions
     * @return      placeholder with unique identifier
     *
     * @see <a href="https://fiddle.skia.org/c/@Picture_MakePlaceholder">https://fiddle.skia.org/c/@Picture_MakePlaceholder</a>
     */
    public static M3Picture makePlaceholder(M3Rect cull) {
        M3Stats.onNativeCall();
        return new M3Picture(_nMakePlaceholder(cull._left, cull._top, cull._right, cull._bottom));
    }

    /**
     * Returns the approximate number of operations in SkPicture. Returned value
     * may be greater or less than the number of SkCanvas calls
     * recorded: some calls may be recorded as more than one operation, other
     * calls may be optimized away.
     *
     * @return  approximate operation count
     *
     * @see <a href="https://fiddle.skia.org/c/@Picture_approximateOpCount">https://fiddle.skia.org/c/@Picture_approximateOpCount</a>
     */
    public int getApproximateOpCount() {
        try {
            M3Stats.onNativeCall();
            return _nGetApproximateOpCount(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns the approximate byte size of Picture. Does not include large objects
     * referenced by Picture.
     *
     * @return  approximate size
     *
     * @see <a href="https://fiddle.skia.org/c/@Picture_approximateBytesUsed">https://fiddle.skia.org/c/@Picture_approximateBytesUsed</a>
     */
    public long getApproximateBytesUsed() {
        try {
            M3Stats.onNativeCall();
            return _nGetApproximateBytesUsed(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Return a new shader that will draw with this picture. The tile rect is considered
     * equal to the picture bounds.
     *
     * @param tmx   The tiling mode to use when sampling in the x-direction.
     * @param tmy   The tiling mode to use when sampling in the y-direction.
     * @param mode  How to filter the tiles
     * @return      Returns a new shader object. Note: this function never returns null.
     */
    public M3Shader makeShader(M3FilterTileMode tmx,
                             M3FilterTileMode tmy,
                             M3FilterMode mode) {
        return makeShader(tmx, tmy, mode, null, null);
    }

    /**
     * Return a new shader that will draw with this picture. The tile rect is considered
     * equal to the picture bounds.
     *
     * @param tmx          The tiling mode to use when sampling in the x-direction.
     * @param tmy          The tiling mode to use when sampling in the y-direction.
     * @param mode         How to filter the tiles
     * @param localMatrix  Optional matrix used when sampling
     * @return             Returns a new shader object. Note: this function never returns null.
     */
    public M3Shader makeShader(M3FilterTileMode tmx,
                             M3FilterTileMode tmy,
                             M3FilterMode mode,
                             M3Matrix33 localMatrix) {
        return makeShader(tmx, tmy, mode, localMatrix, null);
    }

    /**
     * Return a new shader that will draw with this picture.
     *
     * @param tmx          The tiling mode to use when sampling in the x-direction.
     * @param tmy          The tiling mode to use when sampling in the y-direction.
     * @param mode         How to filter the tiles
     * @param localMatrix  Optional matrix used when sampling
     * @param tileRect     The tile rectangle in picture coordinates: this represents the subset
     *                     (or superset) of the picture used when building a tile. It is not
     *                     affected by localMatrix and does not imply scaling (only translation
     *                     and cropping). If null, the tile rect is considered equal to the picture
     *                     bounds.
     * @return             Returns a new shader object. Note: this function never returns null.
     */
    public M3Shader makeShader(M3FilterTileMode tmx,
                             M3FilterTileMode tmy,
                             M3FilterMode mode,
                             M3Matrix33 localMatrix,
                             M3Rect tileRect)
    {
        try {
            M3Stats.onNativeCall();
            float[] arr = localMatrix == null ? null : localMatrix._mat;
            return new M3Shader(_nMakeShader(_ptr, tmx.ordinal(), tmy.ordinal(), mode.ordinal(), arr, tileRect));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public static native long _nMakeFromData(long dataPtr /*, SkDeserialProcs */);
    public static native void _nPlayback(long ptr, long canvasPtr, BooleanSupplier abort);
    public static native M3Rect _nGetCullRect(long ptr);
    public static native int  _nGetUniqueId(long ptr);
    public static native long _nSerializeToData(long ptr /*, SkSerialProcs */);
    public static native long _nMakePlaceholder(float left, float top, float right, float bottom);
    public static native int  _nGetApproximateOpCount(long ptr);
    public static native long _nGetApproximateBytesUsed(long ptr);
    public static native long _nMakeShader(long ptr, int tmx, int tmy, int filterMode, float[] localMatrix, M3Rect tileRect);
}