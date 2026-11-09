package jdk.m3.graphics.svg;

import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.*;

public abstract class M3SVGNode extends M3RefCnt {
    static { M3Library.staticLoad(); }

    public M3SVGNode(long ptr) {
        super(ptr);
    }

    public M3SVGTag getTag() {
        try {
            M3Stats.onNativeCall();
            return M3SVGTag._values[_nGetTag(_ptr)];
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public static native int _nGetTag(long ptr);
}