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
package com.synexia.rewrite;

/** Hash-sealed serial M3JDK21 existing-owner refinement; no parallel rewrite engine. */
public final class M3IndexedMultivalueOccurrenceRecipe extends M3GpuToolJoinRecipe {
    public M3IndexedMultivalueOccurrenceRecipe() { }
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/m3-five-library-occurrences-20261018/";
    }
    @Override public String getDisplayName() {
        return "Integrate M3JDK21 indexed multivalue occurrence mutation";
    }
    @Override public String getDescription() {
        return "Source-sealed Object[] payload and int[]/byte[] index-state owner changes.";
    }
}
