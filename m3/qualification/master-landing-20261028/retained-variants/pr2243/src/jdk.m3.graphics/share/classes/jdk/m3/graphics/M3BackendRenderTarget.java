package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;

public class M3BackendRenderTarget extends M3Managed {
    static { M3Library.staticLoad(); }
    public static M3BackendRenderTarget makeGL(int width, int height, int sampleCnt, int stencilBits, int fbId, int fbFormat) {
        M3Stats.onNativeCall();
        return new M3BackendRenderTarget(_nMakeGL(width, height, sampleCnt, stencilBits, fbId, fbFormat));
    }

    public static M3BackendRenderTarget makeMetal(int width, int height, long texturePtr) {
        M3Stats.onNativeCall();
        return new M3BackendRenderTarget(_nMakeMetal(width, height, texturePtr));
    }

    /**
     * <p>Creates Direct3D backend render target object from D3D12 texture resource.</p>
     * <p>For more information refer to skia GrBackendRenderTarget class.</p>
     *
     * @param width         width of the render target in pixels
     * @param height        height of the render target in pixels
     * @param texturePtr    pointer to ID3D12Resource texture resource object; must be not zero
     * @param format        pixel data DXGI_FORMAT fromat of the texturePtr resource
     * @param sampleCnt     samples count for texture resource
     * @param levelCnt      sampling quality level for texture resource
     */
    public static M3BackendRenderTarget makeDirect3D(int width, int height, long texturePtr, int format, int sampleCnt, int levelCnt) {
        M3Stats.onNativeCall();
        return new M3BackendRenderTarget(_nMakeDirect3D(width, height, texturePtr, format, sampleCnt, levelCnt));
    }

    public static M3BackendRenderTarget makeVulkan(int width, int height, long imagePtr, int imageTiling, int imageLayout, int format, int imageUsageFlags, int sampleCnt, int levelCnt) {
        M3Stats.onNativeCall();
        return new M3BackendRenderTarget(_nMakeVulkan(width, height, imagePtr, imageTiling, imageLayout, format, imageUsageFlags, sampleCnt, levelCnt));
    }

    public M3BackendRenderTarget(long ptr) {
        super(ptr, _FinalizerHolder.PTR);
    }

    public static class _FinalizerHolder {
        public static final long PTR = _nGetFinalizer();
    }

    public static native long _nGetFinalizer();
    public static native long _nMakeGL(int width, int height, int sampleCnt, int stencilBits, int fbId, int fbFormat);
    public static native long _nMakeMetal(int width, int height, long texturePtr);
    public static native long _nMakeDirect3D(int width, int height, long texturePtr, int format, int sampleCnt, int levelCnt);
    public static native long _nMakeVulkan(int width, int height, long imagePtr, int imageTiling, int imageLayout, int format, int imageUsageFlags, int sampleCnt, int levelCnt);

}