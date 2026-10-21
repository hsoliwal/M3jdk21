// SPDX-License-Identifier: Apache-2.0
// Public-only ORIGINAL-first calibration. No provider implementation source used.
// Quoting/origin patterns reused from owned EcDefaultReceiverBinding.
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringJoiner;

public final class JdkTreePutValidationOracle {
    private static Class<?> provider;
    private static int cases;
    private JdkTreePutValidationOracle() { }
    private static String q(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }
    private static String json(Object object) {
        if (object == null) return "null";
        if (object instanceof String) return q((String) object);
        if (object instanceof Number || object instanceof Boolean) return object.toString();
        if (object instanceof Map<?, ?>) {
            StringJoiner out = new StringJoiner(",", "{", "}");
            for (Map.Entry<?, ?> e : ((Map<?, ?>) object).entrySet()) out.add(q((String) e.getKey()) + ":" + json(e.getValue()));
            return out.toString();
        }
        if (object instanceof Iterable<?>) {
            StringJoiner out = new StringJoiner(",", "[", "]");
            for (Object x : (Iterable<?>) object) out.add(json(x));
            return out.toString();
        }
        throw new AssertionError("Unsupported observation type " + object.getClass().getName());
    }
    private static void origin(Class<?> type, String property) throws Exception {
        String actual = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().toString();
        if (!actual.equals(System.getProperty(property))) throw new AssertionError(property + " differs:" + actual);
    }
    private static final class Marker extends RuntimeException {
        private static final long serialVersionUID = 1L;
        Marker(String id) { super(id); }
    }
    private static final class Key implements Comparable<Object> {
        final Context context;
        final String id;
        final int number;
        Key(Context context, String id, int number) { this.context = context; this.id = id; this.number = number; }
        @Override public int compareTo(Object other) {
            context.trace.add("natural:" + id + ":" + keyLabel(other) + ":self=" + (other == this));
            return Integer.compare(number, ((Key) other).number);
        }
    }
    private static final class Plain { }
    private static final class WrongComparable implements Comparable<String> {
        final Context context;
        WrongComparable(Context context) { this.context = context; }
        @Override public int compareTo(String other) { context.trace.add("wrong-body:" + other); return 0; }
    }
    private static String keyLabel(Object key) {
        if (key == null) return "null";
        if (key instanceof Key) return ((Key) key).id;
        if (key instanceof Plain) return "PLAIN";
        if (key instanceof WrongComparable) return "WRONG_COMPARABLE";
        throw new AssertionError("Unexpected key class");
    }
    private static final class Counting implements Comparator<Object> {
        final Context context;
        Counting(Context context) { this.context = context; }
        @Override public int compare(Object left, Object right) {
            context.trace.add(context.mode + ":" + keyLabel(left) + ":" + keyLabel(right) + ":self=" + (left == right));
            if (context.armed && context.mode.equals("THROW_SELF") && left == right) throw context.marker;
            if (context.armed && context.mode.equals("THROW_VISITED") && right == context.visited) throw context.marker;
            if (context.mode.equals("NULL_FRIENDLY")) {
                if (left == null) return right == null ? 0 : -1;
                if (right == null) return 1;
            }
            int compared = Integer.compare(((Key) left).number, ((Key) right).number);
            return context.mode.equals("REVERSE") ? -compared : compared;
        }
    }
    private static final class Context {
        final String mode;
        final Marker marker;
        final List<String> trace = new ArrayList<>();
        final Map<Integer, Key> seeds = new LinkedHashMap<>();
        final IdentityHashMap<Object, String> valueIds = new IdentityHashMap<>();
        final SortedMap<Object, Object> root;
        boolean armed;
        Object visited;
        @SuppressWarnings("unchecked")
        Context(String id, String mode) throws Exception {
            this.mode = mode;
            marker = new Marker(id);
            valueIds.put(null, "null");
            Constructor<?> constructor = mode.equals("NATURAL") ? provider.getConstructor() : provider.getConstructor(Comparator.class);
            try { root = (SortedMap<Object, Object>) (mode.equals("NATURAL") ? constructor.newInstance() : constructor.newInstance(new Counting(this))); }
            catch (InvocationTargetException failure) { throw new AssertionError("provider construction", failure.getCause()); }
        }
        Key key(String id, int number) { return new Key(this, id, number); }
        Object value(String id) { Object value = new String(id); valueIds.put(value, id); return value; }
        void seed(int number, boolean nil) { Key key = key("S" + number, number); seeds.put(number, key); root.put(key, nil ? null : value("seed" + number)); }
        void seedThree() { seed(5, false); seed(1, false); seed(9, false); }
        String valueLabel(Object value) {
            String id = valueIds.get(value);
            if (id == null) throw new AssertionError("Provider returned an unknown value reference");
            return id;
        }
        Map<String, Object> state() {
            Map<String, Object> state = new LinkedHashMap<>();
            state.put("size", root.size());
            List<String> rows = new ArrayList<>();
            for (Map.Entry<Object, Object> entry : root.entrySet()) rows.add(keyLabel(entry.getKey()) + "=" + valueLabel(entry.getValue()));
            state.put("entries", rows);
            return state;
        }
    }
    private static void execute(String id, Context context, Object query, Object value, String view) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("query", keyLabel(query));
        row.put("incoming", context.valueLabel(value));
        row.put("seedCmp", new ArrayList<>(context.trace));
        context.trace.clear();
        row.put("before", context.state());
        SortedMap<Object, Object> receiver = context.root;
        if (view.equals("HEAD")) receiver = receiver.headMap(context.key("B5", 5));
        if (view.equals("TAIL")) receiver = receiver.tailMap(context.key("B5", 5));
        row.put("viewCmp", new ArrayList<>(context.trace));
        context.trace.clear();
        context.armed = true;
        try {
            Object returned = receiver.put(query, value);
            row.put("outcome", "return");
            row.put("returned", context.valueLabel(returned));
            row.put("returnedOwnedReference", context.valueIds.containsKey(returned));
        } catch (Throwable failure) {
            row.put("outcome", "throw");
            row.put("exception", failure.getClass().getName());
            row.put("message", failure.getMessage());
            row.put("markerSame", failure == context.marker);
        }
        row.put("putCmp", new ArrayList<>(context.trace));
        context.trace.clear();
        row.put("after", context.state());
        row.put("stateCmp", new ArrayList<>(context.trace));
        System.out.println(json(row));
        cases++;
    }
    private static void ordinary(String mode, String profile) throws Exception {
        String family = mode.equals("NATURAL") ? "NATURAL_ROOT" : "REVERSE_ROOT";
        String id = family + "/" + profile;
        Context context = new Context(id, mode);
        if (!profile.equals("EMPTY_FIRST")) context.seed(5, profile.equals("NULL_VALUE_NOOP"));
        Object query = profile.equals("SAME_ROOT") || profile.equals("NULL_VALUE_NOOP") ? context.seeds.get(5)
                : context.key(profile.equals("NEW_LEFT") ? "Q2" : profile.equals("NEW_RIGHT") ? "Q8" : "Q5", profile.equals("NEW_LEFT") ? 2 : profile.equals("NEW_RIGHT") ? 8 : 5);
        execute(id, context, query, profile.equals("NULL_VALUE_NOOP") ? null : context.value("incoming"), "ROOT");
    }
    private static void invalid(String profile) throws Exception {
        String id = "NATURAL_INVALID/" + profile;
        Context context = new Context(id, "NATURAL");
        if (profile.startsWith("NONEMPTY")) context.seed(5, false);
        Object query = profile.endsWith("_NULL") ? null : profile.endsWith("_PLAIN") ? new Plain() : new WrongComparable(context);
        execute(id, context, query, context.value("incoming"), "ROOT");
    }
    private static void nullFriendly(String profile) throws Exception {
        String id = "NULL_FRIENDLY/" + profile;
        Context context = new Context(id, "NULL_FRIENDLY");
        if (profile.equals("NONEMPTY_NULL")) context.seed(5, false);
        if (profile.equals("UPDATE_NULL") || profile.equals("NULL_TO_NEW")) context.root.put(null, context.value("seedNull"));
        execute(id, context, profile.equals("NULL_TO_NEW") ? context.key("Q2", 2) : null, context.value("incoming"), "ROOT");
    }
    private static void throwing(String profile) throws Exception {
        String id = "THROWING_COMPARATOR/" + profile;
        Context context = new Context(id, profile.startsWith("SELF") ? "THROW_SELF" : "THROW_VISITED");
        if (profile.startsWith("VISITED_SECOND")) context.seedThree();
        else if (!profile.equals("SELF_EMPTY")) context.seed(5, false);
        context.visited = context.seeds.get(profile.equals("VISITED_SECOND_LEFT") ? 1 : profile.equals("VISITED_SECOND_RIGHT") ? 9 : 5);
        Object query = profile.equals("SELF_SAME_ROOT") ? context.seeds.get(5)
                : context.key(profile.endsWith("_EQUAL") || profile.equals("SELF_EMPTY") ? "Q5" : profile.equals("VISITED_SECOND_RIGHT") ? "Q8" : "Q2", profile.endsWith("_EQUAL") || profile.equals("SELF_EMPTY") ? 5 : profile.equals("VISITED_SECOND_RIGHT") ? 8 : 2);
        execute(id, context, query, context.value("incoming"), "ROOT");
    }
    private static void bounded(String profile) throws Exception {
        String id = "BOUNDED_CONTROL/" + profile;
        boolean reverse = profile.startsWith("REVERSE");
        Context context = new Context(id, reverse ? "REVERSE" : "NATURAL");
        context.seedThree();
        boolean head = profile.contains("_HEAD_");
        int key = profile.endsWith("_BOUND") ? 5 : head == reverse ? 8 : 2;
        execute(id, context, context.key("Q" + key, key), context.value("incoming"), head ? "HEAD" : "TAIL");
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Public provider class required");
        provider = Class.forName(args[0]);
        if (provider == java.util.TreeMap.class) {
            if (provider.getClassLoader() != null || !provider.getModule().getName().equals("java.base")) throw new AssertionError("Original provider identity");
        } else origin(provider, "providerOrigin");
        origin(JdkTreePutValidationOracle.class, "fixtureOrigin");
        if (!Path.of(System.getProperty("java.home")).toRealPath().equals(Path.of(System.getProperty("expectedJavaHome")).toRealPath())) throw new AssertionError("Unexpected JDK home");
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("kind", "header"); header.put("provider", provider.getName()); header.put("module", provider.getModule().getName());
        header.put("bootstrap", provider.getClassLoader() == null); header.put("runtime", Runtime.version().toString()); header.put("casesPlanned", 36);
        System.out.println(json(header));
        String[] ordinary = {"EMPTY_FIRST", "NEW_LEFT", "NEW_RIGHT", "DISTINCT_EQUAL_ROOT", "SAME_ROOT", "NULL_VALUE_NOOP"};
        for (String p : ordinary) ordinary("NATURAL", p);
        for (String p : new String[]{"EMPTY_NULL", "NONEMPTY_NULL", "EMPTY_PLAIN", "NONEMPTY_PLAIN", "EMPTY_WRONG_COMPARABLE", "NONEMPTY_WRONG_COMPARABLE"}) invalid(p);
        for (String p : ordinary) ordinary("REVERSE", p);
        for (String p : new String[]{"EMPTY_NULL", "NONEMPTY_NULL", "UPDATE_NULL", "NULL_TO_NEW"}) nullFriendly(p);
        for (String p : new String[]{"SELF_EMPTY", "SELF_NEW", "SELF_DISTINCT_EQUAL", "SELF_SAME_ROOT", "VISITED_ROOT_NEW", "VISITED_ROOT_EQUAL", "VISITED_SECOND_LEFT", "VISITED_SECOND_RIGHT"}) throwing(p);
        for (String p : new String[]{"NATURAL_HEAD_ALLOWED", "NATURAL_TAIL_ALLOWED", "NATURAL_HEAD_BOUND", "REVERSE_HEAD_ALLOWED", "REVERSE_TAIL_ALLOWED", "REVERSE_HEAD_BOUND"}) bounded(p);
        if (cases != 36) throw new AssertionError("Incomplete frozen matrix " + cases);
        System.out.println("{\"kind\":\"footer\",\"casesExecuted\":" + cases + "}");
    }
}
