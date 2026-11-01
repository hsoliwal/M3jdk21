package jdk.m3.graphics;

/**
 * Configuration flags for {@link VulkanAlloc}, represented as bitmask bits.
 */
public enum M3VulkanAllocFlag {
    /**
     * <p>No special memory properties are requested.</p>
     */
    NONE(0), /**
     * <p>Memory must be flushed to device after mapping.</p>
     */
    NONCOHERENT(1), /**
     * <p>Memory is able to be mapped.</p>
     */
    MAPPABLE(1 << 1), /**
     * <p>Memory was created with lazy allocation.</p>
     */
    LAZILY_ALLOCATED(1 << 2);
    private final int _value;

    public static int toBitmask(M3VulkanAllocFlag... flags) {
        int combined = 0;
        for (M3VulkanAllocFlag flag : flags) {
            combined |= flag._value;
        }
        return combined;
    }

    public static M3VulkanAllocFlag valueOf(int value) {
        for (M3VulkanAllocFlag flag : M3VulkanAllocFlag.values()) {
            if (flag._value == value) {
                return flag;
            }
        }
        return null;
    }

    @java.lang.SuppressWarnings("all")
    private M3VulkanAllocFlag(final int value) {
        this._value = value;
    }

    @java.lang.SuppressWarnings("all")
    public int getValue() {
        return this._value;
    }
}
