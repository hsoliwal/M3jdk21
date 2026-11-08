// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.atomjit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jdk.m3.tooling.atoms.AtomCensus;
import jdk.m3.tooling.atoms.contract.AtomKind;
import jdk.m3.tooling.atoms.contract.AtomRecord;

/**
 * Read-only OpenJDK FILE atom -> actual HotSpot JIT -> pattern review -> JIT retest.
 *
 * <p>The atoms and their patterns come from the M3 atom census ({@code m3/tooling/atom-census}, {@code --semantic}):
 * its closed catalogue pattern and the exact source digest of each executable Java atom. The census carries no
 * contract/structure/logic projection, so the donor's projection-recurrence states are not produced here.</p>
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/b65df53330e3d82f6b2d5509ded971f19883f4ca/synexia-maven-plugin/src/main/java/com/synexia/ai/m3scale/M3OpenJdkOrderedPasses.java blob 698de807e693; recipe m3/tooling/atom-jit-gate/recipe/atom-jit-gate-20261106/relocate.py v4
public final class M3OpenJdkOrderedPasses {
    private static final long MAX_FILE_BYTES = 16L * 1024 * 1024;
    private static final int MAX_VM_OUTPUT = 1 << 20;
    private static final Duration VM_TIMEOUT = Duration.ofSeconds(60);
    private static final String DIGEST = "M3_JIT_DIGEST=";
    private static final String CLASS = M3OpenJdkOrderedPasses.class.getName();

    private M3OpenJdkOrderedPasses() {}

    public static final class Atomized {
        private final String path;
        private final String sourceSha256;
        private final List<AtomRecord> atoms;

        private Atomized(String path, String sourceSha256, List<AtomRecord> atoms) {
            this.path = path;
            this.sourceSha256 = sourceSha256;
            this.atoms = List.copyOf(atoms);
        }
        public String path() { return path; }
        public String sourceSha256() { return sourceSha256; }
        public int atomCount() { return atoms.size(); }
    }

    public static final class JitChecked {
        private final Atomized before;
        private final String digest;
        private final Path java;
        private final String classpath;

        private JitChecked(Atomized before, String digest, Path java, String classpath) {
            this.before = before;
            this.digest = digest;
            this.java = java;
            this.classpath = classpath;
        }
        public Atomized atomized() { return before; }
        public String digest() { return digest; }
    }

    public static final class PatternReviewed {
        private final JitChecked tested;
        private final Map<String, String> entries;

        private PatternReviewed(JitChecked tested, Map<String, String> entries) {
            this.tested = tested;
            this.entries = Map.copyOf(entries);
        }
        public int patternEntries() { return entries.size(); }
        public String patternRoot() {
            List<String> ordered = entries.entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .map(e -> e.getKey() + ":" + e.getValue()).toList();
            return canonical("M3_OPENJDK_CENSUS_PATTERN_REVIEW_V1", ordered);
        }
        public boolean mutationAuthority() { return false; }
        public boolean promotionAuthority() { return false; }
    }

    public static final class Retested {
        private final PatternReviewed reviewed;

        private Retested(PatternReviewed reviewed) { this.reviewed = reviewed; }
        public int checkedAtoms() { return reviewed.patternEntries(); }
        public String sourceSha256() { return reviewed.tested.before.sourceSha256(); }
        public String patternRoot() { return reviewed.patternRoot(); }
        public boolean promotionAuthority() { return false; }
    }

    /** First phase: the M3 atom census over the exact bytes (javac parse only), with no byte rewriting. */
    public static Atomized atomize(String logicalPath, byte[] source, Path patternRules) {
        Objects.requireNonNull(source, "source");
        String path = Objects.requireNonNull(logicalPath, "logicalPath");
        if (!path.matches("src/[A-Za-z0-9_.-]+/(?:share|unix|linux|windows|macos|aix)/classes/(?:[A-Za-z0-9_$.-]+/)*[A-Za-z0-9_$.-]+\\.java")
                || path.contains("/../") || path.contains("/./")) {
            throw new IllegalArgumentException("OpenJDK source coordinate required");
        }
        if (source.length == 0 || source.length > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("OpenJDK file size budget");
        }
        String module = path.substring(4, path.indexOf('/', 4));
        List<AtomRecord> atoms = census(module, path, source,
                Objects.requireNonNull(patternRules, "patternRules"));
        if (atoms.isEmpty()) {
            throw new IllegalArgumentException("no executable declaration atoms");
        }
        return new Atomized(path, sha256(source), atoms);
    }

    /** Second phase: run a real child JVM in interpreted and compiled modes. */
    public static JitChecked testJit(Atomized atoms, byte[] currentSource, Path javaExecutable, String classpath)
            throws IOException, InterruptedException {
        Objects.requireNonNull(atoms, "atoms");
        verifyCurrent(atoms, currentSource);
        Objects.requireNonNull(javaExecutable, "javaExecutable");
        String cp = Objects.requireNonNull(classpath, "classpath");
        if (cp.isBlank()) throw new IllegalArgumentException("classpath");
        Path java = javaExecutable.toAbsolutePath().normalize();
        if (!Files.isRegularFile(java) || !Files.isExecutable(java)) {
            throw new IOException("Java executable is absent");
        }
        String interpreted = run(java, cp, true);
        String compiled = run(java, cp, false);
        String a = digest(interpreted);
        String b = digest(compiled);
        if (!a.equals(b) || !compiled.contains(CLASS + "::hotLoop")) {
            throw new IllegalStateException("HotSpot JIT parity/compilation witness missing");
        }
        return new JitChecked(atoms, a, java, cp);
    }

    /** Third phase: the census catalogue pattern and exact source digest per atom; recognition is not equivalence. */
    public static PatternReviewed patternize(JitChecked tested) {
        Objects.requireNonNull(tested, "tested");
        Map<String, String> entries = tested.before.atoms.stream().collect(Collectors.toMap(
                AtomRecord::atomId, atom -> atom.patternId() + ":" + atom.sourceSha256(),
                (left, right) -> { throw new IllegalStateException("duplicate atom identity"); }));
        if (entries.size() != tested.before.atoms.size()) {
            throw new IllegalStateException("duplicate or missing atom identity");
        }
        return new PatternReviewed(tested, entries);
    }

    /** Fourth phase: rerun the same executable; never promote on receipt alone. */
    public static Retested retest(PatternReviewed reviewed, byte[] currentSource)
            throws IOException, InterruptedException {
        Objects.requireNonNull(reviewed, "reviewed");
        JitChecked after = testJit(reviewed.tested.before, currentSource,
                reviewed.tested.java, reviewed.tested.classpath);
        if (!after.digest.equals(reviewed.tested.digest)) {
            throw new IllegalStateException("post-pattern HotSpot behavior changed");
        }
        return new Retested(reviewed);
    }

    private static void verifyCurrent(Atomized atoms, byte[] currentSource) {
        byte[] data = Objects.requireNonNull(currentSource, "currentSource");
        if (!atoms.sourceSha256.equals(sha256(data))) {
            throw new IllegalStateException("OpenJDK source drift between ordered passes");
        }
    }

    private static String run(Path java, String cp, boolean interpreted)
            throws IOException, InterruptedException {
        List<String> command = interpreted
                ? List.of(java.toString(), "-Xint", "-cp", cp, CLASS, "--hot")
                : List.of(java.toString(), "-Xbatch", "-XX:-TieredCompilation",
                        "-XX:CompileThreshold=100", "-XX:+PrintCompilation",
                        "-cp", cp, CLASS, "--hot");
        Path log = Files.createTempFile("m3-hotspot-probe-", ".log");
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true).redirectOutput(log.toFile()).start();
            if (!process.waitFor(VM_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new IOException("HotSpot witness exceeded time budget");
            }
            if (process.exitValue() != 0 || Files.size(log) > MAX_VM_OUTPUT) {
                throw new IOException("HotSpot witness failed or exceeded output budget");
            }
            return Files.readString(log, StandardCharsets.UTF_8);
        } finally {
            Files.deleteIfExists(log);
        }
    }

    private static String digest(String output) {
        String marker = null;
        for (String line : output.split("\\R")) {
            if (line.startsWith(DIGEST)) {
                if (marker != null || !line.substring(DIGEST.length()).matches("[0-9a-f]{1,16}")) {
                    throw new IllegalStateException("invalid HotSpot oracle digest");
                }
                marker = line.substring(DIGEST.length());
            }
        }
        if (marker == null) throw new IllegalStateException("HotSpot oracle absent");
        return marker;
    }

    /** Columns of the census SEMANTIC_CENSUS_&lt;repo&gt;.tsv ledger (contract/SEMANTIC_SOURCE_CENSUS_V1.md). */
    private static final int CENSUS_COLUMNS = 12;
    private static final String CENSUS_HEADER_PREFIX = "atom_id\trepo\tpath\tkind\towner\tname\tsignature\t";
    private static final String CENSUS_CONTRACT = "m3/tooling/atom-census SEMANTIC_CENSUS_V1";

    /** Runs the census CLI ({@code --semantic}) on the one file at its logical path; keeps executable Java atoms. */
    private static List<AtomRecord> census(String module, String path, byte[] source, Path rules) {
        Path work = null;
        try {
            work = Files.createTempDirectory("m3-atom-census-");
            Path root = work.resolve("src");
            Path file = root.resolve(path);
            Files.createDirectories(file.getParent());
            Files.write(file, source);
            Path out = work.resolve("out");
            AtomCensus.main(new String[] {"--semantic", module, rules.toString(), out.toString(), root.toString()});
            return atoms(path, source, Files.readAllLines(
                    out.resolve("SEMANTIC_CENSUS_" + module + ".tsv"), StandardCharsets.UTF_8));
        } catch (IOException failure) {
            throw new IllegalArgumentException("census atom parse failed", failure);
        } finally {
            if (work != null) deleteTree(work);
        }
    }

    /** The census rows of one file, bound to its exact bytes through the FILE_CONTAINER source digest. */
    private static List<AtomRecord> atoms(String path, byte[] source, List<String> rows) {
        if (rows.isEmpty() || !rows.get(0).startsWith(CENSUS_HEADER_PREFIX)) {
            throw new IllegalStateException("census ledger schema changed");
        }
        List<AtomRecord> all = rows.stream().skip(1).map(row -> row.split("\t", -1)).map(cells -> {
            if (cells.length != CENSUS_COLUMNS || !path.equals(cells[2])) {
                throw new IllegalStateException("census row outside the atomized file");
            }
            return new AtomRecord(cells[0], cells[2], AtomKind.valueOf(cells[3]), cells[4], cells[6],
                    cells[9], cells[10], CENSUS_CONTRACT, Boolean.parseBoolean(cells[11]));
        }).toList();
        String digest = sha256(source);
        if (all.stream().noneMatch(atom -> atom.kind() == AtomKind.FILE_CONTAINER
                && atom.sourceSha256().equals(digest))) {
            throw new IllegalStateException("census source identity drift");
        }
        return all.stream().filter(atom -> atom.kind().executable() && atom.kind().javaAtom()).toList();
    }

    private static void deleteTree(Path root) {
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        } catch (IOException failure) {
            throw new UncheckedIOException("census scratch cleanup failed", failure);
        }
    }

    /** SHA-256 of the bytes, lowercase hex (the donor's M3Hash.sha256, on java.security.MessageDigest). */
    static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    /** Domain-separated, length-framed SHA-256 of the values (the donor's M3Hash.canonical). */
    private static String canonical(String domain, List<String> values) {
        StringBuilder out = new StringBuilder();
        append(out, domain);
        for (String value : values) {
            append(out, value);
        }
        return sha256(out.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void append(StringBuilder out, String value) {
        String safe = value == null ? "" : value;
        out.append(safe.length()).append(':').append(safe).append(';');
    }

    /** Fixed, independently repeatable arithmetic oracle; not a JDK product benchmark. */
    private static long hotLoop(int seed) {
        long value = seed ^ 0x9e3779b97f4a7c15L;
        for (int i = 0; i < 64; i++) {
            value ^= value >>> 13;
            value *= 0xff51afd7ed558ccdL;
            value += i ^ seed;
        }
        return value;
    }

    public static void main(String[] args) {
        if (args.length != 1 || !"--hot".equals(args[0])) {
            throw new IllegalArgumentException("only the sealed HotSpot oracle is executable");
        }
        long digest = 0L;
        for (int i = 0; i < 80_000; i++) {
            digest = Long.rotateLeft(digest, 3) ^ hotLoop(i);
        }
        System.out.println(DIGEST + Long.toUnsignedString(digest, 16));
    }
}
