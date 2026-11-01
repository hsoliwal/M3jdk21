package jdk.m3.graphics;

import java.nio.*;
import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.*;

public class M3Image extends M3RefCnt implements M3IHasImageInfo {
    static { M3Library.staticLoad(); }

    public M3ImageInfo _imageInfo = null;

    public M3Image(long ptr) {
        super(ptr);
    }

    /**
     * @deprecated - use {@link #makeRasterFromBytes(ImageInfo, byte[], long)}
     */
    @Deprecated
    public static M3Image makeRaster(M3ImageInfo imageInfo, byte[] bytes, long rowBytes) {
        return makeRasterFromBytes(imageInfo, bytes, rowBytes);
    }

    /**
     * <p>Creates Image and adopts texture.</p>
     *
     * <p>Image is returned if the texture is valid.</p>
     *
     * @param directContext  GrDirectContext
     * @param backendTexture GrBackendTexture
     * @param surfaceOrigin  GrSurfaceOrigin
     * @param colorType      SkColorType
     * @return               Image
     */
    public static M3Image adoptTextureFrom(M3DirectContext directContext, M3BackendTexture backendTexture, M3SurfaceOrigin surfaceOrigin, M3ColorType colorType) {
        try {
            M3Stats.onNativeCall();
            long ptr = _nAdoptTextureFrom(M3Native.getPtr(directContext),
                                          M3Native.getPtr(backendTexture),
                                          surfaceOrigin.ordinal(),
                                          colorType.ordinal());
            if (ptr == 0)
                throw new RuntimeException("Failed to adoptTextureFrom " + backendTexture);
            return new M3Image(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(directContext);
            M3ReferenceUtil.reachabilityFence(backendTexture);
        }
    }

    /**
     * <p>Creates image and borrows texture.</p>
     *
     * <p>Image is returned if the texture is valid.</p>
     *
     * @param directContext  GrDirectContext
     * @param backendTexture GrBackendTexture
     * @param surfaceOrigin  GrSurfaceOrigin
     * @param colorType      SkColorType
     * @param colorAlphaType SkAlphaType
     * @param colorSpace     SkColorSpace
     * @param releaseProc    TextureReleaseProc
     * @return               Image
     */
    public static M3Image borrowTextureFrom(M3DirectContext directContext, M3BackendTexture backendTexture, M3SurfaceOrigin surfaceOrigin, M3ColorType colorType, M3ColorAlphaType colorAlphaType, M3ColorSpace colorSpace, Runnable releaseProc) {
        try {
            M3Stats.onNativeCall();
            long ptr = _nBorrowTextureFrom(M3Native.getPtr(directContext),
                                           M3Native.getPtr(backendTexture),
                                           surfaceOrigin.ordinal(),
                                           colorType.ordinal(),
                                           colorAlphaType.ordinal(),
                                           M3Native.getPtr(colorSpace),
                                           releaseProc);
            if (ptr == 0)
                throw new RuntimeException("Failed to borrowTextureFrom " + backendTexture);
            return new M3Image(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(directContext);
            M3ReferenceUtil.reachabilityFence(backendTexture);
            M3ReferenceUtil.reachabilityFence(colorSpace);
        }
    }
    /**
     * <p>Creates Image from pixels.</p>
     *
     * <p>Image is returned if pixels are valid. Valid Pixmap parameters include:</p>
     * <ul>
     * <li>dimensions are greater than zero;</li>
     * <li>each dimension fits in 29 bits;</li>
     * <li>ColorType and AlphaType are valid, and ColorType is not ColorType.UNKNOWN;</li>
     * <li>row bytes are large enough to hold one row of pixels;</li>
     * <li>pixel address is not null.</li>
     * </ul>
     *
     * @param imageInfo  ImageInfo
     * @param bytes      pixels array
     * @param rowBytes   how many bytes in a row
     * @return           Image
     *
     * @see <a href="https://fiddle.skia.org/c/@Image_MakeRasterCopy">https://fiddle.skia.org/c/@Image_MakeRasterCopy</a>
     */
    public static M3Image makeRasterFromBytes(M3ImageInfo imageInfo, byte[] bytes, long rowBytes) {
        try {
            M3Stats.onNativeCall();
            long ptr = _nMakeRasterFromBytes(imageInfo._width,
                                             imageInfo._height,
                                             imageInfo._colorInfo._colorType.ordinal(),
                                             imageInfo._colorInfo._alphaType.ordinal(),
                                             M3Native.getPtr(imageInfo._colorInfo._colorSpace),
                                             bytes,
                                             rowBytes);
            if (ptr == 0)
                throw new RuntimeException("Failed to makeRaster " + imageInfo + " " + bytes + " " + rowBytes);
            return new M3Image(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(imageInfo._colorInfo._colorSpace);
        }
    }

    /**
     * @deprecated - use {@link #makeRasterFromData(ImageInfo, Data, long)}
     */
    @Deprecated
    public static M3Image makeRaster(M3ImageInfo imageInfo, M3Data data, long rowBytes) {
        return makeRasterFromData(imageInfo, data, rowBytes);
    }

    /**
     * <p>Creates Image from pixels.</p>
     *
     * <p>Image is returned if pixels are valid. Valid Pixmap parameters include:</p>
     * <ul>
     * <li>dimensions are greater than zero;</li>
     * <li>each dimension fits in 29 bits;</li>
     * <li>ColorType and AlphaType are valid, and ColorType is not ColorType.UNKNOWN;</li>
     * <li>row bytes are large enough to hold one row of pixels;</li>
     * <li>pixel address is not null.</li>
     * </ul>
     *
     * @param imageInfo  ImageInfo
     * @param data       pixels array
     * @param rowBytes   how many bytes in a row
     * @return           Image
     */
    public static M3Image makeRasterFromData(M3ImageInfo imageInfo, M3Data data, long rowBytes) {
        try {
            M3Stats.onNativeCall();
            long ptr = _nMakeRasterFromData(imageInfo._width,
                                            imageInfo._height,
                                            imageInfo._colorInfo._colorType.ordinal(),
                                            imageInfo._colorInfo._alphaType.ordinal(),
                                            M3Native.getPtr(imageInfo._colorInfo._colorSpace),
                                            M3Native.getPtr(data),
                                            rowBytes);
            if (ptr == 0)
                throw new RuntimeException("Failed to makeRaster " + imageInfo + " " + data + " " + rowBytes);
            return new M3Image(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(imageInfo._colorInfo._colorSpace);
            M3ReferenceUtil.reachabilityFence(data);
        }
    }

    /**
     * @deprecated - use {@link #makeRasterFromBitmap(Bitmap)}
     */
    @Deprecated
    public static M3Image makeFromBitmap(M3Bitmap bitmap) {
        return makeRasterFromBitmap(bitmap);
    }

    /**
     * <p>Creates Image from bitmap, sharing or copying bitmap pixels. If the bitmap
     * is marked immutable, and its pixel memory is shareable, it may be shared
     * instead of copied.</p>
     *
     * <p>Image is returned if bitmap is valid. Valid Bitmap parameters include:</p>
     * <ul>
     * <li>dimensions are greater than zero;</li>
     * <li>each dimension fits in 29 bits;</li>
     * <li>ColorType and AlphaType are valid, and ColorType is not ColorType.UNKNOWN;</li>
     * <li>row bytes are large enough to hold one row of pixels;</li>
     * <li>pixel address is not nullptr.</li>
     * </ul>
     *
     * @param bitmap  ImageInfo, row bytes, and pixels
     * @return        created Image
     *
     * @see <a href="https://fiddle.skia.org/c/@Image_MakeFromBitmap">https://fiddle.skia.org/c/@Image_MakeFromBitmap</a>
     */
    public static M3Image makeRasterFromBitmap(M3Bitmap bitmap) {
        try {
            assert bitmap != null : "Can’t makeFromBitmap with bitmap == null";
            M3Stats.onNativeCall();
            long ptr = _nMakeRasterFromBitmap(M3Native.getPtr(bitmap));
            if (ptr == 0)
                throw new RuntimeException("Failed to Image::makeFromBitmap " + bitmap);
            return new M3Image(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(bitmap);
        }
    }

    /**
     * @deprecated - use {@link #makeRasterFromPixmap(Pixmap)}
     */
    @Deprecated
    public static M3Image makeFromPixmap(M3Pixmap pixmap) {
        return makeRasterFromPixmap(pixmap);
    }

    public static M3Image makeRasterFromPixmap(M3Pixmap pixmap) {
        try {
            assert pixmap != null : "Can’t makeFromPixmap with pixmap == null";
            M3Stats.onNativeCall();
            long ptr = _nMakeRasterFromPixmap(M3Native.getPtr(pixmap));
            if (ptr == 0)
                throw new RuntimeException("Failed to Image::makeFromRaster " + pixmap);
            return new M3Image(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(pixmap);
        }
    }

    /**
     * @deprecated - use {@link #makeDeferredFromEncodedBytes(byte[])}
     */
    @Deprecated
    public static M3Image makeFromEncoded(byte[] bytes) {
        return makeDeferredFromEncodedBytes(bytes);
    }

    public static M3Image makeDeferredFromEncodedBytes(byte[] bytes) {
        M3Stats.onNativeCall();
        long ptr = _nMakeDeferredFromEncodedBytes(bytes);
        if (ptr == 0)
            throw new IllegalArgumentException("Failed to Image::makeFromEncoded");
        return new M3Image(ptr);
    }

    /**
     * Returns a ImageInfo describing the width, height, color type, alpha type, and color space
     * of the Image.
     *
     * @return  image info of Image.
     */
    @Override
    public M3ImageInfo getImageInfo() {
        try {
            if (_imageInfo == null) {
                synchronized(this) {
                    if (_imageInfo == null) {
                        M3Stats.onNativeCall();
                        _imageInfo = _nGetImageInfo(_ptr);
                    }
                }
            }
            return _imageInfo;
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @deprecated - use {@link EncoderPNG#encode(Image)}, {@link EncoderJPEG#encode(Image)} or {@link EncoderWEBP#encode(Image)}
     */
    @Deprecated
    public M3Data encodeToData() {
        return encodeToData(M3EncodedImageFormat.PNG, 100);
    }

    /**
     * @deprecated - use {@link EncoderPNG#encode(Image)}, {@link EncoderJPEG#encode(Image)} or {@link EncoderWEBP#encode(Image)}
     */
    @Deprecated
    public M3Data encodeToData(M3EncodedImageFormat format) {
        return encodeToData(format, 100);
    }

    /**
     * @deprecated - use {@link EncoderPNG#encode(Image, EncodePNGOptions)}, {@link EncoderJPEG#encode(Image, EncodeJPEGOptions)} or {@link EncoderWEBP#encode(Image, EncodeWEBPOptions)}
     */
    @Deprecated
    public M3Data encodeToData(M3EncodedImageFormat format, int quality) {
        switch(format) {
        case PNG:
            return M3EncoderPNG.encode(this);
        case JPEG:
            return M3EncoderJPEG.encode(this, M3EncodeJPEGOptions.DEFAULT.withQuality(quality));
        case WEBP:
            return M3EncoderWEBP.encode(this, M3EncodeWEBPOptions.DEFAULT.withQuality(quality));
        default:
            throw new IllegalArgumentException("Format " + format + " is not supported");
        }
    }

    public M3Shader makeShader() {
        return makeShader(M3FilterTileMode.CLAMP, M3FilterTileMode.CLAMP, M3SamplingMode.DEFAULT, null);
    }

    public M3Shader makeShader(M3Matrix33 localMatrix) {
        return makeShader(M3FilterTileMode.CLAMP, M3FilterTileMode.CLAMP, M3SamplingMode.DEFAULT, localMatrix);
    }

    public M3Shader makeShader(M3FilterTileMode tm) {
        return makeShader(tm, tm, M3SamplingMode.DEFAULT, null);
    }

    public M3Shader makeShader(M3FilterTileMode tmx, M3FilterTileMode tmy) {
        return makeShader(tmx, tmy, M3SamplingMode.DEFAULT, null);
    }

    public M3Shader makeShader(M3FilterTileMode tmx,
                             M3FilterTileMode tmy,
                             M3Matrix33 localMatrix) {
        return makeShader(tmx, tmy, M3SamplingMode.DEFAULT, localMatrix);
    }

    public M3Shader makeShader(M3FilterTileMode tmx,
                             M3FilterTileMode tmy,
                             M3SamplingMode sampling,
                             M3Matrix33 localMatrix) {
        try {
            assert tmx != null : "Can’t Bitmap.makeShader with tmx == null";
            assert tmy != null : "Can’t Bitmap.makeShader with tmy == null";
            assert sampling != null : "Can’t Bitmap.makeShader with sampling == null";
            M3Stats.onNativeCall();
            return new M3Shader(_nMakeShader(_ptr, tmx.ordinal(), tmy.ordinal(), sampling._pack(), localMatrix == null ? null : localMatrix._mat));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * If pixel address is available, return ByteBuffer wrapping it.
     * If pixel address is not available, return null.
     *
     * @return  ByteBuffer with direct access to pixels, or null
     *
     * @see <a href="https://fiddle.skia.org/c/@Image_peekPixels">https://fiddle.skia.org/c/@Image_peekPixels</a>
     */
    public ByteBuffer peekPixels() {
        try {
            M3Stats.onNativeCall();
            return _nPeekPixels(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public boolean peekPixels(M3Pixmap pixmap) {
        try {
            M3Stats.onNativeCall();
            return _nPeekPixelsToPixmap(_ptr, M3Native.getPtr(pixmap));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(pixmap);
        }
    }

    public boolean readPixels(M3Bitmap dst) {
        return readPixels(null, dst, 0, 0, false);
    }

    public boolean readPixels(M3Bitmap dst, int srcX, int srcY) {
        return readPixels(null, dst, srcX, srcY, false);
    }

    /**
     * <p>Copies Rect of pixels from Image to Bitmap. Copy starts at offset (srcX, srcY),
     * and does not exceed Image (getWidth(), getHeight()).</p>
     *
     * <p>dst specifies width, height, ColorType, AlphaType, and ColorSpace of destination.</p>
     *
     * <p>Returns true if pixels are copied. Returns false if:</p>
     * <ul>
     * <li>dst has no pixels allocated.</li>
     * </ul>
     *
     * <p>Pixels are copied only if pixel conversion is possible. If Image ColorType is
     * ColorType.GRAY_8, or ColorType.ALPHA_8; dst.getColorType() must match.
     * If Image ColorType is ColorType.GRAY_8, dst.getColorSpace() must match.
     * If Image AlphaType is AlphaType.OPAQUE, dst.getAlphaType() must
     * match. If Image ColorSpace is null, dst.getColorSpace() must match. Returns
     * false if pixel conversion is not possible.</p>
     *
     * <p>srcX and srcY may be negative to copy only top or left of source. Returns
     * false if getWidth() or getHeight() is zero or negative.</p>
     *
     * <p>Returns false if abs(srcX) &gt;= Image.getWidth(), or if abs(srcY) &gt;= Image.getHeight().</p>
     *
     * <p>If cache is true, pixels may be retained locally, otherwise pixels are not added to the local cache.</p>
     *
     * @param context the DirectContext in play, if it exists
     * @param dst     destination bitmap
     * @param srcX    column index whose absolute value is less than getWidth()
     * @param srcY    row index whose absolute value is less than getHeight()
     * @param cache   whether the pixels should be cached locally
     * @return        true if pixels are copied to dstPixels
     */
    public boolean readPixels(M3DirectContext context, M3Bitmap dst, int srcX, int srcY, boolean cache) {
        try {
            assert dst != null : "Can’t readPixels with dst == null";
            return _nReadPixelsBitmap(_ptr, M3Native.getPtr(context), M3Native.getPtr(dst), srcX, srcY, cache);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(context);
            M3ReferenceUtil.reachabilityFence(dst);
        }
    }

    public boolean readPixels(M3Pixmap dst, int srcX, int srcY, boolean cache) {
        try {
            assert dst != null : "Can’t readPixels with dst == null";
            return _nReadPixelsPixmap(_ptr, M3Native.getPtr(dst), srcX, srcY, cache);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(dst);
        }
    }

    public boolean scalePixels(M3Pixmap dst, M3SamplingMode samplingMode, boolean cache) {
        try {
            assert dst != null : "Can't scalePixels with dst == null";
            return _nScalePixels(_ptr, M3Native.getPtr(dst), samplingMode._pack(), cache);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(dst);
        }
    }

    /**
     * Create a new image by copying this image and scaling to fit the
     * ImageInfo's dimensions and converting the pixels into the ImageInfo's
     * ColorInfo. This is done retaining the domain (backend) of the image
     * (e.g. gpu, raster)
     *
     * @param   imageInfo    New image dimensions
     * @param   samplingMode sampling mode to use
     * @return  null if the requested ColorInfo is not supported, its dimesions
     *          are out of range
     */
    public M3Image makeScaled(M3ImageInfo imageInfo, M3SamplingMode samplingMode) {
        try {
            long ptr = _nMakeScaled(_ptr,
                                    imageInfo._width,
                                    imageInfo._height,
                                    imageInfo._colorInfo._colorType.ordinal(),
                                    imageInfo._colorInfo._alphaType.ordinal(),
                                    M3Native.getPtr(imageInfo._colorInfo._colorSpace),
                                    samplingMode._pack());
            return ptr == 0 ? null : new M3Image(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(imageInfo);
        }
    }

    /**
     * Returns an Image that is made from subset of this Image.
     *
     * @param subset bounds of Image to extract
     * @return       subset Image or null
     */
    public M3Image makeSubset(M3IRect subset) {
        return makeSubset(null, subset);
    }

    /**
     * Returns an Image that is made from subset of this Image.
     *
     * @param context the DirectContext in play - if it exists
     * @param subset  bounds of Image to extract
     * @return        subset Image or null
     */
    public M3Image makeSubset(M3DirectContext context, M3IRect subset) {
        try {
            assert subset != null : "Can't makeSubset with subset == null";
            M3Stats.onNativeCall();
            long ptr = _nMakeSubset(_ptr, M3Native.getPtr(context), subset._left, subset._top, subset._right, subset._bottom);
            return ptr == 0 ? null : new M3Image(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(context);
            M3ReferenceUtil.reachabilityFence(subset);
        }
    }

    /**
     * <p>Creates a filtered Image on the CPU. The filter processes the src image, potentially changing
     * the color, position, and size. The subset parameter defines the bounds of src that are processed
     * by the filter. The clipBounds parameter specifies the expected bounds of the filtered Image.
     * The result includes the actual bounds of the filtered Image and a translation offset.</p>
     *
     * <p>Returns null if a filtered result could not be created.</p>
     *
     * <p>Useful for animation of ImageFilter that varies size from frame to frame.
     * The returned subset describes the valid bounds of the filtered image. The offset translates
     * the returned Image to keep subsequent animation frames aligned with respect to each other.</p>
     *
     * @param filter       the image filter to be applied
     * @param subset       bounds of Image processed by filter
     * @param clipBounds   expected bounds of filtered Image
     * @return             ImageWithFilterResult containing the filtered Image (or null), actual bounds, and offset
     */
    public static M3ImageWithFilterResult makeWithFilter(M3ImageFilter filter,
                                                       M3Image src,
                                                       M3IRect subset,
                                                       M3IRect clipBounds) {
        try {
            assert filter != null : "Can't makeWithFilter with filter == null";
            assert src != null : "Can't makeWithFilter with src == null";
            assert subset != null : "Can't makeWithFilter with subset == null";
            assert clipBounds != null : "Can't makeWithFilter with clipBounds == null";
            M3Stats.onNativeCall();
            return _nMakeWithFilter(M3Native.getPtr(src),
                                    M3Native.getPtr(filter),
                                    subset._left, subset._top, subset._right, subset._bottom,
                                    clipBounds._left, clipBounds._top, clipBounds._right, clipBounds._bottom);
        } finally {
            M3ReferenceUtil.reachabilityFence(filter);
            M3ReferenceUtil.reachabilityFence(src);
        }
    }

    /**
     * <p>Creates a filtered Image on the GPU. The filter processes the src image, potentially changing
     * color, position, and size. The subset parameter defines the bounds of src that are processed
     * by the filter. The clipBounds parameter specifies the expected bounds of the filtered Image.
     * The result includes the actual bounds of the filtered Image and a translation offset.</p>
     *
     * <p>Returns null if Image could not be created or if the recording context provided doesn't
     * match the GPU context in which the image was created.</p>
     *
     * <p>Useful for animation of ImageFilter that varies size from frame to frame.
     * Returned Image is created larger than required by filter so that GPU texture
     * can be reused with different sized effects. The returned subset describes the valid bounds
     * of GPU texture returned. The offset translates the returned Image to keep subsequent
     * animation frames aligned with respect to each other.</p>
     *
     * @param context      the DirectContext in play - if it exists
     * @param filter       the image filter to be applied
     * @param subset       bounds of Image processed by filter
     * @param clipBounds   expected bounds of filtered Image
     * @return             ImageWithFilterResult containing the filtered Image (or null), actual bounds, and offset
     *
     * @see ImageWithFilterResult
     */
    public static M3ImageWithFilterResult makeWithFilter(M3DirectContext context,
                                                       M3ImageFilter filter,
                                                       M3Image src,
                                                       M3IRect subset,
                                                       M3IRect clipBounds) {
        try {
            assert context != null : "Can't makeWithFilter with context == null";
            assert filter != null : "Can't makeWithFilter with filter == null";
            assert src != null : "Can't makeWithFilter with src == null";
            assert subset != null : "Can't makeWithFilter with subset == null";
            assert clipBounds != null : "Can't makeWithFilter with clipBounds == null";
            M3Stats.onNativeCall();
            return _nMakeWithFilterContext(M3Native.getPtr(context),
                                           M3Native.getPtr(src),
                                           M3Native.getPtr(filter),
                                           subset._left, subset._top, subset._right, subset._bottom,
                                           clipBounds._left, clipBounds._top, clipBounds._right, clipBounds._bottom);
        } finally {
            M3ReferenceUtil.reachabilityFence(context);
            M3ReferenceUtil.reachabilityFence(filter);
            M3ReferenceUtil.reachabilityFence(src);
        }
    }

    public static native long _nAdoptTextureFrom(long contextPtr, long backendTexturePtr, int surfaceOrigin, int colorType);
    public static native long _nBorrowTextureFrom(long contextPtr, long backendTexturePtr, int surfaceOrigin, int colorType, int alphaType, long colorSpacePtr, Runnable releaseContextPtr);
    public static native long _nMakeRasterFromBytes(int width, int height, int colorType, int alphaType, long colorSpacePtr, byte[] pixels, long rowBytes);
    public static native long _nMakeRasterFromData(int width, int height, int colorType, int alphaType, long colorSpacePtr, long dataPtr, long rowBytes);
    public static native long _nMakeRasterFromBitmap(long bitmapPtr);
    public static native long _nMakeRasterFromPixmap(long pixmapPtr);
    public static native long _nMakeDeferredFromEncodedBytes(byte[] bytes);
    public static native M3ImageInfo _nGetImageInfo(long ptr);
    public static native long    _nMakeShader(long ptr, int tmx, int tmy, long samplingMode, float[] localMatrix);
    public static native ByteBuffer _nPeekPixels(long ptr);
    public static native boolean _nPeekPixelsToPixmap(long ptr, long pixmapPtr);
    public static native boolean _nScalePixels(long ptr, long pixmapPtr, long samplingOptions, boolean cache);
    public static native long    _nMakeScaled(long ptr, int width, int height, int colorType, int alphaType, long colorSpacePtr, long samplingOptions);
    public static native long    _nMakeSubset(long ptr, long contextPtr, int subsetL, int subsetT, int subsetR, int subsetB);
    public static native boolean _nReadPixelsBitmap(long ptr, long contextPtr, long bitmapPtr, int srcX, int srcY, boolean cache);
    public static native boolean _nReadPixelsPixmap(long ptr, long pixmapPtr, int srcX, int srcY, boolean cache);
    public static native M3ImageWithFilterResult _nMakeWithFilter(long srcPtr, long filterPtr, int subsetL, int subsetT, int subsetR, int subsetB, int clipBoundsL, int clipBoundsT, int clipBoundsR, int clipBoundsB);
    public static native M3ImageWithFilterResult _nMakeWithFilterContext(long contextPtr, long srcPtr, long filterPtr, int subsetL, int subsetT, int subsetR, int subsetB, int clipBoundsL, int clipBoundsT, int clipBoundsR, int clipBoundsB);
}
