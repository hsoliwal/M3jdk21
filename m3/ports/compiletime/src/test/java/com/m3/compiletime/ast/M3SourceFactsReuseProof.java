// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.compiletime.ast;

import com.sun.source.util.JavacTask;
import com.synexia.mindex.ast.MIndexASTLimits;
import com.synexia.mindex.swap.SwapStore;
import com.synexia.mindex.swap.TieredSwapCache;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.MessageDigestSpi;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

/**
 * Exact-owner differential and work-count proof. The optional --baseline mode preserves all
 * behavioral assertions while expecting the original repeated source-digest work.
 *
 * <p>Domain/UTF-16 oracle and immutable test store adapt the existing Apache-2.0
 * M3FileSourceFactsTest and M3ASTCacheTestSupport. The test-only JCA provider counts actual
 * source-domain digests while delegating every byte to the installed SHA-256 implementation.</p>
 */
public final class M3SourceFactsReuseProof {
    private static final byte[] SOURCE_DOMAIN =
            "M3_FILE_SOURCE_UTF16_V1".getBytes(StandardCharsets.US_ASCII);
    private static final String PROVIDER_NAME = "M3SourceFactsProofSHA256";
    private static final AtomicLong SOURCE_DIGESTS = new AtomicLong();
    private static final AtomicLong SOURCE_UPDATES = new AtomicLong();
    private static final AtomicLong SOURCE_FAILURES = new AtomicLong();
    private static final AtomicBoolean FAIL_SOURCE_ONCE = new AtomicBoolean();
    private static Provider delegateProvider;
    private static boolean baseline;
    private static long checks;
    private static long fileDigests;
    private static long concurrentDigests;
    private static long viewDigests;
    private static long repeatedTaskDigests;
    private static long fallbackDigests;
    private static long blockUpdates;

    private M3SourceFactsReuseProof() { }

    public static void main(String[] args) throws Exception {
        if (args.length > 1 || (args.length == 1 && !"--baseline".equals(args[0]))) {
            throw new IllegalArgumentException("expected no argument or --baseline");
        }
        baseline = args.length == 1;
        delegateProvider = MessageDigest.getInstance("SHA-256").getProvider();
        if (Security.getProvider(PROVIDER_NAME) != null) throw new AssertionError("provider already installed");
        check(Security.insertProviderAt(new CountingProvider(), 1) == 1, "proof provider selected");
        try {
            unicodeFraming();
            immutableFileReuse();
            compilerConsumers();
            frozenInputAdmission();
            workspaceIdentityLaw();
            System.out.println("M3_SOURCE_FACTS_REUSE_PASS checks=" + checks + " baseline=" + baseline
                    + " fileDigests=" + fileDigests + " concurrentDigests=" + concurrentDigests
                    + " viewDigests=" + viewDigests + " repeatedTaskDigests=" + repeatedTaskDigests
                    + " fallbackDigests=" + fallbackDigests + " blockUpdates=" + blockUpdates);
        } finally {
            Security.removeProvider(PROVIDER_NAME);
            FAIL_SOURCE_ONCE.set(false);
        }
    }

    private static void unicodeFraming() throws Exception {
        List<String> values = new ArrayList<>(List.of("", "\0", "\r\n", "\n", "\ufeff",
                "\ud800", "\ud801", "\udfff", "\ud83d\ude00", "a\ud800b\udfff",
                "class A { String s = \"\\\\u0000\"; /* class B {} */ }\r\n"));
        for (int length : new int[] {1, 2, 15, 4_095, 4_096, 4_097, 8_191, 8_192, 8_193}) {
            char[] units = new char[length];
            Arrays.fill(units, 'x');
            units[0] = '\ufeff';
            if (length > 4_095) units[4_095] = '\ud83d';
            if (length > 4_096) units[4_096] = '\ude00';
            units[length - 1] = '\ud800';
            values.add(new String(units));
        }
        Random random = new Random(0x4d33535452494e47L);
        for (int sample = 0; sample < 128; sample++) {
            char[] units = new char[random.nextInt(12_288)];
            for (int i = 0; i < units.length; i++) units[i] = (char) random.nextInt(65_536);
            values.add(new String(units));
        }
        for (String value : values) {
            String expected = oracle(value);
            equal(expected, M3FileSourceFacts.sourceSha256(value), "String exact UTF-16");
            equal(expected, M3FileSourceFacts.sourceSha256ImmutableView(value), "immutable String exact UTF-16");
            SegmentedText segments = new SegmentedText(value, Math.min(value.length(), 4_096));
            equal(expected, M3FileSourceFacts.sourceSha256ImmutableView(segments), "segmented exact UTF-16");
            equal((long) value.length(), segments.reads, "one code-unit read per immutable-view unit");
        }
        check(!oracle("\ud800").equals(oracle("\ud801")), "unpaired surrogates stay distinct");
        check(!oracle("").equals(oracle("\0")), "length and NUL stay distinct");
        check(!oracle("\r\n").equals(oracle("\n")), "line ending identity");
        check(!M3FileSourceFacts.contextSha256(List.of("ab", "c")).equals(
                M3FileSourceFacts.contextSha256(List.of("a", "bc"))), "context field framing");
        check(!M3FileSourceFacts.contextSha256(List.of("A.jar", "B.jar")).equals(
                M3FileSourceFacts.contextSha256(List.of("B.jar", "A.jar"))), "ordered dependencies");
        StringBuilder mutable = new StringBuilder("class Frozen {}");
        M3FileSourceFacts key = M3FileSourceFacts.capture("memory:///Frozen.java", mutable,
                "a".repeat(64), "javac", 1, "PARSE");
        mutable.append(" class Changed {}");
        equal(oracle("class Frozen {}"), key.sourceSha256(), "mutable capture snapshots");
        expect(IllegalArgumentException.class, () -> new M3FileSourceFacts("", "a".repeat(64),
                0, "b".repeat(64), "javac", 1, "PARSE"));
        expect(IllegalArgumentException.class, () -> new M3FileSourceFacts("memory:///A.java",
                "A".repeat(64), 0, "b".repeat(64), "javac", 1, "PARSE"));

        String longText = "a".repeat(8_193);
        resetCounts();
        equal(oracle(longText), M3FileSourceFacts.sourceSha256ImmutableView(longText), "block digest");
        equal(1L, SOURCE_DIGESTS.get(), "one completed block digest");
        blockUpdates = SOURCE_UPDATES.get();
        equal(baseline ? 5L + 2L * longText.length() : 8L, blockUpdates,
                "domain, four length bytes and exact bulk windows");
    }

    private static void immutableFileReuse() throws Exception {
        String source = "/*\ud800 \ud83d\ude00*/ class Frozen {}";
        String expected = oracle(source);
        resetCounts();
        M3File file = M3File.of(URI.create("memory:///Frozen.java"), source);
        equal(0L, SOURCE_DIGESTS.get(), "cold file construction does not hash characters");
        check(file.source() == source, "retain original immutable String identity");
        check(file.getCharContent(false) == source, "compiler sees original String identity");
        check(file.byteSha256() == null, "character ingress does not invent byte identity");
        for (int i = 0; i < 32; i++) equal(expected, file.sourceSha256(), "repeated file seal");
        fileDigests = SOURCE_DIGESTS.get();
        equal(baseline ? 32L : 1L, fileDigests, "file digest work");
        StringBuilder mutable = new StringBuilder("class Stable {}");
        M3File snapshot = M3File.of(URI.create("memory:///Stable.java"), mutable);
        mutable.append(" class Changed {}");
        equal("class Stable {}", snapshot.source(), "file mutable ingress snapshot");
        equal(oracle("class Stable {}"), snapshot.sourceSha256(), "snapshot hash survives edit");
        try (var reader = file.openReader(false)) {
            StringBuilder read = new StringBuilder();
            int unit;
            while ((unit = reader.read()) != -1) read.append((char) unit);
            equal(source, read.toString(), "reader preserves raw characters");
        }

        M3File concurrent = M3File.of(URI.create("memory:///Concurrent.java"), source);
        resetCounts();
        CountDownLatch start = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(8)) {
            var jobs = new ArrayList<java.util.concurrent.Future<String>>();
            for (int i = 0; i < 32; i++) jobs.add(workers.submit(() -> {
                if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("start timeout");
                return concurrent.sourceSha256();
            }));
            start.countDown();
            for (var job : jobs) equal(expected, job.get(10, TimeUnit.SECONDS), "concurrent seal");
        }
        concurrentDigests = SOURCE_DIGESTS.get();
        equal(baseline ? 32L : 1L, concurrentDigests, "concurrent first demand work");

        M3File retry = M3File.of(URI.create("memory:///Retry.java"), source);
        resetCounts();
        FAIL_SOURCE_ONCE.set(true);
        expect(IllegalStateException.class, retry::sourceSha256);
        equal(expected, retry.sourceSha256(), "failed preparation remains retryable");
        equal(expected, retry.sourceSha256(), "successful retry is reusable");
        equal(1L, SOURCE_FAILURES.get(), "injected failure observed");
        equal(baseline ? 2L : 1L, SOURCE_DIGESTS.get(), "only successful result is published");
    }

    private static void compilerConsumers() throws Exception {
        String source = "package probe; class Reuse { String x = \"a\"; } class Neighbor {}\n";
        M3File file = M3File.of(URI.create("memory:///probe/Reuse.java"), source);
        M3JavacASTIndex.SourceResolver frozen = M3JavacASTIndex.frozenInputs(List.of(file));
        viewDigests = runCompilerConsumers(file, frozen);
        equal(baseline ? 48L : 1L, viewDigests, "frozen owner shares one digest across consumers and phases");
        repeatedTaskDigests = runCompilerConsumers(file, frozen);
        equal(baseline ? 48L : 0L, repeatedTaskDigests, "same immutable owner shares digest across tasks");
        // Wrapping an existing resolver preserves its public behavior, but gives no private seal authority.
        fallbackDigests = runCompilerConsumers(file, unit -> frozen.source(unit));
        equal(baseline ? 48L : 3L, fallbackDigests, "arbitrary resolver hashes once per demanded event");
    }

    private static long runCompilerConsumers(M3File file, M3JavacASTIndex.SourceResolver resolver)
            throws Exception {
        String source = file.source();
        String expectedHash = oracle(source);
        var compiler = ToolProvider.getSystemJavaCompiler();
        check(compiler != null, "actual javac exists");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        List<String> options = List.of("-proc:none", "--release", "21");
        M3CompilationContext context = M3CompilationContext.isolated(options);
        var leaked = new M3JavacASTIndex.View[1];
        List<M3FileSourceFacts> facts = new ArrayList<>();
        long[] eventCallbacks = {0};
        long digests;
        try (var files = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) compiler.getTask(null, files, diagnostics, options, null, List.of(file));
            try (var index = new M3JavacASTIndex(task, context, resolver, MIndexASTLimits.defaults(), 21)) {
                for (M3JavacASTIndex.Phase phase :
                        List.of(M3JavacASTIndex.Phase.PARSE, M3JavacASTIndex.Phase.ANALYZE)) {
                    for (int consumer = 0; consumer < 2; consumer++) {
                        String consumerId = "consumer-" + consumer;
                        index.on(phase, consumerId, view -> {
                            check(view.source() == source, "all consumers share original immutable text");
                            leaked[0] = view;
                            if (eventCallbacks[0] == 0) verifyCompilerThread(view);
                            eventCallbacks[0]++;
                            for (int provider = 0; provider < 8; provider++) {
                                M3FileSourceFacts key = view.facts("provider-" + provider, provider + 1);
                                equal(expectedHash, key.sourceSha256(), "provider source seal");
                                equal(context.sha256(), key.contextSha256(), "provider context seal");
                                equal(view.phase().name(), key.phase(), "provider phase");
                                equal(source.length(), key.utf16Length(), "provider length");
                                facts.add(key);
                            }
                        });
                    }
                }
                resetCounts();
                task.parse();
                task.analyze();
                digests = SOURCE_DIGESTS.get();
                equal(1L, index.statistics().parseEvents(), "one authoritative source parse");
                equal(2L, index.statistics().analyzeEvents(), "two original top-level analysis events");
                equal(0L, index.statistics().syntaxCaptures(), "metadata demand does not capture syntax");
                equal(6L, eventCallbacks[0], "two consumers on each original event");
                equal(48, facts.size(), "all provider keys retained independently");
                check(!facts.get(0).equals(facts.get(1)), "provider/schema keys cannot alias");
                check(!facts.get(0).equals(facts.get(16)), "parse and analysis keys cannot alias");
                check(!facts.get(16).providerId().equals(facts.get(32).providerId()),
                        "top-level analysis type identities cannot alias");
                expect(IllegalStateException.class, () -> leaked[0].facts("late", 1));
                for (var field : leaked[0].getClass().getDeclaredFields()) {
                    if (field.getType() == String.class || List.of("unit", "type", "syntax").contains(field.getName())) {
                        field.setAccessible(true);
                        check(field.get(leaked[0]) == null, "released view drops " + field.getName());
                    }
                }
            }
        }
        check(diagnostics.getDiagnostics().stream().noneMatch(d -> d.getKind() == Diagnostic.Kind.ERROR),
                "actual compiler accepts fixture");
        return digests;
    }

    private static void verifyCompilerThread(M3JavacASTIndex.View view) {
        try (var worker = Executors.newSingleThreadExecutor()) {
            check(worker.submit(() -> {
                try { view.facts("foreign-thread", 1); return false; }
                catch (IllegalStateException expected) { return true; }
            }).get(10, TimeUnit.SECONDS), "view refuses a foreign thread during callback");
        } catch (Exception failure) { throw new AssertionError("compiler-thread proof failed", failure); }
    }

    private static void frozenInputAdmission() throws Exception {
        String source = "class Admitted {}";
        URI uri = URI.create("memory:///Admitted.java");
        M3File admitted = M3File.of(uri, source);
        M3File differentOwner = M3File.of(uri, new String(source.toCharArray()));
        check(admitted.source() != differentOwner.source(), "equal text can have distinct admitted owners");
        expect(IllegalArgumentException.class,
                () -> M3JavacASTIndex.frozenInputs(List.of(admitted, differentOwner)));
        var compiler = ToolProvider.getSystemJavaCompiler();
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var files = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) compiler.getTask(null, files, diagnostics,
                    List.of("-proc:none", "--release", "21"), null, List.of(admitted));
            var unit = task.parse().iterator().next();
            check(M3JavacASTIndex.frozenInputs(List.of(admitted)).source(unit) == source,
                    "frozen resolver admits exact source String");
            expect(IOException.class, () -> M3JavacASTIndex.frozenInputs(List.of(differentOwner)).source(unit));
            M3File absent = M3File.of(URI.create("memory:///Absent.java"), source);
            expect(IOException.class, () -> M3JavacASTIndex.frozenInputs(List.of(absent)).source(unit));
        }
    }

    private static void workspaceIdentityLaw() throws Exception {
        String source = "class Cached { int value() { return 7; } }\n";
        M3File file = M3File.of(URI.create("memory:///Cached.java"), source);
        List<String> options = List.of("-proc:none", "--release", "21");
        String sourceGeneration = oracle(source);
        M3CompilationContext firstContext = M3CompilationContext.sealed(options, sourceGeneration,
                List.of("a".repeat(64)));
        M3CompilationContext changedDependency = M3CompilationContext.sealed(options, sourceGeneration,
                List.of("b".repeat(64)));
        check(!firstContext.sha256().equals(changedDependency.sha256()), "dependency content changes context");
        var limits = MIndexASTLimits.defaults();
        MemoryStore store = new MemoryStore();
        M3AST[] original = new M3AST[1];
        M3FileSourceFacts[] key = new M3FileSourceFacts[1];
        var budget = new TieredSwapCache.Budget(4_194_304, 8, 0);
        try (var cache = new M3ASTWorkspaceCache<>(firstContext.sha256(), store,
                new M3ASTCodec(limits), budget, false)) {
            var compiler = ToolProvider.getSystemJavaCompiler();
            var diagnostics = new DiagnosticCollector<JavaFileObject>();
            try (var files = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
                JavacTask task = (JavacTask) compiler.getTask(null, files, diagnostics,
                        options, null, List.of(file));
                try (var index = new M3JavacASTIndex(task, firstContext,
                        M3JavacASTIndex.frozenInputs(List.of(file)), limits, 21)) {
                    index.on(M3JavacASTIndex.Phase.PARSE, "syntax", view -> {
                        key[0] = view.facts("javac-public-tree", 1);
                        try (var held = view.acquireSyntax(cache, 7)) {
                            original[0] = held.value();
                            check(view.ast().source() == source, "direct capture retains exact source owner");
                            equal(source, original[0].source(), "durable cache projection preserves source characters");
                        } catch (IOException failure) { throw new java.io.UncheckedIOException(failure); }
                    });
                    task.parse();
                    equal(1L, index.statistics().parseEvents(), "cache population parses once");
                    equal(1L, index.statistics().syntaxCaptures(), "cache population captures once");
                }
            }
            check(original[0] != null && original[0].occurrenceCount() > 1, "actual syntax captured");
            check(diagnostics.getDiagnostics().stream().noneMatch(d -> d.getKind() == Diagnostic.Kind.ERROR),
                    "cache fixture parses");
            M3File unchanged = M3File.of(file.toUri(), new String(source.toCharArray()));
            M3FileSourceFacts unchangedKey = M3FileSourceFacts.capture(file.toUri().toString(),
                    unchanged.source(), firstContext.sha256(), "javac-public-tree", 1, "PARSE");
            equal(key[0], unchangedKey, "equal source snapshot and context identify same cached syntax");
            try (var held = cache.acquire(7, unchangedKey, id -> {
                throw new AssertionError("unchanged cached syntax must not invoke a loader/parser");
            })) {
                check(held.value() == original[0], "warm reuse preserves syntax identity");
            }
            equal(1L, cache.stats().loads(), "unchanged source does not repeat loader");
            M3FileSourceFacts otherSource = M3FileSourceFacts.capture(file.toUri().toString(),
                    source + "// changed", firstContext.sha256(), "javac-public-tree", 1, "PARSE");
            expect(IOException.class, () -> acquireAndClose(cache, otherSource));
            M3FileSourceFacts otherProvider = new M3FileSourceFacts(key[0].sourceUri(),
                    key[0].sourceSha256(), key[0].utf16Length(), key[0].contextSha256(),
                    "another-provider", 1, "PARSE");
            expect(IOException.class, () -> acquireAndClose(cache, otherProvider));
        }

        // Reopen the actual workspace/cache/codec over the test store. No compiler exists here.
        try (var reopened = new M3ASTWorkspaceCache<>(firstContext.sha256(), store,
                new M3ASTCodec(limits), budget, false); var held = reopened.acquire(7, key[0])) {
            M3AST decoded = held.value();
            equal(source, decoded.source(), "cold reload exact source");
            equal(original[0].fingerprint(), decoded.fingerprint(), "cold reload syntax fingerprint");
            equal(original[0].occurrenceCount(), decoded.occurrenceCount(), "cold reload occurrence count");
            for (int i = 0; i < decoded.occurrenceCount(); i++) {
                equal(original[0].kind(i), decoded.kind(i), "cold reload node kind");
                equal(original[0].start(i), decoded.start(i), "cold reload source start");
                equal(original[0].end(i), decoded.end(i), "cold reload source end");
            }
        }
        try (var invalidated = new M3ASTWorkspaceCache<>(changedDependency.sha256(), store,
                new M3ASTCodec(limits), budget, false)) {
            expect(IllegalArgumentException.class, () -> acquireAndClose(invalidated, key[0]));
            M3FileSourceFacts changedKey = new M3FileSourceFacts(key[0].sourceUri(), key[0].sourceSha256(),
                    key[0].utf16Length(), changedDependency.sha256(), key[0].providerId(), 1, "PARSE");
            equal(key[0].sourceSha256(), changedKey.sourceSha256(), "unchanged text across dependency edit");
            expect(IOException.class, () -> acquireAndClose(invalidated, changedKey));
            equal(0L, invalidated.stats().loads(), "changed context cannot load stale facts");
        }
    }

    private static void acquireAndClose(M3ASTWorkspaceCache<M3AST> cache, M3FileSourceFacts key)
            throws IOException {
        try (var held = cache.acquire(7, key)) { held.value(); }
    }

    private static String oracle(String source) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.write(SOURCE_DOMAIN);
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(source.length());
            for (char unit : source.toCharArray()) output.writeChar(unit);
        }
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256", delegateProvider)
                .digest(bytes.toByteArray()));
    }

    private static void resetCounts() {
        SOURCE_DIGESTS.set(0);
        SOURCE_UPDATES.set(0);
        SOURCE_FAILURES.set(0);
    }

    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    private static void equal(Object expected, Object actual, String message) {
        check(java.util.Objects.equals(expected, actual), message + ": expected=" + expected + ", actual=" + actual);
    }

    @FunctionalInterface private interface CheckedAction { void run() throws Exception; }

    private static void expect(Class<? extends Throwable> type, CheckedAction action) {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("expected " + type.getName() + ", got " + failure, failure);
        }
        throw new AssertionError("expected " + type.getName());
    }

    /** Segments deliberately refuse flattening and expose each UTF-16 read to the proof. */
    private static final class SegmentedText implements CharSequence {
        private final String left;
        private final String right;
        private long reads;
        SegmentedText(String source, int cut) {
            left = source.substring(0, cut);
            right = source.substring(cut);
        }
        @Override public int length() { return left.length() + right.length(); }
        @Override public char charAt(int index) {
            java.util.Objects.checkIndex(index, length());
            reads++;
            return index < left.length() ? left.charAt(index) : right.charAt(index - left.length());
        }
        @Override public CharSequence subSequence(int start, int end) { throw new AssertionError("slice"); }
        @Override public String toString() { throw new AssertionError("flatten"); }
    }

    private static final class CountingProvider extends Provider {
        private static final long serialVersionUID = 1L;
        CountingProvider() {
            super(PROVIDER_NAME, "1.0", "Test-only source-domain work counter");
            putService(new Service(this, "MessageDigest", "SHA-256", CountingDigest.class.getName(),
                    List.of(), Map.of()));
        }
    }

    /** Public only because JCA instantiates the proof's digest SPI reflectively. */
    public static final class CountingDigest extends MessageDigestSpi {
        private final MessageDigest delegate;
        private boolean first = true;
        private boolean source;
        public CountingDigest() {
            try { delegate = MessageDigest.getInstance("SHA-256", delegateProvider); }
            catch (NoSuchAlgorithmException failure) { throw new AssertionError(failure); }
        }
        @Override protected void engineUpdate(byte input) {
            if (first) { first = false; source = false; }
            if (source) SOURCE_UPDATES.incrementAndGet();
            delegate.update(input);
        }
        @Override protected void engineUpdate(byte[] input, int offset, int length) {
            if (first) {
                first = false;
                source = length == SOURCE_DOMAIN.length
                        && Arrays.equals(input, offset, offset + length, SOURCE_DOMAIN, 0, SOURCE_DOMAIN.length);
            }
            if (source) SOURCE_UPDATES.incrementAndGet();
            delegate.update(input, offset, length);
        }
        @Override protected byte[] engineDigest() {
            if (source && FAIL_SOURCE_ONCE.compareAndSet(true, false)) {
                SOURCE_FAILURES.incrementAndGet();
                throw new IllegalStateException("injected source digest failure");
            }
            byte[] result = delegate.digest();
            if (source) SOURCE_DIGESTS.incrementAndGet();
            first = true;
            source = false;
            return result;
        }
        @Override protected void engineReset() {
            delegate.reset();
            first = true;
            source = false;
        }
        @Override protected int engineGetDigestLength() { return delegate.getDigestLength(); }
    }

    /** Adapted from the incumbent M3ASTCacheTestSupport; this is only a test storage fixture. */
    private static final class MemoryStore implements SwapStore {
        private final TreeMap<Long, byte[]> records = new TreeMap<>();
        private final Map<String, Head> heads = new HashMap<>();
        private boolean closed;
        @Override public synchronized byte[] read(long id) throws IOException {
            requireOpen();
            byte[] bytes = records.get(id);
            return bytes == null ? null : bytes.clone();
        }
        @Override public synchronized void write(Map<Long, byte[]> values, Publication publication)
                throws IOException {
            requireOpen();
            if (values.size() > maxBatchRecords()) throw new IOException("batch count");
            long total = 0;
            for (var entry : values.entrySet()) {
                SwapStore.requireId(entry.getKey());
                byte[] old = records.get(entry.getKey());
                if (old != null && !Arrays.equals(old, entry.getValue())) throw new IOException("record conflict");
                if (entry.getValue().length > maxRecordBytes()) throw new IOException("record bytes");
                total += entry.getValue().length;
            }
            if (total > maxBatchBytes()) throw new IOException("batch bytes");
            if (publication != null && head(publication.name()).revision() != publication.expectedRevision()) {
                throw new IOException("stale publication");
            }
            for (var entry : values.entrySet()) records.putIfAbsent(entry.getKey(), entry.getValue().clone());
            if (publication != null) heads.put(publication.name(),
                    new Head(publication.expectedRevision() + 1, publication.recordId()));
        }
        @Override public synchronized Head head(String name) { return heads.getOrDefault(name, new Head(0, -1)); }
        @Override public synchronized long[] ids(long after, int limit) {
            return records.tailMap(after, false).keySet().stream().limit(limit).mapToLong(Long::longValue).toArray();
        }
        @Override public synchronized long count() { return records.size(); }
        @Override public int maxRecordBytes() { return 1 << 20; }
        @Override public int maxBatchBytes() { return 4 << 20; }
        @Override public int maxBatchRecords() { return 64; }
        @Override public synchronized void close() { closed = true; }
        private void requireOpen() throws IOException { if (closed) throw new IOException("closed store"); }
    }
}
