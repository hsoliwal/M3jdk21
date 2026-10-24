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

/** Serial, source-sealed M3 arrayification recipe reusing the admitted OpenRewrite engine. */
public final class M3Arrayification20261018Recipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/arrayification-20261018/";
    }
    @Override public String getDisplayName() {
        return "Arrayify five-library M3 collection indexes using reviewed challenge categories";
    }
    @Override public String getDescription() {
        return "Refine the existing M3 five-provider indexed SoA owner through checked array/index/offset/mask atoms.";
    }
}
