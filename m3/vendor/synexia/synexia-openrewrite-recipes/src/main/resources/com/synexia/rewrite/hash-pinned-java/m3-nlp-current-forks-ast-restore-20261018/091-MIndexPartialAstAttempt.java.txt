// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.ast.partial;

import java.util.Objects;

/** One parser-adapter attempt: success, monotonic scope escalation, or explicit rejection. */
public sealed interface MIndexPartialAstAttempt
        permits MIndexPartialAstAttempt.Success,
                MIndexPartialAstAttempt.Escalate,
                MIndexPartialAstAttempt.Reject {

    record Success(MIndexPartialAstProjection projection) implements MIndexPartialAstAttempt {
        public Success {
            projection = Objects.requireNonNull(projection, "projection");
        }
    }

    record Escalate(MIndexPartialAstScope requiredScope, String reason)
            implements MIndexPartialAstAttempt {
        public Escalate {
            requiredScope = Objects.requireNonNull(requiredScope, "requiredScope");
            reason = required(reason);
        }
    }

    record Reject(String reason) implements MIndexPartialAstAttempt {
        public Reject {
            reason = required(reason);
        }
    }

    private static String required(String value) {
        String normalized = Objects.requireNonNull(value, "reason").strip();
        if (normalized.isEmpty()) throw new IllegalArgumentException("reason must not be blank");
        return normalized;
    }
}
