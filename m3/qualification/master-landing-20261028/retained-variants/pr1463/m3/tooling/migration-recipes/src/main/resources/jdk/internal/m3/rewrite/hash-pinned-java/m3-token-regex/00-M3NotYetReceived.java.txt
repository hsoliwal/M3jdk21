/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.mindex;

/**
 * Scaffold marker: the contract of this method is fixed, its body has not been received yet.
 * The message names the owner method and the donor row that will fill it, so a caller never
 * mistakes an unported body for a refusal or a false result.
 */
public final class M3NotYetReceived extends UnsupportedOperationException {
    private static final long serialVersionUID = 1L;

    private final String member;
    private final String donorRow;

    /**
     * @param member owner and method, for example {@code M3TokenMatcher.find}
     * @param donorRow the FOSS_REUSE_DECISION or fleet row that will fill the body
     */
    public M3NotYetReceived(String member, String donorRow) {
        super(member + " not yet received; fill from " + donorRow);
        this.member = member;
        this.donorRow = donorRow;
    }

    /** Owner and method whose body is pending. */
    public String member() {
        return member;
    }

    /** Donor or fleet row that will fill the body. */
    public String donorRow() {
        return donorRow;
    }
}
