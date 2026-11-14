// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser.test;

import static jdk.m3.parser.test.M3FactsFixtures.A;
import static jdk.m3.parser.test.M3FactsFixtures.B;
import static jdk.m3.parser.test.M3FactsFixtures.CHILD_END;
import static jdk.m3.parser.test.M3FactsFixtures.CHILD_START;
import static jdk.m3.parser.test.M3FactsFixtures.DEFAULTS;
import static jdk.m3.parser.test.M3FactsFixtures.FILES;
import static jdk.m3.parser.test.M3FactsFixtures.GENERATIONS;
import static jdk.m3.parser.test.M3FactsFixtures.NODES;
import static jdk.m3.parser.test.M3FactsFixtures.TEXT;
import static jdk.m3.parser.test.M3FactsFixtures.context;
import static jdk.m3.parser.test.M3FactsFixtures.count;
import static jdk.m3.parser.test.M3FactsFixtures.database;
import static jdk.m3.parser.test.M3FactsFixtures.execute;
import static jdk.m3.parser.test.M3FactsFixtures.query;
import static jdk.m3.parser.test.M3FactsFixtures.table;
import static jdk.m3.parser.test.M3FactsFixtures.tiny;
import static jdk.m3.parser.test.M3FactsFixtures.work;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3FactsWriter;

/**
 * Facts transaction contract: the transaction-level checks of the first one-pass facts contract, recovered
 * against the current {@link M3FactsWriter} on real in-memory SQLite connections ({@code jdbc:sqlite::memory:}).
 * An autocommit connection gets one owned transaction and is handed back usable; a caller transaction gets one
 * savepoint and keeps publication; refused generations, budget refusals, foreign tables, foreign triggers and
 * TEMP shadows publish nothing and leave unrelated rows intact; exact range hashes obey their cumulative budget;
 * the module-path overload publishes a real file; a module directory reached through a symbolic link is refused
 * (reported SKIPPED, not PASS, when a Windows process lacks the symbolic-link privilege). Stream ownership and
 * lossless lanes are proven by {@link M3FactsStreamContract}.
 */
public final class M3FactsTransactionContract {
    private static final String META = "main.m3_ast_meta";
    private static final String EXTERNAL = "main.external";
    private static final String EXACT_SQL = "SELECT group_concat(node_id||':'||exact_sha256||':'||coalesce(structural_sha256,'-'),"
            + " '|' ORDER BY node_id) FROM main.m3_ast_nodes";
    private static final String TEMP_TRIGGER = "CREATE TEMP TRIGGER silently_skip BEFORE INSERT ON main.M3_AST_NODES"
            + " BEGIN SELECT RAISE(IGNORE); END";
    private static final String URL_PREFIX = "jdbc:sqlite:";
    private static final String FACTS = "target/m3/facts.sqlite";
    private static final String DRIVER_FAMILY = "3.45";
    private static final String SYMLINK = "M3_FACTS_TRANSACTION_SYMLINK_REFUSAL";
    private static final boolean WINDOWS = System.getProperty("os.name", "").startsWith("Windows");
    private static final int LAZY_SUPPLY = 50;
    private static final long TINY_NODES = 2;
    /** Root and child are both exact, so the cumulative exact-span work is the whole text plus the child span. */
    private static final int EXACT_TOTAL = TEXT.length() + CHILD_END - CHILD_START;
    private static final M3FactsWriter.Limits ONE_FILE = new M3FactsWriter.Limits(1, 20, 1, 1024, 0);
    private static final M3FactsWriter.Limits ONE_NODE = new M3FactsWriter.Limits(4, 1, 2, 1024, 0);
    private static final M3FactsWriter.Limits ROW_TEXT = new M3FactsWriter.Limits(1, 10, 8, 1024, 0, 16);
    private static final M3FactsWriter.Limits EXACT = new M3FactsWriter.Limits(1, 10, 1, 0, EXACT_TOTAL);
    private static final M3FactsWriter.Limits EXACT_SHORT = new M3FactsWriter.Limits(1, 10, 1, 0, EXACT_TOTAL - 1);
    private static final int GROUPS = 9;
    private static int checks;

    private M3FactsTransactionContract() { }

    /** Runs every group on fresh in-memory connections; the module-path groups write under {@code m3.parser.test.work}. */
    public static void main(String[] arguments) throws Exception {
        Path work = work();
        owned();
        callerTransaction();
        failedGenerations();
        budgets();
        foreignTrigger();
        temporaryShadows();
        exactBudget();
        modulePath(work.resolve("module"));
        symlink(work.resolve("linked-module"), work.resolve("module"));
        System.out.println("M3_FACTS_TRANSACTION_PASS checks=" + checks + " groups=" + GROUPS);
    }

    /** An autocommit connection gets one owned, committed transaction and is handed back open and in autocommit. */
    private static void owned() throws Exception {
        List<M3ASTIndex> indexes = List.of(tiny(A), tiny(B));
        try (Connection db = database()) {
            M3FactsWriter.Result result = M3FactsWriter.write(db, indexes, context("gen'1"), DEFAULTS);
            check(result.committed() && result.files() == indexes.size(), "owned transaction result");
            check(db.getAutoCommit() && !db.isClosed(), "borrowed connection remains usable");
            check(result.nodes() == indexes.stream().mapToLong(M3ASTIndex::nodeCount).sum(), "node total");
            check(count(db, FILES) == result.files() && count(db, NODES) == result.nodes(), "published rows equal the result");
        }
    }

    /** A caller transaction gets one savepoint: success stays uncommitted and the caller's rollback removes the facts. */
    private static void callerTransaction() throws Exception {
        try (Connection db = database()) {
            execute(db, "CREATE TABLE external(value INTEGER)");
            db.setAutoCommit(false);
            execute(db, "INSERT INTO external VALUES(7)");
            M3FactsWriter.Result result = M3FactsWriter.write(db, List.of(tiny(A)), context("caller"), DEFAULTS);
            check(!result.committed() && !db.getAutoCommit(), "the caller owns publication");
            check(count(db, EXTERNAL) == 1 && count(db, GENERATIONS) == 1, "facts are visible inside the caller transaction");
            db.rollback();
            check(count(db, EXTERNAL) == 0 && !table(db, GENERATIONS), "caller rollback includes the facts and their schema");
            savepointFailure(db);
            db.commit();
        }
    }

    /** A failing write inside a caller transaction rolls back to its savepoint only. */
    private static void savepointFailure(Connection db) throws Exception {
        execute(db, "INSERT INTO external VALUES(8)");
        expect(SQLException.class, () -> M3FactsWriter.write(db, List.of(tiny(A), tiny(A)), context("failure"), DEFAULTS));
        check(!db.getAutoCommit() && count(db, EXTERNAL) == 1, "savepoint rollback preserves caller work");
        check(!table(db, GENERATIONS), "a failed savepoint adds no schema");
    }

    /** Refused generations publish nothing, unrelated rows survive, a failed traversal restores autocommit. */
    private static void failedGenerations() throws Exception {
        try (Connection db = database()) {
            execute(db, "CREATE TABLE external(value TEXT)");
            execute(db, "INSERT INTO external VALUES('preserve')");
            M3FactsWriter.write(db, List.of(tiny(A)), context("first"), DEFAULTS);
            expect(SQLException.class, () -> M3FactsWriter.write(db, List.of(tiny(B)), context("first"), DEFAULTS));
            expect(SQLException.class, () -> M3FactsWriter.write(db, List.of(tiny(A), tiny(A)), context("second"), DEFAULTS));
            check(count(db, GENERATIONS) == 1 && count(db, FILES) == 1, "the existing generation stays intact");
            check(count(db, EXTERNAL) == 1, "unrelated data stays intact");
            expect(IllegalStateException.class, () -> M3FactsWriter.write(db, broken(), context("broken"), DEFAULTS));
            check(count(db, GENERATIONS) == 1 && db.getAutoCommit(), "a lazy traversal failure rolls back, autocommit restored");
        }
        foreignTable();
    }

    /** One index, then a provider failure on the next hasNext. */
    private static Iterable<M3ASTIndex> broken() {
        AtomicBoolean served = new AtomicBoolean();
        return () -> new Iterator<>() {
            @Override public boolean hasNext() {
                if (served.get()) throw new IllegalStateException("iterator sentinel");
                return true;
            }
            @Override public M3ASTIndex next() { served.set(true); return tiny("memory:///c"); }
        };
    }

    /** A same-named table the writer does not own is refused, unchanged. */
    private static void foreignTable() throws Exception {
        try (Connection db = database()) {
            execute(db, "CREATE TABLE m3_ast_meta(secret TEXT)");
            execute(db, "INSERT INTO m3_ast_meta VALUES('foreign')");
            expect(SQLException.class, () -> M3FactsWriter.write(db, List.of(tiny(A)), context("foreign"), DEFAULTS));
            check(count(db, META) == 1 && !table(db, NODES), "foreign ownership refused unchanged");
        }
    }

    /** maxFiles is checked before another index is requested; every budget refusal rolls back schema and rows. */
    private static void budgets() throws Exception {
        try (Connection db = database()) {
            AtomicInteger requested = new AtomicInteger();
            expect(IllegalArgumentException.class, () -> M3FactsWriter.write(db, lazy(requested), context("limit"), ONE_FILE));
            check(requested.get() == 1, "maxFiles checked before requesting another index");
            check(!table(db, GENERATIONS), "a file budget refusal rolls back schema and generation");
            expect(IllegalArgumentException.class, () -> M3FactsWriter.write(db, List.of(tiny(A)), context("nodes"), ONE_NODE));
            check(!table(db, NODES), "a node budget refusal rolls back");
            expect(IllegalArgumentException.class, () -> M3FactsWriter.write(db, List.of(tiny(A)), context("text"), ROW_TEXT));
            check(!table(db, NODES), "an oversized JDBC row is refused before publication");
        }
        expect(IllegalArgumentException.class, () -> new M3FactsWriter.Limits(0, 1, 1, 0, 0));
        expect(IllegalArgumentException.class, () -> new M3FactsWriter.Limits(1, 1, 0, 0, 0));
        expect(IllegalArgumentException.class, () -> new M3FactsWriter.Context("x", "jdk", "not-a-digest"));
    }

    /** A long supply whose next() is the only request that creates an index. */
    private static Iterable<M3ASTIndex> lazy(AtomicInteger requested) {
        return () -> new Iterator<>() {
            @Override public boolean hasNext() { return requested.get() < LAZY_SUPPLY; }
            @Override public M3ASTIndex next() { return tiny("memory:///" + requested.getAndIncrement()); }
        };
    }

    /** A foreign trigger on an owned table could silently drop rows; the writer refuses instead of publishing. */
    private static void foreignTrigger() throws Exception {
        try (Connection db = database()) {
            M3FactsWriter.write(db, List.of(tiny(A)), context("before-trigger"), DEFAULTS);
            execute(db, "CREATE TRIGGER silently_skip BEFORE INSERT ON m3_ast_nodes BEGIN SELECT RAISE(IGNORE); END");
            expect(SQLException.class, () -> M3FactsWriter.write(db, List.of(tiny(B)), context("after-trigger"), DEFAULTS));
            check(count(db, GENERATIONS) == 1 && count(db, NODES) == TINY_NODES, "a foreign trigger cannot publish missing facts");
        }
    }

    /** TEMP tables, views and triggers resolve before main; each shadow is refused, unrelated TEMP tables stay usable. */
    private static void temporaryShadows() throws Exception {
        shadow("CREATE TEMP TABLE M3_AST_NODES AS SELECT * FROM main.m3_ast_nodes WHERE 0");
        shadow("CREATE TEMP VIEW M3_AST_NODES AS SELECT * FROM main.m3_ast_nodes");
        shadow(TEMP_TRIGGER);
        try (Connection db = database()) {
            execute(db, "CREATE TEMP TABLE unrelated(value TEXT)");
            execute(db, "INSERT INTO unrelated VALUES('preserve')");
            M3FactsWriter.write(db, List.of(tiny(A)), context("unrelated"), DEFAULTS);
            check(count(db, "temp.unrelated") == 1 && count(db, NODES) == TINY_NODES, "unrelated TEMP tables remain usable");
        }
    }

    private static void shadow(String sql) throws Exception {
        try (Connection db = database()) {
            M3FactsWriter.write(db, List.of(tiny(A)), context("first"), DEFAULTS);
            execute(db, sql);
            expect(SQLException.class, () -> M3FactsWriter.write(db, List.of(tiny(B)), context("diverted"), DEFAULTS));
            check(count(db, GENERATIONS) == 1 && count(db, NODES) == TINY_NODES, "a TEMP shadow cannot divert main facts");
        }
    }

    /** Exact range hashes are retained within the cumulative UTF-16 budget; one unit less refuses the generation. */
    private static void exactBudget() throws Exception {
        M3ASTIndex index = tiny(A);
        String expected = IntStream.range(0, index.nodeCount())
                .mapToObj(row -> row + ":" + index.sourceRangeSha256(row, EXACT_TOTAL) + ":-").collect(Collectors.joining("|"));
        try (Connection db = database()) {
            M3FactsWriter.write(db, List.of(index), context("exact"), EXACT);
            check(query(db, EXACT_SQL).equals(expected), "exact hash domain retained; structural hashing disabled");
            expect(IllegalArgumentException.class, () -> M3FactsWriter.write(db, List.of(index), context("over"), EXACT_SHORT));
            check(count(db, GENERATIONS) == 1, "the cumulative nested-span budget refuses a partial generation");
        }
    }

    /** The path overload publishes a real file, read back through JNI with the qualified driver family. */
    private static void modulePath(Path module) throws Exception {
        M3FactsWriter.Result result = M3FactsWriter.write(module, Stream.of(tiny("memory:///disk")), context("disk"), DEFAULTS);
        check(result.committed() && Files.isRegularFile(module.resolve(FACTS)), "requested module path");
        try (Connection db = DriverManager.getConnection(URL_PREFIX + module.resolve(FACTS).toUri().toASCIIString())) {
            check(count(db, FILES) == 1, "the SQLite file is readable through JNI");
            check(db.getMetaData().getDriverVersion().startsWith(DRIVER_FAMILY), "qualified SQLite driver family");
        }
    }

    /** A module directory reached through a symbolic link is refused by the writer before SQLite opens a file. */
    private static void symlink(Path link, Path module) throws Exception {
        Optional<String> unavailable = createLink(link, module);
        unavailable.ifPresent(reason -> System.out.println(SYMLINK + " SKIPPED reason=" + reason));
        if (unavailable.isEmpty()) {
            IOException refused = expect(IOException.class, () -> M3FactsWriter.write(link, List.of(tiny(A)),
                    context("link"), DEFAULTS));
            check(refused.getMessage().startsWith("symbolic-link facts path"), "own symlink refusal: " + refused.getMessage());
            System.out.println(SYMLINK + " PASS");
        }
    }

    /** Empty once the link exists; the platform reason when a Windows process lacks the symbolic-link privilege. */
    private static Optional<String> createLink(Path link, Path target) throws IOException {
        try {
            Files.createSymbolicLink(link, target.toAbsolutePath());
            return Optional.empty();
        } catch (FileSystemException failure) {
            if (!WINDOWS || failure.getClass() != FileSystemException.class) throw failure;
            return Optional.of(Objects.toString(failure.getReason(), failure.getMessage()).strip());
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
