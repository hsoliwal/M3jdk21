// SPDX-License-Identifier: Apache-2.0
// Supplemental source-blind getter-effects fixture, frozen before the owner fix.
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SpringEntryGetterProtocol {
    private SpringEntryGetterProtocol() { }

    private static final class RecordingEntry implements Map.Entry<String, Integer> {
        private final String key;
        private final Integer value;
        private final boolean throwValue;
        private final List<String> calls = new ArrayList<>();
        RecordingEntry(String key, Integer value, boolean throwValue) {
            this.key = key;
            this.value = value;
            this.throwValue = throwValue;
        }
        @Override public String getKey() { calls.add("getKey"); return key; }
        @Override public Integer getValue() {
            calls.add("getValue");
            if (throwValue) throw new IllegalArgumentException("entry-value-fixture");
            return value;
        }
        @Override public Integer setValue(Integer next) { throw new UnsupportedOperationException(); }
        @Override public boolean equals(Object other) { throw new AssertionError("query entry equals was invoked"); }
        @Override public int hashCode() { throw new AssertionError("query entry hashCode was invoked"); }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Integer> create(Class<?> owner) throws ReflectiveOperationException {
        return (Map<String, Integer>) owner.getConstructor(Locale.class).newInstance(Locale.ROOT);
    }

    private static void test(Class<?> owner, String label, boolean remove, String key,
            Integer value, boolean throwValue) throws ReflectiveOperationException {
        Map<String, Integer> map = create(owner);
        map.put("A", 1);
        RecordingEntry entry = new RecordingEntry(key, value, throwValue);
        String result;
        try {
            result = Boolean.toString(remove ? map.entrySet().remove(entry) : map.entrySet().contains(entry));
        } catch (RuntimeException failure) {
            result = failure.getClass().getName() + ":" + failure.getMessage();
        }
        System.out.println(label + "\t" + result + "\t" + entry.calls + "\t" + map);
    }

    public static void main(String[] args) throws ReflectiveOperationException {
        if (args.length != 1) throw new IllegalArgumentException("one owner FQN required");
        Class<?> owner = Class.forName(args[0]);
        test(owner, "contains-found", false, "A", 1, false);
        test(owner, "contains-missing", false, "Z", 1, false);
        test(owner, "contains-wrong-value", false, "A", 2, false);
        test(owner, "contains-missing-throw-value", false, "Z", 1, true);
        test(owner, "remove-found", true, "A", 1, false);
        test(owner, "remove-missing", true, "Z", 1, false);
        test(owner, "remove-wrong-value", true, "A", 2, false);
        test(owner, "remove-missing-throw-value", true, "Z", 1, true);
        test(owner, "remove-found-throw-value", true, "A", 1, true);
    }
}
