package jdk.m3.graphics;


public class M3EncodeWEBPOptions {
    public static final M3EncodeWEBPOptions DEFAULT = new M3EncodeWEBPOptions(M3EncodeWEBPCompressionMode.LOSSY, 100.0F);
    public final M3EncodeWEBPCompressionMode _compressionMode;
    public final float _quality;

    public M3EncodeWEBPOptions(M3EncodeWEBPCompressionMode compression, float quality) {
        _compressionMode = compression;
        _quality = quality;
    }

    @java.lang.SuppressWarnings("all")
    public M3EncodeWEBPCompressionMode getCompressionMode() {
        return this._compressionMode;
    }

    @java.lang.SuppressWarnings("all")
    public float getQuality() {
        return this._quality;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3EncodeWEBPOptions)) return false;
        final M3EncodeWEBPOptions other = (M3EncodeWEBPOptions) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (java.lang.Float.compare(this.getQuality(), other.getQuality()) != 0) return false;
        final java.lang.Object this$_compressionMode = this.getCompressionMode();
        final java.lang.Object other$_compressionMode = other.getCompressionMode();
        if (this$_compressionMode == null ? other$_compressionMode != null : !this$_compressionMode.equals(other$_compressionMode)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3EncodeWEBPOptions;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getQuality());
        final java.lang.Object $_compressionMode = this.getCompressionMode();
        result = result * PRIME + ($_compressionMode == null ? 43 : $_compressionMode.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "EncodeWEBPOptions(_compressionMode=" + this.getCompressionMode() + ", _quality=" + this.getQuality() + ")";
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3EncodeWEBPOptions withCompressionMode(final M3EncodeWEBPCompressionMode _compressionMode) {
        return this._compressionMode == _compressionMode ? this : new M3EncodeWEBPOptions(_compressionMode, this._quality);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3EncodeWEBPOptions withQuality(final float _quality) {
        return this._quality == _quality ? this : new M3EncodeWEBPOptions(this._compressionMode, _quality);
    }
}
