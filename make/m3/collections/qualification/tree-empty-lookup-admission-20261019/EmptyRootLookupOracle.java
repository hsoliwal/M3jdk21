// SPDX-License-Identifier: Apache-2.0
// Owned source-aware discriminator; the JDK original is frozen before product edits.
import java.lang.management.ManagementFactory;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;

public final class EmptyRootLookupOracle {
    private EmptyRootLookupOracle() { }
    private static final List<String> EVENTS = new ArrayList<>();
    private static final class Marker extends RuntimeException {
        private static final long serialVersionUID = 1L;
        Marker() { super("empty-root-lookup-marker"); }
    }
    private static final Marker MARKER = new Marker();
    private static final class Probe implements Comparable<Object> {
        private final int id;
        private final String mode;
        Probe(int id, String mode) { this.id = id; this.mode = mode; }
        @Override public int compareTo(Object other) {
            EVENTS.add(this + ":" + label(other));
            if (this.mode.equals("ALWAYS_THROW") || this.mode.equals("SELF_THROW") && other == this) {
                throw MARKER;
            }
            return Integer.compare(this.id, ((Probe) other).id);
        }
        @Override public String toString() { return this.mode + this.id; }
    }
    private static String label(Object value) {
        return value == null ? "null" : value instanceof Probe ? value.toString() : value.getClass().getName();
    }
    private static Map<String, Object> row(Object... pairs) {
        return CanonicalPutAllCalibration3.row(pairs);
    }
    private static void emit(Object value) {
        System.out.println(CanonicalPutAllCalibration3.json(value));
    }
    @SuppressWarnings("unchecked")
    private static NavigableMap<Object, String> receiver(Class<?> type, String order) throws Exception {
        Comparator<Object> comparator = (left, right) -> {
            EVENTS.add("comparator:" + label(left) + ":" + label(right));
            if (left == right) { return 0; }
            if (left == null) { return -1; }
            if (right == null) { return 1; }
            return ((Probe) left).compareTo(right);
        };
        return (NavigableMap<Object, String>) (order.equals("NATURAL")
                ? type.getConstructor().newInstance()
                : type.getConstructor(Comparator.class).newInstance(comparator));
    }
    private static Object invoke(Map<Object, String> map, String operation, Object query) {
        return switch (operation) {
            case "get" -> map.get(query);
            case "getOrDefault" -> map.getOrDefault(query, "fallback");
            case "containsKey" -> map.containsKey(query);
            case "remove" -> map.remove(query);
            case "keys.contains" -> map.keySet().contains(query);
            case "keys.remove" -> map.keySet().remove(query);
            case "entries.contains" -> map.entrySet().contains(new java.util.AbstractMap.SimpleImmutableEntry<>(query, "seed"));
            case "entries.remove" -> map.entrySet().remove(new java.util.AbstractMap.SimpleImmutableEntry<>(query, "seed"));
            default -> throw new IllegalArgumentException(operation);
        };
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 1) { throw new IllegalArgumentException("provider class required"); }
        if (!Objects.equals(System.getProperty("java.home"), System.getProperty("expectedJavaHome"))) {
            throw new IllegalStateException("unexpected Java home");
        }
        Class<?> type = Class.forName(args[0]);
        String origin = type.getProtectionDomain().getCodeSource() == null ? "jrt:/java.base"
                : type.getProtectionDomain().getCodeSource().getLocation().toString();
        if (!Objects.equals(origin, System.getProperty("expectedProviderOrigin"))) {
            throw new IllegalStateException("unexpected provider origin " + origin);
        }
        byte[] classBytes;
        try (var stream = type.getResourceAsStream("/" + type.getName().replace('.', '/') + ".class")) {
            if (stream == null) { throw new IllegalStateException("provider class absent"); }
            classBytes = stream.readAllBytes();
        }
        emit(row("kind", "identity", "provider", type.getName(), "origin", origin,
                "classSha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(classBytes)),
                "javaHome", System.getProperty("java.home"), "javaVersion", System.getProperty("java.version"),
                "temp", System.getProperty("java.io.tmpdir"), "arguments", ManagementFactory.getRuntimeMXBean().getInputArguments()));
        int count = 0;
        for (String order : List.of("NATURAL", "COMPARATOR")) {
            for (String state : List.of("FRESH", "CLEARED", "REMOVED", "NONEMPTY")) {
                for (String view : List.of("ROOT", "DESCENDING")) {
                    for (String key : List.of("COUNTING", "SELF_THROW", "ALWAYS_THROW", "PLAIN", "NULL")) {
                        for (String operation : List.of("get", "getOrDefault", "containsKey", "remove",
                                "keys.contains", "keys.remove", "entries.contains", "entries.remove")) {
                            NavigableMap<Object, String> root = receiver(type, order);
                            Probe seed = new Probe(7, "SEED");
                            if (!state.equals("FRESH")) { root.put(seed, "seed"); }
                            if (state.equals("CLEARED")) { root.clear(); }
                            if (state.equals("REMOVED")) { root.remove(seed); }
                            Map<Object, String> selected = view.equals("ROOT") ? root : root.descendingMap();
                            Object query = key.equals("NULL") ? null : key.equals("PLAIN") ? new Object() : new Probe(17, key);
                            EVENTS.clear();
                            Map<String, Object> outcome;
                            try { outcome = row("kind", "return", "value", invoke(selected, operation, query)); }
                            catch (RuntimeException failure) {
                                outcome = row("kind", "throw", "class", failure.getClass().getName(),
                                        "message", failure.getMessage(), "markerIdentity", failure == MARKER);
                            }
                            List<String> observed = new ArrayList<>(EVENTS);
                            List<String> entries = new ArrayList<>();
                            for (var entry : root.entrySet()) { entries.add(label(entry.getKey()) + "=" + entry.getValue()); }
                            emit(row("kind", "case", "id", order + "/" + state + "/" + view + "/" + key + "/" + operation,
                                    "outcome", outcome, "events", observed, "size", root.size(), "empty", root.isEmpty(), "entries", entries));
                            count++;
                        }
                    }
                }
            }
        }
        emit(row("kind", "complete", "caseCount", count));
    }
}
