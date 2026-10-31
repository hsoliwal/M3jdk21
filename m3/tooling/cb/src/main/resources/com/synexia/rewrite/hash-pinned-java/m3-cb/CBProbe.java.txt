// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary Class counterpart metadata, loader identity, releases and precompute context
 * @library /jdk/internal/mindex/cb
 * @modules java.base/jdk.internal.mindex
 * @build com.m3.cb.VmFixture
 * @run main/othervm -Xmx256m -XX:+ClassUnloading -Xint com.m3.cb.CBProbe
 * @run main/othervm -Xmx256m -XX:+ClassUnloading -Xmixed com.m3.cb.CBProbe
 */
package com.m3.cb;

import jdk.internal.mindex.*;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.invoke.MethodHandles;
import java.lang.module.ModuleDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Executable contract checks against actual repository owners and real JVM classes. */
public final class CBProbe {
    private static int checks;
    private CBProbe() {}

    public static void main(String[] args) throws Exception {
        bridge(); core(); versions(); keys(); unload();
        System.out.println("PASS checks=" + checks + " owner=java.base");
    }

    private static byte[] fixtureBytes() throws Exception {
        try (InputStream input = CBProbe.class.getResourceAsStream("VmFixture.class")) {
            if (input == null) throw new AssertionError("missing fixture bytes");
            return input.readAllBytes();
        }
    }

    private static final class Loader extends ClassLoader {
        Loader() { super(CBProbe.class.getClassLoader()); }
        Class<?> define(byte[] bytes) { return defineClass(null, bytes, 0, bytes.length); }
    }

    private static void bridge() throws Exception {
        System.clearProperty("synexia.vm.fixture.initialized");
        byte[] bytes = fixtureBytes();
        Loader first = new Loader(), second = new Loader();
        Class<?> a = first.define(bytes), b = second.define(bytes);
        ClassLoader child = new ClassLoader(first) {};
        check(child.loadClass(a.getName()) == a, "parent delegation identity");
        check(a != b && a.getName().equals(b.getName()), "independent definitions");
        Class<?> hidden = MethodHandles.lookup().defineHiddenClass(bytes, false).lookupClass();
        List<Class<?>> roots = List.of(a, b, hidden, Object.class, String.class, String[].class,
                Object[].class, int[].class, int.class, void.class, Runnable.class);
        M3CI index = M3CI.compile(roots, M3CI.Options.standard(),
                M3VI.Progress.none());
        check(System.getProperty("synexia.vm.fixture.initialized") == null, "no fixture initialization");
        for (M3CB.Mode mode : M3CB.Mode.values()) {
            try (M3CB session = M3CB.over(index, mode)) {
                for (Class<?> type : roots) {
                    check(session.fromClass(type).orElseThrow().toClass() == type, "round trip");
                    check(session.name(type).toString().equals(type.getName()), "name parity");
                    for (Class<?> candidate : roots) {
                        check(session.isAssignableFrom(type, candidate) == type.isAssignableFrom(candidate), "assignability parity");
                    }
                }
                check(session.fromClass(a).orElseThrow().loaderSpaceId()
                        != session.fromClass(b).orElseThrow().loaderSpaceId(), "loader spaces");
                expect(NullPointerException.class, () -> session.isAssignableFrom(a, null));
                check(session.name(java.util.HashMap.class).toString().equals("java.util.HashMap"), "unknown fallback");
                M3Release release = release("1.0");
                M3PC key = key(List.of(text("dependency:a")));
                var attachment = session.declare(a, release, key, session.epoch());
                check(session.isCurrent(attachment), "current declaration");
                check(session.effectiveVersion(a).orElseThrow() == release, "effective release derives from counterpart");
                check(session.effective(a).orElseThrow().type().equals(attachment.type()), "effective counterpart owner/row");
                check(session.effectiveVersion(b).isEmpty(), "same-name other loader has no declaration");
                check(session.effectiveVersion(java.util.HashMap.class).isEmpty(), "unknown class no declared version");
                session.declare(b, release("other-loader"), key, 0);
                check(session.effectiveVersion(b).orElseThrow().rawVersion().equals("other-loader"), "defining-loader release isolation");
                check(session.isCurrent(session.declare(a, release("1.0"),
                        key(List.of(text("dependency:a"))), session.epoch())), "idempotent declaration");
                check(session.isCurrent(attachment), "original declaration retained");
                try (M3CB other = M3CB.over(index, mode)) {
                    other.declare(a, release, key, 0);
                    check(!other.isCurrent(attachment), "session isolation");
                }
                expect(IllegalStateException.class, () -> session.declare(a, release("2.0"), key, 0));
                expect(IllegalArgumentException.class, () -> session.declare(java.util.HashMap.class, release, key, 0));
                try (var executor = Executors.newFixedThreadPool(4)) {
                    List<java.util.concurrent.Callable<Boolean>> tasks = new ArrayList<>();
                    for (int i = 0; i < 100; i++) tasks.add(() -> session.isCurrent(session.declare(a, release, key, 0)));
                    for (var result : executor.invokeAll(tasks)) check(result.get(), "concurrent declaration");
                }
                session.invalidate();
                check(!session.isCurrent(attachment) && session.fromClass(a).isEmpty(), "epoch invalidation");
                check(session.effectiveVersion(a).isEmpty() && session.effectiveVersion(b).isEmpty(), "stale effective versions cleared");
                check(session.name(a).toString().equals(a.getName()), "stale fallback");
                expect(IllegalStateException.class, () -> session.declare(a, release, key, 0));
                check(session.statistics().mismatches() == 0, "observation parity");
                if (mode == M3CB.Mode.ENABLED) check(session.statistics().reused() > 0, "reuse occurred");
            }
        }
        M3CI compact = M3CI.compile(List.of(a), M3CI.Options.compact(), M3VI.Progress.none());
        try (M3CB session = M3CB.over(compact, M3CB.Mode.ENABLED)) {
            check(session.fromClass(a).isEmpty(), "compact round trip absent");
            check(session.isAssignableFrom(Runnable.class, a), "compact authoritative fallback");
        }
        M3CB closed = M3CB.over(index, M3CB.Mode.OFF);
        closed.close(); closed.close();
        expect(IllegalStateException.class, () -> closed.name(a));
        AtomicInteger done = new AtomicInteger();
        M3VI.Progress canceled = new M3VI.Progress() {
            @Override public void checkCancelled() { throw new CancellationException(); }
            @Override public void done() { done.incrementAndGet(); }
        };
        expect(CancellationException.class, () -> M3CB.prepare(roots,
                M3CI.Options.standard(), M3CB.Mode.ENABLED, canceled));
        check(done.get() == 1, "prepare cancellation cleanup");
        expect(ClassFormatError.class, () -> new Loader().define(new byte[] {0, 1, 2, 3}));
    }

    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @interface Mark { }
    interface Left { }
    interface Right { }
    @Mark public static class Base implements Left {
        public int value;
        public String echo(String text) throws java.io.IOException { return text; }
        private int hidden;
    }
    public static class Child extends Base implements Right { }
    public record Sample(int number, String text) { }
    sealed interface Shape permits Circle { }
    static final class Circle implements Shape { }

    private static void core() throws Exception {
        List<Class<?>> roots = List.of(Base.class, Child.class, Sample.class, Shape.class, Circle.class,
                String.class, String[].class, Object[].class, int[][].class, int.class, void.class,
                Runnable.class, Left.class, Right.class);
        M3CI index = M3CI.compile(roots, M3CI.Options.standard(), null);
        for (Class<?> type : roots) {
            M3Class view = index.fromClass(type).orElseThrow();
            check(view.getName().equals(type.getName()), "class name");
            check(view.getSimpleName().equals(type.getSimpleName()), "simple name");
            check(java.util.Objects.equals(view.getCanonicalName(), type.getCanonicalName()), "canonical name");
            check(view.getPackageName().equals(type.getPackageName()), "package name");
            check(view.getTypeName().equals(type.getTypeName()), "type name");
            check(view.getModifiers() == type.getModifiers(), "modifiers");
            check(view.isArray() == type.isArray() && view.isPrimitive() == type.isPrimitive(), "array/primitive flags");
            check(view.isInterface() == type.isInterface() && view.isRecord() == type.isRecord(), "interface/record flags");
            check(view.isEnum() == type.isEnum() && view.isSealed() == type.isSealed(), "enum/sealed flags");
            check(view.isAnnotation() == type.isAnnotation() && view.isHidden() == type.isHidden(), "annotation/hidden flags");
            check(view.getClassLoader() == type.getClassLoader() && view.getModule() == type.getModule(), "live boundary");
            check(Arrays.equals(Arrays.stream(view.getInterfaces()).map(M3Class::toClass).toArray(Class<?>[]::new), type.getInterfaces()), "interface declaration order");
            check(view.getDeclaredFields().length == type.getDeclaredFields().length, "field count");
            check(view.getDeclaredMethods().length == type.getDeclaredMethods().length, "method count");
            check(Arrays.stream(view.getDeclaredMethods()).map(M3Method::toMethod).collect(java.util.stream.Collectors.toSet())
                    .equals(new java.util.HashSet<>(Arrays.asList(type.getDeclaredMethods()))), "complete declared method set");
            check(view.getDeclaredConstructors().length == type.getDeclaredConstructors().length, "constructor count");
            for (var field : type.getDeclaredFields()) {
                var indexed = view.getDeclaredField(field.getName());
                check(indexed.toField().equals(field), "field round trip");
            }
            for (var method : type.getDeclaredMethods()) {
                var indexed = view.getDeclaredMethod(method.getName(), method.getParameterTypes());
                check(indexed.toMethod().equals(type.getDeclaredMethod(method.getName(), method.getParameterTypes())), "JDK most-specific return lookup");
            }
            for (Class<?> other : roots) check(view.isAssignableFrom(index.fromClass(other).orElseThrow()) == type.isAssignableFrom(other), "hierarchy parity");
        }
        var base = index.fromClass(Base.class).orElseThrow();
        check(base.isAnnotationPresent(Mark.class), "annotation bridge");
        check(index.fromClass(Child.class).orElseThrow().getField("value").toField().getDeclaringClass() == Base.class, "inherited member declaring owner");
        check(index.fromClass(Sample.class).orElseThrow().getRecordComponents().length == 2, "record components");
        check(index.fromClass(Shape.class).orElseThrow().getPermittedSubclasses()[0].toClass() == Circle.class, "sealed permitted class");
        var sample = new Base();
        check(base.cast(sample) == sample && base.isInstance(sample), "cast/instance boundary");
        expect(ClassCastException.class, () -> base.cast("wrong"));
        for (int i = 0; i < 1000; i++) check(index.classRows(new StringBuilder("missing.").append(i)).length == 0, "read-only negative lookup");
        var again = M3CI.compile(roots, M3CI.Options.standard(), null);
        check(Arrays.equals(index.fingerprintSha256(), again.fingerprintSha256()), "snapshot fingerprint stability");
        check(!Arrays.equals(M3CI.compile(String.class).fingerprintSha256(), M3CI.compile(Runnable.class).fingerprintSha256()), "fingerprint binds text/hierarchy");
        expect(IllegalArgumentException.class, () -> base.isAssignableFrom(again.fromClass(Base.class).orElseThrow()));
        expect(IllegalArgumentException.class, () -> M3CI.compile(roots, new M3CI.Options(1, true), null));
    }

    private static void versions() {
        List<String> labels = List.of(text("2.0"), text("1"), text("1.0"), text("1.0.0"), text("1.1"), text("1.0-ea"));
        var table = M3VI.prepare(labels, M3VI.jpms21(), 100, null);
        check(Arrays.equals(table.between(text("1"), true, text("1.0.0"), true), new int[] {1, 2, 3}), "equal precedence preserves raw rows");
        check(table.label(1).toString().equals("1") && table.label(2).toString().equals("1.0"), "raw labels preserved");
        check(table.between(text("1"), false, text("1"), false).length == 0, "open empty interval");
        AtomicInteger parses = new AtomicInteger();
        AtomicInteger revision = new AtomicInteger(1);
        var authority = M3VI.jpms21();
        M3VI.Scheme<ModuleDescriptor.Version> counting = new M3VI.Scheme<>() {
            @Override public String revision() { return text("counted-jpms-" + revision.get()); }
            @Override public ModuleDescriptor.Version parse(String spelling) {
                parses.incrementAndGet(); return authority.parse(spelling);
            }
            @Override public int compare(ModuleDescriptor.Version a, ModuleDescriptor.Version b) {
                return authority.compare(a, b);
            }
        };
        var counted = M3VI.prepare(labels, counting, 100, null);
        check(parses.get() == labels.size(), "stored versions parsed once");
        counted.between(text("1"), true, text("2"), true);
        check(parses.get() == labels.size() + 2, "warm query parses only bounds");
        revision.incrementAndGet();
        expect(IllegalStateException.class, () -> counted.between(text("1"), true, text("2"), true));
        AtomicInteger cleanup = new AtomicInteger();
        expect(CancellationException.class, () -> M3VI.prepare(labels, authority, 100,
                new M3VI.Progress() {
                    @Override public void checkCancelled() { throw new CancellationException(); }
                    @Override public void done() { cleanup.incrementAndGet(); }
                }));
        check(cleanup.get() == 1, "version cancellation cleanup");
        expect(IllegalArgumentException.class, () -> table.between(text("2"), true, text("1"), true));
        expect(IllegalArgumentException.class, () -> M3VI.prepare(List.of(text("")), M3VI.jpms21(), 10, null));
        expect(IllegalArgumentException.class, () -> M3VI.prepare(labels, M3VI.jpms21(), 1, null));
        Random random = new Random(4421);
        List<String> many = new ArrayList<>();
        for (int i = 0; i < 500; i++) many.add(text(random.nextInt(20) + "." + random.nextInt(10)));
        var prepared = M3VI.prepare(many, M3VI.jpms21(), 500, null);
        for (int i = 0; i < 100; i++) {
            var lower = ModuleDescriptor.Version.parse(random.nextInt(10) + ".0");
            var upper = ModuleDescriptor.Version.parse((10 + random.nextInt(10)) + ".0");
            boolean includeLower = random.nextBoolean(), includeUpper = random.nextBoolean();
            int[] expected = java.util.stream.IntStream.range(0, many.size()).boxed()
                    .filter(row -> { var v = ModuleDescriptor.Version.parse(many.get(row).toString());
                        return (v.compareTo(lower) > 0 || includeLower && v.compareTo(lower) == 0)
                                && (v.compareTo(upper) < 0 || includeUpper && v.compareTo(upper) == 0); })
                    .sorted((a, b) -> ModuleDescriptor.Version.parse(many.get(a).toString())
                            .compareTo(ModuleDescriptor.Version.parse(many.get(b).toString())))
                    .mapToInt(Integer::intValue).toArray();
            check(Arrays.equals(expected, prepared.between(text(lower.toString()), includeLower, text(upper.toString()), includeUpper)), "version range oracle");
        }
    }

    private static void keys() {
        var a = key(List.of(text("a"), text("b")));
        check(a.sameInputs(key(List.of(text("a"), text("b")))), "exact dependency inputs");
        check(!a.sameInputs(key(List.of(text("b"), text("a")))), "dependency order matters");
        check(!a.sameInputs(key(List.of(text("a")))), "dependency length matters");
        check(!a.sameInputs(null), "null key");
        check(!release("1").equals(release("1.0")), "raw release identity");
        expect(IllegalArgumentException.class, () -> new M3Release(text("origin"), text("g"), text("a"),
                text("1"), text("jar"), text(""), text("invalid")));
        Random random = new Random(441);
        for (int i = 0; i < 1000; i++) {
            List<String> values = random.ints(random.nextInt(100), -10, 10).mapToObj(Integer::toString).toList();
            List<String> copy = new ArrayList<>(values);
            check(key(values).sameInputs(key(copy)), "exact context equality");
            if (!copy.isEmpty()) copy.set(random.nextInt(copy.size()), "changed");
            check(key(values).sameInputs(key(copy)) == values.equals(copy), "context mismatch parity");
        }
    }

    private record UnloadProbe(M3CB session, WeakReference<ClassLoader> loader,
            WeakReference<Class<?>> type) {}

    private static UnloadProbe probe() throws Exception {
        Loader loader = new Loader(); Class<?> type = loader.define(fixtureBytes());
        var session = M3CB.prepare(List.of(type), M3CI.Options.standard(), M3CB.Mode.ENABLED, null);
        session.declare(type, release("1"), key(List.of()), 0);
        return new UnloadProbe(session, new WeakReference<>(loader), new WeakReference<>(type));
    }

    private static void unload() throws Exception {
        UnloadProbe probe = probe();
        try (M3CB session = probe.session()) {
            for (int i = 0; i < 100 && probe.loader().get() != null; i++) {
                System.gc(); Thread.sleep(10);
            }
            check(probe.loader().get() == null && probe.type().get() == null, "session permits loader unloading");
            check(session.statistics().mismatches() == 0, "session remains available after collection");
        }
    }

    private static String text(String value) { return value; }
    private static M3Release release(String version) {
        return new M3Release(text("test:local"), text("test"), text("fixture"),
                text(version), text("jar"), text(""), text("0".repeat(64)));
    }
    private static M3PC key(List<String> dependencies) {
        return M3PC.of(text("content"), text("analysis-v1"), text("environment"), dependencies);
    }
    private static void check(boolean condition, String description) {
        checks++; if (!condition) throw new AssertionError(description);
    }
    private static void expect(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); } catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("expected " + type.getName(), error);
        }
        throw new AssertionError("expected " + type.getName());
    }
}
