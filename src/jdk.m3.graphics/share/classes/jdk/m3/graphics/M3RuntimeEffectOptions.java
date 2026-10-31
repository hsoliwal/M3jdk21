package jdk.m3.graphics;


public class M3RuntimeEffectOptions {
    public static final M3RuntimeEffectOptions DEFAULT = new M3RuntimeEffectOptions(false, null);
    /**
     * For testing purposes, disables optimization and inlining. (Normally, Runtime Effects
     * don't run the inliner directly, but they still get an inlining pass once they are
     * painted.)
     */
    public final boolean _forceUnoptimized;
    /**
     * When possible this name will be used to identify the created runtime effect.
     */
    public final String _name;

    public M3RuntimeEffectOptions(boolean forceUnoptimized) {
        this(forceUnoptimized, null);
    }

    public M3RuntimeEffectOptions(String name) {
        this(false, name);
    }

    /**
     * Creates a new {@code RuntimeEffectOptions} instance.
     *
     * @param forceUnoptimized For testing purposes, disables optimization and inlining. (Normally, Runtime Effects
     * don't run the inliner directly, but they still get an inlining pass once they are
     * painted.)
     * @param name When possible this name will be used to identify the created runtime effect.
     */
    @java.lang.SuppressWarnings("all")
    public M3RuntimeEffectOptions(final boolean forceUnoptimized, final String name) {
        this._forceUnoptimized = forceUnoptimized;
        this._name = name;
    }

    /**
     * For testing purposes, disables optimization and inlining. (Normally, Runtime Effects
     * don't run the inliner directly, but they still get an inlining pass once they are
     * painted.)
     */
    @java.lang.SuppressWarnings("all")
    public boolean isForceUnoptimized() {
        return this._forceUnoptimized;
    }

    /**
     * When possible this name will be used to identify the created runtime effect.
     */
    @java.lang.SuppressWarnings("all")
    public String getName() {
        return this._name;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3RuntimeEffectOptions)) return false;
        final M3RuntimeEffectOptions other = (M3RuntimeEffectOptions) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.isForceUnoptimized() != other.isForceUnoptimized()) return false;
        final java.lang.Object this$_name = this.getName();
        final java.lang.Object other$_name = other.getName();
        if (this$_name == null ? other$_name != null : !this$_name.equals(other$_name)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3RuntimeEffectOptions;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isForceUnoptimized() ? 79 : 97);
        final java.lang.Object $_name = this.getName();
        result = result * PRIME + ($_name == null ? 43 : $_name.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "RuntimeEffectOptions(_forceUnoptimized=" + this.isForceUnoptimized() + ", _name=" + this.getName() + ")";
    }
}
