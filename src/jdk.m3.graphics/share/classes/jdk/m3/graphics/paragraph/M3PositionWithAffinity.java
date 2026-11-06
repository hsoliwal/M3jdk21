package jdk.m3.graphics.paragraph;

public class M3PositionWithAffinity {
    public final int _position;
    public final M3Affinity _affinity;

    @java.lang.SuppressWarnings("all")
    public M3PositionWithAffinity(final int position, final M3Affinity affinity) {
        this._position = position;
        this._affinity = affinity;
    }

    @java.lang.SuppressWarnings("all")
    public int getPosition() {
        return this._position;
    }

    @java.lang.SuppressWarnings("all")
    public M3Affinity getAffinity() {
        return this._affinity;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3PositionWithAffinity)) return false;
        final M3PositionWithAffinity other = (M3PositionWithAffinity) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getPosition() != other.getPosition()) return false;
        final java.lang.Object this$_affinity = this.getAffinity();
        final java.lang.Object other$_affinity = other.getAffinity();
        if (this$_affinity == null ? other$_affinity != null : !this$_affinity.equals(other$_affinity)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3PositionWithAffinity;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getPosition();
        final java.lang.Object $_affinity = this.getAffinity();
        result = result * PRIME + ($_affinity == null ? 43 : $_affinity.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "PositionWithAffinity(_position=" + this.getPosition() + ", _affinity=" + this.getAffinity() + ")";
    }
}
