package jdk.m3.graphics;


public class M3IPoint {
    public static final M3IPoint ZERO = new M3IPoint(0, 0);
    public final int _x;
    public final int _y;

    public static M3IPoint _makeFromLong(long l) {
        return new M3IPoint((int) (l >>> 32), (int) (l & -1));
    }

    public M3IPoint offset(int dx, int dy) {
        return dx == 0 && dy == 0 ? this : new M3IPoint(_x + dx, _y + dy);
    }

    public M3IPoint offset(M3IPoint vec) {
        assert vec != null : "IPoint::offset expected other != null";
        return offset(vec._x, vec._y);
    }

    public M3IPoint scale(int scale) {
        return scale(scale, scale);
    }

    public M3IPoint scale(int sx, int sy) {
        return (sx == 1 && sy == 1) || (_x == 0 && _y == 0) ? this : new M3IPoint(_x * sx, _y * sy);
    }

    public M3IPoint inverse() {
        return scale(-1, -1);
    }

    public boolean isEmpty() {
        return _x <= 0 || _y <= 0;
    }

    public M3Point toPoint() {
        return new M3Point(_x, _y);
    }

    @java.lang.SuppressWarnings("all")
    public M3IPoint(final int x, final int y) {
        this._x = x;
        this._y = y;
    }

    @java.lang.SuppressWarnings("all")
    public int getX() {
        return this._x;
    }

    @java.lang.SuppressWarnings("all")
    public int getY() {
        return this._y;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3IPoint)) return false;
        final M3IPoint other = (M3IPoint) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getX() != other.getX()) return false;
        if (this.getY() != other.getY()) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3IPoint;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getX();
        result = result * PRIME + this.getY();
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "IPoint(_x=" + this.getX() + ", _y=" + this.getY() + ")";
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3IPoint withX(final int _x) {
        return this._x == _x ? this : new M3IPoint(_x, this._y);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3IPoint withY(final int _y) {
        return this._y == _y ? this : new M3IPoint(this._x, _y);
    }
}
