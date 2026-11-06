package jdk.m3.graphics;


public class M3IRect {
    public final int _left;
    public final int _top;
    public final int _right;
    public final int _bottom;

    public M3IRect(int l, int t, int r, int b) {
        _left = l;
        _top = t;
        _right = r;
        _bottom = b;
    }

    public int getWidth() {
        return _right - _left;
    }

    public int getHeight() {
        return _bottom - _top;
    }

    public M3IRect withWidth(int width) {
        return new M3IRect(_left, _top, _left + width, _bottom);
    }

    public M3IRect withHeight(int height) {
        return new M3IRect(_left, _top, _right, _top + height);
    }

    public static M3IRect makeLTRB(int l, int t, int r, int b) {
        if (l > r) throw new IllegalArgumentException("IRect::makeLTRB expected l <= r, got " + l + " > " + r);
        if (t > b) throw new IllegalArgumentException("IRect::makeLTRB expected t <= b, got " + t + " > " + b);
        return new M3IRect(l, t, r, b);
    }

    public static M3IRect makeXYWH(int l, int t, int w, int h) {
        if (w < 0) throw new IllegalArgumentException("IRect::makeXYWH expected w >= 0, got: " + w);
        if (h < 0) throw new IllegalArgumentException("IRect::makeXYWH expected h >= 0, got: " + h);
        return w >= 0 && h >= 0 ? new M3IRect(l, t, l + w, t + h) : null;
    }

    public static M3IRect makeWH(int w, int h) {
        if (w < 0) throw new IllegalArgumentException("IRect::makeWH expected w >= 0, got: " + w);
        if (h < 0) throw new IllegalArgumentException("IRect::makeWH expected h >= 0, got: " + h);
        return w >= 0 && h >= 0 ? new M3IRect(0, 0, w, h) : null;
    }

    public M3IRect intersect(M3IRect other) {
        assert other != null : "IRect::intersect expected other != null";
        if (_right <= other._left || other._right <= _left || _bottom <= other._top || other._bottom <= _top) return null;
        return new M3IRect(Math.max(_left, other._left), Math.max(_top, other._top), Math.min(_right, other._right), Math.min(_bottom, other._bottom));
    }

    public M3IRect scale(int scale) {
        return scale(scale, scale);
    }

    public M3IRect scale(int sx, int sy) {
        return sx == 1 && sy == 1 ? this : new M3IRect(_left * sx, _top * sy, _right * sx, _bottom * sy);
    }

    public M3IRect offset(int dx, int dy) {
        return dx == 0 && dy == 0 ? this : new M3IRect(_left + dx, _top + dy, _right + dx, _bottom + dy);
    }

    public M3IRect offset(M3IPoint vec) {
        assert vec != null : "IRect::offset expected vec != null";
        return offset(vec._x, vec._y);
    }

    public M3Rect toRect() {
        return new M3Rect(_left, _top, _right, _bottom);
    }

    public boolean isEmpty() {
        return _right == _left || _top == _bottom;
    }

    public boolean contains(int x, int y) {
        return _left <= x && x <= _right && _top <= y && y <= _bottom;
    }

    public boolean contains(M3IPoint vec) {
        assert vec != null : "IRect::contains expected vec != null";
        return _left <= vec._x && vec._x <= _right && _top <= vec._y && vec._y <= _bottom;
    }

    @java.lang.SuppressWarnings("all")
    public int getLeft() {
        return this._left;
    }

    @java.lang.SuppressWarnings("all")
    public int getTop() {
        return this._top;
    }

    @java.lang.SuppressWarnings("all")
    public int getRight() {
        return this._right;
    }

    @java.lang.SuppressWarnings("all")
    public int getBottom() {
        return this._bottom;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3IRect)) return false;
        final M3IRect other = (M3IRect) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getLeft() != other.getLeft()) return false;
        if (this.getTop() != other.getTop()) return false;
        if (this.getRight() != other.getRight()) return false;
        if (this.getBottom() != other.getBottom()) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3IRect;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getLeft();
        result = result * PRIME + this.getTop();
        result = result * PRIME + this.getRight();
        result = result * PRIME + this.getBottom();
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "IRect(_left=" + this.getLeft() + ", _top=" + this.getTop() + ", _right=" + this.getRight() + ", _bottom=" + this.getBottom() + ")";
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3IRect withLeft(final int _left) {
        return this._left == _left ? this : new M3IRect(_left, this._top, this._right, this._bottom);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3IRect withTop(final int _top) {
        return this._top == _top ? this : new M3IRect(this._left, _top, this._right, this._bottom);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3IRect withRight(final int _right) {
        return this._right == _right ? this : new M3IRect(this._left, this._top, _right, this._bottom);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3IRect withBottom(final int _bottom) {
        return this._bottom == _bottom ? this : new M3IRect(this._left, this._top, this._right, _bottom);
    }
}
