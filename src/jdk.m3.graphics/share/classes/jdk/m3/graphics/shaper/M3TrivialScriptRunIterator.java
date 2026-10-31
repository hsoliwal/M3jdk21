package jdk.m3.graphics.shaper;

import java.util.*;
import jdk.m3.graphics.*;

public class M3TrivialScriptRunIterator implements Iterator<M3ScriptRun> {
    public final int     _length;
    public final String  _script;
    public       boolean _atEnd;
    public M3TrivialScriptRunIterator(String text, String script) {
        _length = text.length();
        _script = script;
        _atEnd  = _length == 0;
    }

    @Override
    public M3ScriptRun next() {
        _atEnd = true;
        return new M3ScriptRun(_length, _script);
    }

    @Override
    public boolean hasNext() {
        return !_atEnd;
    }
}