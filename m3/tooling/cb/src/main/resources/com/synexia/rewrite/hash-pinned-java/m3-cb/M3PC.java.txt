// SPDX-License-Identifier: Apache-2.0
package jdk.internal.mindex;

import java.util.List;
import java.util.Objects;

/**
 * Immutable, VM-local candidate key over references to existing immutable JDK Strings.
 * Equality discovers reusable work; it is not verifier, linker or native-code admission.
 * Ordered dependency inputs must include the caller's complete effective context.
 * This object is a caller-owned context key, not a portable cache format.
 */
public final class M3PC {
    private final String[] lanes;

    private M3PC(String[] lanes) {
        this.lanes = lanes;
    }

    public static M3PC of(
            String contentSeal,
            String analysisRevision,
            String environmentSeal,
            List<String> orderedDependencies) {
        Objects.requireNonNull(orderedDependencies, "orderedDependencies");
        if (orderedDependencies.size() > 65_536) {
            throw new IllegalArgumentException("dependency budget exceeded");
        }
        String[] lanes = new String[orderedDependencies.size() + 3];
        lanes[0] = Objects.requireNonNull(contentSeal, "contentSeal");
        lanes[1] = Objects.requireNonNull(analysisRevision, "analysisRevision");
        lanes[2] = Objects.requireNonNull(environmentSeal, "environmentSeal");
        for (int i = 0; i < orderedDependencies.size(); i++) {
            lanes[i + 3] = Objects.requireNonNull(orderedDependencies.get(i), "dependency");
        }
        return new M3PC(lanes);
    }

    public int dependencyCount() { return lanes.length - 3; }

    public boolean sameInputs(M3PC other) {
        return other != null && java.util.Arrays.equals(lanes, other.lanes);
    }
}
