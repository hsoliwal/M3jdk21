package jdk.m3.graphics;


public class M3FontPaletteOverride {
    public static final M3FontPaletteOverride[] EMPTY = new M3FontPaletteOverride[0];
    public final int _index;
    public final int _color;

    @java.lang.SuppressWarnings("all")
    public M3FontPaletteOverride(final int index, final int color) {
        this._index = index;
        this._color = color;
    }

    @java.lang.SuppressWarnings("all")
    public int getIndex() {
        return this._index;
    }

    @java.lang.SuppressWarnings("all")
    public int getColor() {
        return this._color;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3FontPaletteOverride)) return false;
        final M3FontPaletteOverride other = (M3FontPaletteOverride) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getIndex() != other.getIndex()) return false;
        if (this.getColor() != other.getColor()) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3FontPaletteOverride;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getIndex();
        result = result * PRIME + this.getColor();
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "FontPaletteOverride(_index=" + this.getIndex() + ", _color=" + this.getColor() + ")";
    }
}
