package jdk.m3.graphics.paragraph;

import jdk.m3.graphics.M3Rect;

public class M3TextBox {
    public final M3Rect _rect;
    public final M3Direction _direction;

    public M3TextBox(float l, float t, float r, float b, int direction) {
        this(M3Rect.makeLTRB(l, t, r, b), M3Direction._values[direction]);
    }

    @java.lang.SuppressWarnings("all")
    public M3TextBox(final M3Rect rect, final M3Direction direction) {
        this._rect = rect;
        this._direction = direction;
    }

    @java.lang.SuppressWarnings("all")
    public M3Rect getRect() {
        return this._rect;
    }

    @java.lang.SuppressWarnings("all")
    public M3Direction getDirection() {
        return this._direction;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3TextBox)) return false;
        final M3TextBox other = (M3TextBox) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$_rect = this.getRect();
        final java.lang.Object other$_rect = other.getRect();
        if (this$_rect == null ? other$_rect != null : !this$_rect.equals(other$_rect)) return false;
        final java.lang.Object this$_direction = this.getDirection();
        final java.lang.Object other$_direction = other.getDirection();
        if (this$_direction == null ? other$_direction != null : !this$_direction.equals(other$_direction)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3TextBox;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $_rect = this.getRect();
        result = result * PRIME + ($_rect == null ? 43 : $_rect.hashCode());
        final java.lang.Object $_direction = this.getDirection();
        result = result * PRIME + ($_direction == null ? 43 : $_direction.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "TextBox(_rect=" + this.getRect() + ", _direction=" + this.getDirection() + ")";
    }
}
