// SPDX-License-Identifier: Apache-2.0
package jdk.m3.rewrite.scope;

import java.util.Objects;

/**
 * M3JDK 21 compatibility vocabulary for Synexia's canonical contract modes.
 *
 * <p>This target enum is a thin adapter only; reusable meaning is owned by
 * {@link jdk.internal.m3.rewrite.M3ContractMode}.</p>
 */
public enum M3ContractMode {
    BEHAVIOR_AND_CONTRACT_PRESERVING(
            jdk.internal.m3.rewrite.M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING),
    EXPLICIT_CONTRACT_CHANGE(
            jdk.internal.m3.rewrite.M3ContractMode.EXPLICIT_CONTRACT_CHANGE);

    private final jdk.internal.m3.rewrite.M3ContractMode canonical;

    M3ContractMode(jdk.internal.m3.rewrite.M3ContractMode canonical) {
        this.canonical = Objects.requireNonNull(canonical, "canonical");
    }

    static M3ContractMode fromCanonical(jdk.internal.m3.rewrite.M3ContractMode mode) {
        return valueOf(Objects.requireNonNull(mode, "mode").name());
    }

    jdk.internal.m3.rewrite.M3ContractMode canonical() {
        return canonical;
    }
}
