/* *****************************************************************************
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
package jdk.internal.m3.collections;

import java.util.List;

/** Source of bounded, stable descriptor pages for a separately declared total count.
 * Implementations return exactly {@code limit} descriptors starting at {@code offset}.
 * The returned list must remain stable during consumption. Fetch must not load payloads
 * that the descriptor predicate could reject. Callers own source/resource lifetime.
 * @param <D> descriptor type
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/io/PageSource.java}
 * blob {@code 07c547b9a1c724447698bf4038f89a40e49bd039} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@FunctionalInterface
public interface PageSource<D> {
    /** Fetch one page; offset is nonnegative and limit is positive. */
    List<D> fetch(long offset, int limit);
}
