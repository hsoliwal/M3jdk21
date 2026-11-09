package jdk.m3.graphics.paragraph;

public class M3PlaceholderStyle {
    public final float _width;
    public final float _height;
    public final M3PlaceholderAlignment _alignment;
    public final M3BaselineMode _baselineMode;
    /**
     * <p>Distance from the top edge of the rect to the baseline position. This
     * baseline will be aligned against the alphabetic baseline of the surrounding
     * text.</p>
     *
     * <p>Positive values drop the baseline lower (positions the rect higher) and
     * small or negative values will cause the rect to be positioned underneath
     * the line. When baseline == height, the bottom edge of the rect will rest on
     * the alphabetic baseline.</p>
     */
    public final float _baseline;

    /**
     * Creates a new {@code PlaceholderStyle} instance.
     *
     * @param width
     * @param height
     * @param alignment
     * @param baselineMode
     * @param baseline <p>Distance from the top edge of the rect to the baseline position. This
     * baseline will be aligned against the alphabetic baseline of the surrounding
     * text.</p>
     *
     * <p>Positive values drop the baseline lower (positions the rect higher) and
     * small or negative values will cause the rect to be positioned underneath
     * the line. When baseline == height, the bottom edge of the rect will rest on
     * the alphabetic baseline.</p>
     */
    @java.lang.SuppressWarnings("all")
    public M3PlaceholderStyle(final float width, final float height, final M3PlaceholderAlignment alignment, final M3BaselineMode baselineMode, final float baseline) {
        this._width = width;
        this._height = height;
        this._alignment = alignment;
        this._baselineMode = baselineMode;
        this._baseline = baseline;
    }

    @java.lang.SuppressWarnings("all")
    public float getWidth() {
        return this._width;
    }

    @java.lang.SuppressWarnings("all")
    public float getHeight() {
        return this._height;
    }

    @java.lang.SuppressWarnings("all")
    public M3PlaceholderAlignment getAlignment() {
        return this._alignment;
    }

    @java.lang.SuppressWarnings("all")
    public M3BaselineMode getBaselineMode() {
        return this._baselineMode;
    }

    /**
     * <p>Distance from the top edge of the rect to the baseline position. This
     * baseline will be aligned against the alphabetic baseline of the surrounding
     * text.</p>
     *
     * <p>Positive values drop the baseline lower (positions the rect higher) and
     * small or negative values will cause the rect to be positioned underneath
     * the line. When baseline == height, the bottom edge of the rect will rest on
     * the alphabetic baseline.</p>
     */
    @java.lang.SuppressWarnings("all")
    public float getBaseline() {
        return this._baseline;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3PlaceholderStyle)) return false;
        final M3PlaceholderStyle other = (M3PlaceholderStyle) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (java.lang.Float.compare(this.getWidth(), other.getWidth()) != 0) return false;
        if (java.lang.Float.compare(this.getHeight(), other.getHeight()) != 0) return false;
        if (java.lang.Float.compare(this.getBaseline(), other.getBaseline()) != 0) return false;
        final java.lang.Object this$_alignment = this.getAlignment();
        final java.lang.Object other$_alignment = other.getAlignment();
        if (this$_alignment == null ? other$_alignment != null : !this$_alignment.equals(other$_alignment)) return false;
        final java.lang.Object this$_baselineMode = this.getBaselineMode();
        final java.lang.Object other$_baselineMode = other.getBaselineMode();
        if (this$_baselineMode == null ? other$_baselineMode != null : !this$_baselineMode.equals(other$_baselineMode)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3PlaceholderStyle;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getWidth());
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getHeight());
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getBaseline());
        final java.lang.Object $_alignment = this.getAlignment();
        result = result * PRIME + ($_alignment == null ? 43 : $_alignment.hashCode());
        final java.lang.Object $_baselineMode = this.getBaselineMode();
        result = result * PRIME + ($_baselineMode == null ? 43 : $_baselineMode.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "PlaceholderStyle(_width=" + this.getWidth() + ", _height=" + this.getHeight() + ", _alignment=" + this.getAlignment() + ", _baselineMode=" + this.getBaselineMode() + ", _baseline=" + this.getBaseline() + ")";
    }
}
