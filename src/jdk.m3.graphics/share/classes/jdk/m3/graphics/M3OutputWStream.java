package jdk.m3.graphics;

import java.io.*;
import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.*;

public class M3OutputWStream extends M3WStream {
    static { M3Library.staticLoad(); }

    public final OutputStream _out;

    public static class _FinalizerHolder {
        public static final long PTR = _nGetFinalizer();
    }

    public M3OutputWStream(OutputStream out) {
        super(_nMake(out), _FinalizerHolder.PTR);
        M3Stats.onNativeCall();
        _out = out;
    }
    public static native long  _nGetFinalizer();
    public static native long  _nMake(OutputStream out);
}
