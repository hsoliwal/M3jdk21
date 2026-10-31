package jdk.m3.graphics;

// SkShadowUtils.h - SkShadowFlags
public enum M3ShadowUtilsFlag {
    /**
     * No shadow flags set
     */
    NONE(0), /**
     * The occluding object is not opaque. Knowing that the occluder is opaque allows
     * us to cull shadow geometry behind it and improve performance.
     */
    TRANSPARENT_OCCLUDER(1), /**
     * Don't try to use analytic shadows.
     */
    GEOMETRIC_ONLY(2), /**
     * Light position represents a direction, light radius is blur radius at elevation 1
     */
    DIRECTIONAL_LIGHT(4), /**
     * Concave paths will only use blur to generate the shadow
     */
    CONCAVE_BLUR_ONLY(8), /**
     * All shadow flags set
     */
    ALL(15);
    public final int _value;

    M3ShadowUtilsFlag(int value) {
        this._value = value;
    }

    public static int _collect(M3ShadowUtilsFlag... flags) {
        int value = 0;
        for (M3ShadowUtilsFlag flag : flags) value |= flag._value;
        return value;
    }

    @java.lang.SuppressWarnings("all")
    public int getValue() {
        return this._value;
    }
}
