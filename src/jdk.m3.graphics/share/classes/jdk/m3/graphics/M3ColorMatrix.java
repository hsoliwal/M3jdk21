package jdk.m3.graphics;

public class M3ColorMatrix {
    public final float[] _mat;

    public M3ColorMatrix(float... mat) {
        assert mat.length == 20 : "Expected 20 elements, got " + mat == null ? null : mat.length;
        _mat = mat;
    }

    @java.lang.SuppressWarnings("all")
    public float[] getMat() {
        return this._mat;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3ColorMatrix)) return false;
        final M3ColorMatrix other = (M3ColorMatrix) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (!java.util.Arrays.equals(this.getMat(), other.getMat())) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3ColorMatrix;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + java.util.Arrays.hashCode(this.getMat());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "ColorMatrix(_mat=" + java.util.Arrays.toString(this.getMat()) + ")";
    }
}
