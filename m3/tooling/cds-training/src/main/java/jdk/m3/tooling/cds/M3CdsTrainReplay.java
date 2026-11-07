/*
 * SPDX-FileCopyrightText: 2026 Synexia contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.tooling.cds;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * A bounded, opt-in JDK21 CDS training/deployment differential-proof tool.
 * Uses the existing HotSpot AppCDS mechanism without modifying the Java runtime.
 */
public final class M3CdsTrainReplay {
    /** Maximum duration permitted for one child process. */
    public static final Duration TIMEOUT = Duration.ofSeconds(60);
    /** Exact retained training result basename. */
    public static final String RECEIPT = "RECEIPT.tsv";
    /** Exact retained dynamic CDS archive basename. */
    public static final String ARCHIVE = "application.jsa";

    private M3CdsTrainReplay() { }

    /**
     * CLI: train four required arguments plus application arguments, or verify one receipt directory.
     *
     * @param args CLI arguments
     * @throws IOException when source/runtime inputs or process evidence cannot be read
     */
    public static void main(String[] args) throws IOException {
        if (args.length >= 5 && args[0].equals("train")) {
            var command = List.of(args).subList(5, args.length);
            Proof proof = train(Path.of(args[1]), Path.of(args[2]), args[3], Path.of(args[4]), command);
            System.out.println("M3_CDS_PROOF\tTRAIN_PASS\t" + proof.archiveSha256());
        } else if (args.length == 2 && args[0].equals("verify")) {
            Proof proof = verify(Path.of(args[1]));
            System.out.println("M3_CDS_PROOF\tVERIFY_PASS\t" + proof.archiveSha256());
        } else {
            throw new IllegalArgumentException("usage: train <java> <app.jar> <Main> <new-dir> [args...]"
                    + " | verify <existing-dir>");
        }
    }

    /**
     * Constructs a new archive and retains baseline/training/deployment byte-exact output evidence.
     * A failed candidate is never given a PASS receipt.
     *
     * @param java JDK21 java executable
     * @param jar immutable application jar
     * @param main main class
     * @param outputDir new, previously nonexistent output directory
     * @param args deterministic, noninteractive arguments
     * @return exact source and archive identity
     * @throws IOException on I/O or failed runtime proof
     */
    public static Proof train(Path java, Path jar, String main, Path outputDir, List<String> args)
            throws IOException {
        M3CdsIdentity.Plan plan = M3CdsIdentity.inspect(java, jar, main, args);
        Objects.requireNonNull(outputDir, "outputDir");
        Path dir = outputDir.toAbsolutePath().normalize();
        if (Files.exists(dir)) throw new IllegalArgumentException("output directory already exists");
        Path parent = dir.getParent();
        if (parent == null || !Files.isDirectory(parent)) {
            throw new IllegalArgumentException("output parent must already exist");
        }
        Files.createDirectory(dir);
        Path archive = dir.resolve(ARCHIVE);
        Path log = dir.resolve("classload.log");
        run("baseline", plan, dir, List.of("-Xshare:off"));
        run("training", plan, dir, List.of("-XX:ArchiveClassesAtExit=" + archive));
        if (!Files.isRegularFile(archive) || Files.size(archive) == 0) {
            throw new IllegalStateException("JDK21 training failed to generate an archive");
        }
        run("deployment", plan, dir, List.of("-Xshare:on",
                "-XX:SharedArchiveFile=" + archive,
                "-Xlog:class+load=info:file=" + log));
        verifyStages(dir, plan.mainClass(), "deployment", log);
        if (!M3CdsIdentity.inspect(plan.java(), plan.jar(), plan.mainClass(),
                plan.arguments()).fields().equals(plan.fields())) {
            throw new IllegalStateException("CDS JDK/application changed during training");
        }

        TreeMap<String, String> receipt = new TreeMap<>(plan.fields());
        receipt.put("archive_sha256", M3CdsIdentity.sha(archive));
        receipt.put("baseline_stdout_sha256", M3CdsIdentity.sha(dir.resolve("baseline.stdout")));
        receipt.put("baseline_stderr_sha256", M3CdsIdentity.sha(dir.resolve("baseline.stderr")));
        receipt.put("training_stdout_sha256", M3CdsIdentity.sha(dir.resolve("training.stdout")));
        receipt.put("training_stderr_sha256", M3CdsIdentity.sha(dir.resolve("training.stderr")));
        receipt.put("deployment_stdout_sha256", M3CdsIdentity.sha(dir.resolve("deployment.stdout")));
        receipt.put("deployment_stderr_sha256", M3CdsIdentity.sha(dir.resolve("deployment.stderr")));
        receipt.put("classload_log_sha256", M3CdsIdentity.sha(log));
        receipt.put("status", "PASS_BOUNDED_HOST_JDK21");
        Files.writeString(dir.resolve(RECEIPT), M3CdsIdentity.table(receipt), StandardCharsets.UTF_8);
        return new Proof(dir, receipt.get("archive_sha256"), plan.fields().get("plan_sha256"));
    }

    /**
     * Rechecks an immutable, receipt-sealed archive and replays its exact workload.
     * The verified original receipt is never modified by a verification run.
     *
     * @param outputDir retained, previously proven training directory
     * @return archive and plan identity
     * @throws IOException on source drift, missing logs, or failed runtime proof
     */
    public static Proof verify(Path outputDir) throws IOException {
        Objects.requireNonNull(outputDir, "outputDir");
        Path dir = outputDir.toRealPath();
        Map<String, String> receipt = M3CdsIdentity.parse(dir.resolve(RECEIPT));
        if (!"PASS_BOUNDED_HOST_JDK21".equals(receipt.get("status"))) {
            throw new IllegalArgumentException("no bounded PASS training receipt");
        }
        M3CdsIdentity.Plan plan = M3CdsIdentity.recheck(receipt);
        if (!M3CdsIdentity.sha(dir.resolve(ARCHIVE)).equals(receipt.get("archive_sha256"))) {
            throw new IllegalArgumentException("CDS archive source drift");
        }
        for (String stage : List.of("baseline", "training", "deployment")) {
            for (String stream : List.of("stdout", "stderr")) {
                String key = stage + "_" + stream + "_sha256";
                if (!M3CdsIdentity.sha(dir.resolve(stage + "." + stream)).equals(receipt.get(key))) {
                    throw new IllegalArgumentException("training evidence drift: " + key);
                }
            }
        }
        if (!M3CdsIdentity.sha(dir.resolve("classload.log")).equals(receipt.get("classload_log_sha256"))) {
            throw new IllegalArgumentException("dynamic-classload evidence drift");
        }
        Path checkLog = dir.resolve("replay.classload.log");
        run("replay", plan, dir, List.of("-Xshare:on", "-XX:SharedArchiveFile=" + dir.resolve(ARCHIVE),
                "-Xlog:class+load=info:file=" + checkLog));
        verifyStages(dir, plan.mainClass(), "replay", checkLog);
        return new Proof(dir, receipt.get("archive_sha256"), receipt.get("plan_sha256"));
    }

    private static void verifyStages(Path dir, String main, String deployment, Path log) throws IOException {
        for (String stage : List.of("baseline", "training", deployment)) {
            if (Files.mismatch(dir.resolve("baseline.stdout"), dir.resolve(stage + ".stdout")) != -1
                    || Files.mismatch(dir.resolve("baseline.stderr"), dir.resolve(stage + ".stderr")) != -1) {
                throw new IllegalStateException("observable stdout/stderr mismatch: " + stage);
            }
        }
        String loaded = Files.readString(log, StandardCharsets.UTF_8);
        boolean proof = loaded.lines().anyMatch(line -> line.contains("] " + main + " source:")
                && line.contains("shared objects file (top)"));
        if (!proof) throw new IllegalStateException("main class not loaded from the dynamic CDS archive");
    }

    private static void run(String stage, M3CdsIdentity.Plan plan, Path dir, List<String> vmArgs)
            throws IOException {
        List<String> command = new ArrayList<>();
        command.add(plan.java().toString());
        command.addAll(vmArgs);
        command.add("-cp");
        command.add(plan.jar().toString());
        command.add(plan.mainClass());
        command.addAll(plan.arguments());
        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(dir.toFile())
                .redirectOutput(dir.resolve(stage + ".stdout").toFile())
                .redirectError(dir.resolve(stage + ".stderr").toFile());
        for (String key : List.of("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS")) {
            builder.environment().remove(key);
        }
        Process process = builder.start();
        process.getOutputStream().close();
        boolean finished;
        try {
            finished = process.waitFor(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException("CDS stage interrupted: " + stage, exception);
        }
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("CDS stage timed out: " + stage);
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("CDS stage " + stage + " exited " + process.exitValue()
                    + "; retained stderr at " + dir.resolve(stage + ".stderr"));
        }
    }

    /**
     * Immutable receipt identity of one admitted bounded host CDS archive.
     *
     * @param directory owned archive and proof-log directory
     * @param archiveSha256 exact trained archive bytes identity
     * @param planSha256 exact identity of inputs, VM image and arguments
     */
    public record Proof(Path directory, String archiveSha256, String planSha256) { }
}
