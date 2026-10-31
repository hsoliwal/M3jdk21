package jdk.m3.graphics;


public class M3GradientStyle {
    public static final int _INTERPOLATE_PREMUL = 1;
    public static M3GradientStyle DEFAULT = new M3GradientStyle(M3FilterTileMode.CLAMP, true, null);
    public final M3FilterTileMode _tileMode;
    public final boolean _premul;
    public final M3Matrix33 _localMatrix;

    public int _getFlags() {
        return 0 | (_premul ? _INTERPOLATE_PREMUL : 0);
    }

    public float[] _getMatrixArray() {
        return _localMatrix == null ? null : _localMatrix.getMat();
    }

    @java.lang.SuppressWarnings("all")
    public M3GradientStyle(final M3FilterTileMode tileMode, final boolean premul, final M3Matrix33 localMatrix) {
        this._tileMode = tileMode;
        this._premul = premul;
        this._localMatrix = localMatrix;
    }

    @java.lang.SuppressWarnings("all")
    public M3FilterTileMode getTileMode() {
        return this._tileMode;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isPremul() {
        return this._premul;
    }

    @java.lang.SuppressWarnings("all")
    public M3Matrix33 getLocalMatrix() {
        return this._localMatrix;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3GradientStyle)) return false;
        final M3GradientStyle other = (M3GradientStyle) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.isPremul() != other.isPremul()) return false;
        final java.lang.Object this$_tileMode = this.getTileMode();
        final java.lang.Object other$_tileMode = other.getTileMode();
        if (this$_tileMode == null ? other$_tileMode != null : !this$_tileMode.equals(other$_tileMode)) return false;
        final java.lang.Object this$_localMatrix = this.getLocalMatrix();
        final java.lang.Object other$_localMatrix = other.getLocalMatrix();
        if (this$_localMatrix == null ? other$_localMatrix != null : !this$_localMatrix.equals(other$_localMatrix)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3GradientStyle;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isPremul() ? 79 : 97);
        final java.lang.Object $_tileMode = this.getTileMode();
        result = result * PRIME + ($_tileMode == null ? 43 : $_tileMode.hashCode());
        final java.lang.Object $_localMatrix = this.getLocalMatrix();
        result = result * PRIME + ($_localMatrix == null ? 43 : $_localMatrix.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "GradientStyle(_tileMode=" + this.getTileMode() + ", _premul=" + this.isPremul() + ", _localMatrix=" + this.getLocalMatrix() + ")";
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3GradientStyle withTileMode(final M3FilterTileMode _tileMode) {
        return this._tileMode == _tileMode ? this : new M3GradientStyle(_tileMode, this._premul, this._localMatrix);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3GradientStyle withPremul(final boolean _premul) {
        return this._premul == _premul ? this : new M3GradientStyle(this._tileMode, _premul, this._localMatrix);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3GradientStyle withLocalMatrix(final M3Matrix33 _localMatrix) {
        return this._localMatrix == _localMatrix ? this : new M3GradientStyle(this._tileMode, this._premul, _localMatrix);
    }
}
