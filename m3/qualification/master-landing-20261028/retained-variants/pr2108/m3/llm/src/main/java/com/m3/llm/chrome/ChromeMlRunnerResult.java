// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package com.m3.llm.chrome;

import java.util.Objects;

/** Result captured from one ChromeML bare-runner invocation. */
public record ChromeMlRunnerResult(
        String text,
        Integer completeStatus,
        String stdout,
        String stderr) {

    public ChromeMlRunnerResult {
        text = Objects.requireNonNull(text, "text");
        stdout = Objects.requireNonNullElse(stdout, "");
        stderr = Objects.requireNonNullElse(stderr, "");
    }
}
