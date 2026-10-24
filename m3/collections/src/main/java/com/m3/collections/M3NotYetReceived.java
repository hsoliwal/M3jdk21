/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
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
