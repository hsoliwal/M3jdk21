package jdk.m3.graphics;

/**
 * <p>Represents the struct {@code GrGLTextureInfo} in GrGLTypes.h</p>
 *
 * <p>Type for interacting with GL texture created externally to Skia.
 * The {@code format} here should be a sized, internal format for the texture.
 * We will try to use the sized format if the GL Context supports it, otherwise
 * we will internally fall back to using the base internal formats.</p>
 */
public class M3GLTextureInfo {
    private final int _target;
    private final int _id;
    private final int _format;
    private final boolean _isProtected;

    public M3GLTextureInfo(int target, int id, int format) {
        this(target, id, format, false);
    }

    @java.lang.SuppressWarnings("all")
    public M3GLTextureInfo(final int target, final int id, final int format, final boolean isProtected) {
        this._target = target;
        this._id = id;
        this._format = format;
        this._isProtected = isProtected;
    }

    @java.lang.SuppressWarnings("all")
    public int getTarget() {
        return this._target;
    }

    @java.lang.SuppressWarnings("all")
    public int getId() {
        return this._id;
    }

    @java.lang.SuppressWarnings("all")
    public int getFormat() {
        return this._format;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isProtected() {
        return this._isProtected;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3GLTextureInfo)) return false;
        final M3GLTextureInfo other = (M3GLTextureInfo) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getTarget() != other.getTarget()) return false;
        if (this.getId() != other.getId()) return false;
        if (this.getFormat() != other.getFormat()) return false;
        if (this.isProtected() != other.isProtected()) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3GLTextureInfo;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getTarget();
        result = result * PRIME + this.getId();
        result = result * PRIME + this.getFormat();
        result = result * PRIME + (this.isProtected() ? 79 : 97);
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "GLTextureInfo(_target=" + this.getTarget() + ", _id=" + this.getId() + ", _format=" + this.getFormat() + ", _isProtected=" + this.isProtected() + ")";
    }
}
