package jdk.m3.graphics.shaper;


public class M3BidiRun {
    public final int _end;
    /**
     * The unicode bidi embedding level (even ltr, odd rtl)
     */
    public final int _level;

    /**
     * Creates a new {@code BidiRun} instance.
     *
     * @param end
     * @param level The unicode bidi embedding level (even ltr, odd rtl)
     */
    @java.lang.SuppressWarnings("all")
    public M3BidiRun(final int end, final int level) {
        this._end = end;
        this._level = level;
    }

    @java.lang.SuppressWarnings("all")
    public int getEnd() {
        return this._end;
    }

    /**
     * The unicode bidi embedding level (even ltr, odd rtl)
     */
    @java.lang.SuppressWarnings("all")
    public int getLevel() {
        return this._level;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3BidiRun)) return false;
        final M3BidiRun other = (M3BidiRun) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getEnd() != other.getEnd()) return false;
        if (this.getLevel() != other.getLevel()) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3BidiRun;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getEnd();
        result = result * PRIME + this.getLevel();
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "BidiRun(_end=" + this.getEnd() + ", _level=" + this.getLevel() + ")";
    }
}
