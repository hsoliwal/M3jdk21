/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

/**
 * Marker thrown by every five-library scaffold atom that has not yet been received through a
 * hash-pinned Synexia recipe. Deliberately not {@link UnsupportedOperationException}: provider
 * contracts use that exception for frozen or fixed-size refusals, so unreceived atoms stay
 * mechanically distinguishable ({@code rg "new M3NotYetReceived"}).
 */
public final class M3NotYetReceived extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final String atom;

    /** Creates the marker for one named atom ({@code Owner.method}). */
    public M3NotYetReceived(String atom) {
        super("M3 scaffold atom not yet received: " + atom);
        this.atom = atom;
    }

    /** The unreceived atom name. */
    public String atom() {
        return atom;
    }
}
