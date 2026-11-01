package jdk.m3.graphics.skottie;


// Skottie.h - Animation::Builder::Flags
public enum M3AnimationBuilderFlag {
    /**
     * Normally, all static image frames are resolved at
     * load time via ImageAsset::getFrame(0).  With this flag,
     * frames are only resolved when needed, at seek() time.
     */
    DEFER_IMAGE_LOADING(0x01),

    /**
     * Attempt to use the embedded fonts (glyph paths,
     * normally used as fallback) over native Skia typefaces.
     */
    PREFER_EMBEDDED_FONTS(0x02);

    public static final M3AnimationBuilderFlag[] _values = values();

    public final int _flag;

    M3AnimationBuilderFlag(int flag) {
        this._flag = flag;
    }

    public static int _collect(M3AnimationBuilderFlag... flags) {
        int value = 0;
        for (M3AnimationBuilderFlag flag: flags)
            value |= flag._flag;
        return value;
    }
}