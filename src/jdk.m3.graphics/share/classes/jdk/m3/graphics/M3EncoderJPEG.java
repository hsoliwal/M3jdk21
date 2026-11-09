package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;

public class M3EncoderJPEG {
    /**
     * Encode the provided image and return the resulting bytes.
     *
     * @param image  image to encode
     * @return       nullptr if the pixels could not be read or encoding otherwise fails.
     */
    public static M3Data encode(M3Image image) {
        return encode(null, image, M3EncodeJPEGOptions.DEFAULT);
    }

    /**
     * Encode the provided image and return the resulting bytes.
     *
     * @param image  image to encode
     * @param opts   may be used to control the encoding behavior
     * @return       nullptr if the pixels could not be read or encoding otherwise fails.
     */
    public static M3Data encode(M3Image image, M3EncodeJPEGOptions opts) {
        return encode(null, image, opts);
    }

    /**
     * Encode the provided image and return the resulting Data.
     *
     * @param ctx    If the image was created as a texture-backed image on a GPU context,
     *               that ctx must be provided so the pixels can be read before being encoded.
     *               For raster-backed images, ctx can be null.
     * @param image  image to encode
     * @param opts   may be used to control the encoding behavior
     * @return       nullptr if the pixels could not be read or encoding otherwise fails.
     */
    public static M3Data encode(M3DirectContext ctx, M3Image image, M3EncodeJPEGOptions opts) {
        try {
            assert image != null : "Can’t EncoderJPEG::encode with image == null";
            assert opts != null : "Can’t EncoderJPEG::encode with opts == null";
            M3Stats.onNativeCall();
            long ptr = _nEncode(M3Native.getPtr(ctx), M3Native.getPtr(image), opts._quality, opts._downsampleMode.ordinal(), opts._alphaMode.ordinal());
            return ptr == 0 ? null : new M3Data(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(ctx);
            M3ReferenceUtil.reachabilityFence(image);
        }
    }

    public static native long _nEncode(long ctxPtr, long imagePtr, int quality, int downsampleMode, int alphaMode);
}
