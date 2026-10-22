// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.Objects;

/**
 * Content-addressed transfer/backport plan downstream of the proven Nebula recipe DAG.
 *
 * <p>This plan is evidence only. It orders repository-local inventory, recipe absorption and proof
 * but never edits target source or promotes a candidate by itself.</p>
 */
public final class NebulaM3BackportPlan {
    public enum Phase {
        NEBULA_PROOF,
        SYNEXIA_RECIPE_ABSORPTION,
        SYNEXIA_RECIPE_PROOF,
        M3JDK21_MAVEN_ORCHESTRATION_BOOTSTRAP,
        M3JDK21_BACKPORT_INVENTORY,
        M3JDK21_FILE_REPLAY,
        M3JDK21_SCOPE_PROMOTION,
        TARGET_PROOF
    }

    public record Step(
            int ordinal,
            String id,
            Phase phase,
            String repository,
            String action,
            NebulaM3RefactorScope maximumScope,
            List<String> dependsOn,
            boolean directSourceMutationAuthority,
            boolean promotionAuthority) {
        public Step {
            if (ordinal < 0) throw new IllegalArgumentException("ordinal");
            id = text(id, "id");
            phase = Objects.requireNonNull(phase, "phase");
            repository = text(repository, "repository");
            action = text(action, "action");
            maximumScope = Objects.requireNonNull(maximumScope, "maximumScope");
            dependsOn = List.copyOf(Objects.requireNonNull(dependsOn, "dependsOn"));
            if (directSourceMutationAuthority || promotionAuthority) {
                throw new IllegalArgumentException(
                        "transfer/backport plan cannot own mutation or promotion authority");
            }
        }
    }

    private static final List<Step> STEPS =
            validate(List.of(
                    step(
                            0,
                            "nebula-portable-proof",
                            Phase.NEBULA_PROOF,
                            "hsoliwal/nebula",
                            "Prove Java21 Maven/OpenRewrite DAG, original Tycho/SWT product build, "
                                    + "second-pass fixed point and portable roots.",
                            NebulaM3RefactorScope.FILE),
                    step(
                            1,
                            "synexia-absorb-recipe-atoms",
                            Phase.SYNEXIA_RECIPE_ABSORPTION,
                            "hsoliwal/com.synexia",
                            "Inventory canonical owners; absorb or improve the small reusable "
                                    + "OpenRewrite recipe atoms and scheduler-neutral DAG without "
                                    + "copying Nebula product source.",
                            NebulaM3RefactorScope.FILE,
                            "nebula-portable-proof"),
                    step(
                            2,
                            "synexia-prove-canonical-dag",
                            Phase.SYNEXIA_RECIPE_PROOF,
                            "hsoliwal/com.synexia",
                            "Prove Java21/JUnit atom, pattern/IOP, documentation, 99-percent semantic "
                                    + "coverage and second-pass fixed point for the canonical recipe pack.",
                            NebulaM3RefactorScope.FILE,
                            "synexia-absorb-recipe-atoms"),
                    step(
                            3,
                            "m3jdk21-maven-orchestration-bootstrap",
                            Phase.M3JDK21_MAVEN_ORCHESTRATION_BOOTSTRAP,
                            "hsoliwal/M3jdk21",
                            "Install or verify the Maven/OpenRewrite recipe orchestration plane around "
                                    + "the existing OpenJDK build without replacing make/autoconf; prove "
                                    + "Java21 recipe resolution and target-local inventory entry points.",
                            NebulaM3RefactorScope.FILE,
                            "synexia-prove-canonical-dag"),
                    step(
                            4,
                            "m3jdk21-inventory-backports",
                            Phase.M3JDK21_BACKPORT_INVENTORY,
                            "hsoliwal/M3jdk21",
                            "Inventory JDK21 contracts plus later JEP, PR, tooling and compatible "
                                    + "dependency closure; classify every candidate before replay.",
                            NebulaM3RefactorScope.FILE,
                            "m3jdk21-maven-orchestration-bootstrap"),
                    step(
                            5,
                            "m3jdk21-file-replay",
                            Phase.M3JDK21_FILE_REPLAY,
                            "hsoliwal/M3jdk21",
                            "Replay only admitted contract-preserving FILE recipe atoms independently; "
                                    + "require compiler/JUnit/diff/fixed-point evidence.",
                            NebulaM3RefactorScope.FILE,
                            "m3jdk21-inventory-backports"),
                    step(
                            6,
                            "m3jdk21-scope-promotion",
                            Phase.M3JDK21_SCOPE_PROMOTION,
                            "hsoliwal/M3jdk21",
                            "Promote authority only when required through VISIBILITY, PACKAGE, MODULE, "
                                    + "MULTI_MODULE and LIBRARY_API; preserve Java21 default contracts.",
                            NebulaM3RefactorScope.LIBRARY_API,
                            "m3jdk21-file-replay"),
                    step(
                            7,
                            "target-proof",
                            Phase.TARGET_PROOF,
                            "hsoliwal/M3jdk21",
                            "Run target-local diff, lint/static analysis, Java21 compile, jtreg/JUnit, "
                                    + "runtime/JNI parity where declared, setup-inclusive benchmark where "
                                    + "claimed and final fixed point before promotion.",
                            NebulaM3RefactorScope.LIBRARY_API,
                            "m3jdk21-scope-promotion")));

    private static final String ROOT = sha256(tsv());

    private NebulaM3BackportPlan() {}

    public static List<Step> steps() {
        return STEPS;
    }

    public static String root() {
        return ROOT;
    }

    public static boolean directSourceMutationAuthority() {
        return false;
    }

    public static boolean promotionAuthority() {
        return false;
    }

    public static String tsv() {
        StringBuilder out =
                new StringBuilder(
                        "ordinal\tid\tphase\trepository\tmaximumScope\tdirectSourceMutationAuthority"
                                + "\tpromotionAuthority\tdependsOn\taction\n");
        for (Step step : STEPS) {
            out.append(step.ordinal())
                    .append('\t')
                    .append(step.id())
                    .append('\t')
                    .append(step.phase().name())
                    .append('\t')
                    .append(step.repository())
                    .append('\t')
                    .append(step.maximumScope().name())
                    .append("\tfalse\tfalse\t")
                    .append(String.join(",", step.dependsOn()))
                    .append('\t')
                    .append(step.action())
                    .append('\n');
        }
        return out.toString();
    }

    /**
     * Proves the complete transfer DAG before exposing it as a content-addressed plan.
     *
     * <p>Each phase must appear exactly once, in the canonical order. Edges must point to the
     * immediately preceding phase, so neither a skipped proof gate nor a forward/cyclic edge can
     * be hidden inside otherwise well-formed step records. Repository and maximum edit scope
     * are also phase-bound: only Nebula proves Nebula, only Synexia absorbs recipes, and the JDK
     * never obtains wider authority before its FILE replay has been proven.</p>
     *
     * <p>This is a pure validation atom; it performs no source mutation or promotion.</p>
     */
    static List<Step> validate(List<Step> proposed) {
        List<Step> plan = List.copyOf(Objects.requireNonNull(proposed, "proposed"));
        Phase[] phases = Phase.values();
        if (plan.size() != phases.length) {
            throw new IllegalArgumentException("backport plan must contain each phase exactly once");
        }
        Set<String> ids = new HashSet<>();
        for (int index = 0; index < plan.size(); index++) {
            Step step = plan.get(index);
            if (step.ordinal() != index || step.phase() != phases[index]) {
                throw new IllegalArgumentException("backport phase/ordinal drift at " + index);
            }
            if (!ids.add(step.id())) {
                throw new IllegalArgumentException("duplicate backport step: " + step.id());
            }
            List<String> expectedDependencies =
                    index == 0 ? List.of() : List.of(plan.get(index - 1).id());
            if (!expectedDependencies.equals(step.dependsOn())) {
                throw new IllegalArgumentException("backport dependency drift at " + step.id());
            }
            String expectedRepository =
                    switch (step.phase()) {
                        case NEBULA_PROOF -> "hsoliwal/nebula";
                        case SYNEXIA_RECIPE_ABSORPTION, SYNEXIA_RECIPE_PROOF ->
                                "hsoliwal/com.synexia";
                        default -> "hsoliwal/M3jdk21";
                    };
            if (!expectedRepository.equals(step.repository())) {
                throw new IllegalArgumentException("backport repository drift at " + step.id());
            }
            NebulaM3RefactorScope expectedScope =
                    index < 6
                            ? NebulaM3RefactorScope.FILE
                            : NebulaM3RefactorScope.LIBRARY_API;
            if (step.maximumScope() != expectedScope) {
                throw new IllegalArgumentException("backport authority drift at " + step.id());
            }
        }
        return plan;
    }

    private static Step step(
            int ordinal,
            String id,
            Phase phase,
            String repository,
            String action,
            NebulaM3RefactorScope maximumScope,
            String... dependsOn) {
        return new Step(
                ordinal,
                id,
                phase,
                repository,
                action,
                maximumScope,
                List.of(dependsOn),
                false,
                false);
    }

    private static String text(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0 || checked.indexOf('\t') >= 0
                || checked.indexOf('\n') >= 0 || checked.indexOf('\r') >= 0) {
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
