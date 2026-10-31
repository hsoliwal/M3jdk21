package jdk.m3.graphics;

/**
 * <p>Represents the struct {@code VulkanAlloc} in VulkanTypes.h</p>
 */
public final class M3VulkanAlloc {
    /**
     * <p>The native Vulkan device memory handle ({@code VkDeviceMemory}).</p>
     *
     * <p>Can be {@code VK_NULL_HANDLE} iff is an RT and is borrowed</p>
     */
    private final long _memory;
    /**
     * <p>Offset into the native memory allocation ({@code VkDeviceSize}).</p>
     */
    private final long _offset;
    /**
     * <p>Size of the native memory allocation ({@code VkDeviceSize}).</p>
     *
     * <p>This can be indeterminate iff Tex uses borrow semantics.</p>
     */
    private final long _size;
    /**
     * <p>Flat bitmask representing Skia-specific custom allocation states.</p>
     *
     * <p><b>CRITICAL:</b> This field expects a bitmask combined from {@link VulkanAllocFlag},
     * <i>not</i> native Vulkan driver property flags (such as {@code VkMemoryPropertyFlags}).
     */
    private final int _flags;
    /**
     * <p>Opaque handle or pointer to the native memory allocator block.</p>
     *
     * <p>Typically represents a handle managed via {@code skgpu::VulkanMemoryAllocator} (e.g., VMA).</p>
     */
    private final long _backendMemory;

    public M3VulkanAlloc(long memory, long offset, long size, M3VulkanAllocFlag... flags) {
        this(memory, offset, size, M3VulkanAllocFlag.toBitmask(flags), 0L);
    }

    public M3VulkanAlloc(long memory, long offset, long size, long backendMemory, M3VulkanAllocFlag... flags) {
        this(memory, offset, size, M3VulkanAllocFlag.toBitmask(flags), backendMemory);
    }

    public boolean hasFlag(M3VulkanAllocFlag flag) {
        if (flag == M3VulkanAllocFlag.NONE) return _flags == 0;
        return (_flags & flag.getValue()) != 0;
    }

    /**
     * <p>The native Vulkan device memory handle ({@code VkDeviceMemory}).</p>
     *
     * <p>Can be {@code VK_NULL_HANDLE} iff is an RT and is borrowed</p>
     */
    @java.lang.SuppressWarnings("all")
    public long getMemory() {
        return this._memory;
    }

    /**
     * <p>Offset into the native memory allocation ({@code VkDeviceSize}).</p>
     */
    @java.lang.SuppressWarnings("all")
    public long getOffset() {
        return this._offset;
    }

    /**
     * <p>Size of the native memory allocation ({@code VkDeviceSize}).</p>
     *
     * <p>This can be indeterminate iff Tex uses borrow semantics.</p>
     */
    @java.lang.SuppressWarnings("all")
    public long getSize() {
        return this._size;
    }

    /**
     * <p>Flat bitmask representing Skia-specific custom allocation states.</p>
     *
     * <p><b>CRITICAL:</b> This field expects a bitmask combined from {@link VulkanAllocFlag},
     * <i>not</i> native Vulkan driver property flags (such as {@code VkMemoryPropertyFlags}).
     */
    @java.lang.SuppressWarnings("all")
    public int getFlags() {
        return this._flags;
    }

    /**
     * <p>Opaque handle or pointer to the native memory allocator block.</p>
     *
     * <p>Typically represents a handle managed via {@code skgpu::VulkanMemoryAllocator} (e.g., VMA).</p>
     */
    @java.lang.SuppressWarnings("all")
    public long getBackendMemory() {
        return this._backendMemory;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3VulkanAlloc)) return false;
        final M3VulkanAlloc other = (M3VulkanAlloc) o;
        if (this.getMemory() != other.getMemory()) return false;
        if (this.getOffset() != other.getOffset()) return false;
        if (this.getSize() != other.getSize()) return false;
        if (this.getFlags() != other.getFlags()) return false;
        if (this.getBackendMemory() != other.getBackendMemory()) return false;
        return true;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $_memory = this.getMemory();
        result = result * PRIME + (int) ($_memory >>> 32 ^ $_memory);
        final long $_offset = this.getOffset();
        result = result * PRIME + (int) ($_offset >>> 32 ^ $_offset);
        final long $_size = this.getSize();
        result = result * PRIME + (int) ($_size >>> 32 ^ $_size);
        result = result * PRIME + this.getFlags();
        final long $_backendMemory = this.getBackendMemory();
        result = result * PRIME + (int) ($_backendMemory >>> 32 ^ $_backendMemory);
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "VulkanAlloc(_memory=" + this.getMemory() + ", _offset=" + this.getOffset() + ", _size=" + this.getSize() + ", _flags=" + this.getFlags() + ", _backendMemory=" + this.getBackendMemory() + ")";
    }

    @java.lang.SuppressWarnings("all")
    public M3VulkanAlloc() {
        this._memory = 0L;
        this._offset = 0L;
        this._size = 0L;
        this._flags = 0;
        this._backendMemory = 0L;
    }

    /**
     * Creates a new {@code VulkanAlloc} instance.
     *
     * @param memory <p>The native Vulkan device memory handle ({@code VkDeviceMemory}).</p>
     *
     * <p>Can be {@code VK_NULL_HANDLE} iff is an RT and is borrowed</p>
     * @param offset <p>Offset into the native memory allocation ({@code VkDeviceSize}).</p>
     * @param size <p>Size of the native memory allocation ({@code VkDeviceSize}).</p>
     *
     * <p>This can be indeterminate iff Tex uses borrow semantics.</p>
     * @param flags <p>Flat bitmask representing Skia-specific custom allocation states.</p>
     *
     * <p><b>CRITICAL:</b> This field expects a bitmask combined from {@link VulkanAllocFlag},
     * <i>not</i> native Vulkan driver property flags (such as {@code VkMemoryPropertyFlags}).
     * @param backendMemory <p>Opaque handle or pointer to the native memory allocator block.</p>
     *
     * <p>Typically represents a handle managed via {@code skgpu::VulkanMemoryAllocator} (e.g., VMA).</p>
     */
    @java.lang.SuppressWarnings("all")
    public M3VulkanAlloc(final long memory, final long offset, final long size, final int flags, final long backendMemory) {
        this._memory = memory;
        this._offset = offset;
        this._size = size;
        this._flags = flags;
        this._backendMemory = backendMemory;
    }
}
