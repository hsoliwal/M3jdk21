package jdk.m3.graphics.shaper;

import java.text.*;
import java.util.*;
import jdk.m3.graphics.*;

public class M3JavaTextBidiRunIterator implements Iterator<M3BidiRun> {
    public final Bidi _bidi;
    public final int _runsCount;
    public int _run;

    public M3JavaTextBidiRunIterator(String text) {
        this(text, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
    }

    public M3JavaTextBidiRunIterator(String text, int flags) {
        _bidi = new Bidi(text, flags);
        _runsCount = _bidi.getRunCount();
        _run = -1;
    }

    @Override
    public M3BidiRun next() {
        _run++;
        return new M3BidiRun(_bidi.getRunLimit(_run), _bidi.getRunLevel(_run));
    }

    @Override
    public boolean hasNext() {
        return _run + 1 < _runsCount;
    }
}