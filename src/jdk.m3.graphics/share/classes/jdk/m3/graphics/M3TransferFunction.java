package jdk.m3.graphics;


/**
 * <p>A transfer function mapping encoded values to linear values,
 * represented by this 7-parameter piecewise function:</p>
 *
 * <pre>{@code
 *   linear = sign(encoded) *  (c*|encoded| + f)       , 0 <= |encoded| < d
 *          = sign(encoded) * ((a*|encoded| + b)^g + e), d <= |encoded|
 * }</pre>
 *
 * <p>(A simple gamma transfer function sets g to gamma and a to 1.)</p>
 */
public class M3TransferFunction {
    public final float _g;
    public final float _a;
    public final float _b;
    public final float _c;
    public final float _d;
    public final float _e;
    public final float _f;

    public M3TransferFunction(float[] gabcdef) {
        this(gabcdef[0], gabcdef[1], gabcdef[2], gabcdef[3], gabcdef[4], gabcdef[5], gabcdef[6]);
    }

    @java.lang.SuppressWarnings("all")
    public M3TransferFunction(final float g, final float a, final float b, final float c, final float d, final float e, final float f) {
        this._g = g;
        this._a = a;
        this._b = b;
        this._c = c;
        this._d = d;
        this._e = e;
        this._f = f;
    }

    @java.lang.SuppressWarnings("all")
    public float getG() {
        return this._g;
    }

    @java.lang.SuppressWarnings("all")
    public float getA() {
        return this._a;
    }

    @java.lang.SuppressWarnings("all")
    public float getB() {
        return this._b;
    }

    @java.lang.SuppressWarnings("all")
    public float getC() {
        return this._c;
    }

    @java.lang.SuppressWarnings("all")
    public float getD() {
        return this._d;
    }

    @java.lang.SuppressWarnings("all")
    public float getE() {
        return this._e;
    }

    @java.lang.SuppressWarnings("all")
    public float getF() {
        return this._f;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3TransferFunction)) return false;
        final M3TransferFunction other = (M3TransferFunction) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (java.lang.Float.compare(this.getG(), other.getG()) != 0) return false;
        if (java.lang.Float.compare(this.getA(), other.getA()) != 0) return false;
        if (java.lang.Float.compare(this.getB(), other.getB()) != 0) return false;
        if (java.lang.Float.compare(this.getC(), other.getC()) != 0) return false;
        if (java.lang.Float.compare(this.getD(), other.getD()) != 0) return false;
        if (java.lang.Float.compare(this.getE(), other.getE()) != 0) return false;
        if (java.lang.Float.compare(this.getF(), other.getF()) != 0) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3TransferFunction;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getG());
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getA());
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getB());
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getC());
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getD());
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getE());
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getF());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "TransferFunction(_g=" + this.getG() + ", _a=" + this.getA() + ", _b=" + this.getB() + ", _c=" + this.getC() + ", _d=" + this.getD() + ", _e=" + this.getE() + ", _f=" + this.getF() + ")";
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3TransferFunction withG(final float _g) {
        return this._g == _g ? this : new M3TransferFunction(_g, this._a, this._b, this._c, this._d, this._e, this._f);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3TransferFunction withA(final float _a) {
        return this._a == _a ? this : new M3TransferFunction(this._g, _a, this._b, this._c, this._d, this._e, this._f);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3TransferFunction withB(final float _b) {
        return this._b == _b ? this : new M3TransferFunction(this._g, this._a, _b, this._c, this._d, this._e, this._f);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3TransferFunction withC(final float _c) {
        return this._c == _c ? this : new M3TransferFunction(this._g, this._a, this._b, _c, this._d, this._e, this._f);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3TransferFunction withD(final float _d) {
        return this._d == _d ? this : new M3TransferFunction(this._g, this._a, this._b, this._c, _d, this._e, this._f);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3TransferFunction withE(final float _e) {
        return this._e == _e ? this : new M3TransferFunction(this._g, this._a, this._b, this._c, this._d, _e, this._f);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3TransferFunction withF(final float _f) {
        return this._f == _f ? this : new M3TransferFunction(this._g, this._a, this._b, this._c, this._d, this._e, _f);
    }
}
