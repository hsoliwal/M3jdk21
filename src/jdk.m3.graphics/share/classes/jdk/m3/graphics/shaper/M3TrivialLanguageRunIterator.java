package jdk.m3.graphics.shaper;

import java.util.*;
import jdk.m3.graphics.*;

public class M3TrivialLanguageRunIterator implements Iterator<M3LanguageRun> {
    public final int     _length;
    public final String  _language;
    public       boolean _atEnd;
    public M3TrivialLanguageRunIterator(String text, String language) {
        _length   = text.length();
        _language = language;
        _atEnd    = _length == 0;
    }

    @Override
    public M3LanguageRun next() {
        _atEnd = true;
        return new M3LanguageRun(_length, _language);
    }

    @Override
    public boolean hasNext() {
        return !_atEnd;
    }
}