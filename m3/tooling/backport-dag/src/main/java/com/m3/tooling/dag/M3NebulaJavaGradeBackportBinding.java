// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Exact candidate binding from Nebula's Java-grade V2 transfer/backport proof into M3JDK21.
 *
 * <p>The binding is intentionally HOLD-only until the referenced Nebula PR produces a complete
 * Java-grade proof receipt. It can inform inventory, dependency planning and recipe adaptation but
 * grants no OpenJDK source mutation, compatibility, semantic-equivalence or promotion authority.</p>
 */
public final class M3NebulaJavaGradeBackportBinding {
    public static final String SCHEMA = "M3_NEBULA_JAVA_GRADE_BACKPORT_HANDOFF_V1";
    public static final String NEBULA_REPOSITORY = "hsoliwal/nebula";
    public static final String NEBULA_REF =
            "m3/java-grade-transfer-qualification-v3-20261015";
    public static final String NEBULA_COMMIT =
            "7a5a3cb5ffd82bca671b20e1496db80369657578";
    public static final int NEBULA_PR = 208;
    public static final String STATE = "HOLD_PENDING_NEBULA_PROOF";
    public static final String TARGET_REPOSITORY = "hsoliwal/M3jdk21";
    public static final String APPLICATION_POLICY =
            "INVENTORY_AND_ADAPTATION_ONLY_UNTIL_JAVA_GRADE_PROVEN";

    public static final Asset JAVA_GRADE =
            new Asset(
                    "java-grade",
                    "m3/recipe-first/src/main/java/org/eclipse/nebula/m3/rewrite/convergence/"
                            + "NebulaM3JavaGradeQualification.java",
                    "80f3a2a0ca443972f367b5636d3a897a27eff88b");
    public static final Asset BACKPORT_PLAN =
            new Asset(
                    "backport-plan",
                    "m3/recipe-first/src/main/java/org/eclipse/nebula/m3/rewrite/convergence/"
                            + "NebulaM3BackportPlan.java",
                    "eddba9c6546749169b1a038147e06c4d8c863b4d");
    public static final Asset TRANSFER_CONTRACT =
            new Asset(
                    "transfer-contract",
                    "m3/recipe-first/src/main/java/org/eclipse/nebula/m3/rewrite/convergence/"
                            + "NebulaM3TransferContract.java",
                    "368b947eacef8355607706c90991af5b55b08bc2");
    public static final Asset DOCUMENTATION =
            new Asset(
                    "java-grade-doc",
                    "m3/docs/JAVA_GRADE_RECIPE_CONVERGENCE.md",
                    "d74b4180bf6b36a16d9c2f1362c19fcc5d38daf0");

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

    public record Asset(String id, String path, String gitBlobSha1) {
        public Asset {
            id = text(id, "id");
            path = text(path, "path");
            gitBlobSha1 = sha1(gitBlobSha1, "gitBlobSha1");
        }
    }

    public record Step(
            int ordinal,
            Phase phase,
            String repository,
            String maximumScope,
            String dependsOn) {
        public Step {
            if (ordinal < 0) throw new IllegalArgumentException("ordinal");
            phase = Objects.requireNonNull(phase, "phase");
            repository = text(repository, "repository");
            maximumScope = text(maximumScope, "maximumScope");
            dependsOn = Objects.requireNonNull(dependsOn, "dependsOn");
        }
    }

    private static final List<Step> STEPS =
            List.of(
                    step(0, Phase.NEBULA_PROOF, "hsoliwal/nebula", "FILE", ""),
                    step(
                            1,
                            Phase.SYNEXIA_RECIPE_ABSORPTION,
                            "hsoliwal/com.synexia",
                            "FILE",
                            Phase.NEBULA_PROOF.name()),
                    step(
                            2,
                            Phase.SYNEXIA_RECIPE_PROOF,
                            "hsoliwal/com.synexia",
                            "FILE",
                            Phase.SYNEXIA_RECIPE_ABSORPTION.name()),
                    step(
                            3,
                            Phase.M3JDK21_MAVEN_ORCHESTRATION_BOOTSTRAP,
                            TARGET_REPOSITORY,
                            "FILE",
                            Phase.SYNEXIA_RECIPE_PROOF.name()),
                    step(
                            4,
                            Phase.M3JDK21_BACKPORT_INVENTORY,
                            TARGET_REPOSITORY,
                            "FILE",
                            Phase.M3JDK21_MAVEN_ORCHESTRATION_BOOTSTRAP.name()),
                    step(
                            5,
                            Phase.M3JDK21_FILE_REPLAY,
                            TARGET_REPOSITORY,
                            "FILE",
                            Phase.M3JDK21_BACKPORT_INVENTORY.name()),
                    step(
                            6,
                            Phase.M3JDK21_SCOPE_PROMOTION,
                            TARGET_REPOSITORY,
                            "LIBRARY_API",
                            Phase.M3JDK21_FILE_REPLAY.name()),
                    step(
                            7,
                            Phase.TARGET_PROOF,
                            TARGET_REPOSITORY,
                            "LIBRARY_API",
                            Phase.M3JDK21_SCOPE_PROMOTION.name()));

    private M3NebulaJavaGradeBackportBinding() {}

    public static List<Asset> assets() {
        return List.of(JAVA_GRADE, BACKPORT_PLAN, TRANSFER_CONTRACT, DOCUMENTATION);
    }

    public static List<Step> steps() {
        return STEPS;
    }

    /** The exact candidate can be used for read-only target inventory and adaptation planning. */
    public static boolean evidenceAdmitted() {
        return true;
    }

    /** No recipe replay may become executable from this candidate until Nebula proof is green. */
    public static boolean executableBackportDependency() {
        return false;
    }

    public static boolean sourceMutationAuthority() {
        return false;
    }

    public static boolean semanticEquivalenceAuthority() {
        return false;
    }

    public static boolean compatibilityAuthority() {
        return false;
    }

    public static boolean promotionAuthority() {
        return false;
    }

    public static String root() {
        StringBuilder value = new StringBuilder();
        append(value, SCHEMA);
        append(value, NEBULA_REPOSITORY);
        append(value, NEBULA_REF);
        append(value, NEBULA_COMMIT);
        append(value, Integer.toString(NEBULA_PR));
        append(value, STATE);
        append(value, TARGET_REPOSITORY);
        append(value, APPLICATION_POLICY);
        for (Asset asset : assets()) {
            append(value, asset.id());
            append(value, asset.path());
            append(value, asset.gitBlobSha1());
        }
        for (Step step : STEPS) {
            append(value, Integer.toString(step.ordinal()));
            append(value, step.phase().name());
            append(value, step.repository());
            append(value, step.maximumScope());
            append(value, step.dependsOn());
        }
        append(value, "mutation=false;semantic=false;compatibility=false;promotion=false");
        return sha256(value.toString());
    }

    public static void requireExactCandidate(
            String repository,
            String ref,
            String commit,
            int pr,
            String state,
            String applicationPolicy) {
        if (!NEBULA_REPOSITORY.equals(Objects.requireNonNull(repository, "repository"))
                || !NEBULA_REF.equals(Objects.requireNonNull(ref, "ref"))
                || !NEBULA_COMMIT.equals(Objects.requireNonNull(commit, "commit"))
                || NEBULA_PR != pr
                || !STATE.equals(Objects.requireNonNull(state, "state"))
                || !APPLICATION_POLICY.equals(
                        Objects.requireNonNull(applicationPolicy, "applicationPolicy"))) {
            throw new IllegalArgumentException("Nebula Java-grade candidate handoff drift");
        }
    }

    private static Step step(
            int ordinal,
            Phase phase,
            String repository,
            String maximumScope,
            String dependsOn) {
        return new Step(ordinal, phase, repository, maximumScope, dependsOn);
    }

    private static String text(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty()
                || checked.indexOf('\0') >= 0
                || checked.indexOf('\t') >= 0
                || checked.indexOf('\n') >= 0
                || checked.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String sha1(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{40}")) {
            throw new IllegalArgumentException(field);
        }
        return value;
    }

    private static void append(StringBuilder target, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        target.append(bytes.length).append(':').append(value);
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
