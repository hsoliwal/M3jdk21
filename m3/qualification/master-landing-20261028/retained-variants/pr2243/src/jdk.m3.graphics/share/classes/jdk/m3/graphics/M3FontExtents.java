package jdk.m3.graphics;

public class M3FontExtents {
    public final float _ascender;
    public final float _descender;
    public final float _lineGap;

    public float getAscenderAbs() {
        return Math.abs(_ascender);
    }

    public float getLineHeight() {
        return -_ascender + _descender + _lineGap;
    }

    @java.lang.SuppressWarnings("all")
    public M3FontExtents(final float ascender, final float descender, final float lineGap) {
        this._ascender = ascender;
        this._descender = descender;
        this._lineGap = lineGap;
    }

    @java.lang.SuppressWarnings("all")
    public float getAscender() {
        return this._ascender;
    }

    @java.lang.SuppressWarnings("all")
    public float getDescender() {
        return this._descender;
    }

    @java.lang.SuppressWarnings("all")
    public float getLineGap() {
        return this._lineGap;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3FontExtents)) return false;
        final M3FontExtents other = (M3FontExtents) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (java.lang.Float.compare(this.getAscender(), other.getAscender()) != 0) return false;
        if (java.lang.Float.compare(this.getDescender(), other.getDescender()) != 0) return false;
        if (java.lang.Float.compare(this.getLineGap(), other.getLineGap()) != 0) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3FontExtents;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getAscender());
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getDescender());
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getLineGap());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "FontExtents(_ascender=" + this.getAscender() + ", _descender=" + this.getDescender() + ", _lineGap=" + this.getLineGap() + ")";
    }
}
