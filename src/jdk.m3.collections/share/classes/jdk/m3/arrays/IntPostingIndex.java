// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable CSR inverted index: sorted distinct int keys -> offsets -> sorted row ordinals.
 * No collection object is retained per key.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-mat-collections/src/main/java/com/synexia/matcollections/MatIntPostingIndex.java blob da841daf91c4; recipe m3/arrays/recipe/posting-index-20261106/relocate.py v2
public final class IntPostingIndex {
    private final int rows;
    private final int[] keys;
    private final int[] offsets;
    private final int[] rowOrdinals;

    private IntPostingIndex(int rows, int[] keys, int[] offsets, int[] rowOrdinals) {
        this.rows = rows;
        this.keys = keys;
        this.offsets = offsets;
        this.rowOrdinals = rowOrdinals;
    }

    /** One key per row; row ordinal is the source array index. */
    public static IntPostingIndex ofKeys(int[] keyByRow) {
        Objects.requireNonNull(keyByRow, "keyByRow");
        int[] rows = new int[keyByRow.length];
        for (int i = 0; i < rows.length; i++) rows[i] = i;
        return ofPairs(keyByRow, rows, rows.length);
    }

    /** Duplicate (key,row) pairs are canonicalized to one posting. */
    public static IntPostingIndex ofPairs(int[] pairKeys, int[] pairRows, int rowUniverse) {
        Objects.requireNonNull(pairKeys, "pairKeys");
        Objects.requireNonNull(pairRows, "pairRows");
        if (pairKeys.length != pairRows.length) throw new IllegalArgumentException("length mismatch");
        if (rowUniverse < 0) throw new IllegalArgumentException("negative row universe");
        long[] pairs = new long[pairKeys.length];
        for (int i = 0; i < pairs.length; i++) {
            int row = pairRows[i];
            if (row < 0 || row >= rowUniverse) throw new IllegalArgumentException("row outside universe: " + row);
            pairs[i] = ((long) pairKeys[i] << 32) | (row & 0xffff_ffffL);
        }
        Arrays.sort(pairs);
        int unique = 0;
        for (long pair : pairs) {
            if (unique == 0 || pairs[unique - 1] != pair) pairs[unique++] = pair;
        }
        if (unique == 0) return new IntPostingIndex(rowUniverse, new int[0], new int[] {0}, new int[0]);

        int distinct = 1;
        for (int i = 1; i < unique; i++) {
            if ((int) (pairs[i] >> 32) != (int) (pairs[i - 1] >> 32)) distinct++;
        }
        int[] keys = new int[distinct];
        int[] offsets = new int[distinct + 1];
        int[] postings = new int[unique];
        int keyIndex = -1;
        int previousKey = 0;
        for (int i = 0; i < unique; i++) {
            int key = (int) (pairs[i] >> 32);
            if (i == 0 || key != previousKey) {
                keyIndex++;
                keys[keyIndex] = key;
                offsets[keyIndex] = i;
                previousKey = key;
            }
            postings[i] = (int) pairs[i];
        }
        offsets[distinct] = unique;
        return new IntPostingIndex(rowUniverse, keys, offsets, postings);
    }

    public int rows() { return rows; }
    public int keyCount() { return keys.length; }
    public int postingCount() { return rowOrdinals.length; }
    public long primitivePayloadBytes() {
        return 4L * ((long) keys.length + offsets.length + rowOrdinals.length);
    }

    public int keyAt(int index) { return keys[Objects.checkIndex(index, keys.length)]; }
    public int[] keys() { return keys.clone(); }

    public int postingSize(int key) {
        int index = Arrays.binarySearch(keys, key);
        return index < 0 ? 0 : offsets[index + 1] - offsets[index];
    }

    public IntPostingList rows(int key) {
        int index = Arrays.binarySearch(keys, key);
        return index < 0
                ? new IntPostingList(new int[0])
                : IntPostingList.trustedView(rowOrdinals, offsets[index], offsets[index + 1] - offsets[index]);
    }

    public boolean contains(int key, int row) {
        if (row < 0 || row >= rows) return false;
        int index = Arrays.binarySearch(keys, key);
        return index >= 0 && Arrays.binarySearch(rowOrdinals, offsets[index], offsets[index + 1], row) >= 0;
    }

    public IntSelection equalSelection(int key) {
        IntSelection.Builder selected = IntSelection.builder(rows);
        int index = Arrays.binarySearch(keys, key);
        if (index >= 0) {
            for (int i = offsets[index]; i < offsets[index + 1]; i++) selected.add(rowOrdinals[i]);
        }
        return selected.build();
    }

    /** Backends are explicit; the default methods never load a native library. */
    public enum Backend { JAVA, NATIVE_REQUIRED }

    private static volatile boolean nativeLoaded;

    /**
     * Opt-in to an independently built, ABI-matched JNI kernel. No automatic loading,
     * no fallback when NATIVE_REQUIRED is requested, and no retained native pointers.
     */
    public static synchronized void loadNative(java.nio.file.Path library) {
        Objects.requireNonNull(library, "library");
        if (!nativeLoaded) {
            System.load(library.toAbsolutePath().normalize().toString());
            if (nativeAbi() != 20261029) {
                throw new UnsatisfiedLinkError("M3_POSTING_JNI_ABI_MISMATCH");
            }
            nativeLoaded = true;
        }
    }

    public static boolean nativeAvailable() { return nativeLoaded; }

    /**
     * One bitmap build instead of an intermediate bitmap and OR for every requested key.
     * Duplicate/absent keys are coalesced before scanning the immutable CSR postings.
     */
    public IntSelection any(int... queryKeys) {
        return any(Backend.JAVA, queryKeys);
    }

    public IntSelection any(Backend backend, int... queryKeys) {
        Objects.requireNonNull(backend, "backend");
        Objects.requireNonNull(queryKeys, "queryKeys");
        requireNative(backend);
        int[] positions = positions(queryKeys, false);
        IntSelection.Builder result = IntSelection.builder(rows);
        for (int position : positions) {
            for (int index = offsets[position]; index < offsets[position + 1]; index++) {
                result.add(rowOrdinals[index]);
            }
        }
        IntSelection javaResult = result.build();
        if (backend == Backend.NATIVE_REQUIRED) {
            int[] actual = unionNative(rowOrdinals, offsets, positions, rows);
            int[] expected = javaResult.rowsSelected().toArray();
            verifyNative(expected, actual);
        }
        return javaResult;
    }

    /**
     * Shortest-first immutable posting-list intersection. The candidate lane is at most
     * the shortest posting; dense peers use an in-place two-pointer merge, while highly
     * imbalanced peers use monotonic lower-bound searches. No per-entry Java objects.
     */
    public IntSelection all(int... queryKeys) {
        return all(Backend.JAVA, queryKeys);
    }

    public IntSelection all(Backend backend, int... queryKeys) {
        Objects.requireNonNull(backend, "backend");
        Objects.requireNonNull(queryKeys, "queryKeys");
        requireNative(backend);
        IntSelection.Builder result = IntSelection.builder(rows);
        if (queryKeys.length == 0) {
            for (int row = 0; row < rows; row++) result.add(row);
            IntSelection universe = result.build();
            if (backend == Backend.NATIVE_REQUIRED) {
                verifyNative(universe.rowsSelected().toArray(),
                        intersectionNative(rowOrdinals, offsets, new int[0], rows));
            }
            return universe;
        }
        int[] positions = positions(queryKeys, true);
        if (positions == null) {
            IntSelection empty = result.build();
            if (backend == Backend.NATIVE_REQUIRED) {
                // A missing posting is an exact empty intersection; this is a Java-only
                // short-circuit, not a claim that a native kernel executed.
                verifyNative(new int[0], new int[0]);
            }
            return empty;
        }
        // A single selected posting can populate the immutable bitmap directly.
        // NATIVE_REQUIRED still executes the original JNI comparison path.
        if (positions.length == 1 && backend == Backend.JAVA) {
            int position = positions[0];
            for (int index = offsets[position]; index < offsets[position + 1]; index++) {
                result.add(rowOrdinals[index]);
            }
            return result.build();
        }
        int[] matches = intersectJava(positions);
        for (int row : matches) result.add(row);
        if (backend == Backend.NATIVE_REQUIRED) {
            verifyNative(matches, intersectionNative(rowOrdinals, offsets, positions, rows));
        }
        return result.build();
    }

    /** Sorted unique offsets into the key directory, or null for an absent AND key. */
    private int[] positions(int[] queryKeys, boolean requireAll) {
        // A single-key query needs neither sort nor a redundant array copy.
        if (queryKeys.length == 1) {
            int slot = Arrays.binarySearch(keys, queryKeys[0]);
            return slot < 0 ? (requireAll ? null : new int[0]) : new int[] {slot};
        }
        int[] found = new int[queryKeys.length];
        int size = 0;
        for (int key : queryKeys) {
            int position = Arrays.binarySearch(keys, key);
            if (position < 0) {
                if (requireAll) return null;
            } else {
                found[size++] = position;
            }
        }
        Arrays.sort(found, 0, size);
        int unique = 0;
        for (int i = 0; i < size; i++) {
            if (unique == 0 || found[unique - 1] != found[i]) {
                found[unique++] = found[i];
            }
        }
        return unique == found.length ? found : Arrays.copyOf(found, unique);
    }

    private int[] intersectJava(int[] positions) {
        int shortest = 0;
        int possibleFirst = 0;
        int possibleLast = rows - 1;
        // Fuse interval proof with the existing shortest-first selection pass.
        for (int i = 0; i < positions.length; i++) {
            int position = positions[i];
            if (i > 0 && postingLength(position) < postingLength(positions[shortest])) shortest = i;
            int start = offsets[position];
            int end = offsets[position + 1];
            if (start == end) return new int[0];
            possibleFirst = Math.max(possibleFirst, rowOrdinals[start]);
            possibleLast = Math.min(possibleLast, rowOrdinals[end - 1]);
        }
        // The original single-key JNI proof still runs; multi-key disjoint
        // intervals have no possible common row, so no scratch buffer is needed.
        if (positions.length > 1 && possibleFirst > possibleLast) return new int[0];
        int first = positions[shortest];
        int[] selected = Arrays.copyOfRange(
                rowOrdinals, offsets[first], offsets[first + 1]);
        int size = selected.length;
        for (int i = 0; i < positions.length && size > 0; i++) {
            if (i == shortest) continue;
            int position = positions[i];
            int start = offsets[position];
            int end = offsets[position + 1];
            size = ((long) (end - start) >= (long) size * 16L)
                    ? intersectByLowerBound(selected, size, start, end)
                    : intersectByMerge(selected, size, start, end);
        }
        return size == selected.length ? selected : Arrays.copyOf(selected, size);
    }

    private int postingLength(int position) {
        return offsets[position + 1] - offsets[position];
    }

    /** A single in-place merge keeps only current common rows in the same scratch lane. */
    private int intersectByMerge(int[] selected, int size, int start, int end) {
        int read = 0;
        int write = 0;
        int cursor = start;
        while (read < size && cursor < end) {
            int candidate = selected[read];
            int present = rowOrdinals[cursor];
            if (candidate < present) {
                read++;
            } else if (candidate > present) {
                cursor++;
            } else {
                selected[write++] = candidate;
                read++;
                cursor++;
            }
        }
        return write;
    }

    /** Monotonic lower-bound lookup avoids scanning a disproportionately large posting. */
    private int intersectByLowerBound(int[] selected, int size, int start, int end) {
        int write = 0;
        int cursor = start;
        for (int read = 0; read < size && cursor < end; read++) {
            int candidate = selected[read];
            int found = lowerBoundFrom(cursor, end, candidate);
            if (found < end && rowOrdinals[found] == candidate) {
                selected[write++] = candidate;
                cursor = found + 1;
            } else {
                cursor = found;
            }
        }
        return write;
    }

    /**
     * Lower bound in [start,end), galloping from the retained cursor before bisecting.
     * Every skipped row is below value; the bracket never crosses the posting boundary.
     * Saturation before doubling prevents overflow even for an int-sized posting.
     */
    private int lowerBoundFrom(int start, int end, int value) {
        if (start == end || rowOrdinals[start] >= value) return start;
        int low = start + 1;
        int span = end - start;
        int step = 1;
        while (step < span && rowOrdinals[start + step] < value) {
            low = start + step + 1;
            step = step > span / 2 ? span : step * 2;
        }
        int high = start + step;
        while (low < high) {
            int middle = low + (high - low) / 2;
            if (rowOrdinals[middle] < value) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    private static void requireNative(Backend backend) {
        if (backend == Backend.NATIVE_REQUIRED && !nativeLoaded) {
            throw new UnsatisfiedLinkError("M3_POSTING_JNI_NOT_LOADED");
        }
    }

    /** JNI is a required parity lane here, not an unproven production speedup. */
    private static void verifyNative(int[] expected, int[] actual) {
        if (!Arrays.equals(expected, actual)) {
            throw new IllegalStateException("M3_POSTING_JNI_JAVA_ORACLE_MISMATCH");
        }
    }

    private static native int nativeAbi();

    private static native int[] unionNative(
            int[] rowOrdinals, int[] offsets, int[] positions, int rowUniverse);

    private static native int[] intersectionNative(
            int[] rowOrdinals, int[] offsets, int[] positions, int rowUniverse);

    /**
     * Cardinality of the union without allocating a row-universe bitmap or result lane.
     * Empty queries return zero; duplicates and absent keys follow {@link #any(int...)}.
     * Query sorting and a primitive heap use O(query keys) scratch, not O(rows).
     */
    public int countAny(int... queryKeys) {
        return countAny(Backend.JAVA, queryKeys);
    }

    /** Required JNI is an independently checked parity lane, as for any/all. */
    public int countAny(Backend backend, int... queryKeys) {
        Objects.requireNonNull(backend, "backend");
        Objects.requireNonNull(queryKeys, "queryKeys");
        requireNative(backend);
        int[] requested = positions(queryKeys, false);
        int expected = countUnionJava(requested);
        if (backend == Backend.NATIVE_REQUIRED) {
            verifyNativeCount(expected, countUnionNative(rowOrdinals, offsets, requested, rows));
        }
        return expected;
    }

    /**
     * Cardinality of the intersection, without materializing matched rows or a bitmap.
     * The empty conjunction is the row universe; an absent key makes it empty.
     * One monotone cursor per selected posting is retained, even for very large rows.
     */
    public int countAll(int... queryKeys) {
        return countAll(Backend.JAVA, queryKeys);
    }

    public int countAll(Backend backend, int... queryKeys) {
        Objects.requireNonNull(backend, "backend");
        Objects.requireNonNull(queryKeys, "queryKeys");
        requireNative(backend);
        int[] requested = positions(queryKeys, true);
        // As with all(), this is a Java-only absent-key short circuit.
        if (requested == null) return 0;
        int expected = countIntersectionJava(requested);
        if (backend == Backend.NATIVE_REQUIRED) {
            verifyNativeCount(expected, countIntersectionNative(rowOrdinals, offsets, requested, rows));
        }
        return expected;
    }

    private int countUnionJava(int[] requested) {
        return countUnionJava(requested, Integer.MAX_VALUE);
    }

    private int countUnionJava(int[] requested, int limit) {
        if (limit == 0 || requested.length == 0) return 0;
        if (requested.length == 1) return Math.min(limit, postingLength(requested[0]));
        long[] heap = new long[requested.length];
        int[] cursors = new int[requested.length];
        int[] ends = new int[requested.length];
        int active = 0;
        for (int position : requested) {
            int begin = offsets[position], end = offsets[position + 1];
            if (begin == end) continue;
            cursors[active] = begin;
            ends[active] = end;
            heap[active] = ((long) rowOrdinals[begin] << 32) | active;
            active++;
        }
        // Bottom-up heap construction preserves deterministic row/slot ordering.
        for (int root = active / 2; root > 0;) {
            --root;
            siftCountHeap(heap, active, root, heap[root]);
        }
        int count = 0, previous = -1;
        while (active != 0) {
            long entry = heap[0];
            int row = (int) (entry >>> 32);
            if (row != previous) {
                if (++count == limit) return count;
                previous = row;
            }
            int slot = (int) entry;
            if (++cursors[slot] < ends[slot]) {
                entry = ((long) rowOrdinals[cursors[slot]] << 32) | slot;
            } else {
                entry = heap[--active];
            }
            if (active != 0) siftCountHeap(heap, active, 0, entry);
        }
        return count;
    }

    private static void siftCountHeap(long[] heap, int size, int root, long entry) {
        while (root < size / 2) {
            int child = root * 2 + 1;
            if (child + 1 < size && heap[child + 1] < heap[child]) child++;
            if (heap[child] >= entry) break;
            heap[root] = heap[child];
            root = child;
        }
        heap[root] = entry;
    }

    private int countIntersectionJava(int[] requested) {
        return countIntersectionJava(requested, Integer.MAX_VALUE);
    }

    private int countIntersectionJava(int[] requested, int limit) {
        if (limit == 0) return 0;
        if (requested.length == 0) return Math.min(limit, rows);
        if (requested.length == 1) return Math.min(limit, postingLength(requested[0]));
        int shortest = 0;
        for (int i = 1; i < requested.length; i++) {
            if (postingLength(requested[i]) < postingLength(requested[shortest])) shortest = i;
        }
        int first = requested[shortest];
        if (offsets[first] == offsets[first + 1]) return 0;
        int[] cursors = new int[requested.length];
        for (int i = 0; i < requested.length; i++) cursors[i] = offsets[requested[i]];
        int count = 0;
        for (int at = offsets[first]; at < offsets[first + 1]; at++) {
            int candidate = rowOrdinals[at];
            boolean present = true;
            for (int i = 0; i < requested.length; i++) {
                if (i == shortest) continue;
                int end = offsets[requested[i] + 1];
                int found = lowerBoundFrom(cursors[i], end, candidate);
                cursors[i] = found;
                if (found == end) return count;
                if (rowOrdinals[found] != candidate) { present = false; break; }
                cursors[i] = found + 1;
            }
            if (present && ++count == limit) return count;
        }
        return count;
    }

    private static void verifyNativeCount(int expected, int actual) {
        if (expected != actual) {
            throw new IllegalStateException("M3_POSTING_JNI_JAVA_ORACLE_MISMATCH");
        }
    }

    private static native int countUnionNative(
            int[] rowOrdinals, int[] offsets, int[] positions, int rowUniverse);

    private static native int countIntersectionNative(
            int[] rowOrdinals, int[] offsets, int[] positions, int rowUniverse);

    /**
     * Returns min(limit, countAny(queryKeys)). A result below limit is exact;
     * equality means at least limit matches, not necessarily exactly limit.
     * Use limit=1 for existence or limit=N for a threshold check. Negative limits
     * are rejected; zero returns zero after argument/backend/key admission.
     * The shared count engine stops when the limit-th DISTINCT row is found.
     * It retains O(query keys) primitive scratch, never a result/universe bitmap.
     */
    public int countAnyUpTo(int limit, int... queryKeys) {
        return countAnyUpTo(Backend.JAVA, limit, queryKeys);
    }

    /**
     * Required JNI still computes and compares Java/native results. Its complete
     * CSR preflight is not truncated by the limit, even when limit is zero.
     * Thus early termination bounds count traversal, not validation, JNI copies,
     * key admission, wall-clock time, or the number of duplicate occurrences.
     * An old library missing the new symbol fails without disabling old methods.
     */
    public int countAnyUpTo(Backend backend, int limit, int... queryKeys) {
        Objects.requireNonNull(backend, "backend");
        Objects.requireNonNull(queryKeys, "queryKeys");
        if (limit < 0) throw new IllegalArgumentException("negative posting count limit");
        requireNative(backend);
        int[] requested = positions(queryKeys, false);
        int expected = countUnionJava(requested, limit);
        if (backend == Backend.NATIVE_REQUIRED) {
            verifyNativeCount(expected, countUnionUpToNative(rowOrdinals, offsets, requested, rows, limit));
        }
        return expected;
    }

    /**
     * Returns min(limit, countAll(queryKeys)) using the same bounded count engine.
     * Empty conjunction returns min(limit, rows); a missing key returns zero.
     * Failing to find the limit may still require scanning the shortest posting.
     * The result does not expose row IDs or retain mutable query input.
     */
    public int countAllUpTo(int limit, int... queryKeys) {
        return countAllUpTo(Backend.JAVA, limit, queryKeys);
    }

    /** Same parity-first backend contract and argument order as countAnyUpTo. */
    public int countAllUpTo(Backend backend, int limit, int... queryKeys) {
        Objects.requireNonNull(backend, "backend");
        Objects.requireNonNull(queryKeys, "queryKeys");
        if (limit < 0) throw new IllegalArgumentException("negative posting count limit");
        requireNative(backend);
        int[] requested = positions(queryKeys, true);
        // Keep the established absent-AND-key shortcut; no native call is claimed.
        if (requested == null) return 0;
        int expected = countIntersectionJava(requested, limit);
        if (backend == Backend.NATIVE_REQUIRED) {
            verifyNativeCount(expected, countIntersectionUpToNative(rowOrdinals, offsets, requested, rows, limit));
        }
        return expected;
    }

    private static native int countUnionUpToNative(
            int[] rowOrdinals, int[] offsets, int[] positions, int rowUniverse, int limit);

    private static native int countIntersectionUpToNative(
            int[] rowOrdinals, int[] offsets, int[] positions, int rowUniverse, int limit);
}
