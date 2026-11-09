package jdk.m3.graphics.paragraph;


public class M3DecorationStyle {
    public static final M3DecorationStyle NONE = new M3DecorationStyle(false, false, false, true, -16777216, M3DecorationLineStyle.SOLID, 1.0F);
    public final boolean _underline;
    public final boolean _overline;
    public final boolean _lineThrough;
    public final boolean _gaps;
    public final int _color;
    public final M3DecorationLineStyle _lineStyle;
    public final float _thicknessMultiplier;

    public M3DecorationStyle(boolean underline, boolean overline, boolean lineThrough, boolean gaps, int color, int lineStyle, float thicknessMultiplier) {
        this(underline, overline, lineThrough, gaps, color, M3DecorationLineStyle._values[lineStyle], thicknessMultiplier);
    }

    public boolean hasUnderline() {
        return _underline;
    }

    public boolean hasOverline() {
        return _overline;
    }

    public boolean hasLineThrough() {
        return _lineThrough;
    }

    public boolean hasGaps() {
        return _gaps;
    }

    @java.lang.SuppressWarnings("all")
    public M3DecorationStyle(final boolean underline, final boolean overline, final boolean lineThrough, final boolean gaps, final int color, final M3DecorationLineStyle lineStyle, final float thicknessMultiplier) {
        this._underline = underline;
        this._overline = overline;
        this._lineThrough = lineThrough;
        this._gaps = gaps;
        this._color = color;
        this._lineStyle = lineStyle;
        this._thicknessMultiplier = thicknessMultiplier;
    }

    @java.lang.SuppressWarnings("all")
    public int getColor() {
        return this._color;
    }

    @java.lang.SuppressWarnings("all")
    public M3DecorationLineStyle getLineStyle() {
        return this._lineStyle;
    }

    @java.lang.SuppressWarnings("all")
    public float getThicknessMultiplier() {
        return this._thicknessMultiplier;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3DecorationStyle)) return false;
        final M3DecorationStyle other = (M3DecorationStyle) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this._underline != other._underline) return false;
        if (this._overline != other._overline) return false;
        if (this._lineThrough != other._lineThrough) return false;
        if (this._gaps != other._gaps) return false;
        if (this.getColor() != other.getColor()) return false;
        if (java.lang.Float.compare(this.getThicknessMultiplier(), other.getThicknessMultiplier()) != 0) return false;
        final java.lang.Object this$_lineStyle = this.getLineStyle();
        final java.lang.Object other$_lineStyle = other.getLineStyle();
        if (this$_lineStyle == null ? other$_lineStyle != null : !this$_lineStyle.equals(other$_lineStyle)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3DecorationStyle;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this._underline ? 79 : 97);
        result = result * PRIME + (this._overline ? 79 : 97);
        result = result * PRIME + (this._lineThrough ? 79 : 97);
        result = result * PRIME + (this._gaps ? 79 : 97);
        result = result * PRIME + this.getColor();
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getThicknessMultiplier());
        final java.lang.Object $_lineStyle = this.getLineStyle();
        result = result * PRIME + ($_lineStyle == null ? 43 : $_lineStyle.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "DecorationStyle(_underline=" + this._underline + ", _overline=" + this._overline + ", _lineThrough=" + this._lineThrough + ", _gaps=" + this._gaps + ", _color=" + this.getColor() + ", _lineStyle=" + this.getLineStyle() + ", _thicknessMultiplier=" + this.getThicknessMultiplier() + ")";
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3DecorationStyle withUnderline(final boolean _underline) {
        return this._underline == _underline ? this : new M3DecorationStyle(_underline, this._overline, this._lineThrough, this._gaps, this._color, this._lineStyle, this._thicknessMultiplier);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3DecorationStyle withOverline(final boolean _overline) {
        return this._overline == _overline ? this : new M3DecorationStyle(this._underline, _overline, this._lineThrough, this._gaps, this._color, this._lineStyle, this._thicknessMultiplier);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3DecorationStyle withLineThrough(final boolean _lineThrough) {
        return this._lineThrough == _lineThrough ? this : new M3DecorationStyle(this._underline, this._overline, _lineThrough, this._gaps, this._color, this._lineStyle, this._thicknessMultiplier);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3DecorationStyle withGaps(final boolean _gaps) {
        return this._gaps == _gaps ? this : new M3DecorationStyle(this._underline, this._overline, this._lineThrough, _gaps, this._color, this._lineStyle, this._thicknessMultiplier);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3DecorationStyle withColor(final int _color) {
        return this._color == _color ? this : new M3DecorationStyle(this._underline, this._overline, this._lineThrough, this._gaps, _color, this._lineStyle, this._thicknessMultiplier);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3DecorationStyle withLineStyle(final M3DecorationLineStyle _lineStyle) {
        return this._lineStyle == _lineStyle ? this : new M3DecorationStyle(this._underline, this._overline, this._lineThrough, this._gaps, this._color, _lineStyle, this._thicknessMultiplier);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3DecorationStyle withThicknessMultiplier(final float _thicknessMultiplier) {
        return this._thicknessMultiplier == _thicknessMultiplier ? this : new M3DecorationStyle(this._underline, this._overline, this._lineThrough, this._gaps, this._color, this._lineStyle, _thicknessMultiplier);
    }
}
