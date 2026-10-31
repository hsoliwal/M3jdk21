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
package com.synexia.m3;

import com.synexia.rewrite.M3GpuToolJoinRecipe;

/** Source-sealed five-provider prepared bitmap/offset views; existing recipe kernel reused. */
public final class M3FiveLibraryPreparedRowsRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/five-library-prepared-rows-20261018/";
    }
    @Override public String getDisplayName() {
        return "Prepare five-family M3 filtered and sorted row index views";
    }
    @Override public String getDescription() {
        return "Adds immutable cached per-occurrence bit algebra with existing M3 payload/" +
                " JNI owners and JDK/Guava/Eclipse/Spring/Apache contract-specific factories" +
                " from exact SHA-256 sealed templates; existing owners remain unchanged.";
    }
}
