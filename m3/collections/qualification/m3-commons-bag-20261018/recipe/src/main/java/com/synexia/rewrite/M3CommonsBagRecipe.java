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

/** Reuses the established Synexia M3GpuToolJoinRecipe and SHA-256 manifest. */
public final class M3CommonsBagRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/m3-commons-bag-20261018/";
    }
    @Override public String getDisplayName() {
        return "Generate Apache Commons Bag over existing M3CountedBag";
    }
    @Override public String getDescription() {
        return "Adds the genuine Apache Bag API implementation over the first-party M3 indexed counted owner, with donor differential and five-library tests.";
    }
}
