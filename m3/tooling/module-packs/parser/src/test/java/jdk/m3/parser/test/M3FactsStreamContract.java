// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser.test;

import static jdk.m3.parser.test.M3FactsFixtures.A;
import static jdk.m3.parser.test.M3FactsFixtures.ABSENT;
import static jdk.m3.parser.test.M3FactsFixtures.B;
import static jdk.m3.parser.test.M3FactsFixtures.BUILDER_BUDGET;
import static jdk.m3.parser.test.M3FactsFixtures.CHILD_END;
import static jdk.m3.parser.test.M3FactsFixtures.CHILD_START;
import static jdk.m3.parser.test.M3FactsFixtures.DEFAULTS;
import static jdk.m3.parser.test.M3FactsFixtures.GENERATIONS;
import static jdk.m3.parser.test.M3FactsFixtures.QUOTED;
import static jdk.m3.parser.test.M3FactsFixtures.TEXT;
import static jdk.m3.parser.test.M3FactsFixtures.context;
import static jdk.m3.parser.test.M3FactsFixtures.count;
import static jdk.m3.parser.test.M3FactsFixtures.database;
import static jdk.m3.parser.test.M3FactsFixtures.query;
import static jdk.m3.parser.test.M3FactsFixtures.table;
import static jdk.m3.parser.test.M3FactsFixtures.tiny;
import static jdk.m3.parser.test.M3FactsFixtures.work;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3FactsWriter;
import jdk.m3.parser.M3File;

/**
 * Facts stream contract: the Stream overloads of {@link M3FactsWriter} own the supplied stream. It is closed
 * exactly once and inside the owned transaction, before commit: on success, on a failing close (whose exception
 * is the one reported and whose generation is rolled back) and on setup failure, through the connection and the
 * module-path overloads. Only the close boundary is fault-injected, by a {@link Proxy} over a real stream;
 * traversal, transactions and SQLite run unchanged. A stream-owned generation of two domains publishes kinds,
 * names and a lone-surrogate literal losslessly. Companion of {@link M3FactsTransactionContract}.
 */
public final class M3FactsStreamContract {
    private static final String FILES_SQL = "SELECT group_concat(generation||':'||file_id||':'||uri||':'||domain||':'"
            + "||source_utf16_sha256||':'||coalesce(source_byte_sha256,'-')||':'||source_units||':'||node_count,"
            + " '|' ORDER BY file_id) FROM main.m3_ast_files";
    private static final String NODES_SQL = "SELECT group_concat(file_id||':'||node_id||':'||kind||':'||hex(kind_utf16)"
            + "||':'||hex(coalesce(name_utf16,X''))||':'||literal_kind||':'||hex(coalesce(literal_utf16,X''))"
            + "||':'||coalesce(structural_sha256,'-')||':'||coalesce(exact_sha256,'-'),"
            + " '|' ORDER BY file_id,node_id) FROM main.m3_ast_nodes";
    private static final String GENERATION = "gen'1";
    private static final String LONE_SURROGATE = String.valueOf(Character.MIN_HIGH_SURROGATE);
    private static final String UNIT_HEX = "%04X";
    private static final String CLOSE = "close";
    private static final String SENTINEL = "provider close sentinel";
    private static final int GROUPS = 4;
    private static int checks;

    private M3FactsStreamContract() { }

    /** Runs every group on fresh in-memory connections; the module-path group writes under {@code m3.parser.test.work}. */
    public static void main(String[] arguments) throws Exception {
        Path work = work();
        lanes();
        closeBeforeCommit();
        closedOnce();
        pathOwner(work.resolve("close-once-module"));
        System.out.println("M3_FACTS_STREAM_PASS checks=" + checks + " groups=" + GROUPS);
    }

    /** A stream-owned generation of two domains: stable file identities and lossless text lanes, exact hashing opt-in. */
    private static void lanes() throws Exception {
        List<M3ASTIndex> indexes = List.of(tiny(A), surrogate(B));
        try (Connection db = database()) {
            M3FactsWriter.Result result = M3FactsWriter.write(db, indexes.stream(), context(GENERATION), DEFAULTS);
            check(result.committed() && result.files() == indexes.size(), "one owned generation of two files");
            check(query(db, FILES_SQL).equals(files(indexes)), "stable file ordinals, URIs, domains and source identities");
            check(query(db, NODES_SQL).equals(nodes(indexes)), "lossless kind, name and lone-surrogate literal lanes");
        }
    }

    /** A second domain whose child carries a quoted name and a lone high surrogate that JDBC text would replace. */
    private static M3ASTIndex surrogate(String uri) {
        M3ASTIndex.Builder builder = M3ASTIndex.builder(M3File.of(URI.create(uri), TEXT),
                M3ASTIndex.Domain.JACKSON_JSON, BUILDER_BUDGET);
        int root = builder.add(-1, "object", 0, TEXT.length(), M3ASTIndex.EXACT_RANGE);
        int child = builder.add(root, "string", CHILD_START, CHILD_END, M3ASTIndex.EXACT_RANGE);
        builder.details(child, -1, -1, QUOTED, M3ASTIndex.LiteralKind.STRING, LONE_SURROGATE, 0L, -1, -1);
        return builder.seal();
    }

    private static String files(List<M3ASTIndex> indexes) {
        return IntStream.range(0, indexes.size()).mapToObj(file -> file(file, indexes.get(file)))
                .collect(Collectors.joining("|"));
    }

    private static String file(int file, M3ASTIndex index) {
        return String.join(":", GENERATION, Integer.toString(file), index.source().uri().toASCIIString(),
                index.domain().name(), index.source().utf16Sha256(), index.source().byteSha256().orElse(ABSENT),
                Integer.toString(index.source().length()), Integer.toString(index.nodeCount()));
    }

    private static String nodes(List<M3ASTIndex> indexes) {
        return IntStream.range(0, indexes.size()).boxed().flatMap(file -> nodes(file, indexes.get(file)))
                .collect(Collectors.joining("|"));
    }

    /** The NODES_SQL rendering of one index, structural hashes under the writer's default budget. */
    private static Stream<String> nodes(int file, M3ASTIndex index) {
        M3ASTIndex.StructuralHashes hashes = index.structuralHashes(index.nodeCount(), DEFAULTS.maxStructuralTextUnits());
        return IntStream.range(0, index.nodeCount()).mapToObj(row -> String.join(":", Integer.toString(file),
                Integer.toString(row), index.kind(row), units(index.kind(row)), units(index.name(row).orElse("")),
                index.literalKind(row).name(), units(index.literalValue(row).orElse("")), hashes.sha256(row), ABSENT));
    }

    /** SQLite hex() of the writer's UTF-16BE blob, one code unit at a time, so a lone surrogate stays itself. */
    private static String units(String text) {
        return text.chars().mapToObj(unit -> String.format(UNIT_HEX, unit)).collect(Collectors.joining());
    }

    /** A failing provider close is reported unchanged and rolls back; a clean close runs before the owned commit. */
    private static void closeBeforeCommit() throws Exception {
        try (Connection db = database()) {
            AtomicBoolean closed = new AtomicBoolean();
            IllegalStateException failure = expect(IllegalStateException.class,
                    () -> M3FactsWriter.write(db, failing(closed), context("close-fail"), DEFAULTS));
            check(SENTINEL.equals(failure.getMessage()) && closed.get(), "the provider close failure is preserved");
            check(!table(db, GENERATIONS), "a close failure cannot publish its generation");
            AtomicBoolean pending = new AtomicBoolean();
            M3FactsWriter.write(db, Stream.of(tiny(A)).onClose(() -> pending.set(uncommitted(db))), context("closed"), DEFAULTS);
            check(pending.get() && count(db, GENERATIONS) == 1 && db.getAutoCommit(), "the stream closes before the commit");
        }
    }

    /** A real stream whose provider cleanup records itself and then fails. */
    private static Stream<M3ASTIndex> failing(AtomicBoolean closed) {
        return Stream.of(tiny(A)).onClose(() -> {
            closed.set(true);
            throw new IllegalStateException(SENTINEL);
        });
    }

    /** True while the writer's owned transaction is still open on the borrowed connection. */
    private static boolean uncommitted(Connection db) {
        try {
            return !db.getAutoCommit();
        } catch (SQLException failure) {
            throw new IllegalStateException(failure);
        }
    }

    /** Connection overload: one close on success, the first close failure kept and never retried, one on setup failure. */
    private static void closedOnce() throws Exception {
        try (Connection db = database()) {
            AtomicInteger closes = new AtomicInteger();
            M3FactsWriter.write(db, counted(closes, Optional.empty()), context("once"), DEFAULTS);
            check(closes.getAndSet(0) == 1 && count(db, GENERATIONS) == 1, "a successful stream closes once before commit");
            RuntimeException sentinel = new IllegalStateException("first close sentinel");
            check(expect(IllegalStateException.class, () -> M3FactsWriter.write(db, counted(closes, Optional.of(sentinel)),
                    context("once-failure"), DEFAULTS)) == sentinel, "the first close failure keeps its identity");
            check(closes.getAndSet(0) == 1 && count(db, GENERATIONS) == 1, "a failed close rolls back and is never retried");
            expect(NullPointerException.class, () -> M3FactsWriter.write(db, counted(closes, Optional.empty()), null, DEFAULTS));
            check(closes.get() == 1, "a setup failure closes the owned stream once");
        }
    }

    /** The module-path overload shares the single stream owner: one close on success, one on setup failure. */
    private static void pathOwner(Path module) throws Exception {
        AtomicInteger closes = new AtomicInteger();
        M3FactsWriter.Result result = M3FactsWriter.write(module, counted(closes, Optional.empty()), context("path-once"),
                DEFAULTS);
        check(result.committed() && closes.getAndSet(0) == 1, "the path overload commits and closes once");
        expect(NullPointerException.class, () -> M3FactsWriter.write((Path) null, counted(closes, Optional.empty()),
                context("path-setup"), DEFAULTS));
        check(closes.get() == 1, "a path setup failure closes once");
    }

    /** A real stream behind a Proxy that counts close calls and can fail them; every other call is forwarded. */
    @SuppressWarnings("unchecked")
    private static Stream<M3ASTIndex> counted(AtomicInteger closes, Optional<RuntimeException> failure) {
        Stream<M3ASTIndex> actual = Stream.of(tiny("memory:///close-once"));
        return (Stream<M3ASTIndex>) Proxy.newProxyInstance(M3FactsStreamContract.class.getClassLoader(),
                new Class<?>[] {Stream.class}, (proxy, method, arguments) -> CLOSE.equals(method.getName())
                        ? close(actual, closes, failure, method) : forward(actual, method, arguments));
    }

    /** The real close runs first; a configured failure follows it, and a second close is itself a failure. */
    private static Object close(Stream<M3ASTIndex> actual, AtomicInteger closes, Optional<RuntimeException> failure,
                                Method method) throws Throwable {
        int calls = closes.incrementAndGet();
        Object value = forward(actual, method, new Object[0]);
        if (failure.isPresent()) throw failure.get();
        if (calls > 1) throw new IllegalStateException("second close after publication");
        return value;
    }

    private static Object forward(Object target, Method method, Object[] arguments) throws Throwable {
        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException failure) {
            throw failure.getCause();
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
