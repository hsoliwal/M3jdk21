package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.*;
import java.util.*;

public class M3ImageFilter extends M3RefCnt implements M3Flattenable {
    static { M3Library.staticLoad(); }

    /**
     * Create a filter that implements a custom blend mode. Each output pixel is the result of
     * combining the corresponding background and foreground pixels using the 4 coefficients:
     *
     *     k1 * foreground * background + k2 * foreground + k3 * background + k4
     *
     * @param k1              Coefficient for multiplied foreground and background
     * @param k2              Coefficient for foreground
     * @param k3              Coefficient for background
     * @param k4              Constant
     * @param enforcePMColor  If true, the RGB channels will be clamped to the calculated alpha
     * @param bg              The background content, using the source bitmap when this is null
     * @param fg              The foreground content, using the source bitmap when this is null
     * @param crop            Optional rectangle that crops the inputs and output
     * @return                filter that applies the arithmetic blend
     */
    public static M3ImageFilter makeArithmetic(float k1, float k2, float k3, float k4, boolean enforcePMColor, M3ImageFilter bg, M3ImageFilter fg, M3Rect crop) {
        try {
            M3Stats.onNativeCall();
            return new M3ImageFilter(_nMakeArithmetic(k1, k2, k3, k4, enforcePMColor, M3Native.getPtr(bg), M3Native.getPtr(fg), crop));
        } finally {
            M3ReferenceUtil.reachabilityFence(bg);
            M3ReferenceUtil.reachabilityFence(fg);
        }
    }

    public static M3ImageFilter makeArithmetic(float k1, float k2, float k3, float k4, boolean enforcePMColor, M3ImageFilter bg, M3ImageFilter fg, M3IRect crop) {
        return makeArithmetic(k1, k2, k3, k4, enforcePMColor, bg, fg, crop == null ? null : crop.toRect());
    }

    public static M3ImageFilter makeArithmetic(float k1, float k2, float k3, float k4, boolean enforcePMColor, M3ImageFilter bg, M3ImageFilter fg) {
        return makeArithmetic(k1, k2, k3, k4, enforcePMColor, bg, fg, (M3Rect) null);
    }

    /**
     * This filter takes a BlendMode and uses it to composite the two filters together.
     *
     * @param blendMode  The blend mode that defines the compositing operation
     * @param bg         The Dst pixels used in blending, if null the source bitmap is used
     * @param fg         The Src pixels used in blending, if null the source bitmap is used
     * @param crop       Optional rectangle to crop input and output
     * @return           filter that blends the two inputs
     */
    public static M3ImageFilter makeBlend(M3BlendMode blendMode, M3ImageFilter bg, M3ImageFilter fg, M3Rect crop) {
        try {
            M3Stats.onNativeCall();
            return new M3ImageFilter(_nMakeBlend(blendMode.ordinal(), M3Native.getPtr(bg), M3Native.getPtr(fg), crop));
        } finally {
            M3ReferenceUtil.reachabilityFence(bg);
            M3ReferenceUtil.reachabilityFence(fg);
        }
    }

    public static M3ImageFilter makeBlend(M3BlendMode blendMode, M3ImageFilter bg, M3ImageFilter fg, M3IRect crop) {
        return makeBlend(blendMode, bg, fg, crop == null ? null : crop.toRect());
    }

    public static M3ImageFilter makeBlend(M3BlendMode blendMode, M3ImageFilter bg, M3ImageFilter fg) {
        return makeBlend(blendMode, bg, fg, (M3Rect) null);
    }

    /**
     * This filter takes a Blender and uses it to composite the two filters together.
     *
     * @param blender  The blender that defines the compositing operation
     * @param bg       The Dst pixels used in blending, if null the source bitmap is used
     * @param fg       The Src pixels used in blending, if null the source bitmap is used
     * @param crop     Optional rectangle to crop input and output
     * @return         filter that blends the two inputs
     */
    public static M3ImageFilter makeBlend(M3Blender blender, M3ImageFilter bg, M3ImageFilter fg, M3Rect crop) {
        try {
            M3Stats.onNativeCall();
            return new M3ImageFilter(_nMakeBlendBlender(M3Native.getPtr(blender), M3Native.getPtr(bg), M3Native.getPtr(fg), crop));
        } finally {
            M3ReferenceUtil.reachabilityFence(blender);
            M3ReferenceUtil.reachabilityFence(bg);
            M3ReferenceUtil.reachabilityFence(fg);
        }
    }

    public static M3ImageFilter makeBlend(M3Blender blender, M3ImageFilter bg, M3ImageFilter fg, M3IRect crop) {
        return makeBlend(blender, bg, fg, crop == null ? null : crop.toRect());
    }

    public static M3ImageFilter makeBlend(M3Blender blender, M3ImageFilter bg, M3ImageFilter fg) {
        return makeBlend(blender, bg, fg, (M3Rect) null);
    }

    /**
     * Create a filter that blurs its input by the separate X and Y sigmas. The provided tile mode
     * is used when the blur kernel goes outside the input image.
     *
     * @param sigmaX  The Gaussian sigma value for blurring along the X axis
     * @param sigmaY  The Gaussian sigma value for blurring along the Y axis
     * @param mode    The tile mode applied at edges
     * @param input   The input filter that is blurred, uses source bitmap if this is null
     * @param crop    Optional rectangle that crops the input and output
     * @return        filter that blurs the input
     */
    public static M3ImageFilter makeBlur(float sigmaX, float sigmaY, M3FilterTileMode mode, M3ImageFilter input, M3Rect crop) {
        try {
            M3Stats.onNativeCall();
            return new M3ImageFilter(_nMakeBlur(sigmaX, sigmaY, mode.ordinal(), M3Native.getPtr(input), crop));
        } finally {
            M3ReferenceUtil.reachabilityFence(input);
        }
    }

    public static M3ImageFilter makeBlur(float sigmaX, float sigmaY, M3FilterTileMode mode, M3ImageFilter input, M3IRect crop) {
        return makeBlur(sigmaX, sigmaY, mode, input, crop == null ? null : crop.toRect());
    }

    public static M3ImageFilter makeBlur(float sigmaX, float sigmaY, M3FilterTileMode mode) {
        return makeBlur(sigmaX, sigmaY, mode, null, (M3Rect) null);
    }

    /**
     * Create a filter that applies the color filter to the input filter results.
     *
     * @param f      The color filter that transforms the input image
     * @param input  The input filter, or uses the source bitmap if this is null
     * @param crop   Optional rectangle that crops the input and output
     * @return       filter that applies the color filter
     */
    public static M3ImageFilter makeColorFilter(M3ColorFilter f, M3ImageFilter input, M3Rect crop) {
        try {
            M3Stats.onNativeCall();
            return new M3ImageFilter(_nMakeColorFilter(M3Native.getPtr(f), M3Native.getPtr(input), crop));
        } finally {
            M3ReferenceUtil.reachabilityFence(f);
            M3ReferenceUtil.reachabilityFence(input);
        }
    }

    public static M3ImageFilter makeColorFilter(M3ColorFilter f, M3ImageFilter input, M3IRect crop) {
        return makeColorFilter(f, input, crop == null ? null : crop.toRect());
    }

    public static M3ImageFilter makeColorFilter(M3ColorFilter f, M3ImageFilter input) {
        return makeColorFilter(f, input, (M3Rect) null);
    }

    /**
     * Create a filter that composes 'inner' with 'outer', such that the results of 'inner' are
     * treated as the source bitmap passed to 'outer', i.e. result = outer(inner(source)).
     *
     * @param outer  The outer filter that evaluates the results of inner
     * @param inner  The inner filter that produces the input to outer
     * @return       filter that composes the two filters
     */
    public static M3ImageFilter makeCompose(M3ImageFilter outer, M3ImageFilter inner) {
        try {
            M3Stats.onNativeCall();
            return new M3ImageFilter(_nMakeCompose(M3Native.getPtr(outer), M3Native.getPtr(inner)));
        } finally {
            M3ReferenceUtil.reachabilityFence(outer);
            M3ReferenceUtil.reachabilityFence(inner);
        }
    }

    /**
     * <p>Create a filter that applies a crop to the result of the input filter.
     * Pixels within the crop rectangle are unmodified from what the input
     * produced. Pixels outside of crop match the provided
     * {@link FilterTileMode} (defaulting to {@link FilterTileMode#DECAL}).</p>
     *
     * <p>NOTE: The optional Rect crop argument for many of the factory methods
     * is equivalent to creating the filter without a crop and then wrapping it
     * in makeCrop(rect, DECAL). Explicitly adding Crop filters lets you control
     * their tiling and use different geometry for the input and the output of
     * another filter.</p>
     *
     * @param rect      The cropping geometry
     * @param tileMode  The tile mode applied to pixels outside of the crop
     * @param input     The input filter that is cropped, uses source image if this is null
     * @return          filter that crops its input
     */
    public static M3ImageFilter makeCrop(M3Rect rect, M3FilterTileMode tileMode, M3ImageFilter input) {
        try {
            assert rect != null : "Can't makeCrop with rect == null";
            assert tileMode != null : "Can't makeCrop with tileMode == null";
            M3Stats.onNativeCall();
            return new M3ImageFilter(_nMakeCrop(rect._left, rect._top, rect._right, rect._bottom, tileMode.ordinal(), M3Native.getPtr(input)));
        } finally {
            M3ReferenceUtil.reachabilityFence(input);
        }
    }

    /**
     * Create a filter that applies a crop to the result of the input filter. Pixels within the
     * crop rectangle are unmodified from what the input produced. Pixels outside of crop are transparent.
     *
     * @param rect   The cropping geometry
     * @param input  The input filter that is cropped, uses source image if this is null
     * @return       filter that crops its input
     */
    public static M3ImageFilter makeCrop(M3Rect rect, M3ImageFilter input) {
        return makeCrop(rect, M3FilterTileMode.DECAL, input);
    }

    /**
     * Create a filter that moves each pixel in its color input based on an (x,y) vector encoded
     * in its displacement input filter. Two color components of the displacement image are
     * mapped into a vector as scale * (color[xChannel], color[yChannel]), where the channel
     * selectors are one of R, G, B, or A.
     *
     * @param x             RGBA channel that encodes the x displacement per pixel
     * @param y             RGBA channel that encodes the y displacement per pixel
     * @param scale         Scale applied to displacement extracted from image
     * @param displacement  The filter defining the displacement image, or null to use source
     * @param color         The filter providing the color pixels to be displaced. If null, it will use the source
     * @param crop          Optional rectangle that crops the color input and output
     * @return              filter that displaces the color input
     */
    public static M3ImageFilter makeDisplacementMap(M3ColorChannel x, M3ColorChannel y, float scale, M3ImageFilter displacement, M3ImageFilter color, M3Rect crop) {
        try {
            M3Stats.onNativeCall();
            return new M3ImageFilter(_nMakeDisplacementMap(x.ordinal(), y.ordinal(), scale, M3Native.getPtr(displacement), M3Native.getPtr(color), crop));
        } finally {
            M3ReferenceUtil.reachabilityFence(displacement);
            M3ReferenceUtil.reachabilityFence(color);
        }
    }

    public static M3ImageFilter makeDisplacementMap(M3ColorChannel x, M3ColorChannel y, float scale, M3ImageFilter displacement, M3ImageFilter color, M3IRect crop) {
        return makeDisplacementMap(x, y, scale, displacement, color, crop == null ? null : crop.toRect());
    }

    public static M3ImageFilter makeDisplacementMap(M3ColorChannel x, M3ColorChannel y, float scale, M3ImageFilter displacement, M3ImageFilter color) {
        return makeDisplacementMap(x, y, scale, displacement, color, (M3Rect) null);
    }

    /**
     * Create a filter that draws a drop shadow under the input content. This filter produces an
     * image that includes the inputs' content.
     *
     * @param dx          X offset of the shadow
     * @param dy          Y offset of the shadow
     * @param sigmaX      Blur radius for the shadow, along the X axis
     * @param sigmaY      Blur radius for the shadow, along the Y axis
     * @param color       Color of the drop shadow
     * @param colorSpace  The color space of the drop shadow color
     * @param input       The input filter, or will use the source bitmap if this is null
     * @param crop        Optional rectangle that crops the input and output
     * @return            filter that draws a drop shadow
     */
    public static M3ImageFilter makeDropShadow(float dx, float dy, float sigmaX, float sigmaY, M3Color4f color, M3ColorSpace colorSpace, M3ImageFilter input, M3Rect crop) {
        try {
            M3Stats.onNativeCall();
            return new M3ImageFilter(_nMakeDropShadowCS(dx, dy, sigmaX, sigmaY, color.getR(), color.getG(), color.getB(), color.getA(), M3Native.getPtr(colorSpace), M3Native.getPtr(input), crop));
        } finally {
            M3ReferenceUtil.reachabilityFence(colorSpace);
            M3ReferenceUtil.reachabilityFence(input);
        }
    }

    public static M3ImageFilter makeDropShadow(float dx, float dy, float sigmaX, float sigmaY, M3Color4f color, M3ColorSpace colorSpace, M3ImageFilter input, M3IRect crop) {
        return makeDropShadow(dx, dy, sigmaX, sigmaY, color, colorSpace, input, crop == null ? null : crop.toRect());
    }

    public static M3ImageFilter makeDropShadow(float dx, float dy, float sigmaX, float sigmaY, M3Color4f color, M3ColorSpace colorSpace, M3ImageFilter input) {
        return makeDropShadow(dx, dy, sigmaX, sigmaY, color, colorSpace, input, (M3Rect) null);
    }

    /**
     * Create a filter that draws a drop shadow under the input content. This filter produces an
     * image that includes the inputs' content.
     *
     * @param dx      X offset of the shadow
     * @param dy      Y offset of the shadow
     * @param sigmaX  Blur radius for the shadow, along the X axis
     * @param sigmaY  Blur radius for the shadow, along the Y axis
     * @param color   Color of the drop shadow
     * @param input   The input filter, or will use the source bitmap if this is null
     * @param crop    Optional rectangle that crops the input and output
     * @return        filter that draws a drop shadow
     */
    public static M3ImageFilter makeDropShadow(float dx, float dy, float sigmaX, float sigmaY, int color, M3ImageFilter input, M3Rect crop) {
        try {
            M3Stats.onNativeCall();
            return new M3ImageFilter(_nMakeDropShadow(dx, dy, sigmaX, sigmaY, color, M3Native.getPtr(input), crop));
        } finally {
            M3ReferenceUtil.reachabilityFence(input);
        }
    }

    public static M3ImageFilter makeDropShadow(float dx, float dy, float sigmaX, float sigmaY, int color, M3ImageFilter input, M3IRect crop) {
        return makeDropShadow(dx, dy, sigmaX, sigmaY, color, input, crop == null ? null : crop.toRect());
    }

    public static M3ImageFilter makeDropShadow(float dx, float dy, float sigmaX, float sigmaY, int color, M3ImageFilter input) {
        return makeDropShadow(dx, dy, sigmaX, sigmaY, color, input, (M3Rect) null);
    }

    public static M3ImageFilter makeDropShadow(float dx, float dy, float sigmaX, float sigmaY, int color) {
        return makeDropShadow(dx, dy, sigmaX, sigmaY, color, null, (M3Rect) null);
    }

    /**
     * Create a filter that renders a drop shadow, in exactly the same manner as makeDropShadow,
     * except that the resulting image does not include the input content. This allows the shadow
     * and input to be composed by a filter DAG in a more flexible manner.
     *
     * @param dx          The X offset of the shadow
     * @param dy          The Y offset of the shadow
     * @param sigmaX      The blur radius for the shadow, along the X axis
     * @param sigmaY      The blur radius for the shadow, along the Y axis
     * @param color       The color of the drop shadow
     * @param colorSpace  The color space of the drop shadow color
     * @param input       The input filter, or will use the source bitmap if this is null
     * @param crop        Optional rectangle that crops the input and output
     * @return            filter that renders only the drop shadow
     */
    public static M3ImageFilter makeDropShadowOnly(float dx, float dy, float sigmaX, float sigmaY, M3Color4f color, M3ColorSpace colorSpace, M3ImageFilter input, M3Rect crop) {
        try {
            M3Stats.onNativeCall();
            return new M3ImageFilter(_nMakeDropShadowOnlyCS(dx, dy, sigmaX, sigmaY, color.getR(), color.getG(), color.getB(), color.getA(), M3Native.getPtr(colorSpace), M3Native.getPtr(input), crop));
        } finally {
            M3ReferenceUtil.reachabilityFence(colorSpace);
            M3ReferenceUtil.reachabilityFence(input);
        }
    }

    public static M3ImageFilter makeDropShadowOnly(float dx, float dy, float sigmaX, float sigmaY, M3Color4f color, M3ColorSpace colorSpace, M3ImageFilter input, M3IRect crop) {
        return makeDropShadowOnly(dx, dy, sigmaX, sigmaY, color, colorSpace, input, crop == null ? null : crop.toRect());
    }

    public static M3ImageFilter makeDropShadowOnly(float dx, float dy, float sigmaX, float sigmaY, M3Color4f color, M3ColorSpace colorSpace, M3ImageFilter input) {
        return makeDropShadowOnly(dx, dy, sigmaX, sigmaY, color, colorSpace, input, (M3Rect) null);
    }

    /**
     * Create a filter that renders a drop shadow, in exactly the same manner as makeDropShadow,
     * except that the resulting image does not include the input content. This allows the shadow
     * and input to be composed by a filter DAG in a more flexible manner.
     *
     * @param dx      The X offset of the shadow
     * @param dy      The Y offset of the shadow
     * @param sigmaX  The blur radius for the shadow, along the X axis
     * @param sigmaY  The blur radius for the shadow, along the Y axis
     * @param color   The color of the drop shadow
     * @param input   The input filter, or will use the source bitmap if this is null
     * @param crop    Optional rectangle that crops the input and output
     * @return        filter that renders only the drop shadow
     */
    public static M3ImageFilter makeDropShadowOnly(float dx, float dy, float sigmaX, float sigmaY, int color, M3ImageFilter input, M3Rect crop) {
        try {
            M3Stats.onNativeCall();
            return new M3ImageFilter(_nMakeDropShadowOnly(dx, dy, sigmaX, sigmaY, color, M3Native.getPtr(input), crop));
        } finally {
            M3ReferenceUtil.reachabilityFence(input);
        }
    }

    public static M3ImageFilter makeDropShadowOnly(float dx, float dy, float sigmaX, float sigmaY, int color, M3ImageFilter input, M3IRect crop) {
        return makeDropShadowOnly(dx, dy, sigmaX, sigmaY, color, input, crop == null ? null : crop.toRect());
    }

    public static M3ImageFilter makeDropShadowOnly(float dx, float dy, float sigmaX, float sigmaY, int color) {
        return makeDropShadowOnly(dx, dy, sigmaX, sigmaY, color, null, (M3Rect) null);
    }

    /**
     * Create a filter that always produces transparent black.
     *
     * @return filter that produces transparent black
     */
    public static M3ImageFilter makeEmpty() {
        M3Stats.onNativeCall();
        return new M3ImageFilter(_nMakeEmpty());
    }

    /**
     * Create a filter that draws the 'src' portion of image into 'dst' using the given
     * filter quality. The returned image filter evaluates to transparent black if 'image' is null.
     *
     * @param image   The image that is output by the filter, subset by 'src'
     * @param src     The source pixels sampled into 'dst'
     * @param dst     The local rectangle to draw the image into
     * @param mode    The sampling to use when drawing the image
     * @return        filter that draws the image
     */
    public static M3ImageFilter makeImage(M3Image image, M3Rect src, M3Rect dst, M3SamplingMode mode) {
        try {
            M3Stats.onNativeCall();
            return new M3ImageFilter(_nMakeImage(M3Native.getPtr(image), src._left, src._top, src._right, src._bottom, dst._left, dst._top, dst._right, dst._bottom, mode._pack()));
        } finally {
            M3ReferenceUtil.reachabilityFence(image);
        }
    }

    /**
     * Create a filter that draws the image using the given sampling.
     *
     * @param image  The image that is output by the filter
     * @return       filter that draws the image
     */
    public static M3ImageFilter makeImage(M3Image image) {
        M3Rect r = M3Rect.makeWH(image.getWidth(), image.getHeight());
        return makeImage(image, r, r, M3SamplingMode.DEFAULT);
    }

    /**
     * Create a filter that fills 'lensBounds' with a magnification of the input.
     *
     * @param r           The outer bounds of the magnifier effect
     * @param zoomAmount  The amount of magnification applied to the input image
     * @param inset       The size or width of the fish-eye distortion around the magnified content
     * @param mode        The SamplingOptions applied to the input image when magnified
     * @param input       The input filter that is magnified; if null the source bitmap is used
     * @param crop        Optional rectangle that crops the input and output
     * @return            filter that magnifies the input
     */
    public static M3ImageFilter makeMagnifier(M3Rect r, float zoomAmount, float inset, M3SamplingMode mode, M3ImageFilter input, M3Rect crop) {
        try {
            M3Stats.onNativeCall();
            return new M3ImageFilter(_nMakeMagnifier(r._left, r._top, r._right, r._bottom, zoomAmount, inset, mode._pack(), M3Native.getPtr(input), crop));
        } finally {
            M3ReferenceUtil.reachabilityFence(input);
        }
    }

    public static M3ImageFilter makeMagnifier(M3Rect r, float zoomAmount, float inset, M3SamplingMode mode, M3ImageFilter input, M3IRect crop) {
        return makeMagnifier(r, zoomAmount, inset, mode, input, crop == null ? null : crop.toRect());
    }

    public static M3ImageFilter makeMagnifier(M3Rect r, float zoomAmount, float inset, M3SamplingMode mode, M3ImageFilter input) {
        return makeMagnifier(r, zoomAmount, inset, mode, input, (M3Rect) null);
    }

    /**
     * Create a filter that applies an NxM image processing kernel to the input image. This can be
     * used to produce effects such as sharpening, blurring, edge detection, etc.
     *
     * @param kernelW       The kernel size in pixels, in each dimension (N by M)
     * @param kernelH       The kernel height in pixels
     * @param kernel        The image processing kernel. Must contain N * M elements, in row order
     * @param gain          A scale factor applied to each pixel after convolution
     * @param bias          A bias factor added to each pixel after convolution
     * @param offsetX       An offset applied to each pixel coordinate before convolution
     * @param offsetY       An offset applied to each pixel coordinate before convolution
     * @param tileMode      How accesses outside the image are treated
     * @param convolveAlpha If true, all channels are convolved. If false, only RGB channels are convolved
     * @param input         The input image filter, if null the source bitmap is used instead
     * @param crop          Optional rectangle to which the output processing will be limited
     * @return              filter that applies matrix convolution
     */
    public static M3ImageFilter makeMatrixConvolution(int kernelW, int kernelH, float[] kernel, float gain, float bias, int offsetX, int offsetY, M3FilterTileMode tileMode, boolean convolveAlpha, M3ImageFilter input, M3Rect crop) {
        try {
            M3Stats.onNativeCall();
            return new M3ImageFilter(_nMakeMatrixConvolution(kernelW, kernelH, kernel, gain, bias, offsetX, offsetY, tileMode.ordinal(), convolveAlpha, M3Native.getPtr(input), crop));
        } finally {
            M3ReferenceUtil.reachabilityFence(input);
        }
    }

    public static M3ImageFilter makeMatrixConvolution(int kernelW, int kernelH, float[] kernel, float gain, float bias, int offsetX, int offsetY, M3FilterTileMode tileMode, boolean convolveAlpha, M3ImageFilter input, M3IRect crop) {
        return makeMatrixConvolution(kernelW, kernelH, kernel, gain, bias, offsetX, offsetY, tileMode, convolveAlpha, input, crop == null ? null : crop.toRect());
    }

    public static M3ImageFilter makeMatrixConvolution(int kernelW, int kernelH, float[] kernel, float gain, float bias, int offsetX, int offsetY, M3FilterTileMode tileMode, boolean convolveAlpha, M3ImageFilter input) {
        return makeMatrixConvolution(kernelW, kernelH, kernel, gain, bias, offsetX, offsetY, tileMode, convolveAlpha, input, (M3Rect) null);
    }

    /**
     * Create a filter that transforms the input image by 'matrix'. This matrix transforms the
     * local space, which means it effectively happens prior to any transformation coming from the
     * Canvas initiating the filtering.
     *
     * @param matrix  The matrix to apply to the original content
     * @param mode    How the image will be sampled when it is transformed
     * @param input   The image filter to transform, or null to use the source image
     * @return        filter that transforms the input
     */
    public static M3ImageFilter makeMatrixTransform(M3Matrix33 matrix, M3SamplingMode mode, M3ImageFilter input) {
        try {
            M3Stats.onNativeCall();
            return new M3ImageFilter(_nMakeMatrixTransform(matrix.getMat(), mode._pack(), M3Native.getPtr(input)));
        } finally {
            M3ReferenceUtil.reachabilityFence(input);
        }
    }

    /**
     * Create a filter that merges the filter array together by drawing their results in order
     * with src-over blending.
     *
     * @param filters  The input filter array to merge. Any null filter pointers will use the source bitmap instead
     * @param crop     Optional rectangle that crops all input filters and the output
     * @return         filter that merges the inputs
     */
    public static M3ImageFilter makeMerge(M3ImageFilter[] filters, M3Rect crop) {
        try {
            M3Stats.onNativeCall();
            long[] filterPtrs = new long[filters.length];
            Arrays.setAll(filterPtrs, i -> M3Native.getPtr(filters[i]));
            return new M3ImageFilter(_nMakeMerge(filterPtrs, crop));
        } finally {
            M3ReferenceUtil.reachabilityFence(filters);
        }
    }

    public static M3ImageFilter makeMerge(M3ImageFilter[] filters, M3IRect crop) {
        return makeMerge(filters, crop == null ? null : crop.toRect());
    }

    public static M3ImageFilter makeMerge(M3ImageFilter[] filters) {
        return makeMerge(filters, (M3Rect) null);
    }

    /**
     * Create a filter that offsets the input filter by the given vector.
     *
     * @param dx     The x offset in local space that the image is shifted
     * @param dy     The y offset in local space that the image is shifted
     * @param input  The input that will be moved, if null the source bitmap is used instead
     * @param crop   Optional rectangle to crop the input and output
     * @return       filter that offsets the input
     */
    public static M3ImageFilter makeOffset(float dx, float dy, M3ImageFilter input, M3Rect crop) {
        try {
            M3Stats.onNativeCall();
            return new M3ImageFilter(_nMakeOffset(dx, dy, M3Native.getPtr(input), crop));
        } finally {
            M3ReferenceUtil.reachabilityFence(input);
        }
    }

    public static M3ImageFilter makeOffset(float dx, float dy, M3ImageFilter input, M3IRect crop) {
        return makeOffset(dx, dy, input, crop == null ? null : crop.toRect());
    }

    public static M3ImageFilter makeOffset(float dx, float dy, M3ImageFilter input) {
        return makeOffset(dx, dy, input, (M3Rect) null);
    }

    /**
     * <p>Create a filter that produces the Picture as its output, clipped to both targetRect and
     * the picture's internal cull rect.</p>
     *
     * <p>If picture is null, the returned image filter produces transparent black.</p>
     *
     * @param picture    The picture that is drawn for the filter output
     * @param targetRect The drawing region for the picture
     * @return           filter that draws the picture
     */
    public static M3ImageFilter makePicture(M3Picture picture, M3Rect targetRect) {
        try {
            M3Stats.onNativeCall();
            return new M3ImageFilter(_nMakePicture(M3Native.getPtr(picture), targetRect._left, targetRect._top, targetRect._right, targetRect._bottom));
        } finally {
            M3ReferenceUtil.reachabilityFence(picture);
        }
    }

    /**
     * <p>Create a filter that produces the Picture as its output, using the picture's cull rect
     * as the drawing region.</p>
     *
     * <p>If picture is null, the returned image filter produces transparent black.</p>
     *
     * @param picture The picture that is drawn for the filter output
     * @return        filter that draws the picture
     */
    public static M3ImageFilter makePicture(M3Picture picture) {
        try {
            M3Stats.onNativeCall();
            return new M3ImageFilter(_nMakePicture(M3Native.getPtr(picture)));
        } finally {
            M3ReferenceUtil.reachabilityFence(picture);
        }
    }

    /**
     * Create a filter that produces a runtime shader effect. The runtime shader is evaluated per-pixel
     * and the result fills the output. The childShaderName is used to bind the input filter to a shader
     * child in the builder, or uses the source bitmap when input is null.
     *
     * @param builder          The RuntimeEffectBuilder that defines the shader
     * @param childShaderName  The name of the child shader in the builder to bind the input to
     * @param input            The input filter, or uses the source bitmap when null
     * @return                 filter that applies the runtime shader
     */
    public static M3ImageFilter makeRuntimeShader(M3RuntimeEffectBuilder builder, String childShaderName, M3ImageFilter input) {
        return makeRuntimeShader(builder, 0.0f, childShaderName, input);
    }

    /**
     * Create a filter that produces a runtime shader effect with a sample radius. The runtime shader
     * is evaluated per-pixel and the result fills the output. The sampleRadius defines how far
     * from the output pixel coordinate the child shader may be sampled.
     *
     * @param builder          The RuntimeEffectBuilder that defines the shader
     * @param sampleRadius     Maximum distance (in pixels) that the child shader may be sampled from the output coordinate
     * @param childShaderName  The name of the child shader in the builder to bind the input to
     * @param input            The input filter, or uses the source bitmap when null
     * @return                 filter that applies the runtime shader
     */
    public static M3ImageFilter makeRuntimeShader(M3RuntimeEffectBuilder builder, float sampleRadius, String childShaderName, M3ImageFilter input) {
        if (childShaderName == null || "".equals(childShaderName)) {
            M3RuntimeEffectChildInfo[] children = builder._effect.getChildren();
            assert children.length == 1 : "Expected 1 child, got " + children.length;
            childShaderName = children[0]._name;
        }
        return makeRuntimeShader(builder, sampleRadius, new String[] { childShaderName }, new M3ImageFilter[] { input });
    }

    /**
     * Create a filter that produces a runtime shader effect with multiple child shaders. Each child
     * shader name is bound to the corresponding input filter, or uses the source bitmap when the input is null.
     *
     * @param builder           The RuntimeEffectBuilder that defines the shader
     * @param childShaderNames  The names of the child shaders in the builder to bind the inputs to
     * @param inputs            The input filters corresponding to each child shader name
     * @return                  filter that applies the runtime shader
     */
    public static M3ImageFilter makeRuntimeShader(M3RuntimeEffectBuilder builder, String[] childShaderNames, M3ImageFilter[] inputs) {
        return makeRuntimeShader(builder, 0.0f, childShaderNames, inputs);
    }

    /**
     * Create a filter that produces a runtime shader effect with multiple child shaders and a maximum
     * sample radius. The maxSampleRadius defines how far from the output pixel coordinate any child
     * shader may be sampled.
     *
     * @param builder           The RuntimeEffectBuilder that defines the shader
     * @param maxSampleRadius   Maximum distance (in pixels) that any child shader may be sampled from the output coordinate
     * @param childShaderNames  The names of the child shaders in the builder to bind the inputs to
     * @param inputs            The input filters corresponding to each child shader name
     * @return                  filter that applies the runtime shader
     */
    public static M3ImageFilter makeRuntimeShader(M3RuntimeEffectBuilder builder, float maxSampleRadius, String[] childShaderNames, M3ImageFilter[] inputs) {
        try {
            M3Stats.onNativeCall();

            assert childShaderNames != null : "Can't makeRuntimeShader with childShaderNames == null";
            assert Arrays.stream(childShaderNames).allMatch(Objects::nonNull) : "Can't makeRuntimeShader with null childShaderNames elements";

            long[] inputPtrs = new long[inputs.length];
            for (int i = 0; i < inputs.length; i++) {
                inputPtrs[i] = M3Native.getPtr(inputs[i]);
            }
            return new M3ImageFilter(_nMakeRuntimeShader(M3Native.getPtr(builder), maxSampleRadius, childShaderNames, inputPtrs));
        } finally {
            M3ReferenceUtil.reachabilityFence(builder);
            M3ReferenceUtil.reachabilityFence(inputs);
            for (M3ImageFilter input: inputs) {
                M3ReferenceUtil.reachabilityFence(input);
            }
        }
    }

    /**
     * <p>Create a filter that fills the output with the per-pixel evaluation of
     * the shader.</p>
     *
     * <p>The shader is defined in the image filter's local coordinate system,
     * so will automatically be affected by {@link Canvas}' transform.</p>
     *
     * <p>Like {@link makeImage} and {@link makePicture}, this is a leaf filter
     * that can be used to introduce inputs to a complex filter graph, but
     * should generally be combined with a filter that as at least one null
     * input to use the implicit source image.</p>
     *
     * <p>Returns an image filter that evaluates to transparent black if
     * 'shader' is null.</p>
     *
     * @param shader The shader to evaluate on every output pixel
     * @param dither Whether to apply dithering
     * @param crop   Optional crop rect to apply to the result
     * @return       filter that fills output with shader evaluation
     */
    public static M3ImageFilter makeShader(M3Shader shader, boolean dither, M3Rect crop) {
        try {
            M3Stats.onNativeCall();
            return new M3ImageFilter(_nMakeShader(M3Native.getPtr(shader), dither, crop));
        } finally {
            M3ReferenceUtil.reachabilityFence(shader);
        }
    }

    public static M3ImageFilter makeShader(M3Shader shader, boolean dither, M3IRect crop) {
        return makeShader(shader, dither, crop == null ? (M3Rect) null : crop.toRect());
    }

    public static M3ImageFilter makeShader(M3Shader shader, M3Rect crop) {
        return makeShader(shader, false, crop);
    }

    public static M3ImageFilter makeShader(M3Shader shader, M3IRect crop) {
        return makeShader(shader, false, crop == null ? (M3Rect) null : crop.toRect());
    }

    public static M3ImageFilter makeShader(M3Shader shader) {
        return makeShader(shader, false, (M3Rect) null);
    }

    /**
     * Create a tile image filter.
     *
     * @param src    Defines the pixels to tile
     * @param dst    Defines the pixel region that the tiles will be drawn to
     * @param input  The input that will be tiled, if null the source bitmap is used instead
     * @return       filter that tiles the input
     */
    public static M3ImageFilter makeTile(M3Rect src, M3Rect dst, M3ImageFilter input) {
        try {
            M3Stats.onNativeCall();
            return new M3ImageFilter(_nMakeTile(src._left, src._top, src._right, src._bottom, dst._left, dst._top, dst._right, dst._bottom, M3Native.getPtr(input)));
        } finally {
            M3ReferenceUtil.reachabilityFence(input);
        }
    }

    /**
     * Create a filter that dilates each input pixel's channel values to the max value within the
     * given radii along the x and y axes.
     *
     * @param rx     The distance to dilate along the x axis to either side of each pixel
     * @param ry     The distance to dilate along the y axis to either side of each pixel
     * @param input  The image filter that is dilated, using source bitmap if this is null
     * @param crop   Optional rectangle that crops the input and output
     * @return       filter that dilates the input
     */
    public static M3ImageFilter makeDilate(float rx, float ry, M3ImageFilter input, M3Rect crop) {
        try {
            M3Stats.onNativeCall();
            return new M3ImageFilter(_nMakeDilate(rx, ry, M3Native.getPtr(input), crop));
        } finally {
            M3ReferenceUtil.reachabilityFence(input);
        }
    }

    public static M3ImageFilter makeDilate(float rx, float ry, M3ImageFilter input, M3IRect crop) {
        return makeDilate(rx, ry, input, crop == null ? null : crop.toRect());
    }

    public static M3ImageFilter makeDilate(float rx, float ry, M3ImageFilter input) {
        return makeDilate(rx, ry, input, (M3Rect) null);
    }

    /**
     * Create a filter that erodes each input pixel's channel values to the minimum channel value
     * within the given radii along the x and y axes.
     *
     * @param rx     The distance to erode along the x axis to either side of each pixel
     * @param ry     The distance to erode along the y axis to either side of each pixel
     * @param input  The image filter that is eroded, using source bitmap if this is null
     * @param crop   Optional rectangle that crops the input and output
     * @return       filter that erodes the input
     */
    public static M3ImageFilter makeErode(float rx, float ry, M3ImageFilter input, M3Rect crop) {
        try {
            M3Stats.onNativeCall();
            return new M3ImageFilter(_nMakeErode(rx, ry, M3Native.getPtr(input), crop));
        } finally {
            M3ReferenceUtil.reachabilityFence(input);
        }
    }

    public static M3ImageFilter makeErode(float rx, float ry, M3ImageFilter input, M3IRect crop) {
        return makeErode(rx, ry, input, crop == null ? null : crop.toRect());
    }

    public static M3ImageFilter makeErode(float rx, float ry, M3ImageFilter input) {
        return makeErode(rx, ry, input, (M3Rect) null);
    }

    /**
     * Create a filter that calculates the diffuse illumination from a distant light source,
     * interpreting the alpha channel of the input as the height profile of the surface (to
     * approximate normal vectors).
     *
     * @param x             The direction to the distance light (X component)
     * @param y             The direction to the distance light (Y component)
     * @param z             The direction to the distance light (Z component)
     * @param lightColor    The color of the diffuse light source
     * @param surfaceScale  Scale factor to transform from alpha values to physical height
     * @param kd            Diffuse reflectance coefficient
     * @param input         The input filter that defines surface normals (as alpha), or uses the source bitmap when null
     * @param crop          Optional rectangle that crops the input and output
     * @return              filter that applies distant diffuse lighting
     */
    public static M3ImageFilter makeDistantLitDiffuse(float x, float y, float z, int lightColor, float surfaceScale, float kd, M3ImageFilter input, M3Rect crop) {
        try {
            M3Stats.onNativeCall();
            return new M3ImageFilter(_nMakeDistantLitDiffuse(x, y, z, lightColor, surfaceScale, kd, M3Native.getPtr(input), crop));
        } finally {
            M3ReferenceUtil.reachabilityFence(input);
        }
    }

    public static M3ImageFilter makeDistantLitDiffuse(float x, float y, float z, int lightColor, float surfaceScale, float kd, M3ImageFilter input, M3IRect crop) {
        return makeDistantLitDiffuse(x, y, z, lightColor, surfaceScale, kd, input, crop == null ? null : crop.toRect());
    }

    public static M3ImageFilter makeDistantLitDiffuse(float x, float y, float z, int lightColor, float surfaceScale, float kd, M3ImageFilter input) {
        return makeDistantLitDiffuse(x, y, z, lightColor, surfaceScale, kd, input, (M3Rect) null);
    }

    /**
     * Create a filter that calculates the diffuse illumination from a point light source, using
     * alpha channel of the input as the height profile of the surface (to approximate normal vectors).
     *
     * @param x             The location of the point light (X component)
     * @param y             The location of the point light (Y component)
     * @param z             The location of the point light (Z component)
     * @param lightColor    The color of the diffuse light source
     * @param surfaceScale  Scale factor to transform from alpha values to physical height
     * @param kd            Diffuse reflectance coefficient
     * @param input         The input filter that defines surface normals (as alpha), or uses the source bitmap when null
     * @param crop          Optional rectangle that crops the input and output
     * @return              filter that applies point diffuse lighting
     */
    public static M3ImageFilter makePointLitDiffuse(float x, float y, float z, int lightColor, float surfaceScale, float kd, M3ImageFilter input, M3Rect crop) {
        try {
            M3Stats.onNativeCall();
            return new M3ImageFilter(_nMakePointLitDiffuse(x, y, z, lightColor, surfaceScale, kd, M3Native.getPtr(input), crop));
        } finally {
            M3ReferenceUtil.reachabilityFence(input);
        }
    }

    public static M3ImageFilter makePointLitDiffuse(float x, float y, float z, int lightColor, float surfaceScale, float kd, M3ImageFilter input, M3IRect crop) {
        return makePointLitDiffuse(x, y, z, lightColor, surfaceScale, kd, input, crop == null ? null : crop.toRect());
    }

    public static M3ImageFilter makePointLitDiffuse(float x, float y, float z, int lightColor, float surfaceScale, float kd, M3ImageFilter input) {
        return makePointLitDiffuse(x, y, z, lightColor, surfaceScale, kd, input, (M3Rect) null);
    }

    /**
     * Create a filter that calculates the diffuse illumination from a spot light source, using
     * alpha channel of the input as the height profile of the surface (to approximate normal vectors).
     * The spot light is restricted to be within 'cutoffAngle' of the vector between the location and target.
     *
     * @param x0               The location of the spot light (X component)
     * @param y0               The location of the spot light (Y component)
     * @param z0               The location of the spot light (Z component)
     * @param x1               The location that the spot light is point towards (X component)
     * @param y1               The location that the spot light is point towards (Y component)
     * @param z1               The location that the spot light is point towards (Z component)
     * @param falloffExponent  Exponential falloff parameter for illumination outside of cutoffAngle
     * @param cutoffAngle      Maximum angle from lighting direction that receives full light
     * @param lightColor       The color of the diffuse light source
     * @param surfaceScale     Scale factor to transform from alpha values to physical height
     * @param kd               Diffuse reflectance coefficient
     * @param input            The input filter that defines surface normals (as alpha), or uses the source bitmap when null
     * @param crop             Optional rectangle that crops the input and output
     * @return                 filter that applies spot diffuse lighting
     */
    public static M3ImageFilter makeSpotLitDiffuse(float x0, float y0, float z0, float x1, float y1, float z1, float falloffExponent, float cutoffAngle, int lightColor, float surfaceScale, float kd, M3ImageFilter input, M3Rect crop) {
        try {
            M3Stats.onNativeCall();
            return new M3ImageFilter(_nMakeSpotLitDiffuse(x0, y0, z0, x1, y1, z1, falloffExponent, cutoffAngle, lightColor, surfaceScale, kd, M3Native.getPtr(input), crop));
        } finally {
            M3ReferenceUtil.reachabilityFence(input);
        }
    }

    public static M3ImageFilter makeSpotLitDiffuse(float x0, float y0, float z0, float x1, float y1, float z1, float falloffExponent, float cutoffAngle, int lightColor, float surfaceScale, float kd, M3ImageFilter input, M3IRect crop) {
        return makeSpotLitDiffuse(x0, y0, z0, x1, y1, z1, falloffExponent, cutoffAngle, lightColor, surfaceScale, kd, input, crop == null ? null : crop.toRect());
    }

    public static M3ImageFilter makeSpotLitDiffuse(float x0, float y0, float z0, float x1, float y1, float z1, float falloffExponent, float cutoffAngle, int lightColor, float surfaceScale, float kd, M3ImageFilter input) {
        return makeSpotLitDiffuse(x0, y0, z0, x1, y1, z1, falloffExponent, cutoffAngle, lightColor, surfaceScale, kd, input, (M3Rect) null);
    }

    /**
     * Create a filter that calculates the specular illumination from a distant light source,
     * interpreting the alpha channel of the input as the height profile of the surface (to
     * approximate normal vectors).
     *
     * @param x             The direction to the distance light (X component)
     * @param y             The direction to the distance light (Y component)
     * @param z             The direction to the distance light (Z component)
     * @param lightColor    The color of the specular light source
     * @param surfaceScale  Scale factor to transform from alpha values to physical height
     * @param ks            Specular reflectance coefficient
     * @param shininess     The specular exponent determining how shiny the surface is
     * @param input         The input filter that defines surface normals (as alpha), or uses the source bitmap when null
     * @param crop          Optional rectangle that crops the input and output
     * @return              filter that applies distant specular lighting
     */
    public static M3ImageFilter makeDistantLitSpecular(float x, float y, float z, int lightColor, float surfaceScale, float ks, float shininess, M3ImageFilter input, M3Rect crop) {
        try {
            M3Stats.onNativeCall();
            return new M3ImageFilter(_nMakeDistantLitSpecular(x, y, z, lightColor, surfaceScale, ks, shininess, M3Native.getPtr(input), crop));
        } finally {
            M3ReferenceUtil.reachabilityFence(input);
        }
    }

    public static M3ImageFilter makeDistantLitSpecular(float x, float y, float z, int lightColor, float surfaceScale, float ks, float shininess, M3ImageFilter input, M3IRect crop) {
        return makeDistantLitSpecular(x, y, z, lightColor, surfaceScale, ks, shininess, input, crop == null ? null : crop.toRect());
    }

    public static M3ImageFilter makeDistantLitSpecular(float x, float y, float z, int lightColor, float surfaceScale, float ks, float shininess, M3ImageFilter input) {
        return makeDistantLitSpecular(x, y, z, lightColor, surfaceScale, ks, shininess, input, (M3Rect) null);
    }

    /**
     * Create a filter that calculates the specular illumination from a point light source, using
     * alpha channel of the input as the height profile of the surface (to approximate normal vectors).
     *
     * @param x             The location of the point light (X component)
     * @param y             The location of the point light (Y component)
     * @param z             The location of the point light (Z component)
     * @param lightColor    The color of the specular light source
     * @param surfaceScale  Scale factor to transform from alpha values to physical height
     * @param ks            Specular reflectance coefficient
     * @param shininess     The specular exponent determining how shiny the surface is
     * @param input         The input filter that defines surface normals (as alpha), or uses the source bitmap when null
     * @param crop          Optional rectangle that crops the input and output
     * @return              filter that applies point specular lighting
     */
    public static M3ImageFilter makePointLitSpecular(float x, float y, float z, int lightColor, float surfaceScale, float ks, float shininess, M3ImageFilter input, M3Rect crop) {
        try {
            M3Stats.onNativeCall();
            return new M3ImageFilter(_nMakePointLitSpecular(x, y, z, lightColor, surfaceScale, ks, shininess, M3Native.getPtr(input), crop));
        } finally {
            M3ReferenceUtil.reachabilityFence(input);
        }
    }

    public static M3ImageFilter makePointLitSpecular(float x, float y, float z, int lightColor, float surfaceScale, float ks, float shininess, M3ImageFilter input, M3IRect crop) {
        return makePointLitSpecular(x, y, z, lightColor, surfaceScale, ks, shininess, input, crop == null ? null : crop.toRect());
    }

    public static M3ImageFilter makePointLitSpecular(float x, float y, float z, int lightColor, float surfaceScale, float ks, float shininess, M3ImageFilter input) {
        return makePointLitSpecular(x, y, z, lightColor, surfaceScale, ks, shininess, input, (M3Rect) null);
    }

    /**
     * Create a filter that calculates the specular illumination from a spot light source, using
     * alpha channel of the input as the height profile of the surface (to approximate normal vectors).
     * The spot light is restricted to be within 'cutoffAngle' of the vector between the location and target.
     *
     * @param x0               The location of the spot light (X component)
     * @param y0               The location of the spot light (Y component)
     * @param z0               The location of the spot light (Z component)
     * @param x1               The location that the spot light is point towards (X component)
     * @param y1               The location that the spot light is point towards (Y component)
     * @param z1               The location that the spot light is point towards (Z component)
     * @param falloffExponent  Exponential falloff parameter for illumination outside of cutoffAngle
     * @param cutoffAngle      Maximum angle from lighting direction that receives full light
     * @param lightColor       The color of the specular light source
     * @param surfaceScale     Scale factor to transform from alpha values to physical height
     * @param ks               Specular reflectance coefficient
     * @param shininess        The specular exponent determining how shiny the surface is
     * @param input            The input filter that defines surface normals (as alpha), or uses the source bitmap when null
     * @param crop             Optional rectangle that crops the input and output
     * @return                 filter that applies spot specular lighting
     */
    public static M3ImageFilter makeSpotLitSpecular(float x0, float y0, float z0, float x1, float y1, float z1, float falloffExponent, float cutoffAngle, int lightColor, float surfaceScale, float ks, float shininess, M3ImageFilter input, M3Rect crop) {
        try {
            M3Stats.onNativeCall();
            return new M3ImageFilter(_nMakeSpotLitSpecular(x0, y0, z0, x1, y1, z1, falloffExponent, cutoffAngle, lightColor, surfaceScale, ks, shininess, M3Native.getPtr(input), crop));
        } finally {
            M3ReferenceUtil.reachabilityFence(input);
        }
    }

    public static M3ImageFilter makeSpotLitSpecular(float x0, float y0, float z0, float x1, float y1, float z1, float falloffExponent, float cutoffAngle, int lightColor, float surfaceScale, float ks, float shininess, M3ImageFilter input, M3IRect crop) {
        return makeSpotLitSpecular(x0, y0, z0, x1, y1, z1, falloffExponent, cutoffAngle, lightColor, surfaceScale, ks, shininess, input, crop == null ? null : crop.toRect());
    }

    public static M3ImageFilter makeSpotLitSpecular(float x0, float y0, float z0, float x1, float y1, float z1, float falloffAngle, float cutoffAngle, int lightColor, float surfaceScale, float ks, float shininess, M3ImageFilter input) {
        return makeSpotLitSpecular(x0, y0, z0, x1, y1, z1, falloffAngle, cutoffAngle, lightColor, surfaceScale, ks, shininess, input, (M3Rect) null);
    }

    public M3ImageFilter(long ptr) {
        super(ptr);
    }
    public static native long _nMakeArithmetic(float k1, float k2, float k3, float k4, boolean enforcePMColor, long bg, long fg, M3Rect crop);
    public static native long _nMakeBlend(int blendMode, long bg, long fg, M3Rect crop);
    public static native long _nMakeBlendBlender(long blenderPtr, long bg, long fg, M3Rect crop);
    public static native long _nMakeBlur(float sigmaX, float sigmaY, int tileMode, long input, M3Rect crop);
    public static native long _nMakeColorFilter(long colorFilterPtr, long input, M3Rect crop);
    public static native long _nMakeCompose(long outer, long inner);
    public static native long _nMakeCrop(float l, float t, float r, float b, int tileMode, long input);
    public static native long _nMakeDisplacementMap(int xChan, int yChan, float scale, long displacement, long color, M3Rect crop);
    public static native long _nMakeDropShadowCS(float dx, float dy, float sigmaX, float sigmaY, float r, float g, float b, float a, long colorSpace, long input, M3Rect crop);
    public static native long _nMakeDropShadow(float dx, float dy, float sigmaX, float sigmaY, int color, long input, M3Rect crop);
    public static native long _nMakeDropShadowOnlyCS(float dx, float dy, float sigmaX, float sigmaY, float r, float g, float b, float a, long colorSpace, long input, M3Rect crop);
    public static native long _nMakeDropShadowOnly(float dx, float dy, float sigmaX, float sigmaY, int color, long input, M3Rect crop);
    public static native long _nMakeEmpty();
    public static native long _nMakeImage(long image, float l0, float t0, float r0, float b0, float l1, float t1, float r1, float b1, long samplingMode);
    public static native long _nMakeMagnifier(float l, float t, float r, float b, float zoomAmount, float inset, long samplingMode, long input, M3Rect crop);
    public static native long _nMakeMatrixConvolution(int kernelW, int kernelH, float[] kernel, float gain, float bias, int offsetX, int offsetY, int tileMode, boolean convolveAlpha, long input, M3Rect crop);
    public static native long _nMakeMatrixTransform(float[] matrix, long samplingMode, long input);
    public static native long _nMakeMerge(long[] filters, M3Rect crop);
    public static native long _nMakeOffset(float dx, float dy, long input, M3Rect crop);
    public static native long _nMakePicture(long picture, float l, float t, float r, float b);
    public static native long _nMakePicture(long picture);
    public static native long _nMakeShader(long shader, boolean dither, M3Rect crop);
    public static native long _nMakeTile(float l0, float t0, float r0, float b0, float l1, float t1, float r1, float b1, long input);
    public static native long _nMakeDilate(float rx, float ry, long input, M3Rect crop);
    public static native long _nMakeErode(float rx, float ry, long input, M3Rect crop);
    public static native long _nMakeDistantLitDiffuse(float x, float y, float z, int lightColor, float surfaceScale, float kd, long input, M3Rect crop);
    public static native long _nMakePointLitDiffuse(float x, float y, float z, int lightColor, float surfaceScale, float kd, long input, M3Rect crop);
    public static native long _nMakeSpotLitDiffuse(float x0, float y0, float z0, float x1, float y1, float z1, float falloffExponent, float cutoffAngle, int lightColor, float surfaceScale, float kd, long input, M3Rect crop);
    public static native long _nMakeDistantLitSpecular(float x, float y, float z, int lightColor, float surfaceScale, float ks, float shininess, long input, M3Rect crop);
    public static native long _nMakePointLitSpecular(float x, float y, float z, int lightColor, float surfaceScale, float ks, float shininess, long input, M3Rect crop);
    public static native long _nMakeSpotLitSpecular(float x0, float y0, float z0, float x1, float y1, float z1, float falloffExponent, float cutoffAngle, int lightColor, float surfaceScale, float ks, float shininess, long input, M3Rect crop);
    public static native long _nMakeRuntimeShader(long builderPtr, float maxSampleRadius, String[] childShaderNames, long[] inputs);
}