package jdk.m3.graphics;

import jdk.m3.graphics.*;

/**
 * Result of filtering an image with {@link Image#makeWithFilter}.
 */
public class M3ImageWithFilterResult implements AutoCloseable {
    /**
     * The filtered image.
     */
    public final M3Image _image;
    /**
     * The actual bounds of the filtered image.
     */
    public final M3IRect _subset;
    /**
     * Translation offset for the returned image.
     */
    public final M3IPoint _offset;

    @Override
    public void close() {
        _image.close();
    }

    /**
     * The filtered image.
     */
    @java.lang.SuppressWarnings("all")
    public M3Image getImage() {
        return this._image;
    }

    /**
     * The actual bounds of the filtered image.
     */
    @java.lang.SuppressWarnings("all")
    public M3IRect getSubset() {
        return this._subset;
    }

    /**
     * Translation offset for the returned image.
     */
    @java.lang.SuppressWarnings("all")
    public M3IPoint getOffset() {
        return this._offset;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3ImageWithFilterResult)) return false;
        final M3ImageWithFilterResult other = (M3ImageWithFilterResult) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$_image = this.getImage();
        final java.lang.Object other$_image = other.getImage();
        if (this$_image == null ? other$_image != null : !this$_image.equals(other$_image)) return false;
        final java.lang.Object this$_subset = this.getSubset();
        final java.lang.Object other$_subset = other.getSubset();
        if (this$_subset == null ? other$_subset != null : !this$_subset.equals(other$_subset)) return false;
        final java.lang.Object this$_offset = this.getOffset();
        final java.lang.Object other$_offset = other.getOffset();
        if (this$_offset == null ? other$_offset != null : !this$_offset.equals(other$_offset)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3ImageWithFilterResult;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $_image = this.getImage();
        result = result * PRIME + ($_image == null ? 43 : $_image.hashCode());
        final java.lang.Object $_subset = this.getSubset();
        result = result * PRIME + ($_subset == null ? 43 : $_subset.hashCode());
        final java.lang.Object $_offset = this.getOffset();
        result = result * PRIME + ($_offset == null ? 43 : $_offset.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "ImageWithFilterResult(_image=" + this.getImage() + ", _subset=" + this.getSubset() + ", _offset=" + this.getOffset() + ")";
    }

    /**
     * Creates a new {@code ImageWithFilterResult} instance.
     *
     * @param image The filtered image.
     * @param subset The actual bounds of the filtered image.
     * @param offset Translation offset for the returned image.
     */
    @java.lang.SuppressWarnings("all")
    public M3ImageWithFilterResult(final M3Image image, final M3IRect subset, final M3IPoint offset) {
        if (image == null) {
            throw new java.lang.NullPointerException("image is marked non-null but is null");
        }
        if (subset == null) {
            throw new java.lang.NullPointerException("subset is marked non-null but is null");
        }
        if (offset == null) {
            throw new java.lang.NullPointerException("offset is marked non-null but is null");
        }
        this._image = image;
        this._subset = subset;
        this._offset = offset;
    }
}
