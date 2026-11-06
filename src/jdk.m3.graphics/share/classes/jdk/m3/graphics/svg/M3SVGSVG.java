package jdk.m3.graphics.svg;

import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.*;

public class M3SVGSVG extends M3SVGContainer {
    static { M3Library.staticLoad(); }

    public M3SVGSVG(long ptr) {
        super(ptr);
    }

    public M3SVGLength getX() {
        try {
            M3Stats.onNativeCall();
            return _nGetX(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3SVGLength getY() {
        try {
            M3Stats.onNativeCall();
            return _nGetY(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3SVGLength getWidth() {
        try {
            M3Stats.onNativeCall();
            return _nGetWidth(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3SVGLength getHeight() {
        try {
            M3Stats.onNativeCall();
            return _nGetHeight(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3SVGPreserveAspectRatio getPreserveAspectRatio() {
        try {
            M3Stats.onNativeCall();
            return _nGetPreserveAspectRatio(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3Rect getViewBox() {
        try {
            M3Stats.onNativeCall();
            return _nGetViewBox(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3Point getIntrinsicSize(M3SVGLengthContext lc) {
        try {
            M3Stats.onNativeCall();
            return _nGetIntrinsicSize(_ptr, lc._width, lc._height, lc._dpi);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3SVGSVG setX(M3SVGLength length) {
        try {
            M3Stats.onNativeCall();
            _nSetX(_ptr, length._value, length._unit.ordinal());
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3SVGSVG setY(M3SVGLength length) {
        try {
            M3Stats.onNativeCall();
            _nSetY(_ptr, length._value, length._unit.ordinal());
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3SVGSVG setWidth(M3SVGLength length) {
        try {
            M3Stats.onNativeCall();
            _nSetWidth(_ptr, length._value, length._unit.ordinal());
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3SVGSVG setHeight(M3SVGLength length) {
        try {
            M3Stats.onNativeCall();
            _nSetHeight(_ptr, length._value, length._unit.ordinal());
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3SVGSVG setPreserveAspectRatio(M3SVGPreserveAspectRatio ratio) {
        try {
            M3Stats.onNativeCall();
            _nSetPreserveAspectRatio(_ptr, ratio._align._value, ratio._scale.ordinal());
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3SVGSVG setViewBox(M3Rect viewBox) {
        try {
            M3Stats.onNativeCall();
            _nSetViewBox(_ptr, viewBox._left, viewBox._top, viewBox._right, viewBox._bottom);
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public static native M3SVGLength _nGetX(long ptr);
    public static native M3SVGLength _nGetY(long ptr);
    public static native M3SVGLength _nGetWidth(long ptr);
    public static native M3SVGLength _nGetHeight(long ptr);
    public static native M3SVGPreserveAspectRatio _nGetPreserveAspectRatio(long ptr);
    public static native M3Rect _nGetViewBox(long ptr);
    public static native M3Point _nGetIntrinsicSize(long ptr, float width, float height, float dpi);
    public static native void _nSetX(long ptr, float value, int unit);
    public static native void _nSetY(long ptr, float value, int unit);
    public static native void _nSetWidth(long ptr, float value, int unit);
    public static native void _nSetHeight(long ptr, float value, int unit);
    public static native void _nSetPreserveAspectRatio(long ptr, int align, int scale);
    public static native void _nSetViewBox(long ptr, float l, float t, float r, float b);
}