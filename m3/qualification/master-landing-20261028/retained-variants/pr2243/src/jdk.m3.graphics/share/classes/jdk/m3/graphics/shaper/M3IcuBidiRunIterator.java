package jdk.m3.graphics.shaper;

import java.util.*;
import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.*;

public class M3IcuBidiRunIterator extends M3ManagedRunIterator<M3BidiRun> {
    static { M3Library.staticLoad(); }

    public M3IcuBidiRunIterator(M3ManagedString text, boolean manageText, int bidiLevel) {
        super(_nMake(M3Native.getPtr(text), bidiLevel), text, manageText);
        M3Stats.onNativeCall();
        M3ReferenceUtil.reachabilityFence(text);
    }

    public M3IcuBidiRunIterator(String text, int bidiLevel) {
        this(new M3ManagedString(text), true, bidiLevel);
    }

    @Override
    public M3BidiRun next() {
        try {
            _nConsume(_ptr);
            return new M3BidiRun(_getEndOfCurrentRun(), _nGetCurrentLevel(_ptr));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public static native long _nMake(long textPtr, int bidiLevel);
    public static native int _nGetCurrentLevel(long ptr);
}