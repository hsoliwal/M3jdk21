package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.*;

public class M3Paint extends M3Managed {
    static { M3Library.staticLoad(); }

    public static class _FinalizerHolder {
        public static final long PTR = _nGetFinalizer();
    }
    public M3Paint(long ptr, boolean managed) {
        super(ptr, _FinalizerHolder.PTR, managed);
    }

    /**
     * Constructs SkPaint with default values.
     *
     * @see <a href="https://fiddle.skia.org/c/@Paint_empty_constructor">https://fiddle.skia.org/c/@Paint_empty_constructor</a>
    */
    public M3Paint() {
        super(_nMake(), _FinalizerHolder.PTR);
        M3Stats.onNativeCall();
    }

    /**
     * <p>Makes a shallow copy of Paint. PathEffect, Shader,
     * MaskFilter, ColorFilter, and ImageFilter are shared
     * between the original paint and the copy.</p>
     *
     * <p>The referenced objects PathEffect, Shader, MaskFilter, ColorFilter,
     * and ImageFilter cannot be modified after they are created.</p>
     *
     * @return  shallow copy of paint
     *
     * @see <a href="https://fiddle.skia.org/c/@Paint_copy_const_SkPaint">https://fiddle.skia.org/c/@Paint_copy_const_SkPaint</a>
     */
    public M3Paint makeClone() {
        try {
            M3Stats.onNativeCall();
            return new M3Paint(_nMakeClone(_ptr), true);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    @Override
    public boolean _nativeEquals(M3Native other) {
        try {
            return _nEquals(_ptr, M3Native.getPtr(other));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(other);
        }
    }

    /**
     * Sets all Paint contents to their initial values. This is equivalent to replacing
     * Paint with the result of Paint().
     *
     * @see <a href="https://fiddle.skia.org/c/@Paint_reset">https://fiddle.skia.org/c/@Paint_reset</a>
     */
    public M3Paint reset() {
        M3Stats.onNativeCall();
        _nReset(_ptr);
        return this;
    }

    /**
     * Returns true if pixels on the active edges of Path may be drawn with partial transparency.
     *
     * @return  antialiasing state
     */
    public boolean isAntiAlias() {
        try {
            M3Stats.onNativeCall();
            return _nIsAntiAlias(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Requests, but does not require, that edge pixels draw opaque or with partial transparency.
     *
     * @param value  setting for antialiasing
     */
    public M3Paint setAntiAlias(boolean value) {
        M3Stats.onNativeCall();
        _nSetAntiAlias(_ptr, value);
        return this;
    }

    /**
     * @return  true if color error may be distributed to smooth color transition.
     */
    public boolean isDither() {
        try {
            M3Stats.onNativeCall();
            return _nIsDither(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Requests, but does not require, to distribute color error.
     *
     * @param value  setting for ditering
     * @return       this
     */
    public M3Paint setDither(boolean value) {
        M3Stats.onNativeCall();
        _nSetDither(_ptr, value);
        return this;
    }

    /**
     * @return  whether the geometry is filled, stroked, or filled and stroked.
     */
    public M3PaintMode getMode() {
        try {
            M3Stats.onNativeCall();
            return M3PaintMode._values[_nGetMode(_ptr)];
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Sets whether the geometry is filled, stroked, or filled and stroked.
     *
     * @see <a href="https://fiddle.skia.org/c/@Paint_setStyle">https://fiddle.skia.org/c/@Paint_setStyle</a>
     * @see <a href="https://fiddle.skia.org/c/@Stroke_Width">https://fiddle.skia.org/c/@Stroke_Width</a>
     */
    public M3Paint setMode(M3PaintMode style) {
        assert style != null : "Paint::setMode expected style != null";
        M3Stats.onNativeCall();
        _nSetMode(_ptr, style.ordinal());
        return this;
    }

    /**
     * Set paint's mode to STROKE if true, or FILL if false.
     *
     * @param value  stroke or fill
     * @return       this
     */
    public M3Paint setStroke(boolean value) {
        return setMode(value ? M3PaintMode.STROKE : M3PaintMode.FILL);
    }

    /**
     * Retrieves alpha and RGB, unpremultiplied, packed into 32 bits.
     * Use helpers {@link Color#getA(int)}, {@link Color#getR(int)}, {@link Color#getG(int)}, and {@link Color#getB(int)} to extract
     * a color component.
     *
     * @return  unpremultiplied ARGB
     */
    public int getColor() {
        try {
            M3Stats.onNativeCall();
            return _nGetColor(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Retrieves alpha and RGB, unpremultiplied, as four floating point values. RGB are
     * extended sRGB values (sRGB gamut, and encoded with the sRGB transfer function).
     *
     * @return  unpremultiplied RGBA
     */
    public M3Color4f getColor4f() {
        try {
            M3Stats.onNativeCall();
            return _nGetColor4f(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Sets alpha and RGB used when stroking and filling. The color is a 32-bit value,
     * unpremultiplied, packing 8-bit components for alpha, red, blue, and green.
     *
     * @param color  unpremultiplied ARGB
     *
     * @see <a href="https://fiddle.skia.org/c/@Paint_setColor">https://fiddle.skia.org/c/@Paint_setColor</a>
     */
    public M3Paint setColor(int color) {
        M3Stats.onNativeCall();
        _nSetColor(_ptr, color);
        return this;
    }

    /**
     * Sets alpha and RGB used when stroking and filling. The color is four floating
     * point values, unpremultiplied. The color values are interpreted as being in sRGB.
     *
     * @param color       unpremultiplied RGBA
     * @return            this
     */
    public M3Paint setColor4f(M3Color4f color) {
        return setColor4f(color, null);
    }

    /**
     * Sets alpha and RGB used when stroking and filling. The color is four floating
     * point values, unpremultiplied. The color values are interpreted as being in
     * the colorSpace. If colorSpace is nullptr, then color is assumed to be in the
     * sRGB color space.
     *
     * @param color       unpremultiplied RGBA
     * @param colorSpace  SkColorSpace describing the encoding of color
     * @return            this
     */
    public M3Paint setColor4f(M3Color4f color, M3ColorSpace colorSpace) {
        try {
            assert color != null : "Paint::setColor4f expected color != null";
            M3Stats.onNativeCall();
            _nSetColor4f(_ptr, color.getR(), color.getG(), color.getB(), color.getA(), M3Native.getPtr(colorSpace));
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(colorSpace);
        }
    }

    /**
     * Retrieves alpha from the color used when stroking and filling.
     *
     * @return  alpha ranging from 0f, fully transparent, to 1f, fully opaque
     */
    public float getAlphaf() {
        return getColor4f().getA();
    }

    /**
     * Retrieves alpha from the color used when stroking and filling.
     *
     * @return  alpha ranging from 0, fully transparent, to 255, fully opaque
     */
    public int getAlpha() {
        return Math.round(getAlphaf() * 255f);
    }

    /**
     * <p>Replaces alpha, leaving RGB unchanged. An out of range value triggers
     * an assert in the debug build. a is a value from 0f to 1f.</p>
     *
     * <p>a set to zero makes color fully transparent; a set to 1.0 makes color
     * fully opaque.</p>
     *
     * @param a  alpha component of color
     * @return   this
     */
    public M3Paint setAlphaf(float a) {
        return setColor4f(getColor4f().withA(a));
    }

    /**
     * <p>Replaces alpha, leaving RGB unchanged. An out of range value triggers
     * an assert in the debug build. a is a value from 0 to 255.</p>
     *
     * <p>a set to zero makes color fully transparent; a set to 255 makes color
     * fully opaque.</p>
     *
     * @param a  alpha component of color
     * @return   this
     */
    public M3Paint setAlpha(int a) {
        return setAlphaf(a / 255f);
    }

    /**
     * Sets color used when drawing solid fills. The color components range from 0 to 255.
     * The color is unpremultiplied; alpha sets the transparency independent of RGB.
     *
     * @param a  amount of alpha, from fully transparent (0) to fully opaque (255)
     * @param r  amount of red, from no red (0) to full red (255)
     * @param g  amount of green, from no green (0) to full green (255)
     * @param b  amount of blue, from no blue (0) to full blue (255)
     *
     * @see <a href="https://fiddle.skia.org/c/@Paint_setARGB">https://fiddle.skia.org/c/@Paint_setARGB</a>
     */
    public M3Paint setARGB(int a, int r, int g, int b) {
        M3Stats.onNativeCall();
        _nSetColor4f(_ptr, r / 255f, g / 255f, b / 255f, a / 255f, 0);
        return this;
    }

    /**
     * Returns the thickness of the pen used by Paint to outline the shape.
     *
     * @return  zero for hairline, greater than zero for pen thickness
     */
    public float getStrokeWidth() {
        try {
            M3Stats.onNativeCall();
            return _nGetStrokeWidth(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Sets the thickness of the pen used by the paint to outline the shape.
     * A stroke-width of zero is treated as "hairline" width. Hairlines are always exactly one
     * pixel wide in device space (their thickness does not change as the canvas is scaled).
     * Negative stroke-widths are invalid; setting a negative width will have no effect.
     *
     * @param width  zero thickness for hairline; greater than zero for pen thickness
     *
     * @see <a href="https://fiddle.skia.org/c/@Miter_Limit">https://fiddle.skia.org/c/@Miter_Limit</a>
     * @see <a href="https://fiddle.skia.org/c/@Paint_setStrokeWidth">https://fiddle.skia.org/c/@Paint_setStrokeWidth</a>
     */
    public M3Paint setStrokeWidth(float width) {
        M3Stats.onNativeCall();
        _nSetStrokeWidth(_ptr, width);
        return this;
    }

    /**
     * Returns the limit at which a sharp corner is drawn beveled.
     *
     * @return  zero and greater miter limit
     */
    public float getStrokeMiter() {
        try {
            M3Stats.onNativeCall();
            return _nGetStrokeMiter(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Sets the limit at which a sharp corner is drawn beveled.
     * Valid values are zero and greater.
     * Has no effect if miter is less than zero.
     *
     * @param miter  zero and greater miter limit
     * @return       this
     *
     * @see <a href="https://fiddle.skia.org/c/@Paint_setStrokeMiter">https://fiddle.skia.org/c/@Paint_setStrokeMiter</a>
     */
    public M3Paint setStrokeMiter(float miter) {
        M3Stats.onNativeCall();
        _nSetStrokeMiter(_ptr, miter);
        return this;
    }

    /**
     * @return  the geometry drawn at the beginning and end of strokes.
     */
    public M3PaintStrokeCap getStrokeCap() {
        try {
            M3Stats.onNativeCall();
            return M3PaintStrokeCap._values[_nGetStrokeCap(_ptr)];
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Sets the geometry drawn at the beginning and end of strokes.
     *
     * @return  this
     *
     * @see <a href="https://fiddle.skia.org/c/@Paint_setStrokeCap_a">https://fiddle.skia.org/c/@Paint_setStrokeCap_a</a>
     * @see <a href="https://fiddle.skia.org/c/@Paint_setStrokeCap_b">https://fiddle.skia.org/c/@Paint_setStrokeCap_b</a>
     */
    public M3Paint setStrokeCap(M3PaintStrokeCap cap) {
        assert cap != null : "Paint::setStrokeCap expected cap != null";
        M3Stats.onNativeCall();
        _nSetStrokeCap(_ptr, cap.ordinal());
        return this;
    }

    /**
     * @return  the geometry drawn at the corners of strokes.
     */
    public M3PaintStrokeJoin getStrokeJoin() {
        try {
            M3Stats.onNativeCall();
            return M3PaintStrokeJoin._values[_nGetStrokeJoin(_ptr)];
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Sets the geometry drawn at the corners of strokes.
     *
     * @return  this
     *
     * @see <a href="https://fiddle.skia.org/c/@Paint_setStrokeJoin">https://fiddle.skia.org/c/@Paint_setStrokeJoin</a>
     */
    public M3Paint setStrokeJoin(M3PaintStrokeJoin join) {
        assert join != null : "Paint::setStrokeJoin expected join != null";
        M3Stats.onNativeCall();
        _nSetStrokeJoin(_ptr, join.ordinal());
        return this;
    }

    /**
     * Returns the filled equivalent of the stroked path.
     *
     * @param src       Path read to create a filled version
     * @return          resulting Path
     */
    public M3Path getFillPath(M3Path src) {
        return getFillPath(src, null, 1);
    }

    /**
     * Returns the filled equivalent of the stroked path.
     *
     * @param src       Path read to create a filled version
     * @param cull      Optional limit passed to PathEffect
     * @param resScale  if &gt; 1, increase precision, else if (0 &lt; resScale &lt; 1) reduce precision
     *                  to favor speed and size
     * @return          resulting Path
     */
    public M3Path getFillPath(M3Path src, M3Rect cull, float resScale) {
        try {
            assert src != null : "Paint::getFillPath expected src != null";
            M3Stats.onNativeCall();
            if (cull == null)
                return new M3Path(_nGetFillPath(_ptr, M3Native.getPtr(src), resScale));
            else
                return new M3Path(_nGetFillPathCull(_ptr, M3Native.getPtr(src), cull._left, cull._top, cull._right, cull._bottom, resScale));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(src);
        }
    }

    /**
     * @return  {@link Shader} or null
     * @see     <a href="https://fiddle.skia.org/c/@Paint_refShader">https://fiddle.skia.org/c/@Paint_refShader</a>
     */
    public M3Shader getShader() {
        try {
            M3Stats.onNativeCall();
            long shaderPtr = _nGetShader(_ptr);
            return shaderPtr == 0 ? null : new M3Shader(shaderPtr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @param shader  how geometry is filled with color; if null, color is used instead
     *
     * @see <a href="https://fiddle.skia.org/c/@Color_Filter_Methods">https://fiddle.skia.org/c/@Color_Filter_Methods</a>
     * @see <a href="https://fiddle.skia.org/c/@Paint_setShader">https://fiddle.skia.org/c/@Paint_setShader</a>
     */
    public M3Paint setShader(M3Shader shader) {
        try {
            M3Stats.onNativeCall();
            _nSetShader(_ptr, M3Native.getPtr(shader));
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(shader);
        }
    }

    /**
     * @return  {@link ColorFilter} or null
     * @see     <a href="https://fiddle.skia.org/c/@Paint_refColorFilter">https://fiddle.skia.org/c/@Paint_refColorFilter</a>
     */
    public M3ColorFilter getColorFilter() {
        try {
            M3Stats.onNativeCall();
            long colorFilterPtr = _nGetColorFilter(_ptr);
            return colorFilterPtr == 0 ? null : new M3ColorFilter(colorFilterPtr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @param colorFilter {@link ColorFilter} to apply to subsequent draw
     *
     * @see <a href="https://fiddle.skia.org/c/@Blend_Mode_Methods">https://fiddle.skia.org/c/@Blend_Mode_Methods</a>
     * @see <a href="https://fiddle.skia.org/c/@Paint_setColorFilter">https://fiddle.skia.org/c/@Paint_setColorFilter</a>
     */
    public M3Paint setColorFilter(M3ColorFilter colorFilter) {
        try {
            M3Stats.onNativeCall();
            _nSetColorFilter(_ptr, M3Native.getPtr(colorFilter));
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(colorFilter);
        }
    }

    /**
     * Returns optional Blender.
     *
     * @return  Blender if previously set, null otherwise
     */
    public M3Blender getBlender() {
        try {
            M3Stats.onNativeCall();
            long blenderPtr = _nGetBlender(_ptr);
            return blenderPtr == 0 ? null : new M3Blender(blenderPtr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Sets Blender to blender, decreasing reference count of the previous Blender.
     *
     * @param blender  Blender to apply to Paint
     * @return         this Paint object
     */
    public M3Paint setBlender(M3Blender blender) {
        try {
            M3Stats.onNativeCall();
            _nSetBlender(_ptr, M3Native.getPtr(blender));
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(blender);
        }
    }

    /**
     * Returns BlendMode. By default, returns {@link BlendMode#SRC_OVER}.
     *
     * @return  mode used to combine source color with destination color
     */
    public M3BlendMode getBlendMode() {
        try {
            M3Stats.onNativeCall();
            return M3BlendMode._values[_nGetBlendMode(_ptr)];
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @return  true if BlendMode is BlendMode.SRC_OVER, the default.
     */
    public boolean isSrcOver() {
        return getBlendMode() == M3BlendMode.SRC_OVER;
    }

    /**
     * Sets SkBlendMode to mode. Does not check for valid input.
     *
     * @param mode  BlendMode used to combine source color and destination
     * @return      this
     */
    public M3Paint setBlendMode(M3BlendMode mode) {
        assert mode != null : "Paint::setBlendMode expected mode != null";
        M3Stats.onNativeCall();
        _nSetBlendMode(_ptr, mode.ordinal());
        return this;
    }

    /**
     * @return  {@link PathEffect} or null
     * @see     <a href="https://fiddle.skia.org/c/@Paint_refPathEffect">https://fiddle.skia.org/c/@Paint_refPathEffect</a>
     */
    public M3PathEffect getPathEffect() {
        try {
            M3Stats.onNativeCall();
            long pathEffectPtr = _nGetPathEffect(_ptr);
            return pathEffectPtr == 0 ? null : new M3PathEffect(pathEffectPtr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @param p  replace {@link Path} with a modification when drawn
     *
     * @see <a href="https://fiddle.skia.org/c/@Mask_Filter_Methods">https://fiddle.skia.org/c/@Mask_Filter_Methods</a>
     * @see <a href="https://fiddle.skia.org/c/@Paint_setPathEffect">https://fiddle.skia.org/c/@Paint_setPathEffect</a>
     */
    public M3Paint setPathEffect(M3PathEffect p) {
        try {
            M3Stats.onNativeCall();
            _nSetPathEffect(_ptr, M3Native.getPtr(p));
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(p);
        }
    }

    /**
     * @return  {@link MaskFilter} if previously set, null otherwise
     * @see     <a href="https://fiddle.skia.org/c/@Paint_refMaskFilter">https://fiddle.skia.org/c/@Paint_refMaskFilter</a>
     */
    public M3MaskFilter getMaskFilter() {
        try {
            M3Stats.onNativeCall();
            long maskFilterPtr = _nGetMaskFilter(_ptr);
            return maskFilterPtr == 0 ? null : new M3MaskFilter(maskFilterPtr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @param maskFilter  modifies clipping mask generated from drawn geometry
     * @return            this
     *
     * @see  <a href="https://fiddle.skia.org/c/@Paint_setMaskFilter">https://fiddle.skia.org/c/@Paint_setMaskFilter</a>
     * @see  <a href="https://fiddle.skia.org/c/@Typeface_Methods">https://fiddle.skia.org/c/@Typeface_Methods</a>
     */
    public M3Paint setMaskFilter(M3MaskFilter maskFilter) {
        try {
            M3Stats.onNativeCall();
            _nSetMaskFilter(_ptr, M3Native.getPtr(maskFilter));
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(maskFilter);
        }
    }

    /**
     * @return  {@link ImageFilter} or null
     * @see     <a href="https://fiddle.skia.org/c/@Paint_refImageFilter">https://fiddle.skia.org/c/@Paint_refImageFilter</a>
     */
    public M3ImageFilter getImageFilter() {
        try {
            M3Stats.onNativeCall();
            long imageFilterPtr = _nGetImageFilter(_ptr);
            return imageFilterPtr == 0 ? null : new M3ImageFilter(imageFilterPtr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @param imageFilter  how SkImage is sampled when transformed
     *
     * @see <a href="https://fiddle.skia.org/c/@Draw_Looper_Methods">https://fiddle.skia.org/c/@Draw_Looper_Methods</a>
     * @see <a href="https://fiddle.skia.org/c/@Paint_setImageFilter">https://fiddle.skia.org/c/@Paint_setImageFilter</a>
     */
    public M3Paint setImageFilter(M3ImageFilter imageFilter) {
        try {
            M3Stats.onNativeCall();
            _nSetImageFilter(_ptr, M3Native.getPtr(imageFilter));
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(imageFilter);
        }
    }

    /**
     * <p>Returns true if Paint prevents all drawing;
     * otherwise, the Paint may or may not allow drawing.</p>
     *
     * <p>Returns true if, for example, BlendMode combined with alpha computes a
     * new alpha of zero.</p>
     *
     * @return  true if Paint prevents all drawing
     *
     * @see <a href="https://fiddle.skia.org/c/@Paint_nothingToDraw">https://fiddle.skia.org/c/@Paint_nothingToDraw</a>
     */
    public boolean hasNothingToDraw() {
        try {
            M3Stats.onNativeCall();
            return _nHasNothingToDraw(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public static native long  _nGetFinalizer();
    public static native long  _nMake();
    public static native long  _nMakeClone(long ptr);
    public static native boolean _nEquals(long ptr, long otherPtr);
    public static native void _nReset(long ptr);
    public static native boolean _nIsAntiAlias(long ptr);
    public static native void  _nSetAntiAlias(long ptr, boolean value);
    public static native boolean _nIsDither(long ptr);
    public static native void  _nSetDither(long ptr, boolean value);
    public static native int   _nGetMode(long ptr);
    public static native void  _nSetMode(long ptr, int value);
    public static native int   _nGetColor(long ptr);
    public static native M3Color4f _nGetColor4f(long ptr);
    public static native void  _nSetColor(long ptr, int argb);
    public static native void  _nSetColor4f(long ptr, float r, float g, float b, float a, long colorSpacePtr);
    public static native float _nGetStrokeWidth(long ptr);
    public static native void  _nSetStrokeWidth(long ptr, float value);
    public static native float _nGetStrokeMiter(long ptr);
    public static native void  _nSetStrokeMiter(long ptr, float value);
    public static native int   _nGetStrokeCap(long ptr);
    public static native void  _nSetStrokeCap(long ptr, int value);
    public static native int   _nGetStrokeJoin(long ptr);
    public static native void  _nSetStrokeJoin(long ptr, int value);
    public static native long  _nGetFillPath(long ptr, long path, float resScale);
    public static native long  _nGetFillPathCull(long ptr, long path, float left, float top, float right, float bottom, float resScale);
    public static native long  _nGetShader(long ptr);
    public static native void  _nSetShader(long ptr, long shaderPtr);
    public static native long  _nGetColorFilter(long ptr);
    public static native void  _nSetColorFilter(long ptr, long colorFilterPtr);
    public static native long  _nGetBlender(long ptr);
    public static native void  _nSetBlender(long ptr, long blenderPtr);
    public static native int   _nGetBlendMode(long ptr);
    public static native void  _nSetBlendMode(long ptr, int mode);
    public static native long  _nGetPathEffect(long ptr);
    public static native void  _nSetPathEffect(long ptr, long pathEffectPtr);
    public static native long  _nGetMaskFilter(long ptr);
    public static native void  _nSetMaskFilter(long ptr, long filterPtr);
    public static native long  _nGetImageFilter(long ptr);
    public static native void  _nSetImageFilter(long ptr, long filterPtr);
    public static native boolean _nHasNothingToDraw(long ptr);
}
