package jdk.m3.graphics;


public enum M3FilterBlurMode {
    /** fuzzy inside and outside */
    NORMAL,
    /** solid inside, fuzzy outside */
    SOLID,
    /** nothing inside, fuzzy outside */
    OUTER,
    /** fuzzy inside, nothing outside */
    INNER;

    public static final M3FilterBlurMode[] _values = values();
}
