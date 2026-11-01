package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.*;

public class M3Surface extends M3RefCnt {
    static {
        M3Library.staticLoad();
    }

    public M3Canvas _canvas;
    public final M3DirectContext _context;
    public final M3BackendRenderTarget _renderTarget;

    /**
     * @deprecated - use {@link #wrapPixels(ImageInfo, long, long)}
     */
    @Deprecated
    public static M3Surface makeRasterDirect(M3ImageInfo imageInfo,
                                           long pixelsPtr,
                                           long rowBytes) {
        return wrapPixels(imageInfo, pixelsPtr, rowBytes, null);
    }

    /**
     * <p>Allocates raster Surface. Canvas returned by Surface draws directly into pixels.</p>
     *
     * <p>Surface is returned if all parameters are valid. Valid parameters include:</p>
     *
     * <ul><li>info dimensions are greater than zero;</li>
     * <li>info contains ColorType and AlphaType supported by raster surface;</li>
     * <li>pixelsPtr is not 0;</li>
     * <li>rowBytes is large enough to contain info width pixels of ColorType.</li></ul>
     *
     * <p>Pixel buffer size should be info height times computed rowBytes.</p>
     *
     * <p>Pixels are not initialized.</p>
     *
     * <p>To access pixels after drawing, peekPixels() or readPixels().</p>
     *
     * @param imageInfo     width, height, ColorType, AlphaType, ColorSpace,
     *                      of raster surface; width and height must be greater than zero
     * @param pixelsPtr     pointer to destination pixels buffer
     * @param rowBytes      memory address of destination native pixels buffer
     * @return              created Surface
     */
    public static M3Surface wrapPixels(M3ImageInfo imageInfo,
                                     long pixelsPtr,
                                     long rowBytes) {
        return wrapPixels(imageInfo, pixelsPtr, rowBytes, null);
    }

    /**
     * @deprecated - use {@link #wrapPixels(ImageInfo, long, long, SurfaceProps)}
     */
    @Deprecated
    public static M3Surface makeRasterDirect(M3ImageInfo imageInfo,
                                           long pixelsPtr,
                                           long rowBytes,
                                           M3SurfaceProps surfaceProps) {
        return wrapPixels(imageInfo, pixelsPtr, rowBytes, surfaceProps);
    }

    /**
     * <p>Allocates raster Surface. Canvas returned by Surface draws directly into pixels.</p>
     *
     * <p>Surface is returned if all parameters are valid. Valid parameters include:</p>
     *
     * <ul><li>info dimensions are greater than zero;</li>
     * <li>info contains ColorType and AlphaType supported by raster surface;</li>
     * <li>pixelsPtr is not 0;</li>
     * <li>rowBytes is large enough to contain info width pixels of ColorType.</li></ul>
     *
     * <p>Pixel buffer size should be info height times computed rowBytes.</p>
     *
     * <p>Pixels are not initialized.</p>
     *
     * <p>To access pixels after drawing, peekPixels() or readPixels().</p>
     *
     * @param imageInfo     width, height, ColorType, AlphaType, ColorSpace,
     *                      of raster surface; width and height must be greater than zero
     * @param pixelsPtr     pointer to destination pixels buffer
     * @param rowBytes      memory address of destination native pixels buffer
     * @param surfaceProps  LCD striping orientation and setting for device independent fonts;
     *                      may be null
     * @return              created Surface
     */
    public static M3Surface wrapPixels(M3ImageInfo imageInfo,
                                     long pixelsPtr,
                                     long rowBytes,
                                     M3SurfaceProps surfaceProps) {
        try {
            assert imageInfo != null : "Can’t wrapPixels with imageInfo == null";
            M3Stats.onNativeCall();
            long ptr = _nWrapPixels(
                imageInfo._width,
                imageInfo._height,
                imageInfo._colorInfo._colorType.ordinal(),
                imageInfo._colorInfo._alphaType.ordinal(),
                M3Native.getPtr(imageInfo._colorInfo._colorSpace),
                pixelsPtr,
                rowBytes,
                surfaceProps);
            if (ptr == 0) {
                throw new IllegalArgumentException(String.format("Failed Surface.wrapPixels(%s, %d, %d, %s)", imageInfo, pixelsPtr, rowBytes, surfaceProps));
            }
            return new M3Surface(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(imageInfo._colorInfo._colorSpace);
        }
    }

    /**
     * @deprecated - use {@link #wrapPixels(Pixmap)}
     */
    @Deprecated
    public static M3Surface makeRasterDirect(M3Pixmap pixmap) {
        return wrapPixels(pixmap, null);
    }

    public static M3Surface wrapPixels(M3Pixmap pixmap) {
        return wrapPixels(pixmap, null);
    }

    /**
     * @deprecated - use {@link #wrapPixels(Pixmap, SurfaceProps)}
     */
    @Deprecated
    public static M3Surface makeRasterDirect(M3Pixmap pixmap,
                                           M3SurfaceProps surfaceProps) {
        return wrapPixels(pixmap, surfaceProps);
    }

    public static M3Surface wrapPixels(M3Pixmap pixmap,
                                     M3SurfaceProps surfaceProps) {
        try {
            assert pixmap != null : "Can’t wrapPixels with pixmap == null";
            M3Stats.onNativeCall();
            long ptr = _nWrapPixelsPixmap(M3Native.getPtr(pixmap), surfaceProps);
            if (ptr == 0) {
                throw new IllegalArgumentException(String.format("Failed Surface.wrapPixels(%s, %s)", pixmap, surfaceProps));
            }
            return new M3Surface(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(pixmap);
        }
    }

    /**
     * <p>Allocates raster Surface. Canvas returned by Surface draws directly into pixels.
     * Allocates and zeroes pixel memory. Pixel memory size is imageInfo.height() times imageInfo.minRowBytes().
     * Pixel memory is deleted when Surface is deleted.</p>
     *
     * <p>Surface is returned if all parameters are valid. Valid parameters include:</p>
     *
     * <ul><li>info dimensions are greater than zero;</li>
     * <li>info contains ColorType and AlphaType supported by raster surface;</li></ul>
     *
     * @param imageInfo     width, height, ColorType, AlphaType, ColorSpace,
     *                      of raster surface; width and height must be greater than zero
     * @return              new Surface
     */
    public static M3Surface makeRaster(M3ImageInfo imageInfo) {
        return makeRaster(imageInfo, 0, null);
    }

    /**
     * <p>Allocates raster Surface. Canvas returned by Surface draws directly into pixels.
     * Allocates and zeroes pixel memory. Pixel memory size is imageInfo.height() times
     * rowBytes, or times imageInfo.minRowBytes() if rowBytes is zero.
     * Pixel memory is deleted when Surface is deleted.</p>
     *
     * <p>Surface is returned if all parameters are valid. Valid parameters include:</p>
     *
     * <ul><li>info dimensions are greater than zero;</li>
     * <li>info contains ColorType and AlphaType supported by raster surface;</li>
     * <li>rowBytes is large enough to contain info width pixels of ColorType, or is zero.</li></ul>
     *
     * <p>If rowBytes is zero, a suitable value will be chosen internally.</p>
     *
     * @param imageInfo     width, height, ColorType, AlphaType, ColorSpace,
     *                      of raster surface; width and height must be greater than zero
     * @param rowBytes      interval from one Surface row to the next; may be zero
     * @return              new Surface
     */
    public static M3Surface makeRaster(M3ImageInfo imageInfo,
                                     long rowBytes) {
        return makeRaster(imageInfo, rowBytes, null);
    }

    /**
     * <p>Allocates raster Surface. Canvas returned by Surface draws directly into pixels.
     * Allocates and zeroes pixel memory. Pixel memory size is imageInfo.height() times
     * rowBytes, or times imageInfo.minRowBytes() if rowBytes is zero.
     * Pixel memory is deleted when Surface is deleted.</p>
     *
     * <p>Surface is returned if all parameters are valid. Valid parameters include:</p>
     *
     * <ul><li>info dimensions are greater than zero;</li>
     * <li>info contains ColorType and AlphaType supported by raster surface;</li>
     * <li>rowBytes is large enough to contain info width pixels of ColorType, or is zero.</li></ul>
     *
     * <p>If rowBytes is zero, a suitable value will be chosen internally.</p>
     *
     * @param imageInfo     width, height, ColorType, AlphaType, ColorSpace,
     *                      of raster surface; width and height must be greater than zero
     * @param rowBytes      interval from one Surface row to the next; may be zero
     * @param surfaceProps  LCD striping orientation and setting for device independent fonts;
     *                      may be null
     * @return              new Surface
     */
    public static M3Surface makeRaster(M3ImageInfo imageInfo,
                                     long rowBytes,
                                     M3SurfaceProps surfaceProps) {
        try {
            assert imageInfo != null : "Can’t makeRaster with imageInfo == null";
            M3Stats.onNativeCall();
            long ptr = _nMakeRaster(
                imageInfo._width,
                imageInfo._height,
                imageInfo._colorInfo._colorType.ordinal(),
                imageInfo._colorInfo._alphaType.ordinal(),
                M3Native.getPtr(imageInfo._colorInfo._colorSpace),
                rowBytes,
                surfaceProps);
            if (ptr == 0)
                throw new IllegalArgumentException(String.format("Failed Surface.makeRaster(%s, %d, %s)", imageInfo, rowBytes, surfaceProps));
            return new M3Surface(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(imageInfo._colorInfo._colorSpace);
        }
    }

    /**
     * @deprecated - use {@link #wrapBackendRenderTarget(DirectContext, BackendRenderTarget, SurfaceOrigin, ColorType, ColorSpace)}
     */
    @Deprecated
    public static M3Surface makeFromBackendRenderTarget(M3DirectContext context,
                                                      M3BackendRenderTarget rt,
                                                      M3SurfaceOrigin origin,
                                                      M3ColorType colorType,
                                                      M3ColorSpace colorSpace) {
        return wrapBackendRenderTarget(context, rt, origin, colorType, colorSpace, null);
    }

    /**
     * <p>Wraps a GPU-backed buffer into {@link Surface}.</p>
     *
     * <p>Caller must ensure backendRenderTarget is valid for the lifetime of returned {@link Surface}.</p>
     *
     * <p>{@link Surface} is returned if all parameters are valid. backendRenderTarget is valid if its pixel
     * configuration agrees with colorSpace and context;
     * for instance, if backendRenderTarget has an sRGB configuration, then context must support sRGB,
     * and colorSpace must be present. Further, backendRenderTarget width and height must not exceed
     * context capabilities, and the context must be able to support back-end render targets.</p>
     *
     * @param context       GPU context
     * @param rt            texture residing on GPU
     * @param origin        surfaceOrigin pins either the top-left or the bottom-left corner to the origin.
     * @param colorType   color format
     * @param colorSpace    range of colors; may be null
     * @return              Surface if all parameters are valid; otherwise, null
     * @see <a href="https://fiddle.skia.org/c/@Surface_MakeFromBackendTexture">https://fiddle.skia.org/c/@Surface_MakeFromBackendTexture</a>
     */
    public static M3Surface wrapBackendRenderTarget(M3DirectContext context,
                                                  M3BackendRenderTarget rt,
                                                  M3SurfaceOrigin origin,
                                                  M3ColorType colorType,
                                                  M3ColorSpace colorSpace) {
        return wrapBackendRenderTarget(context, rt, origin, colorType, colorSpace, null);
    }

    /**
     * @deprecated - use {@link #wrapBackendRenderTarget(DirectContext, BackendRenderTarget, SurfaceOrigin, ColorType, ColorSpace, SurfaceProps)}
     */
    @Deprecated
    public static M3Surface makeFromBackendRenderTarget(M3DirectContext context,
                                                      M3BackendRenderTarget rt,
                                                      M3SurfaceOrigin origin,
                                                      M3ColorType colorType,
                                                      M3ColorSpace colorSpace,
                                                      M3SurfaceProps surfaceProps) {
        return wrapBackendRenderTarget(context, rt, origin, colorType, colorSpace, surfaceProps);
    }

    /**
     * <p>Wraps a GPU-backed buffer into {@link Surface}.</p>
     *
     * <p>Caller must ensure backendRenderTarget is valid for the lifetime of returned {@link Surface}.</p>
     *
     * <p>{@link Surface} is returned if all parameters are valid. backendRenderTarget is valid if its pixel
     * configuration agrees with colorSpace and context;
     * for instance, if backendRenderTarget has an sRGB configuration, then context must support sRGB,
     * and colorSpace must be present. Further, backendRenderTarget width and height must not exceed
     * context capabilities, and the context must be able to support back-end render targets.</p>
     *
     * @param context       GPU context
     * @param rt            texture residing on GPU
     * @param origin        surfaceOrigin pins either the top-left or the bottom-left corner to the origin.
     * @param colorType   color format
     * @param colorSpace    range of colors; may be null
     * @param surfaceProps  LCD striping orientation and setting for device independent fonts; may be null
     * @return              Surface if all parameters are valid; otherwise, null
     * @see <a href="https://fiddle.skia.org/c/@Surface_MakeFromBackendTexture">https://fiddle.skia.org/c/@Surface_MakeFromBackendTexture</a>
     */

    public static M3Surface wrapBackendRenderTarget(M3DirectContext context,
                                                  M3BackendRenderTarget rt,
                                                  M3SurfaceOrigin origin,
                                                  M3ColorType colorType,
                                                  M3ColorSpace colorSpace,
                                                  M3SurfaceProps surfaceProps) {
        try {
            assert context != null : "Can’t wrapBackendRenderTarget with context == null";
            assert rt != null : "Can’t wrapBackendRenderTarget with rt == null";
            assert origin != null : "Can’t wrapBackendRenderTarget with origin == null";
            assert colorType != null : "Can’t wrapBackendRenderTarget with colorType == null";
            M3Stats.onNativeCall();
            long ptr = _nWrapBackendRenderTarget(M3Native.getPtr(context), M3Native.getPtr(rt), origin.ordinal(), colorType.ordinal(), M3Native.getPtr(colorSpace), surfaceProps);
            if (ptr == 0)
                throw new IllegalArgumentException(String.format("Failed Surface.wrapBackendRenderTarget(%s, %s, %s, %s, %s)", context, rt, origin, colorType, colorSpace));
            return new M3Surface(ptr, context, rt);
        } finally {
            M3ReferenceUtil.reachabilityFence(context);
            M3ReferenceUtil.reachabilityFence(rt);
            M3ReferenceUtil.reachabilityFence(colorSpace);
        }
    }

    /**
     * @deprecated - use {@link #wrapMTKView(DirectContext, long, SurfaceOrigin, int, ColorType, ColorSpace, SurfaceProps)}
     */
    @Deprecated
    public static M3Surface makeFromMTKView(M3DirectContext context,
                                          long mtkViewPtr,
                                          M3SurfaceOrigin origin,
                                          int sampleCount,
                                          M3ColorType colorType,
                                          M3ColorSpace colorSpace,
                                          M3SurfaceProps surfaceProps) {
        return wrapMTKView(context, mtkViewPtr, origin, sampleCount, colorType, colorSpace, surfaceProps);
    }

    public static M3Surface wrapMTKView(M3DirectContext context,
                                      long mtkViewPtr,
                                      M3SurfaceOrigin origin,
                                      int sampleCount,
                                      M3ColorType colorType,
                                      M3ColorSpace colorSpace,
                                      M3SurfaceProps surfaceProps) {
        try {
            assert context != null : "Can’t wrapMTKView with context == null";
            assert origin != null : "Can’t wrapMTKView with origin == null";
            assert colorType != null : "Can’t wrapMTKView with colorType == null";
            M3Stats.onNativeCall();
            long ptr = _nWrapMTKView(M3Native.getPtr(context), mtkViewPtr, origin.ordinal(), sampleCount, colorType.ordinal(), M3Native.getPtr(colorSpace), surfaceProps);
            if (ptr == 0)
                throw new IllegalArgumentException(String.format("Failed Surface.WrapMTKView(%s, %s, %s, %s, %s, %s)", context, mtkViewPtr, origin, colorType, colorSpace, surfaceProps));
            return new M3Surface(ptr, context);
        } finally {
            M3ReferenceUtil.reachabilityFence(context);
            M3ReferenceUtil.reachabilityFence(colorSpace);
        }
    }

    /**
     * @deprecated - use makeRaster(ImageInfo.makeN32Premul(width, height))
     *
     * <p>Allocates raster {@link Surface}.</p>
     *
     * <p>Canvas returned by Surface draws directly into pixels. Allocates and zeroes pixel memory.
     * Pixel memory size is height times width times four. Pixel memory is deleted when Surface is deleted.</p>
     *
     * <p>Internally, sets ImageInfo to width, height, native color type, and ColorAlphaType.PREMUL.</p>
     *
     * <p>Surface is returned if width and height are greater than zero.</p>
     *
     * <p>Use to create Surface that matches PMColor, the native pixel arrangement on the platform.
     * Surface drawn to output device skips converting its pixel format.</p>
     *
     * @param width  pixel column count; must be greater than zero
     * @param height pixel row count; must be greater than zero
     * @return Surface if all parameters are valid; otherwise, null
     * @see <a href="https://fiddle.skia.org/c/@Surface_MakeRasterN32Premul">https://fiddle.skia.org/c/@Surface_MakeRasterN32Premul</a>
     */
    @Deprecated
    public static M3Surface makeRasterN32Premul(int width, int height) {
        return makeRaster(M3ImageInfo.makeN32Premul(width, height));
    }

    /**
     * <p>Returns Surface on GPU indicated by context. Allocates memory for
     * pixels, based on the width, height, and ColorType in ImageInfo.
     * describes the pixel format in ColorType, and transparency in
     * AlphaType, and color matching in ColorSpace.</p>
     *
     * @param context               GPU context
     * @param budgeted              selects whether allocation for pixels is tracked by context
     * @param imageInfo             width, height, ColorType, AlphaType, ColorSpace;
     *                              width, or height, or both, may be zero
     * @return                      new SkSurface
     */
    public static M3Surface makeRenderTarget(M3DirectContext context,
                                           boolean budgeted,
                                           M3ImageInfo imageInfo) {
        return makeRenderTarget(context, budgeted, imageInfo, 0, M3SurfaceOrigin.BOTTOM_LEFT, null, false);
    }

    /**
     * <p>Returns Surface on GPU indicated by context. Allocates memory for
     * pixels, based on the width, height, and ColorType in ImageInfo.
     * describes the pixel format in ColorType, and transparency in
     * AlphaType, and color matching in ColorSpace.</p>
     *
     * <p>sampleCount requests the number of samples per pixel.
     * Pass zero to disable multi-sample anti-aliasing.  The request is rounded
     * up to the next supported count, or rounded down if it is larger than the
     * maximum supported count.</p>
     *
     * @param context               GPU context
     * @param budgeted              selects whether allocation for pixels is tracked by context
     * @param imageInfo             width, height, ColorType, AlphaType, ColorSpace;
     *                              width, or height, or both, may be zero
     * @param sampleCount           samples per pixel, or 0 to disable full scene anti-aliasing
     * @param surfaceProps          LCD striping orientation and setting for device independent
     *                               fonts; may be null
     * @return                      new SkSurface
     */
    public static M3Surface makeRenderTarget(M3DirectContext context,
                                           boolean budgeted,
                                           M3ImageInfo imageInfo,
                                           int sampleCount,
                                           M3SurfaceProps surfaceProps) {
        return makeRenderTarget(context, budgeted, imageInfo, sampleCount, M3SurfaceOrigin.BOTTOM_LEFT, surfaceProps, false);
    }

    /**
     * <p>Returns Surface on GPU indicated by context. Allocates memory for
     * pixels, based on the width, height, and ColorType in ImageInfo.
     * describes the pixel format in ColorType, and transparency in
     * AlphaType, and color matching in ColorSpace.</p>
     *
     * <p>sampleCount requests the number of samples per pixel.
     * Pass zero to disable multi-sample anti-aliasing.  The request is rounded
     * up to the next supported count, or rounded down if it is larger than the
     * maximum supported count.</p>
     *
     * @param context               GPU context
     * @param budgeted              selects whether allocation for pixels is tracked by context
     * @param imageInfo             width, height, ColorType, AlphaType, ColorSpace;
     *                              width, or height, or both, may be zero
     * @param sampleCount           samples per pixel, or 0 to disable full scene anti-aliasing
     * @param origin                pins either the top-left or the bottom-left corner to the origin.
     * @param surfaceProps          LCD striping orientation and setting for device independent
     *                               fonts; may be null
     * @return                      new SkSurface
     */
    public static M3Surface makeRenderTarget(M3DirectContext context,
                                           boolean budgeted,
                                           M3ImageInfo imageInfo,
                                           int sampleCount,
                                           M3SurfaceOrigin origin,
                                           M3SurfaceProps surfaceProps) {
        return makeRenderTarget(context, budgeted, imageInfo, sampleCount, origin, surfaceProps, false);
    }

    /**
     * <p>Returns Surface on GPU indicated by context. Allocates memory for
     * pixels, based on the width, height, and ColorType in ImageInfo.
     * describes the pixel format in ColorType, and transparency in
     * AlphaType, and color matching in ColorSpace.</p>
     *
     * <p>sampleCount requests the number of samples per pixel.
     * Pass zero to disable multi-sample anti-aliasing.  The request is rounded
     * up to the next supported count, or rounded down if it is larger than the
     * maximum supported count.</p>
     *
     * <p>shouldCreateWithMips hints that Image returned by {@link #makeImageSnapshot()} is mip map.</p>
     *
     * @param context               GPU context
     * @param budgeted              selects whether allocation for pixels is tracked by context
     * @param imageInfo             width, height, ColorType, AlphaType, ColorSpace;
     *                              width, or height, or both, may be zero
     * @param sampleCount           samples per pixel, or 0 to disable full scene anti-aliasing
     * @param origin                pins either the top-left or the bottom-left corner to the origin.
     * @param surfaceProps          LCD striping orientation and setting for device independent
     *                               fonts; may be null
     * @param shouldCreateWithMips  hint that SkSurface will host mip map images
     * @return                      new SkSurface
     */
    public static M3Surface makeRenderTarget(M3DirectContext context,
                                           boolean budgeted,
                                           M3ImageInfo imageInfo,
                                           int sampleCount,
                                           M3SurfaceOrigin origin,
                                           M3SurfaceProps surfaceProps,
                                           boolean shouldCreateWithMips) {
        try {
            assert context != null : "Can’t makeFromBackendRenderTarget with context == null";
            assert imageInfo != null : "Can’t makeFromBackendRenderTarget with imageInfo == null";
            assert origin != null : "Can’t makeFromBackendRenderTarget with origin == null";
            M3Stats.onNativeCall();
            long ptr = _nMakeRenderTarget(
                M3Native.getPtr(context),
                budgeted,
                imageInfo._width,
                imageInfo._height,
                imageInfo._colorInfo._colorType.ordinal(),
                imageInfo._colorInfo._alphaType.ordinal(),
                M3Native.getPtr(imageInfo._colorInfo._colorSpace),
                sampleCount,
                origin.ordinal(),
                surfaceProps,
                shouldCreateWithMips);
            if (ptr == 0)
                throw new IllegalArgumentException(String.format("Failed Surface.makeRenderTarget(%s, %b, %s, %d, %s, %s, %b)", context, budgeted, imageInfo, sampleCount, origin, surfaceProps, shouldCreateWithMips));
            return new M3Surface(ptr, context);
        } finally {
            M3ReferenceUtil.reachabilityFence(context);
            M3ReferenceUtil.reachabilityFence(imageInfo._colorInfo._colorSpace);
        }
    }

    /**
     * Returns Surface without backing pixels. Drawing to Canvas returned from Surface
     * has no effect. Calling makeImageSnapshot() on returned Surface returns null.
     *
     * @param width   one or greater
     * @param height  one or greater
     * @return        Surface if width and height are positive
     *
     * @see <a href="https://fiddle.skia.org/c/@Surface_MakeNull">https://fiddle.skia.org/c/@Surface_MakeNull</a>
     */
    public static M3Surface makeNull(int width, int height) {
        M3Stats.onNativeCall();
        long ptr = _nMakeNull(width, height);
        if (ptr == 0)
            throw new IllegalArgumentException(String.format("Failed Surface.makeNull(%d, %d)", width, height));
        return new M3Surface(ptr);
    }

    /**
     * <p>Returns pixel count in each row; may be zero or greater.</p>
     *
     * @return number of pixel columns
     * @see <a href="https://fiddle.skia.org/c/@Surface_width">https://fiddle.skia.org/c/@Surface_width</a>
     */
    public int getWidth() {
        try {
            M3Stats.onNativeCall();
            return _nGetWidth(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Returns pixel row count; may be zero or greater.</p>
     *
     * @return number of pixel rows
     * @see <a href="https://fiddle.skia.org/c/@Surface_height">https://fiddle.skia.org/c/@Surface_height</a>
     */
    public int getHeight() {
        try {
            M3Stats.onNativeCall();
            return _nGetHeight(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Returns an ImageInfo describing the surface.</p>
     *
     * @return ImageInfo describing the surface.
     */
    public M3ImageInfo getImageInfo() {
        try {
            M3Stats.onNativeCall();
            return _nGetImageInfo(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Returns unique value identifying the content of Surface.</p>
     *
     * <p>Returned value changes each time the content changes.
     * Content is changed by drawing, or by calling notifyContentWillChange().</p>
     *
     * @return unique content identifier
     */
    public int getGenerationId() {
        try {
            M3Stats.onNativeCall();
            return _nGenerationId(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Notifies that Surface contents will be changed by code outside of Skia.</p>
     *
     * <p>Subsequent calls to generationID() return a different value.</p>
     *
     * @see <a href="https://fiddle.skia.org/c/@Surface_notifyContentWillChange">https://fiddle.skia.org/c/@Surface_notifyContentWillChange</a>
     */
    public void notifyContentWillChange(M3ContentChangeMode mode) {
        try {
            M3Stats.onNativeCall();
            _nNotifyContentWillChange(_ptr, mode.ordinal());
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Returns the recording context being used by the Surface.</p>
     *
     * @return the recording context, if available; null otherwise
     */
    public M3DirectContext getRecordingContext() {
        try {
            M3Stats.onNativeCall();
            long ptr = _nGetRecordingContext(_ptr);
            return ptr == 0 ? null : new M3DirectContext(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Returns Canvas that draws into Surface.</p>
     *
     * <p>Subsequent calls return the same Canvas.
     * Canvas returned is managed and owned by Surface, and is deleted when Surface is deleted.</p>
     *
     * @return Canvas for Surface
     */
    public M3Canvas getCanvas() {
        try {
            if (_canvas == null) {
                M3Stats.onNativeCall();
                long ptr = _nGetCanvas(_ptr);
                _canvas = ptr == 0 ? null : new M3Canvas(ptr, false, this);
            }
            return _canvas;
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Returns a compatible Surface, or null.</p>
     *
     * <p>Returned Surface contains the same raster, GPU, or null properties as the original.
     * Returned Surface does not share the same pixels.</p>
     *
     * <p>Returns null if imageInfo width or height are zero, or if imageInfo is incompatible with Surface.</p>
     *
     * @param imageInfo contains width, height, AlphaType, ColorType, ColorSpace
     * @return compatible SkSurface or null
     * @see <a href="https://fiddle.skia.org/c/@Surface_makeSurface">https://fiddle.skia.org/c/@Surface_makeSurface</a>
     */
    public M3Surface makeSurface(M3ImageInfo imageInfo) {
        try {
            M3Stats.onNativeCall();
            long ptr = _nMakeSurfaceI(_ptr,
                    imageInfo._width,
                    imageInfo._height,
                    imageInfo._colorInfo._colorType.ordinal(),
                    imageInfo._colorInfo._alphaType.ordinal(),
                    M3Native.getPtr(imageInfo._colorInfo._colorSpace));
            return new M3Surface(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Calls makeSurface(ImageInfo) with the same ImageInfo as this surface,
     * but with the specified width and height.</p>
     *
     * <p>Returned Surface contains the same raster, GPU, or null properties as the original.
     * Returned Surface does not share the same pixels.</p>
     *
     * <p>Returns null if imageInfo width or height are zero, or if imageInfo is incompatible with Surface.</p>
     *
     * @param width  pixel column count; must be greater than zero
     * @param height pixel row count; must be greater than zero
     * @return compatible SkSurface or null
     */
    public M3Surface makeSurface(int width, int height) {
        try {
            M3Stats.onNativeCall();
            long ptr = _nMakeSurface(_ptr, width, height);
            return new M3Surface(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Returns Image capturing Surface contents.</p>
     *
     * <p>Subsequent drawing to Surface contents are not captured.
     * Image allocation is accounted for if Surface was created with SkBudgeted::kYes.</p>
     *
     * @return Image initialized with Surface contents
     * @see <a href="https://fiddle.skia.org/c/@Surface_makeImageSnapshot">https://fiddle.skia.org/c/@Surface_makeImageSnapshot</a>
     */
    public M3Image makeImageSnapshot() {
        try {
            M3Stats.onNativeCall();
            return new M3Image(_nMakeImageSnapshot(_ptr));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Like the no-parameter version, this returns an image of the current surface contents.</p>
     *
     * <p>This variant takes a rectangle specifying the subset of the surface that is of interest.
     * These bounds will be sanitized before being used.</p>
     *
     * <ul>
     *  <li>If bounds extends beyond the surface, it will be trimmed to just the intersection of it and the surface.</li>
     *  <li>If bounds does not intersect the surface, then this returns null.</li>
     *  <li>If bounds == the surface, then this is the same as calling the no-parameter variant.</li>
     * </ul>
     *
     * @return Image initialized with Surface contents or null
     * @see <a href="https://fiddle.skia.org/c/@Surface_makeImageSnapshot_2">https://fiddle.skia.org/c/@Surface_makeImageSnapshot_2</a>
     */
    public M3Image makeImageSnapshot(M3IRect area) {
        try {
            assert area != null : "Can’t Surface.makeImageSnapshot with area == null";
            M3Stats.onNativeCall();
            long ptr = _nMakeImageSnapshotR(_ptr, area._left, area._top, area._right, area._bottom);
            return ptr == 0 ? null : new M3Image(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Draws Surface contents to canvas, with its top-left corner at (x, y).</p>
     *
     * <p>If Paint paint is not null, apply ColorFilter, alpha, ImageFilter, and BlendMode.</p>
     *
     * @param canvas Canvas drawn into
     * @param x      horizontal offset in Canvas
     * @param y      vertical offset in Canvas
     * @param paint  Paint containing BlendMode, ColorFilter, ImageFilter, and so on; or null
     * @see <a href="https://fiddle.skia.org/c/@Surface_draw">https://fiddle.skia.org/c/@Surface_draw</a>
     */
    public void draw(M3Canvas canvas, int x, int y, M3Paint paint) {
        try {
            M3Stats.onNativeCall();
            _nDraw(_ptr, M3Native.getPtr(canvas), x, y, M3Native.getPtr(paint));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(canvas);
            M3ReferenceUtil.reachabilityFence(paint);
        }
    }

    public boolean peekPixels(M3Pixmap pixmap) {
        try {
            M3Stats.onNativeCall();
            return _nPeekPixels(_ptr, M3Native.getPtr(pixmap));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(pixmap);
        }
    }

    public boolean readPixels(M3Pixmap pixmap, int srcX, int srcY) {
        try {
            M3Stats.onNativeCall();
            return _nReadPixelsToPixmap(_ptr, M3Native.getPtr(pixmap), srcX, srcY);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(pixmap);
        }
    }

    /**
     * <p>Copies Rect of pixels from Surface into bitmap.</p>
     *
     * <p>Source Rect corners are (srcX, srcY) and Surface (width(), height()).
     * Destination Rect corners are (0, 0) and (bitmap.width(), bitmap.height()).
     * Copies each readable pixel intersecting both rectangles, without scaling,
     * converting to bitmap.colorType() and bitmap.alphaType() if required.</p>
     *
     * <p>Pixels are readable when Surface is raster, or backed by a GPU.</p>
     *
     * <p>The destination pixel storage must be allocated by the caller.</p>
     *
     * <p>Pixel values are converted only if ColorType and AlphaType do not match.
     * Only pixels within both source and destination rectangles are copied.
     * dst contents outside Rect intersection are unchanged.</p>
     *
     * <p>Pass negative values for srcX or srcY to offset pixels across or down destination.</p>
     *
     * <p>Does not copy, and returns false if:</p>
     *
     * <ul>
     *  <li>Source and destination rectangles do not intersect.</li>
     *  <li>Surface pixels could not be converted to dst.colorType() or dst.alphaType().</li>
     *  <li>dst pixels could not be allocated.</li>
     *  <li>dst.rowBytes() is too small to contain one row of pixels.</li>
     * </ul>
     *
     * @param bitmap  storage for pixels copied from SkSurface
     * @param srcX    offset into readable pixels on x-axis; may be negative
     * @param srcY    offset into readable pixels on y-axis; may be negative
     * @return        true if pixels were copied
     * @see <a href="https://fiddle.skia.org/c/@Surface_readPixels_3">https://fiddle.skia.org/c/@Surface_readPixels_3</a>
     */
    public boolean readPixels(M3Bitmap bitmap, int srcX, int srcY) {
        try {
            M3Stats.onNativeCall();
            return _nReadPixels(_ptr, M3Native.getPtr(bitmap), srcX, srcY);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(bitmap);
        }
    }

    public void writePixels(M3Pixmap pixmap, int x, int y) {
        try {
            M3Stats.onNativeCall();
            _nWritePixelsFromPixmap(_ptr, M3Native.getPtr(pixmap), x, y);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(pixmap);
        }
    }

    /**
     * <p>Copies Rect of pixels from the src Bitmap to the Surface.</p>
     *
     * <p>Source Rect corners are (0, 0) and (src.width(), src.height()).
     * Destination Rect corners are (dstX, dstY) and (dstX + Surface width(), dstY + Surface height()).</p>
     *
     * <p>Copies each readable pixel intersecting both rectangles, without scaling,
     * converting to Surface colorType() and Surface alphaType() if required.</p>
     *
     * @param bitmap storage for pixels to copy to Surface
     * @param x      x-axis position relative to Surface to begin copy; may be negative
     * @param y      y-axis position relative to Surface to begin copy; may be negative
     * @see <a href="https://fiddle.skia.org/c/@Surface_writePixels_2">https://fiddle.skia.org/c/@Surface_writePixels_2</a>
     */
    public void writePixels(M3Bitmap bitmap, int x, int y) {
        try {
            M3Stats.onNativeCall();
            _nWritePixels(_ptr, M3Native.getPtr(bitmap), x, y);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(bitmap);
        }
    }

    /**
     * <p>May return true if the caller is the only owner.</p>
     *
     * <p>Ensures that all previous owner's actions are complete.</p>
     */
    public boolean isUnique() {
        try {
            M3Stats.onNativeCall();
            return _nUnique(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3Surface(long ptr) {
        super(ptr);
        _context = null;
        _renderTarget = null;
    }

    public M3Surface(long ptr, M3DirectContext context) {
        super(ptr);
        _context = context;
        _renderTarget = null;
    }

    public M3Surface(long ptr, M3DirectContext context, M3BackendRenderTarget renderTarget) {
        super(ptr);
        _context = context;
        _renderTarget = renderTarget;
    }

    @Override
    public void close() {
        if (_canvas != null) {
            _canvas.invalidate();
            _canvas = null;
        }
        super.close();
    }

    public static native long _nWrapPixels(int width, int height, int colorType, int alphaType, long colorSpacePtr, long pixelsPtr, long rowBytes, M3SurfaceProps surfaceProps);
    public static native long _nWrapPixelsPixmap(long pixmapPtr, M3SurfaceProps surfaceProps);
    public static native long _nMakeRaster(int width, int height, int colorType, int alphaType, long colorSpacePtr, long rowBytes, M3SurfaceProps surfaceProps);
    public static native long _nWrapBackendRenderTarget(long pContext, long pBackendRenderTarget, int surfaceOrigin, int colorType, long colorSpacePtr, M3SurfaceProps surfaceProps);
    public static native long _nWrapMTKView(long contextPtr, long mtkViewPtr, int surfaceOrigin, int sampleCount, int colorType, long colorSpacePtr, M3SurfaceProps surfaceProps);
    public static native long _nMakeRenderTarget(long contextPtr, boolean budgeted, int width, int height, int colorType, int alphaType, long colorSpacePtr, int sampleCount, int surfaceOrigin, M3SurfaceProps surfaceProps, boolean shouldCreateWithMips);
    public static native long _nMakeNull(int width, int height);
    public static native int _nGetWidth(long ptr);
    public static native int _nGetHeight(long ptr);
    public static native M3ImageInfo _nGetImageInfo(long ptr);
    public static native int _nGenerationId(long ptr);
    public static native void _nNotifyContentWillChange(long ptr, int mode);
    public static native long _nGetRecordingContext(long ptr);
    public static native long _nGetCanvas(long ptr);
    public static native long _nMakeSurfaceI(long ptr, int width, int height, int colorType, int alphaType, long colorSpacePtr);
    public static native long _nMakeSurface(long ptr, int width, int height);
    public static native long _nMakeImageSnapshot(long ptr);
    public static native long _nMakeImageSnapshotR(long ptr, int left, int top, int right, int bottom);
    public static native void _nDraw(long ptr, long canvasPtr, float x, float y, long paintPtr);
    public static native boolean _nPeekPixels(long ptr, long pixmapPtr);
    public static native boolean _nReadPixelsToPixmap(long ptr, long pixmapPtr, int srcX, int srcY);
    public static native boolean _nReadPixels(long ptr, long bitmapPtr, int srcX, int srcY);
    public static native void _nWritePixelsFromPixmap(long ptr, long pixmapPtr, int x, int y);
    public static native void _nWritePixels(long ptr, long bitmapPtr, int x, int y);
    public static native boolean _nUnique(long ptr);
}