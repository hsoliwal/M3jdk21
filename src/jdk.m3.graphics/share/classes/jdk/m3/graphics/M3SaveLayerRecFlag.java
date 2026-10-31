package jdk.m3.graphics;


// SkCanvas.h - SkCanvas::SaveLayerFlagsSet
public enum M3SaveLayerRecFlag {
    PRESERVE_LCD_TEXT(1 << 1),

    /**
     * initializes with previous contents
     */
    INIT_WITH_PREVIOUS(1 << 2),

    /**
     * instead of matching previous layer's colortype, use F16
     */
    F16_COLOR_TYPE(1 << 4);

    public static final M3SaveLayerRecFlag[] _values = values();

    public final int _flag;

    M3SaveLayerRecFlag(int flag) {
        this._flag = flag;
    }
}