package jdk.m3.graphics;

import java.util.regex.*;

public class M3FontVariation {
    public static final M3FontVariation[] EMPTY = new M3FontVariation[0];
    public final int _tag;
    public final float _value;

    public M3FontVariation(String feature, float value) {
        this(M3FourByteTag.fromString(feature), value);
    }

    public String getTag() {
        return M3FourByteTag.toString(_tag);
    }

    public String toString() {
        return getTag() + "=" + _value;
    }

    public static final Pattern _splitPattern = Pattern.compile("\\s+");
    public static final Pattern _variationPattern = Pattern.compile("(?<tag>[a-z0-9]{4})=(?<value>\\d+)");

    public static M3FontVariation parseOne(String s) {
        Matcher m = _variationPattern.matcher(s);
        if (!m.matches()) throw new IllegalArgumentException("Can’t parse FontVariation: " + s);
        float value = Float.parseFloat(m.group("value"));
        return new M3FontVariation(m.group("tag"), value);
    }

    public static M3FontVariation[] parse(String s) {
        return _splitPattern.splitAsStream(s).map(M3FontVariation::parseOne).toArray(M3FontVariation[]::new);
    }

    @java.lang.SuppressWarnings("all")
    public M3FontVariation(final int tag, final float value) {
        this._tag = tag;
        this._value = value;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3FontVariation)) return false;
        final M3FontVariation other = (M3FontVariation) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$_tag = this.getTag();
        final java.lang.Object other$_tag = other.getTag();
        if (this$_tag == null ? other$_tag != null : !this$_tag.equals(other$_tag)) return false;
        if (java.lang.Float.compare(this.getValue(), other.getValue()) != 0) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3FontVariation;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $_tag = this.getTag();
        result = result * PRIME + ($_tag == null ? 43 : $_tag.hashCode());
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getValue());
        return result;
    }

    @java.lang.SuppressWarnings("all")
    public float getValue() {
        return this._value;
    }
}
