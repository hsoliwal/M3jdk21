// SPDX-License-Identifier: Apache-2.0
package jdk.internal.mindex;

/**
 * Stable internal identity for one canonical M3 coordinate space/generation.
 *
 * <p>Numeric IDs are meaningful only inside their exact coordinate space. This value carries no
 * spelling and owns no payload; it exists only to prevent accidental aliasing of equal numeric IDs
 * from different dictionaries, mapped images, model generations, or formula universes.</p>
 */
public record M3CoordinateSpace(long namespaceHigh, long namespaceLow, long generation) {
    public M3CoordinateSpace {
        if (namespaceHigh == 0L && namespaceLow == 0L) {
            throw new IllegalArgumentException("M3 coordinate namespace must be nonzero");
        }
        if (generation < 0L) {
            throw new IllegalArgumentException("M3 coordinate generation must be nonnegative");
        }
    }

    public static M3CoordinateSpace of(
            long namespaceHigh, long namespaceLow, long generation) {
        return new M3CoordinateSpace(namespaceHigh, namespaceLow, generation);
    }
}
