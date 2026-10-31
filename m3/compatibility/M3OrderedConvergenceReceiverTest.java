// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
// Isolated Java 21 protocol tests; does not claim actual Synexia/JDK product acceptance.
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class M3OrderedConvergenceReceiverTest {
    private static int assertions;
    private static final String SOURCE_FILE = "src/synexia/Example.java";
    private static final String TARGET_FILE = "src/java.base/share/classes/java/lang/M3Example.java";
    private static final String SOURCE = "package synexia; public class Example { int f() { return 1; } }\n";
    private static final String CANDIDATE = "package synexia; public class Example { int f() { return 1 + 0; } }\n";
    private static final String TARGET = "package java.lang; class M3Example { int f() { return 1; } }\n";
    private static final String ROOT = "a".repeat(64);
    private static final String DATED = "b".repeat(64);
    private static final String CATEGORY = "c".repeat(64);
    private static final String REFINEMENT = "d".repeat(64);
    private static final String PROOF = "e".repeat(64);

    private M3OrderedConvergenceReceiverTest() { }
    interface Action { void run() throws Exception; }

    private static void truth(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }

    private static void refuse(Action action, String cause) throws Exception {
        boolean held = false;
        try { action.run(); }
        catch (IllegalArgumentException expected) {
            held = true;
            truth(expected.getMessage().contains("M3_ORDERED_RECEIVER_HELD"), cause + " wrong refusal");
        }
        truth(held, "Failed open: " + cause);
    }

    private static void write(Path root, String path, String data) throws Exception {
        Path file = root.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, data, StandardCharsets.UTF_8);
    }

    private static String git(Path repo, String... args) throws Exception {
        String[] command = new String[args.length + 3];
        command[0] = "git";
        command[1] = "-C";
        command[2] = repo.toString();
        System.arraycopy(args, 0, command, 3, args.length);
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (process.waitFor() != 0) {
            throw new AssertionError("git fixture " + String.join(" ", args) + ": " + output);
        }
        return output;
    }

    private static String init(Path path, String file, String data) throws Exception {
        Files.createDirectories(path);
        git(path, "init");
        git(path, "config", "user.email", "m3@example.invalid");
        git(path, "config", "user.name", "M3 Fixture");
        write(path, file, data);
        git(path, "add", file);
        git(path, "commit", "-m", "baseline");
        return git(path, "rev-parse", "HEAD");
    }

    private static String packet(String sourceSha, String candidateSha, String sourceCommit) {
        StringBuilder lines = new StringBuilder(M3OrderedConvergenceReceiver.HEADER).append('\n');
        add(lines, "META", "schema", M3OrderedConvergenceReceiver.SCHEMA, ROOT);
        add(lines, "META", "sourceRepository", M3OrderedConvergenceReceiver.REPOSITORY, DATED);
        add(lines, "META", "sourceRevision", sourceCommit, DATED);
        add(lines, "META", "sourcePath", SOURCE_FILE, sourceSha);
        add(lines, "META", "sourcePreimage", sourceSha, sourceSha);
        add(lines, "META", "sourceCandidate", candidateSha, candidateSha);
        add(lines, "META", "datedRoot", DATED, DATED);
        add(lines, "META", "categoryRoot", CATEGORY, CATEGORY);
        add(lines, "META", "refinementRoot", REFINEMENT, REFINEMENT);
        add(lines, "META", "outcome", "AWAITING_VERIFICATION", ROOT);
        add(lines, "META", "licensePolicy", M3OrderedConvergenceReceiver.POLICY, ROOT);
        add(lines, "META", "planRoot", ROOT, ROOT);
        for (int i = 0; i < M3OrderedConvergenceReceiver.STAGES.size(); i++) {
            String key = M3OrderedConvergenceReceiver.STAGES.get(i);
            add(lines, "STAGE", key, i < 7 ? "PASSED" : "PENDING",
                    i == 1 ? DATED : i == 2 ? CATEGORY : i == 7 ? PROOF : i == 8 ? "-" : ROOT);
        }
        return lines.toString();
    }

    private static void add(StringBuilder out, String a, String b, String c, String d) {
        out.append(a).append('\t').append(b).append('\t').append(c).append('\t').append(d).append('\n');
    }

    private static class Fixture {
        final Path source, target, candidate, packet;
        final String sourceCommit, targetCommit, targetSha, packetSha;
        Fixture() throws Exception {
            Path home = Files.createTempDirectory("m3-ordered-receiver-");
            source = home.resolve("source");
            target = home.resolve("target");
            candidate = home.resolve("candidate.java");
            packet = home.resolve("handoff.tsv");
            sourceCommit = init(source, SOURCE_FILE, SOURCE);
            targetCommit = init(target, TARGET_FILE, TARGET);
            targetSha = M3OrderedConvergenceReceiver.sha(TARGET.getBytes(StandardCharsets.UTF_8));
            Files.writeString(candidate, CANDIDATE, StandardCharsets.UTF_8);
            String content = packet(M3OrderedConvergenceReceiver.sha(SOURCE.getBytes(StandardCharsets.UTF_8)),
                    M3OrderedConvergenceReceiver.sha(CANDIDATE.getBytes(StandardCharsets.UTF_8)), sourceCommit);
            Files.writeString(packet, content, StandardCharsets.UTF_8);
            packetSha = M3OrderedConvergenceReceiver.sha(content.getBytes(StandardCharsets.UTF_8));
        }
        M3OrderedConvergenceReceiver.Receipt receive() throws Exception {
            return M3OrderedConvergenceReceiver.receive(source, target, packet,
                    packetSha, sourceCommit, candidate, TARGET_FILE, targetSha, targetCommit);
        }
    }

    private static void acceptance() throws Exception {
        Fixture fixture = new Fixture();
        var receipt = fixture.receive();
        truth(receipt.targetHead().equals(fixture.targetCommit), "exact target Git head");
        truth(receipt.sourceHead().equals(fixture.sourceCommit), "exact source Git head");
        truth(receipt.packetSha256().equals(fixture.packetSha), "exact packet");
        truth(receipt.targetPlane().equals("JDK_JAVA_OWNER"), "target owner plane");
        truth(receipt.root().matches("[0-9a-f]{64}"), "stable target receipt root");
        truth(fixture.receive().root().equals(receipt.root()), "repeat stable");
        Path out = fixture.target.getParent().resolve("receipt-out");
        M3OrderedConvergenceReceiver.write(out, fixture.target, receipt);
        String data = Files.readString(out.resolve("M3JDK_ORDERED_RECEIVING.tsv"));
        truth(data.contains("GATE\tJNI_PARITY\tPENDING"), "native pending");
        truth(data.contains("GATE\tJTREG\tPENDING"), "jtreg pending");
        truth(data.contains("RECEIPT\truntimeAccepted\tfalse"), "runtime not falsely accepted");
        truth(Files.readString(fixture.target.resolve(TARGET_FILE)).equals(TARGET), "target never modified");
        refuse(() -> M3OrderedConvergenceReceiver.write(out, fixture.target, receipt), "existing output");
    }

    private static void adversarial() throws Exception {
        Fixture fixture = new Fixture();
        byte[] originalPacket = Files.readAllBytes(fixture.packet);
        refuse(() -> M3OrderedConvergenceReceiver.parse(originalPacket, "0".repeat(64)), "packet digest mismatch");
        String text = Files.readString(fixture.packet);
        String forged = text.replace("COMPILE_TEST_GATES\tPENDING", "COMPILE_TEST_GATES\tPASSED");
        refuse(() -> M3OrderedConvergenceReceiver.parse(forged.getBytes(StandardCharsets.UTF_8),
                M3OrderedConvergenceReceiver.sha(forged.getBytes(StandardCharsets.UTF_8))), "faked build proof");
        String reordered = text.replace("INLINE\tPASSED", "PATTERNIZE\tPASSED");
        refuse(() -> M3OrderedConvergenceReceiver.parse(reordered.getBytes(StandardCharsets.UTF_8),
                M3OrderedConvergenceReceiver.sha(reordered.getBytes(StandardCharsets.UTF_8))), "stage order drift");
        String wrongRoot = text.replace("STAGE\tPROBLEM_CATEGORY_CATALOGUE\tPASSED\t"+CATEGORY,
                "STAGE\tPROBLEM_CATEGORY_CATALOGUE\tPASSED\t"+ROOT);
        refuse(() -> M3OrderedConvergenceReceiver.parse(wrongRoot.getBytes(StandardCharsets.UTF_8),
                M3OrderedConvergenceReceiver.sha(wrongRoot.getBytes(StandardCharsets.UTF_8))), "category evidence drift");
        refuse(() -> M3OrderedConvergenceReceiver.checkedPath("../src/X.java"), "traversal");
        refuse(() -> M3OrderedConvergenceReceiver.checkedPath("C:/src/X.java"), "drive path");
        refuse(() -> M3OrderedConvergenceReceiver.plane("src/java.base/share/classes/java/lang/String.txt"), "target type");
        Files.writeString(fixture.candidate, "compromised", StandardCharsets.UTF_8);
        refuse(fixture::receive, "candidate source drift");
        Files.writeString(fixture.candidate, CANDIDATE, StandardCharsets.UTF_8);
        Files.writeString(fixture.target.resolve(TARGET_FILE), TARGET + "//dirty\n", StandardCharsets.UTF_8);
        refuse(fixture::receive, "dirty working tree");
        Files.writeString(fixture.target.resolve(TARGET_FILE), TARGET, StandardCharsets.UTF_8);
        write(fixture.source, SOURCE_FILE, SOURCE + "//dirty\n");
        refuse(fixture::receive, "dirty Synexia source");
        write(fixture.source, SOURCE_FILE, SOURCE);
        refuse(() -> M3OrderedConvergenceReceiver.receive(fixture.source, fixture.target,
                fixture.packet, fixture.packetSha, fixture.sourceCommit, fixture.candidate,
                TARGET_FILE, fixture.targetSha, "0".repeat(40)), "missing target base");
        refuse(() -> M3OrderedConvergenceReceiver.receive(fixture.source, fixture.target,
                fixture.packet, fixture.packetSha, "0".repeat(40), fixture.candidate,
                TARGET_FILE, fixture.targetSha, fixture.targetCommit), "unapproved source revision");
        // Same branch ancestry does not excuse an old deletion even when target file was restored.
        git(fixture.target, "rm", TARGET_FILE);
        git(fixture.target, "commit", "-m", "remove target");
        write(fixture.target, TARGET_FILE, TARGET);
        git(fixture.target, "add", TARGET_FILE);
        git(fixture.target, "commit", "-m", "restore target");
        refuse(() -> M3OrderedConvergenceReceiver.receive(fixture.source, fixture.target,
                fixture.packet, fixture.packetSha, fixture.sourceCommit, fixture.candidate,
                TARGET_FILE, fixture.targetSha, fixture.targetCommit), "delete then restore");
    }

    public static void main(String[] args) throws Exception {
        truth(args.length == 0, "test takes no arguments");
        acceptance();
        adversarial();
        System.out.println("M3_ORDERED_RECEIVER_TESTS_PASS assertions=" + assertions);
    }
}
