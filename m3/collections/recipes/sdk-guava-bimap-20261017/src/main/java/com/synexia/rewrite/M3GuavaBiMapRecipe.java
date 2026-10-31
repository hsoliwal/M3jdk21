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

/** Maven/OpenRewrite source-sealed Guava BiMap SDK handoff; no parallel transform engine. */
public final class M3GuavaBiMapRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/m3-guava-bimap-20261017/";
    }
    @Override public String getDisplayName() {
        return "Receive M3 array-indexed Guava BiMap with verified names and live views";
    }
    @Override public String getDescription() {
        return "Preserve the original Guava BiMap API; install the M3 owner, factory, mappings and differential tests only on exact, sealed preimages.";
    }
}
