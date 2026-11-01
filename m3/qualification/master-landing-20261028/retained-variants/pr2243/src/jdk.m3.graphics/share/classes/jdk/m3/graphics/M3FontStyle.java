package jdk.m3.graphics;


public class M3FontStyle {
    public final int _value;
    public static final M3FontStyle NORMAL = new M3FontStyle(M3FontWeight.NORMAL, M3FontWidth.NORMAL, M3FontSlant.UPRIGHT);
    public static final M3FontStyle BOLD = new M3FontStyle(M3FontWeight.BOLD, M3FontWidth.NORMAL, M3FontSlant.UPRIGHT);
    public static final M3FontStyle ITALIC = new M3FontStyle(M3FontWeight.NORMAL, M3FontWidth.NORMAL, M3FontSlant.ITALIC);
    public static final M3FontStyle BOLD_ITALIC = new M3FontStyle(M3FontWeight.BOLD, M3FontWidth.NORMAL, M3FontSlant.ITALIC);

    public M3FontStyle(int weight, int width, M3FontSlant slant) {
        _value = (weight & 65535) | ((width & 255) << 16) | (slant.ordinal() << 24);
    }

    public M3FontStyle(int value) {
        this._value = value;
    }

    public int getWeight() {
        return _value & 65535;
    }

    public M3FontStyle withWeight(int weight) {
        return new M3FontStyle(weight, getWidth(), getSlant());
    }

    public int getWidth() {
        return (_value >> 16) & 255;
    }

    public M3FontStyle withWidth(int width) {
        return new M3FontStyle(getWeight(), width, getSlant());
    }

    public M3FontSlant getSlant() {
        return M3FontSlant._values[(_value >> 24) & 255];
    }

    public M3FontStyle withSlant(M3FontSlant slant) {
        return new M3FontStyle(getWeight(), getWidth(), slant);
    }

    @Override
    public String toString() {
        return "FontStyle(" + "weight=" + getWeight() + ", width=" + getWidth() + ", slant=\'" + getSlant() + ')';
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3FontStyle)) return false;
        final M3FontStyle other = (M3FontStyle) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this._value != other._value) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3FontStyle;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this._value;
        return result;
    }
}
