package jdk.m3.graphics;


public class M3SamplingModeAnisotropic implements M3SamplingMode {
    public final int _maxAniso;

    public M3SamplingModeAnisotropic(int maxAniso) {
        assert maxAniso >= 1 : "Expected maxAniso = " + maxAniso + " >= 1";
        _maxAniso = maxAniso;
    }

    @Override
    public long _pack() {
        return 4611686018427387904L | (4611686018427387903L & _maxAniso);
    }

    @java.lang.SuppressWarnings("all")
    public int getMaxAniso() {
        return this._maxAniso;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3SamplingModeAnisotropic)) return false;
        final M3SamplingModeAnisotropic other = (M3SamplingModeAnisotropic) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getMaxAniso() != other.getMaxAniso()) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3SamplingModeAnisotropic;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getMaxAniso();
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "SamplingModeAnisotropic(_maxAniso=" + this.getMaxAniso() + ")";
    }
}
