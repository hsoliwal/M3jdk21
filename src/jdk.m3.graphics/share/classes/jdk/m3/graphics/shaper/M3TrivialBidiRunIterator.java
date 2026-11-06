package jdk.m3.graphics.shaper;

import java.util.*;
import jdk.m3.graphics.*;

public class M3TrivialBidiRunIterator implements Iterator<M3BidiRun> {
    public final int _length;
    public final int _level;
    public boolean _atEnd;
    public M3TrivialBidiRunIterator(String text, int level) {
        _length = text.length();
        _level  = level;
        _atEnd  = _length == 0;
    }

    @Override
    public M3BidiRun next() {
        _atEnd = true;
        return new M3BidiRun(_length, _level);
    }

    @Override
    public boolean hasNext() {
        return !_atEnd;
    }
}