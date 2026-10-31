package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;

/**
 * <p>Describes pixel and encoding. ImageInfo can be created from ColorInfo by
 * providing dimensions.</p>
 *
 * <p>It encodes how pixel bits describe alpha, transparency; color components red, blue,
 * and green; and ColorSpace, the range and linearity of colors.</p>
 */
public class M3ColorInfo {
    public final M3ColorType _colorType;
    public final M3ColorAlphaType _alphaType;
    public final M3ColorSpace _colorSpace;
    /**
     * Creates an ColorInfo with {@link ColorType#UNKNOWN}, {@link ColorAlphaType#UNKNOWN},
     * and no ColorSpace.
     */
    public static final M3ColorInfo DEFAULT = new M3ColorInfo(M3ColorType.UNKNOWN, M3ColorAlphaType.UNKNOWN, null);

    public boolean isOpaque() {
        return _alphaType == M3ColorAlphaType.OPAQUE || _colorType.isAlwaysOpaque();
    }

    /**
     * Returns number of bytes per pixel required by ColorType.
     * Returns zero if getColorType() is {@link ColorType#UNKNOWN}.
     *
     * @return  bytes in pixel
     *
     * @see <a href="https://fiddle.skia.org/c/@ImageInfo_bytesPerPixel">https://fiddle.skia.org/c/@ImageInfo_bytesPerPixel</a>
     */
    public int getBytesPerPixel() {
        return _colorType.getBytesPerPixel();
    }

    /**
     * Returns bit shift converting row bytes to row pixels.
     * Returns zero for {@link ColorType#UNKNOWN}.
     *
     * @return  one of: 0, 1, 2, 3, 4; left shift to convert pixels to bytes
     *
     * @see <a href="https://fiddle.skia.org/c/@ImageInfo_shiftPerPixel">https://fiddle.skia.org/c/@ImageInfo_shiftPerPixel</a>
     */
    public int getShiftPerPixel() {
        return _colorType.getShiftPerPixel();
    }

    public boolean isGammaCloseToSRGB() {
        return _colorSpace != null && _colorSpace.isGammaCloseToSRGB();
    }

    @java.lang.SuppressWarnings("all")
    public M3ColorInfo(final M3ColorType colorType, final M3ColorAlphaType alphaType, final M3ColorSpace colorSpace) {
        if (colorType == null) {
            throw new java.lang.NullPointerException("colorType is marked non-null but is null");
        }
        if (alphaType == null) {
            throw new java.lang.NullPointerException("alphaType is marked non-null but is null");
        }
        this._colorType = colorType;
        this._alphaType = alphaType;
        this._colorSpace = colorSpace;
    }

    @java.lang.SuppressWarnings("all")
    public M3ColorType getColorType() {
        return this._colorType;
    }

    @java.lang.SuppressWarnings("all")
    public M3ColorAlphaType getAlphaType() {
        return this._alphaType;
    }

    @java.lang.SuppressWarnings("all")
    public M3ColorSpace getColorSpace() {
        return this._colorSpace;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3ColorInfo)) return false;
        final M3ColorInfo other = (M3ColorInfo) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$_colorType = this.getColorType();
        final java.lang.Object other$_colorType = other.getColorType();
        if (this$_colorType == null ? other$_colorType != null : !this$_colorType.equals(other$_colorType)) return false;
        final java.lang.Object this$_alphaType = this.getAlphaType();
        final java.lang.Object other$_alphaType = other.getAlphaType();
        if (this$_alphaType == null ? other$_alphaType != null : !this$_alphaType.equals(other$_alphaType)) return false;
        final java.lang.Object this$_colorSpace = this.getColorSpace();
        final java.lang.Object other$_colorSpace = other.getColorSpace();
        if (this$_colorSpace == null ? other$_colorSpace != null : !this$_colorSpace.equals(other$_colorSpace)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3ColorInfo;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $_colorType = this.getColorType();
        result = result * PRIME + ($_colorType == null ? 43 : $_colorType.hashCode());
        final java.lang.Object $_alphaType = this.getAlphaType();
        result = result * PRIME + ($_alphaType == null ? 43 : $_alphaType.hashCode());
        final java.lang.Object $_colorSpace = this.getColorSpace();
        result = result * PRIME + ($_colorSpace == null ? 43 : $_colorSpace.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "ColorInfo(_colorType=" + this.getColorType() + ", _alphaType=" + this.getAlphaType() + ", _colorSpace=" + this.getColorSpace() + ")";
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3ColorInfo withColorType(final M3ColorType _colorType) {
        if (_colorType == null) {
            throw new java.lang.NullPointerException("_colorType is marked non-null but is null");
        }
        return this._colorType == _colorType ? this : new M3ColorInfo(_colorType, this._alphaType, this._colorSpace);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3ColorInfo withAlphaType(final M3ColorAlphaType _alphaType) {
        if (_alphaType == null) {
            throw new java.lang.NullPointerException("_alphaType is marked non-null but is null");
        }
        return this._alphaType == _alphaType ? this : new M3ColorInfo(this._colorType, _alphaType, this._colorSpace);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3ColorInfo withColorSpace(final M3ColorSpace _colorSpace) {
        return this._colorSpace == _colorSpace ? this : new M3ColorInfo(this._colorType, this._alphaType, _colorSpace);
    }
}
