package jdk.m3.graphics;


public class M3FontVariationAxis {
    public final int _tag;
    public final float _minValue;
    public final float _defaultValue;
    public final float _maxValue;
    public final boolean _hidden;

    public String getTag() {
        return M3FourByteTag.toString(_tag);
    }

    public M3FontVariationAxis(String tag, float min, float def, float max, boolean hidden) {
        this(M3FourByteTag.fromString(tag), min, def, max, hidden);
    }

    public M3FontVariationAxis(String tag, float min, float def, float max) {
        this(M3FourByteTag.fromString(tag), min, def, max, false);
    }

    @java.lang.SuppressWarnings("all")
    public float getMinValue() {
        return this._minValue;
    }

    @java.lang.SuppressWarnings("all")
    public float getDefaultValue() {
        return this._defaultValue;
    }

    @java.lang.SuppressWarnings("all")
    public float getMaxValue() {
        return this._maxValue;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isHidden() {
        return this._hidden;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3FontVariationAxis)) return false;
        final M3FontVariationAxis other = (M3FontVariationAxis) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$_tag = this.getTag();
        final java.lang.Object other$_tag = other.getTag();
        if (this$_tag == null ? other$_tag != null : !this$_tag.equals(other$_tag)) return false;
        if (java.lang.Float.compare(this.getMinValue(), other.getMinValue()) != 0) return false;
        if (java.lang.Float.compare(this.getDefaultValue(), other.getDefaultValue()) != 0) return false;
        if (java.lang.Float.compare(this.getMaxValue(), other.getMaxValue()) != 0) return false;
        if (this.isHidden() != other.isHidden()) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3FontVariationAxis;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $_tag = this.getTag();
        result = result * PRIME + ($_tag == null ? 43 : $_tag.hashCode());
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getMinValue());
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getDefaultValue());
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getMaxValue());
        result = result * PRIME + (this.isHidden() ? 79 : 97);
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "FontVariationAxis(_tag=" + this.getTag() + ", _minValue=" + this.getMinValue() + ", _defaultValue=" + this.getDefaultValue() + ", _maxValue=" + this.getMaxValue() + ", _hidden=" + this.isHidden() + ")";
    }

    @java.lang.SuppressWarnings("all")
    public M3FontVariationAxis(final int tag, final float minValue, final float defaultValue, final float maxValue, final boolean hidden) {
        this._tag = tag;
        this._minValue = minValue;
        this._defaultValue = defaultValue;
        this._maxValue = maxValue;
        this._hidden = hidden;
    }
}
