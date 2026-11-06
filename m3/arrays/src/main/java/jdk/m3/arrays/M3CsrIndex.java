// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable CSR relation: row -> contiguous references.
 *
 * <p>The runtime representation is exactly two int arrays: starts and references.</p>
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-mat-value/src/main/java/com/synexia/matvalue/MatCsrIndex.java blob 5fed861ccfb8; recipe m3/arrays/recipe/csr-index-20261106/relocate.py v1
public final class M3CsrIndex {
    private final int[] starts;
    private final int[] references;

    private M3CsrIndex(int[] starts, int[] references) {
        this.starts = starts;
        this.references = references;
        validate(starts, references.length);
    }

    public static Builder builder(int rows) { return new Builder(rows, 16); }

    public static Builder builder(int rows, int expectedEntries) {
        return new Builder(rows, expectedEntries);
    }

    /** Build row -> reference-index from one owner row per reference. */
    public static M3CsrIndex fromOwners(int rows, int[] ownerByReference) {
        Objects.requireNonNull(ownerByReference, "ownerByReference");
        if (rows < 0) throw new IllegalArgumentException("negative CSR capacity");
        int[] counts = new int[rows];
        for (int owner : ownerByReference) {
            int checked = Objects.checkIndex(owner, rows);
            counts[checked] = Math.addExact(counts[checked], 1);
        }
        int[] starts = new int[rows + 1];
        for (int row = 0; row < rows; row++) {
            starts[row + 1] = Math.addExact(starts[row], counts[row]);
        }
        int[] cursor = starts.clone();
        int[] references = new int[ownerByReference.length];
        for (int reference = 0; reference < ownerByReference.length; reference++) {
            int owner = ownerByReference[reference];
            references[cursor[owner]++] = reference;
        }
        return new M3CsrIndex(starts, references);
    }

    public int rowCount() { return starts.length - 1; }
    public int size() { return references.length; }
    public long payloadBytes() { return 4L * (starts.length + references.length); }

    public int start(int row) {
        return starts[Objects.checkIndex(row, rowCount())];
    }

    public int end(int row) {
        int checked = Objects.checkIndex(row, rowCount());
        return starts[checked + 1];
    }

    public int degree(int row) {
        int checked = Objects.checkIndex(row, rowCount());
        return starts[checked + 1] - starts[checked];
    }

    public int referenceAt(int row, int ordinal) {
        int checked = Objects.checkIndex(row, rowCount());
        int start = starts[checked];
        int length = starts[checked + 1] - start;
        return references[start + Objects.checkIndex(ordinal, length)];
    }

    public int referenceAtAbsolute(int position) {
        return references[Objects.checkIndex(position, references.length)];
    }

    public int[] row(int row) {
        int checked = Objects.checkIndex(row, rowCount());
        return Arrays.copyOfRange(references, starts[checked], starts[checked + 1]);
    }

    public int[] starts() { return starts.clone(); }
    public int[] references() { return references.clone(); }

    private static void validate(int[] starts, int referenceCount) {
        if (starts.length == 0 || starts[0] != 0) {
            throw new IllegalArgumentException("CSR starts must begin at zero");
        }
        int previous = 0;
        for (int start : starts) {
            if (start < previous || start > referenceCount) {
                throw new IllegalArgumentException("invalid CSR starts");
            }
            previous = start;
        }
        if (previous != referenceCount) {
            throw new IllegalArgumentException("CSR has unaddressed references");
        }
    }

    public static final class Builder {
        private final int rows;
        private int[] ownerRows;
        private int[] refs;
        private int size;

        private Builder(int rows, int expectedEntries) {
            if (rows < 0 || expectedEntries < 0) {
                throw new IllegalArgumentException("negative CSR capacity");
            }
            this.rows = rows;
            int capacity = Math.max(1, expectedEntries);
            ownerRows = new int[capacity];
            refs = new int[capacity];
        }

        public Builder add(int row, int reference) {
            Objects.checkIndex(row, rows);
            if (reference < 0) throw new IllegalArgumentException("negative CSR reference");
            reserve(size + 1);
            ownerRows[size] = row;
            refs[size] = reference;
            size++;
            return this;
        }

        public int size() { return size; }

        public M3CsrIndex build() {
            int[] counts = new int[rows];
            for (int index = 0; index < size; index++) {
                counts[ownerRows[index]] = Math.addExact(counts[ownerRows[index]], 1);
            }
            int[] starts = new int[rows + 1];
            for (int row = 0; row < rows; row++) {
                starts[row + 1] = Math.addExact(starts[row], counts[row]);
            }
            int[] cursor = starts.clone();
            int[] references = new int[size];
            for (int index = 0; index < size; index++) {
                int row = ownerRows[index];
                references[cursor[row]++] = refs[index];
            }
            return new M3CsrIndex(starts, references);
        }

        private void reserve(int required) {
            if (required <= refs.length) return;
            int next = grow(refs.length, required);
            ownerRows = Arrays.copyOf(ownerRows, next);
            refs = Arrays.copyOf(refs, next);
        }

        private static int grow(int current, int required) {
            long grown = Math.max(required, Math.max(2L, current + (current >>> 1) + 1L));
            if (grown > Integer.MAX_VALUE - 8L) {
                throw new IllegalStateException("CSR capacity");
            }
            return (int) grown;
        }
    }
}
