/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */

/* @test
 * @summary Detached typed method facts reuse original attribution, source boundaries and event memoization
 * @modules jdk.compiler/com.sun.source.util
 * @run main M3ASTMethodFactsTest
 */

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import com.sun.source.util.M3AST.MethodFacts;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.lang.model.element.TypeElement;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import static com.sun.source.util.M3AST.MethodFacts.*;

public class M3ASTMethodFactsTest {
    private static int checks;
    private static int sequence;
    private static Path work;
    private static final String TEXT = """
            package facts;
            import java.io.IOException;
            import java.util.function.IntUnaryOperator;
            abstract class Host {
                int field, other;
                int[] array = new int[1];
                Host() { field = 1; }
                Host(int x) { this(); field = x; }
                abstract int absent(int p) throws IOException;
                native int nativeCall();
                static int helper(int x) { return x; }
                static int helper(long x) { return (int) x; }
                <T extends Number> T generic(T x) { return x; }
                String[] variable(int... xs) { return new String[xs.length]; }
                int sample(int p) throws IOException {
                    String decoy = "if (false) { missing(); new Fake(); throw fake; }";
                    // while (true) { field++; phantom(); }
                    int sum = field;
                    field = p;
                    other += field;
                    ++field;
                    int[] a = new int[] { field };
                    a[0]++;
                    this.array[0] = other;
                    if (p > 0) { for (int i = 0; i < p; i++) { sum += helper(i); } }
                    else { while (sum < 0) sum++; }
                    do { sum--; } while (sum > 10);
                    for (int x : a) sum += x;
                    int branch = p > 0 ? 1 : 2;
                    switch (p) { case 0: sum++; break; default: sum--; }
                    int choice = switch (p) { case 1 -> 1; default -> 2; };
                    synchronized (this) { field++; }
                    Runnable skipped = () -> {
                        field++; helper(8); new Object(); throw new RuntimeException();
                    };
                    class Local { int value() { return field; } }
                    Runnable anonymous = new Runnable() {
                        public void run() { field++; helper(9); }
                    };
                    IntUnaryOperator reference = Host::helper;
                    Object object = new Object();
                    if (p < 0) throw new IOException();
                    return sum + branch + choice;
                }
                class Nested { int inner() { return field; } }
            }
            class Peer { int independent() { return 1; } }
            record Pair(int value) {
                Pair { if (value < 0) throw new IllegalArgumentException(); }
                int twice() { return value * 2; }
            }
            """;

    public static void main(String[] args) throws Exception {
        work = Files.createTempDirectory("m3-method-facts-");
        exactSourceFacts();
        unresolvedAndBoundaries();
        contextAndSourceLifetime();
        nestedLambdaAndFieldTargets();
        primitiveStorageAndValidation();
        widerProjection();
        System.out.println("M3_AST_METHOD_FACTS_PASS checks=" + checks);
    }

    private static final class Source extends SimpleJavaFileObject {
        String text;
        int reads;
        Source(String name, String text) {
            super(URI.create("memory:///" + name + ".java"), Kind.SOURCE);
            this.text = text;
        }
        @Override public CharSequence getCharContent(boolean ignored) {
            reads++;
            return new StringBuilder(text);
        }
    }

    private record Result(Path output, Map<String, MethodFacts> facts,
                          M3AST.Summary summary, boolean successful) { }

    private static Result compile(Source source, boolean selected, boolean syntax,
                                  Runnable afterParse) throws Exception {
        Path output = Files.createDirectory(work.resolve("compile-" + (++sequence)));
        var compiler = ToolProvider.getSystemJavaCompiler();
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        Map<String, MethodFacts> facts = new HashMap<>();
        M3AST.Unit[] expired = new M3AST.Unit[1];
        try (var files = compiler.getStandardFileManager(diagnostics, null, null)) {
            JavacTask task = (JavacTask) compiler.getTask(null, files, diagnostics,
                    List.of("-proc:none", "-g", "-d", output.toString()), null, List.of(source));
            M3AST session = selected ? M3AST.open(task, syntax) : null;
            if (selected) {
                session.addConsumer(new M3AST.Consumer() {
                    @Override public void parsed(M3AST.Unit unit) throws Exception {
                        expect(IllegalStateException.class, () -> M3AST.methodFacts(unit));
                        if (afterParse != null) afterParse.run();
                    }
                    @Override public void analyzed(M3AST.Unit unit) throws Exception {
                        MethodFacts value = M3AST.methodFacts(unit);
                        check(value == M3AST.methodFacts(unit), "same event reuses the same result");
                        check(facts.put(value.analyzedBinaryName(), value) == null,
                                "one slice per original top-level analysis event");
                        check(!value.completeCallGraph(), "observed edges cannot certify a complete call graph");
                        check(CompletableFuture.supplyAsync(() -> {
                            try { M3AST.methodFacts(unit); return false; }
                            catch (IllegalStateException expected) { return true; }
                            catch (Exception unexpected) { throw new AssertionError(unexpected); }
                        }).join(), "cross-thread live event access is refused");
                        check(CompletableFuture.supplyAsync(value::rowCount).join() == value.rowCount(),
                                "detached DTO is readable from another thread");
                        expired[0] = unit;
                    }
                });
                session.addConsumer(new M3AST.Consumer() {
                    @Override public void analyzed(M3AST.Unit unit) throws Exception {
                        MethodFacts value = M3AST.methodFacts(unit);
                        check(value == facts.get(value.analyzedBinaryName()), "consumers share one projection");
                    }
                });
            }
            boolean successful = task.call();
            M3AST.Summary summary = selected ? session.summary() : null;
            if (selected) {
                check(summary.closed() && summary.completed() && !summary.consumerFailed(), "normal owner cleanup");
                check(summary.projectionComputations() == summary.analyzedTypes(), "one new computation per event");
                check(summary.projectionHits() == 2 * summary.analyzedTypes(), "two exact reuse hits per event");
                check(summary.sourceReads() == 0 && source.reads == 1, "no additional source read or parse");
                check(summary.parsedUnits() == 1 && summary.capturedSources() == 1, "one original source capture");
                expect(IllegalStateException.class, () -> M3AST.methodFacts(expired[0]));
                session.close();
            }
            return new Result(output, Map.copyOf(facts), summary, successful);
        }
    }

    private static Result compile(String name, String text) throws Exception {
        return compile(new Source(name, text), true, true, null);
    }

    private static void exactSourceFacts() throws Exception {
        Result observed = compile("facts/Host", TEXT);
        Result ordinary = compile(new Source("facts/Host", TEXT), false, false, null);
        check(observed.successful() && ordinary.successful(), "real source compiles in both lanes");
        check(observed.facts().size() == 3, "three top-level types produce separate slices");
        MethodFacts facts = observed.facts().get("facts.Host");
        check(facts.rowCount() == 12, "only twelve explicit methods/constructors, including nested source methods");
        check(facts.sourceUri().equals(URI.create("memory:///facts/Host.java"))
                        && facts.sourceLength() == TEXT.length()
                        && facts.sourceUtf16Sha256().equals(hash(TEXT)), "original source seal is exact UTF-16");
        check(UUID.fromString(facts.compilerContextId()).toString().equals(facts.compilerContextId()),
                "context discriminator is a detached canonical UUID");
        for (MethodFacts value : observed.facts().values())
            check(value.compilerContextId().equals(facts.compilerContextId()), "one context token per compiler task");
        int row = row(facts, "facts.Host#sample(I)I");
        int[][] expected = {
            {PARAMETER_COUNT, 1}, {IF_COUNT, 2}, {CONDITIONAL_COUNT, 1}, {SWITCH_COUNT, 2},
            {LOOP_COUNT, 4}, {MAX_BLOCK_DEPTH, 3}, {INVOCATION_COUNT, 1}, {NEW_CLASS_COUNT, 3},
            {NEW_ARRAY_COUNT, 1}, {THROW_COUNT, 1}, {SYNCHRONIZED_COUNT, 1},
            {FIELD_READ_COUNT, 8}, {FIELD_WRITE_COUNT, 4}, {LAMBDA_COUNT, 1},
            {MEMBER_REFERENCE_COUNT, 1}, {DECLARED_THROWS_COUNT, 1}, {UNRESOLVED, 0}
        };
        for (int[] entry : expected)
            equal(facts.intValue(row, entry[0]), entry[1], "sample column " + columnName(entry[0]));
        check(TEXT.substring(facts.intValue(row, SOURCE_START), facts.intValue(row, SOURCE_END))
                        .startsWith("int sample(int p)"), "method range starts at original declaration syntax");
        check(TEXT.charAt(facts.intValue(row, BODY_START)) == '{'
                        && TEXT.charAt(facts.intValue(row, BODY_END) - 1) == '}', "body range is half-open");
        check(facts.identity(facts.intValue(row, OWNER_ID)).equals("facts.Host"), "declaring owner ID is exact");
        equal(edges(facts, row, CALL).length, 4, "call sites include explicit object creation");
        check(Arrays.asList(edges(facts, row, CALL)).contains("facts.Host#helper(I)I"), "overload selected by attribution");
        check(Arrays.equals(edges(facts, row, MEMBER_REFERENCE), new String[] {"facts.Host#helper(I)I"}),
                "member reference is a distinct relation rather than an executed call");
        equal(edges(facts, row, FIELD_READ).length, 8, "read CSR preserves all resolved sites");
        equal(edges(facts, row, FIELD_WRITE).length, 4, "write CSR preserves all resolved sites");
        for (String owner : edgeOwners(facts, row, FIELD_WRITE))
            check(owner.equals("facts.Host"), "field owner is the declaring class, not a use-site class");
        equal(facts.intValue(row(facts, "facts.Host#<init>()V"), INVOCATION_COUNT), 0,
                "implicit super() is not source syntax");
        equal(facts.intValue(row(facts, "facts.Host#<init>(I)V"), INVOCATION_COUNT), 1,
                "explicit this() is source syntax");
        int absent = row(facts, "facts.Host#absent(I)I");
        check(!facts.hasValue(absent, IF_COUNT) && !facts.hasValue(absent, BODY_START),
                "bodyless method is missing, not a measured zero body");
        expect(IllegalStateException.class, () -> facts.intValue(absent, IF_COUNT));
        check((facts.longValue(absent, DECLARATION_FLAGS) & FLAG_ABSTRACT) != 0, "abstract declaration flag");
        check((facts.longValue(row(facts, "facts.Host#nativeCall()I"), DECLARATION_FLAGS) & FLAG_NATIVE) != 0,
                "native declaration flag");
        check((facts.longValue(row(facts, "facts.Host#variable([I)[Ljava/lang/String;"), DECLARATION_FLAGS)
                        & FLAG_VARARGS) != 0, "varargs declaration flag");
        row(facts, "facts.Host#generic(Ljava/lang/Number;)Ljava/lang/Number;");
        check(!facts.hasUnresolvedSymbols(), "valid fixture has fully represented observed symbols");
        check(observed.facts().get("facts.Peer").rowCount() == 1, "sibling methods stay in their own event");
        check(observed.facts().get("facts.Pair").rowCount() == 2, "generated record methods are excluded");
        equal(observed.facts().get("facts.Pair").intValue(
                row(observed.facts().get("facts.Pair"), "facts.Pair#<init>(I)V"), PARAMETER_COUNT), 1,
                "compact constructor reports attributed formal parameters");
        Map<String, byte[]> before = classes(ordinary.output()), after = classes(observed.output());
        check(before.keySet().equals(after.keySet()), "projection preserves emitted class inventory");
        for (String name : before.keySet())
            check(Arrays.equals(before.get(name), after.get(name)), "ordinary compiler bytes preserved: " + name);
    }

    private static void unresolvedAndBoundaries() throws Exception {
        Result broken = compile("Broken", "class Broken { Missing unresolved(Missing p) { missing(); missingField++; return p; } }");
        check(!broken.successful() && broken.summary().compilerErrors() > 0, "compiler diagnostics stay authoritative");
        MethodFacts facts = broken.facts().get("Broken");
        check(facts.rowCount() == 1 && facts.hasUnresolvedSymbols(), "failed attribution remains explicit");
        check(facts.intValue(0, UNRESOLVED) == 1 && !facts.hasValue(0, METHOD_ID),
                "invalid descriptor cannot become an exact method ID");
        equal(facts.intValue(0, INVOCATION_COUNT), 1, "syntax count remains available when a callee is unresolved");
        equal(edges(facts, 0, CALL).length, 0, "unresolved callees do not invent dictionary identities");
        expect(NullPointerException.class, () -> M3AST.methodFacts(null));
        M3AST.Unit foreign = new M3AST.Unit() {
            private AssertionError unused() { return new AssertionError("foreign unit must not be queried"); }
            public M3AST.Phase phase() { throw unused(); }
            public CompilationUnitTree tree() { throw unused(); }
            public Trees trees() { throw unused(); }
            public TypeElement analyzedType() { throw unused(); }
            public Object contextIdentity() { throw unused(); }
            public M3AST.Source source() { throw unused(); }
            public <T> T project(M3AST.Projection<T> key) { throw unused(); }
        };
        expect(IllegalArgumentException.class, () -> M3AST.methodFacts(foreign));
        Result noSyntax = compile(new Source("Plain", "class Plain { int f() { return 1; } }"), true, false, null);
        check(noSyntax.successful() && noSyntax.facts().get("Plain").rowCount() == 1,
                "default capture policy remains supported without retroactive parser changes");
        Result noMethods = compile("Empty", "class Empty { }");
        check(noMethods.facts().get("Empty").rowCount() == 0, "implicit default constructor is not a source method");
    }

    private static void contextAndSourceLifetime() throws Exception {
        String text = "public class Stable { public static int value() { return 9; } }";
        Result first = compile("Stable", text), second = compile("Stable", text);
        MethodFacts left = first.facts().get("Stable"), right = second.facts().get("Stable");
        check(!left.compilerContextId().equals(right.compilerContextId()), "identical source in distinct compiler contexts has distinct tokens");
        check(left.sourceUtf16Sha256().equals(right.sourceUtf16Sha256()), "source seal does not pretend to identify context");
        check(Arrays.equals(left.identities(), right.identities())
                        && Arrays.equals(left.presenceMasks(), right.presenceMasks()), "detached facts are deterministic apart from context token");
        for (int column = 1; column < COLUMN_COUNT; column++)
            check(Arrays.equals(left.intColumn(column), right.intColumn(column)), "deterministic integer lane " + column);
        Source source = new Source("Stable", text);
        Result changedAfterParse = compile(source, true, true, () -> source.text = text.replace("return 9", "return 10"));
        MethodFacts frozen = changedAfterParse.facts().get("Stable");
        check(frozen.sourceLength() == text.length() && frozen.sourceUtf16Sha256().equals(hash(text)),
                "method facts bind original parser generation despite later source edits");
        try (URLClassLoader loader = new URLClassLoader(new URL[] {changedAfterParse.output().toUri().toURL()},
                                                       ClassLoader.getPlatformClassLoader())) {
            equal((Integer) loader.loadClass("Stable").getMethod("value").invoke(null), 9,
                    "original source generation still owns emitted behavior");
        }
    }

    private static void nestedLambdaAndFieldTargets() throws Exception {
        String text = """
                class Targets {
                    int x; Targets peer; int[] array;
                    void outer() {
                        Runnable r = () -> { class InLambda { int nested() { return x; } } x++; };
                        int x = 1;
                        x++;
                        (this.x) = x;
                        this.peer.x += this.x;
                        this.array[0]++;
                        int length = this.array.length;
                        Class<?> type = Targets.class;
                    }
                }
                """;
        MethodFacts facts = compile("Targets", text).facts().get("Targets");
        check(facts.rowCount() == 2, "nested source method inside lambda is still its own row");
        int row = row(facts, "Targets#outer()V");
        equal(facts.intValue(row, LAMBDA_COUNT), 1, "lambda is a site, not an inlined body");
        equal(facts.intValue(row, FIELD_READ_COUNT), 5,
                "target qualifier and array reference reads count; local, this, array length and class literal do not");
        equal(facts.intValue(row, FIELD_WRITE_COUNT), 2,
                "array element mutation does not become a field write; parentheses keep target mode");
        check(Arrays.stream(facts.identities()).anyMatch(name -> name.contains("InLambda#nested()I")),
                "local binary name comes from the attributed declaration");
    }

    private static void primitiveStorageAndValidation() throws Exception {
        MethodFacts original = compile("Shape", "class Shape { int x; int f() { return x; } }").facts().get("Shape");
        for (var field : MethodFacts.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            Class<?> type = field.getType();
            check(type.isPrimitive() || type == String.class || type == URI.class
                            || type == String[].class || type == int[][].class || type == long[].class,
                    "DTO has only scalar identities and primitive column arrays: " + field);
        }
        Data input = new Data(original);
        MethodFacts detached = input.create();
        String[] canonical = detached.identities();
        input.names[0] = "changed";
        input.flags[0] = Long.MAX_VALUE;
        input.masks[0] = 0;
        input.ints[0][0] = 999;
        input.offsets[FIELD_READ][0] = 999;
        input.targets[FIELD_READ][0] = 999;
        input.owners[FIELD_READ][0] = 999;
        check(Arrays.equals(canonical, detached.identities()) && detached.hasValue(0, METHOD_ID),
                "constructor detaches every retained lane");
        detached.identities()[0] = "changed again";
        detached.intColumn(PARAMETER_COUNT)[0] = 99;
        detached.longColumn(DECLARATION_FLAGS)[0] = 99;
        detached.presenceMasks()[0] = 0;
        detached.relationOffsets(FIELD_READ)[0] = 99;
        detached.relationIds(FIELD_READ)[0] = 99;
        detached.relationOwnerIds(FIELD_READ)[0] = 99;
        equal(detached.intValue(0, PARAMETER_COUNT), 0, "accessors return defensive copies");
        check(Arrays.equals(canonical, detached.identities()) && edges(detached, 0, FIELD_READ).length == 1,
                "identity and relationship accessors cannot mutate the projection");
        expect(IndexOutOfBoundsException.class, () -> detached.intValue(-1, PARAMETER_COUNT));
        expect(IndexOutOfBoundsException.class, () -> detached.hasValue(1, PARAMETER_COUNT));
        expect(IllegalArgumentException.class, () -> detached.intValue(0, DECLARATION_FLAGS));
        expect(IllegalArgumentException.class, () -> detached.longValue(0, PARAMETER_COUNT));
        expect(IllegalArgumentException.class, () -> detached.intColumn(COLUMN_COUNT));
        expect(IllegalArgumentException.class, () -> detached.relationIds(RELATION_COUNT));
        Data badMask = new Data(original); badMask.masks[0] |= 1L << 60;
        expect(IllegalArgumentException.class, badMask::create);
        Data badWidth = new Data(original); badWidth.ints[0] = new int[0];
        expect(IllegalArgumentException.class, badWidth::create);
        Data badOffsets = new Data(original); badOffsets.offsets[FIELD_READ][1]++;
        expect(IllegalArgumentException.class, badOffsets::create);
        Data badId = new Data(original); badId.targets[FIELD_READ][0] = badId.names.length;
        expect(IllegalArgumentException.class, badId::create);
        Data badNames = new Data(original); badNames.names[0] = badNames.names[1];
        expect(IllegalArgumentException.class, badNames::create);
        Data badRange = new Data(original); badRange.ints[SOURCE_END - 1][0] = original.sourceLength() + 1;
        expect(IllegalArgumentException.class, badRange::create);
    }

    private static final class Data {
        final MethodFacts source;
        final String[] names;
        final long[] flags, masks;
        final int[][] ints = new int[COLUMN_COUNT - 1][];
        final int[][] offsets = new int[RELATION_COUNT][], targets = new int[RELATION_COUNT][], owners = new int[RELATION_COUNT][];
        Data(MethodFacts source) {
            this.source = source; names = source.identities();
            flags = source.longColumn(DECLARATION_FLAGS); masks = source.presenceMasks();
            for (int c = 1; c < COLUMN_COUNT; c++) ints[c - 1] = source.intColumn(c);
            for (int r = 0; r < RELATION_COUNT; r++) {
                offsets[r] = source.relationOffsets(r); targets[r] = source.relationIds(r); owners[r] = source.relationOwnerIds(r);
            }
        }
        MethodFacts create() {
            return new MethodFacts(source.sourceUri(), source.sourceLength(), source.sourceUtf16Sha256(),
                    source.compilerContextId(), source.analyzedBinaryName(), names, flags, ints, masks,
                    offsets, targets, owners);
        }
    }

    private static void widerProjection() throws Exception {
        StringBuilder text = new StringBuilder("class Many { static int helper(int x) { return x; }");
        for (int i = 0; i < 257; i++) text.append("int m").append(i).append("() { return helper(").append(i).append("); }");
        text.append('}');
        MethodFacts facts = compile("Many", text.toString()).facts().get("Many");
        equal(facts.rowCount(), 258, "growing primitive method lanes preserve all source declarations");
        for (int i = 0; i < 257; i++) {
            int row = row(facts, "Many#m" + i + "()I");
            check(Arrays.equals(edges(facts, row, CALL), new String[] {"Many#helper(I)I"}),
                    "CSR growth preserves resolved callee for method " + i);
        }
    }

    private static int row(MethodFacts facts, String identity) {
        for (int row = 0; row < facts.rowCount(); row++)
            if (facts.hasValue(row, METHOD_ID) && facts.identity(facts.intValue(row, METHOD_ID)).equals(identity)) return row;
        throw new AssertionError("missing method " + identity + " in " + Arrays.toString(facts.identities()));
    }

    private static String[] edges(MethodFacts facts, int row, int kind) {
        int[] offsets = facts.relationOffsets(kind), ids = facts.relationIds(kind);
        String[] result = new String[offsets[row + 1] - offsets[row]];
        for (int i = 0; i < result.length; i++) result[i] = facts.identity(ids[offsets[row] + i]);
        return result;
    }

    private static String[] edgeOwners(MethodFacts facts, int row, int kind) {
        int[] offsets = facts.relationOffsets(kind), ids = facts.relationOwnerIds(kind);
        String[] result = new String[offsets[row + 1] - offsets[row]];
        for (int i = 0; i < result.length; i++) result[i] = facts.identity(ids[offsets[row] + i]);
        return result;
    }

    private static Map<String, byte[]> classes(Path root) throws IOException {
        Map<String, byte[]> result = new HashMap<>();
        try (var files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".class")).toList())
                result.put(root.relativize(file).toString(), Files.readAllBytes(file));
        }
        return result;
    }

    private static String hash(String text) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (int i = 0; i < text.length(); i++) {
            digest.update((byte) (text.charAt(i) >>> 8)); digest.update((byte) text.charAt(i));
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    @FunctionalInterface private interface Action { void run() throws Exception; }
    private static void expect(Class<? extends Throwable> type, Action action) throws Exception {
        try { action.run(); }
        catch (Throwable failure) {
            if (!type.isInstance(failure)) throw new AssertionError("expected " + type + ", got " + failure, failure);
            checks++; return;
        }
        throw new AssertionError("expected " + type);
    }
    private static void equal(int actual, int expected, String message) {
        check(actual == expected, message + ": expected " + expected + ", got " + actual);
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
