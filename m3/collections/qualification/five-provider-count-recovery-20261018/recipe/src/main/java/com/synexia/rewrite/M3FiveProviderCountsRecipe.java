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

/** Named, source-sealed OpenRewrite recipe reusing the existing Synexia engine. */
public final class M3FiveProviderCountsRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/five-provider-count-recovery-20261018/";
    }
    @Override public String getDisplayName() {
        return "Reconcile five-provider indexed M3 shared count and flag owners";
    }
    @Override public String getDescription() {
        return "Restore a previously qualified M3 primitive owner, reuse existing M3CountedBag across real providers, and refuse preimage drift.";
    }
}
