package com.synexia.primitives;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic typo-tolerant selector for the dense primitive collection substrate.
 *
 * <p>The selector is intentionally not LLM-backed. It normalizes common synonyms and
 * small spelling errors into a fixed vocabulary, infers the requested data shape and
 * returns both a machine-readable plan and the exact concrete no-bloat implementation.
 */
public final class FuzzyStructureFactory {

    public enum Family {
        LIST,
        DEQUE,
        STACK,
        QUEUE,
        SET,
        MAP,
        BAG,
        MULTIMAP,
        BIMAP,
        TABLE,
        RANGE_SET,
        RANGE_MAP,
        HEAP,
        DOUBLE_ENDED_PRIORITY_QUEUE,
        TOP_K,
        LRU,
        COUNTER_MAP,
        PAIR_LIST
    }

    public enum Storage {
        ARRAY,
        HASH,
        SORTED_ARRAY,
        SORTED_GAP,
        INSERTION_ORDERED,
        DIRECT,
        ADAPTIVE,
        COMPRESSED,
        BIG,
        RING,
        BLOCKING_RING,
        SPECIALIZED
    }

    public enum Concurrency {
        NONE,
        SPSC,
        MPSC,
        SPMC,
        MPMC,
        BLOCKING_MPMC
    }

    public record Plan(
            String originalIntent,
            String normalizedIntent,
            Family family,
            Storage storage,
            Concurrency concurrency,
            PrimitiveKind keyKind,
            PrimitiveKind valueKind,
            int expectedSize,
            int limit,
            int rows,
            int columns,
            boolean keepSmallest,
            String implementationType,
            List<String> rationale) {
        public Plan {
            Objects.requireNonNull(originalIntent, "originalIntent");
            Objects.requireNonNull(normalizedIntent, "normalizedIntent");
            Objects.requireNonNull(family, "family");
            Objects.requireNonNull(storage, "storage");
            Objects.requireNonNull(concurrency, "concurrency");
            Objects.requireNonNull(keyKind, "keyKind");
            Objects.requireNonNull(valueKind, "valueKind");
            Objects.requireNonNull(implementationType, "implementationType");
            rationale = List.copyOf(rationale);
            if (expectedSize < 1) throw new IllegalArgumentException("expectedSize < 1");
            if (limit < 1) throw new IllegalArgumentException("limit < 1");
            if (rows < 1 || columns < 1) throw new IllegalArgumentException("table dimensions < 1");
        }

        public Object create() {
            return createFrom(this);
        }
    }

    private static final Map<String, String> VOCABULARY = vocabulary();

    public Plan plan(String intent) {
        String original = Objects.requireNonNull(intent, "intent").trim();
        if (original.isEmpty()) throw new IllegalArgumentException("intent is blank");

        Parsed parsed = parse(original);
        Family family = family(parsed.tokens);
        PrimitiveKind[] kinds = kinds(parsed.tokens);
        PrimitiveKind keyKind = kinds.length == 0 ? PrimitiveKind.LONG : kinds[0];
        PrimitiveKind valueKind =
                kinds.length >= 2 ? kinds[1] : (family == Family.MAP || family == Family.TABLE
                        || family == Family.MULTIMAP || family == Family.BIMAP
                        || family == Family.LRU || family == Family.COUNTER_MAP
                        || family == Family.PAIR_LIST ? PrimitiveKind.LONG : keyKind);

        List<Integer> numbers = parsed.numbers;
        int expectedSize = numbers.isEmpty() ? 1024 : positive(numbers.getFirst(), 1024);
        int limit = numbers.isEmpty() ? 10 : positive(numbers.getFirst(), 10);
        int rows = numbers.size() >= 1 ? positive(numbers.get(0), 64) : 64;
        int columns = numbers.size() >= 2 ? positive(numbers.get(1), 64) : rows;
        boolean smallest = parsed.has("smallest") || parsed.has("minimum") || parsed.has("min");
        if (!smallest && !(parsed.has("largest") || parsed.has("maximum") || parsed.has("max"))) {
            smallest = true;
        }

        Concurrency concurrency = concurrency(parsed);
        Storage storage = storage(family, parsed, concurrency, keyKind);
        String implementation = implementationType(family, storage, concurrency, keyKind, valueKind);
        List<String> rationale = rationale(family, storage, concurrency, keyKind, valueKind, parsed);

        return new Plan(
                original,
                String.join(" ", parsed.tokens),
                family,
                storage,
                concurrency,
                keyKind,
                valueKind,
                expectedSize,
                limit,
                rows,
                columns,
                smallest,
                implementation,
                rationale);
    }

    public Object create(String intent) {
        return plan(intent).create();
    }

    private static Object createFrom(Plan plan) {
        return switch (plan.family()) {
            case LIST -> switch (plan.storage()) {
                case BIG -> DenseCollections.primitiveBigList(plan.keyKind());
                default -> DenseCollections.primitiveList(plan.keyKind());
            };
            case DEQUE -> DenseCollections.primitiveDeque(plan.keyKind());
            case STACK -> DenseCollections.primitiveStack(plan.keyKind());
            case QUEUE -> queue(plan);
            case SET -> set(plan);
            case MAP -> map(plan);
            case BAG -> plan.storage() == Storage.SORTED_ARRAY
                    || plan.storage() == Storage.SORTED_GAP
                    ? DenseCollections.primitiveSortedBag(plan.keyKind())
                    : DenseCollections.primitiveBag(plan.keyKind());
            case MULTIMAP -> plan.storage() == Storage.SORTED_ARRAY
                    || plan.storage() == Storage.SORTED_GAP
                    ? DenseCollections.primitiveMultiMap(plan.keyKind(), plan.valueKind())
                    : DenseCollections.primitiveHashMultiMap(plan.keyKind(), plan.valueKind());
            case BIMAP -> DenseCollections.primitiveBiMap(plan.keyKind(), plan.valueKind());
            case TABLE -> plan.storage() == Storage.DIRECT
                    ? DenseCollections.primitiveDirectIntTable(
                            plan.valueKind(), plan.rows(), plan.columns())
                    : DenseCollections.primitiveTable(
                            plan.keyKind(), plan.valueKind(), PrimitiveKind.LONG);
            case RANGE_SET -> DenseCollections.longRanges();
            case RANGE_MAP -> DenseCollections.longRangeMap();
            case HEAP -> DenseCollections.primitiveHeap(plan.keyKind());
            case DOUBLE_ENDED_PRIORITY_QUEUE ->
                    DenseCollections.primitiveDoubleEndedPriorityQueue(plan.keyKind());
            case TOP_K -> DenseCollections.primitiveTopK(
                    plan.keyKind(),
                    plan.limit(),
                    plan.keepSmallest()
                            ? PrimitiveTopK.Mode.KEEP_SMALLEST
                            : PrimitiveTopK.Mode.KEEP_LARGEST);
            case LRU -> DenseCollections.primitiveLruMap(
                    plan.keyKind(), plan.valueKind(), plan.expectedSize());
            case COUNTER_MAP -> DenseCollections.primitiveCounterMap(plan.keyKind());
            case PAIR_LIST -> DenseCollections.primitivePairs(plan.keyKind(), plan.valueKind());
        };
    }

    private static Object queue(Plan plan) {
        return switch (plan.concurrency()) {
            case SPSC -> DenseCollections.primitiveSpsc(plan.keyKind(), plan.expectedSize());
            case MPSC -> DenseCollections.primitiveMpsc(plan.keyKind(), plan.expectedSize());
            case SPMC -> DenseCollections.primitiveSpmc(plan.keyKind(), plan.expectedSize());
            case MPMC -> DenseCollections.primitiveMpmc(plan.keyKind(), plan.expectedSize());
            case BLOCKING_MPMC ->
                    DenseCollections.primitiveSemaphoreMpmc(plan.keyKind(), plan.expectedSize());
            case NONE -> DenseCollections.primitiveQueue(plan.keyKind());
        };
    }

    private static Object set(Plan plan) {
        return switch (plan.storage()) {
            case COMPRESSED -> plan.keyKind() == PrimitiveKind.INT
                    ? DenseCollections.roaringInts()
                    : plan.keyKind() == PrimitiveKind.LONG
                            ? DenseCollections.roaringLongs()
                            : DenseCollections.primitiveSet(plan.keyKind());
            case BIG -> DenseCollections.primitiveBigSet(plan.keyKind());
            case INSERTION_ORDERED -> DenseCollections.primitiveOrderedSet(plan.keyKind());
            case SORTED_GAP -> DenseCollections.primitiveGapSortedSet(plan.keyKind());
            case SORTED_ARRAY -> DenseCollections.primitiveSortedSet(plan.keyKind());
            default -> DenseCollections.primitiveSet(plan.keyKind());
        };
    }

    private static Object map(Plan plan) {
        if (plan.storage() == Storage.DIRECT) {
            return switch (plan.keyKind()) {
                case BOOLEAN -> DenseCollections.primitiveDirectBooleanMap(plan.valueKind());
                case BYTE -> DenseCollections.primitiveDirectByteMap(plan.valueKind());
                case SHORT -> DenseCollections.primitiveDirectShortMap(plan.valueKind());
                case CHAR -> DenseCollections.primitiveDirectCharMap(plan.valueKind());
                case INT -> DenseCollections.primitiveDirectIntMap(plan.valueKind());
                default -> DenseCollections.primitiveMap(plan.keyKind(), plan.valueKind());
            };
        }
        if (plan.storage() == Storage.ADAPTIVE) {
            return switch (plan.keyKind()) {
                case BYTE -> DenseCollections.primitiveAdaptiveByteMap(plan.valueKind());
                case SHORT -> DenseCollections.primitiveAdaptiveShortMap(plan.valueKind());
                case CHAR -> DenseCollections.primitiveAdaptiveCharMap(plan.valueKind());
                default -> DenseCollections.adaptivePrimitiveMap(
                        plan.keyKind(), plan.valueKind(), plan.expectedSize());
            };
        }
        return switch (plan.storage()) {
            case BIG -> DenseCollections.primitiveBigMap(plan.keyKind(), plan.valueKind());
            case INSERTION_ORDERED ->
                    DenseCollections.primitiveOrderedMap(plan.keyKind(), plan.valueKind());
            case SORTED_GAP ->
                    DenseCollections.primitiveGapSortedMap(plan.keyKind(), plan.valueKind());
            case SORTED_ARRAY ->
                    DenseCollections.primitiveSortedMap(plan.keyKind(), plan.valueKind());
            default -> DenseCollections.primitiveMap(plan.keyKind(), plan.valueKind());
        };
    }

    private static Family family(List<String> tokens) {
        if (contains(tokens, "top")) return Family.TOP_K;
        if (contains(tokens, "lru") || contains(tokens, "cache")) return Family.LRU;
        if (contains(tokens, "range") && contains(tokens, "map")) return Family.RANGE_MAP;
        if (contains(tokens, "range") && contains(tokens, "set")) return Family.RANGE_SET;
        if (contains(tokens, "counter")) return Family.COUNTER_MAP;
        if (contains(tokens, "multimap")) return Family.MULTIMAP;
        if (contains(tokens, "bimap")) return Family.BIMAP;
        if (contains(tokens, "table")) return Family.TABLE;
        if (contains(tokens, "double") && contains(tokens, "priority")) {
            return Family.DOUBLE_ENDED_PRIORITY_QUEUE;
        }
        if (contains(tokens, "heap") || contains(tokens, "priority")) return Family.HEAP;
        if (contains(tokens, "queue")) return Family.QUEUE;
        if (contains(tokens, "stack")) return Family.STACK;
        if (contains(tokens, "deque")) return Family.DEQUE;
        if (contains(tokens, "bag") || contains(tokens, "multiset")) return Family.BAG;
        if (contains(tokens, "pair")) return Family.PAIR_LIST;
        if (contains(tokens, "map")) return Family.MAP;
        if (contains(tokens, "set") || contains(tokens, "unique")) return Family.SET;
        if (contains(tokens, "list") || contains(tokens, "array")) return Family.LIST;
        throw new IllegalArgumentException(
                "Cannot infer collection family from intent tokens: " + tokens);
    }

    private static Storage storage(
            Family family, Parsed parsed, Concurrency concurrency, PrimitiveKind keyKind) {
        if (family == Family.QUEUE && concurrency != Concurrency.NONE) {
            return concurrency == Concurrency.BLOCKING_MPMC ? Storage.BLOCKING_RING : Storage.RING;
        }
        if (parsed.has("compressed") || parsed.has("roaring")) return Storage.COMPRESSED;
        if (parsed.has("huge") || parsed.has("big")) return Storage.BIG;
        if (parsed.has("direct") || parsed.has("dense")) return Storage.DIRECT;
        if (parsed.has("adaptive") || parsed.has("sparse")) return Storage.ADAPTIVE;
        if (parsed.has("ordered") || parsed.has("insertion")) return Storage.INSERTION_ORDERED;
        if (parsed.has("sorted")) {
            if (parsed.has("mutation") || parsed.has("update") || parsed.has("heavy")) {
                return Storage.SORTED_GAP;
            }
            return Storage.SORTED_ARRAY;
        }
        if (family == Family.TABLE && keyKind == PrimitiveKind.INT) return Storage.DIRECT;
        if (family == Family.LIST || family == Family.DEQUE || family == Family.STACK) {
            return Storage.ARRAY;
        }
        return Storage.HASH;
    }

    private static Concurrency concurrency(Parsed parsed) {
        if (parsed.has("blocking")) return Concurrency.BLOCKING_MPMC;
        if (parsed.has("spsc")) return Concurrency.SPSC;
        if (parsed.has("mpsc")) return Concurrency.MPSC;
        if (parsed.has("spmc")) return Concurrency.SPMC;
        if (parsed.has("mpmc")) return Concurrency.MPMC;

        int producerCount = roleCardinality(parsed, "producer", "producers");
        int consumerCount = roleCardinality(parsed, "consumer", "consumers");
        if (producerCount != 0 && consumerCount != 0) {
            if (producerCount == 1) {
                return consumerCount == 1 ? Concurrency.SPSC : Concurrency.SPMC;
            }
            return consumerCount == 1 ? Concurrency.MPSC : Concurrency.MPMC;
        }

        boolean many = parsed.has("many") || parsed.has("multiple");
        boolean producers = parsed.has("producer") || parsed.has("producers");
        boolean consumers = parsed.has("consumer") || parsed.has("consumers");
        boolean one = parsed.has("one") || parsed.has("single");

        if (many && producers && one && consumers) return Concurrency.MPSC;
        if (one && producers && many && consumers) return Concurrency.SPMC;
        if (one && producers && one && consumers) return Concurrency.SPSC;
        if (many && producers && consumers) return Concurrency.MPMC;
        return Concurrency.NONE;
    }

    /** Zero retains the existing fallback for absent or contradictory role quantities. */
    private static int roleCardinality(Parsed parsed, String singular, String plural) {
        int result = 0;
        for (int index = 1; index < parsed.tokens.size(); index++) {
            String role = parsed.tokens.get(index);
            if (!role.equals(singular) && !role.equals(plural)) continue;
            int current = switch (parsed.tokens.get(index - 1)) {
                case "one", "single" -> 1;
                case "many", "multiple" -> 2;
                default -> 0;
            };
            if (current == 0) continue;
            if (result != 0 && result != current) return 0;
            result = current;
        }
        return result;
    }

    private static PrimitiveKind[] kinds(List<String> tokens) {
        List<PrimitiveKind> kinds = new ArrayList<>();
        for (String token : tokens) {
            PrimitiveKind kind = switch (token) {
                case "boolean" -> PrimitiveKind.BOOLEAN;
                case "byte" -> PrimitiveKind.BYTE;
                case "short" -> PrimitiveKind.SHORT;
                case "char" -> PrimitiveKind.CHAR;
                case "int", "integer" -> PrimitiveKind.INT;
                case "long" -> PrimitiveKind.LONG;
                case "float" -> PrimitiveKind.FLOAT;
                case "double" -> PrimitiveKind.DOUBLE;
                default -> null;
            };
            if (kind != null) kinds.add(kind);
        }
        return kinds.toArray(PrimitiveKind[]::new);
    }

    private static String implementationType(
            Family family,
            Storage storage,
            Concurrency concurrency,
            PrimitiveKind keyKind,
            PrimitiveKind valueKind) {
        if (family == Family.QUEUE) {
            return switch (concurrency) {
                case SPSC -> PrimitiveSpscRingBuffer.class.getName();
                case MPSC -> PrimitiveMpscRingBuffer.class.getName();
                case SPMC -> PrimitiveSpmcRingBuffer.class.getName();
                case MPMC -> PrimitiveMpmcRingBuffer.class.getName();
                case BLOCKING_MPMC -> PrimitiveSemaphoreMpmcQueue.class.getName();
                case NONE -> PrimitiveQueue.class.getName();
            };
        }
        return switch (family) {
            case LIST -> storage == Storage.BIG
                    ? PrimitiveBigList.class.getName() : PrimitiveArrayList.class.getName();
            case DEQUE -> PrimitiveArrayDeque.class.getName();
            case STACK -> PrimitiveStack.class.getName();
            case SET -> switch (storage) {
                case COMPRESSED -> keyKind == PrimitiveKind.INT
                        ? PrimitiveRoaringIntSet.class.getName()
                        : keyKind == PrimitiveKind.LONG
                                ? PrimitiveRoaringLongSet.class.getName()
                                : PrimitiveHashSet.class.getName();
                case BIG -> PrimitiveBigHashSet.class.getName();
                case INSERTION_ORDERED -> PrimitiveOrderedSet.class.getName();
                case SORTED_GAP -> PrimitiveSortedGapSet.class.getName();
                case SORTED_ARRAY -> PrimitiveSortedArraySet.class.getName();
                default -> PrimitiveHashSet.class.getName();
            };
            case MAP -> switch (storage) {
                case BIG -> PrimitiveBigHashMap.class.getName();
                case INSERTION_ORDERED -> PrimitiveOrderedMap.class.getName();
                case SORTED_GAP -> PrimitiveSortedGapMap.class.getName();
                case SORTED_ARRAY -> PrimitiveSortedArrayMap.class.getName();
                case DIRECT -> directType(keyKind);
                case ADAPTIVE -> adaptiveType(keyKind);
                default -> PrimitivePrimitiveHashMap.class.getName();
            };
            case BAG -> storage == Storage.SORTED_ARRAY || storage == Storage.SORTED_GAP
                    ? PrimitiveSortedBag.class.getName() : PrimitiveBag.class.getName();
            case MULTIMAP -> storage == Storage.SORTED_ARRAY || storage == Storage.SORTED_GAP
                    ? PrimitiveSortedMultiMap.class.getName() : PrimitiveHashMultiMap.class.getName();
            case BIMAP -> PrimitiveBiMap.class.getName();
            case TABLE -> storage == Storage.DIRECT
                    ? PrimitiveDirectIntTable.class.getName() : PrimitiveSortedTable.class.getName();
            case RANGE_SET -> LongRangeSet.class.getName();
            case RANGE_MAP -> LongLongRangeMap.class.getName();
            case HEAP -> PrimitiveMinHeap.class.getName();
            case DOUBLE_ENDED_PRIORITY_QUEUE ->
                    PrimitiveDoubleEndedPriorityQueue.class.getName();
            case TOP_K -> PrimitiveTopK.class.getName();
            case LRU -> PrimitiveLruMap.class.getName();
            case COUNTER_MAP -> PrimitiveCounterMap.class.getName();
            case PAIR_LIST -> PrimitivePairList.class.getName();
            case QUEUE -> throw new IllegalStateException("queue handled above");
        };
    }

    private static String directType(PrimitiveKind keyKind) {
        return switch (keyKind) {
            case BOOLEAN -> PrimitiveDirectBooleanMap.class.getName();
            case BYTE -> PrimitiveDirectByteMap.class.getName();
            case SHORT -> PrimitiveDirectShortMap.class.getName();
            case CHAR -> PrimitiveDirectCharMap.class.getName();
            case INT -> PrimitiveDirectIntMap.class.getName();
            default -> PrimitivePrimitiveHashMap.class.getName();
        };
    }

    private static String adaptiveType(PrimitiveKind keyKind) {
        return switch (keyKind) {
            case BYTE -> PrimitiveAdaptiveByteMap.class.getName();
            case SHORT -> PrimitiveAdaptiveShortMap.class.getName();
            case CHAR -> PrimitiveAdaptiveCharMap.class.getName();
            default -> PrimitiveAdaptiveMap.class.getName();
        };
    }

    private static List<String> rationale(
            Family family,
            Storage storage,
            Concurrency concurrency,
            PrimitiveKind keyKind,
            PrimitiveKind valueKind,
            Parsed parsed) {
        List<String> result = new ArrayList<>();
        result.add("family=" + family);
        result.add("storage=" + storage);
        result.add("keyKind=" + keyKind);
        if (family == Family.MAP || family == Family.MULTIMAP || family == Family.BIMAP
                || family == Family.LRU || family == Family.TABLE || family == Family.PAIR_LIST) {
            result.add("valueKind=" + valueKind);
        }
        if (concurrency != Concurrency.NONE) result.add("concurrency=" + concurrency);
        if (!parsed.corrections.isEmpty()) result.add("fuzzyCorrections=" + parsed.corrections);
        return result;
    }

    private static Parsed parse(String intent) {
        String raw = intent.toLowerCase(Locale.ROOT);
        String[] parts = raw.split("[^a-z0-9]+");
        List<String> tokens = new ArrayList<>();
        List<Integer> numbers = new ArrayList<>();
        List<String> corrections = new ArrayList<>();

        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (part.chars().allMatch(Character::isDigit)) {
                try {
                    numbers.add(Integer.parseInt(part));
                } catch (NumberFormatException ignored) {
                    numbers.add(Integer.MAX_VALUE);
                }
                continue;
            }
            String canonical = canonical(part);
            tokens.add(canonical);
            if (!canonical.equals(part)) corrections.add(part + "->" + canonical);
        }
        return new Parsed(tokens, numbers, corrections);
    }

    private static String canonical(String token) {
        // Preserve the typed-map connector before approximate family selection.
        if ("to".equals(token)) return token;
        String exact = VOCABULARY.get(token);
        if (exact != null) return exact;

        String best = token;
        int bestDistance = Integer.MAX_VALUE;
        for (Map.Entry<String, String> entry : VOCABULARY.entrySet()) {
            int threshold = token.length() <= 4 ? 1 : 2;
            int distance = distance(token, entry.getKey(), threshold);
            if (distance < bestDistance && distance <= threshold) {
                bestDistance = distance;
                best = entry.getValue();
            }
        }
        return best;
    }

    private static int distance(String left, String right, int cutoff) {
        if (Math.abs(left.length() - right.length()) > cutoff) return cutoff + 1;
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) previous[j] = j;

        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            int rowMin = current[0];
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(
                        Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + cost);
                rowMin = Math.min(rowMin, current[j]);
            }
            if (rowMin > cutoff) return cutoff + 1;
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()];
    }

    private static Map<String, String> vocabulary() {
        Map<String, String> map = new LinkedHashMap<>();
        add(map, "sorted", "sorted", "sort", "soreted");
        add(map, "ordered", "ordered", "order");
        add(map, "insertion", "insertion", "insert");
        add(map, "mutation", "mutation", "mutating");
        add(map, "update", "update", "updates");
        add(map, "heavy", "heavy", "hevy");
        add(map, "dense", "dense");
        add(map, "direct", "direct");
        add(map, "sparse", "sparse");
        add(map, "adaptive", "adaptive");
        add(map, "compressed", "compressed", "compress");
        add(map, "roaring", "roaring");
        add(map, "big", "big");
        add(map, "huge", "huge", "large");
        add(map, "list", "list");
        add(map, "array", "array");
        add(map, "deque", "deque");
        add(map, "stack", "stack");
        add(map, "queue", "queue", "quue");
        add(map, "set", "set");
        add(map, "unique", "unique");
        add(map, "map", "map");
        add(map, "bag", "bag");
        add(map, "multiset", "multiset");
        add(map, "multimap", "multimap");
        add(map, "bimap", "bimap");
        add(map, "table", "table");
        add(map, "range", "range");
        add(map, "heap", "heap");
        add(map, "priority", "priority");
        add(map, "double", "double");
        add(map, "top", "top");
        add(map, "smallest", "smallest", "bottom");
        add(map, "largest", "largest");
        add(map, "minimum", "minimum");
        add(map, "maximum", "maximum");
        add(map, "min", "min");
        add(map, "max", "max");
        add(map, "counter", "counter", "count");
        add(map, "cache", "cache");
        add(map, "lru", "lru");
        add(map, "pair", "pair", "pairs");
        add(map, "blocking", "blocking", "block");
        add(map, "spsc", "spsc");
        add(map, "mpsc", "mpsc");
        add(map, "spmc", "spmc");
        add(map, "mpmc", "mpmc");
        add(map, "many", "many");
        add(map, "multiple", "multiple", "multi");
        add(map, "one", "one");
        add(map, "single", "single");
        add(map, "producer", "producer");
        add(map, "producers", "producers");
        add(map, "consumer", "consumer");
        add(map, "consumers", "consumers");
        add(map, "boolean", "boolean", "bool");
        add(map, "byte", "byte");
        add(map, "short", "short");
        add(map, "char", "char", "character");
        add(map, "int", "int");
        add(map, "integer", "integer");
        add(map, "long", "long");
        add(map, "float", "float");
        add(map, "double", "double");
        return Map.copyOf(map);
    }

    private static void add(Map<String, String> map, String canonical, String... aliases) {
        for (String alias : aliases) map.put(alias, canonical);
    }

    private static boolean contains(List<String> tokens, String value) {
        return tokens.contains(value);
    }

    private static int positive(int value, int fallback) {
        return value > 0 ? value : fallback;
    }

    private record Parsed(
            List<String> tokens,
            List<Integer> numbers,
            List<String> corrections) {
        Parsed {
            tokens = List.copyOf(tokens);
            numbers = List.copyOf(numbers);
            corrections = List.copyOf(corrections);
        }

        boolean has(String token) {
            return tokens.contains(token);
        }
    }
}
