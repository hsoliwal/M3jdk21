package jdk.m3.graphics;


public class M3IRange {
    public final int _start;
    public final int _end;

    public static M3IRange _makeFromLong(long l) {
        return new M3IRange((int) (l >>> 32), (int) (l & -1));
    }

    @java.lang.SuppressWarnings("all")
    public M3IRange(final int start, final int end) {
        this._start = start;
        this._end = end;
    }

    @java.lang.SuppressWarnings("all")
    public int getStart() {
        return this._start;
    }

    @java.lang.SuppressWarnings("all")
    public int getEnd() {
        return this._end;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3IRange)) return false;
        final M3IRange other = (M3IRange) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getStart() != other.getStart()) return false;
        if (this.getEnd() != other.getEnd()) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3IRange;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getStart();
        result = result * PRIME + this.getEnd();
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "IRange(_start=" + this.getStart() + ", _end=" + this.getEnd() + ")";
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3IRange withStart(final int _start) {
        return this._start == _start ? this : new M3IRange(_start, this._end);
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3IRange withEnd(final int _end) {
        return this._end == _end ? this : new M3IRange(this._start, _end);
    }
}
