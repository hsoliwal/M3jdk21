package jdk.m3.graphics;


public class M3Rect {
    public final float _left;
    public final float _top;
    public final float _right;
    public final float _bottom;

    public M3Rect(float l, float t, float r, float b) {
        this._left = l;
        this._top = t;
        this._right = r;
        this._bottom = b;
    }

    public float getWidth() {
        return _right - _left;
    }

    public float getHeight() {
        return _bottom - _top;
    }

    public M3Rect withWidth(float width) {
        return new M3Rect(_left, _top, _left + width, _bottom);
    }

    public M3Rect withHeight(float height) {
        return new M3Rect(_left, _top, _right, _top + height);
    }

    public static M3Rect makeLTRB(float l, float t, float r, float b) {
        if (l > r) throw new IllegalArgumentException("Rect::makeLTRB expected l <= r, got " + l + " > " + r);
        if (t > b) throw new IllegalArgumentException("Rect::makeLTRB expected t <= b, got " + t + " > " + b);
        return new M3Rect(l, t, r, b);
    }

    public static M3Rect makeWH(float w, float h) {
        if (w < 0) throw new IllegalArgumentException("Rect::makeWH expected w >= 0, got: " + w);
        if (h < 0) throw new IllegalArgumentException("Rect::makeWH expected h >= 0, got: " + h);
        return new M3Rect(0, 0, w, h);
    }

    public static M3Rect makeWH(M3Point size) {
        assert size != null : "Rect::makeWH expected size != null";
        return makeWH(size._x, size._y);
    }

    public static M3Rect makeXYWH(float l, float t, float w, float h) {
        if (w < 0) throw new IllegalArgumentException("Rect::makeXYWH expected w >= 0, got: " + w);
        if (h < 0) throw new IllegalArgumentException("Rect::makeXYWH expected h >= 0, got: " + h);
        return new M3Rect(l, t, l + w, t + h);
    }

    public M3Rect intersect(M3Rect other) {
        assert other != null : "Rect::intersect expected other != null";
        if (_right <= other._left || other._right <= _left || _bottom <= other._top || other._bottom <= _top) return null;
        return new M3Rect(Math.max(_left, other._left), Math.max(_top, other._top), Math.min(_right, other._right), Math.min(_bottom, other._bottom));
    }

    public M3Rect scale(float scale) {
        return scale(scale, scale);
    }

    public M3Rect scale(float sx, float sy) {
        return sx == 1 && sy == 1 ? this : new M3Rect(_left * sx, _top * sy, _right * sx, _bottom * sy);
    }

    public M3Rect offset(float dx, float dy) {
        return dx == 0 && dy == 0 ? this : new M3Rect(_left + dx, _top + dy, _right + dx, _bottom + dy);
    }

    public M3Rect offset(M3Point vec) {
        assert vec != null : "Rect::offset expected vec != null";
        return offset(vec._x, vec._y);
    }

    public M3IRect toIRect() {
        return new M3IRect((int) _left, (int) _top, (int) _right, (int) _bottom);
    }

    public M3RRect withRadii(float radius) {
        return M3RRect.makeLTRB(_left, _top, _right, _bottom, radius);
    }

    public M3RRect withRadii(float xRad, float yRad) {
        return M3RRect.makeLTRB(_left, _top, _right, _bottom, xRad, yRad);
    }

    public M3RRect withRadii(float tlRad, float trRad, float brRad, float blRad) {
        return M3RRect.makeLTRB(_left, _top, _right, _bottom, tlRad, trRad, brRad, blRad);
    }

    public M3RRect withRadii(float[] radii) {
        return M3RRect.makeComplexLTRB(_left, _top, _right, _bottom, radii);
    }

    public M3Rect inflate(float spread) {
        if (spread <= 0) return M3Rect.makeLTRB(_left - spread, _top - spread, Math.max(_left - spread, _right + spread), Math.max(_top - spread, _bottom + spread));
         else return M3RRect.makeLTRB(_left - spread, _top - spread, Math.max(_left - spread, _right + spread), Math.max(_top - spread, _bottom + spread), spread);
    }

    public boolean isEmpty() {
        return _right == _left || _top == _bottom;
    }

    public boolean contains(float x, float y) {
        return _left <= x && x <= _right && _top <= y && y <= _bottom;
    }

    public boolean contains(M3Point vec) {
        assert vec != null : "Rect::contains expected vec != null";
        return _left <= vec._x && vec._x <= _right && _top <= vec._y && vec._y <= _bottom;
    }

    @java.lang.SuppressWarnings("all")
    public float getLeft() {
        return this._left;
    }

    @java.lang.SuppressWarnings("all")
    public float getTop() {
        return this._top;
    }

    @java.lang.SuppressWarnings("all")
    public float getRight() {
        return this._right;
    }

    @java.lang.SuppressWarnings("all")
    public float getBottom() {
        return this._bottom;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3Rect)) return false;
        final M3Rect other = (M3Rect) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (java.lang.Float.compare(this.getLeft(), other.getLeft()) != 0) return false;
        if (java.lang.Float.compare(this.getTop(), other.getTop()) != 0) return false;
        if (java.lang.Float.compare(this.getRight(), other.getRight()) != 0) return false;
        if (java.lang.Float.compare(this.getBottom(), other.getBottom()) != 0) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3Rect;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getLeft());
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getTop());
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getRight());
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getBottom());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "Rect(_left=" + this.getLeft() + ", _top=" + this.getTop() + ", _right=" + this.getRight() + ", _bottom=" + this.getBottom() + ")";
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3Rect withLeft(final float _left) {
        return this._left == _left ? this : new M3Rect(_left, this._top, this._right, this._bottom);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3Rect withTop(final float _top) {
        return this._top == _top ? this : new M3Rect(this._left, _top, this._right, this._bottom);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3Rect withRight(final float _right) {
        return this._right == _right ? this : new M3Rect(this._left, this._top, _right, this._bottom);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3Rect withBottom(final float _bottom) {
        return this._bottom == _bottom ? this : new M3Rect(this._left, this._top, this._right, _bottom);
    }
}
