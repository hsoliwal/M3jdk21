package jdk.m3.graphics;

/**
 * <p>Represents the struct {@code GrVkImageInfo} in GrVkTypes.h</p>
 *
 * <p>When wrapping a BackendTexture or BackendRendenderTarget, the {@code currentQueueFamily} should
 * either be {@code VK_QUEUE_FAMILY_IGNORED}, {@code VK_QUEUE_FAMILY_EXTERNAL}, or {@code VK_QUEUE_FAMILY_FOREIGN_EXT}. If
 * {@code sharingMode} is {@code VK_SHARING_MODE_EXCLUSIVE} then {@code currentQueueFamily} can also be the graphics
 * queue index passed into Skia.</p>
 */
public final class M3VkImageInfo {
    private final long _vkImage;
    /**
     * @see VulkanAlloc
     */
    private final M3VulkanAlloc _alloc;
    private final int _imageTiling;
    private final int _imageLayout;
    private final int _format;
    private final int _imageUsageFlags;
    private final int _sampleCount;
    private final int _levelCount;
    private final int _currentQueueFamily;
    private final boolean _isProtected;
    private final int _sharingMode;
    private final boolean _partOfSwapchainOrAndroidWindow;

    public M3VkImageInfo(long vkImage, M3VulkanAlloc alloc, int imageTiling, int imageLayout, int format, int imageUsageFlags, int sampleCount, int levelCount) {
        this(vkImage, alloc, imageTiling, imageLayout, format, imageUsageFlags, sampleCount, levelCount, (~0), false, 0, false);
    }

    public M3VkImageInfo(long vkImage, M3VulkanAlloc alloc, int imageTiling, int imageLayout, int format, int imageUsageFlags, int sampleCount, int levelCount, int currentQueueFamily, boolean isProtected, int sharingMode) {
        this(vkImage, alloc, imageTiling, imageLayout, format, imageUsageFlags, sampleCount, levelCount, currentQueueFamily, isProtected, sharingMode, false);
    }

    /**
     * Creates a new {@code VkImageInfo} instance.
     *
     * @param vkImage
     * @param alloc @see VulkanAlloc
     * @param imageTiling
     * @param imageLayout
     * @param format
     * @param imageUsageFlags
     * @param sampleCount
     * @param levelCount
     * @param currentQueueFamily
     * @param isProtected
     * @param sharingMode
     * @param partOfSwapchainOrAndroidWindow
     */
    @java.lang.SuppressWarnings("all")
    public M3VkImageInfo(final long vkImage, final M3VulkanAlloc alloc, final int imageTiling, final int imageLayout, final int format, final int imageUsageFlags, final int sampleCount, final int levelCount, final int currentQueueFamily, final boolean isProtected, final int sharingMode, final boolean partOfSwapchainOrAndroidWindow) {
        this._vkImage = vkImage;
        this._alloc = alloc;
        this._imageTiling = imageTiling;
        this._imageLayout = imageLayout;
        this._format = format;
        this._imageUsageFlags = imageUsageFlags;
        this._sampleCount = sampleCount;
        this._levelCount = levelCount;
        this._currentQueueFamily = currentQueueFamily;
        this._isProtected = isProtected;
        this._sharingMode = sharingMode;
        this._partOfSwapchainOrAndroidWindow = partOfSwapchainOrAndroidWindow;
    }

    @java.lang.SuppressWarnings("all")
    public long getVkImage() {
        return this._vkImage;
    }

    /**
     * @see VulkanAlloc
     */
    @java.lang.SuppressWarnings("all")
    public M3VulkanAlloc getAlloc() {
        return this._alloc;
    }

    @java.lang.SuppressWarnings("all")
    public int getImageTiling() {
        return this._imageTiling;
    }

    @java.lang.SuppressWarnings("all")
    public int getImageLayout() {
        return this._imageLayout;
    }

    @java.lang.SuppressWarnings("all")
    public int getFormat() {
        return this._format;
    }

    @java.lang.SuppressWarnings("all")
    public int getImageUsageFlags() {
        return this._imageUsageFlags;
    }

    @java.lang.SuppressWarnings("all")
    public int getSampleCount() {
        return this._sampleCount;
    }

    @java.lang.SuppressWarnings("all")
    public int getLevelCount() {
        return this._levelCount;
    }

    @java.lang.SuppressWarnings("all")
    public int getCurrentQueueFamily() {
        return this._currentQueueFamily;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isProtected() {
        return this._isProtected;
    }

    @java.lang.SuppressWarnings("all")
    public int getSharingMode() {
        return this._sharingMode;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isPartOfSwapchainOrAndroidWindow() {
        return this._partOfSwapchainOrAndroidWindow;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3VkImageInfo)) return false;
        final M3VkImageInfo other = (M3VkImageInfo) o;
        if (this.getVkImage() != other.getVkImage()) return false;
        if (this.getImageTiling() != other.getImageTiling()) return false;
        if (this.getImageLayout() != other.getImageLayout()) return false;
        if (this.getFormat() != other.getFormat()) return false;
        if (this.getImageUsageFlags() != other.getImageUsageFlags()) return false;
        if (this.getSampleCount() != other.getSampleCount()) return false;
        if (this.getLevelCount() != other.getLevelCount()) return false;
        if (this.getCurrentQueueFamily() != other.getCurrentQueueFamily()) return false;
        if (this.isProtected() != other.isProtected()) return false;
        if (this.getSharingMode() != other.getSharingMode()) return false;
        if (this.isPartOfSwapchainOrAndroidWindow() != other.isPartOfSwapchainOrAndroidWindow()) return false;
        final java.lang.Object this$_alloc = this.getAlloc();
        final java.lang.Object other$_alloc = other.getAlloc();
        if (this$_alloc == null ? other$_alloc != null : !this$_alloc.equals(other$_alloc)) return false;
        return true;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $_vkImage = this.getVkImage();
        result = result * PRIME + (int) ($_vkImage >>> 32 ^ $_vkImage);
        result = result * PRIME + this.getImageTiling();
        result = result * PRIME + this.getImageLayout();
        result = result * PRIME + this.getFormat();
        result = result * PRIME + this.getImageUsageFlags();
        result = result * PRIME + this.getSampleCount();
        result = result * PRIME + this.getLevelCount();
        result = result * PRIME + this.getCurrentQueueFamily();
        result = result * PRIME + (this.isProtected() ? 79 : 97);
        result = result * PRIME + this.getSharingMode();
        result = result * PRIME + (this.isPartOfSwapchainOrAndroidWindow() ? 79 : 97);
        final java.lang.Object $_alloc = this.getAlloc();
        result = result * PRIME + ($_alloc == null ? 43 : $_alloc.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "VkImageInfo(_vkImage=" + this.getVkImage() + ", _alloc=" + this.getAlloc() + ", _imageTiling=" + this.getImageTiling() + ", _imageLayout=" + this.getImageLayout() + ", _format=" + this.getFormat() + ", _imageUsageFlags=" + this.getImageUsageFlags() + ", _sampleCount=" + this.getSampleCount() + ", _levelCount=" + this.getLevelCount() + ", _currentQueueFamily=" + this.getCurrentQueueFamily() + ", _isProtected=" + this.isProtected() + ", _sharingMode=" + this.getSharingMode() + ", _partOfSwapchainOrAndroidWindow=" + this.isPartOfSwapchainOrAndroidWindow() + ")";
    }
}
