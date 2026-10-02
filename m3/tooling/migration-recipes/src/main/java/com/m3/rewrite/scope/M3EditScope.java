// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

/**
 * Maximum mutation boundary required by an M3 refactoring recipe.
 *
 * <p>Scope promotion is monotonic. A recipe that can be proven at FILE scope must not request a
 * broader boundary merely for convenience.
 */
public enum M3EditScope {
    FILE(0),
    PACKAGE_VISIBILITY(1),
    MODULE(2),
    MULTI_MODULE(3),
    LIBRARY_API(4);

    private final int level;

    M3EditScope(int level) {
        this.level = level;
    }

    /** Returns true when this granted scope is broad enough for the required scope. */
    public boolean permits(M3EditScope required) {
        return level >= required.level;
    }

    /** Returns the narrowest scope that contains both boundaries. */
    public M3EditScope promote(M3EditScope other) {
        return level >= other.level ? this : other;
    }

    /** Returns the immediately broader boundary, or LIBRARY_API at the top. */
    public M3EditScope next() {
        return switch (this) {
            case FILE -> PACKAGE_VISIBILITY;
            case PACKAGE_VISIBILITY -> MODULE;
            case MODULE -> MULTI_MODULE;
            case MULTI_MODULE, LIBRARY_API -> LIBRARY_API;
        };
    }
}
