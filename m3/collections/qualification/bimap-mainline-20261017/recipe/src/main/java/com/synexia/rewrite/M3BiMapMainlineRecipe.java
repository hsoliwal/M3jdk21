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

/**
 * Receive already-qualified Synexia M3 BiMap into current M3JDK21 master.
 * Only new files; source-sealed OpenRewrite PlainText results, no second engine.
 * Any subsequent existing-owner refinement must use an attributed Java LST recipe.
 */
public final class M3BiMapMainlineRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/m3-bimap-mainline-20261017/";
    }
    @Override public String getDisplayName() {
        return "Receive qualified M3 Guava BiMap into canonical product SDK";
    }
    @Override public String getDescription() {
        return "Install five exact prequalified Java and test-only C source postimages while refusing occupied and drifting targets.";
    }
}
