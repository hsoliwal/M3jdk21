/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package proof;

import com.m3.fs.M3FsProgress;
import com.m3.fs.MFile;
import com.m3.fs.MFileCapture;
import com.m3.fs.MFileContent;
import com.m3.fs.MFileObservation;
import com.m3.fs.MFileShadow;
import com.m3.fs.MFileStreams;
import com.m3.fs.MFiles;
import com.m3.fs.vfs.provider.local.DefaultLocalFileProvider;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3FactsWriter;
import jdk.m3.parser.M3File;
import org.sqlite.SQLiteJDBCLoader;

/** A finite generated-artifact integration proof, deliberately outside product sources. */
public final class OnePassProof {
    private static final int K = 3;
    private static final int MAX_BYTES = 65_536;
    private static final AtomicLong CHECKS = new AtomicLong();
    private static final M3ASTIngress.Limits INGRESS = new M3ASTIngress.Limits(65_536, 10_000, 128);
    private static final M3FactsWriter.Limits FACTS =
            new M3FactsWriter.Limits(32, 100_000, 16, 262_144, 2_000_000, 262_144);

    private OnePassProof() {}

    private static void require(boolean condition, String message) {
        CHECKS.incrementAndGet();
        if (!condition) throw new AssertionError(message);
    }

    private record Fixture(Path path, String text, byte[] bytes) {}
    private record NodeHash(int row, String exact, String structural, int flags) {}
    private record Evidence(String uri, int bytes, int sourceUnits, String rawSha256,
            String utf16Sha256, int primaryOpens, int primaryCloses, long primaryBytes,
            long progressBytes, int shadowBytes, int metadataRequests, int captures,
            int sourceRequests, int parseCalls, int nodes, List<NodeHash> hashes) {}

    private static final class State {
        final String generation;
        final Path module;
        final boolean taskFailure;
        final boolean closeFailure;
        final Map<Path, Fixture> fixtures;
        final Map<String, Evidence> evidence = new ConcurrentHashMap<>();
        final AtomicInteger taskEntries = new AtomicInteger();
        final AtomicInteger activeTasks = new AtomicInteger();
        final AtomicInteger peakTasks = new AtomicInteger();
        final AtomicInteger sessionsOpened = new AtomicInteger();
        final AtomicInteger sessionsClosed = new AtomicInteger();
        final AtomicInteger inputsOpened = new AtomicInteger();
        final AtomicInteger inputsClosed = new AtomicInteger();
        final AtomicInteger activeInputs = new AtomicInteger();
        final AtomicInteger compilerOpened = new AtomicInteger();
        final AtomicInteger compilerClosed = new AtomicInteger();
        final AtomicInteger activeCompilers = new AtomicInteger();
        final AtomicInteger delivered = new AtomicInteger();
        final AtomicInteger streamCloses = new AtomicInteger();
        final CountDownLatch firstWave = new CountDownLatch(K);
        final CountDownLatch firstInserted = new CountDownLatch(1);
        final IOException injectedTask = new IOException("proof injected task failure after complete source read");
        final IllegalStateException injectedClose = new IllegalStateException("proof injected owned-stream close failure");
        volatile boolean cleanupBeforePublication;

        State(String generation, Path module, Map<Path, Fixture> fixtures,
                boolean taskFailure, boolean closeFailure) {
            this.generation = generation;
            this.module = module;
            this.fixtures = fixtures;
            this.taskFailure = taskFailure;
            this.closeFailure = closeFailure;
        }

        void noLeaks() {
            require(activeTasks.get() == 0, generation + ": task cleanup");
            require(activeInputs.get() == 0 && inputsOpened.get() == inputsClosed.get(),
                    generation + ": primary input cleanup");
            require(activeCompilers.get() == 0 && compilerOpened.get() == compilerClosed.get(),
                    generation + ": compiler file-manager cleanup");
            require(sessionsOpened.get() > 0 && sessionsOpened.get() == sessionsClosed.get(),
                    generation + ": registered provider component cleanup");
        }

        void beforePublication() {
            require(streamCloses.incrementAndGet() == 1, "facts owns one stream close");
            noLeaks();
            try {
                require(!visible(module, generation), "generation must not be visible before stream close returns");
            } catch (SQLException error) {
                throw new IllegalStateException("external pre-commit visibility check", error);
            }
            cleanupBeforePublication = true;
            if (closeFailure) throw injectedClose;
        }
    }

    /** An unused additional provider observes real manager lifecycle without replacing file:. */
    private static final class AuditProvider extends DefaultLocalFileProvider {
        private final State state;
        private final Thread owner = Thread.currentThread();
        private boolean closed;

        AuditProvider(State state) {
            this.state = state;
            state.sessionsOpened.incrementAndGet();
        }

        @Override public void close() {
            require(Thread.currentThread() == owner, "provider cleanup stays on its owning worker");
            require(!closed, "registered provider closes once");
            closed = true;
            try { super.close(); }
            finally { state.sessionsClosed.incrementAndGet(); }
        }
    }

    /** Counts the primary provider stream; it performs no second content read. */
    private static final class FileRead implements MFileContent {
        final MFile file;
        final State state;
        final Thread owner = Thread.currentThread();
        final ByteArrayOutputStream shadow = new ByteArrayOutputStream();
        int opens;
        int closes;
        int metadata;
        int captures;
        long bytes;
        long progressed;
        boolean eof;
        MFileStreams.Completion completion;
        MFileCapture capture;

        FileRead(MFile file, State state) { this.file = file; this.state = state; }
        void own() { require(Thread.currentThread() == owner, "file-local observation stays on worker"); }

        @Override public MFileObservation observation() throws IOException {
            own(); metadata++;
            return file.observation();
        }

        @Override public InputStream openInputStream() throws IOException {
            own();
            require(++opens == 1, "one primary provider input open per task");
            InputStream input = file.openInputStream();
            state.inputsOpened.incrementAndGet();
            state.activeInputs.incrementAndGet();
            return new InputStream() {
                @Override public int read() throws IOException {
                    own();
                    int value = input.read();
                    if (value < 0) eof = true;
                    else bytes++;
                    return value;
                }

                @Override public int read(byte[] target, int offset, int length) throws IOException {
                    own();
                    int count = input.read(target, offset, length);
                    if (count < 0) eof = true;
                    else bytes += count;
                    return count;
                }

                @Override public void close() throws IOException {
                    own();
                    require(++closes == 1, "M3File owns exactly one input close");
                    try { input.close(); }
                    finally {
                        state.inputsClosed.incrementAndGet();
                        state.activeInputs.decrementAndGet();
                    }
                }
            };
        }

        M3File read() throws IOException {
            M3FsProgress progress = M3FsProgress.of(() -> Thread.currentThread().isInterrupted(),
                    amount -> { own(); require(amount > 0, "positive progress step"); progressed += amount; });
            MFileShadow observer = new MFileShadow() {
                @Override public void onBytes(byte[] data, int offset, int length) {
                    own();
                    require(completion == null && capture == null, "raw bytes precede completion and capture");
                    shadow.write(data, offset, length);
                }

                @Override public void onComplete(MFileStreams.Completion value) {
                    own();
                    require(eof && closes == 1, "raw completion follows EOF and clean primary close");
                    require(completion == null && capture == null, "one shadow completion before capture");
                    completion = value;
                }
            };
            MFileContent content = progressify(progress).shadow("raw-java", observer).captureReads(value -> {
                own();
                require(completion != null && closes == 1, "capture follows shadow completion and primary close");
                require(++captures == 1 && capture == null, "one capture per successful primary read");
                capture = value;
            });
            // Ownership transfers to M3File.read. Do not add a second close owner.
            return M3File.read(URI.create(file.sourceAddress()), content.openInputStream(),
                    StandardCharsets.UTF_8, MAX_BYTES);
        }
    }

    private static M3ASTIndex project(MFile file, State state) throws IOException {
        int active = state.activeTasks.incrementAndGet();
        state.peakTasks.accumulateAndGet(active, Math::max);
        try {
            require(active <= K, "K=3 active task bound");
            int entry = state.taskEntries.incrementAndGet();
            if (entry <= K) {
                state.firstWave.countDown();
                await(state.firstWave, "three concurrent first-wave tasks");
            }
            Path path = Path.of(URI.create(file.sourceAddress())).toAbsolutePath().normalize();
            Fixture expected = state.fixtures.get(path);
            require(expected != null && file.isFile() && file.name().endsWith(".java"),
                    "only the finite selected Java fixtures enter tasks");
            FileRead observed = new FileRead(file, state);
            M3File source = observed.read();
            require(source.text().equals(expected.text()), "strict decoded source is the fixture text");
            require(source.uri().equals(URI.create(file.sourceAddress())), "provider URI is retained exactly");
            require(observed.eof && observed.opens == 1 && observed.closes == 1 && observed.metadata == 1,
                    "one complete primary read and metadata sample");
            require(observed.bytes == expected.bytes().length && observed.progressed == expected.bytes().length,
                    "primary/progress byte counts match fixture bytes");
            require(Arrays.equals(observed.shadow.toByteArray(), expected.bytes()), "shadow bytes equal primary fixture bytes");
            require(observed.completion != null && observed.capture != null && observed.captures == 1,
                    "successful input yields complete capture");
            require(observed.completion.kind() == MFileStreams.CompletionKind.INPUT_EOF
                            && observed.completion.layer().equals("raw-java")
                            && observed.completion.byteCount() == expected.bytes().length,
                    "raw completion domain and byte count");
            String raw = sha256(expected.bytes());
            require(source.byteSha256().orElseThrow().equals(raw), "M3File raw-byte SHA is exact");
            require(source.utf16Sha256().equals(hashUtf16(expected.text())), "M3File UTF-16 identity is exact");
            require(observed.capture.manifest().raw().id().hex().equals(raw)
                            && observed.capture.manifest().raw().size() == expected.bytes().length
                            && observed.completion.contentHash().hex().equals(raw),
                    "M3FS capture identity equals M3File raw-byte identity");
            require(observed.capture.observation().content().orElseThrow().equals(observed.capture.manifest().raw()),
                    "capture observation and raw manifest agree");
            require(observed.capture.manifest().structures().isEmpty(), "capture performs no hidden AST/precompute");

            AtomicInteger sourceRequests = new AtomicInteger();
            Thread contextOwner = Thread.currentThread();
            JavaFileObject javaSource = new SimpleJavaFileObject(source.uri(), JavaFileObject.Kind.SOURCE) {
                @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                    require(Thread.currentThread() == contextOwner, "compiler source request stays on worker");
                    sourceRequests.incrementAndGet();
                    return source.text();
                }
                @Override public InputStream openInputStream() {
                    throw new AssertionError("javac must not reopen primary source bytes");
                }
            };
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            require(compiler != null, "linked image contains the actual Java compiler");
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            StandardJavaFileManager manager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);
            state.compilerOpened.incrementAndGet();
            state.activeCompilers.incrementAndGet();
            M3ASTIndex index;
            try (manager) {
                JavacTask task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                        List.of("-proc:none", "-source", "21"), null, List.of(javaSource));
                Iterator<? extends CompilationUnitTree> parsed = task.parse().iterator();
                require(parsed.hasNext(), "one parsed compilation unit");
                CompilationUnitTree tree = parsed.next();
                require(!parsed.hasNext(), "task parses exactly one retained source");
                require(diagnostics.getDiagnostics().stream().noneMatch(d -> d.getKind() == Diagnostic.Kind.ERROR),
                        "fixture syntax has no javac errors");
                require(sourceRequests.get() == 1, "javac requests retained source once");
                index = M3ASTIngress.javac(source, tree, Trees.instance(task), INGRESS);
                require(sourceRequests.get() == 1, "existing-tree ingress does not request source or reparse");
            } finally {
                state.compilerClosed.incrementAndGet();
                state.activeCompilers.decrementAndGet();
            }
            require(index.domain() == M3ASTIndex.Domain.JAVAC && index.source() == source,
                    "detached index retains the one admitted M3File");
            M3ASTIndex.StructuralHashes structures = index.structuralHashes(index.nodeCount(), 262_144);
            List<NodeHash> hashes = new ArrayList<>();
            for (int row = 0; row < index.nodeCount(); row++) {
                require((index.flags(row) & M3ASTIndex.SOURCE_UNVERIFIED) != 0,
                        "public supplied-tree source uncertainty remains explicit");
                hashes.add(new NodeHash(row, index.hasExactRange(row)
                        ? index.sourceRangeSha256(row, MAX_BYTES) : null,
                        structures.sha256(row), index.flags(row)));
            }
            Evidence evidence = new Evidence(source.uri().toASCIIString(), expected.bytes().length, source.length(),
                    raw, source.utf16Sha256(), observed.opens, observed.closes, observed.bytes,
                    observed.progressed, observed.shadow.size(), observed.metadata, observed.captures,
                    sourceRequests.get(), 1, index.nodeCount(), List.copyOf(hashes));
            require(state.evidence.putIfAbsent(evidence.uri(), evidence) == null, "one task per selected fixture");
            if (state.taskFailure && file.name().equals("RootC.java")) {
                // The second delivered index can only be demanded after the writer inserted the first.
                await(state.firstInserted, "one earlier index inserted before task failure");
                throw state.injectedTask;
            }
            return index;
        } finally { state.activeTasks.decrementAndGet(); }
    }

    private static void await(CountDownLatch latch, String reason) throws IOException {
        try {
            if (!latch.await(20, TimeUnit.SECONDS)) throw new IOException("timed out: " + reason);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted: " + reason, interrupted);
        }
    }

    private static List<MFiles.Plan> plans(Path fixtures, State state) {
        List<MFiles.Plan> roots = new ArrayList<>();
        for (String root : List.of("root-a", "root-b", "root-c")) {
            roots.add(MFiles.from(fixtures.resolve(root)).extensions(".java").filter(MFile::isFile)
                    .descendInto(file -> !file.name().equals("skip"))
                    .withProvider("proofaudit", () -> new AuditProvider(state)));
        }
        return List.copyOf(roots);
    }

    private static Map<Path, Fixture> fixtures(Path root) throws IOException {
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("root-a/RootA.java", "class RootA { static final int N = 3; String value() { return \"caf\u00e9\"; } }\n");
        sources.put("root-a/sub/Alpha.java", "record Alpha(int n) { int twice() { return n * 2; } }\r\n");
        sources.put("root-b/RootB.java", "interface RootB { int value(); default String label() { return \"\u03bb\"; } }\n");
        sources.put("root-b/sub/deep/Beta.java", "class Beta { int sum() { int s = 0; for (int i=0; i<3; i++) s += i; return s; } }\n");
        sources.put("root-c/RootC.java", "class RootC { String emoji = \"\ud83d\ude80\"; boolean ready() { return true; } }\n");
        sources.put("root-c/sub/Gamma.java", "@interface Gamma { String value() default \"stable\"; }\n");
        Map<Path, Fixture> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> row : sources.entrySet()) {
            Path path = root.resolve(row.getKey()).toAbsolutePath().normalize();
            Files.createDirectories(path.getParent());
            byte[] bytes = row.getValue().getBytes(StandardCharsets.UTF_8);
            Files.write(path, bytes);
            result.put(path, new Fixture(path, row.getValue(), bytes));
        }
        for (String name : List.of("root-a/ignored.txt", "root-b/sub/ignored.java.bak", "root-c/skip/Excluded.java")) {
            Path ignored = root.resolve(name);
            Files.createDirectories(ignored.getParent());
            Files.writeString(ignored, "THIS IS NOT VALID JAVA AND MUST NEVER ENTER A TASK", StandardCharsets.UTF_8);
        }
        return Map.copyOf(result);
    }

    private static M3FactsWriter.Context context(String generation) {
        String configuration = "one-pass-proof-v1|K=3|UTF-8|maxBytes=65536|-proc:none|-source=21"
                + "|existing-public-javac-tree|syntax|source-unverified|three-local-roots";
        return new M3FactsWriter.Context(generation, System.getProperty("java.runtime.version"),
                sha256(configuration.getBytes(StandardCharsets.UTF_8)));
    }

    private static Map<String, Object> runCase(Path fixtures, State state, ExecutorService executor) throws Exception {
        Stream<M3ASTIndex> indexes = MFiles.mapParallel(plans(fixtures, state), executor, K,
                file -> project(file, state)).peek(index -> {
                    if (state.delivered.incrementAndGet() == 2) state.firstInserted.countDown();
                }).onClose(state::beforePublication);
        M3FactsWriter.Result result = null;
        Throwable observed = null;
        try { result = M3FactsWriter.write(state.module, indexes, context(state.generation), FACTS); }
        catch (IOException | SQLException | RuntimeException failure) { observed = failure; }
        state.noLeaks();
        require(state.streamCloses.get() == 1 && state.cleanupBeforePublication, "owned cleanup is before publication");
        require(!executor.isShutdown(), "parallel mapping leaves the borrowed executor open");
        require(state.peakTasks.get() == K, "three roots exercise the full K=3 concurrency bound");
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("generation", state.generation);
        report.put("task_entries", state.taskEntries.get());
        report.put("peak_callbacks", state.peakTasks.get());
        report.put("completed_raw_and_parsed_files", state.evidence.size());
        report.put("delivered_indexes", state.delivered.get());
        report.put("provider_components_opened", state.sessionsOpened.get());
        report.put("provider_components_closed", state.sessionsClosed.get());
        report.put("primary_inputs_opened", state.inputsOpened.get());
        report.put("primary_inputs_closed", state.inputsClosed.get());
        report.put("compiler_contexts_opened", state.compilerOpened.get());
        report.put("compiler_contexts_closed", state.compilerClosed.get());
        report.put("stream_close_calls", state.streamCloses.get());
        report.put("cleanup_before_publication", state.cleanupBeforePublication);
        if (state.taskFailure) {
            require(observed instanceof UncheckedIOException && observed.getCause() == state.injectedTask,
                    "checked task failure retains exact cause through owned stream");
            require(state.delivered.get() >= 2, "task rollback follows at least one persisted uncommitted index");
            require(!visible(state.module, state.generation), "failed task generation is rolled back");
            require(rows(state.module, "m3_ast_files", state.generation) == 0
                            && rows(state.module, "m3_ast_nodes", state.generation) == 0,
                    "task failure leaves no file or node rows");
            report.put("outcome", "EXPECTED_TASK_FAILURE_ROLLED_BACK");
        } else if (state.closeFailure) {
            require(observed == state.injectedClose, "owned stream close failure retains identity");
            require(state.delivered.get() == state.fixtures.size(), "close failure follows all index writes");
            require(!visible(state.module, state.generation)
                            && rows(state.module, "m3_ast_files", state.generation) == 0
                            && rows(state.module, "m3_ast_nodes", state.generation) == 0,
                    "late close failure rolls back generation and its rows");
            report.put("outcome", "EXPECTED_CLOSE_FAILURE_ROLLED_BACK");
        } else {
            if (observed != null) throw new AssertionError("successful integration failed", observed);
            require(result != null && result.committed(), "successful owned generation commits");
            require(result.files() == state.fixtures.size() && state.evidence.size() == state.fixtures.size(),
                    "finite recursive/filter file count");
            require(visible(state.module, state.generation), "generation visible after successful cleanup and commit");
            report.putAll(checkFacts(state, result));
            report.put("outcome", "COMMITTED");
        }
        report.put("file_evidence", state.evidence.values().stream().sorted(Comparator.comparing(Evidence::uri))
                .map(OnePassProof::evidenceJson).toList());
        report.put("status", "PASS");
        return report;
    }

    private static Map<String, Object> checkFacts(State state, M3FactsWriter.Result result) throws SQLException {
        long expectedNodes = state.evidence.values().stream().mapToLong(Evidence::nodes).sum();
        long expectedExact = state.evidence.values().stream().flatMap(value -> value.hashes().stream())
                .filter(value -> value.exact() != null).count();
        require(result.nodes() == expectedNodes, "writer result node total");
        require(rows(state.module, "m3_ast_files", state.generation) == state.fixtures.size()
                        && rows(state.module, "m3_ast_nodes", state.generation) == expectedNodes,
                "actual SQL file and node totals");
        long sqlFiles = 0, sqlNodes = 0, sqlExact = 0, sqlStructural = 0;
        try (Connection connection = connect(state.module);
             PreparedStatement files = connection.prepareStatement("SELECT file_id,uri,domain,source_utf16_sha256,"
                     + "source_byte_sha256,source_units,node_count FROM m3_ast_files WHERE generation=?");
             PreparedStatement nodes = connection.prepareStatement("SELECT node_id,exact_sha256,structural_sha256,flags "
                     + "FROM m3_ast_nodes WHERE generation=? AND file_id=? ORDER BY node_id");
             PreparedStatement generation = connection.prepareStatement("SELECT fact_kind,file_count,node_count,"
                     + "exact_enabled,structural_enabled FROM m3_ast_generations WHERE generation=?")) {
            generation.setString(1, state.generation);
            try (ResultSet row = generation.executeQuery()) {
                require(row.next() && row.getString(1).equals("syntax") && row.getLong(2) == state.fixtures.size()
                                && row.getLong(3) == expectedNodes && row.getBoolean(4) && row.getBoolean(5),
                        "published generation names syntax and enabled bounded hash lanes");
                require(!row.next(), "one generation row");
            }
            files.setString(1, state.generation);
            try (ResultSet row = files.executeQuery()) {
                while (row.next()) {
                    Evidence expected = state.evidence.get(row.getString(2));
                    require(expected != null && row.getString(3).equals("JAVAC"), "SQL URI/domain match selected source");
                    require(row.getString(4).equals(expected.utf16Sha256()) && row.getString(5).equals(expected.rawSha256())
                                    && row.getInt(6) == expected.sourceUnits() && row.getInt(7) == expected.nodes(),
                            "SQL source identities, units and node count match the one admitted source");
                    sqlFiles++;
                    nodes.setString(1, state.generation); nodes.setLong(2, row.getLong(1));
                    int perFileNodes = 0;
                    try (ResultSet node = nodes.executeQuery()) {
                        while (node.next()) {
                            NodeHash expectedNode = expected.hashes().get(perFileNodes);
                            require(node.getInt(1) == expectedNode.row()
                                            && java.util.Objects.equals(node.getString(2), expectedNode.exact())
                                            && node.getString(3).equals(expectedNode.structural())
                                            && node.getInt(4) == expectedNode.flags(),
                                    "SQL node hashes and uncertainty flags match the existing detached index");
                            if (node.getString(2) != null) sqlExact++;
                            if (node.getString(3) != null) sqlStructural++;
                            perFileNodes++; sqlNodes++;
                        }
                    }
                    require(perFileNodes == expected.nodes(), "SQL per-file node total");
                }
            }
        }
        require(sqlFiles == state.fixtures.size() && sqlNodes == expectedNodes
                        && sqlExact == expectedExact && sqlStructural == expectedNodes,
                "SQL file/node/exact/structural hash totals");
        return Map.of("sql_files", sqlFiles, "sql_nodes", sqlNodes, "sql_raw_file_hashes", sqlFiles,
                "sql_utf16_file_hashes", sqlFiles, "sql_exact_node_hashes", sqlExact,
                "sql_structural_node_hashes", sqlStructural);
    }

    private static String databaseUrl(Path module) {
        return "jdbc:sqlite:" + module.resolve("target/m3/facts.sqlite").toAbsolutePath().toUri().toASCIIString();
    }
    private static Connection connect(Path module) throws SQLException { return DriverManager.getConnection(databaseUrl(module)); }

    private static boolean visible(Path module, String generation) throws SQLException {
        try (Connection connection = connect(module);
             PreparedStatement table = connection.prepareStatement(
                     "SELECT count(*) FROM sqlite_master WHERE type='table' AND name='m3_ast_generations'")) {
            try (ResultSet row = table.executeQuery()) { if (!row.next() || row.getInt(1) == 0) return false; }
            try (PreparedStatement query = connection.prepareStatement("SELECT count(*) FROM m3_ast_generations WHERE generation=?")) {
                query.setString(1, generation);
                try (ResultSet row = query.executeQuery()) { return row.next() && row.getInt(1) != 0; }
            }
        }
    }

    private static long rows(Path module, String table, String generation) throws SQLException {
        // All table names are private constants in this qualification harness.
        try (Connection connection = connect(module);
             PreparedStatement statement = connection.prepareStatement("SELECT count(*) FROM " + table + " WHERE generation=?")) {
            statement.setString(1, generation);
            try (ResultSet row = statement.executeQuery()) { require(row.next(), "SQL count row"); return row.getLong(1); }
        }
    }

    private static Map<String, Object> evidenceJson(Evidence value) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("uri", value.uri()); out.put("bytes", value.bytes()); out.put("source_units", value.sourceUnits());
        out.put("raw_sha256", value.rawSha256()); out.put("utf16_sha256", value.utf16Sha256());
        out.put("primary_opens", value.primaryOpens()); out.put("primary_closes", value.primaryCloses());
        out.put("primary_bytes", value.primaryBytes()); out.put("progress_bytes", value.progressBytes());
        out.put("shadow_bytes", value.shadowBytes()); out.put("metadata_requests", value.metadataRequests());
        out.put("captures", value.captures()); out.put("javac_source_requests", value.sourceRequests());
        out.put("javac_parse_calls", value.parseCalls()); out.put("nodes", value.nodes());
        return out;
    }

    private static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private static String hashUtf16(String value) {
        byte[] bytes = new byte[value.length() * 2];
        for (int i = 0; i < value.length(); i++) { bytes[2 * i] = (byte) (value.charAt(i) >>> 8); bytes[2 * i + 1] = (byte) value.charAt(i); }
        return sha256(bytes);
    }

    private static String json(Object value) {
        if (value == null) return "null";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            return "{" + map.entrySet().stream().map(row -> json(row.getKey().toString()) + ":" + json(row.getValue()))
                    .collect(java.util.stream.Collectors.joining(",")) + "}";
        }
        if (value instanceof Iterable<?> values) {
            List<String> encoded = new ArrayList<>();
            for (Object item : values) encoded.add(json(item));
            return "[" + String.join(",", encoded) + "]";
        }
        StringBuilder result = new StringBuilder("\"");
        for (char unit : value.toString().toCharArray()) {
            switch (unit) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> { if (unit < 32) result.append(String.format("\\u%04x", (int) unit)); else result.append(unit); }
            }
        }
        return result.append('"').toString();
    }

    public static void main(String[] args) throws Exception {
        require(args.length == 1, "one fresh work directory argument");
        Path work = Path.of(args[0]).toAbsolutePath();
        Files.createDirectory(work);
        Path module = work.resolve("module");
        Files.createDirectories(module.resolve("target/m3"));
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schema", "M3_ONE_PASS_GENERATED_IMAGE_PROOF_V1");
        report.put("parallelism", K);
        report.put("provider", "generated M3FS local file provider; unused audit provider observes manager cleanup");
        report.put("facts_path", module.resolve("target/m3/facts.sqlite").toString());
        report.put("source_built_m3jdk21", "NOT_RUN");
        report.put("source_binding", "public existing-tree ingress remains SOURCE_UNVERIFIED; syntax facts only");
        report.put("scope", "one primary read and one retained-text javac parse per selected file per case; finite fixture proof");
        try {
            require(OnePassProof.class.getModule().getName().equals("m3fs.onepass.proof"), "named proof consumer");
            require(MFiles.class.getModule().getName().equals("jdk.m3fs"), "actual filesystem module");
            require(M3File.class.getModule().getName().equals("jdk.m3.parser"), "actual parser module");
            try (Connection connection = connect(module);
                 PreparedStatement statement = connection.prepareStatement("SELECT sqlite_version(),sqlite_source_id()");
                 ResultSet row = statement.executeQuery()) {
                require(row.next() && row.getString(1).equals("3.45.1"), "real SQLite 3.45.1 engine");
                report.put("sqlite_version", row.getString(1)); report.put("sqlite_source_id", row.getString(2));
                report.put("jdbc_driver_version", connection.getMetaData().getDriverVersion());
            }
            require(SQLiteJDBCLoader.isNativeMode(), "original SQLite JDBC JNI engine is active");
            require(SQLiteJDBCLoader.class.getModule().getName().equals("org.xerial.sqlitejdbc"), "explicit SQLite module");
            report.put("sqlite_jni", "ACTIVE");
            Path fixtureRoot = work.resolve("fixtures");
            Map<Path, Fixture> fixtures = fixtures(fixtureRoot);
            List<Map<String, Object>> cases = new ArrayList<>();
            try (ExecutorService executor = Executors.newFixedThreadPool(K)) {
                for (int run = 0; run < 3; run++) {
                    String name = List.of("one-pass-success", "one-pass-task-failure", "one-pass-close-failure").get(run);
                    cases.add(runCase(fixtureRoot, new State(name, module, fixtures, run == 1, run == 2), executor));
                }
            }
            report.put("cases", cases);
            require(rows(module, "m3_ast_generations", "one-pass-success") == 1
                            && !visible(module, "one-pass-task-failure") && !visible(module, "one-pass-close-failure"),
                    "successful generation survives both failed publications");
            report.put("status", "PASS");
        } catch (Exception | Error failure) {
            report.put("status", "FAIL"); report.put("failure", failure.toString());
            throw failure;
        } finally {
            report.put("checks", CHECKS.get());
            Files.writeString(work.resolve("RESULTS.json"), json(report) + "\n", StandardCharsets.UTF_8);
        }
        System.out.println("M3_ONE_PASS_GENERATED_IMAGE_PASS checks=" + CHECKS.get() + " cases=3 K=3");
    }
}
