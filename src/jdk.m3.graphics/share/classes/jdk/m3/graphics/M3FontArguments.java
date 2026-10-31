package jdk.m3.graphics;


public class M3FontArguments {
    public static final M3FontArguments DEFAULT = new M3FontArguments(0, M3FontVariation.EMPTY, 0, M3FontPaletteOverride.EMPTY);
    public final int _collectionIndex;
    public final M3FontVariation[] _variations;
    public final int _paletteIndex;
    public final M3FontPaletteOverride[] _paletteOverrides;

    @java.lang.SuppressWarnings("all")
    public M3FontArguments(final int collectionIndex, final M3FontVariation[] variations, final int paletteIndex, final M3FontPaletteOverride[] paletteOverrides) {
        this._collectionIndex = collectionIndex;
        this._variations = variations;
        this._paletteIndex = paletteIndex;
        this._paletteOverrides = paletteOverrides;
    }

    @java.lang.SuppressWarnings("all")
    public int getCollectionIndex() {
        return this._collectionIndex;
    }

    @java.lang.SuppressWarnings("all")
    public M3FontVariation[] getVariations() {
        return this._variations;
    }

    @java.lang.SuppressWarnings("all")
    public int getPaletteIndex() {
        return this._paletteIndex;
    }

    @java.lang.SuppressWarnings("all")
    public M3FontPaletteOverride[] getPaletteOverrides() {
        return this._paletteOverrides;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3FontArguments)) return false;
        final M3FontArguments other = (M3FontArguments) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getCollectionIndex() != other.getCollectionIndex()) return false;
        if (this.getPaletteIndex() != other.getPaletteIndex()) return false;
        if (!java.util.Arrays.deepEquals(this.getVariations(), other.getVariations())) return false;
        if (!java.util.Arrays.deepEquals(this.getPaletteOverrides(), other.getPaletteOverrides())) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3FontArguments;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getCollectionIndex();
        result = result * PRIME + this.getPaletteIndex();
        result = result * PRIME + java.util.Arrays.deepHashCode(this.getVariations());
        result = result * PRIME + java.util.Arrays.deepHashCode(this.getPaletteOverrides());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "FontArguments(_collectionIndex=" + this.getCollectionIndex() + ", _variations=" + java.util.Arrays.deepToString(this.getVariations()) + ", _paletteIndex=" + this.getPaletteIndex() + ", _paletteOverrides=" + java.util.Arrays.deepToString(this.getPaletteOverrides()) + ")";
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3FontArguments withCollectionIndex(final int _collectionIndex) {
        return this._collectionIndex == _collectionIndex ? this : new M3FontArguments(_collectionIndex, this._variations, this._paletteIndex, this._paletteOverrides);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3FontArguments withVariations(final M3FontVariation[] _variations) {
        return this._variations == _variations ? this : new M3FontArguments(this._collectionIndex, _variations, this._paletteIndex, this._paletteOverrides);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3FontArguments withPaletteIndex(final int _paletteIndex) {
        return this._paletteIndex == _paletteIndex ? this : new M3FontArguments(this._collectionIndex, this._variations, _paletteIndex, this._paletteOverrides);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3FontArguments withPaletteOverrides(final M3FontPaletteOverride[] _paletteOverrides) {
        return this._paletteOverrides == _paletteOverrides ? this : new M3FontArguments(this._collectionIndex, this._variations, this._paletteIndex, _paletteOverrides);
    }
}
