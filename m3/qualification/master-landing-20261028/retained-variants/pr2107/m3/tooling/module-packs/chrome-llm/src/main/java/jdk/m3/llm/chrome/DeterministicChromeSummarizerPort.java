/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0
 * Derived from Synexia 2ab439480ecb0643615e85b42def954dbf3b5627:
 * cpullm-opdag/java/chrome-ai-mechanical/src/main/java/com/synexia/cpullm/chrome/DeterministicChromeSummarizerPort.java
 * Adapted for the M3JDK21-owned namespace and qualified summary contracts.
 */
package jdk.m3.llm.chrome;

import static jdk.m3.llm.chrome.MechanicalModel.*;

import java.util.Locale;

/**
 * Deterministic no-network fixture adapter used for Java verification.
 * It proves orchestration and authority boundaries; it is not native Chrome inference.
 */
public final class DeterministicChromeSummarizerPort implements ChromeSummarizerPort {
    /** Preserves the donor's implicitly public no-argument construction contract. */
    public DeterministicChromeSummarizerPort() {}
    @Override
    public Session open() {
        return new Session() {
            private boolean closed;

            @Override
            public ChromeResult summarize(ChromeRequest request) {
                if (closed) throw new IllegalStateException("session closed");
                Hashing.utf8(request.input());
                String normalized = request.input().strip().replaceAll("\\s+", " ");
                int end = Math.min(normalized.length(), 512);
                if (end < normalized.length() && Character.isHighSurrogate(normalized.charAt(end - 1))) end--;
                String text = normalized.substring(0, end);
                if (text.isBlank()) text = "(empty)";
                ChromeResult provisional = new ChromeResult(
                        "CANDIDATE_ONLY", "", request.requestId(), request.sourceRefs(),
                        text, "MODEL_CANDIDATE", true, false, 1, 0);
                return new ChromeResult(
                        provisional.status(), ChromeProtocol.resultId(provisional), provisional.sourceRequestId(),
                        provisional.sourceRefs(), provisional.text(), provisional.authority(),
                        provisional.candidateOnly(), provisional.admitted(), provisional.modelCalls(),
                        provisional.directNetworkCalls());
            }

            @Override
            public void close() { closed = true; }

            @Override
            public String toString() { return "DeterministicChromeSummarizerPort.Session".toLowerCase(Locale.ROOT); }
        };
    }
}
