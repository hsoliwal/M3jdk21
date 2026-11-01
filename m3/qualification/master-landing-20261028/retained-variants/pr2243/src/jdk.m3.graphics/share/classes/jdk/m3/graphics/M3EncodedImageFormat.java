package jdk.m3.graphics;


/**
 * @deprecated - use {@link EncoderJPEG}, {@link EncoderPNG} or {@link EncoderWEBP} directly
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
