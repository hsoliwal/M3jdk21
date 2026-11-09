/* Copyright 2026 Synexia. SPDX-License-Identifier: Apache-2.0
 * Prewritten proposal adapted from Spring6.2.0 test scenarios; no donor body copied.
 * See FOSS_REUSE_DECISION.tsv and PROTOCOL_PLAN.json. Runtime baseline pending.
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;

/** Public-API proposal. Concrete traces are recorded, not guessed from source. */
public final class SpringCaseInsensitiveMapProtocolProof {
    public static final int SEED = 1592594996;
    public static final int ORDINARY_OPERATIONS = 4096;
    private static final String[] QUERIES = {"A", "a", "B", "b", "Ghost", "gHoSt", "I", "i", ""};
    private static final Locale[] LOCALES = {Locale.ROOT, Locale.ENGLISH,
        Locale.forLanguageTag("tr"), Locale.forLanguageTag("az"), Locale.forLanguageTag("lt")};
    private static final String[] UNICODE_KEYS = {"", "A", "a", "I", "i", "\u0130", "\u0131",
        "\u039f\u03a3", "\u03bf\u03c3", "\u03bf\u03c2", "I\u0307", "I\u0301",
        "\ud801\udc00", "\ud801\udc28", "\ud800", "\udc00", "a\u0080", "ba", "\u0000"};
    private static Class<?> owner;
    private static long checks;
    private static long observations;
    private static final StringBuilder commonLog = new StringBuilder();
    private static final StringBuilder observationLog = new StringBuilder();

    private SpringCaseInsensitiveMapProtocolProof() { }
    @FunctionalInterface private interface Action { Object run() throws Exception; }
    private static final class Boom extends RuntimeException {
        @Serial private static final long serialVersionUID = 1L;
    }
    private static final class StringBomb {
        @Override public String toString() { throw new Boom(); }
    }
    private static final class Value implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private final int id;
        private int revision;
        Value(int id) { this.id = id; }
        @Override public String toString() { return "V" + id + ":" + revision; }
    }
    private static final class EqualsProbe {
        private final List<String> trace;
        EqualsProbe(List<String> trace) { this.trace = trace; }
        @Override public boolean equals(Object value) {
            trace.add(encode(value));
            if (trace.size() == 2) throw new Boom();
            return false;
        }
        @Override public int hashCode() { return 0; }
    }

    private static String quote(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char c = value.charAt(index);
            if (c == '"' || c == '\\') out.append('\\').append(c);
            else if (c < 32 || c >= 127) {
                out.append("\\u");
                String hex = Integer.toHexString(c);
                out.append("0".repeat(4 - hex.length())).append(hex);
            } else out.append(c);
        }
        return out.append('"').toString();
    }
    private static String encode(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) return quote(text);
        if (value instanceof Number || value instanceof Boolean) return String.valueOf(value);
        if (value instanceof Value token) return quote(token.toString());
        if (value instanceof Map<?, ?>) return quote("MAP_REFERENCE");
        if (value instanceof Map.Entry<?, ?> entry) return "[" + encode(entry.getKey()) + "," + encode(entry.getValue()) + "]";
        if (value instanceof Object[] array) {
            List<String> parts = new ArrayList<>();
            for (Object element : array) parts.add(encode(element));
            return "[" + String.join(",", parts) + "]";
        }
        if (value instanceof Collection<?> collection) {
            List<String> parts = new ArrayList<>();
            for (Object element : collection) parts.add(encode(element));
            return "[" + String.join(",", parts) + "]";
        }
        return quote(value.getClass().getSimpleName());
    }
    private static String outcome(Action action) {
        try { return "{\"return\":" + encode(action.run()) + "}"; }
        catch (Exception failure) {
            Throwable actual = failure;
            while (actual instanceof InvocationTargetException wrapped && wrapped.getCause() != null) actual = wrapped.getCause();
            return "{\"exception\":" + quote(actual.getClass().getSimpleName()) + "}";
        }
    }
    private static String snapshot(Map<String, Object> map) {
        List<Object> entries = new ArrayList<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            entries.add(new Object[]{entry.getKey(), entry.getValue()});
        }
        List<Object> queries = new ArrayList<>();
        for (String query : QUERIES) {
            queries.add(new Object[]{query, outcome(() -> map.containsKey(query)),
                outcome(() -> map.keySet().contains(query)), outcome(() -> map.get(query)),
                outcome(() -> map.getOrDefault(query, "FALLBACK"))});
        }
        return "{\"size\":" + map.size() + ",\"empty\":" + map.isEmpty()
            + ",\"entries\":" + encode(entries) + ",\"queries\":" + encode(queries) + "}";
    }
    private static void record(String id, Map<String, Object> map, Action action, List<String> callbacks) {
        String result = outcome(action);
        String line = "{\"case\":" + quote(id) + ",\"result\":" + result
            + ",\"callbacks\":" + encode(callbacks) + ",\"state\":" + snapshot(map) + "}";
        observationLog.append(line).append('\n');
        observations++;
        System.out.println(line);
    }
    private static void check(boolean condition, String id) {
        checks++;
        commonLog.append(id).append(':').append(condition).append('\n');
        if (!condition) throw new AssertionError(id);
    }
    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Object value) {
        if (!(value instanceof Map<?, ?>)) throw new IllegalArgumentException("public factory is not Map");
        return (Map<String, Object>) value;
    }
    private static Map<String, Object> create(Locale locale) throws Exception {
        return cast(owner.getConstructor(Locale.class).newInstance(locale));
    }
    private static Map<String, Object> ordinary() throws Exception { return create(Locale.ROOT); }
    private static Map<String, Object> populated() throws Exception {
        Map<String, Object> map = ordinary();
        map.put("A", 1); map.put("B", null); map.put("C", 3);
        return map;
    }
    private static void cleanLaws(Map<String, Object> map, Map<String, Object> model, int step) {
        String id = "clean/" + step + "/";
        check(map.size() == model.size(), id + "size");
        check(map.isEmpty() == model.isEmpty(), id + "empty");
        check(map.equals(model) && model.equals(map), id + "contents");
        check(map.equals(map), id + "reflexive");
        check(map.hashCode() == model.hashCode(), id + "entryhash");
        int count = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            count++;
            check(map.containsKey(entry.getKey()), id + "enumerated-key");
            check(Objects.equals(map.get(entry.getKey()), entry.getValue()), id + "enumerated-value");
        }
        check(count == map.size(), id + "entrycount");
        check(map.keySet().toArray().length == count && map.values().toArray().length == count, id + "viewcounts");
        check(map.keySet().containsAll(List.of()) && map.values().containsAll(List.of())
            && map.entrySet().containsAll(List.of()), id + "containsAll-empty");
        Object[] output = map.values().toArray(new Object[count + 2]);
        check(output[count] == null, id + "array-null-trailer");
        Object old = count == 0 ? null : map.values().iterator().next();
        if (count != 0) output[0] = "DETACHED";
        check(count == 0 || Objects.equals(map.values().iterator().next(), old), id + "array-detached");
    }
    private static void commonModel() throws Exception {
        Map<String, Object> map = ordinary();
        Map<String, Object> model = new LinkedHashMap<>();
        Random random = new Random(SEED);
        for (int step = 0; step < ORDINARY_OPERATIONS; step++) {
            String key = "k" + random.nextInt(32);
            Integer value = random.nextInt(5) == 0 ? null : random.nextInt(1000);
            Integer nonnull = random.nextInt(1000);
            int operation = random.nextInt(16);
            Object actual;
            Object expected;
            switch (operation) {
                case 0 -> { actual = map.put(key, value); expected = model.put(key, value); }
                case 1 -> { actual = map.remove(key); expected = model.remove(key); }
                case 2 -> { actual = map.putIfAbsent(key, value); expected = model.putIfAbsent(key, value); }
                case 3 -> { actual = map.getOrDefault(key, nonnull); expected = model.getOrDefault(key, nonnull); }
                case 4 -> { actual = map.remove(key, value); expected = model.remove(key, value); }
                case 5 -> { actual = map.replace(key, value); expected = model.replace(key, value); }
                case 6 -> { actual = map.replace(key, nonnull, value); expected = model.replace(key, nonnull, value); }
                case 7 -> { actual = map.computeIfAbsent(key, ignored -> nonnull); expected = model.computeIfAbsent(key, ignored -> nonnull); }
                case 8 -> { actual = map.computeIfPresent(key, (ignored, prior) -> value); expected = model.computeIfPresent(key, (ignored, prior) -> value); }
                case 9 -> { actual = map.compute(key, (ignored, prior) -> value); expected = model.compute(key, (ignored, prior) -> value); }
                case 10 -> { actual = map.merge(key, nonnull, (prior, supplied) -> value); expected = model.merge(key, nonnull, (prior, supplied) -> value); }
                case 11 -> { actual = map.values().remove(value); expected = model.values().remove(value); }
                case 12 -> { Map.Entry<String, Object> entry = new AbstractMap.SimpleImmutableEntry<>(key, value);
                    actual = map.entrySet().remove(entry); expected = model.entrySet().remove(entry); }
                case 13 -> { map.replaceAll((ignored, prior) -> prior == null ? null : ((Integer) prior + 1));
                    model.replaceAll((ignored, prior) -> prior == null ? null : ((Integer) prior + 1)); actual = null; expected = null; }
                case 14 -> { actual = map.containsKey(key); expected = model.containsKey(key); }
                default -> { if (step % 17 == 0) { map.clear(); model.clear(); } actual = null; expected = null; }
            }
            check(Objects.equals(actual, expected), "clean/" + step + "/operation-result");
            cleanLaws(map, model, step);
        }
        record("CLEAN/encounter-order-observation", map, () -> new ArrayList<>(map.keySet()), List.of());
    }
    private static void constructorsAndLocale() throws Exception {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));
            for (int kind = 0; kind < 4; kind++) {
                Map<String, Object> map = switch (kind) {
                    case 0 -> cast(owner.getConstructor().newInstance());
                    case 1 -> create(null);
                    case 2 -> cast(owner.getConstructor(int.class).newInstance(12));
                    default -> cast(owner.getConstructor(int.class, Locale.class).newInstance(12, null));
                };
                Locale.setDefault(Locale.ENGLISH);
                map.put("I", 1);
                record("C01/default-capture/" + kind, map, () -> owner.getMethod("getLocale").invoke(map).toString(), List.of());
                Locale.setDefault(Locale.forLanguageTag("tr"));
            }
        } finally { Locale.setDefault(previous); }
        for (int size : new int[]{0, 1, 12, -1, Integer.MIN_VALUE}) {
            Map<String, Object> witness = ordinary();
            record("C01/expected-size/" + size, witness,
                () -> {
                    try { Map<String, Object> made = cast(owner.getConstructor(int.class, Locale.class).newInstance(size, Locale.ROOT));
                        return made.size(); }
                    catch (InvocationTargetException wrapped) {
                        Throwable cause = wrapped.getCause(); return new Object[]{cause.getClass().getSimpleName(), cause.getMessage()};
                    }
                }, List.of());
        }
        for (Locale locale : LOCALES) {
            Map<String, Object> map = create(locale);
            List<String> callbacks = new ArrayList<>();
            for (int index = 0; index < UNICODE_KEYS.length; index++) {
                String key = UNICODE_KEYS[index];
                Object old = map.put(key, index);
                callbacks.add(quote(key) + " old=" + encode(old) + " folded=" + quote(key.toLowerCase(locale)));
            }
            record("C02/unicode/" + locale.toLanguageTag(), map, () -> {
                List<Object> found = new ArrayList<>();
                for (String key : UNICODE_KEYS) found.add(new Object[]{key, map.get(key), map.containsKey(key)});
                return found;
            }, callbacks);
        }
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object rawPut(Map map, Object key) { return map.put(key, 4); }
    private static void ordinaryAndGhostPolicies() throws Exception {
        Map<String, Object> map = populated();
        record("C04/nonString-queries", map, () -> new Object[]{map.containsKey(null), map.get(null), map.getOrDefault(null, 9),
            map.containsKey(23), map.get(23), map.remove(23), map.getOrDefault(23, 9)}, List.of());
        Map<String, Object> nonString = ordinary(); nonString.put("23", 5); StringBomb bomb = new StringBomb();
        record("C04/nonString-must-not-stringify", nonString, () -> new Object[]{
            nonString.get(23), nonString.containsKey(23), nonString.getOrDefault(23, 9),
            outcome(() -> nonString.containsKey(bomb)), outcome(() -> nonString.get(bomb)),
            outcome(() -> nonString.getOrDefault(bomb, 9)), outcome(() -> nonString.remove(bomb))}, List.of());
        record("C04/raw-put-nonString", map, () -> rawPut(map, 23), List.of());
        record("C04/null-put", map, () -> map.put(null, 7), List.of());
        record("C05/containsValue", map, () -> new Object[]{map.containsValue(1), map.containsValue(null), map.containsValue(99)}, List.of());
        List<String> equalityTrace = new ArrayList<>();
        record("C05/value-equality-callback-failure", map, () -> map.containsValue(new EqualsProbe(equalityTrace)), equalityTrace);
        record("C07/same-spelling-equal-object", map, () -> map.put(new String("A"), 4), List.of());
        record("C08/different-spelling", map, () -> map.put("a", 5), List.of());
        record("C10/null-original-spelling", map, () -> map.putIfAbsent("b", 2), List.of());
        record("C11/existing-nonnull-nullfunction", map, () -> map.computeIfAbsent("a", null), List.of());
        Map<String, Object> existingNull = ordinary(); existingNull.put("A", null); List<String> nullTrace = new ArrayList<>();
        record("C11/existingnull-callback-spelling", existingNull, () -> existingNull.computeIfAbsent("a", key -> {
            nullTrace.add(key); return 6;
        }), nullTrace);
        for (String mutation : new String[]{"append", "clear", "throw", "ordinary"}) {
            Map<String, Object> callbackMap = ordinary(); List<String> trace = new ArrayList<>();
            record("C11-C12/computeIfAbsent-callback/" + mutation, callbackMap,
                () -> callbackMap.computeIfAbsent("A", key -> {
                    trace.add(key + ":contains=" + callbackMap.containsKey("a") + ":size=" + callbackMap.size());
                    callbackMutation(callbackMap, mutation, key); return 5;
                }), trace);
        }
        for (int variant = 0; variant < 3; variant++) {
            Map<String, Object> ghost = ordinary();
            List<String> callbacks = new ArrayList<>();
            final int mode = variant;
            record("C12/alias-residue/" + variant, ghost, () -> ghost.computeIfAbsent("Ghost", mode == 2 ? null : key -> {
                callbacks.add(key + ":contains=" + ghost.containsKey("gHoSt") + ":size=" + ghost.size());
                if (mode == 1) throw new Boom();
                return null;
            }), callbacks);
            record("C12/retained-first-callback-spelling/" + variant, ghost,
                () -> ghost.computeIfAbsent("gHoSt", key -> { callbacks.add("repair:" + key); return 8; }), callbacks);
        }
        for (String action : new String[]{"remove", "conditional", "replace", "compute-null", "clear", "key-clear", "value-clear", "entry-clear"}) {
            Map<String, Object> ghost = ordinary(); ghost.computeIfAbsent("Ghost", ignored -> null);
            record("C12/ghost-followup/" + action, ghost, () -> switch (action) {
                case "remove" -> ghost.remove("gHoSt");
                case "conditional" -> ghost.remove("gHoSt", null);
                case "replace" -> ghost.replace("gHoSt", 5);
                case "compute-null" -> ghost.compute("gHoSt", (key, prior) -> null);
                case "clear" -> { ghost.clear(); yield null; }
                case "key-clear" -> { ghost.keySet().clear(); yield null; }
                case "value-clear" -> { ghost.values().clear(); yield null; }
                default -> { ghost.entrySet().clear(); yield null; }
            }, List.of());
        }
        Map<String, Object> nullMap = ordinary(); nullMap.put("A", null);
        record("C24/key-remove-null-value", nullMap, () -> nullMap.keySet().remove("a"), List.of());
        final Map<String, Object> entries = populated();
        record("C26/entry-membership-exact-case", entries, () -> new Object[]{
            entries.entrySet().contains(new AbstractMap.SimpleEntry<>("a", 1)),
            entries.entrySet().contains(new AbstractMap.SimpleEntry<>("A", 1)),
            entries.entrySet().remove(new AbstractMap.SimpleEntry<>("a", 1))}, List.of());
    }
    private static void bulkPolicies() throws Exception {
        Map<String, Object> map = populated();
        record("C09/putAll-self", map, () -> { map.putAll(map); return null; }, List.of());
        List<String> callbacks = new ArrayList<>();
        Map<String, Object> source = new AbstractMap<>() {
            @Override public Set<Map.Entry<String, Object>> entrySet() { throw new Boom(); }
            @Override public boolean isEmpty() { callbacks.add("isEmpty"); return false; }
            @Override public void forEach(java.util.function.BiConsumer<? super String, ? super Object> action) {
                callbacks.add("forEach"); action.accept("a", 10); callbacks.add("after-a");
                action.accept("D", 4); callbacks.add("after-D"); throw new Boom();
            }
        };
        record("C09/putAll-dispatch-prefix", map, () -> { map.putAll(source); return null; }, callbacks);
        for (int view = 0; view < 3; view++) {
            for (String operation : new String[]{"add-empty", "add-one", "add-null", "removeAll-small", "removeAll-large",
                    "retain-empty", "removeIf-prefix-throw", "removeIf-null", "containsAll-null"}) {
                Map<String, Object> target = populated();
                Collection<?> selected = view(target, view);
                List<String> trace = new ArrayList<>();
                List<?> removed = view == 0 ? List.of("a") : view == 1 ? List.of(1) : List.of(new AbstractMap.SimpleEntry<>("A", 1));
                List<?> large = view == 0 ? List.of("a", "b", "c", "absent") : view == 1 ? List.of(1, 2, 3, 4)
                    : List.of(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("B", null),
                        new AbstractMap.SimpleEntry<>("C", 3), new AbstractMap.SimpleEntry<>("D", 4));
                record("C31-34/view=" + view + "/" + operation, target, () -> switch (operation) {
                    case "add-empty" -> addAll(selected, List.of());
                    case "add-one" -> addAll(selected, List.of("new"));
                    case "add-null" -> addAll(selected, null);
                    case "removeAll-small" -> selected.removeAll(removed);
                    case "removeAll-large" -> selected.removeAll(large);
                    case "retain-empty" -> selected.retainAll(List.of());
                    case "removeIf-null" -> selected.removeIf(null);
                    case "containsAll-null" -> selected.containsAll(null);
                    default -> selected.removeIf(value -> { trace.add(encode(value)); if (trace.size() == 2) throw new Boom(); return true; });
                }, trace);
            }
            Map<String, Object> target = populated(); Collection<?> selected = view(target, view);
            record("C31/self-removeAll/view=" + view, target, () -> selected.removeAll(selected), List.of());
        }
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean addAll(Collection target, Collection values) { return target.addAll(values); }
    private static Collection<?> view(Map<String, Object> map, int kind) {
        return switch (kind) { case 0 -> map.keySet(); case 1 -> map.values(); default -> map.entrySet(); };
    }
    private static void callbackMutation(Map<String, Object> map, String mutation, String key) {
        switch (mutation) {
            case "append" -> map.put("D", 4);
            case "remove-current" -> map.remove(key);
            case "remove-next" -> map.remove("B");
            case "clear" -> map.clear();
            case "throw" -> throw new Boom();
            default -> map.put(key, 12);
        }
    }
    private static void defaultsAndCallbacks() throws Exception {
        for (String operation : new String[]{"replace", "replace-conditional", "remove-conditional", "computeIfPresent", "compute", "merge", "replaceAll"}) {
            for (String mutation : new String[]{"ordinary", "append", "remove-current", "remove-next", "clear", "throw"}) {
                Map<String, Object> map = populated(); List<String> trace = new ArrayList<>();
                record("C16-22/" + operation + "/" + mutation, map, () -> switch (operation) {
                    case "replace" -> map.replace("a", 10);
                    case "replace-conditional" -> map.replace("a", 1, 10);
                    case "remove-conditional" -> map.remove("b", null);
                    case "computeIfPresent" -> map.computeIfPresent("a", (key, prior) -> {
                        trace.add(key + ":" + encode(prior)); callbackMutation(map, mutation, key); return 10; });
                    case "compute" -> map.compute("a", (key, prior) -> {
                        trace.add(key + ":" + encode(prior)); callbackMutation(map, mutation, key); return 10; });
                    case "merge" -> map.merge("a", 8, (prior, supplied) -> {
                        trace.add(encode(prior) + ":" + encode(supplied)); callbackMutation(map, mutation, "A"); return 10; });
                    default -> { map.replaceAll((key, prior) -> {
                        trace.add(key + ":" + encode(prior)); if (trace.size() == 1) callbackMutation(map, mutation, key); return 10; }); yield null; }
                }, trace);
            }
        }
        for (int kind = -1; kind < 3; kind++) {
            for (String mutation : new String[]{"append", "remove-current", "remove-next", "clear", "throw", "ordinary"}) {
                Map<String, Object> map = populated(); List<String> trace = new ArrayList<>(); final int selected = kind;
                record("C15-C30/forEach/view=" + kind + "/" + mutation, map, () -> {
                    if (selected == -1) map.forEach((key, value) -> {
                        trace.add(key + ":" + encode(value)); if (trace.size() == 1) callbackMutation(map, mutation, key); });
                    else view(map, selected).forEach(value -> {
                        trace.add(encode(value)); if (trace.size() == 1) callbackMutation(map, mutation, "A"); });
                    return null;
                }, trace);
                Map<String, Object> empty = ordinary();
                record("C15-C30/forEach-null/view=" + kind, empty, () -> {
                    if (selected == -1) empty.forEach(null); else view(empty, selected).forEach(null); return null;
                }, List.of());
            }
        }
        Map<String, Object> map = populated();
        record("C19-C22/null-functions", map, () -> new Object[]{outcome(() -> { map.replaceAll(null); return null; }),
            outcome(() -> map.computeIfPresent("a", null)), outcome(() -> map.compute("a", null)),
            outcome(() -> map.merge("a", null, null)), outcome(() -> map.merge("a", null, (a, b) -> a))}, List.of());
    }
    private static void cursorsAndTraversal() throws Exception {
        for (int kind = 0; kind < 3; kind++) {
            for (String mutation : new String[]{"samecase", "wrongcase", "append", "clear"}) {
                Map<String, Object> map = populated(); Iterator<?> cursor = view(map, kind).iterator();
                List<String> trace = new ArrayList<>(); trace.add(outcome(cursor::next));
                switch (mutation) { case "samecase" -> map.put("A", 9); case "wrongcase" -> map.put("a", 9);
                    case "append" -> map.put("D", 4); default -> map.clear(); }
                record("C28/epoch/view=" + kind + "/" + mutation, map, () -> new Object[]{
                    outcome(cursor::hasNext), outcome(cursor::next), outcome(() -> { cursor.remove(); return null; })}, trace);
            }
            Map<String, Object> empty = ordinary(); Iterator<?> cursor = view(empty, kind).iterator(); empty.clear();
            record("C14-C28/empty-clear/view=" + kind, empty, () -> new Object[]{outcome(cursor::hasNext), outcome(cursor::next),
                outcome(() -> { cursor.remove(); return null; }), outcome(() -> { cursor.forEachRemaining(null); return null; })}, List.of());
            Map<String, Object> map = populated(); Iterator<?> pending = view(map, kind).iterator(); List<String> trace = new ArrayList<>();
            record("C29/remaining-throw-remove/view=" + kind, map, () -> {
                String failure = outcome(() -> { pending.forEachRemaining(value -> { trace.add(encode(value)); throw new Boom(); }); return null; });
                String removed = outcome(() -> { pending.remove(); return null; }); return new Object[]{failure, removed, outcome(pending::next)};
            }, trace);
            Map<String, Object> iteratorMap = populated(); Iterator<?> states = view(iteratorMap, kind).iterator();
            record("C28/state-priority/view=" + kind, iteratorMap, () -> {
                List<Object> results = new ArrayList<>(); results.add(outcome(() -> { states.remove(); return null; }));
                results.add(outcome(states::next)); results.add(outcome(() -> { states.remove(); return null; }));
                results.add(outcome(() -> { states.remove(); return null; }));
                for (int attempts = 0; attempts < 8 && states.hasNext(); attempts++) results.add(outcome(states::next));
                results.add(outcome(states::hasNext));
                results.add(outcome(states::next)); results.add(outcome(() -> { states.forEachRemaining(null); return null; }));
                return results;
            }, List.of());
            Map<String, Object> splitMap = populated(); Spliterator<?> late = view(splitMap, kind).spliterator(); splitMap.put("D", 4);
            List<String> emitted = new ArrayList<>();
            record("C35/latebinding/view=" + kind, splitMap, () -> {
                int flags = late.characteristics(); long size = late.estimateSize(); Spliterator<?> first = late.trySplit();
                if (first != null) first.forEachRemaining(value -> emitted.add(encode(value)));
                late.forEachRemaining(value -> emitted.add(encode(value)));
                return new Object[]{flags, size, outcome(late::getComparator), emitted};
            }, List.of());
            Map<String, Object> boundMap = populated(); Spliterator<?> bound = view(boundMap, kind).spliterator(); bound.estimateSize(); boundMap.put("D", 4);
            List<String> emittedBound = new ArrayList<>();
            record("C35/bound-mutation/view=" + kind, boundMap,
                () -> { bound.forEachRemaining(value -> emittedBound.add(encode(value))); return null; }, emittedBound);
            Map<String, Object> streamMap = populated(); final int chosen = kind;
            record("C35/streams/view=" + kind, streamMap, () -> new Object[]{view(streamMap, chosen).stream().toArray(),
                view(streamMap, chosen).parallelStream().toArray()}, List.of());
            Map<String, Object> emptySplit = ordinary(); Spliterator<?> exhausted = view(emptySplit, kind).spliterator();
            record("C35/nullaction-empty/view=" + kind, emptySplit, () -> new Object[]{
                outcome(() -> exhausted.tryAdvance(null)), outcome(() -> { exhausted.forEachRemaining(null); return null; })}, List.of());
        }
    }
    private static void arraysAndEntries() throws Exception {
        for (int kind = 0; kind < 3; kind++) {
            Map<String, Object> map = populated(); Collection<?> collection = view(map, kind); List<String> trace = new ArrayList<>();
            Object[] caller = new Object[]{"sentinel", "sentinel", "sentinel", "sentinel", "tail"};
            record("C34/arrays/view=" + kind, map, () -> new Object[]{collection.toArray(), collection.toArray(new Object[0]),
                collection.toArray(caller), outcome(() -> collection.toArray((Object[]) null)), collection.toArray(length -> {
                    trace.add("factory:" + length); return new Object[length]; })}, trace);
            String[] typed = new String[]{"untouched0", "untouched1", "untouched2", "untouched3"};
            record("C34/partial-array-store/view=" + kind, map,
                () -> new Object[]{outcome(() -> collection.toArray(typed)), typed}, List.of());
            record("C34/null-generator/view=" + kind, map,
                () -> collection.toArray((java.util.function.IntFunction<Object[]>) null), List.of());
        }
        Map<String, Object> mixed = ordinary(); mixed.put("A", "prefix"); mixed.put("B", 2);
        String[] partial = new String[]{"old0", "old1", "old2"};
        record("C34/middle-array-store-prefix", mixed,
            () -> new Object[]{outcome(() -> mixed.values().toArray(partial)), partial}, List.of());
        Map<String, Object> map = ordinary(); map.put("A", 1); map.put("B", 2);
        Map.Entry<String, Object> entry = map.entrySet().iterator().next();
        check(Objects.equals(entry.setValue(null), 1), "attached-entry/return-old");
        check(map.containsKey("A") && map.get("A") == null, "attached-entry/live-write");
        check(entry.equals(new AbstractMap.SimpleEntry<>("A", null)) && entry.hashCode() == "A".hashCode(), "attached-entry/tuple-law");
        record("C27/attached-entry", map, () -> new Object[]{entry.getKey(), entry.getValue(), entry.toString()}, List.of());
        map.remove("A"); map.put("a", 7);
        record("C27/stale-entry-after-reinsert", map, () -> new Object[]{entry.getKey(), entry.getValue(), entry.setValue(9), entry.getValue()}, List.of());
        Map<String, Object> arrayMap = ordinary(); arrayMap.put("A", 1);
        Object[] output = arrayMap.entrySet().toArray();
        Map.Entry<?, ?> exposed = (Map.Entry<?, ?>) output[0];
        record("C27/array-entry-write", arrayMap, () -> setEntry(exposed, 5), List.of());
        for (String mutation : new String[]{"remove", "clear", "growth", "clone"}) {
            Map<String, Object> heldMap = ordinary(); heldMap.put("A", 1);
            Map.Entry<String, Object> held = heldMap.entrySet().iterator().next(); heldMap.put("A", 2);
            final Map<String, Object> clone = mutation.equals("clone") ? cast(owner.getMethod("clone").invoke(heldMap)) : null;
            if (mutation.equals("remove")) heldMap.remove("A");
            else if (mutation.equals("clear")) heldMap.clear();
            else if (mutation.equals("growth")) for (int index = 0; index < 128; index++) heldMap.put("grow" + index, index);
            record("C27/held-entry/" + mutation, heldMap, () -> {
                Object before = held.getValue(); Object returned = held.setValue(9);
                Object changed = heldMap.get("A"); heldMap.put("A", 3); Object secondReturn = held.setValue(11);
                return new Object[]{before, returned, changed, secondReturn, heldMap.get("A"), clone == null ? null : clone.get("A")};
            }, List.of());
        }
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object setEntry(Map.Entry entry, Object value) { return entry.setValue(value); }
    private static Object roundtrip(Object value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) { output.writeObject(value); }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) { return input.readObject(); }
    }
    private static void clonesAndGraphs() throws Exception {
        Map<String, Object> map = ordinary(); Value shared = new Value(7);
        map.put("left", shared); map.put("right", shared); map.computeIfAbsent("Ghost", ignored -> null);
        Set<String> originalKeys = map.keySet(); Collection<Object> originalValues = map.values(); Set<Map.Entry<String, Object>> originalEntries = map.entrySet();
        Map<String, Object> copy = cast(owner.getMethod("clone").invoke(map));
        check(copy != map && copy.get("left") == shared && copy.get("right") == shared, "clone/shallow-values");
        record("C37/cache-view-identities", copy, () -> new Object[]{copy.keySet() != originalKeys,
            copy.values() != originalValues, copy.entrySet() != originalEntries,
            copy.keySet() == copy.keySet(), copy.values() == copy.values(), copy.entrySet() == copy.entrySet(),
            map.keySet() == originalKeys, map.values() == originalValues, map.entrySet() == originalEntries}, List.of());
        copy.put("new", 9); map.put("source", 10); shared.revision++;
        check(!map.containsKey("new") && !copy.containsKey("source"), "clone/ordinary-directory-independence");
        record("C37/source-after-clone", map, () -> snapshot(copy), List.of());
        record("C37/clone-ghost-policy", copy, () -> new Object[]{copy.containsKey("gHoSt"), copy.get("gHoSt"),
            copy.getOrDefault("gHoSt", 12), owner.getMethod("getLocale").invoke(copy).toString()}, List.of());
        Map<String, Object> secondCopy = cast(owner.getMethod("clone").invoke(copy));
        record("C37/clone-view-outer-coupling", copy, () -> new Object[]{copy.containsKey("new"), copy.keySet().contains("new"),
            copy.containsKey("source"), copy.keySet().contains("source"), secondCopy.containsKey("new"), secondCopy.keySet().contains("new")}, List.of());
        map.put("self", map); copy.put("source-reference", map);
        Object[] graph = (Object[]) roundtrip(new Object[]{map, copy, shared, shared});
        Map<String, Object> received = cast(graph[0]); Map<String, Object> receivedCopy = cast(graph[1]);
        check(received.get("self") == received, "serde/outer-self-reference");
        check(graph[2] == graph[3] && received.get("left") == graph[2] && received.get("right") == graph[2], "serde/shared-value-reference");
        check(receivedCopy.get("source-reference") == received && receivedCopy.get("left") == graph[2], "serde/cross-map-reference");
        record("C38/received-graph-referenceIDs", received, () -> new Object[]{
            "map0.self=map0", "map0.left=map0.right=value0", "map1.source-reference=map0", "map1.left=value0",
            owner.getMethod("getLocale").invoke(received).toString(), received.containsKey("gHoSt")}, List.of());
        record("C38/postread-view-mutation", received, () -> {
            received.put("LEFT", 11); received.values().remove(11); received.put("after", 12); return snapshot(receivedCopy);
        }, List.of());
        Map<String, Object> invalid = ordinary(); invalid.put("bad", new Object());
        record("C38/nonserializable-value", invalid, () -> roundtrip(invalid), List.of());
    }
    private static void equalityObservers() throws Exception {
        Map<String, Object> map = ordinary(); map.put("A", 1); map.put("B", null);
        Map<String, Object> same = new LinkedHashMap<>(); same.put("A", 1); same.put("B", null);
        Map<String, Object> alternate = new LinkedHashMap<>(); alternate.put("a", 1); alternate.put("b", null);
        record("C36/equals-hash-string", map, () -> new Object[]{map.equals(map), map.equals(same), same.equals(map),
            map.equals(alternate), alternate.equals(map), map.hashCode(), map.toString()}, List.of());
        Map<String, Object> ghost = ordinary(); ghost.computeIfAbsent("Ghost", ignored -> null);
        record("C36/ghost-equality", ghost, () -> new Object[]{ghost.equals(Map.of()), Map.of().equals(ghost), ghost.hashCode(), ghost.toString()}, List.of());
    }
    private static String sha(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("exact owner and frozen hook-fixture class required");
        owner = Class.forName(args[0]);
        commonModel(); constructorsAndLocale(); ordinaryAndGhostPolicies(); bulkPolicies();
        defaultsAndCallbacks(); cursorsAndTraversal(); arraysAndEntries(); clonesAndGraphs(); equalityObservers();
        Object hooks = Class.forName(args[1]).getMethod("run").invoke(null);
        if (!(hooks instanceof List<?>)) throw new IllegalArgumentException("hook fixture must return List<String>");
        for (String hook : (List<String>) hooks) {
            String line = "{\"case\":\"C02-C39/protected-hook\",\"trace\":" + quote(hook) + "}";
            observationLog.append(line).append('\n'); observations++; System.out.println(line);
        }
        System.out.println("{\"status\":\"COMMON_CLEAN_LAWS_PASSED_OBSERVATIONS_RECORDED\",\"ordinaryOperations\":"
            + ORDINARY_OPERATIONS + ",\"commonChecks\":" + checks + ",\"selectedObservations\":" + observations
            + ",\"commonSHA256\":" + quote(sha(commonLog.toString())) + ",\"observationSHA256\":" + quote(sha(observationLog.toString()))
            + ",\"scope\":\"prewritten finite public protocol; baseline expectations and candidate comparison separate; no universal ghost law or storage audit\"}");
    }
}
