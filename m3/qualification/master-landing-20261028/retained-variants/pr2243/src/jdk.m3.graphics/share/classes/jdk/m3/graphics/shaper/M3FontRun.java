package jdk.m3.graphics.shaper;

import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.*;

public class M3FontRun {
    public final int _end;
    public final M3Font _font;

    public long _getFontPtr() {
        try {
            return M3Native.getPtr(_font);
        } finally {
            M3ReferenceUtil.reachabilityFence(_font);
        }
    }

    @java.lang.SuppressWarnings("all")
    public M3FontRun(final int end, final M3Font font) {
        this._end = end;
        this._font = font;
    }

    @java.lang.SuppressWarnings("all")
    public int getEnd() {
        return this._end;
    }

    @java.lang.SuppressWarnings("all")
    public M3Font getFont() {
        return this._font;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3FontRun)) return false;
        final M3FontRun other = (M3FontRun) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getEnd() != other.getEnd()) return false;
        final java.lang.Object this$_font = this.getFont();
        final java.lang.Object other$_font = other.getFont();
        if (this$_font == null ? other$_font != null : !this$_font.equals(other$_font)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3FontRun;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getEnd();
        final java.lang.Object $_font = this.getFont();
        result = result * PRIME + ($_font == null ? 43 : $_font.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "FontRun(_end=" + this.getEnd() + ", _font=" + this.getFont() + ")";
    }
}
