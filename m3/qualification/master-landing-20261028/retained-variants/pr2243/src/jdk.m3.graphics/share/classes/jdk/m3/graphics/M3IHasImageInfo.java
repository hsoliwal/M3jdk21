package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;

public interface M3IHasImageInfo {
    M3ImageInfo getImageInfo();

        /**
     * Returns pixel count in each row. Should be equal or less than
     * getRowBytes() / getImageInfo().getBytesPerPixel().
     *
     * May be less than getPixelRef().getWidth(). Will not exceed getPixelRef().getWidth() less
     *
     * @return  pixel width in ImageInfo
     */
    default int getWidth() {
        return getImageInfo()._width;
    }

    /**
     * Returns pixel row count.
     *
     * Maybe be less than getPixelRef().getHeight(). Will not exceed getPixelRef().getHeight()
     *
     * @return  pixel height in ImageInfo
    */
    default int getHeight() {
        return getImageInfo()._height;
    }

    default M3ColorInfo getColorInfo() {
        return getImageInfo()._colorInfo;
    }

    default M3ColorType getColorType() {
        return getImageInfo()._colorInfo._colorType;
    }

    default M3ColorAlphaType getAlphaType() {
        return getImageInfo()._colorInfo._alphaType;
    }

    default M3ColorSpace getColorSpace() {
        return getImageInfo()._colorInfo._colorSpace;
    }

    /**
     * Returns number of bytes per pixel required by ColorType.
     * Returns zero if colorType is {@link ColorType#UNKNOWN}.
     *
     * @return  bytes in pixel
     */
    default int getBytesPerPixel() {
        return getImageInfo().getBytesPerPixel();
    }


    /**
     * Returns bit shift converting row bytes to row pixels.
     * Returns zero for {@link ColorType#UNKNOWN}.
     *
     * @return  one of: 0, 1, 2, 3; left shift to convert pixels to bytes
     */
    default int getShiftPerPixel() {
        return getImageInfo().getShiftPerPixel();
    }

    /**
     * Returns true if either getWidth() or getHeight() are zero.
     *
     * Does not check if PixelRef is null; call {@link Bitmap#drawsNothing()} to check
     * getWidth(), getHeight(), and PixelRef.
     *
     * @return  true if dimensions do not enclose area
     */
    default boolean isEmpty() {
        return getImageInfo().isEmpty();
    }

    /**
     * <p>Returns true if ColorAlphaType is set to hint that all pixels are opaque; their
     * alpha value is implicitly or explicitly 1.0. If true, and all pixels are
     * not opaque, Skia may draw incorrectly.</p>
     *
     * <p>Does not check if SkColorType allows alpha, or if any pixel value has
     * transparency.</p>
     *
     * @return  true if ImageInfo ColorAlphaType is {@link ColorAlphaType#OPAQUE}
     */
    default boolean isOpaque() {
        return getImageInfo()._colorInfo.isOpaque();
    }
}
