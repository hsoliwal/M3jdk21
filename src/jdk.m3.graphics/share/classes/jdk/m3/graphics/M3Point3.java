package jdk.m3.graphics;


public class M3Point3 {
    public static final M3Point ZERO = new M3Point(0, 0);
    public final float _x;
    public final float _y;
    public final float _z;

    @java.lang.SuppressWarnings("all")
    public M3Point3(final float x, final float y, final float z) {
        this._x = x;
        this._y = y;
        this._z = z;
    }

    @java.lang.SuppressWarnings("all")
    public float getX() {
        return this._x;
    }

    @java.lang.SuppressWarnings("all")
    public float getY() {
        return this._y;
    }

    @java.lang.SuppressWarnings("all")
    public float getZ() {
        return this._z;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3Point3)) return false;
        final M3Point3 other = (M3Point3) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (java.lang.Float.compare(this.getX(), other.getX()) != 0) return false;
        if (java.lang.Float.compare(this.getY(), other.getY()) != 0) return false;
        if (java.lang.Float.compare(this.getZ(), other.getZ()) != 0) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3Point3;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getX());
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getY());
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getZ());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "Point3(_x=" + this.getX() + ", _y=" + this.getY() + ", _z=" + this.getZ() + ")";
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3Point3 withX(final float _x) {
        return this._x == _x ? this : new M3Point3(_x, this._y, this._z);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3Point3 withY(final float _y) {
        return this._y == _y ? this : new M3Point3(this._x, _y, this._z);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3Point3 withZ(final float _z) {
        return this._z == _z ? this : new M3Point3(this._x, this._y, _z);
    }
}
