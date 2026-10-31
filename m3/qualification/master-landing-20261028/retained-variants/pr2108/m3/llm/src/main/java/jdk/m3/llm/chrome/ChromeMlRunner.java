// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.chrome;

import java.io.IOException;

/** Host-owned runtime seam. Implementations may call a native runner but never own model licensing. */
public interface ChromeMlRunner {

    @FunctionalInterface
    interface TokenSink {
        void accept(String token) throws IOException;
    }

    ChromeMlRunnerResult generate(ChromeMlRunnerRequest request, TokenSink sink) throws Exception;

    default ChromeMlRunnerResult generate(ChromeMlRunnerRequest request) throws Exception {
        return generate(request, token -> { });
    }
}
