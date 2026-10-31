package jdk.m3.graphics;


public class M3EncodeJPEGOptions {
    public static final M3EncodeJPEGOptions DEFAULT = new M3EncodeJPEGOptions(100, M3EncodeJPEGDownsampleMode.DS_420, M3EncodeJPEGAlphaMode.IGNORE);
    public final int _quality;
    public final M3EncodeJPEGDownsampleMode _downsampleMode;
    public final M3EncodeJPEGAlphaMode _alphaMode;

    @java.lang.SuppressWarnings("all")
    public M3EncodeJPEGOptions(final int quality, final M3EncodeJPEGDownsampleMode downsampleMode, final M3EncodeJPEGAlphaMode alphaMode) {
        this._quality = quality;
        this._downsampleMode = downsampleMode;
        this._alphaMode = alphaMode;
    }

    @java.lang.SuppressWarnings("all")
    public int getQuality() {
        return this._quality;
    }

    @java.lang.SuppressWarnings("all")
    public M3EncodeJPEGDownsampleMode getDownsampleMode() {
        return this._downsampleMode;
    }

    @java.lang.SuppressWarnings("all")
    public M3EncodeJPEGAlphaMode getAlphaMode() {
        return this._alphaMode;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3EncodeJPEGOptions)) return false;
        final M3EncodeJPEGOptions other = (M3EncodeJPEGOptions) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getQuality() != other.getQuality()) return false;
        final java.lang.Object this$_downsampleMode = this.getDownsampleMode();
        final java.lang.Object other$_downsampleMode = other.getDownsampleMode();
        if (this$_downsampleMode == null ? other$_downsampleMode != null : !this$_downsampleMode.equals(other$_downsampleMode)) return false;
        final java.lang.Object this$_alphaMode = this.getAlphaMode();
        final java.lang.Object other$_alphaMode = other.getAlphaMode();
        if (this$_alphaMode == null ? other$_alphaMode != null : !this$_alphaMode.equals(other$_alphaMode)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3EncodeJPEGOptions;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getQuality();
        final java.lang.Object $_downsampleMode = this.getDownsampleMode();
        result = result * PRIME + ($_downsampleMode == null ? 43 : $_downsampleMode.hashCode());
        final java.lang.Object $_alphaMode = this.getAlphaMode();
        result = result * PRIME + ($_alphaMode == null ? 43 : $_alphaMode.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "EncodeJPEGOptions(_quality=" + this.getQuality() + ", _downsampleMode=" + this.getDownsampleMode() + ", _alphaMode=" + this.getAlphaMode() + ")";
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3EncodeJPEGOptions withQuality(final int _quality) {
        return this._quality == _quality ? this : new M3EncodeJPEGOptions(_quality, this._downsampleMode, this._alphaMode);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3EncodeJPEGOptions withDownsampleMode(final M3EncodeJPEGDownsampleMode _downsampleMode) {
        return this._downsampleMode == _downsampleMode ? this : new M3EncodeJPEGOptions(this._quality, _downsampleMode, this._alphaMode);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3EncodeJPEGOptions withAlphaMode(final M3EncodeJPEGAlphaMode _alphaMode) {
        return this._alphaMode == _alphaMode ? this : new M3EncodeJPEGOptions(this._quality, this._downsampleMode, _alphaMode);
    }
}
