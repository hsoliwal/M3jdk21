package jdk.m3.graphics;

import java.util.Objects;
import jdk.m3.graphics.*;

public class M3PathSegment {
    public final M3PathVerb _verb;
    public final M3Point _p0;
    public final M3Point _p1;
    public final M3Point _p2;
    public final M3Point _p3;
    public final float _conicWeight;
    public final boolean _closeLine;
    public final boolean _closedContour;

    public M3PathSegment() {
        this(M3PathVerb.DONE, null, null, null, null, 0.0F, false, false);
    }

    public M3PathSegment(int verbOrdinal, float x0, float y0, boolean isClosedContour) {
        this(M3PathVerb._values[verbOrdinal], new M3Point(x0, y0), null, null, null, 0.0F, false, isClosedContour);
        assert verbOrdinal == M3PathVerb.MOVE.ordinal() || verbOrdinal == M3PathVerb.CLOSE.ordinal() : "Expected MOVE or CLOSE, got " + M3PathVerb._values[verbOrdinal];
    }

    public M3PathSegment(float x0, float y0, float x1, float y1, boolean isCloseLine, boolean isClosedContour) {
        this(M3PathVerb.LINE, new M3Point(x0, y0), new M3Point(x1, y1), null, null, 0.0F, isCloseLine, isClosedContour);
    }

    public M3PathSegment(float x0, float y0, float x1, float y1, float x2, float y2, boolean isClosedContour) {
        this(M3PathVerb.QUAD, new M3Point(x0, y0), new M3Point(x1, y1), new M3Point(x2, y2), null, 0.0F, false, isClosedContour);
    }

    public M3PathSegment(float x0, float y0, float x1, float y1, float x2, float y2, float conicWeight, boolean isClosedContour) {
        this(M3PathVerb.CONIC, new M3Point(x0, y0), new M3Point(x1, y1), new M3Point(x2, y2), null, conicWeight, false, isClosedContour);
    }

    public M3PathSegment(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, boolean isClosedContour) {
        this(M3PathVerb.CUBIC, new M3Point(x0, y0), new M3Point(x1, y1), new M3Point(x2, y2), new M3Point(x3, y3), 0.0F, false, isClosedContour);
    }

    @Override
    public String toString() {
        return "Segment(" + "verb=" + _verb + (_verb != M3PathVerb.DONE ? ", p0=" + _p0 : "") + (_verb == M3PathVerb.LINE || _verb == M3PathVerb.QUAD || _verb == M3PathVerb.CONIC || _verb == M3PathVerb.CUBIC ? ", p1=" + _p1 : "") + (_verb == M3PathVerb.QUAD || _verb == M3PathVerb.CONIC || _verb == M3PathVerb.CUBIC ? ", p2=" + _p2 : "") + (_verb == M3PathVerb.CUBIC ? ", p3=" + _p3 : "") + (_verb == M3PathVerb.CONIC ? ", conicWeight=" + _conicWeight : "") + (_verb == M3PathVerb.LINE ? ", closeLine=" + _closeLine : "") + (_verb != M3PathVerb.DONE ? ", closedContour=" + _closedContour : "") + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        M3PathSegment segment = (M3PathSegment) o;
        return _verb == segment._verb && (_verb != M3PathVerb.DONE ? Objects.equals(_p0, segment._p0) : true) && (_verb == M3PathVerb.LINE || _verb == M3PathVerb.QUAD || _verb == M3PathVerb.CONIC || _verb == M3PathVerb.CUBIC ? Objects.equals(_p1, segment._p1) : true) && (_verb == M3PathVerb.QUAD || _verb == M3PathVerb.CONIC || _verb == M3PathVerb.CUBIC ? Objects.equals(_p2, segment._p2) : true) && (_verb == M3PathVerb.CUBIC ? Objects.equals(_p3, segment._p3) : true) && (_verb == M3PathVerb.CONIC ? Float.compare(segment._conicWeight, _conicWeight) == 0 : true) && (_verb == M3PathVerb.LINE ? _closeLine == segment._closeLine : true) && (_verb != M3PathVerb.DONE ? _closedContour == segment._closedContour : true);
    }

    @Override
    public int hashCode() {
        switch (_verb) {
        case DONE:
            return Objects.hash(_verb);
        case MOVE:
            return Objects.hash(_verb, _p0, _closedContour);
        case LINE:
            return Objects.hash(_verb, _p0, _p1, _closeLine, _closedContour);
        case QUAD:
            return Objects.hash(_verb, _p0, _p1, _p2, _closedContour);
        case CONIC:
            return Objects.hash(_verb, _p0, _p1, _p2, _conicWeight, _closedContour);
        case CUBIC:
            return Objects.hash(_verb, _p0, _p1, _p2, _p3, _closedContour);
        default:
            throw new RuntimeException("Unreachable");
        }
    }

    @java.lang.SuppressWarnings("all")
    public M3PathSegment(final M3PathVerb verb, final M3Point p0, final M3Point p1, final M3Point p2, final M3Point p3, final float conicWeight, final boolean closeLine, final boolean closedContour) {
        this._verb = verb;
        this._p0 = p0;
        this._p1 = p1;
        this._p2 = p2;
        this._p3 = p3;
        this._conicWeight = conicWeight;
        this._closeLine = closeLine;
        this._closedContour = closedContour;
    }

    @java.lang.SuppressWarnings("all")
    public M3PathVerb getVerb() {
        return this._verb;
    }

    @java.lang.SuppressWarnings("all")
    public M3Point getP0() {
        return this._p0;
    }

    @java.lang.SuppressWarnings("all")
    public M3Point getP1() {
        return this._p1;
    }

    @java.lang.SuppressWarnings("all")
    public M3Point getP2() {
        return this._p2;
    }

    @java.lang.SuppressWarnings("all")
    public M3Point getP3() {
        return this._p3;
    }

    @java.lang.SuppressWarnings("all")
    public float getConicWeight() {
        return this._conicWeight;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isCloseLine() {
        return this._closeLine;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isClosedContour() {
        return this._closedContour;
    }
}
