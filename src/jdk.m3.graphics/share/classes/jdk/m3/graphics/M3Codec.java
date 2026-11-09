package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.*;

public class M3Codec extends M3Managed implements M3IHasImageInfo {
    static { M3Library.staticLoad(); }

    public M3ImageInfo _imageInfo = null;

    public M3Codec(long ptr) {
        super(ptr, _FinalizerHolder.PTR);
    }

    /**
     * If this data represents an encoded image that we know how to decode,
     * return an Codec that can decode it. Otherwise throws IllegalArgumentException.
     */
    public static M3Codec makeFromData(M3Data data) {
        try {
            M3Stats.onNativeCall();
            long ptr = _nMakeFromData(M3Native.getPtr(data));
            if (ptr == 0)
                throw new IllegalArgumentException("Unsupported format");
            return new M3Codec(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(data);
        }
    }

    @Override
    public M3ImageInfo getImageInfo() {
        try {
            if (_imageInfo == null) {
                M3Stats.onNativeCall();
                _imageInfo = _nGetImageInfo(_ptr);
            }
            return _imageInfo;
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3IPoint getSize() {
        try {
            M3Stats.onNativeCall();
            return M3IPoint._makeFromLong(_nGetSize(_ptr));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3EncodedOrigin getEncodedOrigin() {
        try {
            M3Stats.onNativeCall();
            return M3EncodedOrigin._values[_nGetEncodedOrigin(_ptr)];
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    @SuppressWarnings("deprecation")
    public M3EncodedImageFormat getEncodedImageFormat() {
        try {
            M3Stats.onNativeCall();
            return M3EncodedImageFormat._values[_nGetEncodedImageFormat(_ptr)];
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Decodes an image into a bitmap.</p>
     *
     * @return  decoded bitmap
     */
    public M3Bitmap readPixels() {
        M3Bitmap bitmap = new M3Bitmap();
        bitmap.allocPixels(getImageInfo());
        readPixels(bitmap);
        return bitmap;
    }

    /**
     * <p>Decodes an image into a bitmap.</p>
     *
     * <p>Repeated calls to this function should give the same results,
     * allowing the PixelRef to be immutable.</p>
     *
     * <p>Bitmap specifies the description of the format (config, size)
     * expected by the caller.  This can simply be identical
     * to the info returned by getImageInfo().</p>
     *
     * <p>This contract also allows the caller to specify
     * different output-configs, which the implementation can
     * decide to support or not.</p>
     *
     * <p>A size that does not match getImageInfo() implies a request
     * to scale. If the generator cannot perform this scale,
     * it will throw an exception.</p>
     *
     * <p>If the info contains a non-null ColorSpace, the codec
     * will perform the appropriate color space transformation.</p>
     *
     * <p>If the caller passes in the ColorSpace that maps to the
     * ICC profile reported by getICCProfile(), the color space
     * transformation is a no-op.</p>
     *
     * <p>If the caller passes a null SkColorSpace, no color space
     * transformation will be done.</p>
     *
     * @param bitmap      the description of the format (config, size) expected by the caller
     * @return            this
     */
    public M3Codec readPixels(M3Bitmap bitmap) {
        try {
            M3Stats.onNativeCall();
            _validateResult(_nReadPixels(_ptr, M3Native.getPtr(bitmap), 0, -1));
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(bitmap);
        }
    }

    /**
     * <p>Decodes a frame in a multi-frame image into a bitmap.</p>
     *
     * <p>Repeated calls to this function should give the same results,
     * allowing the PixelRef to be immutable.</p>
     *
     * <p>Bitmap specifies the description of the format (config, size)
     * expected by the caller.  This can simply be identical
     * to the info returned by getImageInfo().</p>
     *
     * <p>This contract also allows the caller to specify
     * different output-configs, which the implementation can
     * decide to support or not.</p>
     *
     * <p>A size that does not match getImageInfo() implies a request
     * to scale. If the generator cannot perform this scale,
     * it will throw an exception.</p>
     *
     * <p>If the info contains a non-null ColorSpace, the codec
     * will perform the appropriate color space transformation.</p>
     *
     * <p>If the caller passes in the ColorSpace that maps to the
     * ICC profile reported by getICCProfile(), the color space
     * transformation is a no-op.</p>
     *
     * <p>If the caller passes a null SkColorSpace, no color space
     * transformation will be done.</p>
     *
     * @param bitmap      the description of the format (config, size) expected by the caller
     * @param frame       index of the frame in multi-frame image to decode
     * @return            this
     */
    public M3Codec readPixels(M3Bitmap bitmap, int frame) {
        try {
            M3Stats.onNativeCall();
            _validateResult(_nReadPixels(_ptr, M3Native.getPtr(bitmap), frame, -1));
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(bitmap);
        }
    }

    /**
     * <p>Decodes a frame in a multi-frame image into a bitmap.</p>
     *
     * <p>Repeated calls to this function should give the same results,
     * allowing the PixelRef to be immutable.</p>
     *
     * <p>Bitmap specifies the description of the format (config, size)
     * expected by the caller.  This can simply be identical
     * to the info returned by getImageInfo().</p>
     *
     * <p>This contract also allows the caller to specify
     * different output-configs, which the implementation can
     * decide to support or not.</p>
     *
     * <p>A size that does not match getImageInfo() implies a request
     * to scale. If the generator cannot perform this scale,
     * it will throw an exception.</p>
     *
     * <p>If the info contains a non-null ColorSpace, the codec
     * will perform the appropriate color space transformation.</p>
     *
     * <p>If the caller passes in the ColorSpace that maps to the
     * ICC profile reported by getICCProfile(), the color space
     * transformation is a no-op.</p>
     *
     * <p>If the caller passes a null SkColorSpace, no color space
     * transformation will be done.</p>
     *
     * @param bitmap      the description of the format (config, size) expected by the caller
     * @param frame       index of the frame in multi-frame image to decode
     * @param priorFrame  index of the frame already in bitmap, might be used to optimize retrieving current frame
     * @return            this
     */
    public M3Codec readPixels(M3Bitmap bitmap, int frame, int priorFrame) {
        try {
            M3Stats.onNativeCall();
            _validateResult(_nReadPixels(_ptr, M3Native.getPtr(bitmap), frame, priorFrame));
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(bitmap);
        }
    }

    /**
     * <p>Return the number of frames in the image.</p>
     *
     * <p>May require reading through the stream.</p>
     */
    public int getFrameCount() {
        try {
            M3Stats.onNativeCall();
            return _nGetFrameCount(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Return info about a single frame.</p>
     *
     * <p>Only supported by multi-frame images. Does not read through the stream,
     * so it should be called after getFrameCount() to parse any frames that
     * have not already been parsed.</p>
     */
    public M3AnimationFrameInfo getFrameInfo(int frame) {
        try {
            M3Stats.onNativeCall();
            return _nGetFrameInfo(_ptr, frame);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Return info about all the frames in the image.</p>
     *
     * <p>May require reading through the stream to determine info about the
     * frames (including the count).</p>
     *
     * <p>As such, future decoding calls may require a rewind.</p>
     *
     * <p>For still (non-animated) image codecs, this will return an empty array.</p>
     */
    public M3AnimationFrameInfo[] getFramesInfo() {
        try {
            M3Stats.onNativeCall();
            return _nGetFramesInfo(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Return the number of times to repeat, if this image is animated. This number does not
     * include the first play through of each frame. For example, a repetition count of 4 means
     * that each frame is played 5 times and then the animation stops.</p>
     *
     * <p>It can return -1, a negative number, meaning that the animation
     * should loop forever.</p>
     *
     * <p>May require reading the stream to find the repetition count.</p>
     *
     * <p>As such, future decoding calls may require a rewind.</p>
     *
     * <p>For still (non-animated) image codecs, this will return 0.</p>
     */
    public int getRepetitionCount() {
        try {
            M3Stats.onNativeCall();
            return _nGetRepetitionCount(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>{@link #isAnimated} returns whether the full input is expected to contain an
     * animated image (i.e. more than 1 image frame).  This can be used to
     * disambiguate the meaning of {@link #getRepetitionCount} returning 0</p>
     *
     * <p>Note that in some codecs {@link #getFrameCount()} only returns the number of
     * frames for which all the metadata has been already successfully decoded.
     * Therefore for a partial input {@link #isAnimated()} may return true, even
     * though {@link #getFrameCount()} may temporarily return 1 until more of the
     * input is available.</p>
     *
     * <p>When handling partial input, some codecs may not know until later (e.g.
     * until encountering additional image frames) whether the given image has
     * more than one frame.  Such codecs may initially return null and only later
     * give a definitive true or false answer. GIF format is one example where
     * this may happen.</p>
     *
     * <p>Other codecs may be able to decode the information from the metadata
     * present before the first image frame.  Such codecs should be able to give
     * a definitive true or false answer as soon as they are constructed.  PNG
     * format is one example where this happens.</p>
     *
     * @return  true/false/null (unknown)
     */
    public Boolean isAnimated() {
        try {
            M3Stats.onNativeCall();
            int res = _nIsAnimated(_ptr);
            return res == 0 ? true : res == 1 ? false : null;
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public static void _validateResult(int result) {
        switch (result) {
            case 1: // kIncompleteInput
                throw new IllegalArgumentException("Incomplete input: A partial image was generated.");

            case 2: // kErrorInInput
                throw new IllegalArgumentException("Error in input");

            case 3: // kInvalidConversion
                throw new IllegalArgumentException("Invalid conversion: The generator cannot convert to match the request, ignoring dimensions");

            case 4: // kInvalidScale
                throw new IllegalArgumentException("Invalid scale: The generator cannot scale to requested size");

            case 5: // kInvalidParameters
                throw new IllegalArgumentException("Invalid parameter: Parameters (besides info) are invalid. e.g. NULL pixels, rowBytes too small, etc");

            case 6: // kInvalidInput
                throw new IllegalArgumentException("Invalid input: The input did not contain a valid image");

            case 7: // kCouldNotRewind
                throw new UnsupportedOperationException("Could not rewind: Fulfilling this request requires rewinding the input, which is not supported for this input");

            case 8: // kInternalError
                throw new RuntimeException("Internal error");

            case 9: // kUnimplemented
                throw new UnsupportedOperationException("Unimplemented: This method is not implemented by this codec");
        }
    }

    public static class _FinalizerHolder {
        public static final long PTR = _nGetFinalizer();
    }

    public static native long _nGetFinalizer();
    public static native long _nMakeFromData(long dataPtr);
    public static native M3ImageInfo _nGetImageInfo(long ptr);
    public static native long _nGetSize(long ptr);
    public static native int _nGetEncodedOrigin(long ptr);
    public static native int _nGetEncodedImageFormat(long ptr);
    public static native int _nReadPixels(long ptr, long bitmapPtr, int frame, int priorFrame);
    public static native int _nGetFrameCount(long ptr);
    public static native int _nIsAnimated(long ptr);
    public static native M3AnimationFrameInfo _nGetFrameInfo(long ptr, int frame);
    public static native M3AnimationFrameInfo[] _nGetFramesInfo(long ptr);
    public static native int _nGetRepetitionCount(long ptr);
}
