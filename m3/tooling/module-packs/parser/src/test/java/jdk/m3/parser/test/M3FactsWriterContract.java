/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser.test;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3FactsWriter;
import jdk.m3.parser.M3File;

/** Real SQLite/JNI, transaction and detached-lane contract (no mocked database). */
public final class M3FactsWriterContract {
    private static int checks;

    private M3FactsWriterContract() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("fresh fixture directory required");
        Path directory = Path.of(args[0]);
        Files.createDirectories(directory);
        laneRoundTrip();
        lazyAndBounded();
        streamCloseBeforeCommit();
        callerTransactions();
        failedGenerationAndOwnership();
        foreignTriggersCannotSkipRows();
        temporaryNamespacesCannotDivertRows();
        streamClosedExactlyOnce(directory);
        exactHashBudget();
        pathAndDriver(directory);
        System.out.println("M3_AST_FACTS_SQLITE_PASS checks=" + checks);
    }

    private static M3FactsWriter.Context context(String generation) {
        return new M3FactsWriter.Context(generation, "fixture-javac21", null);
    }

    private static M3ASTIndex tiny(String uri) {
        M3File file = M3File.of(URI.create(uri), "abcdef");
        var builder = M3ASTIndex.builder(file, M3ASTIndex.Domain.GRADLE_GROOVY, 8);
        builder.add(-1, "ROOT'\"", 0, 6, M3ASTIndex.EXACT_RANGE | M3ASTIndex.SYNTAX_ONLY);
        builder.add(0, "child", 1, 3, M3ASTIndex.EXACT_RANGE);
        return builder.seal();
    }

    private static M3ASTIndex javaIndex() throws IOException {
        String text = "class T { static final int N=42; String s=\"\\uD800\"; }\n";
        URI uri = URI.create("memory:///T.java");
        var input = new SimpleJavaFileObject(uri, JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) { return text; }
        };
        var compiler = ToolProvider.getSystemJavaCompiler();
        try (var manager = compiler.getStandardFileManager(null, null, null)) {
            JavacTask task = (JavacTask) compiler.getTask(null, manager, null, List.of("-proc:none"),
                    null, List.of(input));
            CompilationUnitTree unit = task.parse().iterator().next();
            return M3ASTIngress.javac(M3File.of(uri, text), unit, Trees.instance(task),
                    M3ASTIngress.Limits.defaults());
        }
    }

    private static Connection database() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite::memory:");
    }

    private static void laneRoundTrip() throws Exception {
        List<M3ASTIndex> indexes = List.of(tiny("memory:///a.groovy"), javaIndex());
        try (Connection db = database()) {
            var result = M3FactsWriter.write(db, indexes, context("gen'1"), M3FactsWriter.Limits.DEFAULTS);
            check(result.committed() && result.files() == 2, "owned transaction result");
            check(db.getAutoCommit() && !db.isClosed(), "borrowed connection remains usable");
            check(result.nodes() == indexes.stream().mapToLong(M3ASTIndex::nodeCount).sum(), "node total");
            try (var statement = db.prepareStatement("SELECT * FROM m3_ast_files ORDER BY file_id");
                 var rows = statement.executeQuery()) {
                for (int fileId = 0; fileId < indexes.size(); fileId++) {
                    M3ASTIndex index = indexes.get(fileId);
                    check(rows.next(), "complete file rows");
                    check(rows.getInt("file_id") == fileId, "stable file ordinal");
                    check(rows.getString("source_utf16_sha256").equals(index.source().utf16Sha256()), "source identity");
                    check(rows.getString("domain").equals(index.domain().name()), "domain identity");
                    try (var nodes = db.prepareStatement("SELECT * FROM m3_ast_nodes WHERE file_id=? ORDER BY node_id")) {
                        nodes.setInt(1, fileId);
                        try (var row = nodes.executeQuery()) {
                            var hashes = index.structuralHashes(index.nodeCount(), 16_777_216);
                            for (int node = 0; node < index.nodeCount(); node++) {
                                check(row.next(), "complete node rows");
                                check(row.getInt("node_id") == node && row.getString("kind").equals(index.kind(node)), "kind lane");
                                check(Arrays.equals(row.getBytes("kind_utf16"), utf16(index.kind(node))), "lossless kind lane");
                                check(row.getInt("parent") == index.parent(node), "parent lane");
                                check(row.getInt("start") == index.start(node) && row.getInt("end") == index.end(node), "range lanes");
                                check(row.getInt("flags") == index.flags(node), "flags lane");
                                check(row.getInt("first_child") == index.firstChild(node)
                                        && row.getInt("next_sibling") == index.nextSibling(node)
                                        && row.getInt("child_count") == index.childCount(node), "child lanes");
                                check(row.getBoolean("has_details") == index.hasDetails(node), "detail presence");
                                check(row.getInt("start_line") == index.startLine(node)
                                        && row.getInt("end_line") == index.endLine(node), "line lanes");
                                check(Arrays.equals(row.getBytes("name_utf16"), utf16(index.name(node).orElse(null))), "lossless name lane");
                                check(row.getString("literal_kind").equals(index.literalKind(node).name()), "literal kind lane");
                                check(Arrays.equals(row.getBytes("literal_utf16"), utf16(index.literalValue(node).orElse(null))),
                                        "lossless literal including unpaired surrogate");
                                check(row.getLong("modifier_bits") == index.modifierBits(node), "modifier lane");
                                check(row.getInt("enclosing_type") == index.enclosingType(node)
                                        && row.getInt("enclosing_executable") == index.enclosingExecutable(node), "owner lanes");
                                check(row.getString("structural_sha256").equals(hashes.sha256(node)), "structural hash lane");
                                check(row.getString("exact_sha256") == null, "exact hashing is opt-in");
                            }
                            check(!row.next(), "no extra nodes");
                        }
                    }
                }
                check(!rows.next(), "no extra files");
            }
        }
    }

    private static void lazyAndBounded() throws Exception {
        try (Connection db = database()) {
            int[] admitted = {0};
            Iterable<M3ASTIndex> input = () -> new Iterator<>() {
                @Override public boolean hasNext() { return admitted[0] < 50; }
                @Override public M3ASTIndex next() { return tiny("memory:///" + admitted[0]++); }
            };
            var oneFile = new M3FactsWriter.Limits(1, 20, 1, 1024, 0);
            expect(IllegalArgumentException.class, () -> M3FactsWriter.write(db, input, context("limit"), oneFile));
            check(admitted[0] == 1, "maxFiles checked before requesting another index");
            check(!table(db, "m3_ast_generations"), "limit failure rolls back schema and generation");
            var oneNode = new M3FactsWriter.Limits(4, 1, 2, 1024, 0);
            expect(IllegalArgumentException.class, () -> M3FactsWriter.write(db, List.of(tiny("memory:///a")),
                    context("nodes"), oneNode));
            check(!table(db, "m3_ast_nodes"), "node limit rollback");
            expect(IllegalArgumentException.class, () -> new M3FactsWriter.Limits(0, 1, 1, 0, 0));
            expect(IllegalArgumentException.class, () -> new M3FactsWriter.Limits(1, 1, 0, 0, 0));
            expect(IllegalArgumentException.class, () -> new M3FactsWriter.Context("x", "jdk", "not-a-digest"));
            var textBound = new M3FactsWriter.Limits(1, 10, 8, 1024, 0, 16);
            expect(IllegalArgumentException.class, () -> M3FactsWriter.write(db, List.of(tiny("memory:///a")),
                    context("text-budget"), textBound));
            check(!table(db, "m3_ast_nodes"), "oversized JDBC row rolls back before publication");
        }
    }

    private static void streamCloseBeforeCommit() throws Exception {
        try (Connection db = database()) {
            AtomicBoolean closed = new AtomicBoolean();
            var stream = Stream.of(tiny("memory:///a")).onClose(() -> {
                closed.set(true);
                throw new IllegalStateException("provider close sentinel");
            });
            var failure = expect(IllegalStateException.class, () -> M3FactsWriter.write(db, stream,
                    context("close-fail"), M3FactsWriter.Limits.DEFAULTS));
            check(failure.getMessage().equals("provider close sentinel") && closed.get(), "source close failure preserved");
            check(!table(db, "m3_ast_generations"), "close failure cannot publish generation");
            AtomicBoolean successClosed = new AtomicBoolean();
            var success = Stream.of(tiny("memory:///a")).onClose(() -> successClosed.set(true));
            M3FactsWriter.write(db, success, context("closed-success"), M3FactsWriter.Limits.DEFAULTS);
            check(successClosed.get() && count(db, "m3_ast_generations") == 1, "stream closed before successful commit");
        }
    }

    private static void callerTransactions() throws Exception {
        try (Connection db = database()) {
            execute(db, "CREATE TABLE external(value INTEGER)");
            db.setAutoCommit(false);
            execute(db, "INSERT INTO external VALUES(7)");
            var result = M3FactsWriter.write(db, List.of(tiny("memory:///a")), context("caller"),
                    M3FactsWriter.Limits.DEFAULTS);
            check(!result.committed() && !db.getAutoCommit(), "caller owns publication");
            check(count(db, "external") == 1 && count(db, "m3_ast_generations") == 1, "transaction visible locally");
            db.rollback();
            check(count(db, "external") == 0 && !table(db, "m3_ast_generations"), "caller rollback includes facts");
            execute(db, "INSERT INTO external VALUES(8)");
            List<M3ASTIndex> duplicate = List.of(tiny("memory:///a"), tiny("memory:///a"));
            expect(SQLException.class, () -> M3FactsWriter.write(db, duplicate, context("failure"),
                    M3FactsWriter.Limits.DEFAULTS));
            check(!db.getAutoCommit() && count(db, "external") == 1, "savepoint rollback preserves caller work");
            check(!table(db, "m3_ast_generations"), "failed savepoint adds no schema");
            db.commit();
        }
    }

    private static void failedGenerationAndOwnership() throws Exception {
        try (Connection db = database()) {
            execute(db, "CREATE TABLE external(value TEXT)");
            execute(db, "INSERT INTO external VALUES('preserve')");
            M3FactsWriter.write(db, List.of(tiny("memory:///a")), context("first"), M3FactsWriter.Limits.DEFAULTS);
            expect(SQLException.class, () -> M3FactsWriter.write(db, List.of(tiny("memory:///b")),
                    context("first"), M3FactsWriter.Limits.DEFAULTS));
            expect(SQLException.class, () -> M3FactsWriter.write(db, List.of(tiny("memory:///a"), tiny("memory:///a")),
                    context("second"), M3FactsWriter.Limits.DEFAULTS));
            check(count(db, "m3_ast_generations") == 1 && count(db, "m3_ast_files") == 1, "existing generation intact");
            check(count(db, "external") == 1, "unrelated data intact");
            Iterable<M3ASTIndex> broken = () -> new Iterator<>() {
                boolean first = true;
                @Override public boolean hasNext() {
                    if (first) return true;
                    throw new IllegalStateException("iterator sentinel");
                }
                @Override public M3ASTIndex next() { first = false; return tiny("memory:///c"); }
            };
            expect(IllegalStateException.class, () -> M3FactsWriter.write(db, broken, context("broken"),
                    M3FactsWriter.Limits.DEFAULTS));
            check(count(db, "m3_ast_generations") == 1 && db.getAutoCommit(), "lazy traversal failure rollback");
        }
        try (Connection db = database()) {
            execute(db, "CREATE TABLE m3_ast_meta(secret TEXT)");
            execute(db, "INSERT INTO m3_ast_meta VALUES('foreign')");
            expect(SQLException.class, () -> M3FactsWriter.write(db, List.of(tiny("memory:///a")),
                    context("foreign"), M3FactsWriter.Limits.DEFAULTS));
            check(count(db, "m3_ast_meta") == 1 && !table(db, "m3_ast_nodes"), "foreign ownership refused unchanged");
        }
    }

    private static void exactHashBudget() throws Exception {
        try (Connection db = database()) {
            var index = tiny("memory:///a");
            var limits = new M3FactsWriter.Limits(1, 10, 1, 0, 8);
            M3FactsWriter.write(db, List.of(index), context("exact"), limits);
            try (var statement = db.createStatement(); var rows = statement.executeQuery(
                    "SELECT node_id,exact_sha256,structural_sha256 FROM m3_ast_nodes ORDER BY node_id")) {
                while (rows.next()) {
                    int node = rows.getInt(1);
                    check(rows.getString(2).equals(index.sourceRangeSha256(node, 8)), "exact hash domain retained");
                    check(rows.getString(3) == null, "structural hashing can be disabled");
                }
            }
            expect(IllegalArgumentException.class, () -> M3FactsWriter.write(db, List.of(index), context("over-budget"),
                    new M3FactsWriter.Limits(1, 10, 1, 0, 7)));
            check(count(db, "m3_ast_generations") == 1, "cumulative nested-span budget refuses partial generation");
        }
    }

    private static void foreignTriggersCannotSkipRows() throws Exception {
        try (Connection db = database()) {
            M3FactsWriter.write(db, List.of(tiny("memory:///a")), context("before-trigger"), M3FactsWriter.Limits.DEFAULTS);
            execute(db, "CREATE TRIGGER silently_skip BEFORE INSERT ON m3_ast_nodes BEGIN SELECT RAISE(IGNORE); END");
            expect(SQLException.class, () -> M3FactsWriter.write(db, List.of(tiny("memory:///b")),
                    context("after-trigger"), M3FactsWriter.Limits.DEFAULTS));
            check(count(db, "m3_ast_generations") == 1 && count(db, "m3_ast_nodes") == 2,
                    "foreign trigger cannot silently publish missing facts");
        }
    }

    private static void temporaryNamespacesCannotDivertRows() throws Exception {
        for (String sql : List.of(
                "CREATE TEMP TABLE M3_AST_NODES AS SELECT * FROM main.m3_ast_nodes WHERE 0",
                "CREATE TEMP VIEW M3_AST_NODES AS SELECT * FROM main.m3_ast_nodes",
                "CREATE TEMP TRIGGER silently_skip BEFORE INSERT ON main.M3_AST_NODES BEGIN SELECT RAISE(IGNORE); END")) {
            try (Connection db = database()) {
                M3FactsWriter.write(db, List.of(tiny("memory:///a")), context("first"), M3FactsWriter.Limits.DEFAULTS);
                execute(db, sql);
                expect(SQLException.class, () -> M3FactsWriter.write(db, List.of(tiny("memory:///b")),
                        context("diverted"), M3FactsWriter.Limits.DEFAULTS));
                check(count(db, "main.m3_ast_generations") == 1 && count(db, "main.m3_ast_nodes") == 2,
                        "temporary owned names and triggers cannot publish missing main facts");
            }
        }
        try (Connection db = database()) {
            execute(db, "CREATE TEMP TABLE unrelated(value TEXT)");
            execute(db, "INSERT INTO unrelated VALUES('preserve')");
            M3FactsWriter.write(db, List.of(tiny("memory:///a")), context("unrelated"), M3FactsWriter.Limits.DEFAULTS);
            check(count(db, "temp.unrelated") == 1 && count(db, "main.m3_ast_nodes") == 2,
                    "unrelated temporary tables remain usable");
        }
    }

    private static void streamClosedExactlyOnce(Path directory) throws Exception {
        try (Connection db = database()) {
            AtomicInteger closes = new AtomicInteger();
            M3FactsWriter.write(db, closeBoundary(closes, null), context("once"), M3FactsWriter.Limits.DEFAULTS);
            check(closes.get() == 1 && count(db, "m3_ast_generations") == 1, "successful stream closes once before commit");
            closes.set(0);
            RuntimeException sentinel = new IllegalStateException("first close sentinel");
            check(expect(IllegalStateException.class, () -> M3FactsWriter.write(db, closeBoundary(closes, sentinel),
                    context("once-failure"), M3FactsWriter.Limits.DEFAULTS)) == sentinel, "first close identity retained");
            check(closes.get() == 1 && count(db, "m3_ast_generations") == 1, "failed close rolls back and is never retried");
            closes.set(0);
            expect(NullPointerException.class, () -> M3FactsWriter.write(db, closeBoundary(closes, null),
                    null, M3FactsWriter.Limits.DEFAULTS));
            check(closes.get() == 1, "setup failure closes owned stream once");
        }
        AtomicInteger closes = new AtomicInteger();
        M3FactsWriter.write(directory.resolve("close-once-module"), closeBoundary(closes, null),
                context("path-once"), M3FactsWriter.Limits.DEFAULTS);
        check(closes.get() == 1, "path overload shares the single stream owner");
        closes.set(0);
        expect(NullPointerException.class, () -> M3FactsWriter.write((Path) null, closeBoundary(closes, null),
                context("path-setup"), M3FactsWriter.Limits.DEFAULTS));
        check(closes.get() == 1, "path setup failure closes once");
    }

    @SuppressWarnings("unchecked")
    private static Stream<M3ASTIndex> closeBoundary(AtomicInteger closes, RuntimeException failure) {
        Stream<M3ASTIndex> actual = Stream.of(tiny("memory:///close-once"));
        // Only the external resource-close boundary is fault-injected. Actual
        // stream traversal, transactions and SQLite/JNI execute unchanged.
        return (Stream<M3ASTIndex>) Proxy.newProxyInstance(M3FactsWriterContract.class.getClassLoader(),
                new Class<?>[] {Stream.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("close")) {
                        int calls = closes.incrementAndGet();
                        actual.close();
                        if (failure != null) throw failure;
                        if (calls > 1) throw new IllegalStateException("second close after publication");
                        return null;
                    }
                    try { return method.invoke(actual, arguments); }
                    catch (InvocationTargetException error) { throw error.getCause(); }
                });
    }

    private static void pathAndDriver(Path directory) throws Exception {
        Path module = directory.resolve("module");
        var result = M3FactsWriter.write(module, Stream.of(tiny("memory:///disk")), context("disk"),
                M3FactsWriter.Limits.DEFAULTS);
        Path path = module.resolve("target/m3/facts.sqlite");
        check(result.committed() && Files.isRegularFile(path), "requested module path");
        try (Connection db = DriverManager.getConnection("jdbc:sqlite:" + path.toUri().toASCIIString())) {
            check(count(db, "m3_ast_files") == 1, "actual SQLite file readable through JNI");
            check(db.getMetaData().getDriverVersion().startsWith("3.45"), "qualified SQLite driver family");
        }
        Path link = directory.resolve("linked-module");
        Files.createSymbolicLink(link, module.toAbsolutePath());
        expect(IOException.class, () -> M3FactsWriter.write(link, List.of(tiny("memory:///no")), context("link"),
                M3FactsWriter.Limits.DEFAULTS));
    }

    private static byte[] utf16(String text) {
        if (text == null) return null;
        ByteBuffer buffer = ByteBuffer.allocate(text.length() * 2);
        text.chars().forEach(c -> buffer.putChar((char) c));
        return buffer.array();
    }

    private static boolean table(Connection db, String name) throws SQLException {
        try (var statement = db.prepareStatement("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name=?")) {
            statement.setString(1, name);
            try (var rows = statement.executeQuery()) { rows.next(); return rows.getInt(1) != 0; }
        }
    }

    private static long count(Connection db, String table) throws SQLException {
        try (var statement = db.createStatement(); var rows = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rows.next(); return rows.getLong(1);
        }
    }

    private static void execute(Connection db, String sql) throws SQLException {
        try (var statement = db.createStatement()) { statement.execute(sql); }
    }

    @FunctionalInterface private interface Checked { void run() throws Exception; }

    private static <T extends Throwable> T expect(Class<T> type, Checked action) throws Exception {
        try { action.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) { checks++; return type.cast(failure); }
            throw failure;
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
