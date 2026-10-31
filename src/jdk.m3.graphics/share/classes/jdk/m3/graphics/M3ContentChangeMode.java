package jdk.m3.graphics;


public enum M3ContentChangeMode {
    /** Discards surface on change. */
    DISCARD,

    /** Preserves surface on change. */
    RETAIN;

    public static final M3ContentChangeMode[] _values = values();
}