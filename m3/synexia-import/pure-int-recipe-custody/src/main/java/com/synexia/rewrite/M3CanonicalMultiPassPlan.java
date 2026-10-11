// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Canonical scope-aware M3 convergence order.
 *
 * <p>Independent FILE work may fan out. Within FILE scope, semantic convergence is explicitly
 * atomization -> patternization/IOP -> derivable documentation -> fixed point. Scope authority grows
 * only after that FILE frontier is sealed. Candidate production may parallelize; canonical
 * promotion remains serial/proof-gated.</p>
 */
public final class M3CanonicalMultiPassPlan {
    private static final List<Pass> PASSES = List.of(
            new Pass(
                    0,
                    "INVENTORY",
                    M3EditScope.FILE,
                    false,
                    "inventory/hash/catalogue stable for unchanged preimages"),
            new Pass(
                    1,
                    "FILE_ATOMIZATION",
                    M3EditScope.FILE,
                    true,
                    "every admitted atomization recipe preserves the sealed file contract"),
            new Pass(
                    2,
                    "FILE_PATTERNIZATION",
                    M3EditScope.FILE,
                    true,
                    "every behavioral leaf has admitted pattern/IOP identity or typed residue"),
            new Pass(
                    3,
                    "FILE_DOCUMENTATION",
                    M3EditScope.FILE,
                    true,
                    "derivable Javadoc/semantic documentation converges without behavior drift"),
            new Pass(
                    4,
                    "FILE_FIXED_POINT",
                    M3EditScope.FILE,
                    false,
                    "the complete FILE recipe DAG is unchanged on the next pass"),
            new Pass(
                    5,
                    "VISIBILITY",
                    M3EditScope.VISIBILITY,
                    false,
                    "every accessibility concern is classified before any visibility mutation"),
            new Pass(
                    6,
                    "PACKAGE",
                    M3EditScope.PACKAGE,
                    false,
                    "package/default/protected participant relationships converge"),
            new Pass(
                    7,
                    "MODULE",
                    M3EditScope.MODULE,
                    false,
                    "module-local symbol/type/recipe relationships converge"),
            new Pass(
                    8,
                    "MULTI_MODULE",
                    M3EditScope.MULTI_MODULE,
                    false,
                    "reactor-wide fan-in roots and recipe/donor catalogues are stable"),
            new Pass(
                    9,
                    "LIBRARY_API",
                    M3EditScope.LIBRARY_API,
                    false,
                    "exported API deltas are explicitly approved or typed exclusions"),
            new Pass(
                    10,
                    "PROOF",
                    M3EditScope.LIBRARY_API,
                    false,
                    "diff -> lint/static analysis -> compile -> tests -> runtime -> Java/JNI parity -> fixed point"));

    private M3CanonicalMultiPassPlan() {}

    public static List<Pass> passes() {
        return PASSES;
    }

    /** FILE-local convergence frontier that must seal before any scope promotion. */
    public static List<Pass> fileConvergencePasses() {
        return PASSES.subList(0, 5);
    }

    public static Pass pass(int ordinal) {
        return PASSES.get(Objects.checkIndex(ordinal, PASSES.size()));
    }

    /** Content-addressed identity of the exact ordered scope-aware pass programme. */
    public static String root() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            frame(digest, "M3_CANONICAL_MULTIPASS_PLAN_V1");
            frame(digest, Integer.toString(PASSES.size()));
            for (Pass pass : PASSES) {
                frame(digest, Integer.toString(pass.ordinal()));
                frame(digest, pass.id());
                frame(digest, pass.maximumScope().name());
                frame(digest, Boolean.toString(pass.mutationAuthority()));
                frame(digest, pass.stopCondition());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static void frame(MessageDigest digest, String value) {
        byte[] bytes = Objects.requireNonNull(value, "value").getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }

    public record Pass(
            int ordinal,
            String id,
            M3EditScope maximumScope,
            boolean mutationAuthority,
            String stopCondition) {
        public Pass {
            if (ordinal < 0) throw new IllegalArgumentException("ordinal");
            if (id == null || id.isBlank()) throw new IllegalArgumentException("id");
            maximumScope = Objects.requireNonNull(maximumScope, "maximumScope");
            if (stopCondition == null || stopCondition.isBlank()) {
                throw new IllegalArgumentException("stopCondition");
            }
            if (mutationAuthority && maximumScope != M3EditScope.FILE) {
                throw new IllegalArgumentException(
                        "canonical broad-scope passes are review/fan-in only by default");
            }
        }

        public boolean fileParallel() {
            return maximumScope == M3EditScope.FILE;
        }
    }
}
