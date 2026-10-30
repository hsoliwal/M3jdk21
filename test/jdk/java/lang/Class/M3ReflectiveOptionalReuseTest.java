// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0

/*
 * @test
 * @summary Typed M3 reflective boundaries preserve payloads, absence and metadata
 * @modules java.base/jdk.internal.mindex
 * @run main/othervm M3ReflectiveOptionalReuseTest
 * @run main/othervm -Xint M3ReflectiveOptionalReuseTest
 */

import java.lang.ref.Reference;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import jdk.internal.mindex.M3CI;
import jdk.internal.mindex.M3Class;
import jdk.internal.mindex.M3Constructor;
import jdk.internal.mindex.M3Descriptor;
import jdk.internal.mindex.M3Field;
import jdk.internal.mindex.M3MemberKind;
import jdk.internal.mindex.M3Method;
import jdk.internal.mindex.M3RecordComponent;
import jdk.internal.mindex.M3VI;

public final class M3ReflectiveOptionalReuseTest {
    public static final class Sample {
        public int number;
        private long hidden = 17L;

        public Sample() { this(3); }
        public Sample(int number) { this.number = number; }
        public int scale(int factor) { return number * factor; }
        public String scale(String suffix) { return number + suffix; }
    }

    public static class Value<T> {
        public T value() { return null; }
    }

    public static final class StringValue extends Value<String> {
        @Override public String value() { return "retained"; }
    }

    public record Pair(int id, String label) { }
    public enum Tone { LOW, HIGH }

    private static final List<Class<?>> ROOTS = List.of(
            Sample.class, Value.class, StringValue.class, Pair.class, Tone.class,
            int.class, String[].class);

    private record Access(
            M3CI owner, int row, M3MemberKind kind, Class<?> declaringClass,
            Class<?> runtimeType, Supplier<? extends Optional<?>> optional,
            Supplier<? extends AnnotatedElement> required, String absentMessage) {
        String key() {
            return memberKey(kind, declaringClass, owner.memberName(row),
                    owner.memberDescriptor(row));
        }
    }

    private record Saved(Access access, Optional<?> optional, AnnotatedElement payload) { }

    public record Observation(long checks, String sha256) { }

    private static final class Oracle {
        private final MessageDigest digest;
        private final List<String> observations = new ArrayList<>();
        private long checks;

        Oracle() {
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException impossible) {
                throw new AssertionError(impossible);
            }
        }

        void check(boolean condition, String message) {
            checks++;
            if (!condition) throw new AssertionError(message);
        }

        void observe(String value) {
            observations.add(value);
        }

        Observation finish() {
            // JVM reflection enumeration and owner-local coordinates need not be
            // identical across processes. Compare the sorted semantic observations.
            observations.sort(String::compareTo);
            for (String value : observations) {
                digest.update(value.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) '\n');
            }
            return new Observation(checks, HexFormat.of().formatHex(digest.digest()));
        }
    }

    public static void main(String[] args) throws Exception {
        print("liveKinds", liveKinds());
        print("compactBoundaries", compactBoundaries());
        print("retainedPayloads", retainedPayloads());
        print("standardCompactParity", standardCompactParity());
    }

    private static void print(String atom, Observation result) {
        System.out.println(atom + " checks=" + result.checks()
                + " observations=" + result.sha256());
    }

    /** Compare every fixture member with the JVM, including bridge methods. */
    public static Observation liveKinds() {
        Oracle oracle = new Oracle();
        M3CI owner = compile(M3CI.Options.standard());
        List<Access> accesses = accesses(owner);
        oracle.check(owner.boundary().retained(), "standard boundary retained");
        oracle.check(owner.boundary().retainedReferenceCount() > 0,
                "standard boundary has weak references");
        oracle.check(keys(accesses).equals(reflectiveKeys()), "complete JVM member oracle");
        int[] counts = new int[M3MemberKind.values().length];
        int bridges = 0;
        for (Access access : accesses) {
            AnnotatedElement payload = checkLive(access, oracle);
            counts[access.kind().ordinal()]++;
            if (payload instanceof Method method && method.isBridge()) bridges++;
        }
        checkAllKinds(counts, oracle);
        oracle.check(bridges > 0, "fixture bridge method observed");
        Reference.reachabilityFence(ROOTS);
        return oracle.finish();
    }

    /** Absent compact boundaries retain the exact existing to* exception contract. */
    public static Observation compactBoundaries() {
        Oracle oracle = new Oracle();
        M3CI owner = compile(M3CI.Options.compact());
        List<Access> accesses = accesses(owner);
        oracle.check(!owner.boundary().retained(), "compact boundary disabled");
        oracle.check(owner.boundary().retainedReferenceCount() == 0,
                "compact boundary retains no weak references");
        oracle.check(keys(accesses).equals(reflectiveKeys()), "compact metadata complete");
        int[] counts = new int[M3MemberKind.values().length];
        for (Access access : accesses) {
            oracle.check(owner.resolveReflectiveElement(access.row()).isEmpty(),
                    "generic compact boundary absent: " + access.key());
            Optional<?> result = access.optional().get();
            oracle.check(result != null && result.isEmpty(),
                    "typed compact boundary absent: " + access.key());
            try {
                access.required().get();
                throw new AssertionError("to* accepted absent boundary: " + access.key());
            } catch (IllegalStateException expected) {
                oracle.check(expected.getClass() == IllegalStateException.class,
                        "exact absent exception class: " + access.key());
                oracle.check(access.absentMessage().equals(expected.getMessage()),
                        "exact absent exception message: " + access.key());
                oracle.observe(access.key() + "|" + expected.getClass().getName()
                        + "|boundary-absent");
            }
            counts[access.kind().ordinal()]++;
        }
        checkAllKinds(counts, oracle);
        Reference.reachabilityFence(ROOTS);
        return oracle.finish();
    }

    /** Keep published results live across later calls and exercise their real payloads. */
    public static Observation retainedPayloads() throws ReflectiveOperationException {
        Oracle oracle = new Oracle();
        M3CI owner = compile(M3CI.Options.standard());
        List<Access> accesses = accesses(owner);
        List<Saved> saved = new ArrayList<>();
        for (Access access : accesses) {
            Optional<?> result = access.optional().get();
            oracle.check(result.isPresent(), "retained result present: " + access.key());
            Object payload = result.orElseThrow();
            oracle.check(access.runtimeType().isInstance(payload),
                    "retained runtime type: " + access.key());
            saved.add(new Saved(access, result, (AnnotatedElement) payload));
        }

        // A second owner and repeated lookups must not repurpose old wrappers or payloads.
        M3CI otherOwner = compile(M3CI.Options.standard());
        for (int repetition = 0; repetition < 4; repetition++) {
            for (Access access : accesses) checkLive(access, oracle);
            for (Access access : accesses(otherOwner)) checkLive(access, oracle);
            for (Saved result : saved) {
                oracle.check(result.optional().orElseThrow() == result.payload(),
                        "published payload identity stable: " + result.access().key());
                oracle.check(describe(result.payload()).equals(result.access().key()),
                        "published payload metadata stable: " + result.access().key());
                oracle.observe(result.access().key() + "|retained|" + repetition);
            }
        }

        Constructor<?> constructor = (Constructor<?>) savedPayload(saved,
                M3MemberKind.CONSTRUCTOR, Sample.class, Sample.class.getName(), "(I)V");
        Sample instance = (Sample) constructor.newInstance(7);
        Field field = (Field) savedPayload(saved, M3MemberKind.FIELD, Sample.class,
                "number", "I");
        field.setInt(instance, 11);
        oracle.check(field.getInt(instance) == 11, "retained Field reads and writes");
        Method method = (Method) savedPayload(saved, M3MemberKind.METHOD, Sample.class,
                "scale", "(I)I");
        oracle.check(Integer.valueOf(33).equals(method.invoke(instance, 3)),
                "retained Method invokes exact overload");
        RecordComponent component = (RecordComponent) savedPayload(saved,
                M3MemberKind.RECORD_COMPONENT, Pair.class, "label", "Ljava/lang/String;");
        oracle.check("pair".equals(component.getAccessor().invoke(new Pair(5, "pair"))),
                "retained RecordComponent accessor remains usable");

        // The wrapper is immutable; a reflective payload may have mutable access state.
        Field hidden = (Field) savedPayload(saved, M3MemberKind.FIELD, Sample.class,
                "hidden", "J");
        oracle.check(hidden.trySetAccessible(), "retained private field access state");
        hidden.setLong(instance, 29L);
        for (Access access : accesses) checkLive(access, oracle);
        oracle.check(hidden.getLong(instance) == 29L, "retained mutable payload remains usable");
        for (Saved result : saved) {
            oracle.check(result.optional().orElseThrow() == result.payload(),
                    "final published payload identity stable: " + result.access().key());
        }
        oracle.observe("usable|11|33|pair|29");
        Reference.reachabilityFence(ROOTS);
        return oracle.finish();
    }

    /** Boundary retention policy must not alter the frozen owner/member observations. */
    public static Observation standardCompactParity() {
        Oracle oracle = new Oracle();
        M3CI standard = compile(M3CI.Options.standard());
        M3CI compact = compile(M3CI.Options.compact());
        oracle.check(standard.size() == compact.size(), "class count parity");
        oracle.check(standard.memberSize() == compact.memberSize(), "member count parity");
        oracle.check(Arrays.equals(standard.fingerprintSha256(), compact.fingerprintSha256()),
                "frozen fingerprint parity");
        oracle.check(keys(accesses(standard)).equals(keys(accesses(compact))),
                "typed member metadata parity");
        for (int row = 0; row < standard.size(); row++) {
            oracle.check(standard.binaryName(row).equals(compact.binaryName(row)),
                    "class metadata parity at " + row);
            oracle.check(standard.boundary().classAt(row).isPresent(),
                    "standard class boundary at " + row);
            oracle.check(compact.boundary().classAt(row).isEmpty(),
                    "compact class boundary at " + row);
            oracle.observe("class|" + standard.binaryName(row));
        }
        for (int row = 0; row < standard.memberSize(); row++) {
            oracle.check(standard.memberKind(row) == compact.memberKind(row),
                    "member kind parity at " + row);
            oracle.check(standard.memberDeclaringClassRow(row)
                            == compact.memberDeclaringClassRow(row),
                    "declaring row parity at " + row);
            oracle.check(standard.memberName(row).equals(compact.memberName(row)),
                    "member name parity at " + row);
            oracle.check(standard.memberDescriptor(row).equals(compact.memberDescriptor(row)),
                    "member descriptor parity at " + row);
            oracle.observe("member|" + standard.memberKind(row) + "|"
                    + standard.binaryName(standard.memberDeclaringClassRow(row)) + "|"
                    + standard.memberName(row) + "|" + standard.memberDescriptor(row));
        }
        // The existing fingerprint includes first-encounter symbol IDs. Its equality
        // is asserted within this VM above; it is not a portable cross-process key.
        oracle.observe("standard-compact-fingerprints-equal");
        Reference.reachabilityFence(ROOTS);
        return oracle.finish();
    }

    private static M3CI compile(M3CI.Options options) {
        return M3CI.compile(ROOTS, options, M3VI.Progress.none());
    }

    private static List<Access> accesses(M3CI owner) {
        List<Access> result = new ArrayList<>();
        for (Class<?> root : ROOTS) {
            M3Class view = classView(owner, root);
            for (M3Field field : view.getDeclaredFields()) {
                result.add(new Access(owner, field.row(), M3MemberKind.FIELD, root,
                        Field.class, field::tryField, field::toField,
                        "JVM Field boundary is not live for row " + field.row()));
            }
            for (M3Method method : view.getDeclaredMethods()) {
                result.add(new Access(owner, method.row(), M3MemberKind.METHOD, root,
                        Method.class, method::tryMethod, method::toMethod,
                        "JVM Method boundary is not live for row " + method.row()));
            }
            for (M3Constructor constructor : view.getDeclaredConstructors()) {
                result.add(new Access(owner, constructor.row(), M3MemberKind.CONSTRUCTOR, root,
                        Constructor.class, constructor::tryConstructor, constructor::toConstructor,
                        "JVM Constructor boundary is not live for row " + constructor.row()));
            }
            M3RecordComponent[] components = view.getRecordComponents();
            if (components != null) {
                for (M3RecordComponent component : components) {
                    result.add(new Access(owner, component.row(), M3MemberKind.RECORD_COMPONENT,
                            root, RecordComponent.class, component::tryRecordComponent,
                            component::toRecordComponent,
                            "JVM RecordComponent boundary is not live for row " + component.row()));
                }
            }
        }
        return result;
    }

    private static M3Class classView(M3CI owner, Class<?> type) {
        for (int row = 0; row < owner.size(); row++) {
            if (owner.binaryName(row).equals(type.getName())) return owner.classAt(row);
        }
        throw new AssertionError("missing fixture class " + type.getName());
    }

    private static AnnotatedElement checkLive(Access access, Oracle oracle) {
        Optional<?> result = access.optional().get();
        oracle.check(result != null && result.isPresent(), "live result: " + access.key());
        Object value = result.orElseThrow();
        oracle.check(access.runtimeType().isInstance(value), "live type: " + access.key());
        AnnotatedElement payload = (AnnotatedElement) value;
        oracle.check(describe(payload).equals(access.key()), "live payload: " + access.key());
        oracle.check(declaringClass(payload) == access.declaringClass(),
                "live declaring Class identity: " + access.key());
        Optional<AnnotatedElement> generic = access.owner().resolveReflectiveElement(access.row());
        oracle.check(generic.isPresent(), "generic live result: " + access.key());
        oracle.check(describe(generic.orElseThrow()).equals(access.key()),
                "generic and typed payload observations: " + access.key());
        oracle.check(describe(access.required().get()).equals(access.key()),
                "required and optional payload observations: " + access.key());
        oracle.observe(access.key() + "|present");
        return payload;
    }

    private static AnnotatedElement savedPayload(List<Saved> saved, M3MemberKind kind,
            Class<?> declaringClass, String name, String descriptor) {
        String key = memberKey(kind, declaringClass, name, descriptor);
        for (Saved value : saved) {
            if (value.access().key().equals(key)) return value.payload();
        }
        throw new AssertionError("missing saved payload " + key);
    }

    private static void checkAllKinds(int[] counts, Oracle oracle) {
        for (M3MemberKind kind : M3MemberKind.values()) {
            oracle.check(counts[kind.ordinal()] > 0, "coverage for " + kind);
            oracle.observe("count|" + kind + "|" + counts[kind.ordinal()]);
        }
    }

    private static List<String> keys(List<Access> accesses) {
        return accesses.stream().map(Access::key).sorted().toList();
    }

    private static List<String> reflectiveKeys() {
        List<String> keys = new ArrayList<>();
        for (Class<?> root : ROOTS) {
            for (Field field : root.getDeclaredFields()) keys.add(describe(field));
            for (Method method : root.getDeclaredMethods()) keys.add(describe(method));
            for (Constructor<?> constructor : root.getDeclaredConstructors()) {
                keys.add(describe(constructor));
            }
            RecordComponent[] components = root.getRecordComponents();
            if (components != null) {
                for (RecordComponent component : components) keys.add(describe(component));
            }
        }
        return keys.stream().sorted().toList();
    }

    private static String describe(AnnotatedElement element) {
        if (element instanceof Field field) {
            return memberKey(M3MemberKind.FIELD, field.getDeclaringClass(), field.getName(),
                    M3Descriptor.field(field));
        }
        if (element instanceof Method method) {
            return memberKey(M3MemberKind.METHOD, method.getDeclaringClass(), method.getName(),
                    M3Descriptor.method(method));
        }
        if (element instanceof Constructor<?> constructor) {
            return memberKey(M3MemberKind.CONSTRUCTOR, constructor.getDeclaringClass(),
                    constructor.getName(), M3Descriptor.constructor(constructor));
        }
        if (element instanceof RecordComponent component) {
            return memberKey(M3MemberKind.RECORD_COMPONENT, component.getDeclaringRecord(),
                    component.getName(), M3Descriptor.recordComponent(component));
        }
        throw new AssertionError("unexpected reflective element " + element);
    }

    private static Class<?> declaringClass(AnnotatedElement element) {
        if (element instanceof Field field) return field.getDeclaringClass();
        if (element instanceof Method method) return method.getDeclaringClass();
        if (element instanceof Constructor<?> constructor) return constructor.getDeclaringClass();
        if (element instanceof RecordComponent component) return component.getDeclaringRecord();
        throw new AssertionError("unexpected reflective element " + element);
    }

    private static String memberKey(M3MemberKind kind, Class<?> owner,
            String name, String descriptor) {
        return kind + "|" + owner.getName() + "|" + name + "|" + descriptor;
    }
}
