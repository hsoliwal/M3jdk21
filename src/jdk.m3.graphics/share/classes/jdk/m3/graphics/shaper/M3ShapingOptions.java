package jdk.m3.graphics.shaper;

import jdk.m3.graphics.*;

public class M3ShapingOptions {
    public static final M3ShapingOptions DEFAULT = new M3ShapingOptions(null, null, true, true, true);
    public final M3FontMgr _fontMgr;
    public final M3FontFeature[] _features;
    public final boolean _leftToRight;
    /**
     * If enabled, fallback font runs will not be broken by whitespace from original font
     */
    public final boolean _approximateSpaces;
    /**
     * If enabled, fallback font runs will not be broken by punctuation from original font
     */
    public final boolean _approximatePunctuation;

    public M3ShapingOptions withFeatures(M3FontFeature[] features) {
        return new M3ShapingOptions(_fontMgr, features, _leftToRight, _approximateSpaces, _approximatePunctuation);
    }

    public M3ShapingOptions withFeatures(String featuresString) {
        return featuresString == null ? withFeatures((M3FontFeature[]) null) : withFeatures(M3FontFeature.parse(featuresString));
    }

    /**
     * Creates a new {@code ShapingOptions} instance.
     *
     * @param fontMgr
     * @param features
     * @param leftToRight
     * @param approximateSpaces If enabled, fallback font runs will not be broken by whitespace from original font
     * @param approximatePunctuation If enabled, fallback font runs will not be broken by punctuation from original font
     */
    @java.lang.SuppressWarnings("all")
    public M3ShapingOptions(final M3FontMgr fontMgr, final M3FontFeature[] features, final boolean leftToRight, final boolean approximateSpaces, final boolean approximatePunctuation) {
        this._fontMgr = fontMgr;
        this._features = features;
        this._leftToRight = leftToRight;
        this._approximateSpaces = approximateSpaces;
        this._approximatePunctuation = approximatePunctuation;
    }

    @java.lang.SuppressWarnings("all")
    public M3FontMgr getFontMgr() {
        return this._fontMgr;
    }

    @java.lang.SuppressWarnings("all")
    public M3FontFeature[] getFeatures() {
        return this._features;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isLeftToRight() {
        return this._leftToRight;
    }

    /**
     * If enabled, fallback font runs will not be broken by whitespace from original font
     */
    @java.lang.SuppressWarnings("all")
    public boolean isApproximateSpaces() {
        return this._approximateSpaces;
    }

    /**
     * If enabled, fallback font runs will not be broken by punctuation from original font
     */
    @java.lang.SuppressWarnings("all")
    public boolean isApproximatePunctuation() {
        return this._approximatePunctuation;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3ShapingOptions)) return false;
        final M3ShapingOptions other = (M3ShapingOptions) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.isLeftToRight() != other.isLeftToRight()) return false;
        if (this.isApproximateSpaces() != other.isApproximateSpaces()) return false;
        if (this.isApproximatePunctuation() != other.isApproximatePunctuation()) return false;
        final java.lang.Object this$_fontMgr = this.getFontMgr();
        final java.lang.Object other$_fontMgr = other.getFontMgr();
        if (this$_fontMgr == null ? other$_fontMgr != null : !this$_fontMgr.equals(other$_fontMgr)) return false;
        if (!java.util.Arrays.deepEquals(this.getFeatures(), other.getFeatures())) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3ShapingOptions;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isLeftToRight() ? 79 : 97);
        result = result * PRIME + (this.isApproximateSpaces() ? 79 : 97);
        result = result * PRIME + (this.isApproximatePunctuation() ? 79 : 97);
        final java.lang.Object $_fontMgr = this.getFontMgr();
        result = result * PRIME + ($_fontMgr == null ? 43 : $_fontMgr.hashCode());
        result = result * PRIME + java.util.Arrays.deepHashCode(this.getFeatures());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "ShapingOptions(_fontMgr=" + this.getFontMgr() + ", _features=" + java.util.Arrays.deepToString(this.getFeatures()) + ", _leftToRight=" + this.isLeftToRight() + ", _approximateSpaces=" + this.isApproximateSpaces() + ", _approximatePunctuation=" + this.isApproximatePunctuation() + ")";
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3ShapingOptions withFontMgr(final M3FontMgr _fontMgr) {
        return this._fontMgr == _fontMgr ? this : new M3ShapingOptions(_fontMgr, this._features, this._leftToRight, this._approximateSpaces, this._approximatePunctuation);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3ShapingOptions withLeftToRight(final boolean _leftToRight) {
        return this._leftToRight == _leftToRight ? this : new M3ShapingOptions(this._fontMgr, this._features, _leftToRight, this._approximateSpaces, this._approximatePunctuation);
    }

    /**
     * If enabled, fallback font runs will not be broken by whitespace from original font
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3ShapingOptions withApproximateSpaces(final boolean _approximateSpaces) {
        return this._approximateSpaces == _approximateSpaces ? this : new M3ShapingOptions(this._fontMgr, this._features, this._leftToRight, _approximateSpaces, this._approximatePunctuation);
    }

    /**
     * If enabled, fallback font runs will not be broken by punctuation from original font
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3ShapingOptions withApproximatePunctuation(final boolean _approximatePunctuation) {
        return this._approximatePunctuation == _approximatePunctuation ? this : new M3ShapingOptions(this._fontMgr, this._features, this._leftToRight, this._approximateSpaces, _approximatePunctuation);
    }
}
