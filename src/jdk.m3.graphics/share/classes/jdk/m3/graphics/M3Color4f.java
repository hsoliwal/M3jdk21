package jdk.m3.graphics;


public class M3Color4f {
    public final float _r;
    public final float _g;
    public final float _b;
    public final float _a;

    public M3Color4f(float r, float g, float b) {
        this(r, g, b, 1.0F);
    }

    public M3Color4f(float[] rgba) {
        this(rgba[0], rgba[1], rgba[2], rgba[3]);
    }

    public M3Color4f(int c) {
        this((c >> 16 & 255) / 255.0F, (c >> 8 & 255) / 255.0F, (c & 255) / 255.0F, (c >> 24 & 255) / 255.0F);
    }

    public int toColor() {
        return ((Math.round(_a * 255.0F)) << 24) | ((Math.round(_r * 255.0F)) << 16) | ((Math.round(_g * 255.0F)) << 8) | (Math.round(_b * 255.0F));
    }

    public float[] flatten() {
        return new float[] {_r, _g, _b, _a};
    }

    public static float[] flattenArray(M3Color4f[] colors) {
        float[] arr = new float[colors.length * 4];
        for (int i = 0; i < colors.length; ++i) {
            arr[i * 4] = colors[i]._r;
            arr[i * 4 + 1] = colors[i]._g;
            arr[i * 4 + 2] = colors[i]._b;
            arr[i * 4 + 3] = colors[i]._a;
        }
        return arr;
    }

    public M3Color4f premultiply() {
        if (_a == 1.0F) return this;
        return new M3Color4f(_r * _a, _g * _a, _b * _a, _a);
    }

    public M3Color4f unpremultiply() {
        if (_a == 1.0F) return this;
        if (_a == 0.0F) return new M3Color4f(0, 0, 0, 0);
        return new M3Color4f(_r / _a, _g / _a, _b / _a, _a);
    }

    /**
     * Interpolate between two unpremultiplied colors.
     *
     * @param other  color to interpolate to
     * @param t      interpolation ratio 0..1, 0 == fully this, 1 == fully other
     *
     * @return       interpolated color, unpremultiplied
     */
    public M3Color4f makeLerp(M3Color4f other, float t) {
        if (t <= 0) return this;
        if (t >= 1) return other;
        M3Color4f c1 = this.premultiply();
        M3Color4f c2 = other.premultiply();
        return new M3Color4f(c1._r + (c2._r - c1._r) * t, c1._g + (c2._g - c1._g) * t, c1._b + (c2._b - c1._b) * t, c1._a + (c2._a - c1._a) * t).unpremultiply();
    }

    /**
     * Interpolate between two premultiplied colors.
     *
     * @param other  color to interpolate to
     * @param t      interpolation ratio 0..1, 0 == fully this, 1 == fully other
     *
     * @return       interpolated color, premultiplied
     */
    public M3Color4f makeLerpPM(M3Color4f other, float t) {
        if (t <= 0) return this;
        if (t >= 1) return other;
        return new M3Color4f(_r + (other._r - _r) * t, _g + (other._g - _g) * t, _b + (other._b - _b) * t, _a + (other._a - _a) * t);
    }

    @java.lang.SuppressWarnings("all")
    public M3Color4f(final float r, final float g, final float b, final float a) {
        this._r = r;
        this._g = g;
        this._b = b;
        this._a = a;
    }

    @java.lang.SuppressWarnings("all")
    public float getR() {
        return this._r;
    }

    @java.lang.SuppressWarnings("all")
    public float getG() {
        return this._g;
    }

    @java.lang.SuppressWarnings("all")
    public float getB() {
        return this._b;
    }

    @java.lang.SuppressWarnings("all")
    public float getA() {
        return this._a;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3Color4f)) return false;
        final M3Color4f other = (M3Color4f) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (java.lang.Float.compare(this.getR(), other.getR()) != 0) return false;
        if (java.lang.Float.compare(this.getG(), other.getG()) != 0) return false;
        if (java.lang.Float.compare(this.getB(), other.getB()) != 0) return false;
        if (java.lang.Float.compare(this.getA(), other.getA()) != 0) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3Color4f;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getR());
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getG());
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getB());
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getA());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "Color4f(_r=" + this.getR() + ", _g=" + this.getG() + ", _b=" + this.getB() + ", _a=" + this.getA() + ")";
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3Color4f withR(final float _r) {
        return this._r == _r ? this : new M3Color4f(_r, this._g, this._b, this._a);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3Color4f withG(final float _g) {
        return this._g == _g ? this : new M3Color4f(this._r, _g, this._b, this._a);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3Color4f withB(final float _b) {
        return this._b == _b ? this : new M3Color4f(this._r, this._g, _b, this._a);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3Color4f withA(final float _a) {
        return this._a == _a ? this : new M3Color4f(this._r, this._g, this._b, _a);
    }
}
