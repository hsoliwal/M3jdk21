// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser.test;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3FactsWriter;
import jdk.m3.parser.M3File;

/**
 * Facts writer contract: one real javac parse, one JAVAC-domain index, one {@code target/m3/facts.sqlite}
 * written by {@link M3FactsWriter} into the consumer work directory and read back through JDBC. It asserts
 * the recorded schema marker, the per-generation file and node counts against the returned
 * {@link M3FactsWriter.Result}, committed publication, exact lane equality, the documented second-write
 * behaviour (a repeated generation is refused whole, a new generation is appended) and the writer's
 * declared refusals for a missing driver and a facts path that cannot be a regular file.
 */
public final class M3FactsWriterContract {
    private static final String TEXT = """
            package demo;
            public class Demo {
                static final int LIMIT = 42;
                String label(int count) {
                    if (count > LIMIT) {
                        return "many";
                    }
                    return "few";
                }
            }
            """;
    private static final String URL_PREFIX = "jdbc:sqlite:";
    private static final String FACTS = "target/m3/facts.sqlite";
    private static final String TOOLCHAIN = "javac-21-contract";
    private static final String FIRST = "generation-1";
    private static final String SECOND = "generation-2";
    private static final String SET = "1";
    private static final String CLEAR = "0";
    /** One ordered digest of every lane of every node; compared verbatim against the index. */
    private static final String LANE_SQL = "SELECT group_concat(node_id||':'||kind||':'||parent||':'||start||':'||\"end\""
            + "||':'||flags||':'||first_child||':'||next_sibling||':'||child_count||':'||has_details||':'||start_line"
            + "||':'||end_line||':'||hex(coalesce(name_utf16,X''))||':'||literal_kind||':'||hex(coalesce(literal_utf16,X''))"
            + "||':'||modifier_bits||':'||enclosing_type||':'||enclosing_executable||':'||coalesce(structural_sha256,'')"
            + "||':'||hex(kind_utf16), '|' ORDER BY node_id) FROM main.m3_ast_nodes WHERE generation=?";
    private static final int META_ID = 1;
    private static final long ONE_FILE = 1;
    private static final long ONE_GENERATION = 1;
    private static final long TWO_GENERATIONS = 2;
    private static final int GROUPS = 5;
    private static int checks;

    private M3FactsWriterContract() { }

    /** Runs every group against fresh module directories under {@code m3.parser.test.work}. */
    public static void main(String[] arguments) throws Exception {
        Path work = work();
        M3ASTIndex index = index(TEXT);
        missingDriver(work.resolve("missing-driver"), index);
        Path module = work.resolve("module");
        M3FactsWriter.Result first = M3FactsWriter.write(module, List.of(index), context(FIRST, index),
                M3FactsWriter.Limits.DEFAULTS);
        published(module, index, first);
        repeatedGeneration(module, index, first);
        appendedGeneration(module, index, first);
        unwritable(work.resolve("unwritable"), index);
        System.out.println("M3_FACTS_WRITER_PASS checks=" + checks + " groups=" + GROUPS);
    }

    /** Without a registered SQLite driver the writer fails through its declared SQLException and publishes nothing. */
    private static void missingDriver(Path module, M3ASTIndex index) throws Exception {
        Driver driver = DriverManager.getDriver(URL_PREFIX);
        DriverManager.deregisterDriver(driver);
        try {
            SQLException refused = expect(SQLException.class, () -> M3FactsWriter.write(module, List.of(index),
                    context(FIRST, index), M3FactsWriter.Limits.DEFAULTS));
            check(refused.getMessage().contains("No suitable driver"), "DriverManager refusal surfaces unchanged: " + refused.getMessage());
            check(!Files.exists(module.resolve(FACTS)), "no facts file is created without a driver");
        } finally {
            DriverManager.registerDriver(driver);
        }
    }

    /** Schema marker, counts against the result, committed publication and exact lane equality. */
    private static void published(Path module, M3ASTIndex index, M3FactsWriter.Result result) throws SQLException {
        check(result.committed(), "the path overload owns and commits its transaction");
        check(result.files() == ONE_FILE && result.nodes() == index.nodeCount(), "the result counts the one index");
        try (Connection db = open(module)) {
            check(query(db, "SELECT group_concat(id||':'||schema, '|') FROM main.m3_ast_meta")
                    .equals(META_ID + ":" + M3FactsWriter.SCHEMA), "schema marker recorded once in m3_ast_meta");
            check(count(db, "SELECT COUNT(*) FROM main.m3_ast_generations") == ONE_GENERATION, "one generation");
            counts(db, FIRST, result);
            check(query(db, LANE_SQL, FIRST).equals(lanes(index)), "every lane of every node round-trips exactly");
        }
    }

    /** Rows and recorded totals of one generation equal the returned result. */
    private static void counts(Connection db, String generation, M3FactsWriter.Result result) throws SQLException {
        check(count(db, "SELECT COUNT(*) FROM main.m3_ast_files WHERE generation=?", generation) == result.files(),
                "file rows equal Result.files");
        check(count(db, "SELECT COUNT(*) FROM main.m3_ast_nodes WHERE generation=?", generation) == result.nodes(),
                "node rows equal Result.nodes");
        check(count(db, "SELECT file_count FROM main.m3_ast_generations WHERE generation=?", generation) == result.files(),
                "generation file_count equals Result.files");
        check(count(db, "SELECT node_count FROM main.m3_ast_generations WHERE generation=?", generation) == result.nodes(),
                "generation node_count equals Result.nodes");
    }

    /** A repeated generation is refused whole by the generation primary key; the first generation stays intact. */
    private static void repeatedGeneration(Path module, M3ASTIndex index, M3FactsWriter.Result first) throws Exception {
        expect(SQLException.class, () -> M3FactsWriter.write(module, List.of(index), context(FIRST, index),
                M3FactsWriter.Limits.DEFAULTS));
        try (Connection db = open(module)) {
            counts(db, FIRST, first);
            check(count(db, "SELECT COUNT(*) FROM main.m3_ast_nodes") == first.nodes(), "a refused generation leaves no row");
            check(count(db, "SELECT COUNT(*) FROM main.m3_ast_generations") == ONE_GENERATION, "a refused generation is absent");
        }
    }

    /** A new generation of the same index is appended beside the first one, with the same result and lanes. */
    private static void appendedGeneration(Path module, M3ASTIndex index, M3FactsWriter.Result first) throws Exception {
        M3FactsWriter.Result second = M3FactsWriter.write(module, List.of(index), context(SECOND, index),
                M3FactsWriter.Limits.DEFAULTS);
        check(second.equals(first), "an appended generation of the same index reports the same result");
        try (Connection db = open(module)) {
            counts(db, FIRST, first);
            counts(db, SECOND, second);
            check(count(db, "SELECT COUNT(*) FROM main.m3_ast_generations") == TWO_GENERATIONS, "two generations, one schema");
            check(query(db, LANE_SQL, SECOND).equals(lanes(index)), "the appended generation round-trips the same lanes");
        }
    }

    /** The writer's own admission refuses a facts path that is not a regular file; a blocked parent fails as IOException. */
    private static void unwritable(Path root, M3ASTIndex index) throws Exception {
        Path occupied = root.resolve("occupied");
        Files.createDirectories(occupied.resolve(FACTS));
        IOException own = expect(IOException.class, () -> M3FactsWriter.write(occupied, List.of(index),
                context(FIRST, index), M3FactsWriter.Limits.DEFAULTS));
        check(own.getMessage().startsWith("facts path is not a regular file"), "writer's own refusal: " + own.getMessage());
        Path blocked = root.resolve("blocked");
        Files.createDirectories(blocked);
        Files.writeString(blocked.resolve("target"), "not a directory", StandardCharsets.UTF_8);
        expect(IOException.class, () -> M3FactsWriter.write(blocked, List.of(index), context(FIRST, index),
                M3FactsWriter.Limits.DEFAULTS));
        check(!Files.exists(blocked.resolve(FACTS)), "a regular file where target/ must be created is refused");
    }

    /** The same lane digest as {@link #LANE_SQL}, computed from the index with the writer's own structural budget. */
    private static String lanes(M3ASTIndex index) {
        M3ASTIndex.StructuralHashes hashes = index.structuralHashes(index.nodeCount(),
                M3FactsWriter.Limits.DEFAULTS.maxStructuralTextUnits());
        return IntStream.range(0, index.nodeCount()).mapToObj(row -> lane(index, hashes, row))
                .collect(Collectors.joining("|"));
    }

    private static String lane(M3ASTIndex index, M3ASTIndex.StructuralHashes hashes, int row) {
        return String.join(":", Integer.toString(row), index.kind(row), Integer.toString(index.parent(row)),
                Integer.toString(index.start(row)), Integer.toString(index.end(row)), Integer.toString(index.flags(row)),
                Integer.toString(index.firstChild(row)), Integer.toString(index.nextSibling(row)),
                Integer.toString(index.childCount(row)), index.hasDetails(row) ? SET : CLEAR,
                Integer.toString(index.startLine(row)), Integer.toString(index.endLine(row)),
                hex(index.name(row).orElse("")), index.literalKind(row).name(), hex(index.literalValue(row).orElse("")),
                Long.toString(index.modifierBits(row)), Integer.toString(index.enclosingType(row)),
                Integer.toString(index.enclosingExecutable(row)), hashes.sha256(row), hex(index.kind(row)));
    }

    /** SQLite's {@code hex()} rendering of the writer's exact UTF-16BE lane; the fixture has no unpaired surrogate. */
    private static String hex(String text) {
        return HexFormat.of().withUpperCase().formatHex(text.getBytes(StandardCharsets.UTF_16BE));
    }

    /** The source seal doubles as the optional configuration seal, so no nullable column stays unexercised. */
    private static M3FactsWriter.Context context(String generation, M3ASTIndex index) {
        return new M3FactsWriter.Context(generation, TOOLCHAIN, index.source().utf16Sha256());
    }

    private static Connection open(Path module) throws SQLException {
        return DriverManager.getConnection(URL_PREFIX + module.resolve(FACTS).toAbsolutePath().toUri().toASCIIString());
    }

    private static String query(Connection db, String sql) throws SQLException {
        try (Statement statement = db.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            if (!rows.next()) throw new AssertionError("no row for " + sql);
            return Objects.toString(rows.getString(1), "");
        }
    }

    private static String query(Connection db, String sql, String generation) throws SQLException {
        try (PreparedStatement statement = db.prepareStatement(sql)) {
            statement.setString(1, generation);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) throw new AssertionError("no row for " + sql);
                return Objects.toString(rows.getString(1), "");
            }
        }
    }

    private static long count(Connection db, String sql) throws SQLException {
        return Long.parseLong(query(db, sql));
    }

    private static long count(Connection db, String sql, String generation) throws SQLException {
        return Long.parseLong(query(db, sql, generation));
    }

    private static Path work() throws IOException {
        String configured = System.getProperty("m3.parser.test.work");
        if (configured == null) throw new IllegalStateException("explicit m3.parser.test.work is required");
        return Files.createDirectories(Path.of(configured));
    }

    private static final class Source extends SimpleJavaFileObject {
        private final String text;
        Source(String text) { super(URI.create("memory:/Demo.java"), Kind.SOURCE); this.text = text; }
        @Override public CharSequence getCharContent(boolean ignored) { return text; }
    }

    /** One real javac parse; the JAVAC-domain index comes from the public ingress, as in the sibling contracts. */
    private static M3ASTIndex index(String text) throws Exception {
        Source input = new Source(text);
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (StandardJavaFileManager files = ToolProvider.getSystemJavaCompiler().getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(null, files, diagnostics,
                    List.of("-proc:none", "-source", "21", "-target", "21"), null, List.of(input));
            CompilationUnitTree tree = task.parse().iterator().next();
            SourcePositions positions = Trees.instance(task).getSourcePositions();
            return M3ASTIngress.javac(M3File.of(input.toUri(), text), tree, positions, M3ASTIngress.Limits.defaults());
        }
    }

    @FunctionalInterface private interface Checked { void run() throws Exception; }

    private static <T extends Throwable> T expect(Class<T> type, Checked action) throws Exception {
        try {
            action.run();
        } catch (Throwable thrown) {
            if (type.isInstance(thrown)) { checks++; return type.cast(thrown); }
            throw new AssertionError("expected " + type.getSimpleName() + " but got " + thrown, thrown);
        }
        throw new AssertionError("expected " + type.getSimpleName());
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
