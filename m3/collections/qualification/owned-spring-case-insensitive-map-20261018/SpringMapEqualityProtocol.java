// SPDX-License-Identifier: Apache-2.0
// Legal reflexive caller-value equals effects, frozen before the Table.equals fix.
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SpringMapEqualityProtocol {
    private SpringMapEqualityProtocol() { }
    private static final List<String> CALLS = new ArrayList<>();
    private static final class Value {
        private final String name;
        Value(String name) { this.name = name; }
        @Override public boolean equals(Object other) {
            CALLS.add(name + ":" + (this == other ? "self" : String.valueOf(other)));
            return other instanceof Value value && name.equals(value.name);
        }
        @Override public int hashCode() { return name.hashCode(); }
        @Override public String toString() { return name; }
    }
    @SuppressWarnings("unchecked")
    private static Map<String, Object> create(Class<?> owner) throws ReflectiveOperationException {
        return (Map<String, Object>) owner.getConstructor(Locale.class).newInstance(Locale.ROOT);
    }
    private static void test(Class<?> owner, String label, int variant) throws ReflectiveOperationException {
        Map<String, Object> map = create(owner);
        Map<String, Object> foreign = new LinkedHashMap<>();
        Value value = new Value("V");
        map.put("A", variant == 3 || variant == 4 ? null : value);
        if (variant != 4) foreign.put("A", switch (variant) {
            case 0 -> value;
            case 1 -> new Value("V");
            case 2 -> new Value("W");
            case 3 -> null;
            default -> value;
        });
        if (variant == 5) { map.put("B", value); foreign.put("B", value); }
        CALLS.clear();
        boolean result = map.equals(foreign);
        System.out.println(label + "\t" + result + "\t" + CALLS + "\t" + map);
    }
    public static void main(String[] args) throws ReflectiveOperationException {
        if (args.length != 1) throw new IllegalArgumentException("one owner FQN required");
        Class<?> owner = Class.forName(args[0]);
        test(owner, "same-value-reference", 0);
        test(owner, "equal-distinct-value", 1);
        test(owner, "unequal-value", 2);
        test(owner, "present-null", 3);
        test(owner, "different-size", 4);
        test(owner, "two-shared-values", 5);
    }
}
