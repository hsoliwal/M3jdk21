package jdk.m3.graphics.shaper;

import java.util.*;
import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.*;

public class M3FontMgrRunIterator extends M3ManagedRunIterator<M3FontRun> {
    static { M3Library.staticLoad(); }

    public M3FontMgrRunIterator(M3ManagedString text, boolean manageText, M3Font font, Iterator<M3LanguageRun> languageRunIterator, M3ShapingOptions opts) {
        super(_nMake(M3Native.getPtr(text), M3Native.getPtr(font), languageRunIterator, opts), text, manageText);
        M3Stats.onNativeCall();
        M3ReferenceUtil.reachabilityFence(text);
        M3ReferenceUtil.reachabilityFence(font);
        M3ReferenceUtil.reachabilityFence(opts);
    }

    public M3FontMgrRunIterator(M3ManagedString text, boolean manageText, M3Font font, M3ShapingOptions opts) {
        this(text, manageText, font, null, opts);
    }

    public M3FontMgrRunIterator(String text, M3Font font, M3ShapingOptions opts) {
        this(new M3ManagedString(text), true, font, null, opts);
    }

    public M3FontMgrRunIterator(String text, M3Font font) {
        this(new M3ManagedString(text), true, font, null, M3ShapingOptions.DEFAULT);
    }

    @Override
    public M3FontRun next() {
        try {
            _nConsume(_ptr);
            return new M3FontRun(_getEndOfCurrentRun(), new M3Font(_nGetCurrentFont(_ptr)));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public static native long _nMake(long textPtr, long fontPtr, Iterator<M3LanguageRun> languageRunIterator, M3ShapingOptions opts);
    public static native long _nGetCurrentFont(long ptr);
}