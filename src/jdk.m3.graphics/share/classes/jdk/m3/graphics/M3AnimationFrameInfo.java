package jdk.m3.graphics;

import jdk.m3.graphics.*;

/**
 * Information about individual frames in a multi-framed image.
 */
public class M3AnimationFrameInfo {
    public M3AnimationFrameInfo(int requiredFrame, int duration, boolean fullyReceived, int alphaTypeOrdinal, boolean hasAlphaWithinBounds, int disposalMethodOrdinal, int blendModeOrdinal, M3IRect frameRect) {
        this(requiredFrame, duration, fullyReceived, M3ColorAlphaType._values[alphaTypeOrdinal], hasAlphaWithinBounds, M3AnimationDisposalMode._values[disposalMethodOrdinal], M3BlendMode._values[blendModeOrdinal], frameRect);
    }

    /**
     * <p>The frame that this frame needs to be blended with, or
     * -1 if this frame is independent (so it can be
     * drawn over an uninitialized buffer).</p>
     *
     * <p>Note that this is the *earliest* frame that can be used
     * for blending. Any frame from [_requiredFrame, i) can be
     * used, unless its getDisposalMethod() is {@link AnimationDisposalMode#RESTORE_PREVIOUS}.</p>
     */
    public int _requiredFrame;
    /**
     * Number of milliseconds to show this frame.
     */
    public int _duration;
    /**
     * <p>Whether the end marker for this frame is contained in the stream.</p>
     *
     * <p>Note: this does not guarantee that an attempt to decode will be complete.
     * There could be an error in the stream.</p>
     */
    public boolean _fullyReceived;
    /**
     * <p>This is conservative; it will still return non-opaque if e.g. a
     * color index-based frame has a color with alpha but does not use it.</p>
     */
    public M3ColorAlphaType _alphaType;
    /**
     * <p>Whether the updated rectangle contains alpha.</p>
     *
     * <p>This is conservative; it will still be set to true if e.g. a color
     * index-based frame has a color with alpha but does not use it. In
     * addition, it may be set to true, even if the final frame, after
     * blending, is opaque.</p>
     */
    public boolean _hasAlphaWithinBounds;
    /**
     * <p>How this frame should be modified before decoding the next one.</p>
     */
    public M3AnimationDisposalMode _disposalMethod;
    /**
     * <p>How this frame should blend with the prior frame.</p>
     */
    public M3BlendMode _blendMode;
    /**
     * <p>The rectangle updated by this frame.</p>
     *
     * <p>It may be empty, if the frame does not change the image. It will
     * always be contained by {@link Codec#getSize()}.
     */
    public M3IRect _frameRect;

    /**
     * <p>The frame that this frame needs to be blended with, or
     * -1 if this frame is independent (so it can be
     * drawn over an uninitialized buffer).</p>
     *
     * <p>Note that this is the *earliest* frame that can be used
     * for blending. Any frame from [_requiredFrame, i) can be
     * used, unless its getDisposalMethod() is {@link AnimationDisposalMode#RESTORE_PREVIOUS}.</p>
     */
    @java.lang.SuppressWarnings("all")
    public int getRequiredFrame() {
        return this._requiredFrame;
    }

    /**
     * Number of milliseconds to show this frame.
     */
    @java.lang.SuppressWarnings("all")
    public int getDuration() {
        return this._duration;
    }

    /**
     * <p>Whether the end marker for this frame is contained in the stream.</p>
     *
     * <p>Note: this does not guarantee that an attempt to decode will be complete.
     * There could be an error in the stream.</p>
     */
    @java.lang.SuppressWarnings("all")
    public boolean isFullyReceived() {
        return this._fullyReceived;
    }

    /**
     * <p>This is conservative; it will still return non-opaque if e.g. a
     * color index-based frame has a color with alpha but does not use it.</p>
     */
    @java.lang.SuppressWarnings("all")
    public M3ColorAlphaType getAlphaType() {
        return this._alphaType;
    }

    /**
     * <p>Whether the updated rectangle contains alpha.</p>
     *
     * <p>This is conservative; it will still be set to true if e.g. a color
     * index-based frame has a color with alpha but does not use it. In
     * addition, it may be set to true, even if the final frame, after
     * blending, is opaque.</p>
     */
    @java.lang.SuppressWarnings("all")
    public boolean isHasAlphaWithinBounds() {
        return this._hasAlphaWithinBounds;
    }

    /**
     * <p>How this frame should be modified before decoding the next one.</p>
     */
    @java.lang.SuppressWarnings("all")
    public M3AnimationDisposalMode getDisposalMethod() {
        return this._disposalMethod;
    }

    /**
     * <p>How this frame should blend with the prior frame.</p>
     */
    @java.lang.SuppressWarnings("all")
    public M3BlendMode getBlendMode() {
        return this._blendMode;
    }

    /**
     * <p>The rectangle updated by this frame.</p>
     *
     * <p>It may be empty, if the frame does not change the image. It will
     * always be contained by {@link Codec#getSize()}.
     */
    @java.lang.SuppressWarnings("all")
    public M3IRect getFrameRect() {
        return this._frameRect;
    }

    /**
     * <p>The frame that this frame needs to be blended with, or
     * -1 if this frame is independent (so it can be
     * drawn over an uninitialized buffer).</p>
     *
     * <p>Note that this is the *earliest* frame that can be used
     * for blending. Any frame from [_requiredFrame, i) can be
     * used, unless its getDisposalMethod() is {@link AnimationDisposalMode#RESTORE_PREVIOUS}.</p>
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3AnimationFrameInfo setRequiredFrame(final int _requiredFrame) {
        this._requiredFrame = _requiredFrame;
        return this;
    }

    /**
     * Number of milliseconds to show this frame.
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3AnimationFrameInfo setDuration(final int _duration) {
        this._duration = _duration;
        return this;
    }

    /**
     * <p>Whether the end marker for this frame is contained in the stream.</p>
     *
     * <p>Note: this does not guarantee that an attempt to decode will be complete.
     * There could be an error in the stream.</p>
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3AnimationFrameInfo setFullyReceived(final boolean _fullyReceived) {
        this._fullyReceived = _fullyReceived;
        return this;
    }

    /**
     * <p>This is conservative; it will still return non-opaque if e.g. a
     * color index-based frame has a color with alpha but does not use it.</p>
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3AnimationFrameInfo setAlphaType(final M3ColorAlphaType _alphaType) {
        this._alphaType = _alphaType;
        return this;
    }

    /**
     * <p>Whether the updated rectangle contains alpha.</p>
     *
     * <p>This is conservative; it will still be set to true if e.g. a color
     * index-based frame has a color with alpha but does not use it. In
     * addition, it may be set to true, even if the final frame, after
     * blending, is opaque.</p>
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3AnimationFrameInfo setHasAlphaWithinBounds(final boolean _hasAlphaWithinBounds) {
        this._hasAlphaWithinBounds = _hasAlphaWithinBounds;
        return this;
    }

    /**
     * <p>How this frame should be modified before decoding the next one.</p>
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3AnimationFrameInfo setDisposalMethod(final M3AnimationDisposalMode _disposalMethod) {
        this._disposalMethod = _disposalMethod;
        return this;
    }

    /**
     * <p>How this frame should blend with the prior frame.</p>
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3AnimationFrameInfo setBlendMode(final M3BlendMode _blendMode) {
        this._blendMode = _blendMode;
        return this;
    }

    /**
     * <p>The rectangle updated by this frame.</p>
     *
     * <p>It may be empty, if the frame does not change the image. It will
     * always be contained by {@link Codec#getSize()}.
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3AnimationFrameInfo setFrameRect(final M3IRect _frameRect) {
        this._frameRect = _frameRect;
        return this;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3AnimationFrameInfo)) return false;
        final M3AnimationFrameInfo other = (M3AnimationFrameInfo) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getRequiredFrame() != other.getRequiredFrame()) return false;
        if (this.getDuration() != other.getDuration()) return false;
        if (this.isFullyReceived() != other.isFullyReceived()) return false;
        if (this.isHasAlphaWithinBounds() != other.isHasAlphaWithinBounds()) return false;
        final java.lang.Object this$_alphaType = this.getAlphaType();
        final java.lang.Object other$_alphaType = other.getAlphaType();
        if (this$_alphaType == null ? other$_alphaType != null : !this$_alphaType.equals(other$_alphaType)) return false;
        final java.lang.Object this$_disposalMethod = this.getDisposalMethod();
        final java.lang.Object other$_disposalMethod = other.getDisposalMethod();
        if (this$_disposalMethod == null ? other$_disposalMethod != null : !this$_disposalMethod.equals(other$_disposalMethod)) return false;
        final java.lang.Object this$_blendMode = this.getBlendMode();
        final java.lang.Object other$_blendMode = other.getBlendMode();
        if (this$_blendMode == null ? other$_blendMode != null : !this$_blendMode.equals(other$_blendMode)) return false;
        final java.lang.Object this$_frameRect = this.getFrameRect();
        final java.lang.Object other$_frameRect = other.getFrameRect();
        if (this$_frameRect == null ? other$_frameRect != null : !this$_frameRect.equals(other$_frameRect)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3AnimationFrameInfo;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getRequiredFrame();
        result = result * PRIME + this.getDuration();
        result = result * PRIME + (this.isFullyReceived() ? 79 : 97);
        result = result * PRIME + (this.isHasAlphaWithinBounds() ? 79 : 97);
        final java.lang.Object $_alphaType = this.getAlphaType();
        result = result * PRIME + ($_alphaType == null ? 43 : $_alphaType.hashCode());
        final java.lang.Object $_disposalMethod = this.getDisposalMethod();
        result = result * PRIME + ($_disposalMethod == null ? 43 : $_disposalMethod.hashCode());
        final java.lang.Object $_blendMode = this.getBlendMode();
        result = result * PRIME + ($_blendMode == null ? 43 : $_blendMode.hashCode());
        final java.lang.Object $_frameRect = this.getFrameRect();
        result = result * PRIME + ($_frameRect == null ? 43 : $_frameRect.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "AnimationFrameInfo(_requiredFrame=" + this.getRequiredFrame() + ", _duration=" + this.getDuration() + ", _fullyReceived=" + this.isFullyReceived() + ", _alphaType=" + this.getAlphaType() + ", _hasAlphaWithinBounds=" + this.isHasAlphaWithinBounds() + ", _disposalMethod=" + this.getDisposalMethod() + ", _blendMode=" + this.getBlendMode() + ", _frameRect=" + this.getFrameRect() + ")";
    }

    /**
     * Creates a new {@code AnimationFrameInfo} instance.
     *
     * @param requiredFrame <p>The frame that this frame needs to be blended with, or
     * -1 if this frame is independent (so it can be
     * drawn over an uninitialized buffer).</p>
     *
     * <p>Note that this is the *earliest* frame that can be used
     * for blending. Any frame from [_requiredFrame, i) can be
     * used, unless its getDisposalMethod() is {@link AnimationDisposalMode#RESTORE_PREVIOUS}.</p>
     * @param duration Number of milliseconds to show this frame.
     * @param fullyReceived <p>Whether the end marker for this frame is contained in the stream.</p>
     *
     * <p>Note: this does not guarantee that an attempt to decode will be complete.
     * There could be an error in the stream.</p>
     * @param alphaType <p>This is conservative; it will still return non-opaque if e.g. a
     * color index-based frame has a color with alpha but does not use it.</p>
     * @param hasAlphaWithinBounds <p>Whether the updated rectangle contains alpha.</p>
     *
     * <p>This is conservative; it will still be set to true if e.g. a color
     * index-based frame has a color with alpha but does not use it. In
     * addition, it may be set to true, even if the final frame, after
     * blending, is opaque.</p>
     * @param disposalMethod <p>How this frame should be modified before decoding the next one.</p>
     * @param blendMode <p>How this frame should blend with the prior frame.</p>
     * @param frameRect <p>The rectangle updated by this frame.</p>
     *
     * <p>It may be empty, if the frame does not change the image. It will
     * always be contained by {@link Codec#getSize()}.
     */
    @java.lang.SuppressWarnings("all")
    public M3AnimationFrameInfo(final int requiredFrame, final int duration, final boolean fullyReceived, final M3ColorAlphaType alphaType, final boolean hasAlphaWithinBounds, final M3AnimationDisposalMode disposalMethod, final M3BlendMode blendMode, final M3IRect frameRect) {
        this._requiredFrame = requiredFrame;
        this._duration = duration;
        this._fullyReceived = fullyReceived;
        this._alphaType = alphaType;
        this._hasAlphaWithinBounds = hasAlphaWithinBounds;
        this._disposalMethod = disposalMethod;
        this._blendMode = blendMode;
        this._frameRect = frameRect;
    }

    /**
     * <p>The frame that this frame needs to be blended with, or
     * -1 if this frame is independent (so it can be
     * drawn over an uninitialized buffer).</p>
     *
     * <p>Note that this is the *earliest* frame that can be used
     * for blending. Any frame from [_requiredFrame, i) can be
     * used, unless its getDisposalMethod() is {@link AnimationDisposalMode#RESTORE_PREVIOUS}.</p>
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3AnimationFrameInfo withRequiredFrame(final int _requiredFrame) {
        return this._requiredFrame == _requiredFrame ? this : new M3AnimationFrameInfo(_requiredFrame, this._duration, this._fullyReceived, this._alphaType, this._hasAlphaWithinBounds, this._disposalMethod, this._blendMode, this._frameRect);
    }

    /**
     * Number of milliseconds to show this frame.
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3AnimationFrameInfo withDuration(final int _duration) {
        return this._duration == _duration ? this : new M3AnimationFrameInfo(this._requiredFrame, _duration, this._fullyReceived, this._alphaType, this._hasAlphaWithinBounds, this._disposalMethod, this._blendMode, this._frameRect);
    }

    /**
     * <p>Whether the end marker for this frame is contained in the stream.</p>
     *
     * <p>Note: this does not guarantee that an attempt to decode will be complete.
     * There could be an error in the stream.</p>
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3AnimationFrameInfo withFullyReceived(final boolean _fullyReceived) {
        return this._fullyReceived == _fullyReceived ? this : new M3AnimationFrameInfo(this._requiredFrame, this._duration, _fullyReceived, this._alphaType, this._hasAlphaWithinBounds, this._disposalMethod, this._blendMode, this._frameRect);
    }

    /**
     * <p>This is conservative; it will still return non-opaque if e.g. a
     * color index-based frame has a color with alpha but does not use it.</p>
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3AnimationFrameInfo withAlphaType(final M3ColorAlphaType _alphaType) {
        return this._alphaType == _alphaType ? this : new M3AnimationFrameInfo(this._requiredFrame, this._duration, this._fullyReceived, _alphaType, this._hasAlphaWithinBounds, this._disposalMethod, this._blendMode, this._frameRect);
    }

    /**
     * <p>Whether the updated rectangle contains alpha.</p>
     *
     * <p>This is conservative; it will still be set to true if e.g. a color
     * index-based frame has a color with alpha but does not use it. In
     * addition, it may be set to true, even if the final frame, after
     * blending, is opaque.</p>
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3AnimationFrameInfo withHasAlphaWithinBounds(final boolean _hasAlphaWithinBounds) {
        return this._hasAlphaWithinBounds == _hasAlphaWithinBounds ? this : new M3AnimationFrameInfo(this._requiredFrame, this._duration, this._fullyReceived, this._alphaType, _hasAlphaWithinBounds, this._disposalMethod, this._blendMode, this._frameRect);
    }

    /**
     * <p>How this frame should be modified before decoding the next one.</p>
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3AnimationFrameInfo withDisposalMethod(final M3AnimationDisposalMode _disposalMethod) {
        return this._disposalMethod == _disposalMethod ? this : new M3AnimationFrameInfo(this._requiredFrame, this._duration, this._fullyReceived, this._alphaType, this._hasAlphaWithinBounds, _disposalMethod, this._blendMode, this._frameRect);
    }

    /**
     * <p>How this frame should blend with the prior frame.</p>
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3AnimationFrameInfo withBlendMode(final M3BlendMode _blendMode) {
        return this._blendMode == _blendMode ? this : new M3AnimationFrameInfo(this._requiredFrame, this._duration, this._fullyReceived, this._alphaType, this._hasAlphaWithinBounds, this._disposalMethod, _blendMode, this._frameRect);
    }

    /**
     * <p>The rectangle updated by this frame.</p>
     *
     * <p>It may be empty, if the frame does not change the image. It will
     * always be contained by {@link Codec#getSize()}.
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    public M3AnimationFrameInfo withFrameRect(final M3IRect _frameRect) {
        return this._frameRect == _frameRect ? this : new M3AnimationFrameInfo(this._requiredFrame, this._duration, this._fullyReceived, this._alphaType, this._hasAlphaWithinBounds, this._disposalMethod, this._blendMode, _frameRect);
    }
}
