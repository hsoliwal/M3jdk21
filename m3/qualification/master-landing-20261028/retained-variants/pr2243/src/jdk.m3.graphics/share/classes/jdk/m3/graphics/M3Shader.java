package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.*;

public class M3Shader extends M3RuntimeEffectChild {
    static { M3Library.staticLoad(); }
    /**
     * Returns true if the shader is guaranteed to produce only opaque
     * colors, subject to the {@link Paint} using the shader to apply an opaque
     * alpha value. Subclasses should override this to allow some
     * optimizations.
     */
    public boolean isOpaque() {
        try {
            M3Stats.onNativeCall();
            return _nIsOpaque(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Iff this shader is backed by a single {@link Image}, return its ptr (the caller must ref this
     * if they want to keep it longer than the lifetime of the shader). If not, return nullptr.
     */
    public M3Image getImage() {
        try {
            M3Stats.onNativeCall();
            long ptr = _nGetImage(_ptr);
            return ptr == 0 ? null : new M3Image(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Return a shader that will apply the specified localMatrix to this shader.
     * The specified matrix will be applied before any matrix associated with this shader.
     */
    public M3Shader makeWithLocalMatrix(M3Matrix33 localMatrix) {
        try {
            M3Stats.onNativeCall();
            return new M3Shader(_nMakeWithLocalMatrix(_ptr, localMatrix._mat));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Return a shader that will compute this shader in a context such that any child shaders
     * return RGBA values converted to the {@code input} colorspace.
     *
     * <p>It is then assumed that the RGBA values returned by this shader have been transformed into
     * {@code input} by the shader being wrapped.  By default, shaders are assumed to return values
     * in the destination colorspace and premultiplied. Using a different output than input
     * allows custom shaders to replace the color management Skia normally performs w/o forcing
     * authors to otherwise manipulate surface/image color info to avoid unnecessary or incorrect
     * work.
     *
     * <p>A null input is assumed to be the destination CS.
     *
     * @param input the {@link ColorSpace} to use for input
     */
    public M3Shader makeWithWorkingColorSpace(M3ColorSpace input) {
        return makeWithWorkingColorSpace(input, null);
    }

    /**
     * Return a shader that will compute this shader in a context such that any child shaders
     * return RGBA values converted to the {@code input} colorspace.
     *
     * <p>It is then assumed that the RGBA values returned by this shader have been transformed into
     * {@code output} by the shader being wrapped.  By default, shaders are assumed to return values
     * in the destination colorspace and premultiplied. Using a different output than input
     * allows custom shaders to replace the color management Skia normally performs w/o forcing
     * authors to otherwise manipulate surface/image color info to avoid unnecessary or incorrect
     * work.
     *
     * <p>If the shader is not performing colorspace conversion but needs to operate in the {@code input}
     * then it should have {@code output} be the same as {@code input}. Regardless of the {@code output} here,
     * the RGBA values of the returned {@link Shader} are always converted from {@code output} to the
     * destination surface color space.
     *
     * <p>A null input is assumed to be the destination CS.
     * A null output is assumed to be the input.
     *
     * @param input the {@link ColorSpace} to use for input
     * @param output the {@link ColorSpace} to use for output, or null
     */
    public M3Shader makeWithWorkingColorSpace(M3ColorSpace input, M3ColorSpace output) {
        try {
            M3Stats.onNativeCall();
            return new M3Shader(_nMakeWithWorkingColorSpace(_ptr, M3Native.getPtr(input), M3Native.getPtr(output)));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(input);
            M3ReferenceUtil.reachabilityFence(output);
        }
    }

    public M3Shader makeWithColorFilter(M3ColorFilter f) {
        try {
            return new M3Shader(_nMakeWithColorFilter(_ptr, M3Native.getPtr(f)));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(f);
        }
    }

    // Linear

    public static M3Shader makeLinearGradient(M3Point p0, M3Point p1, int[] colors) {
        return makeLinearGradient(p0._x, p0._y, p1._x, p1._y, colors);
    }

    public static M3Shader makeLinearGradient(float x0, float y0, float x1, float y1, int[] colors) {
        return makeLinearGradient(x0, y0, x1, y1, colors, null, M3GradientStyle.DEFAULT);
    }
    public static M3Shader makeLinearGradient(M3Point p0, M3Point p1, int[] colors, float[] positions) {
        return makeLinearGradient(p0._x, p0._y, p1._x, p1._y, colors, positions);
    }

    public static M3Shader makeLinearGradient(float x0, float y0, float x1, float y1, int[] colors, float[] positions) {
        return makeLinearGradient(x0, y0, x1, y1, colors, positions, M3GradientStyle.DEFAULT);
    }

    public static M3Shader makeLinearGradient(M3Point p0, M3Point p1, int[] colors, float[] positions, M3GradientStyle style) {
        return makeLinearGradient(p0._x, p0._y, p1._x, p1._y, colors, positions, style);
    }

    public static M3Shader makeLinearGradient(float x0, float y0, float x1, float y1, int[] colors, float[] positions, M3GradientStyle style) {
        assert positions == null || colors.length == positions.length : "colors.length " + colors.length + "!= positions.length " + positions.length;
        M3Stats.onNativeCall();
        return new M3Shader(_nMakeLinearGradient(x0, y0, x1, y1, colors, positions, style.getTileMode().ordinal(), style._getFlags(), style._getMatrixArray()));
    }

    public static M3Shader makeLinearGradient(M3Point p0, M3Point p1, M3Color4f[] colors, M3ColorSpace cs, float[] positions, M3GradientStyle style) {
        return makeLinearGradient(p0._x, p0._y, p1._x, p1._y, colors, cs, positions, style);
    }

    public static M3Shader makeLinearGradient(float x0, float y0, float x1, float y1, M3Color4f[] colors, M3ColorSpace cs, float[] positions, M3GradientStyle style) {
        try {
            assert positions == null || colors.length == positions.length : "colors.length " + colors.length + "!= positions.length " + positions.length;
            M3Stats.onNativeCall();
            return new M3Shader(_nMakeLinearGradientCS(x0, y0, x1, y1, M3Color4f.flattenArray(colors), M3Native.getPtr(cs), positions, style.getTileMode().ordinal(), style._getFlags(), style._getMatrixArray()));
        } finally {
            M3ReferenceUtil.reachabilityFence(cs);
        }
    }

    // Radial

    public static M3Shader makeRadialGradient(M3Point center, float r, int[] colors) {
        return makeRadialGradient(center._x, center._y, r, colors);
    }

    public static M3Shader makeRadialGradient(float x, float y, float r, int[] colors) {
        return makeRadialGradient(x, y, r, colors, null, M3GradientStyle.DEFAULT);
    }

    public static M3Shader makeRadialGradient(M3Point center, float r, int[] colors, float[] positions) {
        return makeRadialGradient(center._x, center._y, r, colors, positions);
    }

    public static M3Shader makeRadialGradient(float x, float y, float r, int[] colors, float[] positions) {
        return makeRadialGradient(x, y, r, colors, positions, M3GradientStyle.DEFAULT);
    }

    public static M3Shader makeRadialGradient(M3Point center, float r, int[] colors, float[] positions, M3GradientStyle style) {
        return makeRadialGradient(center._x, center._y, r, colors, positions, style);
    }

    public static M3Shader makeRadialGradient(float x, float y, float r, int[] colors, float[] positions, M3GradientStyle style) {
        assert positions == null || colors.length == positions.length : "colors.length " + colors.length + "!= positions.length " + positions.length;
        M3Stats.onNativeCall();
        return new M3Shader(_nMakeRadialGradient(x, y, r, colors, positions, style.getTileMode().ordinal(), style._getFlags(), style._getMatrixArray()));
    }

    public static M3Shader makeRadialGradient(M3Point center, float r, M3Color4f[] colors, M3ColorSpace cs, float[] positions, M3GradientStyle style) {
        return makeRadialGradient(center._x, center._y, r, colors, cs, positions, style);
    }

    public static M3Shader makeRadialGradient(float x, float y, float r, M3Color4f[] colors, M3ColorSpace cs, float[] positions, M3GradientStyle style) {
        try {
            assert positions == null || colors.length == positions.length : "colors.length " + colors.length + "!= positions.length " + positions.length;
            M3Stats.onNativeCall();
            return new M3Shader(_nMakeRadialGradientCS(x, y, r, M3Color4f.flattenArray(colors), M3Native.getPtr(cs), positions, style.getTileMode().ordinal(), style._getFlags(), style._getMatrixArray()));
        } finally {
            M3ReferenceUtil.reachabilityFence(cs);
        }
    }

    // Two-point Conical

    public static M3Shader makeTwoPointConicalGradient(M3Point p0, float r0, M3Point p1, float r1, int[] colors) {
        return makeTwoPointConicalGradient(p0._x, p0._y, r0, p1._x, p1._y, r1, colors);
    }

    public static M3Shader makeTwoPointConicalGradient(float x0, float y0, float r0, float x1, float y1, float r1, int[] colors) {
        return makeTwoPointConicalGradient(x0, y0, r0, x1, y1, r1, colors, null, M3GradientStyle.DEFAULT);
    }

    public static M3Shader makeTwoPointConicalGradient(M3Point p0, float r0, M3Point p1, float r1, int[] colors, float[] positions) {
        return makeTwoPointConicalGradient(p0._x, p0._y, r0, p1._x, p1._y, r1, colors, positions);
    }

    public static M3Shader makeTwoPointConicalGradient(float x0, float y0, float r0, float x1, float y1, float r1, int[] colors, float[] positions) {
        return makeTwoPointConicalGradient(x0, y0, r0, x1, y1, r1, colors, positions, M3GradientStyle.DEFAULT);
    }

    public static M3Shader makeTwoPointConicalGradient(M3Point p0, float r0, M3Point p1, float r1, int[] colors, float[] positions, M3GradientStyle style) {
        return makeTwoPointConicalGradient(p0._x, p0._y, r0, p1._x, p1._y, r1, colors, positions, style);
    }

    public static M3Shader makeTwoPointConicalGradient(float x0, float y0, float r0, float x1, float y1, float r1, int[] colors, float[] positions, M3GradientStyle style) {
        assert positions == null || colors.length == positions.length : "colors.length " + colors.length + "!= positions.length " + positions.length;
        M3Stats.onNativeCall();
        return new M3Shader(_nMakeTwoPointConicalGradient(x0, y0, r0, x1, y1, r1, colors, positions, style.getTileMode().ordinal(), style._getFlags(), style._getMatrixArray()));
    }

    public static M3Shader makeTwoPointConicalGradient(M3Point p0, float r0, M3Point p1, float r1, M3Color4f[] colors, M3ColorSpace cs, float[] positions, M3GradientStyle style) {
        return makeTwoPointConicalGradient(p0._x, p0._y, r0, p1._x, p1._y, r1, colors, cs, positions, style);
    }

    public static M3Shader makeTwoPointConicalGradient(float x0, float y0, float r0, float x1, float y1, float r1, M3Color4f[] colors, M3ColorSpace cs, float[] positions, M3GradientStyle style) {
        try {
            assert positions == null || colors.length == positions.length : "colors.length " + colors.length + "!= positions.length " + positions.length;
            M3Stats.onNativeCall();
            return new M3Shader(_nMakeTwoPointConicalGradientCS(x0, y0, r0, x1, y1, r1, M3Color4f.flattenArray(colors), M3Native.getPtr(cs), positions, style.getTileMode().ordinal(), style._getFlags(), style._getMatrixArray()));
        } finally {
            M3ReferenceUtil.reachabilityFence(cs);
        }
    }

    // Sweep

    public static M3Shader makeSweepGradient(M3Point center, int[] colors) {
        return makeSweepGradient(center._x, center._y, colors);
    }

    public static M3Shader makeSweepGradient(float x, float y, int[] colors) {
        return makeSweepGradient(x, y, 0, 360, colors, null, M3GradientStyle.DEFAULT);
    }

    public static M3Shader makeSweepGradient(M3Point center, int[] colors, float[] positions) {
        return makeSweepGradient(center._x, center._y, colors, positions);
    }

    public static M3Shader makeSweepGradient(float x, float y, int[] colors, float[] positions) {
        return makeSweepGradient(x, y, 0, 360, colors, positions, M3GradientStyle.DEFAULT);
    }

    public static M3Shader makeSweepGradient(M3Point center, int[] colors, float[] positions, M3GradientStyle style) {
        return makeSweepGradient(center._x, center._y, colors, positions, style);
    }

    public static M3Shader makeSweepGradient(float x, float y, int[] colors, float[] positions, M3GradientStyle style) {
        return makeSweepGradient(x, y, 0, 360, colors, positions, style);
    }

    public static M3Shader makeSweepGradient(M3Point center, float startAngle, float endAngle, int[] colors, float[] positions, M3GradientStyle style) {
        return makeSweepGradient(center._x, center._y, startAngle, endAngle, colors, positions, style);
    }

    public static M3Shader makeSweepGradient(float x, float y, float startAngle, float endAngle, int[] colors, float[] positions, M3GradientStyle style) {
        assert positions == null || colors.length == positions.length : "colors.length " + colors.length + "!= positions.length " + positions.length;
        M3Stats.onNativeCall();
        return new M3Shader(_nMakeSweepGradient(x, y, startAngle, endAngle, colors, positions, style.getTileMode().ordinal(), style._getFlags(), style._getMatrixArray()));
    }

    public static M3Shader makeSweepGradient(M3Point center, float startAngle, float endAngle, M3Color4f[] colors, M3ColorSpace cs, float[] positions, M3GradientStyle style) {
        return makeSweepGradient(center._x, center._y, startAngle, endAngle, colors, cs, positions, style);
    }

    public static M3Shader makeSweepGradient(float x, float y, float startAngle, float endAngle, M3Color4f[] colors, M3ColorSpace cs, float[] positions, M3GradientStyle style) {
        try {
            assert positions == null || colors.length == positions.length : "colors.length " + colors.length + "!= positions.length " + positions.length;
            M3Stats.onNativeCall();
            return new M3Shader(_nMakeSweepGradientCS(x, y, startAngle, endAngle, M3Color4f.flattenArray(colors), M3Native.getPtr(cs), positions, style.getTileMode().ordinal(), style._getFlags(), style._getMatrixArray()));
        } finally {
            M3ReferenceUtil.reachabilityFence(cs);
        }
    }

    // Perlin Noise

    /**
     * <p>Creates a shader that generates fractal Perlin noise.</p>
     *
     * @param baseFrequencyX Base frequency in the X direction; usually in the range [0..1]
     * @param baseFrequencyY Base frequency in the Y direction; usually in the range [0..1]
     * @param numOctaves     Number of octaves of noise to add together; should be fairly small (limit of 255)
     * @param seed           Random seed value
     * @param tileSize       If non-null, modifies frequencies to make tileable noise for the given tile size
     * @return               Shader that generates fractal Perlin noise
     */
    public static M3Shader makeFractalNoise(float baseFrequencyX, float baseFrequencyY, int numOctaves, float seed, M3IPoint tileSize) {
        M3Stats.onNativeCall();
        return new M3Shader(_nMakeFractalNoise(baseFrequencyX, baseFrequencyY, numOctaves, seed, tileSize == null ? 0 : tileSize.getX(), tileSize == null ? 0 : tileSize.getY()));
    }

    /**
     * <p>Creates a shader that generates fractal Perlin noise without tiling.</p>
     *
     * @param baseFrequencyX Base frequency in the X direction; usually in the range [0..1]
     * @param baseFrequencyY Base frequency in the Y direction; usually in the range [0..1]
     * @param numOctaves     Number of octaves of noise to add together; should be fairly small (limit of 255)
     * @param seed           Random seed value
     * @return               Shader that generates fractal Perlin noise
     */
    public static M3Shader makeFractalNoise(float baseFrequencyX, float baseFrequencyY, int numOctaves, float seed) {
        return makeFractalNoise(baseFrequencyX, baseFrequencyY, numOctaves, seed, null);
    }

    /**
     * <p>Creates a shader that generates turbulence using Perlin noise.</p>
     *
     * @param baseFrequencyX Base frequency in the X direction; usually in the range [0..1]
     * @param baseFrequencyY Base frequency in the Y direction; usually in the range [0..1]
     * @param numOctaves     Number of octaves of noise to add together; should be fairly small (limit of 255)
     * @param seed           Random seed value
     * @param tileSize       If non-null, modifies frequencies to make tileable noise for the given tile size
     * @return               Shader that generates turbulent Perlin noise
     */
    public static M3Shader makeTurbulence(float baseFrequencyX, float baseFrequencyY, int numOctaves, float seed, M3IPoint tileSize) {
        M3Stats.onNativeCall();
        return new M3Shader(_nMakeTurbulence(baseFrequencyX, baseFrequencyY, numOctaves, seed, tileSize == null ? 0 : tileSize.getX(), tileSize == null ? 0 : tileSize.getY()));
    }

    /**
     * <p>Creates a shader that generates turbulence using Perlin noise without tiling.</p>
     *
     * @param baseFrequencyX Base frequency in the X direction; usually in the range [0..1]
     * @param baseFrequencyY Base frequency in the Y direction; usually in the range [0..1]
     * @param numOctaves     Number of octaves of noise to add together; should be fairly small (limit of 255)
     * @param seed           Random seed value
     * @return               Shader that generates turbulent Perlin noise
     */
    public static M3Shader makeTurbulence(float baseFrequencyX, float baseFrequencyY, int numOctaves, float seed) {
        return makeTurbulence(baseFrequencyX, baseFrequencyY, numOctaves, seed, null);
    }

    //

    public static M3Shader makeEmpty() {
        M3Stats.onNativeCall();
        return new M3Shader(_nMakeEmpty());
    }

    public static M3Shader makeColor(int color) {
        M3Stats.onNativeCall();
        return new M3Shader(_nMakeColor(color));
    }

    public static M3Shader makeColor(M3Color4f color, M3ColorSpace space) {
        try {
            M3Stats.onNativeCall();
            return new M3Shader(_nMakeColorCS(color.getR(), color.getG(), color.getB(), color.getA(), M3Native.getPtr(space)));
        } finally {
            M3ReferenceUtil.reachabilityFence(space);
        }
    }

    public static M3Shader makeBlend(M3BlendMode mode, M3Shader dst, M3Shader src) {
        try {
            M3Stats.onNativeCall();
            return new M3Shader(_nMakeBlend(mode.ordinal(), M3Native.getPtr(dst), M3Native.getPtr(src)));
        } finally {
            M3ReferenceUtil.reachabilityFence(dst);
            M3ReferenceUtil.reachabilityFence(src);
        }
    }

    public M3Shader(long ptr) {
        super(ptr);
    }

    public static native boolean _nIsOpaque(long ptr);
    public static native long _nGetImage(long ptr);
    public static native long _nMakeWithLocalMatrix(long ptr, float[] matrix);
    public static native long _nMakeWithWorkingColorSpace(long ptr, long inputColorSpacePtr, long outputColorSpacePtr);
    public static native long _nMakeWithColorFilter(long ptr, long colorFilterPtr);
    public static native long _nMakeLinearGradient(float x0, float y0, float x1, float y1, int[] colors, float[] positions, int tileType, int flags, float[] matrix);
    public static native long _nMakeLinearGradientCS(float x0, float y0, float x1, float y1, float[] colors, long colorSpacePtr, float[] positions, int tileType, int flags, float[] matrix);
    public static native long _nMakeRadialGradient(float x, float y, float r, int[] colors, float[] positions, int tileType, int flags, float[] matrix);
    public static native long _nMakeRadialGradientCS(float x, float y, float r, float[] colors, long colorSpacePtr, float[] positions, int tileType, int flags, float[] matrix);
    public static native long _nMakeTwoPointConicalGradient(float x0, float y0, float r0, float x1, float y1, float r1, int[] colors, float[] positions, int tileType, int flags, float[] matrix);
    public static native long _nMakeTwoPointConicalGradientCS(float x0, float y0, float r0, float x1, float y1, float r1, float[] colors, long colorSpacePtr, float[] positions, int tileType, int flags, float[] matrix);
    public static native long _nMakeSweepGradient(float x, float y, float startAngle, float endAngle, int[] colors, float[] positions, int tileType, int flags, float[] matrix);
    public static native long _nMakeSweepGradientCS(float x, float y, float startAngle, float endAngle, float[] colors, long colorSpacePtr, float[] positions, int tileType, int flags, float[] matrix);
    public static native long _nMakeFractalNoise(float baseFrequencyX, float baseFrequencyY, int numOctaves, float seed, int tileSizeX, int tileSizeY);
    public static native long _nMakeTurbulence(float baseFrequencyX, float baseFrequencyY, int numOctaves, float seed, int tileSizeX, int tileSizeY);
    public static native long _nMakeEmpty();
    public static native long _nMakeColor(int color);
    public static native long _nMakeColorCS(float r, float g, float b, float a, long colorSpacePtr);
    public static native long _nMakeBlend(int blendMode, long dst, long src);
}