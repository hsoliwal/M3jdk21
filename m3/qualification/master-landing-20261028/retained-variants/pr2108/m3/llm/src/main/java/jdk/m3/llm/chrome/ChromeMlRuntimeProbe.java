// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.chrome;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Host-only audit/smoke CLI over the existing production config and native runner. */
public final class ChromeMlRuntimeProbe {
    private ChromeMlRuntimeProbe() {}

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--describe")) {
            System.out.println("usage=ChromeMlRuntimeProbe --audit | --smoke --execute-native");
            System.out.println("backendRequested=GPU; downloads=false; accountAccess=false");
            System.out.println("audit=BYTES_MATCH_ONLY; smoke=INFERENCE_COMPLETED; gpuVerified=false");
            return;
        }
        boolean audit = args.length == 1 && args[0].equals("--audit");
        boolean smoke = args.length == 2 && args[0].equals("--smoke") && args[1].equals("--execute-native");
        if (!audit && !smoke) {
            throw new IllegalArgumentException("use --audit or --smoke --execute-native");
        }
        if (smoke && !System.getProperty("os.name", "").startsWith("Windows")) {
            throw new IllegalStateException("WINDOWS_CHROMEML_RUNTIME_REQUIRED");
        }
        ChromeMlProductionConfig config = configuration(System.getenv());
        if (audit) { audit(config, System.out); }
        else { smoke(config, System.out); }
    }

    static ChromeMlProductionConfig configuration(Map<String, String> environment) throws IOException {
        Objects.requireNonNull(environment, "environment");
        // No legacy path guesses or profile scans in this operator-facing setup path.
        for (String key : List.of("CHROMEML_ROOT", "CHROMEML_RUNNER", "CHROMEML_DLL",
                "CHROMEML_DAWN_DLL", "CHROMEML_WEIGHTS")) {
            String value = environment.get(key);
            if (value == null || value.isBlank() || !Path.of(value).isAbsolute()) {
                throw new IllegalArgumentException("EXPLICIT_ABSOLUTE_PATH_REQUIRED:" + key);
            }
        }
        return ChromeMlProductionConfig.fromEnvironment(environment);
    }

    static void audit(ChromeMlProductionConfig config, PrintStream output) throws IOException {
        var identity = config.doctor();
        output.println("status=BYTES_MATCH_ONLY");
        output.println("runtimeRoot=" + identity.root());
        output.println("backendRequested=GPU");
        output.println("inferenceExecuted=false");
        output.println("gpuVerified=false");
    }

    static void smoke(ChromeMlProductionConfig config, PrintStream output) throws Exception {
        try (var runner = new ChromeMlProcessRunner(config.runtimeLayout(), config.expectedHashes(),
                config.requestTimeout(), config.maxCaptureBytes())) {
            var request = new ChromeMlRunnerRequest("Return only the word READY.", List.of(), null,
                    16_000, 16, null, 1, 0.0, false, true, false);
            var result = runner.generate(request);
            if (result.text().isBlank()) { throw new IOException("SMOKE_EMPTY_OUTPUT"); }
            output.println("status=INFERENCE_COMPLETED");
            output.println("runtimeRoot=" + runner.runtimeIdentity().root());
            output.println("outputSha256=" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(result.text().getBytes(StandardCharsets.UTF_8))));
            output.println("backendRequested=GPU");
            output.println("gpuVerified=false");
            // A native process report is not physical GPU attestation, speed or semantic correctness.
        }
    }
}
