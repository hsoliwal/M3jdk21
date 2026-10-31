// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser.test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Optional;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3FactsWriter;
import jdk.m3.parser.M3File;

/**
 * Shared fixtures of the facts transaction and stream contracts: a two-row detached index, a caller context,
 * a fresh in-memory SQLite connection ({@code jdbc:sqlite::memory:}), the consumer work directory, and
 * single-value SQL readback. No assertion state lives here; each contract counts its own checks.
 */
final class M3FactsFixtures {
    static final M3FactsWriter.Limits DEFAULTS = M3FactsWriter.Limits.DEFAULTS;
    static final String A = "memory:///a";
    static final String B = "memory:///b";
    static final String TEXT = "abcdef";
    static final String QUOTED = "ROOT'" + '"';
    static final String ABSENT = "-";
    static final String GENERATIONS = "main.m3_ast_generations";
    static final String FILES = "main.m3_ast_files";
    static final String NODES = "main.m3_ast_nodes";
    static final int CHILD_START = 1;
    static final int CHILD_END = 3;
    static final int BUILDER_BUDGET = 8;
    private static final String TABLE_SQL = "SELECT COUNT(*) FROM main.sqlite_master WHERE type='table' AND 'main.'||name=?";
    private static final String MEMORY = "jdbc:sqlite::memory:";
    private static final String TOOLCHAIN = "fixture-javac21";
    private static final String CHILD_KIND = "child";

    private M3FactsFixtures() { }

    /** Two exact rows over {@link #TEXT}: a root whose kind carries both quote characters, and one child. */
    static M3ASTIndex tiny(String uri) {
        M3ASTIndex.Builder builder = M3ASTIndex.builder(M3File.of(URI.create(uri), TEXT),
                M3ASTIndex.Domain.GRADLE_GROOVY, BUILDER_BUDGET);
        int root = builder.add(-1, QUOTED, 0, TEXT.length(), M3ASTIndex.EXACT_RANGE | M3ASTIndex.SYNTAX_ONLY);
        builder.add(root, CHILD_KIND, CHILD_START, CHILD_END, M3ASTIndex.EXACT_RANGE);
        return builder.seal();
    }

    /** No configuration seal: the nullable column stays exercised (the facts-writer contract supplies one). */
    static M3FactsWriter.Context context(String generation) {
        return new M3FactsWriter.Context(generation, TOOLCHAIN, null);
    }

    static Connection database() throws SQLException {
        return DriverManager.getConnection(MEMORY);
    }

    static Path work() throws IOException {
        return Files.createDirectories(Path.of(Optional.ofNullable(System.getProperty("m3.parser.test.work"))
                .orElseThrow(() -> new IllegalStateException("explicit m3.parser.test.work is required"))));
    }

    /** The first column of the first row; SQL NULL reads as the empty string. */
    static String query(Connection db, String sql) throws SQLException {
        try (Statement statement = db.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            if (!rows.next()) throw new AssertionError("no row for " + sql);
            return Objects.toString(rows.getString(1), "");
        }
    }

    static long count(Connection db, String table) throws SQLException {
        return Long.parseLong(query(db, "SELECT COUNT(*) FROM " + table));
    }

    /** Whether the main-schema table named {@code main.<name>} exists. */
    static boolean table(Connection db, String table) throws SQLException {
        try (PreparedStatement statement = db.prepareStatement(TABLE_SQL)) {
            statement.setString(1, table);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() && rows.getLong(1) != 0;
            }
        }
    }

    static void execute(Connection db, String sql) throws SQLException {
        try (Statement statement = db.createStatement()) {
            statement.execute(sql);
        }
    }
}
