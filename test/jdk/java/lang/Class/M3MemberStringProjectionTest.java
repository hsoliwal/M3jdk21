// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary Member text uses existing metadata without a temporary declaring-class view
 * @modules java.base/jdk.internal.mindex
 * @run main/othervm M3MemberStringProjectionTest
 * @run main/othervm -Xint M3MemberStringProjectionTest
 */
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import jdk.internal.mindex.M3CI;
import jdk.internal.mindex.M3Class;
import jdk.internal.mindex.M3Field;
import jdk.internal.mindex.M3Member;
import jdk.internal.mindex.M3MemberKind;
import jdk.internal.mindex.M3Method;
import jdk.internal.mindex.M3RecordComponent;
import jdk.internal.mindex.M3VI;

public final class M3MemberStringProjectionTest {
    public static class Generic<T> { public T item; public T read() { return item; } }
    public static final class Sample extends Generic<String> {
        public int number;
        private long hidden;
        public Sample() { hidden = 7; }
        public Sample(int number) { this(); this.number = number; }
        @Override public String read() { return item; }
        public long mix(String text, int... values) { return hidden + text.length() + values.length; }
    }
    public record Pair(int id, String label) { }
    public enum Tone { LOW, HIGH }
    private static final List<Class<?>> ROOTS = List.of(Sample.class, Generic.class,
            Pair.class, Tone.class, int.class, String[].class);
    private record Saved(Supplier<String> supplier, String value) { }
    public record Observation(long checks, String digest) { }

    private static String key(M3MemberKind kind, String name, String descriptor) {
        return kind + "|" + name + "|" + descriptor;
    }

    private static Map<String, String> expected(Class<?> type) {
        Map<String, String> values = new HashMap<>();
        for (Field field : type.getDeclaredFields()) {
            String descriptor = field.getType().descriptorString();
            values.put(key(M3MemberKind.FIELD, field.getName(), descriptor),
                    type.getName() + "#" + field.getName() + descriptor);
        }
        for (Method method : type.getDeclaredMethods()) {
            String descriptor = MethodType.methodType(method.getReturnType(), method.getParameterTypes()).descriptorString();
            values.put(key(M3MemberKind.METHOD, method.getName(), descriptor),
                    type.getName() + "#" + method.getName() + descriptor);
        }
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            String descriptor = MethodType.methodType(void.class, constructor.getParameterTypes()).descriptorString();
            values.put(key(M3MemberKind.CONSTRUCTOR, constructor.getName(), descriptor),
                    type.getName() + "#" + constructor.getName() + descriptor);
        }
        RecordComponent[] components = type.getRecordComponents();
        if (components != null) for (RecordComponent component : components) {
            String descriptor = component.getType().descriptorString();
            values.put(key(M3MemberKind.RECORD_COMPONENT, component.getName(), descriptor),
                    type.getName() + "#" + component.getName() + descriptor);
        }
        return values;
    }

    private static Observation verify(String category) throws Exception {
        M3CI standard = M3CI.compile(ROOTS, M3CI.Options.standard(), M3VI.Progress.none());
        M3CI compact = M3CI.compile(ROOTS, M3CI.Options.compact(), M3VI.Progress.none());
        List<String> observations = new ArrayList<>();
        List<Saved> retained = new ArrayList<>();
        long checks = 0;
        for (M3CI ci : List.of(standard, compact)) {
            for (int row = 0; row < ci.size(); row++) {
                M3Class view = ci.classAt(row);
                int standardRow = standard.classRow(ci.loaderSpaceId(row), ci.binaryName(row));
                if (standardRow < 0) throw new AssertionError("missing standard peer");
                Class<?> type = standard.boundary().classAt(standardRow).orElseThrow();
                Map<String, String> expected = expected(type);
                if (category.equals("member")) for (int ordinal = 0; ordinal < view.memberCount(); ordinal++) {
                    M3Member member = view.memberAt(ordinal);
                    String wanted = expected.get(key(member.kind(), member.name(), member.descriptor()));
                    checks += check(member::toString, wanted, retained, observations);
                }
                if (category.equals("field")) for (M3Field field : view.getDeclaredFields()) {
                    String descriptor = field.descriptor();
                    String general = expected.get(key(M3MemberKind.FIELD, field.getName(), descriptor));
                    if (general == null) throw new AssertionError("missing reflection field");
                    checks += check(field::toString, general.substring(0, general.length() - descriptor.length()), retained, observations);
                }
                if (category.equals("method")) for (M3Method method : view.getDeclaredMethods()) {
                    checks += check(method::toString, expected.get(key(M3MemberKind.METHOD,
                            method.getName(), method.descriptor())), retained, observations);
                }
                if (category.equals("record") && view.isRecord()) for (M3RecordComponent component : view.getRecordComponents()) {
                    String descriptor = component.descriptor();
                    String general = expected.get(key(M3MemberKind.RECORD_COMPONENT, component.getName(), descriptor));
                    if (general == null) throw new AssertionError("missing reflection component");
                    checks += check(component::toString, general.substring(0, general.length() - descriptor.length()), retained, observations);
                }
            }
        }
        for (Saved saved : retained) {
            if (!saved.value.equals(saved.supplier.get())) throw new AssertionError("retained member changed");
            checks++;
        }
        if (checks == 0) throw new AssertionError("empty behavioral atom " + category);
        observations.sort(String::compareTo);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (String value : observations) {
            digest.update(value.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
        }
        return new Observation(checks, HexFormat.of().formatHex(digest.digest()));
    }

    private static long check(Supplier<String> actual, String expected, List<Saved> retained,
                              List<String> observations) {
        if (expected == null) throw new AssertionError("missing reflection oracle");
        String value = actual.get();
        if (!expected.equals(value)) throw new AssertionError(expected + " != " + value);
        if (!value.equals(actual.get())) throw new AssertionError("repeat changed");
        retained.add(new Saved(actual, value));
        observations.add(value);
        return 2;
    }

    public static Observation member() throws Exception { return verify("member"); }
    public static Observation field() throws Exception { return verify("field"); }
    public static Observation method() throws Exception { return verify("method"); }
    public static Observation record() throws Exception { return verify("record"); }

    public static void main(String[] args) throws Exception {
        long total = 0;
        for (String category : List.of("member", "field", "method", "record")) {
            Observation result = verify(category);
            System.out.println(category + " checks=" + result.checks + " observations=" + result.digest);
            total += result.checks;
        }
        System.out.println("TOTAL_CHECKS=" + total);
    }
}
