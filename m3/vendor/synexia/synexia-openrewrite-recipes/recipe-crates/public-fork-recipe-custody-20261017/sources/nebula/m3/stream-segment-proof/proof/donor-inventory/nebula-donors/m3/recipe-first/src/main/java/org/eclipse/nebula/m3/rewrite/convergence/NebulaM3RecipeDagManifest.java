// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Content-addressed, orchestration-neutral manifest for the proven Nebula recipe DAG.
 *
 * <p>OpenRewrite recipes remain the mutation atoms and Maven remains the build/proof root. Camel,
 * Airflow and Drools/KIE may schedule or select these atoms, but orchestration itself gains no
 * source-mutation or promotion authority.</p>
 */
public final class NebulaM3RecipeDagManifest {
    public enum Orchestrator {
        MAVEN_OPENREWRITE,
        CAMEL,
        AIRFLOW,
        DROOLS
    }

    public record Node(
            int ordinal,
            String id,
            NebulaM3FileConvergenceRecipeDag.Phase phase,
            String recipeClass,
            String scope,
            String authority,
            List<String> dependsOn) {
        public Node {
            if (ordinal < 0) throw new IllegalArgumentException("ordinal");
            id = text(id, "id");
            phase = Objects.requireNonNull(phase, "phase");
            recipeClass = text(recipeClass, "recipeClass");
            scope = text(scope, "scope");
            authority = text(authority, "authority");
            dependsOn = List.copyOf(Objects.requireNonNull(dependsOn, "dependsOn"));
        }
    }

    private static final List<Node> NODES = build();
    private static final Set<Orchestrator> TARGETS =
            Set.copyOf(EnumSet.allOf(Orchestrator.class));
    private static final String ROOT = sha256(tsv());

    private NebulaM3RecipeDagManifest() {}

    public static List<Node> nodes() {
        return NODES;
    }

    public static Set<Orchestrator> orchestrationTargets() {
        return TARGETS;
    }

    /** External orchestration can choose/order work but never edits source directly. */
    public static boolean orchestratorMutationAuthority() {
        return false;
    }

    /** External orchestration can produce receipts but never promotes canonical source. */
    public static boolean orchestratorPromotionAuthority() {
        return false;
    }

    public static String root() {
        return ROOT;
    }

    /** Stable TSV representation suitable for Camel/Airflow/Drools adapters and evidence storage. */
    public static String tsv() {
        StringBuilder out = new StringBuilder();
        out.append("ordinal\tid\tphase\trecipe\tscope\tauthority\tdependsOn\n");
        for (Node node : NODES) {
            out.append(node.ordinal())
                    .append('\t')
                    .append(node.id())
                    .append('\t')
                    .append(node.phase().name())
                    .append('\t')
                    .append(node.recipeClass())
                    .append('\t')
                    .append(node.scope())
                    .append('\t')
                    .append(node.authority())
                    .append('\t')
                    .append(String.join(",", node.dependsOn()))
                    .append('\n');
        }
        return out.toString();
    }

    private static List<Node> build() {
        List<NebulaM3FileConvergenceRecipeDag.Atom> atoms =
                NebulaM3FileConvergenceRecipeDag.atoms();
        ArrayList<Node> result = new ArrayList<>(atoms.size());
        String previous = null;
        for (int ordinal = 0; ordinal < atoms.size(); ordinal++) {
            NebulaM3FileConvergenceRecipeDag.Atom atom = atoms.get(ordinal);
            List<String> dependencies =
                    previous == null ? List.of() : List.of(previous);
            String authority =
                    atom.phase().mutating() ? "CANDIDATE_ONLY" : "READ_ONLY";
            result.add(
                    new Node(
                            ordinal,
                            atom.id(),
                            atom.phase(),
                            atom.recipe().getClass().getName(),
                            "FILE",
                            authority,
                            dependencies));
            previous = atom.id();
        }
        validate(result);
        return List.copyOf(result);
    }

    private static void validate(List<Node> nodes) {
        for (int index = 0; index < nodes.size(); index++) {
            Node node = nodes.get(index);
            if (node.ordinal() != index) {
                throw new IllegalStateException("recipe DAG ordinal drift");
            }
            if (index == 0) {
                if (!node.dependsOn().isEmpty()) {
                    throw new IllegalStateException("first recipe DAG node must be a root");
                }
            } else if (!node.dependsOn().equals(List.of(nodes.get(index - 1).id()))) {
                throw new IllegalStateException("recipe DAG dependency drift");
            }
            if (!"FILE".equals(node.scope())) {
                throw new IllegalStateException("Nebula proving DAG must remain FILE scoped");
            }
        }
    }

    private static String text(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
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
