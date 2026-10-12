// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

import java.util.Objects;

/**
 * M3JDK21 compatibility vocabulary for Synexia's canonical edit-scope lattice.
 *
 * <p>This enum is a thin target adapter only. Ordering/promotion semantics are delegated to the
 * provenance-pinned Apache-2.0 {@link com.synexia.rewrite.M3EditScope} owner.</p>
 */
public enum M3EditScope {
    FILE(com.synexia.rewrite.M3EditScope.FILE),
    VISIBILITY(com.synexia.rewrite.M3EditScope.VISIBILITY),
    PACKAGE(com.synexia.rewrite.M3EditScope.PACKAGE),
    MODULE(com.synexia.rewrite.M3EditScope.MODULE),
    MULTI_MODULE(com.synexia.rewrite.M3EditScope.MULTI_MODULE),
    LIBRARY_API(com.synexia.rewrite.M3EditScope.LIBRARY_API);

    private final com.synexia.rewrite.M3EditScope canonical;

    M3EditScope(com.synexia.rewrite.M3EditScope canonical) {
        this.canonical = Objects.requireNonNull(canonical, "canonical");
    }

    public boolean permits(M3EditScope required) {
        return canonical.permits(Objects.requireNonNull(required, "required").canonical);
    }

    public M3EditScope promote(M3EditScope other) {
        return fromCanonical(canonical.promote(Objects.requireNonNull(other, "other").canonical));
    }

    public M3EditScope next() {
        return fromCanonical(canonical.next());
    }

    static M3EditScope fromCanonical(com.synexia.rewrite.M3EditScope scope) {
        return valueOf(Objects.requireNonNull(scope, "scope").name());
    }

    com.synexia.rewrite.M3EditScope canonical() {
        return canonical;
    }
}
