package jdk.m3.graphics.skottie;


// Skottie.h - Animation::RenderFlag
public enum M3RenderFlag {
    /**
     * When rendering into a known transparent buffer, clients can pass
     * this flag to avoid some unnecessary compositing overhead for
     * animations using layer blend modes.
     */
    SKIP_TOP_LEVEL_ISOLATION(0x01),

    /**
     * By default, content is clipped to the intrinsic animation
     * bounds (as determined by its size).  If this flag is set,
     * then the animation can draw outside of the bounds.
     */
    DISABLE_TOP_LEVEL_CLIPPING(0x02);

    public static final M3RenderFlag[] _values = values();

    public final int _flag;

    M3RenderFlag(int flag) {
        this._flag = flag;
    }
}