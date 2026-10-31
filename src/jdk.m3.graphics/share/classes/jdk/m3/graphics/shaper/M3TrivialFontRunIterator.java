package jdk.m3.graphics.shaper;

import java.util.*;
import jdk.m3.graphics.*;

public class M3TrivialFontRunIterator implements Iterator<M3FontRun> {
    public final int     _length;
    public final M3Font    _font;
    public       boolean _atEnd;
    public M3TrivialFontRunIterator(String text, M3Font font) {
        _length = text.length();
        _font   = font;
        _atEnd  = _length == 0;
    }

    @Override
    public M3FontRun next() {
        _atEnd = true;
        return new M3FontRun(_length, _font);
    }

    @Override
    public boolean hasNext() {
        return !_atEnd;
    }
}