// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.internal.mindex;

/**
 * Scaffold marker: thrown by a declared entry point whose implementation has not been received from its canonical
 * Synexia recipe yet. The message names the owner, the method and the design section that will fill it; grep-able,
 * never {@link UnsupportedOperationException}. Internal; not a public JDK API.
 */
public final class M3NotYetReceived extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** Creates the marker for {@code site} (for example {@code "M3RunTable.wordRowImage R6_DESIGN section 4"}). */
    public M3NotYetReceived(String site) {
        super(site, null, false, false);
    }
}
