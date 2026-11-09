/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Types;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Transactional SQLite projection of already detached AST lanes.
 *
 * <p>The existing compiler/parser remains the source of truth. No source is read,
 * parsed, printed, or retained by this writer. Input is consumed serially; only
 * one index, one optional flat structural-hash lane and a bounded JDBC batch are
 * retained at a time. A generation appears only after successful traversal and
 * transaction publication. This syntax schema is explicitly versioned; it is
 * not an assertion of compatibility with an external E48 schema.
 *
 * <p>The JDBC driver is supplied by the application or SDK image. The parser
 * module has an optional {@code java.sql} read edge and no driver dependency.
 * Existing Synexia ContentAddressSqlitePack and SqliteHashPackStore supplied the
 * prepared-binding/owned-schema/transaction patterns; their schemas remain
 * independent. First-party donor lineage is recorded in the recipe catalogue.
 */
public final class M3FactsWriter {
    public static final String SCHEMA = "M3_AST_FACTS_SQLITE_V1";

    /**
     * Caller-owned generation and toolchain identity. A configuration seal is
     * optional. When supplied, the caller must include compiler options,
     * class/module paths, processors, rules and their versions in its framing.
     * This writer emits syntax facts only; neither the source hash nor this
     * opaque seal certifies attributed semantic equivalence or a reusable cache.
     */
    public record Context(String generation, String toolchain, String configurationSha256) {
        public Context {
            text(generation, "generation", 1024);
            text(toolchain, "toolchain", 4096);
            if (configurationSha256 != null && !configurationSha256.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("configurationSha256 must be a lowercase SHA-256 or null");
            }
        }
    }

    /**
     * Operation-wide file/node limits, bounded JDBC batch, per-file structural
     * text-work limit, operation-wide exact-span UTF-16 work limit, and node-batch
     * text/UTF-16 payload limit. The latter bounds variable-sized rows as well as
     * their count; a single oversized row is refused before binding.
     * Zero disables the corresponding hash lane. Exact spans overlap, so their
     * total is checked before hashing each file; nested trees cannot silently
     * turn an apparently linear export into unbounded quadratic hashing.
     */
    public record Limits(int maxFiles, long maxNodes, int batchSize,
                         int maxStructuralTextUnits, long maxExactSourceUnits, int maxBatchTextUnits) {
        public static final Limits DEFAULTS = new Limits(100_000, 10_000_000L, 256, 16_777_216, 0, 1_048_576);
        public Limits(int maxFiles, long maxNodes, int batchSize, int maxStructuralTextUnits, long maxExactSourceUnits) {
            this(maxFiles, maxNodes, batchSize, maxStructuralTextUnits, maxExactSourceUnits, 1_048_576);
        }
        public Limits {
            if (maxFiles < 1 || maxFiles > 1_000_000 || maxNodes < 1 || maxNodes > 1_000_000_000L
                    || batchSize < 1 || batchSize > 8192 || maxStructuralTextUnits < 0
                    || maxExactSourceUnits < 0 || maxBatchTextUnits < 1 || maxBatchTextUnits > 67_108_864) {
                throw new IllegalArgumentException("invalid facts publication limits");
            }
        }
    }

    /** committed=false means the caller still owns the enclosing transaction. */
    public record Result(long files, long nodes, boolean committed) {}

    private static final Map<String, String> TABLES = tables();
    private static final String INSERT_NODE = "INSERT INTO main.m3_ast_nodes VALUES ("
            + "?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private M3FactsWriter() {}

    /** Write to {@code moduleDirectory/target/m3/facts.sqlite}; input is borrowed. */
    public static Result write(Path moduleDirectory, Iterable<? extends M3ASTIndex> indexes,
                               Context context, Limits limits) throws IOException, SQLException {
        Objects.requireNonNull(indexes, "indexes");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(limits, "limits");
        try (Connection connection = open(moduleDirectory)) {
            return write(connection, indexes, context, limits);
        }
    }

    /**
     * Write to the module facts path and own the supplied stream. Stream close,
     * including provider cleanup, happens before commit; a close failure rolls
     * back this generation. The stream is also closed on setup failure.
     */
    public static Result write(Path moduleDirectory, Stream<? extends M3ASTIndex> indexes,
                               Context context, Limits limits) throws IOException, SQLException {
        StreamOwner owner = new StreamOwner(indexes);
        Throwable primary = null;
        try {
            Objects.requireNonNull(context, "context");
            Objects.requireNonNull(limits, "limits");
            try (Connection connection = open(moduleDirectory)) {
                return writeOwned(connection, owner, context, limits);
            }
        } catch (IOException | SQLException | RuntimeException | Error failure) {
            primary = failure;
            throw failure;
        } finally {
            owner.close(primary);
        }
    }

    /**
     * Borrow a SQLite connection and iterable. An autocommit connection gets one
     * owned transaction; an existing caller transaction gets one savepoint and
     * remains uncommitted. Neither connection nor iterable is closed. Iterable
     * owners must ensure that successful exhaustion includes their cleanup; use
     * the Stream overload when close is part of the success boundary.
     */
    public static Result write(Connection connection, Iterable<? extends M3ASTIndex> indexes,
                               Context context, Limits limits) throws SQLException {
        Objects.requireNonNull(indexes, "indexes");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(limits, "limits");
        return transaction(connection, () -> load(connection, indexes.iterator(), context, limits));
    }

    /** Borrow the connection; own and close the stream before publishing its generation. */
    public static Result write(Connection connection, Stream<? extends M3ASTIndex> indexes,
                               Context context, Limits limits) throws SQLException {
        StreamOwner owner = new StreamOwner(indexes);
        Throwable primary = null;
        try {
            Objects.requireNonNull(context, "context");
            Objects.requireNonNull(limits, "limits");
            return writeOwned(connection, owner, context, limits);
        } catch (SQLException | RuntimeException | Error failure) {
            primary = failure;
            throw failure;
        } finally {
            owner.close(primary);
        }
    }

    private static Result writeOwned(Connection connection, StreamOwner owner,
                                     Context context, Limits limits) throws SQLException {
        return transaction(connection, () -> {
            Throwable primary = null;
            try {
                return load(connection, owner.stream.iterator(), context, limits);
            } catch (SQLException | RuntimeException | Error failure) {
                primary = failure;
                throw failure;
            } finally {
                owner.close(primary);
            }
        });
    }

    /** One operation owns the external resource, including failed setup. */
    private static final class StreamOwner {
        private final Stream<? extends M3ASTIndex> stream;
        private boolean closed;

        StreamOwner(Stream<? extends M3ASTIndex> stream) {
            this.stream = Objects.requireNonNull(stream, "indexes");
        }

        void close(Throwable primary) {
            if (closed) return;
            closed = true; // A failing close is still one consumed ownership action.
            try {
                stream.close();
            } catch (RuntimeException | Error failure) {
                if (primary == null) throw failure;
                if (failure != primary) primary.addSuppressed(failure);
            }
        }
    }

    private static Connection open(Path moduleDirectory) throws IOException, SQLException {
        Path path = Objects.requireNonNull(moduleDirectory, "moduleDirectory").toAbsolutePath().normalize()
                .resolve("target/m3/facts.sqlite");
        safe(path);
        Files.createDirectories(path.getParent());
        safe(path);
        return DriverManager.getConnection("jdbc:sqlite:" + path.toUri().toASCIIString());
    }

    private static void safe(Path path) throws IOException {
        for (Path entry = path; entry != null; entry = entry.getParent()) {
            if (Files.isSymbolicLink(entry)) throw new IOException("symbolic-link facts path: " + entry);
        }
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS) && !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("facts path is not a regular file: " + path);
        }
        for (String suffix : new String[] {"-journal", "-wal", "-shm"}) {
            if (Files.isSymbolicLink(path.resolveSibling(path.getFileName() + suffix))) {
                throw new IOException("symbolic-link SQLite sidecar");
            }
        }
    }

    @FunctionalInterface private interface Work { Result run() throws SQLException; }

    private static Result transaction(Connection connection, Work work) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        if (!"SQLite".equals(connection.getMetaData().getDatabaseProductName())) {
            throw new SQLException("SQLite connection required for " + SCHEMA);
        }
        boolean owned = connection.getAutoCommit();
        Savepoint savepoint = null;
        if (owned) connection.setAutoCommit(false);
        else savepoint = connection.setSavepoint();
        boolean settled = false;
        Throwable primary = null;
        try {
            Result result = work.run();
            if (owned) connection.commit();
            else connection.releaseSavepoint(savepoint);
            settled = true;
            return new Result(result.files(), result.nodes(), owned);
        } catch (SQLException | RuntimeException | Error failure) {
            primary = failure;
            try {
                if (owned) connection.rollback();
                else connection.rollback(savepoint);
                settled = true;
                if (!owned) connection.releaseSavepoint(savepoint);
            } catch (SQLException | RuntimeException | Error rollbackFailure) {
                if (rollbackFailure != failure) failure.addSuppressed(rollbackFailure);
            }
            throw failure;
        } finally {
            // setAutoCommit(true) could commit pending work after a failed rollback.
            // Refuse that implicit publication; leave an unsettled borrowed connection
            // in its failed transaction and report the rollback failure to its owner.
            if (owned && settled) {
                try { connection.setAutoCommit(true); }
                catch (SQLException | RuntimeException | Error restoreFailure) {
                    if (primary == null) throw restoreFailure;
                    if (restoreFailure != primary) primary.addSuppressed(restoreFailure);
                }
            }
        }
    }

    private static Result load(Connection connection, Iterator<? extends M3ASTIndex> input,
                               Context context, Limits limits) throws SQLException {
        Objects.requireNonNull(input, "indexes.iterator()");
        schema(connection);
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO main.m3_ast_generations VALUES (?,?,?,?,?,?,?,?,?,?)")) {
            insert.setString(1, context.generation());
            insert.setString(2, context.toolchain());
            nullable(insert, 3, context.configurationSha256());
            insert.setString(4, "syntax");
            insert.setString(5, M3ASTIndex.DETAILS_SCHEMA);
            insert.setString(6, M3ASTIndex.STRUCTURAL_HASH_SCHEMA);
            insert.setBoolean(7, limits.maxExactSourceUnits() != 0);
            insert.setBoolean(8, limits.maxStructuralTextUnits() != 0);
            insert.setLong(9, 0);
            insert.setLong(10, 0);
            if (insert.executeUpdate() != 1) throw new SQLException("generation insertion lost");
        }
        long files = 0, nodes = 0, exactWork = 0;
        try (PreparedStatement file = connection.prepareStatement("INSERT INTO main.m3_ast_files VALUES (?,?,?,?,?,?,?,?)");
             PreparedStatement node = connection.prepareStatement(INSERT_NODE)) {
            while (input.hasNext()) {
                if (files == limits.maxFiles()) throw new IllegalArgumentException("facts file budget exceeded");
                M3ASTIndex index = Objects.requireNonNull(input.next(), "null AST index");
                if (index.nodeCount() > limits.maxNodes() - nodes) {
                    throw new IllegalArgumentException("facts node budget exceeded");
                }
                if (limits.maxExactSourceUnits() != 0) {
                    exactWork = exactWork(index, exactWork, limits.maxExactSourceUnits());
                }
                M3ASTIndex.StructuralHashes hashes = limits.maxStructuralTextUnits() == 0 ? null
                        : index.structuralHashes(index.nodeCount(), limits.maxStructuralTextUnits());
                file.setString(1, context.generation());
                file.setLong(2, files);
                file.setString(3, index.source().uri().toASCIIString());
                file.setString(4, index.domain().name());
                file.setString(5, index.source().utf16Sha256());
                nullable(file, 6, index.source().byteSha256().orElse(null));
                file.setInt(7, index.source().length());
                file.setInt(8, index.nodeCount());
                if (file.executeUpdate() != 1) throw new SQLException("file insertion lost");
                int pending = 0;
                long pendingText = 0;
                for (int row = 0; row < index.nodeCount(); row++) {
                    long textUnits = rowTextUnits(context.generation(), index, row, hashes != null,
                            limits.maxExactSourceUnits() != 0);
                    if (textUnits > limits.maxBatchTextUnits()) {
                        throw new IllegalArgumentException("facts JDBC row text budget exceeded");
                    }
                    if (pending != 0 && textUnits > limits.maxBatchTextUnits() - pendingText) {
                        batch(node, pending); pending = 0; pendingText = 0;
                    }
                    bind(node, context.generation(), files, index, row, hashes, limits.maxExactSourceUnits() != 0);
                    node.addBatch();
                    pendingText += textUnits;
                    if (++pending == limits.batchSize()) {
                        batch(node, pending); pending = 0; pendingText = 0;
                    }
                }
                if (pending != 0) batch(node, pending);
                files++;
                nodes += index.nodeCount();
            }
        }
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE main.m3_ast_generations SET file_count=?,node_count=? WHERE generation=?")) {
            update.setLong(1, files); update.setLong(2, nodes); update.setString(3, context.generation());
            if (update.executeUpdate() != 1) throw new SQLException("generation update lost");
        }
        return new Result(files, nodes, false);
    }

    private static long exactWork(M3ASTIndex index, long spent, long limit) {
        for (int row = 0; row < index.nodeCount(); row++) {
            if (index.hasExactRange(row)) {
                int length = index.end(row) - index.start(row);
                if (length > limit - spent) throw new IllegalArgumentException("exact-span UTF-16 work budget exceeded");
                spent += length;
            }
        }
        return spent;
    }

    private static long rowTextUnits(String generation, M3ASTIndex index, int row, boolean structural, boolean exact) {
        return (long) generation.length() + 2L * index.kind(row).length()
                + index.literalKind(row).name().length() + index.name(row).map(String::length).orElse(0)
                + index.literalValue(row).map(String::length).orElse(0)
                + (structural ? 64 : 0) + (exact && index.hasExactRange(row) ? 64 : 0);
    }

    private static void batch(PreparedStatement statement, int expected) throws SQLException {
        int[] counts = statement.executeBatch();
        if (counts.length != expected) throw new SQLException("incomplete facts JDBC batch");
        for (int count : counts) {
            if (count != 1 && count != Statement.SUCCESS_NO_INFO) throw new SQLException("facts row insertion lost");
        }
        statement.clearBatch();
    }

    private static void bind(PreparedStatement out, String generation, long fileId, M3ASTIndex index,
                             int row, M3ASTIndex.StructuralHashes hashes, boolean exact) throws SQLException {
        out.setString(1, generation); out.setLong(2, fileId); out.setInt(3, row);
        out.setString(4, index.kind(row)); out.setInt(5, index.parent(row));
        out.setInt(6, index.start(row)); out.setInt(7, index.end(row)); out.setInt(8, index.flags(row));
        out.setInt(9, index.firstChild(row)); out.setInt(10, index.nextSibling(row)); out.setInt(11, index.childCount(row));
        out.setBoolean(12, index.hasDetails(row)); out.setInt(13, index.startLine(row)); out.setInt(14, index.endLine(row));
        utf16(out, 15, index.name(row).orElse(null)); out.setString(16, index.literalKind(row).name());
        utf16(out, 17, index.literalValue(row).orElse(null)); out.setLong(18, index.modifierBits(row));
        out.setInt(19, index.enclosingType(row)); out.setInt(20, index.enclosingExecutable(row));
        nullable(out, 21, exact && index.hasExactRange(row)
                ? index.sourceRangeSha256(row, index.end(row) - index.start(row)) : null);
        nullable(out, 22, hashes == null ? null : hashes.sha256(row));
        // kind is convenient SQL display text; this lane is its authoritative
        // lossless form, including kinds admitted by the legacy String API.
        utf16(out, 23, index.kind(row));
    }

    private static void nullable(PreparedStatement out, int column, String value) throws SQLException {
        if (value == null) out.setNull(column, Types.VARCHAR);
        else out.setString(column, value);
    }

    /** JDBC text encoding can replace unpaired surrogates; scalar lanes use exact UTF-16BE blobs. */
    private static void utf16(PreparedStatement out, int column, String value) throws SQLException {
        if (value == null) { out.setNull(column, Types.BLOB); return; }
        byte[] bytes = new byte[Math.multiplyExact(value.length(), 2)];
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            bytes[i * 2] = (byte) (c >>> 8); bytes[i * 2 + 1] = (byte) c;
        }
        out.setBytes(column, bytes);
    }

    private static void schema(Connection connection) throws SQLException {
        int present = 0;
        // SQLite resolves unqualified names through TEMP before main. Qualify
        // every owned statement and refuse TEMP shadows or TEMP triggers that
        // could still intercept explicitly main-qualified writes.
        try (PreparedStatement temporary = connection.prepareStatement(
                "SELECT name FROM temp.sqlite_master WHERE (type IN ('table','view') AND name=? COLLATE NOCASE)"
                        + " OR (type='trigger' AND tbl_name=? COLLATE NOCASE)")) {
            for (String table : TABLES.keySet()) {
                temporary.setString(1, table); temporary.setString(2, table);
                try (var rows = temporary.executeQuery()) {
                    if (rows.next()) throw new SQLException("temporary object conflicts with facts table: " + table);
                }
            }
        }
        try (PreparedStatement triggers = connection.prepareStatement(
                "SELECT name FROM main.sqlite_master WHERE type='trigger' AND tbl_name=? COLLATE NOCASE")) {
            for (String table : TABLES.keySet()) {
                triggers.setString(1, table);
                try (var rows = triggers.executeQuery()) {
                    if (rows.next()) throw new SQLException("foreign trigger on facts table: " + table);
                }
            }
        }
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT sql FROM main.sqlite_master WHERE type='table' AND name=? COLLATE NOCASE")) {
            for (var table : TABLES.entrySet()) {
                query.setString(1, table.getKey());
                try (var rows = query.executeQuery()) {
                    if (rows.next()) {
                        present++;
                        if (!table.getValue().equals(rows.getString(1)) || rows.next()) {
                            throw new SQLException("foreign or modified facts table: " + table.getKey());
                        }
                    }
                }
            }
        }
        if (present == 0) {
            try (var statement = connection.createStatement()) {
                for (String ddl : TABLES.values()) statement.execute(ddl.replace("CREATE TABLE ", "CREATE TABLE main."));
            }
            try (var insert = connection.prepareStatement("INSERT INTO main.m3_ast_meta VALUES (1,?)")) {
                insert.setString(1, SCHEMA); insert.executeUpdate();
            }
        } else if (present != TABLES.size()) {
            throw new SQLException("incomplete/foreign facts schema");
        }
        try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT id,schema FROM main.m3_ast_meta")) {
            if (!rows.next() || rows.getInt(1) != 1 || !SCHEMA.equals(rows.getString(2)) || rows.next()) {
                throw new SQLException("unrecognized facts schema identity");
            }
        }
    }

    private static Map<String, String> tables() {
        Map<String, String> tables = new LinkedHashMap<>();
        tables.put("m3_ast_meta", "CREATE TABLE m3_ast_meta (id INTEGER PRIMARY KEY CHECK(id=1),schema TEXT NOT NULL)");
        tables.put("m3_ast_generations", "CREATE TABLE m3_ast_generations (generation TEXT PRIMARY KEY,toolchain TEXT NOT NULL,"
                + "configuration_sha256 TEXT,fact_kind TEXT NOT NULL,details_schema TEXT NOT NULL,structural_schema TEXT NOT NULL,"
                + "exact_enabled INTEGER NOT NULL,structural_enabled INTEGER NOT NULL,file_count INTEGER NOT NULL,node_count INTEGER NOT NULL)");
        tables.put("m3_ast_files", "CREATE TABLE m3_ast_files (generation TEXT NOT NULL,file_id INTEGER NOT NULL,uri TEXT NOT NULL,"
                + "domain TEXT NOT NULL,source_utf16_sha256 TEXT NOT NULL,source_byte_sha256 TEXT,source_units INTEGER NOT NULL,"
                + "node_count INTEGER NOT NULL,PRIMARY KEY(generation,file_id),UNIQUE(generation,uri,domain))");
        tables.put("m3_ast_nodes", "CREATE TABLE m3_ast_nodes (generation TEXT NOT NULL,file_id INTEGER NOT NULL,node_id INTEGER NOT NULL,"
                + "kind TEXT NOT NULL,parent INTEGER NOT NULL,start INTEGER NOT NULL,end INTEGER NOT NULL,flags INTEGER NOT NULL,"
                + "first_child INTEGER NOT NULL,next_sibling INTEGER NOT NULL,child_count INTEGER NOT NULL,has_details INTEGER NOT NULL,"
                + "start_line INTEGER NOT NULL,end_line INTEGER NOT NULL,name_utf16 BLOB,literal_kind TEXT NOT NULL,literal_utf16 BLOB,"
                + "modifier_bits INTEGER NOT NULL,enclosing_type INTEGER NOT NULL,enclosing_executable INTEGER NOT NULL,"
                + "exact_sha256 TEXT,structural_sha256 TEXT,kind_utf16 BLOB NOT NULL,PRIMARY KEY(generation,file_id,node_id))");
        return java.util.Collections.unmodifiableMap(tables);
    }

    private static void text(String value, String label, int maximum) {
        Objects.requireNonNull(value, label);
        if (value.isBlank() || value.length() > maximum) throw new IllegalArgumentException("invalid " + label);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (++i >= value.length() || !Character.isLowSurrogate(value.charAt(i))) {
                    throw new IllegalArgumentException("non-scalar " + label);
                }
            } else if (Character.isLowSurrogate(c) || c == 0) {
                throw new IllegalArgumentException("non-scalar " + label);
            }
        }
    }
}
