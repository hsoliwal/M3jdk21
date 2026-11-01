package jdk.m3.graphics;

import jdk.m3.graphics.*;

/**
 * Contains the state used to create the layer
 */
public class M3SaveLayerRec {
    public final M3Rect _bounds;
    public final M3Paint _paint;
    public final M3ImageFilter _backdrop;
    public final M3FilterTileMode _tileMode;
    public final M3ColorSpace _colorSpace;
    public final int _flags;

    /**
     * Sets bounds, paint, and backdrop to null. Clears flags.
     */
    public M3SaveLayerRec() {
        this(null, null, null, M3FilterTileMode.CLAMP, null, new M3SaveLayerRecFlag[0]);
    }

    /**
     * Sets bounds and paint. Sets backdrop to null, clears flags
     *
     * @param bounds    layer dimensions; may be null
     * @param paint     applied to layer when overlaying prior layer; may be null
     */
    public M3SaveLayerRec(M3Rect bounds, M3Paint paint) {
        this(bounds, paint, null, M3FilterTileMode.CLAMP, null, new M3SaveLayerRecFlag[0]);
    }

    /**
     * Sets bounds, paint, and flags. Sets backdrop to null
     *
     * @param bounds    layer dimensions; may be null
     * @param paint     applied to layer when overlaying prior layer; may be null
     * @param flags     options to modify layer
     */
    public M3SaveLayerRec(M3Rect bounds, M3Paint paint, M3SaveLayerRecFlag... flags) {
        this(bounds, paint, null, M3FilterTileMode.CLAMP, null, flags);
    }

    /**
     * Sets bounds, paint, and backdrop
     *
     * @param bounds    layer dimensions; may be null
     * @param paint     applied to layer when overlaying prior layer; may be null
     * @param backdrop  If not null, this causes the current layer to be filtered by
     *                  backdrop, and then drawn into the new layer
     *                  (respecting the current clip).
     *                  If null, the new layer is initialized with transparent-black.
     */
    public M3SaveLayerRec(M3Rect bounds, M3Paint paint, M3ImageFilter backdrop) {
        this(bounds, paint, backdrop, M3FilterTileMode.CLAMP, null, new M3SaveLayerRecFlag[0]);
    }

    /**
     * Sets bounds, paint, backdrop, and flags
     *
     * @param bounds     layer dimensions; may be null
     * @param paint      applied to layer when overlaying prior layer; may be null
     * @param backdrop   If not null, this causes the current layer to be filtered by
     *                   backdrop, and then drawn into the new layer
     *                   (respecting the current clip).
     *                   If null, the new layer is initialized with transparent-black.
     * @param flags      options to modify layer
     */
    public M3SaveLayerRec(M3Rect bounds, M3Paint paint, M3ImageFilter backdrop, M3SaveLayerRecFlag... flags) {
        this(bounds, paint, backdrop, M3FilterTileMode.CLAMP, null, flags);
    }

    /**
     * Sets bounds, paint, backdrop, color space and flags
     *
     * @param bounds     layer dimensions; may be null
     * @param paint      applied to layer when overlaying prior layer; may be null
     * @param backdrop   If not null, this causes the current layer to be filtered by
     *                   backdrop, and then drawn into the new layer
     *                   (respecting the current clip).
     *                   If null, the new layer is initialized with transparent-black.
     * @param colorSpace If not null, when the layer is restored, a color space
     *                   conversion will be applied from this color space to the parent's
     *                   color space. The restore paint and backdrop filters will be
     *                   applied in this color space. If null, the new layer will
     *                   inherit the color space from its parent.
     * @param flags      options to modify layer
     */
    public M3SaveLayerRec(M3Rect bounds, M3Paint paint, M3ImageFilter backdrop, M3ColorSpace colorSpace, M3SaveLayerRecFlag... flags) {
        this(bounds, paint, backdrop, M3FilterTileMode.CLAMP, colorSpace, flags);
    }

    /**
     * Sets bounds, paint, backdrop, tile mode, color space and flags
     *
     * @param bounds     layer dimensions; may be null
     * @param paint      applied to layer when overlaying prior layer; may be null
     * @param backdrop   If not null, this causes the current layer to be filtered by
     *                   backdrop, and then drawn into the new layer
     *                   (respecting the current clip).
     *                   If null, the new layer is initialized with transparent-black.
     * @param tileMode   If the 'backdrop' is not null, or 'flags' has
     *                   {@link SaveLayerRecFlag#INIT_WITH_PREVIOUS} set, this tile
     *                   mode is used when the new layer would read outside the backdrop
     *                   image's available content.
     * @param colorSpace If not null, when the layer is restored, a color space
     *                   conversion will be applied from this color space to the parent's
     *                   color space. The restore paint and backdrop filters will be
     *                   applied in this color space. If null, the new layer will
     *                   inherit the color space from its parent.
     * @param flags      options to modify layer
     */
    public M3SaveLayerRec(M3Rect bounds, M3Paint paint, M3ImageFilter backdrop, M3FilterTileMode tileMode, M3ColorSpace colorSpace, M3SaveLayerRecFlag... flags) {
        _bounds = bounds;
        _paint = paint;
        _backdrop = backdrop;
        _tileMode = tileMode;
        _colorSpace = colorSpace;
        int mask = 0;
        for (M3SaveLayerRecFlag flag : flags) mask |= flag._flag;
        _flags = mask;
    }

    @java.lang.SuppressWarnings("all")
    public M3Rect getBounds() {
        return this._bounds;
    }

    @java.lang.SuppressWarnings("all")
    public M3Paint getPaint() {
        return this._paint;
    }

    @java.lang.SuppressWarnings("all")
    public M3ImageFilter getBackdrop() {
        return this._backdrop;
    }

    @java.lang.SuppressWarnings("all")
    public M3FilterTileMode getTileMode() {
        return this._tileMode;
    }

    @java.lang.SuppressWarnings("all")
    public M3ColorSpace getColorSpace() {
        return this._colorSpace;
    }

    @java.lang.SuppressWarnings("all")
    public int getFlags() {
        return this._flags;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3SaveLayerRec)) return false;
        final M3SaveLayerRec other = (M3SaveLayerRec) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getFlags() != other.getFlags()) return false;
        final java.lang.Object this$_bounds = this.getBounds();
        final java.lang.Object other$_bounds = other.getBounds();
        if (this$_bounds == null ? other$_bounds != null : !this$_bounds.equals(other$_bounds)) return false;
        final java.lang.Object this$_paint = this.getPaint();
        final java.lang.Object other$_paint = other.getPaint();
        if (this$_paint == null ? other$_paint != null : !this$_paint.equals(other$_paint)) return false;
        final java.lang.Object this$_backdrop = this.getBackdrop();
        final java.lang.Object other$_backdrop = other.getBackdrop();
        if (this$_backdrop == null ? other$_backdrop != null : !this$_backdrop.equals(other$_backdrop)) return false;
        final java.lang.Object this$_tileMode = this.getTileMode();
        final java.lang.Object other$_tileMode = other.getTileMode();
        if (this$_tileMode == null ? other$_tileMode != null : !this$_tileMode.equals(other$_tileMode)) return false;
        final java.lang.Object this$_colorSpace = this.getColorSpace();
        final java.lang.Object other$_colorSpace = other.getColorSpace();
        if (this$_colorSpace == null ? other$_colorSpace != null : !this$_colorSpace.equals(other$_colorSpace)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3SaveLayerRec;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getFlags();
        final java.lang.Object $_bounds = this.getBounds();
        result = result * PRIME + ($_bounds == null ? 43 : $_bounds.hashCode());
        final java.lang.Object $_paint = this.getPaint();
        result = result * PRIME + ($_paint == null ? 43 : $_paint.hashCode());
        final java.lang.Object $_backdrop = this.getBackdrop();
        result = result * PRIME + ($_backdrop == null ? 43 : $_backdrop.hashCode());
        final java.lang.Object $_tileMode = this.getTileMode();
        result = result * PRIME + ($_tileMode == null ? 43 : $_tileMode.hashCode());
        final java.lang.Object $_colorSpace = this.getColorSpace();
        result = result * PRIME + ($_colorSpace == null ? 43 : $_colorSpace.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "SaveLayerRec(_bounds=" + this.getBounds() + ", _paint=" + this.getPaint() + ", _backdrop=" + this.getBackdrop() + ", _tileMode=" + this.getTileMode() + ", _colorSpace=" + this.getColorSpace() + ", _flags=" + this.getFlags() + ")";
    }
}
