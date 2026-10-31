package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;

/**
 * <p>Represents {@code GrBackendTexture} in GrDirectContext.h</p>
 */
public class M3BackendTexture extends M3Managed {
    static { M3Library.staticLoad(); }

    public M3BackendTexture(long ptr) {
        super(ptr, _FinalizerHolder.PTR);
    }

    /**
     * <p>Creates GrBackendTexture from GL texture.</p>
     *
     * @param width       width of the texture
     * @param height      height of the texture
     * @param isMipmapped if the passed textureId has a GL mipmap, this should be true, otherwise false
     * @param info        GLTextureInfo describing the GL texture to be used as a backend texture
     * @return            BackendTexture wrapping the passed GL texture
     */
    public static M3BackendTexture makeGL(int width, int height, boolean isMipmapped, M3GLTextureInfo info) {
        M3Stats.onNativeCall();
        return new M3BackendTexture(_nMakeGL(width, height, isMipmapped, info.getTarget(), info.getId(), info.getFormat(), info.isProtected()));
    }

    /**
     * <p>Call this to indicate that the texture parameters have been modified
     * in the GL context externally to Gr context.</p>
     */
    public void glTextureParametersModified() {
        try {
            M3Stats.onNativeCall();
            _nGLTextureParametersModified(M3Native.getPtr(this));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Creates GrBackendTexture from Vulkan image.</p>
     *
     * @param width  width of the texture
     * @param height height of the texture
     * @param info   VkImageInfo describing the Vulkan image to be used as backend texture
     * @return       BackendTexture wrapping the passed Vulkan image
     */
    public static M3BackendTexture makeVulkan(int width, int height, M3VkImageInfo info) {
        M3Stats.onNativeCall();
        return new M3BackendTexture(_nMakeVulkan(width, height, info.getVkImage(), info.getAlloc().getMemory(), info.getAlloc().getOffset(), info.getAlloc().getSize(), info.getAlloc().getFlags(), info.getAlloc().getBackendMemory(), info.getImageTiling(), info.getImageLayout(), info.getFormat(), info.getImageUsageFlags(), info.getSampleCount(), info.getLevelCount(), info.getCurrentQueueFamily(), info.isProtected(), info.getSharingMode(), info.isPartOfSwapchainOrAndroidWindow()));
    }

    public static class _FinalizerHolder {
        public static final long PTR = _nGetFinalizer();
    }

    public static native long _nGetFinalizer();
    public static native long _nMakeGL(int width, int height, boolean isMipmapped, int target, int id, int format, boolean isProtected);
    public static native void _nGLTextureParametersModified(long ptr);
    public static native long _nMakeVulkan(int width, int height, long imagePtr, long deviceMemory, long memoryOffset, long memorySize, int flags, long backendMemory, int imageTiling, int imageLayout, int format, int imageUsageFlags, int sampleCount, int levelCount, int currentQueueFamily, boolean isProtected, int sharingMode, boolean partOfSwapchainOrAndroidWindow);
}