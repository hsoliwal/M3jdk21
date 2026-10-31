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
/** The existing Synexia source-sealed OpenRewrite engine, bound to five-library BiMap postimages. */
public final class M3FiveProviderBidiMapsMasterRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/m3-five-provider-bimaps-master-20261017/";
    }
    @Override public String getDisplayName() { return "Five-provider SDK bidirectional maps on M3 array/index owners"; }
    @Override public String getDescription() {
        return "Qualifies source-sealed Guava and Apache interface adapters, preserving the existing M3 JDK, Eclipse and Spring contract owners.";
    }
}
