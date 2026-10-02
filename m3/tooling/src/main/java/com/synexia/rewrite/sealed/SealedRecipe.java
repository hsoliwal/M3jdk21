// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.sealed;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Candidate producer only. Implementations must be bounded, thread-confined and side-effect free. */
public interface SealedRecipe {
    record Identity(String id, String version, String implementationSha256, String category) {
        public Identity {
            if (id == null || !id.matches("[A-Za-z0-9_.-]{1,160}") || version == null || version.isBlank()
                    || category == null || category.isBlank()) throw new IllegalArgumentException("RECIPE_IDENTITY");
            SealHash.require(implementationSha256);
        }
        public String root() { return SealHash.frame(id, version, implementationSha256, category); }
    }
    record Output(Map<String, String> replacements, String residue) {
        public Output {
            replacements = Map.copyOf(replacements); residue = Objects.requireNonNull(residue, "residue");
            if (!replacements.isEmpty() && !residue.isEmpty()) throw new IllegalArgumentException("CANDIDATE_AND_RESIDUE");
        }
        public static Output unchanged() { return new Output(Map.of(), ""); }
        public static Output hold(String reason) { return new Output(Map.of(), reason); }
    }
    /** Optional exact built-in extractor for host-validated structural coverage; empty by default. */
    default java.util.Optional<ExtractPrivateIntHelper> privateIntExtraction() {
        return java.util.Optional.empty();
    }
    Identity identity();
    Output propose(SealedSources sources, Set<String> writablePaths, SealedContract contract) throws Exception;
}
