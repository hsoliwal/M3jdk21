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

import java.util.Objects;

/**
 * Immutable count-only compatibility result for legacy index diff callers.
 *
 * <p>The richer dependency-light owner is {@link SnapshotDelta}. This type remains intentionally
 * small so existing callers that consume insertion/update/removal counts do not need Lombok or any
 * other runtime dependency. Its builder/getter surface and arithmetic semantics are retained for
 * source and behavior compatibility with the previous implementation.</p>
 *
 * @author Hitesh Soliwal (hsoliwal@gmail.com)
 * @version 1.0.0
 * @see "IndexDiff"
 * @see SnapshotDelta
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/io/DiffResult.java}
 * blob {@code 6376bf101a04fd8407a200910cb50ac554a5bbe2} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class DiffResult {

    private final int inserted;
    private final int updated;
    private final int removed;

    /** Creates a count-only diff result without adding validation absent from the legacy contract. */
    public DiffResult(int inserted, int updated, int removed) {
        this.inserted = inserted;
        this.updated = updated;
        this.removed = removed;
    }

    /** Returns a compatibility result from the richer deterministic delta. */
    public static DiffResult from(SnapshotDelta delta) {
        Objects.requireNonNull(delta, "delta");
        return new DiffResult(delta.inserted(), delta.updated(), delta.removed());
    }

    /** Returns a builder compatible with the former Lombok-generated API. */
    public static Builder builder() {
        return new Builder();
    }

    /** Number of inserted rows/keys. */
    public int getInserted() {
        return inserted;
    }

    /** Number of updated rows/keys. */
    public int getUpdated() {
        return updated;
    }

    /** Number of removed rows/keys. */
    public int getRemoved() {
        return removed;
    }

    /** Returns inserted + updated + removed using the legacy Java int arithmetic semantics. */
    public int totalChanges() {
        return inserted + updated + removed;
    }

    /** Getter alias retained for compatibility. */
    public int getTotalChanges() {
        return totalChanges();
    }

    /** Returns {@code true} when the legacy total-change count is positive. */
    public boolean hasChanges() {
        return totalChanges() > 0;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DiffResult that)) {
            return false;
        }
        return inserted == that.inserted && updated == that.updated && removed == that.removed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(inserted, updated, removed);
    }

    @Override
    public String toString() {
        return "DiffResult(inserted=" + inserted + ", updated=" + updated + ", removed=" + removed + ")";
    }

    /** Mutable construction helper retained solely for source compatibility. */
    public static final class Builder {
        private int inserted;
        private int updated;
        private int removed;

        private Builder() {}

        public Builder inserted(int value) {
            inserted = value;
            return this;
        }

        public Builder updated(int value) {
            updated = value;
            return this;
        }

        public Builder removed(int value) {
            removed = value;
            return this;
        }

        public DiffResult build() {
            return new DiffResult(inserted, updated, removed);
        }
    }
}
