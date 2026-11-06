package jdk.m3.graphics;

import java.nio.ByteBuffer;
import jdk.m3.graphics.M3IRect;
import jdk.m3.graphics.M3ImageInfo;
import jdk.m3.graphics.M3SamplingMode;
import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.*;

public class M3Pixmap extends M3Managed implements M3IHasImageInfo {
    static { M3Library.staticLoad(); }

    public M3Pixmap(long ptr, boolean managed) {
        super(ptr, _FinalizerHolder.PTR, managed);
    }

    public M3ImageInfo _imageInfo = null;

    public M3Pixmap() {
        this(_nMakeNull(), true);
        M3Stats.onNativeCall();
    }

    @Override
    public M3ImageInfo getImageInfo() {
        try {
            if (_imageInfo == null) {
                M3Stats.onNativeCall();
                _imageInfo = _nGetImageInfo(_ptr);
            }
            return _imageInfo;
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public static M3Pixmap make(M3ImageInfo info, ByteBuffer buffer, int rowBytes) {
        return make(info, M3BufferUtil.getPointerFromByteBuffer(buffer), rowBytes);
    }

    public static M3Pixmap make(M3ImageInfo info, long addr, int rowBytes) {
        try {
            long ptr = _nMake(
                info._width, info._height,
                info._colorInfo._colorType.ordinal(),
                info._colorInfo._alphaType.ordinal(),
                M3Native.getPtr(info._colorInfo._colorSpace), addr, rowBytes);
            if (ptr == 0)
                throw new IllegalArgumentException("Failed to create Pixmap.");
            return new M3Pixmap(ptr, true);
        } finally {
            M3ReferenceUtil.reachabilityFence(info._colorInfo._colorSpace);
        }
    }

    public void reset() {
        M3Stats.onNativeCall();
        _nReset(_ptr);
        _imageInfo = null;
        M3ReferenceUtil.reachabilityFence(this);
    }

    public void reset(M3ImageInfo info, long addr, int rowBytes) {
        M3Stats.onNativeCall();
        _nResetWithInfo(_ptr,
            info._width, info._height,
            info._colorInfo._colorType.ordinal(),
            info._colorInfo._alphaType.ordinal(),
            M3Native.getPtr(info._colorInfo._colorSpace), addr, rowBytes);
        _imageInfo = null;
        M3ReferenceUtil.reachabilityFence(this);
        M3ReferenceUtil.reachabilityFence(info._colorInfo._colorSpace);
    }

    public void reset(M3ImageInfo info, ByteBuffer buffer, int rowBytes) {
        reset(info, M3BufferUtil.getPointerFromByteBuffer(buffer), rowBytes);
    }

    public void setColorSpace(M3ColorSpace colorSpace) {
        M3Stats.onNativeCall();
        _nSetColorSpace(_ptr, M3Native.getPtr(colorSpace));
        _imageInfo = null;
        M3ReferenceUtil.reachabilityFence(this);
        M3ReferenceUtil.reachabilityFence(colorSpace);
    }

    public boolean extractSubset(long subsetPtr, M3IRect area) {
        try {
            M3Stats.onNativeCall();
            return _nExtractSubset(_ptr, subsetPtr, area._left, area._top, area._right, area._bottom);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public boolean extractSubset(ByteBuffer buffer, M3IRect area) {
        return extractSubset(M3BufferUtil.getPointerFromByteBuffer(buffer), area);
    }

    public int getRowBytes() {
        M3Stats.onNativeCall();
        try {
            return _nGetRowBytes(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public int getRowBytesAsPixels() {
        M3Stats.onNativeCall();
        try {
            return _nGetRowBytesAsPixels(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public int computeByteSize() {
        M3Stats.onNativeCall();
        try {
            return _nComputeByteSize(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public boolean computeIsOpaque() {
        M3Stats.onNativeCall();
        try {
            return _nComputeIsOpaque(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public int getColor(int x, int y) {
        M3Stats.onNativeCall();
        try {
            return _nGetColor(_ptr, x, y);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3Color4f getColor4f(int x, int y) {
        M3Stats.onNativeCall();
        try {
            return _nGetColor4f(_ptr, x, y);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public float getAlphaF(int x, int y) {
        M3Stats.onNativeCall();
        try {
            return _nGetAlphaF(_ptr, x, y);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public long getAddr() {
        M3Stats.onNativeCall();
        try {
            return _nGetAddr(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public long getAddrAt(int x, int y) {
        M3Stats.onNativeCall();
        try {
            return _nGetAddrAt(_ptr, x, y);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public boolean readPixels(M3ImageInfo info, long addr, int rowBytes) {
        M3Stats.onNativeCall();
        try {
            return _nReadPixels(_ptr,
                info._width, info._height,
                info._colorInfo._colorType.ordinal(),
                info._colorInfo._alphaType.ordinal(),
                M3Native.getPtr(info._colorInfo._colorSpace), addr, rowBytes);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(info._colorInfo._colorSpace);
        }
    }

    public boolean readPixels(M3ImageInfo info, long addr, int rowBytes, int srcX, int srcY) {
        M3Stats.onNativeCall();
        try {
            return _nReadPixelsFromPoint(_ptr,
                info._width, info._height,
                info._colorInfo._colorType.ordinal(),
                info._colorInfo._alphaType.ordinal(),
                M3Native.getPtr(info._colorInfo._colorSpace), addr, rowBytes,
                srcX, srcY);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(info._colorInfo._colorSpace);
        }
    }

    public boolean readPixels(M3Pixmap pixmap) {
        M3Stats.onNativeCall();
        try {
            return _nReadPixelsToPixmap(_ptr, M3Native.getPtr(pixmap));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(pixmap);
        }
    }

    public boolean readPixels(M3Pixmap pixmap, int srcX, int srcY) {
        M3Stats.onNativeCall();
        try {
            return _nReadPixelsToPixmapFromPoint(_ptr, M3Native.getPtr(pixmap), srcX, srcY);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(pixmap);
        }
    }

    public boolean scalePixels(M3Pixmap dstPixmap, M3SamplingMode samplingMode) {
        M3Stats.onNativeCall();
        try {
            return _nScalePixels(_ptr, M3Native.getPtr(dstPixmap), samplingMode._pack());
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(dstPixmap);
        }
    }

    public boolean erase(int color) {
        M3Stats.onNativeCall();
        try {
            return _nErase(_ptr, color);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public boolean erase(int color, M3IRect subset) {
        M3Stats.onNativeCall();
        try {
            return _nEraseSubset(_ptr, color, subset._left, subset._top, subset._right, subset._bottom);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public boolean erase(M3Color4f color) {
        M3Stats.onNativeCall();
        try {
            return _nErase4f(_ptr, color._r, color._g, color._b, color._a);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public boolean erase(M3Color4f color, M3IRect subset) {
        M3Stats.onNativeCall();
        try {
            return _nEraseSubset4f(_ptr, color._r, color._g, color._b, color._a, subset._left, subset._top, subset._right, subset._bottom);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public ByteBuffer getBuffer() {
        return M3BufferUtil.getByteBufferFromPointer(getAddr(), computeByteSize());
    }

    public static class _FinalizerHolder {
        public static final long PTR = _nGetFinalizer();
    }

    public static native long _nGetFinalizer();
    public static native long _nMakeNull();
    public static native long _nMake(int width, int height, int colorType, int alphaType, long colorSpacePtr, long pixelsPtr, int rowBytes);

    public static native void _nReset(long ptr);
    public static native void _nResetWithInfo(long ptr, int width, int height, int colorType, int alphaType, long colorSpacePtr, long pixelsPtr, int rowBytes);
    public static native void _nSetColorSpace(long ptr, long colorSpacePtr);
    public static native boolean _nExtractSubset(long ptr, long subsetPtr, int l, int t, int r, int b);
    public static native M3ImageInfo _nGetImageInfo(long ptr);
    public static native int _nGetRowBytes(long ptr);
    public static native long _nGetAddr(long ptr);
    public static native int _nGetRowBytesAsPixels(long ptr);
    public static native int _nComputeByteSize(long ptr);
    public static native boolean _nComputeIsOpaque(long ptr);
    public static native int _nGetColor(long ptr, int x, int y);
    public static native M3Color4f _nGetColor4f(long ptr, int x, int y);
    public static native float _nGetAlphaF(long ptr, int x, int y);
    public static native long _nGetAddrAt(long ptr, int x, int y);
    // methods related to C++ types(addr8/16/32/64, writable_addr8/16/32/64) not included - not needed
    public static native boolean _nReadPixels(long ptr, int width, int height, int colorType, int alphaType, long colorSpacePtr, long dstPixelsPtr, int dstRowBytes);
    public static native boolean _nReadPixelsFromPoint(long ptr, int width, int height, int colorType, int alphaType, long colorSpacePtr, long dstPixelsPtr, int dstRowBytes, int srcX, int srcY);
    public static native boolean _nReadPixelsToPixmap(long ptr, long dstPixmapPtr);
    public static native boolean _nReadPixelsToPixmapFromPoint(long ptr, long dstPixmapPtr, int srcX, int srcY);
    public static native boolean _nScalePixels(long ptr, long dstPixmapPtr, long samplingOptions);
    public static native boolean _nErase(long ptr, int color);
    public static native boolean _nEraseSubset(long ptr, int color, int l, int t, int r, int b);
    public static native boolean _nErase4f(long ptr, float r, float g, float b, float a);
    public static native boolean _nEraseSubset4f(long ptr, float r, float g, float b, float a, int l, int t, int r1, int b1);
}