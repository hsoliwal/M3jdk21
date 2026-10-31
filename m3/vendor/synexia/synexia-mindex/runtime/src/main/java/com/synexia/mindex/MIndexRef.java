// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.util.Objects;

/**
 * MIndex semantic symbol identity paired with its current stable logical storage coordinate.
 *
 * <p>The symbol id is meaningful inside the region that owns it. Raw machine pointers are
 * intentionally excluded: native addresses are derived, scoped values exposed only by an owning
 * native lease.</p>
 */
public record MIndexRef(long symbolId, MIndexAddress address) {
    public MIndexRef {
        if (symbolId < 0L) {
            throw new IllegalArgumentException("symbolId must not be negative: " + symbolId);
        }
        address = Objects.requireNonNull(address, "address");
    }

    /**
     * Compare semantic identity without requiring the same physical storage generation.
     *
     * <p>A future compaction may move storage and increment generation while preserving the
     * region-local symbol id.</p>
     */
    public boolean sameSymbol(MIndexRef other) {
        return other != null
                && symbolId == other.symbolId
                && address.regionId() == other.address.regionId();
    }

    /** True only when both references identify the same current storage coordinate. */
    public boolean sameStorage(MIndexRef other) {
        return other != null && address.equals(other.address);
    }
}
