// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
// Product-specific, read-only M3JDK21 receiver. Synexia owns the source recipe.
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Bind a qualified Synexia ordered-convergence proposal to the unchanged, exact M3JDK21 source
 * revision. No target source mutation or product-gate promotion occurs in this process.
 *
 * Source receipt format: M3_ORDERED_CONVERGENCE_HANDOFF_V1, emitted by the Synexia Maven
 * m3-ordered-convergence goal. JSON parsing and a second recipe engine are deliberately avoided.
 */
public final class M3OrderedConvergenceReceiver {
    static final String SCHEMA = "M3_ORDERED_CONVERGENCE_HANDOFF_V1";
    static final String REPOSITORY = "hsoliwal/com.synexia";
    static final String POLICY = "SYNEXIA_FIRST_PARTY_APACHE2_V1";
    static final String HEADER = "kind\tname\tvalue\tevidence";
    static final int MAX_HANDOFF_BYTES = 64 * 1024;
    static final int MAX_SOURCE_BYTES = 16 * 1024 * 1024;
    static final List<String> STAGES = List.of(
            "INVENTORY", "DATED_ADDITIVE_CONVERGENCE", "PROBLEM_CATEGORY_CATALOGUE",
            "INLINE", "ATOMIZE", "PATTERNIZE", "COMMENT", "COMPILE_TEST_GATES", "PR");
    static final List<String> FIELDS = List.of("schema", "sourceRepository", "sourceRevision",
            "sourcePath", "sourcePreimage", "sourceCandidate", "datedRoot", "categoryRoot",
            "refinementRoot", "outcome", "licensePolicy", "planRoot");
    private static final String[] PENDING = {
            "DIFF", "LINTER", "COMPILE", "JUNIT", "JTREG", "JNI_PARITY",
            "HOTSPOT_GC_JIT_CDS", "RUNTIME", "API_ABI", "LICENSE", "REVIEWABLE_PR"};
    private M3OrderedConvergenceReceiver() { }

    record Stage(String name, String state, String evidence) { }
    record Packet(Map<String, String> values, List<Stage> stages, String digest) {
        String value(String name) { return values.get(name); }
    }
    public record Receipt(String targetHead, String targetBlobSha256, String sourceHead,
                          String sourceSha256, String candidateSha256, String packetSha256,
                          String targetPath, String targetPlane, String root) { }

    static void require(boolean condition, String reason) {
        if (!condition) throw new IllegalArgumentException("M3_ORDERED_RECEIVER_HELD:" + reason);
    }

    static String sha(byte[] bytes) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    static String checkedSha(String value) {
        require(value != null && value.matches("[0-9a-f]{64}"), "invalid sha256");
        return value;
    }

    static String checkedCommit(String value) {
        require(value != null && value.matches("[0-9a-f]{40}"), "invalid full source commit");
        return value;
    }

    static String checkedPath(String value) {
        require(value != null && !value.isEmpty() && !value.startsWith("/")
                && !value.contains("\\") && !value.contains("//")
                && value.indexOf(':') == -1 && value.length() <= 1024,
                "invalid relative path");
        for (String part : value.split("/", -1)) {
            require(!part.isEmpty() && !part.equals(".") && !part.equals("..")
                    && !part.equals(".git"), "path escape");
        }
        require(value.chars().noneMatch(c -> c < 32 || c == 127), "path control character");
        return value;
    }

    static Packet parse(byte[] bytes, String expectedSha) {
        require(bytes.length <= MAX_HANDOFF_BYTES, "oversized handoff");
        String digest = sha(bytes);
        require(digest.equals(checkedSha(expectedSha)), "source receipt hash mismatch");
        String text = new String(bytes, StandardCharsets.UTF_8);
        require(Arrays.equals(text.getBytes(StandardCharsets.UTF_8), bytes), "non-UTF8 handoff");
        require(text.endsWith("\n") && !text.contains("\r"), "noncanonical line endings");
        String[] lines = text.split("\n", -1);
        require(lines.length == 1 + FIELDS.size() + STAGES.size() + 1, "missing or extra stage rows");
        require(HEADER.equals(lines[0]) && lines[lines.length - 1].isEmpty(), "handoff header/trailer");
        java.util.LinkedHashMap<String, String> values = new java.util.LinkedHashMap<>();
        java.util.LinkedHashMap<String, String> anchors = new java.util.LinkedHashMap<>();
        for (int index = 0; index < FIELDS.size(); index++) {
            String[] cells = cells(lines[index + 1]);
            String key = FIELDS.get(index);
            require("META".equals(cells[0]) && key.equals(cells[1]), "metadata order: " + key);
            require(!cells[2].isBlank() && !cells[3].isBlank(), "missing metadata: " + key);
            values.put(key, cells[2]);
            anchors.put(key, cells[3]);
        }
        require(SCHEMA.equals(values.get("schema")), "handoff schema mismatch");
        require(REPOSITORY.equals(values.get("sourceRepository")), "wrong canonical donor repository");
        checkedCommit(values.get("sourceRevision"));
        require(checkedPath(values.get("sourcePath")).endsWith(".java"), "source must be Java");
        for (String key : List.of("sourcePreimage", "sourceCandidate", "datedRoot", "categoryRoot",
                                  "refinementRoot", "planRoot")) {
            checkedSha(values.get(key));
        }
        require(POLICY.equals(values.get("licensePolicy")), "unverified source license lane");
        require(Set.of("UNCHANGED", "AWAITING_VERIFICATION").contains(values.get("outcome")),
                "source recipe was held or promoted without proof");
        for (String field : FIELDS) checkedSha(anchors.get(field));
        require(anchors.get("schema").equals(values.get("planRoot"))
                && anchors.get("outcome").equals(values.get("planRoot"))
                && anchors.get("licensePolicy").equals(values.get("planRoot"))
                && anchors.get("planRoot").equals(values.get("planRoot")), "plan receipt root drift");
        for (String field : List.of("sourcePreimage", "sourceCandidate", "datedRoot",
                                    "categoryRoot", "refinementRoot")) {
            require(anchors.get(field).equals(values.get(field)), "hash field mismatch: " + field);
        }
        require(anchors.get("sourcePath").equals(values.get("sourcePreimage")),
                "source path preimage binding drift");
        require(anchors.get("sourceRevision").equals(values.get("datedRoot"))
                && anchors.get("sourceRepository").equals(values.get("datedRoot")),
                "Git ancestry root drift");
        ArrayList<Stage> stages = new ArrayList<>();
        for (int index = 0; index < STAGES.size(); index++) {
            String[] cells = cells(lines[1 + FIELDS.size() + index]);
            require(cells[0].equals("STAGE") && cells[1].equals(STAGES.get(index)),
                    "stage order: " + STAGES.get(index));
            if (index < 7) {
                require(cells[2].equals("PASSED"), "unproved source phase: " + cells[1]);
                checkedSha(cells[3]);
            } else if (index == 7) {
                require(cells[2].equals("PENDING"), "source build gates must remain pending");
                checkedSha(cells[3]);
            } else {
                require(cells[2].equals("PENDING") && cells[3].equals("-"),
                        "source PR must remain unpromoted");
            }
            stages.add(new Stage(cells[1], cells[2], cells[3]));
        }
        require(stages.get(1).evidence().equals(values.get("datedRoot")), "dated gate evidence drift");
        require(stages.get(2).evidence().equals(values.get("categoryRoot")), "category gate evidence drift");
        return new Packet(Map.copyOf(values), List.copyOf(stages), digest);
    }

    private static String[] cells(String line) {
        String[] cells = line.split("\t", -1);
        require(cells.length == 4 && Arrays.stream(cells).noneMatch(String::isBlank),
                "missing or malformed TSV field");
        return cells;
    }

    static Path regular(Path file) throws IOException {
        require(Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                && !Files.isSymbolicLink(file), "not regular source file: " + file);
        return file.toRealPath();
    }

    static byte[] bounded(Path file) throws IOException {
        regular(file);
        require(Files.size(file) <= MAX_SOURCE_BYTES, "source byte budget");
        return Files.readAllBytes(file);
    }

    static byte[] git(Path repo, String... args) throws Exception {
        List<String> command = new ArrayList<>(List.of("git", "-C", repo.toString(), "--no-pager"));
        command.addAll(Arrays.asList(args));
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.environment().put("GIT_OPTIONAL_LOCKS", "0");
        builder.environment().put("GIT_NO_REPLACE_OBJECTS", "1");
        builder.environment().put("GIT_TERMINAL_PROMPT", "0");
        builder.redirectErrorStream(true);
        Process process = builder.start();
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (var stream = process.getInputStream()) {
                byte[] buffer = new byte[8192];
                int n;
                while ((n = stream.read(buffer)) != -1) {
                    require(output.size() + n <= MAX_SOURCE_BYTES,
                            "Git command output budget exceeded");
                    output.write(buffer, 0, n);
                }
            }
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException("Git command timeout");
            }
            require(process.exitValue() == 0, "Git verification failed: " + args[0]);
            return output.toByteArray();
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }

    private static String text(byte[] data) {
        return new String(data, StandardCharsets.UTF_8).strip();
    }

    private static void cleanGitFile(Path repo, String path, String commit) throws Exception {
        require(text(git(repo, "rev-parse", "--verify", "HEAD^{commit}")).equals(commit),
                "revision moved");
        require(git(repo, "status", "--porcelain=v1", "--", path).length == 0,
                "uncommitted file or index state: " + path);
    }

    static String plane(String path) {
        if (path.startsWith("test/") && path.endsWith(".java")) return "JTREG_TEST";
        if (path.startsWith("m3/") && path.endsWith(".java")) return "JDK_RECEIVER_TOOLING";
        if (path.startsWith("src/") && path.endsWith(".java")) return "JDK_JAVA_OWNER";
        if (path.startsWith("src/") && (path.endsWith(".cpp") || path.endsWith(".c")
                || path.endsWith(".hpp") || path.endsWith(".h"))) return "JDK_NATIVE_JNI_HOTSPOT";
        throw new IllegalArgumentException("M3_ORDERED_RECEIVER_HELD:unapproved target source plane");
    }

    /** Strictly evidence-only even for native and HotSpot targets; never auto-relicense source. */
    public static Receipt receive(Path synexiaRepo, Path m3jdkRepo, Path handoff,
            String expectedHandoffSha, String expectedSourceCommit, Path candidate,
            String targetPath, String expectedTargetPreimage, String targetBaseRef) throws Exception {
        Path sourceRoot = synexiaRepo.toRealPath();
        Path targetRoot = m3jdkRepo.toRealPath();
        require(!sourceRoot.equals(targetRoot), "donor and target must be distinct repositories");
        String sourceCommit = checkedCommit(expectedSourceCommit);
        String targetRel = checkedPath(targetPath);
        String targetSha = checkedSha(expectedTargetPreimage);
        String targetPlane = plane(targetRel);
        Packet packet = parse(bounded(handoff), expectedHandoffSha);
        require(packet.value("sourceRevision").equals(sourceCommit), "donor revision pin mismatch");
        Path sourceFile = sourceRoot.resolve(packet.value("sourcePath")).normalize();
        Path targetFile = targetRoot.resolve(targetRel).normalize();
        require(sourceFile.startsWith(sourceRoot) && targetFile.startsWith(targetRoot),
                "source/target path escapes repository");
        require(regular(sourceFile).startsWith(sourceRoot), "donor source symlink/escape");
        require(regular(targetFile).startsWith(targetRoot), "target source symlink/escape");
        require(!candidate.toRealPath().equals(targetFile.toRealPath()),
                "candidate may not alias target runtime source");
        require(sha(bounded(sourceFile)).equals(packet.value("sourcePreimage")),
                "donor exact preimage changed");
        require(sha(bounded(candidate)).equals(packet.value("sourceCandidate")),
                "donor candidate digest changed");
        require(sha(bounded(targetFile)).equals(targetSha), "target file has changed");
        require(sha(git(sourceRoot, "show", sourceCommit + ":" + packet.value("sourcePath")))
                .equals(packet.value("sourcePreimage")), "donor Git object does not match packet");
        require(sha(git(targetRoot, "show", "HEAD:" + targetRel)).equals(targetSha),
                "target Git object differs from expected preimage");
        cleanGitFile(sourceRoot, packet.value("sourcePath"), sourceCommit);
        String targetCommit = checkedCommit(text(git(targetRoot, "rev-parse", "--verify", "HEAD^{commit}")));
        cleanGitFile(targetRoot, targetRel, targetCommit);
        String base = targetBaseRef;
        require(base != null && base.matches("[0-9a-f]{40}"), "target base must be full commit");
        git(targetRoot, "merge-base", "--is-ancestor", base, targetCommit);
        String deletions = text(git(targetRoot, "log", "--format=%H", "--diff-filter=D",
                base + ".." + targetCommit, "--", targetRel));
        require(deletions.isEmpty(), "target branch dropped file during dated convergence");
        String root = sha(String.join("\n", SCHEMA, packet.digest(), sourceCommit,
                packet.value("planRoot"), targetCommit, targetRel, targetSha, targetPlane,
                packet.value("sourceCandidate"), packet.value("categoryRoot"),
                packet.value("refinementRoot")).getBytes(StandardCharsets.UTF_8));
        return new Receipt(targetCommit, targetSha, sourceCommit,
                packet.value("sourcePreimage"), packet.value("sourceCandidate"),
                packet.digest(), targetRel, targetPlane, root);
    }

    public static void write(Path out, Path repo, Receipt receipt) throws IOException {
        Path root = out.toAbsolutePath().normalize();
        require(!root.startsWith(repo.toRealPath()), "receipt destination inside target repository");
        require(!Files.exists(root, LinkOption.NOFOLLOW_LINKS), "receipt destination already exists");
        Files.createDirectories(root);
        StringBuilder tsv = new StringBuilder("kind\tname\tvalue\n");
        for (String[] item : new String[][] {
                {"SOURCE", "synexiaRevision", receipt.sourceHead()},
                {"SOURCE", "preimageSha256", receipt.sourceSha256()},
                {"SOURCE", "candidateSha256", receipt.candidateSha256()},
                {"SOURCE", "handoffSha256", receipt.packetSha256()},
                {"TARGET", "commit", receipt.targetHead()},
                {"TARGET", "path", receipt.targetPath()},
                {"TARGET", "preimageSha256", receipt.targetBlobSha256()},
                {"TARGET", "plane", receipt.targetPlane()},
                {"RECEIPT", "root", receipt.root()},
                {"RECEIPT", "runtimeAccepted", "false"},
                {"RECEIPT", "sourceMutation", "false"},
                {"RECEIPT", "automaticSourceCopy", "false"}}) {
            tsv.append(String.join("\t", item)).append('\n');
        }
        for (String gate : PENDING) tsv.append("GATE\t").append(gate).append("\tPENDING\n");
        Files.writeString(root.resolve("M3JDK_ORDERED_RECEIVING.tsv"), tsv.toString(),
                StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        String note = "# M3JDK21 ordered-convergence receiving hold\n\n"
                + "Source commit: " + receipt.sourceHead() + "\n"
                + "Exact target: " + receipt.targetPath() + " at "
                + receipt.targetHead() + "\n"
                + "Receipt: " + receipt.root() + "\n\n"
                + "This receiver did not modify any source. The candidate is Synexia-owned"
                + " recipe evidence, not automatically licensed/adapted JDK code. "
                + "Compile, tests, jtreg, ABI/API, JNI, JIT, GC, CDS, native and runtime "
                + "must each be verified by their current M3JDK21 owners before promotion.\n";
        Files.writeString(root.resolve("PR_PROMOTION_HOLD.md"), note,
                StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 10) {
            throw new IllegalArgumentException("Usage: M3OrderedConvergenceReceiver "
                    + "<synexia-repo> <m3jdk-repo> <synexia-handoff.tsv> <handoff-sha256> "
                    + "<source-commit> <candidate-file> <target-relative-path> "
                    + "<target-preimage-sha256> <target-base-commit> <output-dir>");
        }
        Receipt receipt = receive(Path.of(args[0]), Path.of(args[1]), Path.of(args[2]),
                args[3], args[4], Path.of(args[5]), args[6], args[7], args[8]);
        write(Path.of(args[9]), Path.of(args[1]), receipt);
        System.out.println("M3_ORDERED_RECEIVER_CUSTODY_ONLY " + receipt.root());
    }
}
