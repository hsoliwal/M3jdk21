package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;

public class M3SurfaceProps {
    public final boolean _deviceIndependentFonts;
    public final M3PixelGeometry _pixelGeometry;

    public M3SurfaceProps(boolean deviceIndependentFonts, int pixelGeometry) {
        this(deviceIndependentFonts, M3PixelGeometry.values()[pixelGeometry]);
    }

    public M3SurfaceProps() {
        this(false, M3PixelGeometry.UNKNOWN);
    }

    public M3SurfaceProps(M3PixelGeometry geo) {
        this(false, geo);
    }

    public int _getFlags() {
        return 0 | (_deviceIndependentFonts ? 1 : 0);
    }

    public int _getPixelGeometryOrdinal() {
        return _pixelGeometry.ordinal();
    }

    @java.lang.SuppressWarnings("all")
    public boolean isDeviceIndependentFonts() {
        return this._deviceIndependentFonts;
    }

    @java.lang.SuppressWarnings("all")
    public M3PixelGeometry getPixelGeometry() {
        return this._pixelGeometry;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3SurfaceProps)) return false;
        final M3SurfaceProps other = (M3SurfaceProps) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.isDeviceIndependentFonts() != other.isDeviceIndependentFonts()) return false;
        final java.lang.Object this$_pixelGeometry = this.getPixelGeometry();
        final java.lang.Object other$_pixelGeometry = other.getPixelGeometry();
        if (this$_pixelGeometry == null ? other$_pixelGeometry != null : !this$_pixelGeometry.equals(other$_pixelGeometry)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3SurfaceProps;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isDeviceIndependentFonts() ? 79 : 97);
        final java.lang.Object $_pixelGeometry = this.getPixelGeometry();
        result = result * PRIME + ($_pixelGeometry == null ? 43 : $_pixelGeometry.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "SurfaceProps(_deviceIndependentFonts=" + this.isDeviceIndependentFonts() + ", _pixelGeometry=" + this.getPixelGeometry() + ")";
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3SurfaceProps withDeviceIndependentFonts(final boolean _deviceIndependentFonts) {
        return this._deviceIndependentFonts == _deviceIndependentFonts ? this : new M3SurfaceProps(_deviceIndependentFonts, this._pixelGeometry);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3SurfaceProps withPixelGeometry(final M3PixelGeometry _pixelGeometry) {
        return this._pixelGeometry == _pixelGeometry ? this : new M3SurfaceProps(this._deviceIndependentFonts, _pixelGeometry);
    }

    @java.lang.SuppressWarnings("all")
    public M3SurfaceProps(final boolean deviceIndependentFonts, final M3PixelGeometry pixelGeometry) {
        this._deviceIndependentFonts = deviceIndependentFonts;
        this._pixelGeometry = pixelGeometry;
    }
}
