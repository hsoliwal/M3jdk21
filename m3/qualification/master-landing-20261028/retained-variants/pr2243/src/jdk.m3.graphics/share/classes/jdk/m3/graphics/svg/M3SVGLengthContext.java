package jdk.m3.graphics.svg;

import jdk.m3.graphics.*;
import jdk.m3.graphics.*;

public class M3SVGLengthContext {
    public final float _width;
    public final float _height;
    public final float _dpi;

    public M3SVGLengthContext(float width, float height) {
        this(width, height, 90);
    }

    public M3SVGLengthContext(M3Point size) {
        this(size._x, size._y, 90);
    }

    public float resolve(M3SVGLength length, M3SVGLengthType type) {
        switch (length._unit) {
        case NUMBER:
            return length._value;
        case PX:
            return length._value;
        case PERCENTAGE:
            switch (type) {
            case HORIZONTAL:
                return length._value * _width / 100.0F;
            case VERTICAL:
                return length._value * _height / 100.0F;
            case OTHER:
                // https://www.w3.org/TR/SVG11/coords.html#Units_viewport_percentage
                return (float) (length._value * Math.hypot(_width, _height) / Math.sqrt(2.0) / 100.0);
            }
        case CM:
            return length._value * _dpi / 2.54F;
        case MM:
            return length._value * _dpi / 25.4F;
        case IN:
            return length._value * _dpi;
        case PT:
            return length._value * _dpi / 72.272F;
        case PC:
            return length._value * _dpi * 12.0F / 72.272F;
        default:
            throw new IllegalArgumentException("Unknown SVGLengthUnit: " + length._unit);
        }
    }

    public M3Rect resolveRect(M3SVGLength x, M3SVGLength y, M3SVGLength width, M3SVGLength height) {
        return M3Rect.makeXYWH(resolve(x, M3SVGLengthType.HORIZONTAL), resolve(y, M3SVGLengthType.VERTICAL), resolve(width, M3SVGLengthType.HORIZONTAL), resolve(height, M3SVGLengthType.VERTICAL));
    }

    @java.lang.SuppressWarnings("all")
    public M3SVGLengthContext(final float width, final float height, final float dpi) {
        this._width = width;
        this._height = height;
        this._dpi = dpi;
    }

    @java.lang.SuppressWarnings("all")
    public float getWidth() {
        return this._width;
    }

    @java.lang.SuppressWarnings("all")
    public float getHeight() {
        return this._height;
    }

    @java.lang.SuppressWarnings("all")
    public float getDpi() {
        return this._dpi;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3SVGLengthContext)) return false;
        final M3SVGLengthContext other = (M3SVGLengthContext) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (java.lang.Float.compare(this.getWidth(), other.getWidth()) != 0) return false;
        if (java.lang.Float.compare(this.getHeight(), other.getHeight()) != 0) return false;
        if (java.lang.Float.compare(this.getDpi(), other.getDpi()) != 0) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3SVGLengthContext;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getWidth());
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getHeight());
        result = result * PRIME + java.lang.Float.floatToIntBits(this.getDpi());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "SVGLengthContext(_width=" + this.getWidth() + ", _height=" + this.getHeight() + ", _dpi=" + this.getDpi() + ")";
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3SVGLengthContext withWidth(final float _width) {
        return this._width == _width ? this : new M3SVGLengthContext(_width, this._height, this._dpi);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3SVGLengthContext withHeight(final float _height) {
        return this._height == _height ? this : new M3SVGLengthContext(this._width, _height, this._dpi);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3SVGLengthContext withDpi(final float _dpi) {
        return this._dpi == _dpi ? this : new M3SVGLengthContext(this._width, this._height, _dpi);
    }
}
