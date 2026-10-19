// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

/**
 * Common semantic identity surface for immutable MIndex reference families.
 *
 * <p>Implementations must preserve their existing equality and packed-coordinate contracts.
 * Projection to {@link MIndexSymbolRef} is additive and must not imply byte addressability.</p>
 */
public interface MIndexSymbolReference {
    MIndexSymbolRef symbolRef();
}
