/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0
 * Derived from Synexia 2ab439480ecb0643615e85b42def954dbf3b5627:
 * cpullm-opdag/java/chrome-ai-mechanical/src/main/java/jdk/internal/m3/cpullm/chrome/ChromeProtocol.java
 * Adapted for the M3JDK21-owned namespace and qualified summary contracts.
 */
package jdk.m3.llm.chrome;

import static jdk.m3.llm.chrome.MechanicalModel.*;

import java.util.List;
import java.util.Objects;

/** Content-addressed candidate-only protocol shared by Java controller and browser adapter. */
public final class ChromeProtocol {
    private ChromeProtocol() {}

    public static ChromeRequest request(String nodeId, String input, List<String> sourceRefs) {
        Objects.requireNonNull(input, "input");
        for (String ref : sourceRefs) Hashing.utf8(ref);
        List<String> refs = sourceRefs.stream().distinct().sorted().toList();
        String inputSha = Hashing.sha256Hex(Hashing.utf8(input));
        String requestId = Hashing.hash("CPULLM_JAVA_CHROME_REQUEST_V1", nodeId, inputSha, refs);
        return new ChromeRequest(requestId, nodeId, input, inputSha, refs,
                "CANDIDATE_ONLY", true, 0, 0);
    }

    public static String resultId(ChromeResult result) {
        return Hashing.hash("CPULLM_JAVA_CHROME_RESULT_V1",
                result.status(), result.sourceRequestId(), result.sourceRefs(), result.text(),
                result.authority(), result.candidateOnly(), result.admitted(),
                result.modelCalls(), result.directNetworkCalls());
    }

    public static void verify(ChromeRequest request, ChromeResult result) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(result, "result");
        if (!request.equals(request(request.nodeId(), request.input(), request.sourceRefs()))) {
            throw new IllegalArgumentException("request content identity mismatch");
        }
        if (!request.authority().equals("CANDIDATE_ONLY") || !request.candidateOnly()
                || request.modelCalls() != 0 || request.directNetworkCalls() != 0) {
            throw new IllegalArgumentException("request authority escalation");
        }
        if (!result.status().equals("CANDIDATE_ONLY") && !result.status().equals("ABSTAIN")) {
            throw new IllegalArgumentException("invalid result status");
        }
        if (!result.sourceRequestId().equals(request.requestId())) {
            throw new IllegalArgumentException("request/result binding mismatch");
        }
        if (!result.sourceRefs().equals(request.sourceRefs())) {
            throw new IllegalArgumentException("source reference mismatch");
        }
        if (!result.authority().equals("MODEL_CANDIDATE") || !result.candidateOnly() || result.admitted()) {
            throw new IllegalArgumentException("result authority escalation");
        }
        if (result.directNetworkCalls() != 0 || result.modelCalls() < 0 || result.modelCalls() > 1) {
            throw new IllegalArgumentException("invalid call counters");
        }
        if (result.status().equals("CANDIDATE_ONLY") && result.modelCalls() != 1) {
            throw new IllegalArgumentException("candidate must bind exactly one model call");
        }
        Hashing.utf8(result.text());
        if (result.status().equals("CANDIDATE_ONLY") && result.text().isBlank()) {
            throw new IllegalArgumentException("candidate summary required");
        }
        if (!Hashing.isSha256(result.resultId()) || !result.resultId().equals(resultId(result))) {
            throw new IllegalArgumentException("result identity mismatch");
        }
    }
}
