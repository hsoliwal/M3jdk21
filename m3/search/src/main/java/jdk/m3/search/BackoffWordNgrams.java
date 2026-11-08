// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Exact word-level conditional n-gram counts with longest-observed-suffix
 * backoff. Distinct from Synexia's existing UTF-16 character NGrams tool.
 *
 * <p>There is no neural inference, BOS/EOS insertion, smoothing, hidden
 * tokenization, training randomness, or cross-document transition. Every
 * observed token contributes exactly one transition for each available
 * history suffix length from zero to order-1.</p>
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/ca1aafc4d94f9dd1ee288226dcca070cc60991c9/synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/nlp-wordrank-20261031/BackoffWordNgrams.java.template blob a995f3bfaab1; recipe m3/nlp/recipe/backoff-word-ngrams-20261106/relocate.py v3
public final class BackoffWordNgrams {
    private BackoffWordNgrams() { }

    public record Limits(int maxDocuments, long maxTokens, int maxContexts,
                         int maxTransitions, long maxObservations,
                         int maxOrder, int maxTokenChars) {
        public Limits {
            if (maxDocuments < 1 || maxTokens < 1 || maxContexts < 1
                    || maxTransitions < 1 || maxObservations < 1
                    || maxOrder < 1 || maxOrder > 8 || maxTokenChars < 1
                    || maxTokenChars > 1_000_000)
                throw new IllegalArgumentException("invalid n-gram limits");
        }
        public static Limits defaults() {
            return new Limits(10_000,1_000_000,1_000_000,
                    2_000_000,8_000_000,4,4_096);
        }
    }
    public record Prediction(String token, long count, long observed,
                             int suffixLength, double probability) {
        public Prediction {
            Objects.requireNonNull(token);
            if (count < 1 || observed < count || suffixLength < 0
                    || !Double.isFinite(probability)
                    || probability <= 0 || probability > 1.0)
                throw new IllegalArgumentException("invalid conditional prediction");
        }
    }
    private record Context(List<String> tokens) {
        private Context { tokens = List.copyOf(tokens); }
    }
    /** Immutable row: lexical lookup and exact-count ranking prepared once at freeze. */
    private static final class Counts {
        private final String[] tokens;
        private final long[] frequencies;
        private final int[] ranked;
        private final long total;
        private Counts(Map<String,Long> targets, long total) {
            this.total = total;
            tokens = targets.keySet().toArray(String[]::new);
            Arrays.sort(tokens);
            frequencies = new long[tokens.length];
            ranked = new int[tokens.length];
            for (int i = 0; i < tokens.length; i++) {
                frequencies[i] = targets.get(tokens[i]);
            }
            prepareRanking();
        }
        /** Production freeze reads primitive counters directly, with no boxed-map round trip. */
        private Counts(MutableCounts source) {
            total = source.total;
            tokens = new String[source.size];
            frequencies = new long[source.size];
            ranked = new int[source.size];
            source.copySorted(source.root, tokens, frequencies, 0);
            prepareRanking();
        }
        private void prepareRanking() {
            for (int i = 0; i < ranked.length; i++) ranked[i] = i;
            // Primitive index heapsort: no boxed comparator keys or retained target map.
            for (int p = ranked.length / 2 - 1; p >= 0; p--)
                sift(p, ranked.length, ranked[p]);
            for (int last = ranked.length - 1; last > 0; last--) {
                int worst = ranked[0];
                sift(0, last, ranked[last]);
                ranked[last] = worst;
            }
        }
        private long count(String token) {
            int slot = Arrays.binarySearch(tokens, token);
            return slot < 0 ? 0L : frequencies[slot];
        }
        private boolean worse(int a, int b) {
            return frequencies[a] < frequencies[b]
                    || (frequencies[a] == frequencies[b] && a > b);
        }
        private void sift(int position, int length, int value) {
            while (position < length / 2) {
                int child = position * 2 + 1;
                if (child + 1 < length && worse(ranked[child + 1], ranked[child])) child++;
                if (!worse(ranked[child], value)) break;
                ranked[position] = ranked[child];
                position = child;
            }
            ranked[position] = value;
        }
    }
    /** Training-local AVL row: primitive counts and links, no boxed value or node entries. */
    private static final class MutableCounts {
        private static final String[] NO_KEYS = new String[0];
        private static final long[] NO_VALUES = new long[0];
        private static final int[] NO_LINKS = new int[0];
        private static final byte[] NO_HEIGHTS = new byte[0];
        private String[] keys = NO_KEYS;
        private long[] values = NO_VALUES;
        private int[] left = NO_LINKS;
        private int[] right = NO_LINKS;
        private byte[] heights = NO_HEIGHTS;
        private int root;
        private int size;
        private long total;

        boolean has(String token) {
            int node = root;
            while (node != 0) {
                int comparison = token.compareTo(keys[node]);
                if (comparison == 0) return true;
                node = comparison < 0 ? left[node] : right[node];
            }
            return false;
        }
        void add(String token) {
            // Overflow is detected before publishing any tree/count mutation.
            long nextTotal = Math.addExact(total, 1L);
            root = insert(root, token);
            total = nextTotal;
        }
        private int insert(int node, String token) {
            if (node == 0) return append(token);
            int comparison = token.compareTo(keys[node]);
            if (comparison == 0) {
                values[node] = Math.addExact(values[node], 1L);
                return node; // Keep the first equal String reference in this row.
            }
            // Recursive insertion may grow/replace every array. Do NOT evaluate an
            // array-element assignment's left-hand array before that recursion.
            if (comparison < 0) {
                int child = insert(left[node], token);
                left[node] = child;
            } else {
                int child = insert(right[node], token);
                right[node] = child;
            }
            updateHeight(node);
            int balance = heights[left[node]] - heights[right[node]];
            if (balance > 1) {
                int child = left[node];
                if (heights[left[child]] < heights[right[child]]) left[node] = rotateLeft(child);
                return rotateRight(node);
            }
            if (balance < -1) {
                int child = right[node];
                if (heights[right[child]] < heights[left[child]]) right[node] = rotateRight(child);
                return rotateLeft(node);
            }
            return node;
        }
        private int append(String token) {
            if (size + 1 >= keys.length) grow();
            int node = size + 1;
            keys[node] = token;
            values[node] = 1L;
            heights[node] = 1;
            size = node;
            return node;
        }
        private void grow() {
            // Zero is the empty-child sentinel. Leave array-size overflow to an
            // explicit OOME rather than wrapping a capacity into a negative size.
            int maximum = Integer.MAX_VALUE - 8;
            if (keys.length == maximum) throw new OutOfMemoryError("training row capacity");
            int capacity = keys.length == 0 ? 4 : (int)Math.min(maximum, 2L * keys.length);
            String[] newKeys = Arrays.copyOf(keys, capacity);
            long[] newValues = Arrays.copyOf(values, capacity);
            int[] newLeft = Arrays.copyOf(left, capacity);
            int[] newRight = Arrays.copyOf(right, capacity);
            byte[] newHeights = Arrays.copyOf(heights, capacity);
            // Publish together only after all allocations/copies have succeeded.
            keys = newKeys; values = newValues;
            left = newLeft; right = newRight; heights = newHeights;
        }
        private void updateHeight(int node) {
            // An AVL tree with int-indexed nodes cannot approach byte-height overflow.
            heights[node] = (byte)(1 + Math.max(heights[left[node]], heights[right[node]]));
        }
        private int rotateLeft(int node) {
            int pivot = right[node];
            right[node] = left[pivot];
            left[pivot] = node;
            updateHeight(node); updateHeight(pivot);
            return pivot;
        }
        private int rotateRight(int node) {
            int pivot = left[node];
            left[node] = right[pivot];
            right[pivot] = node;
            updateHeight(node); updateHeight(pivot);
            return pivot;
        }
        private int copySorted(int node, String[] tokens, long[] counts, int offset) {
            if (node == 0) return offset;
            int next = copySorted(left[node], tokens, counts, offset);
            tokens[next] = keys[node]; counts[next] = values[node];
            return copySorted(right[node], tokens, counts, next + 1);
        }
        Counts freeze() { return new Counts(this); }
    }
    /** Training-local reverse-context trie: array-backed children and rows, no Context map. */
    private static final class MutableContextIndex {
        private static final int ROOT = 1;
        private static final String[] NO_LABELS = new String[0];
        private static final int[] NO_LINKS = new int[0];
        private static final byte[] NO_HEIGHTS = new byte[0];
        private static final MutableCounts[] NO_ROWS = new MutableCounts[0];
        private String[] labels = NO_LABELS;
        private int[] childRoots = NO_LINKS;
        private int[] left = NO_LINKS;
        private int[] right = NO_LINKS;
        private byte[] heights = NO_HEIGHTS;
        private MutableCounts[] rows = NO_ROWS;
        private int size = ROOT;
        private int contexts;

        private MutableCounts root(int maximumContexts) {
            ensureCapacity(ROOT);
            MutableCounts existing = rows[ROOT];
            if (existing != null) return existing;
            if (contexts == maximumContexts)
                throw new IllegalArgumentException("context budget exhausted");
            MutableCounts created = new MutableCounts();
            rows[ROOT] = created;
            contexts++;
            return created;
        }
        private int childOrCreate(int parent, String token, int maximumContexts) {
            int child = findChild(parent, token);
            if (child != 0) return child;
            if (contexts == maximumContexts)
                throw new IllegalArgumentException("context budget exhausted");
            // Allocate the row before publishing a semantic node. Capacity growth may
            // replace private backing arrays, but cannot expose a new context by itself.
            MutableCounts row = new MutableCounts();
            int node = Math.addExact(size, 1);
            ensureCapacity(node);
            labels[node] = token;
            rows[node] = row;
            heights[node] = 1;
            childRoots[parent] = insertSibling(childRoots[parent], node);
            size = node;
            contexts++;
            return node;
        }
        private MutableCounts row(int node) { return rows[node]; }
        private int findChild(int parent, String token) {
            int node = childRoots[parent];
            while (node != 0) {
                int comparison = token.compareTo(labels[node]);
                if (comparison == 0) return node;
                node = comparison < 0 ? left[node] : right[node];
            }
            return 0;
        }
        private int insertSibling(int node, int inserted) {
            if (node == 0) return inserted;
            int comparison = labels[inserted].compareTo(labels[node]);
            if (comparison < 0) left[node] = insertSibling(left[node], inserted);
            else right[node] = insertSibling(right[node], inserted);
            updateHeight(node);
            int balance = heights[left[node]] - heights[right[node]];
            if (balance > 1) {
                int child = left[node];
                if (heights[left[child]] < heights[right[child]]) left[node] = rotateLeft(child);
                return rotateRight(node);
            }
            if (balance < -1) {
                int child = right[node];
                if (heights[right[child]] < heights[left[child]]) right[node] = rotateRight(child);
                return rotateLeft(node);
            }
            return node;
        }
        private void ensureCapacity(int node) {
            if (node < labels.length) return;
            int maximum = Integer.MAX_VALUE - 8;
            if (labels.length == maximum) throw new OutOfMemoryError("training context capacity");
            int capacity = labels.length == 0 ? 4 : (int)Math.min(maximum, 2L * labels.length);
            while (capacity <= node) {
                if (capacity == maximum) throw new OutOfMemoryError("training context capacity");
                capacity = (int)Math.min(maximum, 2L * capacity);
            }
            String[] newLabels = Arrays.copyOf(labels, capacity);
            int[] newChildRoots = Arrays.copyOf(childRoots, capacity);
            int[] newLeft = Arrays.copyOf(left, capacity);
            int[] newRight = Arrays.copyOf(right, capacity);
            byte[] newHeights = Arrays.copyOf(heights, capacity);
            MutableCounts[] newRows = Arrays.copyOf(rows, capacity);
            labels = newLabels; childRoots = newChildRoots;
            left = newLeft; right = newRight; heights = newHeights; rows = newRows;
        }
        private void updateHeight(int node) {
            heights[node] = (byte)(1 + Math.max(heights[left[node]], heights[right[node]]));
        }
        private int rotateLeft(int node) {
            int pivot = right[node];
            right[node] = left[pivot]; left[pivot] = node;
            updateHeight(node); updateHeight(pivot);
            return pivot;
        }
        private int rotateRight(int node) {
            int pivot = left[node];
            left[node] = right[pivot]; right[pivot] = node;
            updateHeight(node); updateHeight(pivot);
            return pivot;
        }
        private int assignIds(int oldNode, int[] oldToNew, int[] newToOld, int next) {
            oldToNew[oldNode] = next;
            newToOld[next] = oldNode;
            return assignChildren(childRoots[oldNode], oldToNew, newToOld, next + 1);
        }
        private int assignChildren(int sibling, int[] oldToNew, int[] newToOld, int next) {
            if (sibling == 0) return next;
            int cursor = assignChildren(left[sibling], oldToNew, newToOld, next);
            cursor = assignIds(sibling, oldToNew, newToOld, cursor);
            return assignChildren(right[sibling], oldToNew, newToOld, cursor);
        }
        private int childCount(int sibling) {
            return sibling == 0 ? 0 : Math.addExact(1,
                    Math.addExact(childCount(left[sibling]), childCount(right[sibling])));
        }
        private int copyChildren(int sibling, int[] oldToNew,
                                 String[] targetLabels, int[] targetChildren, int offset) {
            if (sibling == 0) return offset;
            int next = copyChildren(left[sibling], oldToNew, targetLabels, targetChildren, offset);
            targetLabels[next] = labels[sibling];
            targetChildren[next] = oldToNew[sibling];
            return copyChildren(right[sibling], oldToNew, targetLabels, targetChildren, next + 1);
        }
    }

    /** Reverse-token trie in compressed rows; builder maps and Context keys are not retained. */
    private static final class ContextIndex {
        private final String[] labels;
        private final int[] offsets;
        private final int[] children;
        private final Counts[] rows;
        private ContextIndex(MutableContextIndex source) {
            if (source.contexts == 0) {
                labels = new String[0]; offsets = new int[1];
                children = new int[0]; rows = new Counts[0];
                return;
            }
            if (source.contexts != source.size || source.rows[MutableContextIndex.ROOT] == null)
                throw new IllegalStateException("training context index corruption");
            int count = source.contexts;
            rows = new Counts[count];
            offsets = new int[Math.addExact(count, 1)];
            labels = new String[count - 1];
            children = new int[labels.length];
            int[] oldToNew = new int[Math.addExact(source.size, 1)];
            int[] newToOld = new int[count];
            int assigned = source.assignIds(MutableContextIndex.ROOT, oldToNew, newToOld, 0);
            if (assigned != count) throw new IllegalStateException("unreachable training context");
            for (int node = 0; node < count; node++) {
                int oldNode = newToOld[node];
                rows[node] = source.rows[oldNode].freeze();
                offsets[node + 1] = Math.addExact(offsets[node], source.childCount(source.childRoots[oldNode]));
            }
            for (int node = 0; node < count; node++) {
                int oldNode = newToOld[node];
                int end = source.copyChildren(source.childRoots[oldNode], oldToNew,
                        labels, children, offsets[node]);
                if (end != offsets[node + 1]) throw new IllegalStateException("context edge count corruption");
            }
        }
        private ContextIndex(Map<Context,Counts> source) {
            Context[] ordered = source.keySet().toArray(Context[]::new);
            Arrays.sort(ordered, ContextIndex::compareReversed);
            rows = new Counts[ordered.length];
            offsets = new int[Math.addExact(ordered.length, 1)];
            labels = new String[Math.max(0, ordered.length - 1)];
            children = new int[labels.length];
            if (ordered.length == 0) return;
            if (!ordered[0].tokens().isEmpty())
                throw new IllegalStateException("missing root context");
            Map<Context,Integer> nodeIds = new HashMap<>();
            for (int node = 0; node < ordered.length; node++) {
                nodeIds.put(ordered[node], node);
                rows[node] = source.get(ordered[node]);
            }
            int[] parents = new int[ordered.length];
            for (int node = 1; node < ordered.length; node++) {
                List<String> tokens = ordered[node].tokens();
                Integer parent = nodeIds.get(new Context(tokens.subList(1, tokens.size())));
                // Training observes every shorter suffix with the same next token.
                if (parent == null || parent >= node)
                    throw new IllegalStateException("context suffix closure violated");
                parents[node] = parent;
                offsets[parent + 1]++;
            }
            for (int node = 1; node < offsets.length; node++)
                offsets[node] = Math.addExact(offsets[node], offsets[node - 1]);
            int[] write = offsets.clone();
            for (int node = 1; node < ordered.length; node++) {
                int edge = write[parents[node]]++;
                labels[edge] = ordered[node].tokens().get(0);
                children[edge] = node;
            }
            // Reverse lexical source order already sorts each parent's edge slice.
        }
        private static int compareReversed(Context left, Context right) {
            List<String> a = left.tokens(), b = right.tokens();
            for (int depth = 1; depth <= Math.min(a.size(), b.size()); depth++) {
                int comparison = a.get(a.size() - depth).compareTo(b.get(b.size() - depth));
                if (comparison != 0) return comparison;
            }
            return Integer.compare(a.size(), b.size());
        }
        private int child(int node, String token) {
            int edge = Arrays.binarySearch(labels, offsets[node], offsets[node + 1], token);
            return edge < 0 ? -1 : children[edge];
        }
    }
    /** Immutable result; source order cannot affect output ranking or ties. */
    public static final class Model {
        private final int order;
        private final long observations;
        private final ContextIndex table;
        private Model(int order, long observations, Map<Context,Counts> table) {
            this.order = order;
            this.observations = observations;
            this.table = new ContextIndex(table);
        }
        private Model(int order, long observations, MutableContextIndex table) {
            this.order = order;
            this.observations = observations;
            this.table = new ContextIndex(table);
        }
        public int order() { return order; }
        public long observations() { return observations; }
        public int contexts() { return table.rows.length; }
        /** Exact count for the supplied context, without backoff. */
        public long exactCount(List<String> context, String next) {
            Objects.requireNonNull(context);
            Objects.requireNonNull(next);
            if (next.isEmpty()) throw new IllegalArgumentException("empty next token");
            List<String> frozen = List.copyOf(context);
            for (String token : frozen) if (token.isEmpty())
                throw new IllegalArgumentException("empty history token");
            if (table.rows.length == 0 || frozen.size() >= order) return 0;
            int node = 0;
            for (int position = frozen.size() - 1; position >= 0; position--) {
                node = table.child(node, frozen.get(position));
                if (node < 0) return 0;
            }
            return table.rows[node].count(next);
        }
        /**
         * Highest-order observed context wins; missing histories back off to
         * shorter suffixes, then to corpus unigram counts. Equal counts break
         * ties by String.compareTo; the returned list is immutable.
         */
        public List<Prediction> rank(List<String> history, int limit) {
            Objects.requireNonNull(history);
            if (limit < 0) throw new IllegalArgumentException("negative rank limit");
            List<String> frozen = List.copyOf(history);
            for (String token : frozen) if (token.isEmpty())
                throw new IllegalArgumentException("empty history token");
            if (limit == 0 || table.rows.length == 0) return List.of();
            int node = 0, length = 0;
            int maximum = Math.min(order - 1, frozen.size());
            while (length < maximum) {
                int child = table.child(node, frozen.get(frozen.size() - 1 - length));
                if (child < 0) break;
                node = child;
                length++;
            }
            Counts counts = table.rows[node];
            int size = Math.min(limit, counts.ranked.length);
            List<Prediction> result = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                int slot = counts.ranked[i];
                long count = counts.frequencies[slot];
                result.add(new Prediction(counts.tokens[slot], count, counts.total,
                        length, count / (double)counts.total));
            }
            return List.copyOf(result);
        }
    }

    public static Model train(List<? extends List<String>> documents,int order) {
        return train(documents,order,Limits.defaults());
    }
    public static Model train(List<? extends List<String>> documents,int order,Limits limits) {
        Objects.requireNonNull(documents);
        Objects.requireNonNull(limits);
        if (order < 1 || order > limits.maxOrder())
            throw new IllegalArgumentException("order outside admitted range");
        if (documents.size() > limits.maxDocuments())
            throw new IllegalArgumentException("document count budget exhausted");
        MutableContextIndex table = new MutableContextIndex();
        long tokens = 0, observations = 0;
        long distinctTransitions = 0;
        for (List<String> document : documents) {
            Objects.requireNonNull(document,"null document");
            if (document.size() > limits.maxTokens()-tokens)
                throw new IllegalArgumentException("token count budget exhausted");
            List<String> doc = List.copyOf(document);
            tokens += doc.size();
            for (String token : doc) {
                if (token.isEmpty() || token.length() > limits.maxTokenChars())
                    throw new IllegalArgumentException("empty or oversized token");
            }
            for (int i=0; i<doc.size(); i++) {
                String next = doc.get(i);
                int node = MutableContextIndex.ROOT;
                for (int length=0; length<=Math.min(i,order-1); length++) {
                    if (observations >= limits.maxObservations())
                        throw new IllegalArgumentException("n-gram observation budget exhausted");
                    MutableCounts counts;
                    if (length == 0) {
                        counts = table.root(limits.maxContexts());
                    } else {
                        node = table.childOrCreate(node, doc.get(i - length), limits.maxContexts());
                        counts = table.row(node);
                    }
                    if (!counts.has(next)) {
                        if (distinctTransitions == limits.maxTransitions())
                            throw new IllegalArgumentException("transition budget exhausted");
                        distinctTransitions++;
                    }
                    counts.add(next);
                    observations++;
                }
            }
        }
        return new Model(order,observations,table);
    }
    public static void main(String[] args) {
        if (args.length != 1 || !args[0].equals("--self-test"))
            throw new IllegalArgumentException("--self-test required");
        Model model = train(List.of(List.of("a","b","a","c")),3);
        if (model.exactCount(List.of("a"),"b") != 1
                || model.exactCount(List.of("a"),"c") != 1
                || model.rank(List.of("a"),2).get(0).probability() != 0.5
                || !model.rank(List.of("z","a","b"),1).get(0).token().equals("a"))
            throw new AssertionError("word n-gram contract");
        System.out.println("M3_JMOD_TEST_PASS text.wordngram");
    }
}
