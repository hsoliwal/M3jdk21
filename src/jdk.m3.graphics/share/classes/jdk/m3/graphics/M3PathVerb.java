package jdk.m3.graphics;


// SkPathTypes.h - SkPathVerb
/**
 * Verb instructs Path how to interpret one or more Point and optional conic weight;
 * manage contour, and terminate Path.
 */
public enum M3PathVerb {
    /** iter.next returns 1 point */
    MOVE,

    /** iter.next returns 2 points */
    LINE,

    /** iter.next returns 3 points */
    QUAD,

    /** iter.next returns 3 points + iter.conicWeight() */
    CONIC,

    /** iter.next returns 4 points */
    CUBIC,

    /** iter.next returns 1 point (contour's moveTo pt) */
    CLOSE,

    /** iter.next returns 0 points */
    DONE;

    public static final M3PathVerb[] _values = values();
}
