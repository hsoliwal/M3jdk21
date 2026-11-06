package jdk.m3.graphics;


public class M3Point {
    public static final M3Point ZERO = new M3Point(0, 0);
    public final float _x;
    public final float _y;

    public static float[] flattenArray(M3Point[] pts) {
        if (pts == null) return null;
        float[] arr = new float[pts.length * 2];
        for (int i = 0; i < pts.length; ++i) {
            arr[i * 2] = pts[i]._x;
            arr[i * 2 + 1] = pts[i]._y;
        }
        return arr;
    }

    public static M3Point[] fromArray(float[] pts) {
        if (pts == null) return null;
        assert pts.length % 2 == 0 : "Expected " + pts.length + " % 2 == 0";
        M3Point[] arr = new M3Point[pts.length / 2];
        for (int i = 0; i < pts.length / 2; ++i) arr[i] = new M3Point(pts[i * 2], pts[i * 2 + 1]);
        return arr;
    }

    public M3Point offset(float dx, float dy) {
        return dx == 0 && dy == 0 ? this : new M3Point(_x + dx, _y + dy);
    }

    public M3Point offset(M3Point vec) {
        assert vec != null : "Point::offset expected other != null";
        return offset(vec._x, vec._y);
    }

    public M3Point scale(float scale) {
        return scale(scale, scale);
    }

    public M3Point scale(float sx, float sy) {
        return (sx == 1 && sy == 1) || (_x == 0 && _y == 0) ? this : new M3Point(_x * sx, _y * sy);
    }

    public M3Point inverse() {
        return scale(-1, -1);
    }

    public boolean isEmpty() {
        return _x <= 0 || _y <= 0;
    }

    public M3IPoint toIPoint() {
        return new M3IPoint((int) _x, (int) _y);
    }

    @java.lang.SuppressWarnings("all")
    public M3Point(final float x, final float y) {
        this._x = x;
        this._y = y;
    }

    @java.lang.SuppressWarnings("all")
    public float getX() {
        return this._x;
    }

    @java.lang.SuppressWarnings("all")
    public float getY() {
        return this._y;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3Point)) return false;
        final M3Point other = (M3Point) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (java.lang.Float.compare(this.getX(), other.getX()) != 0) return false;
        if (java.lang.Float.compare(this.getY(), other.getY()) != 0) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3Point;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getX());
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getY());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "Point(_x=" + this.getX() + ", _y=" + this.getY() + ")";
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3Point withX(final float _x) {
        return this._x == _x ? this : new M3Point(_x, this._y);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3Point withY(final float _y) {
        return this._y == _y ? this : new M3Point(this._x, _y);
    }
}
