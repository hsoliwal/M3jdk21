// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

/**
 * Stable semantic namespaces for canonical MIndex references.
 *
 * <p>The domain is part of identity. Equal numeric coordinates in two domains do not identify the
 * same MIndex symbol. Storage addressability is deliberately orthogonal and remains represented by
 * {@link MIndexAddress}.</p>
 */
public enum MIndexReferenceDomain {
    STRING,
    ATOM,
    NODE,
    RELATION,
    LEXICAL,
    SEMANTIC,
    PATH
}
