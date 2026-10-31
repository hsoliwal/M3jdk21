package jdk.m3.graphics.resources;

import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.*;

public class M3FileResourceProvider extends M3ResourceProvider {
    static { M3Library.staticLoad(); }

    public M3FileResourceProvider(long ptr) {
        super(ptr);
    }

    public static M3FileResourceProvider make(String baseDir) {
        return make(baseDir, false);
    }

    public static M3FileResourceProvider make(String baseDir, boolean predecode) {
        assert baseDir != null : "Can’t FileResourceProvider::make with baseDir == null";
        M3Stats.onNativeCall();
        return new M3FileResourceProvider(_nMake(baseDir, predecode));
    }

    public static native long _nMake(String baseDir, boolean predecode);
}