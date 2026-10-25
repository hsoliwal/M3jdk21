/* Copyright 2026 Synexia. SPDX-License-Identifier: Apache-2.0
 * Prospective provider-only public Entry/wire observer. No owner source/body.
 * Required and NO_FIT channels: BASELINE_PARTITIONS_V2.json.
 */
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class SpringCaseInsensitiveMapEntryCompatibilityObserver {
    private SpringCaseInsensitiveMapEntryCompatibilityObserver() { }
    @SuppressWarnings("unchecked")
    private static Map<String, Object> create(Class<?> owner) throws ReflectiveOperationException {
        Object map = owner.getConstructor(Locale.class).newInstance(Locale.ROOT);
        if (!(map instanceof Map<?, ?>)) throw new IllegalArgumentException("not Map");
        Map<String, Object> result = (Map<String, Object>) map;
        result.put("A", 1); result.put("B", null);
        return result;
    }
    @SuppressWarnings("unchecked")
    private static Map.Entry<String, Object> entry(Object value) {
        if (!(value instanceof Map.Entry<?, ?>)) throw new IllegalArgumentException("not Entry");
        return (Map.Entry<String, Object>) value;
    }
    private static void demand(boolean condition, String id) {
        if (!condition) throw new AssertionError(id);
    }
    private static String hash(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
            .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("exact owner classname required");
        Class<?> owner = Class.forName(args[0]);
        Map<String, Object> map = create(owner);
        var cursor = map.entrySet().iterator();
        Map.Entry<String, Object> current = cursor.next();
        demand(current.getKey().equals("A") && Objects.equals(current.getValue(), 1), "valid-iterator-tuple");
        demand(Objects.equals(current.setValue(2), 1) && Objects.equals(map.get("a"), 2), "valid-iterator-write-through");
        demand(current.equals(new AbstractMap.SimpleEntry<>("A", 2)) && current.hashCode() == ("A".hashCode() ^ 2), "valid-iterator-equals-hash");
        String required = "tuple=A,2;returnOld=1;mapValue=2;entryString=" + current;
        System.out.println("ENTRY_REQUIRED\titerator-defined\t" + required + "\t" + hash(required));
        // Other-than-iteration connection is unspecified by generic Map.Entry.
        // Here it is recorded as the selected Spring mapped policy, before its baseline.
        for (String path : new String[]{"array", "typed-array", "generator-array", "forEach", "spliterator", "stream", "parallelStream"}) {
            Map<String, Object> target = create(owner);
            List<Map.Entry<String, Object>> callback = new ArrayList<>();
            Map.Entry<String, Object> selected = switch (path) {
                case "array" -> entry(target.entrySet().toArray()[0]);
                case "typed-array" -> entry(target.entrySet().toArray(new Object[0])[0]);
                case "generator-array" -> entry(target.entrySet().toArray(Object[]::new)[0]);
                case "forEach" -> { target.entrySet().forEach(callback::add); yield callback.getFirst(); }
                case "spliterator" -> { target.entrySet().spliterator().tryAdvance(callback::add); yield callback.getFirst(); }
                case "stream" -> target.entrySet().stream().findFirst().orElseThrow();
                default -> target.entrySet().parallelStream().findFirst().orElseThrow();
            };
            String record;
            try {
                Object old = selected.setValue(3);
                record = "old=" + old + ";entry=" + selected + ";mapA=" + target.get("A") + ";mapSize=" + target.size();
            } catch (RuntimeException failure) {
                record = "exception=" + failure.getClass().getSimpleName() + ";mapA=" + target.get("A");
            }
            System.out.println("ENTRY_REQUIRED\tselected-spring-policy/" + path + "\t" + record + "\t" + hash(record));
        }
        Map<String, Object> identityMap = create(owner);
        Map.Entry<String, Object> first = identityMap.entrySet().iterator().next();
        List<Map.Entry<String, Object>> all = new ArrayList<>();
        all.add(identityMap.entrySet().iterator().next());
        all.add(entry(identityMap.entrySet().toArray()[0]));
        identityMap.entrySet().forEach(all::add);
        identityMap.entrySet().spliterator().tryAdvance(all::add);
        all.add(identityMap.entrySet().stream().findFirst().orElseThrow());
        all.add(identityMap.entrySet().parallelStream().findFirst().orElseThrow());
        List<Boolean> sameReference = new ArrayList<>();
        for (Map.Entry<String, Object> observed : all) if (observed.getKey().equals("A")) sameReference.add(first == observed);
        String identity = sameReference.toString();
        System.out.println("ENTRY_IDENTITY_NO_FIT\trepeated-api-entry-reference\t" + identity + "\t" + hash(identity));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) { output.writeObject(identityMap); }
        byte[] encoded = bytes.toByteArray();
        String streamSHA = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(encoded));
        long uid = ObjectStreamClass.lookup(owner).getSerialVersionUID();
        System.out.println("PRIVATE_WIRE_NO_FIT\tnominal-envelope\tclass=" + owner.getName()
            + ";uid=" + uid + ";bytes=" + encoded.length + ";rawStreamSHA256=" + streamSHA);
    }
}
