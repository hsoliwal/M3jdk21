/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0
 * Derived from Synexia 2ab439480ecb0643615e85b42def954dbf3b5627:
 * cpullm-opdag/java/chrome-ai-mechanical/src/main/java/jdk/internal/m3/cpullm/chrome/ChromeSummarizerPort.java
 * Adapted for the M3JDK21-owned namespace and qualified summary contracts.
 */
package jdk.m3.llm.chrome;

import static jdk.m3.llm.chrome.MechanicalModel.*;

/**
 * Adapter/port for Chrome built-in Summarizer.
 *
 * <p>The JVM owns all mechanics. A browser adapter may implement this contract because
 * Chrome's built-in AI API itself is a browser JavaScript API, not a JVM API.
 */
public interface ChromeSummarizerPort {
    Session open() throws Exception;

    @SuppressWarnings("try") // Preserve the donor's checked close contract for caller-owned adapters.
    interface Session extends AutoCloseable {
        ChromeResult summarize(ChromeRequest request) throws Exception;
        @Override void close() throws Exception;
    }
}
