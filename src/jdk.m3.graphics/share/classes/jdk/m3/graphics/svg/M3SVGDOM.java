package jdk.m3.graphics.svg;

import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.*;

public class M3SVGDOM extends M3RefCnt {
    static { M3Library.staticLoad(); }

    public M3SVGDOM(M3Data data) {
        this(_nMakeFromData(M3Native.getPtr(data)));
        M3Stats.onNativeCall();
        M3ReferenceUtil.reachabilityFence(data);
    }

    public M3SVGSVG getRoot() {
        try {
            M3Stats.onNativeCall();
            long ptr = _nGetRoot(_ptr);
            return ptr == 0 ? null : new M3SVGSVG(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Deprecated. Use getRoot().getIntrinsicSize() instead
     */
    @Deprecated
    public M3Point getContainerSize() {
        try {
            return _nGetContainerSize(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3SVGDOM setContainerSize(float width, float height) {
        M3Stats.onNativeCall();
        _nSetContainerSize(_ptr, width, height);
        return this;
    }

    public M3SVGDOM setContainerSize(M3Point size) {
        M3Stats.onNativeCall();
        _nSetContainerSize(_ptr, size._x, size._y);
        return this;
    }

    // sk_sp<SkSVGNode>* findNodeById(const char* id);

    public M3SVGDOM render(M3Canvas canvas) {
        try {
            M3Stats.onNativeCall();
            _nRender(_ptr, M3Native.getPtr(canvas));
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(canvas);
        }
    }

    public M3SVGDOM(long ptr) {
        super(ptr);
    }

    public static native long  _nMakeFromData(long dataPtr);
    public static native long  _nGetRoot(long ptr);
    public static native M3Point _nGetContainerSize(long ptr);
    public static native void  _nSetContainerSize(long ptr, float width, float height);
    public static native void  _nRender(long ptr, long canvasPtr);
}