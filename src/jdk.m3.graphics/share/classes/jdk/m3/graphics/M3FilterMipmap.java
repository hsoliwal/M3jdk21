package jdk.m3.graphics;


public class M3FilterMipmap implements M3SamplingMode {
    public final M3FilterMode _filterMode;
    public final M3MipmapMode _mipmapMode;

    public M3FilterMipmap(M3FilterMode filterMode) {
        this(filterMode, M3MipmapMode.NONE);
    }

    @Override
    public long _pack() {
        return 4611686018427387903L & (((long) _filterMode.ordinal() << 32) | (long) _mipmapMode.ordinal());
    }

    @java.lang.SuppressWarnings("all")
    public M3FilterMode getFilterMode() {
        return this._filterMode;
    }

    @java.lang.SuppressWarnings("all")
    public M3MipmapMode getMipmapMode() {
        return this._mipmapMode;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3FilterMipmap)) return false;
        final M3FilterMipmap other = (M3FilterMipmap) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$_filterMode = this.getFilterMode();
        final java.lang.Object other$_filterMode = other.getFilterMode();
        if (this$_filterMode == null ? other$_filterMode != null : !this$_filterMode.equals(other$_filterMode)) return false;
        final java.lang.Object this$_mipmapMode = this.getMipmapMode();
        final java.lang.Object other$_mipmapMode = other.getMipmapMode();
        if (this$_mipmapMode == null ? other$_mipmapMode != null : !this$_mipmapMode.equals(other$_mipmapMode)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3FilterMipmap;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $_filterMode = this.getFilterMode();
        result = result * PRIME + ($_filterMode == null ? 43 : $_filterMode.hashCode());
        final java.lang.Object $_mipmapMode = this.getMipmapMode();
        result = result * PRIME + ($_mipmapMode == null ? 43 : $_mipmapMode.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "FilterMipmap(_filterMode=" + this.getFilterMode() + ", _mipmapMode=" + this.getMipmapMode() + ")";
    }

    @java.lang.SuppressWarnings("all")
    public M3FilterMipmap(final M3FilterMode filterMode, final M3MipmapMode mipmapMode) {
        this._filterMode = filterMode;
        this._mipmapMode = mipmapMode;
    }
}
