package jdk.m3.graphics;

import java.nio.*;
import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.*;

public class M3Bitmap extends M3Managed implements M3IHasImageInfo {
    static { M3Library.staticLoad(); }

    public M3ImageInfo _imageInfo = null;
    public M3Bitmap(long ptr) {
        super(ptr, _FinalizerHolder.PTR);
    }

    /**
     * Creates an empty Bitmap without pixels, with {@link ColorType#UNKNOWN},
     * {@link ColorAlphaType#UNKNOWN}, and with a width and height of zero.
     * PixelRef origin is set to (0, 0). Bitmap is not volatile.
     *
     * Use {@link #setImageInfo(ImageInfo, long)} to associate ColorType, ColorAlphaType, width, and height after Bitmap has been created.
     *
     * @see <a href="https://fiddle.skia.org/c/@Bitmap_empty_constructor">https://fiddle.skia.org/c/@Bitmap_empty_constructor</a>
     */
    public M3Bitmap() {
        this(_nMake());
        M3Stats.onNativeCall();
    }

    /**
     * Copies settings from src to returned Bitmap. Shares pixels if src has pixels
     * allocated, so both bitmaps reference the same pixels.
     *
     * @return  copy of src
     *
     * @see <a href="https://fiddle.skia.org/c/@Bitmap_copy_const_SkBitmap">https://fiddle.skia.org/c/@Bitmap_copy_const_SkBitmap</a>
     */
    public M3Bitmap makeClone() {
        try {
            M3Stats.onNativeCall();
            return new M3Bitmap(_nMakeClone(_ptr));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public static M3Bitmap makeFromImage(M3Image image) {
        assert image != null : "Can’t makeFromImage with image == null";
        M3Bitmap bitmap = new M3Bitmap();
        bitmap.allocPixels(image.getImageInfo());
        if (image.readPixels(bitmap))
            return bitmap;
        else {
            bitmap.close();
            throw new RuntimeException("Failed to readPixels from " + image);
        }
    }

    /**
     * Swaps the fields of the two bitmaps.
     *
     * @param other  Bitmap exchanged with original
     *
     * @see <a href="https://fiddle.skia.org/c/@Bitmap_swap">https://fiddle.skia.org/c/@Bitmap_swap</a>
     */
    public void swap(M3Bitmap other) {
        M3Stats.onNativeCall();
        _nSwap(_ptr, M3Native.getPtr(other));
        _imageInfo = null;
        other._imageInfo = null;
        M3ReferenceUtil.reachabilityFence(this);
        M3ReferenceUtil.reachabilityFence(other);
    }

    @Override
    public M3ImageInfo getImageInfo() {
        try {
            if (_imageInfo == null) {
                M3Stats.onNativeCall();
                _imageInfo = _nGetImageInfo(_ptr);
            }
            return _imageInfo;
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }


    /**
     * Returns number of pixels that fit on row. Should be greater than or equal to
     * getWidth().
     *
     * @return  maximum pixels per row
     */
    public int getRowBytesAsPixels() {
        try {
            M3Stats.onNativeCall();
            return _nGetRowBytesAsPixels(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns true if PixelRef is null.
     *
     * Does not check if width or height are zero; call {@link #drawsNothing()}
     * to check width, height, and PixelRef.
     *
     * @return  true if no PixelRef is associated
     */
    public boolean isNull() {
        try {
            M3Stats.onNativeCall();
            return _nIsNull(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns true if width or height are zero, or if PixelRef is null.
     * If true, Bitmap has no effect when drawn or drawn into.
     *
     * @return  true if drawing has no effect
     */
    public boolean drawsNothing() {
        return isEmpty() || isNull();
    }

    /**
     * Returns row bytes, the interval from one pixel row to the next. Row bytes
     * is at least as large as: getWidth() * getBytesPerPixel().
     *
     * Returns zero if getColorType() is {@link ColorType#UNKNOWN}, or if row bytes
     * supplied to {@link #setImageInfo(ImageInfo)} is not large enough to hold a row of pixels.
     *
     * @return  byte length of pixel row
     */
    public long getRowBytes() {
        try {
            M3Stats.onNativeCall();
            return _nGetRowBytes(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Sets alpha type, if argument is compatible with current color type.
     * Returns true unless argument is {@link ColorAlphaType#UNKNOWN} and current
     * value is {@link ColorAlphaType#UNKNOWN}.</p>
     *
     * <p>Returns true if current color type is {@link ColorType#UNKNOWN}.
     * Argument is ignored, and alpha type remains {@link ColorAlphaType#UNKNOWN}.</p>
     *
     * <p>Returns true if current color type is {@link ColorType#RGB_565} or
     * {@link ColorType#GRAY_8}. Argument is ignored, and alpha type remains
     * {@link ColorAlphaType#OPAQUE}.</p>
     *
     * <p>If current color type is {@link ColorType#ARGB_4444}, {@link ColorType#RGBA_8888},
     * {@link ColorType#BGRA_8888}, or {@link ColorType#RGBA_F16}: returns true unless
     * argument is {@link ColorAlphaType#UNKNOWN} and current alpha type is not
     * {@link ColorAlphaType#UNKNOWN}. If current alpha type is
     * {@link ColorAlphaType#UNKNOWN}, argument is ignored.</p>
     *
     * <p>If current color type is {@link ColorType#ALPHA_8}, returns true unless
     * argument is {@link ColorAlphaType#UNKNOWN} and current alpha type is not
     * {@link ColorAlphaType#UNKNOWN}. If current alpha type is
     * {@link ColorAlphaType#UNKNOWN}, argument is ignored. If argument is
     * {@link ColorAlphaType#UNPREMUL}, it is treated as {@link ColorAlphaType#PREMUL}.</p>
     *
     * <p>This changes alpha type in PixelRef; all bitmaps sharing PixelRef
     * are affected.</p>
     *
     * @return  true if alpha type is set
     *
     * @see <a href="https://fiddle.skia.org/c/@Bitmap_setAlphaType">https://fiddle.skia.org/c/@Bitmap_setAlphaType</a>
     */
    public boolean setAlphaType(M3ColorAlphaType alphaType) {
        try {
            M3Stats.onNativeCall();
            _imageInfo = null;
            return _nSetAlphaType(_ptr, alphaType.ordinal());
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Sets the {@link ColorSpace} associated with this {@link Bitmap}.</p>
     *
     * <p>The raw pixel data is not altered by this call; no conversion is
     * performed.</p>
     *
     * <p>This changes {@link ColorSpace} in {@link PixelRef}; all bitmaps
     * sharing {@link PixelRef} are affected.
     *
     * @param colorSpace  {@link ColorSpace} to set
     * @return            this
     */
    public M3Bitmap setColorSpace(M3ColorSpace colorSpace) {
        try {
            M3Stats.onNativeCall();
            _imageInfo = null;
            _nSetColorSpace(_ptr, M3Native.getPtr(colorSpace));
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(colorSpace);
        }
    }

    /**
     * Returns minimum memory required for pixel storage.
     * Does not include unused memory on last row when getRowBytesAsPixels() exceeds getWidth().
     * Returns zero if height() or width() is 0.
     * Returns getHeight() times getRowBytes() if getColorType() is {@link ColorType#UNKNOWN}.
     *
     * @return  size in bytes of image buffer
     */
    public long computeByteSize() {
        try {
            M3Stats.onNativeCall();
            return _nComputeByteSize(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Returns true if pixels can not change.</p>
     *
     * <p>Most immutable Bitmap checks trigger an assert only on debug builds.</p>
     *
     * @return  true if pixels are immutable
     *
     * @see <a href="https://fiddle.skia.org/c/@Bitmap_isImmutable">https://fiddle.skia.org/c/@Bitmap_isImmutable</a>
     */
    public boolean isImmutable() {
        try {
            M3Stats.onNativeCall();
            return _nIsImmutable(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Sets internal flag to mark Bitmap as immutable. Once set, pixels can not change.
     * Any other bitmap sharing the same PixelRef are also marked as immutable.
     * Once PixelRef is marked immutable, the setting cannot be cleared.</p>
     *
     * <p>Writing to immutable Bitmap pixels triggers an assert on debug builds.</p>
     *
     * @see <a href="https://fiddle.skia.org/c/@Bitmap_setImmutable">https://fiddle.skia.org/c/@Bitmap_setImmutable</a>
     */
    public M3Bitmap setImmutable() {
        M3Stats.onNativeCall();
        _nSetImmutable(_ptr);
        return this;
    }

    /**
     * <p>Resets to its initial state; all fields are set to zero, as if Bitmap had
     * been initialized by Bitmap().</p>
     *
     * <p>Sets width, height, row bytes to zero; pixel address to nullptr; ColorType to
     * {@link ColorType#UNKNOWN}; and ColorAlphaType to {@link ColorAlphaType#UNKNOWN}.</p>
     *
     * <p>If PixelRef is allocated, its reference count is decreased by one, releasing
     * its memory if Bitmap is the sole owner.</p>
     *
     * @see <a href="https://fiddle.skia.org/c/@Bitmap_reset">https://fiddle.skia.org/c/@Bitmap_reset</a>
     */
    public M3Bitmap reset() {
        M3Stats.onNativeCall();
        _imageInfo = null;
        _nReset(_ptr);
        return this;
    }

    /**
     * <p>Returns true if all pixels are opaque. ColorType determines how pixels
     * are encoded, and whether pixel describes alpha. Returns true for ColorType
     * without alpha in each pixel; for other ColorType, returns true if all
     * pixels have alpha values equivalent to 1.0 or greater.</p>
     *
     * <p>For {@link ColorType#RGB_565} or {@link ColorType#GRAY_8}: always
     * returns true. For {@link ColorType#ALPHA_8}, {@link ColorType#BGRA_8888},
     * {@link ColorType#RGBA_8888}: returns true if all pixel alpha values are 255.
     * For {@link ColorType#ARGB_4444}: returns true if all pixel alpha values are 15.
     * For {@link ColorType#RGBA_F16}: returns true if all pixel alpha values are 1.0 or
     * greater.</p>
     *
     * <p>Returns false for {@link ColorType#UNKNOWN}.
     *
     * @return    true if all pixels have opaque values or ColorType is opaque
     */
    public boolean computeIsOpaque() {
        try {
            M3Stats.onNativeCall();
            return _nComputeIsOpaque(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns IRect { 0, 0, width(), height() }.
     *
     * @see <a href="https://fiddle.skia.org/c/@Bitmap_getBounds_2">https://fiddle.skia.org/c/@Bitmap_getBounds_2</a>
     */
    public M3IRect getBounds() {
        return M3IRect.makeXYWH(0, 0, getWidth(), getHeight());
    }

    /**
     * Returns the bounds of this bitmap, offset by its PixelRef origin.
     */
    public M3IRect getSubset() {
        M3IPoint origin = getPixelRefOrigin();
        return M3IRect.makeXYWH(origin.getX(), origin.getY(), getWidth(), getHeight());
    }

    /**
     * <p>Sets width, height, ColorAlphaType, ColorType, ColorSpace.
     * Frees pixels, and returns true if successful.</p>
     *
     * <p>imageInfo.getAlphaType() may be altered to a value permitted by imageInfo.getColorSpace().
     * If imageInfo.getColorType() is {@link ColorType#UNKNOWN}, imageInfo.getAlphaType() is
     * set to {@link ColorAlphaType#UNKNOWN}.
     * If imageInfo.colorType() is {@link ColorType#ALPHA_8} and imageInfo.getAlphaType() is
     * {@link ColorAlphaType#UNPREMUL}, imageInfo.getAlphaType() is replaced by {@link ColorAlphaType#PREMUL}.
     * If imageInfo.colorType() is {@link ColorType#RGB_565} or {@link ColorType#GRAY_8},
     * imageInfo.getAlphaType() is set to {@link ColorAlphaType#OPAQUE}.
     * If imageInfo.colorType() is {@link ColorType#ARGB_4444}, {@link ColorType#RGBA_8888},
     * {@link ColorType#BGRA_8888}, or {@link ColorType#RGBA_F16}: imageInfo.getAlphaType() remains
     * unchanged.</p>
     *
     * <p>Calls reset() and returns false if:
     * - rowBytes exceeds 31 bits
     * - imageInfo.getWidth() is negative
     * - imageInfo.getHeight() is negative
     * - rowBytes is positive and less than imageInfo.getWidth() times imageInfo.getBytesPerPixel()</p>
     *
     * @param imageInfo  contains width, height, AlphaType, ColorType, ColorSpace
     * @return           true if ImageInfo set successfully
     *
     * @see <a href="https://fiddle.skia.org/c/@Bitmap_setInfo">https://fiddle.skia.org/c/@Bitmap_setInfo</a>
     */
    public boolean setImageInfo(M3ImageInfo imageInfo) {
        _imageInfo = null;
        return setImageInfo(imageInfo, 0);
    }

    /**
     * <p>Sets width, height, ColorAlphaType, ColorType, ColorSpace, and optional
     * rowBytes. Frees pixels, and returns true if successful.</p>
     *
     * <p>imageInfo.getAlphaType() may be altered to a value permitted by imageInfo.getColorSpace().
     * If imageInfo.getColorType() is {@link ColorType#UNKNOWN}, imageInfo.getAlphaType() is
     * set to {@link ColorAlphaType#UNKNOWN}.
     * If imageInfo.colorType() is {@link ColorType#ALPHA_8} and imageInfo.getAlphaType() is
     * {@link ColorAlphaType#UNPREMUL}, imageInfo.getAlphaType() is replaced by {@link ColorAlphaType#PREMUL}.
     * If imageInfo.colorType() is {@link ColorType#RGB_565} or {@link ColorType#GRAY_8},
     * imageInfo.getAlphaType() is set to {@link ColorAlphaType#OPAQUE}.
     * If imageInfo.colorType() is {@link ColorType#ARGB_4444}, {@link ColorType#RGBA_8888},
     * {@link ColorType#BGRA_8888}, or {@link ColorType#RGBA_F16}: imageInfo.getAlphaType() remains
     * unchanged.</p>
     *
     * <p>rowBytes must equal or exceed imageInfo.getMinRowBytes(). If imageInfo.getColorSpace() is
     * {@link ColorType#UNKNOWN}, rowBytes is ignored and treated as zero; for all other
     * ColorSpace values, rowBytes of zero is treated as imageInfo.getMinRowBytes().</p>
     *
     * <p>Calls reset() and returns false if:
     * - rowBytes exceeds 31 bits
     * - imageInfo.getWidth() is negative
     * - imageInfo.getHeight() is negative
     * - rowBytes is positive and less than imageInfo.getWidth() times imageInfo.getBytesPerPixel()</p>
     *
     * @param imageInfo  contains width, height, AlphaType, ColorType, ColorSpace
     * @param rowBytes   imageInfo.getMinRowBytes() or larger; or zero
     * @return           true if ImageInfo set successfully
     *
     * @see <a href="https://fiddle.skia.org/c/@Bitmap_setInfo">https://fiddle.skia.org/c/@Bitmap_setInfo</a>
     */
    public boolean setImageInfo(M3ImageInfo imageInfo, long rowBytes) {
        try {
            _imageInfo = null;
            M3Stats.onNativeCall();
            return _nSetImageInfo(_ptr,
                imageInfo._width,
                imageInfo._height,
                imageInfo._colorInfo._colorType.ordinal(),
                imageInfo._colorInfo._alphaType.ordinal(),
                M3Native.getPtr(imageInfo._colorInfo._colorSpace),
                rowBytes);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(imageInfo._colorInfo._colorSpace);
        }
    }

    /**
     * <p>Sets ImageInfo to info following the rules in setImageInfo() and allocates pixel
     * memory. Memory is zeroed.</p>
     *
     * <p>Returns false and calls reset() if ImageInfo could not be set, or memory could
     * not be allocated, or memory could not optionally be zeroed.</p>
     *
     * <p>On most platforms, allocating pixel memory may succeed even though there is
     * not sufficient memory to hold pixels; allocation does not take place
     * until the pixels are written to. The actual behavior depends on the platform
     * implementation of calloc().</p>
     *
     * @param imageInfo   contains width, height, ColorAlphaType, ColorType, ColorSpace
     * @param zeroPixels  whether pixels should be zeroed
     * @return            true if pixels allocation is successful
     */
    public boolean allocPixelsFlags(M3ImageInfo imageInfo, boolean zeroPixels) {
        try {
            _imageInfo = null;
            M3Stats.onNativeCall();
            return _nAllocPixelsFlags(_ptr,
                imageInfo._width,
                imageInfo._height,
                imageInfo._colorInfo._colorType.ordinal(),
                imageInfo._colorInfo._alphaType.ordinal(),
                M3Native.getPtr(imageInfo._colorInfo._colorSpace),
                zeroPixels ? 1 : 0);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(imageInfo._colorInfo._colorSpace);
        }
    }

   /**
    * <p>Sets ImageInfo to info following the rules in setImageInfo() and allocates pixel
    * memory. rowBytes must equal or exceed info.width() times info.bytesPerPixel(),
    * or equal zero.</p>
    *
    * <p>Returns false and calls reset() if ImageInfo could not be set, or memory could
    * not be allocated.</p>
    *
    * <p>On most platforms, allocating pixel memory may succeed even though there is
    * not sufficient memory to hold pixels; allocation does not take place
    * until the pixels are written to. The actual behavior depends on the platform
    * implementation of malloc().</p>
    *
    * @param info      contains width, height, ColorAlphaType, ColorType, ColorSpace
    * @param rowBytes  size of pixel row or larger; may be zero
    * @return          true if pixel storage is allocated
    */
    public boolean allocPixels(M3ImageInfo info, long rowBytes) {
        try {
            _imageInfo = null;
            M3Stats.onNativeCall();
            return _nAllocPixelsRowBytes(_ptr,
                info._width,
                info._height,
                info._colorInfo._colorType.ordinal(),
                info._colorInfo._alphaType.ordinal(),
                M3Native.getPtr(info._colorInfo._colorSpace),
                rowBytes);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(info._colorInfo._colorSpace);
        }
    }

    /**
     * <p>Sets ImageInfo to info following the rules in {@link #setImageInfo(ImageInfo, long)} and allocates pixel
     * memory.</p>
     *
     * <p>Returns false and calls reset() if ImageInfo could not be set, or memory could
     * not be allocated.</p>
     *
     * <p>On most platforms, allocating pixel memory may succeed even though there is
     * not sufficient memory to hold pixels; allocation does not take place
     * until the pixels are written to. The actual behavior depends on the platform
     * implementation of malloc().</p>
     *
     * @param imageInfo  contains width, height, ColorAlphaType, ColorType, ColorSpace
     * @return           true if pixel storage is allocated
     * @see <a href="https://fiddle.skia.org/c/@Bitmap_allocPixels_2">https://fiddle.skia.org/c/@Bitmap_allocPixels_2</a>
     */
    public boolean allocPixels(M3ImageInfo imageInfo) {
        return allocPixels(imageInfo, imageInfo.getMinRowBytes());
    }

    /**
     * Sets ImageInfo to width, height, and native color type; and allocates
     * pixel memory. Sets ImageInfo to {@link ColorAlphaType#PREMUL}.
     *
     * Calls reset() and returns false if width exceeds 29 bits or is negative,
     * or height is negative.
     *
     * Returns false if allocation fails.
     *
     * Use to create Bitmap that matches the native pixel arrangement on
     * the platform. Bitmap drawn to output device skips converting its pixel format.
     *
     * @param width   pixel column count; must be zero or greater
     * @param height  pixel row count; must be zero or greater
     * @return        true if pixel storage is allocated
     */
    public boolean allocN32Pixels(int width, int height) {
        return allocN32Pixels(width, height, false);
    }

    /**
     * Sets ImageInfo to width, height, and native color type; and allocates
     * pixel memory. If opaque is true, sets ImageInfo to {@link ColorAlphaType#OPAQUE};
     * otherwise, sets to {@link ColorAlphaType#PREMUL}.
     *
     * Calls reset() and returns false if width exceeds 29 bits or is negative,
     * or height is negative.
     *
     * Returns false if allocation fails.
     *
     * Use to create Bitmap that matches the native pixel arrangement on
     * the platform. Bitmap drawn to output device skips converting its pixel format.
     *
     * @param width   pixel column count; must be zero or greater
     * @param height  pixel row count; must be zero or greater
     * @param opaque  true if pixels do not have transparency
     * @return        true if pixel storage is allocated
     */
    public boolean allocN32Pixels(int width, int height, boolean opaque) {
        return allocPixels(M3ImageInfo.makeN32(width, height, opaque ? M3ColorAlphaType.OPAQUE : M3ColorAlphaType.PREMUL));
    }

    public boolean installPixels(byte[] pixels) {
        return installPixels(getImageInfo(), pixels, getRowBytes());
    }

    /**
     * <p>Sets ImageInfo to info following the rules in setImageInfo(), and creates PixelRef
     * containing pixels and rowBytes.</p>
     *
     * <p>If ImageInfo could not be set, or rowBytes is less than info.getMinRowBytes():
     * calls reset(), and returns false.</p>
     *
     * @param info     contains width, height, SkAlphaType, SkColorType, SkColorSpace
     * @param pixels   pixel storage
     * @param rowBytes size of pixel row or larger
     * @return         true if ImageInfo is set to info
     */
    public boolean installPixels(M3ImageInfo info,
                                 byte[] pixels,
                                 long rowBytes) {
        try {
            _imageInfo = null;
            M3Stats.onNativeCall();
            return _nInstallPixels(_ptr,
                info._width,
                info._height,
                info._colorInfo._colorType.ordinal(),
                info._colorInfo._alphaType.ordinal(),
                M3Native.getPtr(info._colorInfo._colorSpace),
                pixels,
                rowBytes);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(info._colorInfo._colorSpace);
        }
    }

    /**
     * <p>Allocates pixel memory with HeapAllocator, and replaces existing PixelRef.
     * The allocation size is determined by ImageInfo width, height, and ColorType.</p>
     *
     * <p>Returns false if info().colorType() is {@link ColorType#UNKNOWN}, or allocation fails.</p>
     *
     * @return  true if the allocation succeeds
     */
    public boolean allocPixels() {
        try {
            _imageInfo = null;
            M3Stats.onNativeCall();
            return _nAllocPixels(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns PixelRef, which contains: pixel base address; its dimensions; and
     * rowBytes(), the interval from one row to the next.
     * PixelRef may be shared by multiple bitmaps.
     * If PixelRef has not been set, returns null.
     *
     * @return  SkPixelRef, or null
     */
    public M3PixelRef getPixelRef() {
        try {
            M3Stats.onNativeCall();
            long res = _nGetPixelRef(_ptr);
            return res == 0 ? null : new M3PixelRef(res);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Returns origin of pixels within PixelRef. Bitmap bounds is always contained
     * by PixelRef bounds, which may be the same size or larger. Multiple Bitmap
     * can share the same PixelRef, where each Bitmap has different bounds.</p>
     *
     * <p>The returned origin added to Bitmap dimensions equals or is smaller than the
     * PixelRef dimensions.</p>
     *
     * <p>Returns (0, 0) if PixelRef is nullptr.</p>
     *
     * @return  pixel origin within PixelRef
     *
     * @see <a href="https://fiddle.skia.org/c/@Bitmap_pixelRefOrigin">https://fiddle.skia.org/c/@Bitmap_pixelRefOrigin</a>
     */
    public M3IPoint getPixelRefOrigin() {
        try {
            M3Stats.onNativeCall();
            long res = _nGetPixelRefOrigin(_ptr);
            return new M3IPoint((int) (res & 0xFFFFFFFF), (int) (res >>> 32));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Replaces pixelRef and origin in Bitmap. dx and dy specify the offset
     * within the PixelRef pixels for the top-left corner of the bitmap.</p>
     *
     * <p>Asserts in debug builds if dx or dy are out of range. Pins dx and dy
     * to legal range in release builds.</p>
     *
     * <p>The caller is responsible for ensuring that the pixels match the
     * ColorType and ColorAlphaType in ImageInfo.</p>
     *
     * @param pixelRef  PixelRef describing pixel address and rowBytes()
     * @param dx        column offset in PixelRef for bitmap origin
     * @param dy        row offset in PixelRef for bitmap origin
     *
     * @see <a href="https://fiddle.skia.org/c/@Bitmap_setPixelRef">https://fiddle.skia.org/c/@Bitmap_setPixelRef</a>
     */
    public M3Bitmap setPixelRef(M3PixelRef pixelRef, int dx, int dy) {
        try {
            _imageInfo = null;
            M3Stats.onNativeCall();
            _nSetPixelRef(_ptr, M3Native.getPtr(pixelRef), dx, dy);
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(pixelRef);
        }
    }

    /**
     * Returns true if Bitmap can be drawn.
     *
     * @return  true if getPixels() is not null
     */
    public boolean isReadyToDraw() {
        try {
            M3Stats.onNativeCall();
            return _nIsReadyToDraw(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Returns a unique value corresponding to the pixels in PixelRef.
     * Returns a different value after notifyPixelsChanged() has been called.
     * Returns zero if PixelRef is null.</p>
     *
     * <p>Determines if pixels have changed since last examined.</p>
     *
     * @return  unique value for pixels in PixelRef
     *
     * @see <a href="https://fiddle.skia.org/c/@Bitmap_getGenerationID">https://fiddle.skia.org/c/@Bitmap_getGenerationID</a>
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
     * <p>Marks that pixels in PixelRef have changed. Subsequent calls to
     * getGenerationId() return a different value.</p>
     *
     * @see <a href="https://fiddle.skia.org/c/@Bitmap_notifyPixelsChanged">https://fiddle.skia.org/c/@Bitmap_notifyPixelsChanged</a>
     */
    public M3Bitmap notifyPixelsChanged() {
        try {
            M3Stats.onNativeCall();
            _nNotifyPixelsChanged(_ptr);
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Replaces pixel values with color, interpreted as being in the sRGB.
     * All pixels contained by getBounds() are affected. If the getColorType() is
     * {@link ColorType#GRAY_8} or {@link ColorType#RGB_565}, then alpha is ignored; RGB is
     * treated as opaque. If getColorType() is {@link ColorType#ALPHA_8}, then RGB is ignored.
     *
     * @param color      unpremultiplied color
     * @return           this
     *
     * @see <a href="https://fiddle.skia.org/c/@Bitmap_eraseColor">https://fiddle.skia.org/c/@Bitmap_eraseColor</a>
     */
    public M3Bitmap erase(M3Color4f color) {
        try {
            assert color != null : "Bitmap::erase expected color != null";
            M3Stats.onNativeCall();
            _nErase4f(_ptr, color._r, color._g, color._b, color._a);
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Replaces pixel values with color, interpreted as being in the sRGB.
     * All pixels contained by getBounds() are affected. If the getColorType() is
     * {@link ColorType#GRAY_8} or {@link ColorType#RGB_565}, then alpha is ignored; RGB is
     * treated as opaque. If getColorType() is {@link ColorType#ALPHA_8}, then RGB is ignored.
     *
     * @param color      unpremultiplied color
     * @param area       rectangle to fill
     * @return           this
     *
     * @see <a href="https://fiddle.skia.org/c/@Bitmap_erase">https://fiddle.skia.org/c/@Bitmap_erase</a>
     */
    public M3Bitmap erase(M3Color4f color, M3IRect area) {
        try {
            assert color != null : "Bitmap::erase expected color != null";
            assert area != null : "Bitmap::erase expected area != null";
            M3Stats.onNativeCall();
            _nEraseRect4f(_ptr, color._r, color._g, color._b, color._a, area._left, area._top, area._right, area._bottom);
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Replaces pixel values with color, interpreted as being in the sRGB ColorSpace.
     * All pixels contained by getBounds() are affected. If the getColorType() is
     * {@link ColorType#GRAY_8} or {@link ColorType#RGB_565}, then alpha is ignored; RGB is
     * treated as opaque. If getColorType() is {@link ColorType#ALPHA_8}, then RGB is ignored.
     *
     * @param color  unpremultiplied color
     * @return           this
     *
     * @see <a href="https://fiddle.skia.org/c/@Bitmap_eraseColor">https://fiddle.skia.org/c/@Bitmap_eraseColor</a>
     */
    public M3Bitmap erase(int color) {
        try {
            M3Stats.onNativeCall();
            _nErase(_ptr, color);
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Replaces pixel values inside area with color, interpreted as being in the sRGB
     * ColorSpace. If area does not intersect getBounds(), call has no effect.
     *
     * If the getColorType() is {@link ColorType#GRAY_8} or {@link ColorType#RGB_565},
     * then alpha is ignored; RGB is treated as opaque. If getColorType() is
     * {@link ColorType#ALPHA_8}, then RGB is ignored.
     *
     * @param color  unpremultiplied color
     * @param area   rectangle to fill
     * @return           this
     *
     * @see <a href="https://fiddle.skia.org/c/@Bitmap_erase">https://fiddle.skia.org/c/@Bitmap_erase</a>
     */
    public M3Bitmap erase(int color, M3IRect area) {
        try {
            M3Stats.onNativeCall();
            _nEraseRect(_ptr, color, area._left, area._top, area._right, area._bottom);
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Returns pixel at (x, y) as unpremultiplied color.
     * Returns black with alpha if ColorType is {@link ColorType#ALPHA_8}.</p>
     *
     * <p>Input is not validated: out of bounds values of x or y returns undefined values
     * or may crash if. Fails if ColorType is {@link ColorType#UNKNOWN} or
     * pixel address is nullptr.</p>
     *
     * <p>ColorSpace in ImageInfo is ignored. Some color precision may be lost in the
     * conversion to unpremultiplied color; original pixel data may have additional
     * precision.</p>
     *
     * @param x  column index, zero or greater, and less than getWidth()
     * @param y  row index, zero or greater, and less than getHeight()
     * @return   pixel converted to unpremultiplied color
     */
    public int getColor(int x, int y) {
        try {
            M3Stats.onNativeCall();
            return _nGetColor(_ptr, x, y);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Returns pixel at (x, y) as unpremultiplied color.
     * Returns black with alpha if ColorType is {@link ColorType#ALPHA_8}.</p>
     *
     * <p>Input is not validated: out of bounds values of x or y returns undefined values
     * or may crash if. Fails if ColorType is {@link ColorType#UNKNOWN} or
     * pixel address is nullptr.</p>
     *
     * <p>ColorSpace in ImageInfo is ignored. Some color precision may be lost in the
     * conversion to unpremultiplied color; original pixel data may have additional
     * precision.</p>
     *
     * @param x  column index, zero or greater, and less than getWidth()
     * @param y  row index, zero or greater, and less than getHeight()
     * @return   pixel converted to unpremultiplied color
     */
    public M3Color4f getColor4f(int x, int y) {
        try {
            M3Stats.onNativeCall();
            return _nGetColor4f(_ptr, x, y);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Look up the pixel at (x,y) and return its alpha component, normalized to [0..1].
     * This is roughly equivalent to GetColorA(getColor()), but can be more efficent
     * (and more precise if the pixels store more than 8 bits per component).</p>
     *
     * @param x  column index, zero or greater, and less than getWidth()
     * @param y  row index, zero or greater, and less than getHeight()
     * @return   alpha converted to normalized float
     */
    public float getAlphaf(int x, int y) {
        try {
            M3Stats.onNativeCall();
            return _nGetAlphaf(_ptr, x, y);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Shares PixelRef with dst. Pixels are not copied; this and dst point
     * to the same pixels; dst.getBounds() are set to the intersection of subset
     * and the original getBounds().</p>
     *
     * <p>subset may be larger than getBounds(). Any area outside of getBounds() is ignored.</p>
     *
     * <p>Any contents of dst are discarded. isVolatile() setting is copied to dst.
     * dst is set to getColorType(), getAlphaType(), and getColorSpace().</p>
     *
     * <p>Return false if:
     * <ul>
     *   <li>dst is null</li>
     *   <li>PixelRef is null</li>
     *   <li>subset does not intersect getBounds()</li>
     * </ul>
     *
     * @param dst     Bitmap set to subset
     * @param subset  rectangle of pixels to reference
     * @return        true if dst is replaced by subset
     *
     * @see <a href="https://fiddle.skia.org/c/@Bitmap_extractSubset">https://fiddle.skia.org/c/@Bitmap_extractSubset</a>
     */
    public boolean extractSubset(M3Bitmap dst, M3IRect subset) {
        try {
            M3Stats.onNativeCall();
            return _nExtractSubset(_ptr, M3Native.getPtr(dst), subset._left, subset._top, subset._right, subset._bottom);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(dst);
        }
    }

    public byte[] readPixels() {
        return readPixels(getImageInfo(), getRowBytes(), 0, 0);
    }

    /**
     * <p>Copies a rect of pixels from Bitmap. Copy starts at (srcX, srcY),
     * and does not exceed Bitmap (getWidth(), getHeight()).</p>
     *
     * <p>dstInfo specifies width, height, ColorType, AlphaType, and ColorSpace of
     * destination. dstRowBytes specifics the gap from one destination row to the next.
     * Returns true if pixels are copied. Returns false if:
     *
     * <ul>
     *   <li>dstRowBytes is less than dstInfo.getMinRowBytes()</li>
     *   <li>PixelRef is null</li>
     * </ul>
     *
     * <p>Pixels are copied only if pixel conversion is possible. If Bitmap getColorType() is
     * {@link ColorType#GRAY_8}, or {@link ColorType#ALPHA_8}; dstInfo.colorType() must match.
     * If Bitmap getColorType() is {@link ColorType#GRAY_8}, dstInfo.getColorSpace() must match.
     * If Bitmap getAlphaType() is {@link ColorAlphaType#OPAQUE}, dstInfo.getAlphaType() must
     * match. If Bitmap getColorSpace() is null, dstInfo.getColorSpace() must match. Returns
     * false if pixel conversion is not possible.</p>
     *
     * <p>srcX and srcY may be negative to copy only top or left of source. Returns
     * false if getWidth() or getHeight() is zero or negative.
     * Returns false if abs(srcX) &gt;= getWidth(), or if abs(srcY) &gt;= getHeight().</p>
     *
     * @param dstInfo      destination width, height, ColorType, AlphaType, ColorSpace
     * @param dstRowBytes  destination row length
     * @param srcX         column index whose absolute value is less than width()
     * @param srcY         row index whose absolute value is less than height()
     * @return             pixel data or null
     */
    public byte[] readPixels(M3ImageInfo dstInfo, long dstRowBytes, int srcX, int srcY) {
        try {
            M3Stats.onNativeCall();
            return _nReadPixels(_ptr,
                dstInfo._width,
                dstInfo._height,
                dstInfo._colorInfo._colorType.ordinal(),
                dstInfo._colorInfo._alphaType.ordinal(),
                M3Native.getPtr(dstInfo._colorInfo._colorSpace),
                dstRowBytes,
                srcX,
                srcY);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(dstInfo._colorInfo._colorSpace);
        }
    }

    /**
     * <p>Sets dst to alpha described by pixels. Returns false if dst cannot
     * be written to or dst pixels cannot be allocated.</p>
     *
     * @param dst holds PixelRef to fill with alpha layer
     * @return    true if alpha layer was not constructed in dst PixelRef
     */
    public boolean extractAlpha(M3Bitmap dst) {
        return extractAlpha(dst, null) != null;
    }
    /**
     * <p>Sets dst to alpha described by pixels. Returns false if dst cannot
     * be written to or dst pixels cannot be allocated.</p>
     *
     * <p>If paint is not null and contains MaskFilter, MaskFilter
     * generates mask alpha from Bitmap. Returns offset to top-left position for dst
     * for alignment with Bitmap; (0, 0) unless MaskFilter generates mask.</p>
     *
     * @param dst   holds PixelRef to fill with alpha layer
     * @param paint holds optional MaskFilter; may be null
     * @return      null if alpha layer was not constructed in dst PixelRef, IPoint otherwise
     */
    public M3IPoint extractAlpha(M3Bitmap dst, M3Paint paint) {
        try {
            M3Stats.onNativeCall();
            return _nExtractAlpha(_ptr, M3Native.getPtr(dst), M3Native.getPtr(paint));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(dst);
            M3ReferenceUtil.reachabilityFence(paint);
        }
    }

    /**
     * If pixel address is available, return Pixmap wrapping it.
     * If pixel address is not available, return null.
     *
     * @return  Pixmap with direct access to pixels, or null
     *
     * @see <a href="https://fiddle.skia.org/c/@Bitmap_peekPixels">https://fiddle.skia.org/c/@Bitmap_peekPixels</a>
     */
    public M3Pixmap peekPixels() {
        try {
            M3Stats.onNativeCall();
            long pixmapPtr = _nPeekPixels(_ptr);
            return pixmapPtr == 0 ? null : new M3Pixmap(pixmapPtr, true);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
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

    public static class _FinalizerHolder {
        public static final long PTR = _nGetFinalizer();
    }

    public static native long    _nGetFinalizer();
    public static native long    _nMake();
    public static native long    _nMakeClone(long ptr);
    public static native void    _nSwap(long ptr, long otherPtr);
    public static native M3ImageInfo _nGetImageInfo(long ptr);
    public static native int     _nGetRowBytesAsPixels(long ptr);
    public static native boolean _nIsNull(long ptr);
    public static native long    _nGetRowBytes(long ptr);
    public static native boolean _nSetAlphaType(long ptr, int alphaType);
    public static native void    _nSetColorSpace(long ptr, long colorSpacePtr);
    public static native long    _nComputeByteSize(long ptr);
    public static native boolean _nIsImmutable(long ptr);
    public static native void    _nSetImmutable(long ptr);
    public static native boolean _nIsVolatile(long ptr);
    public static native void    _nSetVolatile(long ptr, boolean value);
    public static native void    _nReset(long ptr);
    public static native boolean _nComputeIsOpaque(long ptr);
    public static native boolean _nSetImageInfo(long ptr, int width, int height, int colorType, int alphaType, long colorSpacePtr, long rowBytes);
    public static native boolean _nAllocPixelsFlags(long ptr, int width, int height, int colorType, int alphaType, long colorSpacePtr, int flags);
    public static native boolean _nAllocPixelsRowBytes(long ptr, int width, int height, int colorType, int alphaType, long colorSpacePtr, long rowBytes);
    public static native boolean _nInstallPixels(long ptr, int width, int height, int colorType, int alphaType, long colorSpacePtr, byte[] pixels, long rowBytes);
    public static native boolean _nAllocPixels(long ptr);
    public static native long    _nGetPixelRef(long ptr);
    public static native long    _nGetPixelRefOrigin(long ptr);
    public static native void    _nSetPixelRef(long ptr, long pixelRefPtr, int dx, int dy);
    public static native boolean _nIsReadyToDraw(long ptr);
    public static native int     _nGetGenerationId(long ptr);
    public static native void    _nNotifyPixelsChanged(long ptr);
    public static native void    _nErase4f(long ptr, float r, float g, float b, float a);
    public static native void    _nEraseRect4f(long ptr, float r, float g, float b, float a, int left, int top, int right, int bottom);
    public static native void    _nErase(long ptr, int color);
    public static native void    _nEraseRect(long ptr, int color, int left, int top, int right, int bottom);
    public static native int     _nGetColor(long ptr, int x, int y);
    public static native M3Color4f _nGetColor4f(long ptr, int x, int y);
    public static native float   _nGetAlphaf(long ptr, int x, int y);
    public static native boolean _nExtractSubset(long ptr, long dstPtr, int left, int top, int right, int bottom);
    public static native byte[]  _nReadPixels(long ptr, int width, int height, int colorType, int alphaType, long colorSpacePtr, long dstRowBytes, int srcX, int srcY);
    public static native M3IPoint  _nExtractAlpha(long ptr, long dstPtr, long paintPtr);
    public static native long    _nPeekPixels(long ptr);
    public static native long    _nMakeShader(long ptr, int tmx, int tmy, long samplingMode, float[] localMatrix);
}