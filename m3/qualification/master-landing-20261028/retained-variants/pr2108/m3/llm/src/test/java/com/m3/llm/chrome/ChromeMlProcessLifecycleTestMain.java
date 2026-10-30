// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package com.m3.llm.chrome;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Real Linux subprocess tests; Python fixtures are not ChromeML or physical GPU evidence. */
public final class ChromeMlProcessLifecycleTestMain {
    private static int checks;
    private static final String TRAILER = "\ndestroyed_session=1\ndestroyed_model=1\n";
    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private record Fixture(Path root, ChromeMlRuntimeLayout layout, ChromeMlRuntimeLayout.ExpectedHashes hashes) {}
    private ChromeMlProcessLifecycleTestMain() {}

    public static void main(String[] args) throws Exception {
        if (!Files.isExecutable(Path.of("/usr/bin/python3"))) {
            throw new IllegalStateException("Tests require /usr/bin/python3; no silent skip");
        }
        Path root = Files.createTempDirectory("chromeml-lifecycle-");
        try {
            parserCases();
            captureCases();
            Fixture ok = fixture(root.resolve("ok"), "import sys\nprint('generate_output status=0 text=hello tool_calls=0')\n"
                    + "print('generate_wait=0 complete_status=1 full_text=hello')\nprint('destroyed_session=1')\nprint('destroyed_model=1')\n");
            try (var runner = runner(ok, Duration.ofSeconds(3), 4096)) {
                var tokens = new ArrayList<String>();
                check("real-process-completion", runner.generate(request(), tokens::add).text().equals("hello"));
                check("stream-token", tokens.equals(List.of("hello")));
                check("successful-instance-reusable", runner.generate(request()).completeStatus().equals(1));
                check("runtime-root-retained", runner.runtimeIdentity().equals(ok.layout().validate(ok.hashes())));
            }
            var closed = runner(ok, Duration.ofSeconds(1), 4096);
            closed.close();
            expect("closed-instance", "RUNNER_CLOSED", () -> closed.generate(request()));
            expect("duration-ceiling-exact", "timeout", () -> runner(ok, Duration.ofMinutes(30).plusMillis(1), 4096));
            expect("capture-ceiling", "maxCaptureBytes", () -> runner(ok, Duration.ofSeconds(1), 100));
            processFailure(root, "long-line", "import os,time\nos.write(1,b'x'*8192)\ntime.sleep(30)\n", "RUNNER_OUTPUT_LIMIT", 2048);
            processFailure(root, "stderr-overflow", "import os,time\nos.write(2,b'x'*8192)\ntime.sleep(30)\n", "RUNNER_OUTPUT_LIMIT", 2048);
            processFailure(root, "invalid-utf8", "import os,time\nos.write(1,b'\\xff\\n')\ntime.sleep(30)\n", "RUNNER_INVALID_UTF8", 4096);
            processFailure(root, "nonzero", "import sys\nsys.exit(7)\n", "RUNNER_EXIT_7", 4096);
            processFailure(root, "no-completion", "print('full_text=not completion')\n", "RUNNER_COMPLETION_REJECTED", 4096);
            processFailure(root, "native-wait-timeout", "print('generate_wait=258 complete_status=1 full_text=x')\nprint('destroyed_session=1')\nprint('destroyed_model=1')\n", "RUNNER_COMPLETION_REJECTED", 4096);
            Fixture stall = fixture(root.resolve("stall"), "import time\ntime.sleep(30)\n");
            try (var runner = runner(stall, Duration.ofMillis(200), 4096)) {
                long start = System.nanoTime();
                expect("process-timeout", "RUNNER_TIMEOUT", () -> runner.generate(request()));
                check("timeout-includes-drain", System.nanoTime() - start < TimeUnit.SECONDS.toNanos(2));
            }
            try (var runner = runner(ok, Duration.ofSeconds(3), 4096)) {
                expect("sink-failure-propagates", "sink-stop", () -> runner.generate(request(), token -> { throw new IOException("sink-stop"); }));
                check("sink-failure-retires", runner.isClosed());
            }
            try (var runner = runner(ok, Duration.ofMillis(200), 4096)) {
                long start = System.nanoTime();
                expect("callback-timeout", "RUNNER_TIMEOUT", () -> runner.generate(request(), token -> {
                    try { Thread.sleep(30_000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                }));
                check("callback-timeout-bounded", System.nanoTime() - start < TimeUnit.SECONDS.toNanos(2));
            }
            cancellation(ok);
            Fixture mutation = fixture(root.resolve("mutation"), "print('unused')\n");
            try (var runner = runner(mutation, Duration.ofSeconds(1), 4096)) {
                Files.writeString(mutation.layout().weights(), "changed-length-assets");
                expect("asset-change-before-run", "FILE_CHANGED_DURING_READ", () -> runner.generate(request()));
                check("asset-change-retires", runner.isClosed());
            }
            Fixture argv = fixture(root.resolve("argv"), "import sys,json\nfrom pathlib import Path\nPath('argv.json').write_text(json.dumps(sys.argv[1:]))\n"
                    + "print('generate_wait=0 complete_status=1 full_text=x')\nprint('destroyed_session=1')\nprint('destroyed_model=1')\n");
            try (var runner = runner(argv, Duration.ofSeconds(2), 8192)) {
                runner.generate(new ChromeMlRunnerRequest("hello; $(no-shell)", List.of(Path.of("/one image"), Path.of("/two")),
                        Path.of("/sound"), 16000, 64, 2048, 2, 0.5, true, true, true));
                String recorded = Files.readString(argv.root().resolve("argv.json"));
                check("gpu-flag-preserved", recorded.contains("\"--gpu\""));
                check("no-shell-prompt", recorded.contains("\"hello; $(no-shell)\""));
                check("media-and-options-preserved", recorded.contains("\"--audio\"") && recorded.contains("\"/one image\"")
                        && recorded.contains("\"--context-tokens\"") && recorded.contains("\"--top-k\"")
                        && recorded.contains("\"--temperature\"") && recorded.contains("\"--audio-target-rate\""));
            }
            probeCases(ok);
            System.out.println("PASS " + checks + " named checks; physical GPU/ChromeML NOT_EXECUTED");
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) { Files.delete(path); }
            }
        }
    }

    private static void parserCases() throws Exception {
        check("completion-status-one", ChromeMlRunnerOutputParser.parseCompleted("generate_wait=0 complete_status=1 full_text=x" + TRAILER).text().equals("x"));
        check("multiline-final-preserved", ChromeMlRunnerOutputParser.parseCompleted("generate_wait=0 complete_status=1 full_text=a\n\nb\n" + TRAILER).text().equals("a\n\nb\n"));
        check("crlf-protocol", ChromeMlRunnerOutputParser.parseCompleted(("generate_wait=0 complete_status=1 full_text=x" + TRAILER).replace("\n", "\r\n")).text().equals("x"));
        check("legacy-extraction-retained", ChromeMlRunnerOutputParser.parseFullText("full_text=legacy\n").text().equals("legacy"));
        expect("fallback-not-proof", "RUNNER_COMPLETION_MISSING", () -> ChromeMlRunnerOutputParser.parseCompleted("full_text=legacy\n"));
        expect("incomplete-status", "RUNNER_INFERENCE_NOT_COMPLETED", () -> ChromeMlRunnerOutputParser.parseCompleted("generate_wait=0 complete_status=0 full_text=x" + TRAILER));
        expect("invalid-constraint-status", "RUNNER_INFERENCE_NOT_COMPLETED", () -> ChromeMlRunnerOutputParser.parseCompleted("generate_wait=0 complete_status=2 full_text=x" + TRAILER));
        expect("missing-cleanup", "RUNNER_CLEANUP_RECORDS_MISSING", () -> ChromeMlRunnerOutputParser.parseCompleted("generate_wait=0 complete_status=1 full_text=x\n"));
        expect("ambiguous-records", "RUNNER_COMPLETION_AMBIGUOUS", () -> ChromeMlRunnerOutputParser.parseCompleted("generate_wait=0 complete_status=1 full_text=x\ngenerate_wait=0 complete_status=1 full_text=y" + TRAILER));
    }

    private static void captureCases() throws Exception {
        var exact = new ChromeMlBoundedCapture(new ByteArrayInputStream("abcd".getBytes(StandardCharsets.UTF_8)), 4, null);
        exact.run(); check("exact-byte-limit", exact.failure() == null && exact.text().equals("abcd"));
        var excess = new ChromeMlBoundedCapture(new ByteArrayInputStream(new byte[5]), 4, null);
        excess.run(); check("one-byte-over-limit", excess.failure().getMessage().equals("RUNNER_OUTPUT_LIMIT"));
        var utf8 = new ChromeMlBoundedCapture(new ByteArrayInputStream(new byte[]{(byte) 0xc3}), 4, null);
        utf8.run(); check("truncated-utf8", utf8.failure().getMessage().equals("RUNNER_INVALID_UTF8"));
        var unicode = new ChromeMlBoundedCapture(new ByteArrayInputStream("हिंदी🙂".getBytes(StandardCharsets.UTF_8)), 128, null);
        unicode.run(); check("unicode-preserved", unicode.text().equals("हिंदी🙂"));
    }

    private static void cancellation(Fixture fixture) throws Exception {
        var runner = runner(fixture, Duration.ofSeconds(5), 4096);
        CountDownLatch callback = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread thread = Thread.ofPlatform().start(() -> {
            try { runner.generate(request(), token -> {
                callback.countDown();
                try { release.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }); } catch (Throwable e) { error.set(e); }
        });
        try {
            check("active-callback-entered", callback.await(2, TimeUnit.SECONDS));
            expect("concurrent-call-rejected", "RUNNER_BUSY", () -> runner.generate(request()));
            runner.close();
            thread.join(2_000);
            check("close-cancels-active", !thread.isAlive() && (error.get() instanceof InterruptedException
                    || (error.get() instanceof IOException && "RUNNER_CLOSED".equals(error.get().getMessage()))));
            check("close-retired", runner.isClosed());
        } finally { runner.close(); release.countDown(); thread.join(2_000); }
    }

    private static void probeCases(Fixture f) throws Exception {
        var env = environment(f);
        var config = ChromeMlRuntimeProbe.configuration(env);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (var out = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            ChromeMlRuntimeProbe.audit(config, out);
            String audit = bytes.toString(StandardCharsets.UTF_8);
            check("audit-no-inference-claim", audit.contains("BYTES_MATCH_ONLY") && audit.contains("inferenceExecuted=false"));
            check("audit-no-secret", !audit.contains(env.get("CHROMEML_API_TOKEN")));
            bytes.reset();
            ChromeMlRuntimeProbe.smoke(config, out);
            String smoke = bytes.toString(StandardCharsets.UTF_8);
            check("synthetic-protocol-completion", smoke.contains("INFERENCE_COMPLETED") && smoke.contains("outputSha256="));
            check("no-false-gpu-attestation", smoke.contains("gpuVerified=false"));
        }
        var relative = new HashMap<>(env); relative.put("CHROMEML_RUNNER", "relative.exe");
        expect("explicit-paths-required", "EXPLICIT_ABSOLUTE_PATH_REQUIRED", () -> ChromeMlRuntimeProbe.configuration(relative));
        var missing = new HashMap<>(env); missing.remove("CHROMEML_WEIGHTS_SHA256");
        expect("all-hashes-required", "CHROMEML_WEIGHTS_SHA256", () -> ChromeMlRuntimeProbe.configuration(missing));
        var weak = new HashMap<>(env); weak.put("CHROMEML_API_TOKEN", "weak");
        expect("production-token-required", "strong bearer token", () -> ChromeMlRuntimeProbe.configuration(weak));
        var remote = new HashMap<>(env); remote.put("CHROMEML_API_HOST", "0.0.0.0");
        expect("loopback-only", "numeric loopback", () -> ChromeMlRuntimeProbe.configuration(remote));
        expect("native-execution-opt-in", "use --audit", () -> ChromeMlRuntimeProbe.main(new String[]{"--smoke"}));
        expect("native-windows-only", "WINDOWS_CHROMEML_RUNTIME_REQUIRED", () -> ChromeMlRuntimeProbe.main(new String[]{"--smoke", "--execute-native"}));
    }

    private static Map<String, String> environment(Fixture f) {
        return Map.ofEntries(Map.entry("CHROMEML_ROOT", f.root().toString()),
                Map.entry("CHROMEML_RUNNER", f.layout().runner().toString()), Map.entry("CHROMEML_DLL", f.layout().chromeMlDll().toString()),
                Map.entry("CHROMEML_DAWN_DLL", f.layout().dawnDll().toString()), Map.entry("CHROMEML_WEIGHTS", f.layout().weights().toString()),
                Map.entry("CHROMEML_RUNNER_SHA256", f.hashes().runner()), Map.entry("CHROMEML_DLL_SHA256", f.hashes().chromeMlDll()),
                Map.entry("CHROMEML_DAWN_DLL_SHA256", f.hashes().dawnDll()), Map.entry("CHROMEML_WEIGHTS_SHA256", f.hashes().weights()),
                Map.entry("CHROMEML_API_TOKEN", "synthetic-test-token-0123456789-abcdef"));
    }

    private static void processFailure(Path root, String id, String code, String expected, int limit) throws Exception {
        Fixture f = fixture(root.resolve(id), code);
        try (var runner = runner(f, Duration.ofSeconds(3), limit)) {
            long start = System.nanoTime();
            expect(id, expected, () -> runner.generate(request()));
            check(id + "-fail-fast", System.nanoTime() - start < TimeUnit.SECONDS.toNanos(2));
            check(id + "-retired", runner.isClosed());
        }
    }

    private static Fixture fixture(Path root, String code) throws IOException {
        Files.createDirectories(root);
        Path exe = root.resolve("runner.py");
        Files.writeString(exe, "#!/usr/bin/python3\n" + code, StandardCharsets.UTF_8);
        Files.setPosixFilePermissions(exe, PosixFilePermissions.fromString("rwx------"));
        Path dll = root.resolve("chrome.dll"); Files.writeString(dll, "synthetic-dll");
        Path dawn = root.resolve("dawn.dll"); Files.writeString(dawn, "synthetic-dawn");
        Path weights = root.resolve("weights.bin"); Files.writeString(weights, "synthetic-weights");
        var layout = new ChromeMlRuntimeLayout(root, exe, dll, dawn, weights);
        var id = layout.validate();
        return new Fixture(root, layout, new ChromeMlRuntimeLayout.ExpectedHashes(id.runnerSha256(), id.chromeMlDllSha256(), id.dawnDllSha256(), id.weightsSha256()));
    }

    private static ChromeMlProcessRunner runner(Fixture f, Duration timeout, int max) throws IOException {
        return new ChromeMlProcessRunner(f.layout(), f.hashes(), timeout, max);
    }
    private static ChromeMlRunnerRequest request() {
        return new ChromeMlRunnerRequest("hello", List.of(), null, 16000, 32, null, null, null, false, false, false);
    }
    private static void check(String name, boolean result) {
        if (!result) { throw new AssertionError(name); }
        checks++; System.out.println("PASS " + name);
    }
    private static void expect(String name, String fragment, Checked action) throws Exception {
        try { action.run(); }
        catch (Exception e) {
            if (String.valueOf(e.getMessage()).contains(fragment)) { check(name, true); return; }
            throw new AssertionError(name + " unexpected: " + e, e);
        }
        throw new AssertionError(name + " did not reject");
    }
}
