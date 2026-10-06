// SPDX-License-Identifier: Apache-2.0
package jdk.internal.mindex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Frozen numeric posting index over canonical M3 token coordinates.
 *
 * <p>This is the JDK-internal projection of the numeric Lucene fast path reviewed in Synexia.
 * It retains token IDs, adjacent token-pair IDs, optional relation-pair IDs, and sorted document
 * rows only. No token spelling, analyzer object, Lucene document, or external search object is
 * retained.</p>
 *
 * <p>All IDs are caller-owned M3 coordinates. Equal numeric IDs are meaningful only inside the
 * same coordinate space/generation supplied by the caller.</p>
 */
public final class M3PostingIndex {
    private final M3CoordinateSpace coordinateSpace;
    private final int documentCount;
    private final LongPostings tokens;
    private final PairPostings adjacentPairs;
    private final PairPostings relationPairs;

    private M3PostingIndex(
            M3CoordinateSpace coordinateSpace,
            int documentCount,
            LongPostings tokens,
            PairPostings adjacentPairs,
            PairPostings relationPairs) {
        this.coordinateSpace = Objects.requireNonNull(coordinateSpace, "coordinateSpace");
        this.documentCount = documentCount;
        this.tokens = tokens;
        this.adjacentPairs = adjacentPairs;
        this.relationPairs = relationPairs;
    }

    public static Builder builder(M3CoordinateSpace coordinateSpace) {
        return new Builder(coordinateSpace);
    }

    public M3CoordinateSpace coordinateSpace() {
        return coordinateSpace;
    }

    public int documentCount() {
        return documentCount;
    }

    /** Documents containing the exact canonical token coordinate. */
    public int[] documents(long tokenId) {
        return tokens.documents(tokenId);
    }

    /** Documents containing every requested token ID, independent of order. */
    public int[] documentsContainingAll(long... tokenIds) {
        Objects.requireNonNull(tokenIds, "tokenIds");
        if (tokenIds.length == 0) return new int[0];
        int[] result = null;
        long[] unique = canonicalIds(tokenIds);
        for (long tokenId : unique) {
            int[] postings = tokens.documentsView(tokenId);
            if (postings == null) return new int[0];
            result = result == null ? postings.clone() : intersect(result, postings);
            if (result.length == 0) return result;
        }
        return result == null ? new int[0] : result;
    }

    /** Documents containing the exact adjacent pair at least once. */
    public int[] adjacentPairDocuments(long leftId, long rightId) {
        return adjacentPairs.documents(leftId, rightId);
    }

    /**
     * Candidate documents for an exact phrase.
     *
     * <p>One token is exact token membership. Two tokens are exact adjacent-pair membership.
     * Longer phrases require every adjacent pair in the same document and therefore remain a
     * candidate set; callers needing absolute sequence identity verify against their canonical
     * source.</p>
     */
    public int[] phraseCandidates(long... tokenIds) {
        Objects.requireNonNull(tokenIds, "tokenIds");
        if (tokenIds.length == 0) return new int[0];
        if (tokenIds.length == 1) return documents(tokenIds[0]);

        int[] result = null;
        HashSet<PairKey> seen = new HashSet<>();
        for (int index = 1; index < tokenIds.length; index++) {
            PairKey pair = new PairKey(tokenIds[index - 1], tokenIds[index]);
            if (!seen.add(pair)) continue;
            int[] postings = adjacentPairs.documentsView(pair.left, pair.right);
            if (postings == null) return new int[0];
            result = result == null ? postings.clone() : intersect(result, postings);
            if (result.length == 0) return result;
        }
        return result == null ? new int[0] : result;
    }

    /** Documents containing one caller-defined canonical relation pair. */
    public int[] relationPairDocuments(long leftId, long rightId) {
        return relationPairs.documents(leftId, rightId);
    }

    public int tokenKeyCount() {
        return tokens.keys.length;
    }

    public int adjacentPairKeyCount() {
        return adjacentPairs.left.length;
    }

    public int relationPairKeyCount() {
        return relationPairs.left.length;
    }

    public long retainedPrimitiveBytes() {
        return tokens.retainedPrimitiveBytes()
                + adjacentPairs.retainedPrimitiveBytes()
                + relationPairs.retainedPrimitiveBytes();
    }

    private static int[] intersect(int[] left, int[] right) {
        int[] output = new int[Math.min(left.length, right.length)];
        int a = 0;
        int b = 0;
        int count = 0;
        while (a < left.length && b < right.length) {
            int x = left[a];
            int y = right[b];
            if (x < y) {
                a++;
            } else if (x > y) {
                b++;
            } else {
                output[count++] = x;
                a++;
                b++;
            }
        }
        return count == output.length ? output : Arrays.copyOf(output, count);
    }

    private static long[] canonicalIds(long[] ids) {
        Long[] boxed = new Long[ids.length];
        for (int index = 0; index < ids.length; index++) {
            long id = ids[index];
            if (id == 0L) throw new IllegalArgumentException("M3 token ID must be nonzero");
            boxed[index] = id;
        }
        Arrays.sort(boxed, Long::compareUnsigned);
        long[] result = new long[boxed.length];
        int count = 0;
        for (Long value : boxed) {
            long id = value;
            if (count == 0 || result[count - 1] != id) result[count++] = id;
        }
        return Arrays.copyOf(result, count);
    }

    public static final class Builder {
        private final M3CoordinateSpace coordinateSpace;
        private final ArrayList<long[]> documents = new ArrayList<>();
        private final ArrayList<RelationDraft> relations = new ArrayList<>();

        private Builder(M3CoordinateSpace coordinateSpace) {
            this.coordinateSpace = Objects.requireNonNull(coordinateSpace, "coordinateSpace");
        }

        public int addDocument(long... tokenIds) {
            Objects.requireNonNull(tokenIds, "tokenIds");
            for (long tokenId : tokenIds) {
                if (tokenId == 0L) throw new IllegalArgumentException("M3 token ID must be nonzero");
            }
            documents.add(tokenIds.clone());
            return documents.size() - 1;
        }

        /**
         * Adds a policy-selected relation pair for an existing document.
         *
         * <p>Use this for the same kind of relation-pair surface that Synexia projects into Lucene
         * numeric points. The pair shares the original canonical token IDs.</p>
         */
        public Builder relationPair(int document, long leftId, long rightId) {
            Objects.checkIndex(document, documents.size());
            if (leftId == 0L || rightId == 0L) {
                throw new IllegalArgumentException("M3 relation IDs must be nonzero");
            }
            relations.add(new RelationDraft(document, leftId, rightId));
            return this;
        }

        public M3PostingIndex build() {
            TreeMap<Long, IntList> tokenMap = new TreeMap<>(Long::compareUnsigned);
            TreeMap<PairKey, IntList> adjacentMap = new TreeMap<>(PairKey.ORDER);
            TreeMap<PairKey, IntList> relationMap = new TreeMap<>(PairKey.ORDER);

            for (int document = 0; document < documents.size(); document++) {
                long[] ids = documents.get(document);
                HashSet<Long> seenTokens = new HashSet<>();
                HashSet<PairKey> seenPairs = new HashSet<>();
                for (int index = 0; index < ids.length; index++) {
                    long id = ids[index];
                    if (seenTokens.add(id)) {
                        tokenMap.computeIfAbsent(id, ignored -> new IntList()).add(document);
                    }
                    if (index != 0) {
                        PairKey pair = new PairKey(ids[index - 1], id);
                        if (seenPairs.add(pair)) {
                            adjacentMap.computeIfAbsent(pair, ignored -> new IntList()).add(document);
                        }
                    }
                }
            }

            HashSet<RelationDocKey> seenRelations = new HashSet<>();
            for (RelationDraft relation : relations) {
                RelationDocKey key =
                        new RelationDocKey(relation.document, relation.left, relation.right);
                if (seenRelations.add(key)) {
                    relationMap
                            .computeIfAbsent(
                                    new PairKey(relation.left, relation.right),
                                    ignored -> new IntList())
                            .add(relation.document);
                }
            }

            return new M3PostingIndex(
                    coordinateSpace,
                    documents.size(),
                    LongPostings.freeze(tokenMap),
                    PairPostings.freeze(adjacentMap),
                    PairPostings.freeze(relationMap));
        }
    }

    private static final class LongPostings {
        final long[] keys;
        final int[] offsets;
        final int[] documents;

        private LongPostings(long[] keys, int[] offsets, int[] documents) {
            this.keys = keys;
            this.offsets = offsets;
            this.documents = documents;
        }

        static LongPostings freeze(TreeMap<Long, IntList> source) {
            long[] keys = new long[source.size()];
            int[] offsets = new int[source.size() + 1];
            int total = source.values().stream().mapToInt(IntList::size).sum();
            int[] documents = new int[total];
            int row = 0;
            int cursor = 0;
            for (var entry : source.entrySet()) {
                keys[row] = entry.getKey();
                offsets[row] = cursor;
                cursor = entry.getValue().copyTo(documents, cursor);
                row++;
            }
            offsets[row] = cursor;
            return new LongPostings(keys, offsets, documents);
        }

        int[] documents(long key) {
            int[] view = documentsView(key);
            return view == null ? new int[0] : view.clone();
        }

        int[] documentsView(long key) {
            int row = findUnsigned(keys, key);
            return row < 0
                    ? null
                    : Arrays.copyOfRange(documents, offsets[row], offsets[row + 1]);
        }

        long retainedPrimitiveBytes() {
            return (long) keys.length * Long.BYTES
                    + (long) (offsets.length + documents.length) * Integer.BYTES;
        }
    }

    private static final class PairPostings {
        final long[] left;
        final long[] right;
        final int[] offsets;
        final int[] documents;

        private PairPostings(long[] left, long[] right, int[] offsets, int[] documents) {
            this.left = left;
            this.right = right;
            this.offsets = offsets;
            this.documents = documents;
        }

        static PairPostings freeze(TreeMap<PairKey, IntList> source) {
            long[] left = new long[source.size()];
            long[] right = new long[source.size()];
            int[] offsets = new int[source.size() + 1];
            int total = source.values().stream().mapToInt(IntList::size).sum();
            int[] documents = new int[total];
            int row = 0;
            int cursor = 0;
            for (var entry : source.entrySet()) {
                left[row] = entry.getKey().left;
                right[row] = entry.getKey().right;
                offsets[row] = cursor;
                cursor = entry.getValue().copyTo(documents, cursor);
                row++;
            }
            offsets[row] = cursor;
            return new PairPostings(left, right, offsets, documents);
        }

        int[] documents(long leftId, long rightId) {
            int[] view = documentsView(leftId, rightId);
            return view == null ? new int[0] : view.clone();
        }

        int[] documentsView(long leftId, long rightId) {
            int row = findPair(left, right, leftId, rightId);
            return row < 0
                    ? null
                    : Arrays.copyOfRange(documents, offsets[row], offsets[row + 1]);
        }

        long retainedPrimitiveBytes() {
            return (long) (left.length + right.length) * Long.BYTES
                    + (long) (offsets.length + documents.length) * Integer.BYTES;
        }
    }

    private static int findUnsigned(long[] values, long wanted) {
        int low = 0;
        int high = values.length - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int compared = Long.compareUnsigned(values[middle], wanted);
            if (compared < 0) low = middle + 1;
            else if (compared > 0) high = middle - 1;
            else return middle;
        }
        return -1;
    }

    private static int findPair(
            long[] left, long[] right, long wantedLeft, long wantedRight) {
        int low = 0;
        int high = left.length - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int compared = Long.compareUnsigned(left[middle], wantedLeft);
            if (compared == 0) compared = Long.compareUnsigned(right[middle], wantedRight);
            if (compared < 0) low = middle + 1;
            else if (compared > 0) high = middle - 1;
            else return middle;
        }
        return -1;
    }

    private record PairKey(long left, long right) {
        static final Comparator<PairKey> ORDER =
                (a, b) -> {
                    int compared = Long.compareUnsigned(a.left, b.left);
                    return compared != 0 ? compared : Long.compareUnsigned(a.right, b.right);
                };

        PairKey {
            if (left == 0L || right == 0L) {
                throw new IllegalArgumentException("M3 pair IDs must be nonzero");
            }
        }
    }

    private record RelationDraft(int document, long left, long right) {}

    private record RelationDocKey(int document, long left, long right) {}

    private static final class IntList {
        private int[] values = new int[4];
        private int size;

        void add(int value) {
            if (size == values.length) values = Arrays.copyOf(values, size << 1);
            values[size++] = value;
        }

        int size() {
            return size;
        }

        int copyTo(int[] destination, int offset) {
            System.arraycopy(values, 0, destination, offset, size);
            return offset + size;
        }
    }
}
