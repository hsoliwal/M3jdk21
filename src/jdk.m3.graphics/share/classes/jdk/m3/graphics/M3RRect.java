package jdk.m3.graphics;

import java.util.*;

public class M3RRect extends M3Rect {
    public final float[] _radii;

    public M3RRect(float l, float t, float r, float b, float[] radii) {
        super(l, t, r, b);
        this._radii = radii;
    }

    public static M3RRect makeLTRB(float l, float t, float r, float b, float radius) {
        return new M3RRect(l, t, r, b, new float[] {radius});
    }

    public static M3RRect makeLTRB(float l, float t, float r, float b, float xRad, float yRad) {
        return new M3RRect(l, t, r, b, new float[] {xRad, yRad});
    }

    public static M3RRect makeLTRB(float l, float t, float r, float b, float tlRad, float trRad, float brRad, float blRad) {
        return new M3RRect(l, t, r, b, new float[] {tlRad, trRad, brRad, blRad});
    }

    public static M3RRect makeNinePatchLTRB(float l, float t, float r, float b, float lRad, float tRad, float rRad, float bRad) {
        return new M3RRect(l, t, r, b, new float[] {lRad, tRad, rRad, tRad, rRad, bRad, lRad, bRad});
    }

    public static M3RRect makeComplexLTRB(float l, float t, float r, float b, float[] radii) {
        return new M3RRect(l, t, r, b, radii);
    }

    public static M3RRect makeOvalLTRB(float l, float t, float r, float b) {
        return new M3RRect(l, t, r, b, new float[] {Math.abs(r - l) / 2.0F, Math.abs(b - t) / 2.0F});
    }

    public static M3RRect makePillLTRB(float l, float t, float r, float b) {
        return new M3RRect(l, t, r, b, new float[] {Math.min(Math.abs(r - l), Math.abs(t - b)) / 2.0F});
    }

    public static M3RRect makeXYWH(float l, float t, float w, float h, float radius) {
        return new M3RRect(l, t, l + w, t + h, new float[] {radius});
    }

    public static M3RRect makeXYWH(float l, float t, float w, float h, float xRad, float yRad) {
        return new M3RRect(l, t, l + w, t + h, new float[] {xRad, yRad});
    }

    public static M3RRect makeXYWH(float l, float t, float w, float h, float tlRad, float trRad, float brRad, float blRad) {
        return new M3RRect(l, t, l + w, t + h, new float[] {tlRad, trRad, brRad, blRad});
    }

    public static M3RRect makeNinePatchXYWH(float l, float t, float w, float h, float lRad, float tRad, float rRad, float bRad) {
        return new M3RRect(l, t, l + w, t + h, new float[] {lRad, tRad, rRad, tRad, rRad, bRad, lRad, bRad});
    }

    public static M3RRect makeComplexXYWH(float l, float t, float w, float h, float[] radii) {
        return new M3RRect(l, t, l + w, t + h, radii);
    }

    public static M3RRect makeOvalXYWH(float l, float t, float w, float h) {
        return new M3RRect(l, t, l + w, t + h, new float[] {w / 2.0F, h / 2.0F});
    }

    public static M3RRect makePillXYWH(float l, float t, float w, float h) {
        return new M3RRect(l, t, l + w, t + h, new float[] {Math.min(w, h) / 2.0F});
    }

    @Override
    public M3RRect scale(float scale) {
        return scale(scale, scale);
    }

    @Override
    public M3RRect scale(float sx, float sy) {
        if (sx == 1 && sy == 1) return this;
        if (sx == sy) {
            switch (_radii.length) {
            case 1:
                return new M3RRect(_left * sx, _top * sx, _right * sx, _bottom * sx, new float[] {_radii[0] * sx});
            case 2:
                return new M3RRect(_left * sx, _top * sx, _right * sx, _bottom * sx, new float[] {_radii[0] * sx, _radii[1] * sx});
            case 4:
                return new M3RRect(_left * sx, _top * sx, _right * sx, _bottom * sx, new float[] {_radii[0] * sx, _radii[1] * sx, _radii[2] * sx, _radii[3] * sx});
            case 8:
                return new M3RRect(_left * sx, _top * sx, _right * sx, _bottom * sx, new float[] {_radii[0] * sx, _radii[1] * sx, _radii[2] * sx, _radii[3] * sx, _radii[4] * sx, _radii[5] * sx, _radii[6] * sx, _radii[7] * sx});
            }
        } else {
            switch (_radii.length) {
            case 1:
                return new M3RRect(_left * sx, _top * sy, _right * sx, _bottom * sy, new float[] {_radii[0] * sx, _radii[0] * sy});
            case 2:
                return new M3RRect(_left * sx, _top * sy, _right * sx, _bottom * sy, new float[] {_radii[0] * sx, _radii[1] * sy});
            case 4:
                return new M3RRect(_left * sx, _top * sy, _right * sx, _bottom * sy, new float[] {_radii[0] * sx, _radii[0] * sy, _radii[1] * sx, _radii[1] * sy, _radii[2] * sx, _radii[2] * sy, _radii[3] * sx, _radii[3] * sy});
            case 8:
                return new M3RRect(_left * sx, _top * sy, _right * sx, _bottom * sy, new float[] {_radii[0] * sx, _radii[1] * sy, _radii[2] * sx, _radii[3] * sy, _radii[4] * sx, _radii[5] * sy, _radii[6] * sx, _radii[7] * sy});
            }
        }
        throw new RuntimeException("Unreachable, _radii=" + Arrays.toString(_radii));
    }

    @Override
    public M3RRect offset(float dx, float dy) {
        return dx == 0 && dy == 0 ? this : new M3RRect(_left + dx, _top + dy, _right + dx, _bottom + dy, _radii);
    }

    @Override
    public M3RRect offset(M3Point vec) {
        assert vec != null : "Rect::offset expected vec != null";
        return offset(vec._x, vec._y);
    }

    @Override
    public M3Rect inflate(float spread) {
        boolean becomesRect = true;
        for (int i = 0; i < _radii.length; ++i) {
            if (_radii[i] + spread >= 0) {
                becomesRect = false;
                break;
            }
        }
        if (becomesRect) return M3Rect.makeLTRB(_left - spread, _top - spread, Math.max(_left - spread, _right + spread), Math.max(_top - spread, _bottom + spread));
         else {
            float[] radii = Arrays.copyOf(_radii, _radii.length);
            for (int i = 0; i < radii.length; ++i) radii[i] = Math.max(0.0F, radii[i] + spread);
            return new M3RRect(_left - spread, _top - spread, Math.max(_left - spread, _right + spread), Math.max(_top - spread, _bottom + spread), radii);
        }
    }

    @Override
    public String toString() {
        return "RRect(_left=" + _left + ", _top=" + _top + ", _right=" + _right + ", _bottom=" + _bottom + ", _radii=" + Arrays.toString(_radii) + ")";
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3RRect)) return false;
        final M3RRect other = (M3RRect) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (!super.equals(o)) return false;
        if (!java.util.Arrays.equals(this._radii, other._radii)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3RRect;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        result = result * PRIME + java.util.Arrays.hashCode(this._radii);
        return result;
    }
}
