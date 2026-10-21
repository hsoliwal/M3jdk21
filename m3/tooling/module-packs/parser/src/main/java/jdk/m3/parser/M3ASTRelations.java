// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Shared, optional relationship queries over ONE existing M3ASTIndex.
 *
 * <p>No input read, parser invocation, re-attribution, AST node wrapper,
 * per-occurrence Map.Entry or second text owner. The caller borrows this
 * context across Maven static-analysis rules, Eclipse linters, JDT and javac
 * consumers, generators and structural queries of the *same* source generation.
 * It reuses the owner's parent/first-child/next-sibling coordinates and its
 * existing source-independent structural SHA-256 algorithm.
 *
 * <p>Kind postings allocate exactly one primitive occurrence lane on demand.
 * Structural classes are a separately admitted, bounded and optional pass.
 * A class means equality of *retained structural facts*, not semantic symbol
 * identity, binding equality, dependency-coordinate identity, or safe source
 * transformation. Source and compiler-context facts remain authoritative.
 */
public final class M3ASTRelations {
    private final M3ASTIndex index;
    private volatile KindPostings postings;
    private volatile StructuralClasses classes;

    private M3ASTRelations(M3ASTIndex index) {
        this.index = Objects.requireNonNull(index, "original sealed M3AST index");
    }

    /** Borrow an original detached AST; construction adds no index or parser work. */
    public static M3ASTRelations of(M3ASTIndex index) {
        return new M3ASTRelations(index);
    }

    public M3ASTIndex index() { return index; }
    public M3File source() { return index.source(); }

    /** Validate source generation explicitly, without reopening or rereading it. */
    public void requireSource(M3File expected) {
        Objects.requireNonNull(expected, "expected M3File");
        M3File actual = index.source();
        if (!actual.uri().equals(expected.uri())
                || !actual.utf16Sha256().equals(expected.utf16Sha256())
                || actual.length() != expected.length()
                || !actual.byteSha256().equals(expected.byteSha256())) {
            throw new IllegalArgumentException("foreign M3AST source generation");
        }
    }

    /** O(1) kind count after one shared, lazy counting-sort index. */
    public int countKind(String kind) {
        int id = kindId(Objects.requireNonNull(kind, "kind"));
        KindPostings p = postings();
        return id < 0 ? 0 : p.offsets[id + 1] - p.offsets[id];
    }

    /**
     * Return one original preorder occurrence coordinate, not a node object.
     * No new allocation or parser work after the shared postings are admitted.
     */
    public int rowOfKind(String kind, int ordinal) {
        int id = kindId(Objects.requireNonNull(kind, "kind"));
        KindPostings p = postings();
        if (id < 0) throw new IndexOutOfBoundsException("unknown AST kind");
        int begin = p.offsets[id], end = p.offsets[id + 1];
        return p.rows[begin + Objects.checkIndex(ordinal, end - begin)];
    }

    /**
     * Explicit snapshot only. Refuses truncated result rather than silently
     * hiding analysis findings beyond the caller's maxResults bound.
     */
    public int[] rowsOfKind(String kind, int maxResults) {
        if (maxResults < 0) throw new IllegalArgumentException("negative result budget");
        int id = kindId(Objects.requireNonNull(kind, "kind"));
        KindPostings p = postings();
        if (id < 0) return new int[0];
        int begin = p.offsets[id], end = p.offsets[id + 1];
        if (end - begin > maxResults) {
            throw new IllegalArgumentException("AST query result budget exceeded");
        }
        return Arrays.copyOfRange(p.rows, begin, end);
    }

    /** No intermediate node list: invoke a caller's primitive action in AST order. */
    public void eachKind(String kind, IntConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        int id = kindId(Objects.requireNonNull(kind, "kind"));
        KindPostings p = postings();
        if (id >= 0) {
            for (int i = p.offsets[id]; i < p.offsets[id + 1]; i++) {
                consumer.accept(p.rows[i]);
            }
        }
    }

    /** Reuse the original parent index directly; never materialize graph edges. */
    public int nearestAncestor(int node, String kind) {
        return index.firstEnclosing(node, kind);
    }

    /** Reuse the original first-child/next-sibling lanes in occurrence order. */
    public void eachChild(int parent, IntConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        int child = index.firstChild(parent); // validates row coordinate
        while (child >= 0) {
            consumer.accept(child);
            child = index.nextSibling(child);
        }
    }

    /**
     * Demand-only source-independent exact class grouping of the current
     * *detached lanes*. Uses the existing M3ASTIndex.structuralHashes pass
     * for candidate hashes, and independently verifies each candidate's
     * intrinsic fields and already-built child class IDs.
     *
     * <p>The existing 32-byte-per-node SHA lane and the temporary primitive
     * open-addressing table are released after construction. Retained
     * grouping is three primitive int lanes per occurrence/class, not another
     * syntax tree. Even SHA prefix/digest collisions cannot merge unequal
     * retained fields. Negative or insufficient budgets fail closed.
     */
    public StructuralClasses structuralClasses(int maxNodes, long maxTextUnits) {
        if (maxNodes < 0 || maxTextUnits < 0 || index.nodeCount() > maxNodes) {
            throw new IllegalArgumentException("AST structural admission budget");
        }
        StructuralClasses found = classes;
        if (found == null) {
            synchronized (this) {
                found = classes;
                if (found == null) {
                    found = buildClasses(maxNodes, maxTextUnits);
                    classes = found;
                }
            }
        }
        if (found.utf16Units > maxTextUnits) {
            throw new IllegalArgumentException("structural text budget smaller than cached admission");
        }
        return found;
    }

    /** Release only optional sidecars, never the original file or AST. */
    public synchronized void discardDerived() {
        postings = null;
        classes = null;
    }

    /** Retained primitive array payload only, excluding M3AST/M3File and array headers. */
    public long retainedPrimitiveBytes() {
        KindPostings p = postings;
        StructuralClasses c = classes;
        return Integer.BYTES * (
                (p == null ? 0L : (long) p.rows.length + p.offsets.length)
                        + (c == null ? 0L : (long) c.classOf.length
                                + c.representatives.length + c.multiplicities.length));
    }

    private int kindId(String kind) {
        for (int id = 0; id < index.kindVocabularySize(); id++) {
            if (index.kindVocabularyName(id).equals(kind)) return id;
        }
        return -1;
    }

    private KindPostings postings() {
        KindPostings found = postings;
        if (found == null) {
            synchronized (this) {
                found = postings;
                if (found == null) {
                    int kinds = index.kindVocabularySize();
                    int n = index.nodeCount();
                    int[] offsets = new int[kinds + 1];
                    for (int row = 0; row < n; row++) {
                        offsets[index.kindOrdinal(row) + 1]++;
                    }
                    for (int id = 1; id <= kinds; id++) {
                        offsets[id] = Math.addExact(offsets[id], offsets[id - 1]);
                    }
                    int[] cursor = Arrays.copyOf(offsets, kinds);
                    int[] rows = new int[n];
                    for (int row = 0; row < n; row++) {
                        int kind = index.kindOrdinal(row);
                        rows[cursor[kind]++] = row;
                    }
                    postings = found = new KindPostings(offsets, rows);
                }
            }
        }
        return found;
    }

    private StructuralClasses buildClasses(int maxNodes, long maxTextUnits) {
        M3ASTIndex.StructuralHashes hashes = index.structuralHashes(maxNodes, maxTextUnits);
        int n = index.nodeCount();
        int capacity = 4;
        while (capacity < (long) n * 2) capacity <<= 1;
        int[] table = new int[capacity]; // 0 means absent; classID + 1 otherwise
        int[] classOf = new int[n];
        int[] representative = new int[n];
        int[] multiplicity = new int[n];
        int count = 0;
        for (int row = n - 1; row >= 0; row--) {
            int slot = mix(hashes.prefix64(row)) & (capacity - 1);
            int group = -1;
            while (table[slot] != 0) {
                int candidate = table[slot] - 1;
                int head = representative[candidate];
                if (hashes.sameHash(row, head) && index.sameStructuralAtom(row, head)
                        && sameChildren(row, head, classOf)) {
                    group = candidate;
                    break;
                }
                slot = (slot + 1) & (capacity - 1);
            }
            if (group < 0) {
                group = count++;
                table[slot] = group + 1;
                multiplicity[group] = 1;
            } else {
                multiplicity[group]++;
            }
            classOf[row] = group;
            representative[group] = row; // earliest occurrence at publication
        }
        // The transient SHA lane and primitive hash table now become unreachable.
        return new StructuralClasses(classOf, Arrays.copyOf(representative, count),
                Arrays.copyOf(multiplicity, count), hashes.textUnits());
    }

    private boolean sameChildren(int left, int right, int[] classOf) {
        int a = index.firstChild(left);
        int b = index.firstChild(right);
        while (a >= 0 && b >= 0) {
            if (classOf[a] != classOf[b]) return false;
            a = index.nextSibling(a);
            b = index.nextSibling(b);
        }
        return a == -1 && b == -1;
    }

    private static int mix(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return (int) value;
    }

    private record KindPostings(int[] offsets, int[] rows) { }

    /** Exact only for retained syntax lanes, not semantic binding identity. */
    public static final class StructuralClasses {
        private final int[] classOf;
        private final int[] representatives;
        private final int[] multiplicities;
        private final long utf16Units;

        private StructuralClasses(int[] classOf, int[] representatives,
                                  int[] multiplicities, long utf16Units) {
            this.classOf = classOf;
            this.representatives = representatives;
            this.multiplicities = multiplicities;
            this.utf16Units = utf16Units;
        }

        public int totalOccurrences() { return classOf.length; }
        public int distinctShapes() { return representatives.length; }
        public long admittedTextUnits() { return utf16Units; }
        public int classOf(int row) { return classOf[Objects.checkIndex(row, classOf.length)]; }
        public int representative(int row) {
            return representatives[classOf(row)];
        }
        public int multiplicity(int row) {
            return multiplicities[classOf(row)];
        }
        public boolean sameRetainedShape(int a, int b) {
            return classOf(a) == classOf(b);
        }
        /**
         * Eager result only when requested; refuses truncation. All occurrences
         * are original coordinates, returned in original document preorder.
         */
        public int[] occurrences(int node, int maxResults) {
            if (maxResults < 0) throw new IllegalArgumentException("negative result budget");
            int selected = classOf(node);
            int needed = multiplicities[selected];
            if (needed > maxResults) {
                throw new IllegalArgumentException("structural class result budget exceeded");
            }
            int[] result = new int[needed];
            int size = 0;
            for (int i = 0; i < classOf.length; i++) {
                if (classOf[i] == selected) result[size++] = i;
            }
            return result;
        }
    }
}
