/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser.test;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3File;

/** Same behavioral consumer runs against the sealed preimage and the candidate. */
public final class M3ASTDetailsContract {
    private static int checks;
    private M3ASTDetailsContract() { }

    public static void main(String[] arguments) throws Exception {
        var failures = new ArrayList<Throwable>();
        run("legacy lanes and flags", M3ASTDetailsContract::legacy, failures);
        run("existing javac tree metadata", M3ASTDetailsContract::existingTree, failures);
        run("physical Unicode-escape and text-block coordinates", M3ASTDetailsContract::escapedSource, failures);
        run("unverified source and missing geometry", M3ASTDetailsContract::sourceEvidence, failures);
        run("half-open lines and strict enclosing ancestors", M3ASTDetailsContract::geometry, failures);
        run("bounded exact UTF-16 hashing", M3ASTDetailsContract::rangeHashes, failures);
        run("bounded structural evidence", M3ASTDetailsContract::structure, failures);
        run("optional provider refusal", M3ASTDetailsContract::optionalProvider, failures);
        if (!failures.isEmpty()) {
            AssertionError error = new AssertionError("M3_AST_DETAILS_FAIL groups=" + failures.size());
            failures.forEach(error::addSuppressed);
            throw error;
        }
        System.out.println("M3_AST_DETAILS_PASS checks=" + checks + " groups=8");
    }

    private static void run(String name, Action action, List<Throwable> failures) {
        try { action.run(); System.out.println("PASS " + name); }
        catch (Throwable failure) { failures.add(failure); System.out.println("FAIL " + name + " " + failure); }
    }

    private static void legacy() throws Exception {
        M3File file = M3File.of(URI.create("memory:/build.gradle"), "plugins { id 'java' }\n");
        M3ASTIndex index = M3ASTIngress.gradleScript(file, false, M3ASTIngress.Limits.defaults());
        check(index.primitiveBytes() == index.nodeCount() * 32L, "old eight-int accounting is unchanged");
        check((Long) call(index, "metadataBytes", new Class<?>[0]) == 0L, "old domain does not allocate optional primitive lanes");
        check(index.domain().ordinal() == 2 && M3ASTIndex.Domain.JACKSON_JSON.ordinal() == 4,
                "old domain ordinals are preserved");
        for (int row = 0; row < index.nodeCount(); row++) {
            check(!(Boolean) call(index, "hasDetails", ints(), row), "old domain has no invented details");
            check(number(index, "startLine", row) == -1 && number(index, "endLine", row) == -1,
                    "old domain line absence remains explicit");
            check(((Optional<?>) call(index, "name", ints(), row)).isEmpty(), "old domain name absence");
        }
        var builder = M3ASTIndex.builder(file, M3ASTIndex.Domain.GRADLE_GROOVY, 2);
        expect(IllegalArgumentException.class, () -> builder.add(-1, "ROOT", 0, file.length(), 16));
        check(builder.add(-1, "ROOT", 0, file.length(), M3ASTIndex.EXACT_RANGE) == 0,
                "unknown flags refusal does not consume a row");
    }

    private static final class Source extends SimpleJavaFileObject {
        private final String text;
        private int reads;
        Source(String text) { super(URI.create("memory:/Demo.java"), Kind.SOURCE); this.text = text; }
        @Override public CharSequence getCharContent(boolean ignoreErrors) { reads++; return text; }
    }
    private record Row(String kind, int parent, int start, int end) { }
    private record Captured(M3ASTIndex index, List<Row> oracle, int reads) { }

    private static Captured parse(String text, ActionWithTree action) throws Exception {
        Source input = new Source(text);
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var files = ToolProvider.getSystemJavaCompiler().getStandardFileManager(
                diagnostics, null, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(null, files, diagnostics,
                    List.of("-proc:none", "-source", "21", "-target", "21"), null, List.of(input));
            CompilationUnitTree tree = task.parse().iterator().next();
            Trees trees = Trees.instance(task);
            SourcePositions positions = trees.getSourcePositions();
            var oracle = new ArrayList<Row>();
            new TreeScanner<Void, Integer>() {
                @Override public Void scan(Tree node, Integer parent) {
                    if (node == null) return null;
                    int row = oracle.size();
                    long start = positions.getStartPosition(tree, node), end = positions.getEndPosition(tree, node);
                    oracle.add(new Row(node.getKind().name(), parent,
                            start < 0 || end < 0 ? -1 : (int) start, start < 0 || end < 0 ? -1 : (int) end));
                    return super.scan(node, row);
                }
            }.scan(tree, -1);
            M3ASTIndex index = action.run(M3File.of(input.toUri(), text), tree, trees, positions, oracle.size());
            check(diagnostics.getDiagnostics().stream().noneMatch(d -> d.getKind() == Diagnostic.Kind.ERROR),
                    "real javac input parses without diagnostics");
            return new Captured(index, List.copyOf(oracle), input.reads);
        }
    }

    private static void existingTree() throws Exception {
        String text = "package sample;\r\npublic class Demo {\r\n"
                + "  static final int N = 42;\r\n  String label = \"hi\";\r\n"
                + "  char letter = '\u03bb';\r\n  long wide = 4294967297L;\r\n"
                + "  float fraction = 1.5f;\r\n  double precise = 1.5d;\r\n"
                + "  boolean yes = true; Object nothing = null;\r\n  interface Api {}\r\n"
                + "  int run(int input) {\r\n    class Local {}\r\n"
                + "    java.util.function.IntUnaryOperator op = v -> v + N;\r\n"
                + "    return op.applyAsInt(input);\r\n  }\r\n}\r\n";
        Captured captured = parse(text, (source, tree, trees, positions, count) ->
                ingress(source, tree, positions, new M3ASTIngress.Limits(text.length(), count, 100)));
        M3ASTIndex index = captured.index();
        check(index.domain().name().equals("JAVAC"), "supplied javac domain");
        check(index.nodeCount() == captured.oracle().size() && captured.reads() == 1,
                "one original parse, no ingress reread or extra tree nodes");
        check(index.source().text().equals(text), "detached source remains readable after compiler file manager closes");
        check((Long) call(index, "metadataBytes", new Class<?>[0]) == index.nodeCount() * 25L,
                "four optional int lanes, one byte lane, and one long lane");
        int unknown = 0;
        for (int row = 0; row < index.nodeCount(); row++) {
            Row expected = captured.oracle().get(row);
            check(index.kind(row).equals(expected.kind()) && index.parent(row) == expected.parent(),
                    "existing TreeScanner preorder and parent coordinates");
            check(index.start(row) == expected.start() && index.end(row) == expected.end(),
                    "existing SourcePositions, with incomplete pairs marked unknown");
            check((index.flags(row) & M3ASTIndex.SOURCE_UNVERIFIED) != 0,
                    "public tree ingress cannot manufacture source binding");
            check((Boolean) call(index, "hasDetails", ints(), row), "all supplied javac occurrences carry detached details");
            if (expected.start() >= 0) {
                check(number(index, "startLine", row) == lineAt(text, expected.start())
                                && number(index, "endLine", row) == lineAt(text, expected.end()),
                        "one-based lines refer to the caller's exact UTF-16 source");
                check(number(index, "line", row) == number(index, "startLine", row), "line aliases startLine");
                int span = expected.start() == expected.end() ? 0
                        : lineAt(text, expected.end() - 1) - lineAt(text, expected.start()) + 1;
                check(number(index, "spanLines", row) == span, "span uses last covered UTF-16 unit");
            } else {
                unknown++;
                check(number(index, "startLine", row) == -1 && number(index, "spanLines", row) == -1,
                        "unknown geometry does not acquire guessed line facts");
            }
        }
        check(unknown > 0, "actual javac supplies nodes without complete geometry");
        int type = named(index, "CLASS", "Demo"), method = named(index, "METHOD", "run");
        int variable = named(index, "VARIABLE", "N"), api = named(index, "INTERFACE", "Api");
        long staticBit = bit("STATIC"), finalBit = bit("FINAL"), interfaceBit = bit("INTERFACE");
        check((modifier(index, variable) & (staticBit | finalBit)) == (staticBit | finalBit), "static and final detached modifier bits");
        check((modifier(index, api) & interfaceBit) != 0, "interface kind contributes explicit interface capability");
        check(number(index, "enclosingType", method) == type, "method's strict enclosing type");
        int lambda = first(index, "LAMBDA_EXPRESSION"), v = named(index, "IDENTIFIER", "v");
        check(number(index, "enclosingExecutable", v) == lambda, "lambda body uses closest executable ancestor");
        check(number(index, "enclosingExecutable", lambda) == method, "lambda itself uses outer method ancestor");
        check((Integer) call(index, "firstEnclosing", new Class<?>[]{int.class, String.class}, v, "METHOD") == method,
                "kind navigation crosses a nested lambda");
        check((Integer) call(index, "firstEnclosing", new Class<?>[]{int.class, String.class}, type, "CLASS") == -1,
                "enclosing query excludes the node itself");
        literal(index, "INT_LITERAL", "INT", "42");
        literal(index, "LONG_LITERAL", "LONG", "4294967297");
        literal(index, "FLOAT_LITERAL", "FLOAT", "1069547520");
        literal(index, "DOUBLE_LITERAL", "DOUBLE", "4609434218613702656");
        literal(index, "STRING_LITERAL", "STRING", "hi");
        literal(index, "CHAR_LITERAL", "CHAR", "\u03bb");
        literal(index, "BOOLEAN_LITERAL", "BOOLEAN", "true");
        literal(index, "NULL_LITERAL", "NULL", null);
        check(call(index, "literalKind", ints(), type).toString().equals("NONE"), "nonliteral distinction from null literal");
        System.out.println("M3_AST_DETAILS_COST nodes=" + index.nodeCount() + " primitive_bytes=" + index.primitiveBytes()
                + " metadata_bytes=" + call(index, "metadataBytes", new Class<?>[0])
                + " source_units=" + text.length() + " original_source_reads=" + captured.reads());
    }

    private static void escapedSource() throws Exception {
        String escape = "\\" + "u";
        String text = "class Demo { // raw physical line" + escape + "000a int n = 0x10; String a = \""
                + escape + "0061\"; String block = \"\"\"\n  hello\n  \"\"\"; }";
        Captured captured = parse(text, (source, tree, trees, positions, count) ->
                ingress(source, tree, positions, M3ASTIngress.Limits.defaults()));
        M3ASTIndex index = captured.index();
        int number = first(index, "INT_LITERAL");
        check(index.sourceText(number).equals("0x10") && value(index, "literalValue", number).equals("16"),
                "literal metadata is decoded while exact source spelling remains unchanged");
        check(number(index, "line", number) == 1, "Unicode-escaped logical newline does not invent a physical source line");
        boolean escaped = false, block = false;
        for (int row = 0; row < index.nodeCount(); row++) {
            if (!index.kind(row).equals("STRING_LITERAL")) continue;
            String value = value(index, "literalValue", row);
            if ("a".equals(value)) escaped = index.sourceText(row).equals("\"" + escape + "0061\"");
            if ("hello\n".equals(value)) block = number(index, "spanLines", row) == 3;
        }
        check(escaped && block, "original escaped spelling and multiline text-block geometry remain detached");
    }

    private static void sourceEvidence() throws Exception {
        String text = "class Demo { int n = 42; }";
        Captured wrong = parse(text, (source, tree, trees, positions, count) -> {
            M3File other = M3File.of(source.uri(), text.replace("42", "99"));
            return (M3ASTIndex) staticCall(M3ASTIngress.class, "javac",
                    new Class<?>[]{M3File.class, CompilationUnitTree.class, Trees.class, M3ASTIngress.Limits.class},
                    other, tree, trees, M3ASTIngress.Limits.defaults());
        });
        int n = first(wrong.index(), "INT_LITERAL");
        check(wrong.index().sourceText(n).equals("99") && value(wrong.index(), "literalValue", n).equals("42"),
                "same URI and length do not equate source text with original tree values");
        check((wrong.index().flags(n) & M3ASTIndex.SOURCE_UNVERIFIED) != 0, "mismatched same-length text stays explicitly unverified");
        parse(text, (source, tree, trees, positions, count) -> {
            SourcePositions unknown = new SourcePositions() {
                @Override public long getStartPosition(CompilationUnitTree unit, Tree node) { return -1; }
                @Override public long getEndPosition(CompilationUnitTree unit, Tree node) { return positions.getEndPosition(unit, node); }
            };
            M3ASTIndex index = ingress(source, tree, unknown, M3ASTIngress.Limits.defaults());
            for (int row = 0; row < index.nodeCount(); row++) check(index.start(row) == -1 && index.end(row) == -1,
                    "one missing endpoint makes the pair explicitly unknown");
            expect(IllegalArgumentException.class, () -> ingress(source, tree, positions,
                    new M3ASTIngress.Limits(source.length(), count - 1, 100)));
            expect(IllegalArgumentException.class, () -> ingress(source, tree, positions,
                    new M3ASTIngress.Limits(source.length(), count, 1)));
            expect(IllegalArgumentException.class, () -> ingress(source, tree, positions,
                    new M3ASTIngress.Limits(source.length() - 1, count, 100)));
            expect(IllegalArgumentException.class, () -> ingress(M3File.of(source.uri(), "x"), tree, positions,
                    M3ASTIngress.Limits.defaults()));
            return index;
        });
    }

    private static void geometry() throws Exception {
        M3File source = M3File.of(URI.create("memory:/lines"), "A\r\nB\nC\rD\n");
        var builder = M3ASTIndex.builder(source, domain("JAVAC"), 4);
        int root = builder.add(-1, "ROOT", 0, source.length(), M3ASTIndex.EXACT_RANGE);
        details(builder, root, 1, 5, null, "NONE", null, 0, -1, -1);
        int b = builder.add(root, "B", 3, 5, M3ASTIndex.EXACT_RANGE);
        details(builder, b, 2, 3, "B", "NONE", null, 0, root, -1);
        int empty = builder.add(root, "EMPTY", 5, 5, M3ASTIndex.EXACT_RANGE);
        expect(IllegalArgumentException.class, () -> details(builder, empty, 3, 3, null, "NONE", null, 0, b, -1));
        details(builder, empty, 3, 3, null, "NONE", null, 0, root, -1);
        expect(IllegalStateException.class, () -> details(builder, b, 2, 3, "again", "NONE", null, 0, root, -1));
        int missing = builder.add(root, "MISSING", -1, -1, 0);
        expect(IllegalArgumentException.class, () -> details(builder, missing, 1, 1, null, "NONE", null, 0, root, -1));
        details(builder, missing, -1, -1, null, "NONE", null, 0, root, -1);
        M3ASTIndex index = builder.seal();
        check(number(index, "spanLines", root) == 4 && number(index, "spanLines", b) == 1,
                "exclusive end at next line start does not add a covered line");
        check(number(index, "spanLines", empty) == 0 && number(index, "spanLines", missing) == -1,
                "empty and unknown spans differ");
        expect(IllegalStateException.class, () -> details(builder, root, 1, 5, null, "NONE", null, 0, -1, -1));
        expect(IndexOutOfBoundsException.class, () -> number(index, "line", index.nodeCount()));
    }

    private static void rangeHashes() throws Exception {
        M3File source = M3File.of(URI.create("memory:/hash"), "A");
        var builder = M3ASTIndex.builder(source, domain("JAVAC"), 3);
        int root = builder.add(-1, "ROOT", 0, 1, M3ASTIndex.EXACT_RANGE);
        int empty = builder.add(root, "EMPTY", 0, 0, M3ASTIndex.EXACT_RANGE);
        int missing = builder.add(root, "MISSING", -1, -1, 0);
        M3ASTIndex index = builder.seal();
        check(rangeHash(index, root, 1).equals("c00b4d3c929cb5cc316691ed4636f634576f2c9b2954767234c5274e9dde185d"),
                "literal independent UTF-16BE SHA-256 for A");
        check(rangeHash(index, empty, 0).equals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"),
                "zero-unit budget accepts exact empty range");
        expect(IllegalArgumentException.class, () -> rangeHash(index, root, 0));
        expect(IllegalArgumentException.class, () -> rangeHash(index, root, -1));
        expect(IllegalStateException.class, () -> rangeHash(index, missing, 100));
        check(M3ASTIndex.class.getField("SOURCE_RANGE_HASH_SCHEMA").get(null)
                .equals("M3_SOURCE_RANGE_UTF16BE_SHA256_V1"), "exact-hash schema is explicit");
        M3File unicode = M3File.of(URI.create("memory:/unicode"), "\ud83d\ude00\r\nx");
        var unicodeBuilder = M3ASTIndex.builder(unicode, domain("JAVAC"), 1);
        unicodeBuilder.add(-1, "ROOT", 0, unicode.length(), M3ASTIndex.EXACT_RANGE);
        check(rangeHash(unicodeBuilder.seal(), 0, 5).equals("8b23411ba5bf7b668dc8da707bead77bd435b2c1f3e71ca53346e9fdebe9363e"),
                "supplementary UTF-16 and CRLF are not normalized before hashing");
    }

    private static void structure() throws Exception {
        M3ASTIndex plain = pair("ab", 0, "a", "b", "1", 0, "JAVAC");
        M3ASTIndex shifted = pair(" ab ", 1, "a", "b", "1", 0, "JAVAC");
        String original = structural(plain, 3, 1000, 0);
        check(original.equals("4988df9deeee727319572c28808cadfdef1c09af26ecd21fcdbb86fc76ec1d10"),
                "literal independent golden for documented V1 structural framing");
        check(original.equals(structural(plain, 3, 28, 0)), "exact metadata text-unit budget is admitted");
        expect(IllegalArgumentException.class, () -> structural(plain, 3, 27, 0));
        check(original.equals(structural(shifted, 3, 1000, 0)), "structural fingerprint excludes source offsets and whitespace");
        check(!original.equals(structural(pair("ab", 0, "b", "a", "1", 0, "JAVAC"), 3, 1000, 0)),
                "ordered child metadata changes structural evidence");
        check(!original.equals(structural(pair("ab", 0, "a", "b", "2", 0, "JAVAC"), 3, 1000, 0)),
                "literal value participates");
        check(!original.equals(structural(pair("ab", 0, "a", "b", "1", bit("STATIC"), "JAVAC"), 3, 1000, 0)),
                "modifier bits participate");
        check(!original.equals(structural(pair("ab", 0, "a", "b", "1", 0, "OPENREWRITE"), 3, 1000, 0)),
                "provider domains are not semantically equated");
        expect(IllegalArgumentException.class, () -> structural(plain, 2, 1000, 0));
        expect(IllegalArgumentException.class, () -> structural(plain, 3, 0, 0));
        expect(IllegalArgumentException.class, () -> structural(plain, 3, -1, 0));
        Object hashes = call(plain, "structuralHashes", new Class<?>[]{int.class, long.class}, 3, 1000L);
        check(call(hashes, "schema", new Class<?>[0]).equals("M3_AST_STRUCTURE_SHA256_V1"), "versioned structural framing");
        check((Integer) call(hashes, "nodeCount", new Class<?>[0]) == 3, "one flat result covers exactly admitted nodes");
        expect(IndexOutOfBoundsException.class, () -> call(hashes, "sha256", ints(), 3));
    }

    private static M3ASTIndex pair(String text, int start, String left, String right, String literal,
                                   long modifiers, String domain) throws Exception {
        var builder = M3ASTIndex.builder(M3File.of(URI.create("memory:/pair"), text), domain(domain), 3);
        int root = builder.add(-1, "ROOT", start, start + 2, M3ASTIndex.EXACT_RANGE | M3ASTIndex.SYNTAX_ONLY);
        details(builder, root, 1, 1, null, "NONE", null, 0, -1, -1);
        int a = builder.add(root, "IDENTIFIER", start, start + 1, M3ASTIndex.EXACT_RANGE | M3ASTIndex.SYNTAX_ONLY);
        details(builder, a, 1, 1, left, "NONE", null, modifiers, -1, -1);
        int b = builder.add(root, "INT_LITERAL", start + 1, start + 2, M3ASTIndex.EXACT_RANGE | M3ASTIndex.SYNTAX_ONLY);
        details(builder, b, 1, 1, right, "INT", literal, 0, -1, -1);
        return builder.seal();
    }

    private static void optionalProvider() throws Exception {
        M3File source = M3File.of(URI.create("memory:/empty.java"), "");
        expect(IllegalArgumentException.class, () -> staticCall(M3ASTIngress.class, "openrewriteJ",
                new Class<?>[]{M3File.class, Object.class, M3ASTIngress.Limits.class},
                source, new Object(), M3ASTIngress.Limits.defaults()));
    }

    private static int lineAt(String text, int offset) {
        int line = 1;
        for (int i = 0; i < offset; i++) {
            char c = text.charAt(i);
            if (c == '\n') line++;
            else if (c == '\r' && (i + 1 == text.length() || text.charAt(i + 1) != '\n')) line++;
        }
        return line;
    }
    private static int named(M3ASTIndex index, String kind, String name) throws Exception {
        for (int i = 0; i < index.nodeCount(); i++) if (index.kind(i).equals(kind) && name.equals(value(index, "name", i))) return i;
        throw new AssertionError("missing " + kind + " " + name);
    }
    private static int first(M3ASTIndex index, String kind) {
        for (int i = 0; i < index.nodeCount(); i++) if (index.kind(i).equals(kind)) return i;
        throw new AssertionError("missing kind " + kind);
    }
    private static void literal(M3ASTIndex index, String nodeKind, String literalKind, String value) throws Exception {
        int node = first(index, nodeKind);
        check(call(index, "literalKind", ints(), node).toString().equals(literalKind), "literal kind " + nodeKind);
        check(java.util.Objects.equals(value(index, "literalValue", node), value), "detached canonical literal " + nodeKind);
    }
    private static long bit(String name) throws Exception { return (Long) M3ASTIndex.class.getField(name).get(null); }
    private static long modifier(M3ASTIndex index, int node) throws Exception { return (Long) call(index, "modifierBits", ints(), node); }
    private static int number(M3ASTIndex index, String method, int node) throws Exception { return (Integer) call(index, method, ints(), node); }
    private static String value(M3ASTIndex index, String method, int node) throws Exception {
        return (String) ((Optional<?>) call(index, method, ints(), node)).orElse(null);
    }
    private static M3ASTIndex.Domain domain(String name) { return Enum.valueOf(M3ASTIndex.Domain.class, name); }
    private static M3ASTIndex ingress(M3File file, CompilationUnitTree tree, SourcePositions positions, M3ASTIngress.Limits limits) throws Exception {
        return (M3ASTIndex) staticCall(M3ASTIngress.class, "javac",
                new Class<?>[]{M3File.class, CompilationUnitTree.class, SourcePositions.class, M3ASTIngress.Limits.class}, file, tree, positions, limits);
    }
    private static String rangeHash(M3ASTIndex index, int node, int budget) throws Exception {
        return (String) call(index, "sourceRangeSha256", new Class<?>[]{int.class, int.class}, node, budget);
    }
    private static String structural(M3ASTIndex index, int nodeBudget, long textBudget, int node) throws Exception {
        Object result = call(index, "structuralHashes", new Class<?>[]{int.class, long.class}, nodeBudget, textBudget);
        return (String) call(result, "sha256", ints(), node);
    }
    private static void details(M3ASTIndex.Builder builder, int node, int startLine, int endLine, String name,
                                String literalKind, String literalValue, long modifiers, int type, int executable) throws Exception {
        Class<?> literalType = Class.forName("jdk.m3.parser.M3ASTIndex$LiteralKind");
        Object literal = literalType.getField(literalKind).get(null);
        call(builder, "details", new Class<?>[]{int.class, int.class, int.class, String.class, literalType,
                String.class, long.class, int.class, int.class}, node, startLine, endLine, name, literal,
                literalValue, modifiers, type, executable);
    }
    private static Class<?>[] ints() { return new Class<?>[]{int.class}; }
    private static Object staticCall(Class<?> type, String method, Class<?>[] parameterTypes, Object... values) throws Exception {
        return invoke(type, null, method, parameterTypes, values);
    }
    private static Object call(Object receiver, String method, Class<?>[] parameterTypes, Object... values) throws Exception {
        return invoke(receiver.getClass(), receiver, method, parameterTypes, values);
    }
    private static Object invoke(Class<?> type, Object receiver, String method, Class<?>[] parameters, Object[] values) throws Exception {
        try { return type.getMethod(method, parameters).invoke(receiver, values); }
        catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof Error error) throw error;
            if (failure.getCause() instanceof Exception exception) throw exception;
            throw new AssertionError(failure.getCause());
        }
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static <T extends Throwable> T expect(Class<T> type, Action action) throws Exception {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return type.cast(failure);
            throw new AssertionError("expected " + type.getName() + " but got " + failure, failure);
        }
        throw new AssertionError("expected " + type.getName());
    }
    @FunctionalInterface private interface Action { void run() throws Exception; }
    @FunctionalInterface private interface ActionWithTree {
        M3ASTIndex run(M3File source, CompilationUnitTree tree, Trees trees, SourcePositions positions, int count) throws Exception;
    }
}
