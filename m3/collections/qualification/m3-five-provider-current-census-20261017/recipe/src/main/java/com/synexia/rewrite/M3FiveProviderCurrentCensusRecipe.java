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
/** The existing Synexia source-sealed OpenRewrite engine, bound to exact five-provider source-census postimages. */
public final class M3FiveProviderCurrentCensusRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/m3-five-provider-current-census-20261017/";
    }
    @Override public String getDisplayName() { return "Five-provider current-source SDK admission across 34 indexed owners"; }
    @Override public String getDescription() {
        return "Seals 34 existing M3 SDK interop Java owners and tests while preserving the original five historical source identities.";
    }
}
