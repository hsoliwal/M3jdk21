package jdk.m3.graphics.svg;


public class M3SVGPreserveAspectRatio {
    public final M3SVGPreserveAspectRatioAlign _align;
    public final M3SVGPreserveAspectRatioScale _scale;

    public M3SVGPreserveAspectRatio(int align, int scale) {
        this(M3SVGPreserveAspectRatioAlign.valueOf(align), M3SVGPreserveAspectRatioScale._values[scale]);
    }

    public M3SVGPreserveAspectRatio() {
        this(M3SVGPreserveAspectRatioAlign.XMID_YMID, M3SVGPreserveAspectRatioScale.MEET);
    }

    public M3SVGPreserveAspectRatio(M3SVGPreserveAspectRatioAlign align) {
        this(align, M3SVGPreserveAspectRatioScale.MEET);
    }

    public M3SVGPreserveAspectRatio(M3SVGPreserveAspectRatioScale scale) {
        this(M3SVGPreserveAspectRatioAlign.XMID_YMID, scale);
    }

    @java.lang.SuppressWarnings("all")
    public M3SVGPreserveAspectRatio(final M3SVGPreserveAspectRatioAlign align, final M3SVGPreserveAspectRatioScale scale) {
        this._align = align;
        this._scale = scale;
    }

    @java.lang.SuppressWarnings("all")
    public M3SVGPreserveAspectRatioAlign getAlign() {
        return this._align;
    }

    @java.lang.SuppressWarnings("all")
    public M3SVGPreserveAspectRatioScale getScale() {
        return this._scale;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3SVGPreserveAspectRatio)) return false;
        final M3SVGPreserveAspectRatio other = (M3SVGPreserveAspectRatio) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$_align = this.getAlign();
        final java.lang.Object other$_align = other.getAlign();
        if (this$_align == null ? other$_align != null : !this$_align.equals(other$_align)) return false;
        final java.lang.Object this$_scale = this.getScale();
        final java.lang.Object other$_scale = other.getScale();
        if (this$_scale == null ? other$_scale != null : !this$_scale.equals(other$_scale)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3SVGPreserveAspectRatio;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $_align = this.getAlign();
        result = result * PRIME + ($_align == null ? 43 : $_align.hashCode());
        final java.lang.Object $_scale = this.getScale();
        result = result * PRIME + ($_scale == null ? 43 : $_scale.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "SVGPreserveAspectRatio(_align=" + this.getAlign() + ", _scale=" + this.getScale() + ")";
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3SVGPreserveAspectRatio withAlign(final M3SVGPreserveAspectRatioAlign _align) {
        return this._align == _align ? this : new M3SVGPreserveAspectRatio(_align, this._scale);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3SVGPreserveAspectRatio withScale(final M3SVGPreserveAspectRatioScale _scale) {
        return this._scale == _scale ? this : new M3SVGPreserveAspectRatio(this._align, _scale);
    }
}
