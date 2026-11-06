package jdk.m3.graphics.shaper;

import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.*;

public class M3HbIcuScriptRunIterator extends M3ManagedRunIterator<M3ScriptRun> {
    static { M3Library.staticLoad(); }

    public M3HbIcuScriptRunIterator(M3ManagedString text, boolean manageText) {
        super(_nMake(M3Native.getPtr(text)), text, manageText);
        M3Stats.onNativeCall();
        M3ReferenceUtil.reachabilityFence(text);
    }

    public M3HbIcuScriptRunIterator(String text) {
        this(new M3ManagedString(text), true);
    }

    @Override
    public M3ScriptRun next() {
        try {
            _nConsume(_ptr);
            return new M3ScriptRun(_getEndOfCurrentRun(), _nGetCurrentScriptTag(_ptr));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public static native long _nMake(long textPtr);
    public static native int _nGetCurrentScriptTag(long ptr);
}