package jdk.m3.graphics;


/**
 * @deprecated - use {@link M3EncoderJPEG}, {@link M3EncoderPNG} or {@link M3EncoderWEBP} directly
 */
@Deprecated
public enum M3EncodedImageFormat {
    BMP,
    GIF,
    ICO,
    JPEG,
    PNG,
    WBMP,
    WEBP,
    PKM,
    KTX,
    ASTC,
    DNG,
    HEIF,
    AVIF,
    JPEGXL;

    public static final M3EncodedImageFormat[] _values = values();
}
