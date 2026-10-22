// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import org.eclipse.nebula.m3.NebulaM3FastSearchReviewPolicy;

/**
 * Portable, content-addressed handoff contract for the proven Nebula M3 recipe DAG.
 *
 * <p>The contract carries no target-source mutation or promotion authority. It identifies the
 * proven Java 21/OpenRewrite entry point and the exact scheduler-neutral DAG that another
 * repository must adapt through its own inventory/scope recipes before any source change.</p>
 */
public final class NebulaM3TransferContract {
    public static final String SCHEMA = "NEBULA_M3_RECIPE_TRANSFER_V1";
    public static final int JAVA_RELEASE = 21;
    public static final String OPENREWRITE_VERSION = "8.90.4";
    public static final String ENTRYPOINT =
            "org.eclipse.nebula.m3.rewrite.NebulaM3Java21ConvergenceRecipe";

    public record Target(
            String repository,
            String adapterLane,
            boolean directSourceMutationAuthority,
            boolean promotionAuthority) {
        public Target {
            repository = text(repository, "repository");
            adapterLane = text(adapterLane, "adapterLane");
            if (directSourceMutationAuthority || promotionAuthority) {
                throw new IllegalArgumentException(
                        "transfer target may not carry mutation/promotion authority");
            }
        }
    }

    private static final List<Target> TARGETS =
            List.of(
                    new Target(
                            "hsoliwal/M3jdk21",
                            "JAVA21_JDK_COMPATIBILITY_AND_BACKPORT_LANES",
                            false,
                            false),
                    new Target(
                            "hsoliwal/com.synexia",
                            "RECIPE_PACK_AND_MODULE_LANES",
                            false,
                            false));

    private NebulaM3TransferContract() {}

    public static List<Target> targets() {
        return TARGETS;
    }

    public static boolean directSourceMutationAuthority() {
        return false;
    }

    public static boolean promotionAuthority() {
        return false;
    }

    public static String metadataTsv() {
        return """
                key	value
                schema	%s
                javaRelease	%d
                openRewriteVersion	%s
                entrypoint	%s
                dagRoot	%s
                orchestratorPlansRoot	%s
                uiBehaviorDonorRoot	%s
                challengeDonorRoot	%s
                refactorScopeOrder	%s
                directSourceMutationAuthority	false
                promotionAuthority	false
                """
                .formatted(
                        SCHEMA,
                        JAVA_RELEASE,
                        OPENREWRITE_VERSION,
                        ENTRYPOINT,
                        NebulaM3RecipeDagManifest.root(),
                        NebulaM3OrchestratorPlans.root(),
                        NebulaM3UiBehaviorDonorCatalog.root(),
                        sha256(NebulaM3FastSearchReviewPolicy.renderTsv()),
                        refactorScopeOrder());
    }

    public static String refactorScopeOrder() {
        return NebulaM3RefactorScope.canonicalOrder().stream()
                .map(Enum::name)
                .collect(java.util.stream.Collectors.joining(","));
    }

    public static String targetsTsv() {
        StringBuilder out =
                new StringBuilder(
                        "repository\tadapterLane\tdirectSourceMutationAuthority\tpromotionAuthority\n");
        for (Target target : TARGETS) {
            out.append(target.repository())
                    .append('\t')
                    .append(target.adapterLane())
                    .append('\t')
                    .append(target.directSourceMutationAuthority())
                    .append('\t')
                    .append(target.promotionAuthority())
                    .append('\n');
        }
        return out.toString();
    }

    public static String root() {
        return sha256(
                framed(metadataTsv())
                        + framed(NebulaM3RecipeDagManifest.tsv())
                        + framed(NebulaM3UiBehaviorDonorCatalog.tsv())
                        + framed(NebulaM3FastSearchReviewPolicy.renderTsv())
                        + framed(targetsTsv())
                        + framed(orchestratorsTsv())
                        + framed(NebulaM3OrchestratorPlans.camelYaml())
                        + framed(NebulaM3OrchestratorPlans.airflowPython())
                        + framed(NebulaM3OrchestratorPlans.droolsDrl()));
    }

    public static String orchestratorsTsv() {
        StringBuilder out =
                new StringBuilder(
                        "orchestrator\tmutationAuthority\tpromotionAuthority\n");
        NebulaM3RecipeDagManifest.orchestrationTargets().stream()
                .sorted(java.util.Comparator.comparing(Enum::name))
                .forEach(
                        target ->
                                out.append(target.name())
                                        .append("\tfalse\tfalse\n"));
        return out.toString();
    }

    private static String framed(String value) {
        byte[] bytes = Objects.requireNonNull(value, "value").getBytes(StandardCharsets.UTF_8);
        return bytes.length + ":" + value;
    }

    private static String text(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf(0) >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
