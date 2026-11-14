// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.chrome;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Immutable input to one ChromeML bare-runner invocation. */
public record ChromeMlRunnerRequest(
        String prompt,
        List<Path> imagePaths,
        Path audioPath,
        int audioTargetRate,
        int maxOutput,
        Integer contextTokens,
        Integer topK,
        Double temperature,
        boolean shortContext,
        boolean top1,
        boolean noSafetyReserve) {

    public ChromeMlRunnerRequest {
        prompt = Objects.requireNonNull(prompt, "prompt");
        if (prompt.isBlank() || prompt.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("prompt");
        }
        imagePaths = List.copyOf(Objects.requireNonNullElse(imagePaths, List.of()));
        imagePaths.forEach(path -> Objects.requireNonNull(path, "image path"));
        if (audioTargetRate < 1) {
            throw new IllegalArgumentException("audioTargetRate");
        }
        if (maxOutput < 1) {
            throw new IllegalArgumentException("maxOutput");
        }
        positive(contextTokens, "contextTokens");
        positive(topK, "topK");
        if (temperature != null && !Double.isFinite(temperature)) {
            throw new IllegalArgumentException("temperature");
        }
    }

    private static void positive(Integer value, String name) {
        if (value != null && value < 1) {
            throw new IllegalArgumentException(name);
        }
    }
}
