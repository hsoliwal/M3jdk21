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
/** Same admitted hash-sealed OpenRewrite SourceFile recipe; only the manifest changes. */
public final class M3GuavaSetMultimapRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/m3-guava-setmultimap-20261018/";
    }
    @Override public String getDisplayName() { return "M3 Guava SetMultimap over indexed owners"; }
    @Override public String getDescription() {
        return "Add source-sealed Guava SetMultimap and real-provider/JNI tests through existing M3 array/index owners.";
    }
}
