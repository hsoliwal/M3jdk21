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
package jdk.internal.m3.collections;

import jdk.m3.collections.LongCollection;

/**
 * Primitive sequence with stable, collection-scoped handles. Handles survive growth;
 * removal and clear invalidate them. A handle from another collection is not valid
 * input even if its numeric representation happens to match. Not thread-safe.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/LongLinkedSequence.java}
 * blob {@code 418ed54ea4b804b91680d4881be91b95e7f17424} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface M3LongLinkedSequence extends LongCollection {
    /** No element, used for empty endpoints and traversal termination. */
    long NONE = 0L;

    /** Appends a value and returns its live handle. */
    long append(long value);

    /** Prepends a value and returns its live handle. */
    long prepend(long value);

    /** Inserts before a live handle; invalid/stale handles throw IllegalArgumentException. */
    long insertBefore(long handle, long value);

    /** Inserts after a live handle; invalid/stale handles throw IllegalArgumentException. */
    long insertAfter(long handle, long value);

    /** Removes a live handle and returns its value; invalid/stale handles are rejected. */
    long removeHandle(long handle);

    /** Reads a live handle; invalid/stale handles are rejected. */
    long valueAt(long handle);

    /** Replaces a live handle's value without structural mutation; returns the old value. */
    long replaceValue(long handle, long value);

    /** Returns whether this list currently recognizes the supplied handle. */
    boolean isLiveHandle(long handle);

    /** First live handle or NONE. */
    long firstHandle();

    /** Last live handle or NONE. */
    long lastHandle();

    /** Successor of a live handle, or NONE at the end; invalid/stale handles are rejected. */
    long nextHandle(long handle);

    /** Predecessor of a live handle, or NONE at the start; invalid/stale handles are rejected. */
    long previousHandle(long handle);

    /** Value at a zero-based logical position; requires traversal, not direct slot access. */
    long get(int index);
}
