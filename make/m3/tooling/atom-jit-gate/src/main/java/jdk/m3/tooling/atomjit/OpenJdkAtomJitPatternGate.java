// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.atomjit;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Structural admission for one canonical source-sealed M3JDK 21 Java/JNI recipe plan.
 *
 * <p>Each file/atom is reviewed serially. An interpreted JVM witness and a compiled C2
 * witness must have the same behavior digest and the same pinned runtime binary. A native
 * plan additionally requires a separate Java/native parity witness on that runtime before patternization. These are
 * evidence-shape checks, not authentication of external test results, actual HotSpot
 * compilation, JNI/GC safety, Java API equivalence, or product promotion authority.
 * Plan construction (donors, contracts, Maven recipe custody) stays with the donor's universal recipe planner; this
 * gate receives only a sealed plan root and its ordered targets ({@link Plan}).
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/b65df53330e3d82f6b2d5509ded971f19883f4ca/synexia-m3-recipe/src/main/java/com/synexia/m3/recipe/M3OpenJdkAtomJitPatternGate.java blob fa51c5df12d5; recipe m3/tooling/atom-jit-gate/recipe/atom-jit-gate-20261106/relocate.py v4
public final class OpenJdkAtomJitPatternGate {
    public enum Phase {
        ATOMIZE, JAVA_COMPILE, JIT_XINT, JIT_C2, JNI_PARITY,
        PATTERNIZE, DIFFERENTIAL, TARGET_JTREG
    }

    public enum Status { PASS, HOLD, NOT_RUN, FAIL, NOT_APPLICABLE }

    private static final String NONE = "-";

    /** Source lane of one planned atom: Java only, or crossing the JNI boundary. */
    public enum SourceLane { JAVA, JNI_NATIVE }

    /** One planned atom: logical source path, atom identity, sealed before-envelope root and source lane. */
    public record Target(String logicalPath, String atomId, String beforeEnvelopeRoot, SourceLane lane) {
        public Target {
            logicalPath = checkedText(logicalPath, "logicalPath");
            atomId = checkedText(atomId, "atomId");
            beforeEnvelopeRoot = sha(beforeEnvelopeRoot, "beforeEnvelopeRoot");
            lane = Objects.requireNonNull(lane, "lane");
        }
    }

    /** One canonical serial work order: its sealed root and its targets in execution order. */
    public record Plan(String root, List<Target> targets) {
        public Plan {
            root = sha(root, "plan root");
            targets = List.copyOf(Objects.requireNonNull(targets, "targets"));
            if (targets.isEmpty()
                    || targets.stream().map(Target::atomId).distinct().count() != targets.size()) {
                throw new IllegalArgumentException("empty or duplicate serial atom plan");
            }
        }
    }

    public record Receipt(String path, String atomId, String beforeEnvelopeRoot,
            Phase phase, Status status, String proofRoot, String jdkBinaryRoot,
            String behaviorRoot, boolean compiledMethodObserved) {
        public Receipt {
            path = checkedText(path, "path");
            atomId = checkedText(atomId, "atomId");
            beforeEnvelopeRoot = sha(beforeEnvelopeRoot, "beforeEnvelopeRoot");
            phase = Objects.requireNonNull(phase, "phase");
            status = Objects.requireNonNull(status, "status");
            proofRoot = optionalSha(proofRoot, "proofRoot");
            jdkBinaryRoot = optionalSha(jdkBinaryRoot, "jdkBinaryRoot");
            behaviorRoot = optionalSha(behaviorRoot, "behaviorRoot");
            if ((status == Status.PASS || status == Status.NOT_APPLICABLE)
                    && NONE.equals(proofRoot)) {
                throw new IllegalArgumentException("unsealed evidence: " + phase);
            }
            if (status == Status.NOT_APPLICABLE && phase != Phase.JNI_PARITY) {
                throw new IllegalArgumentException("only JNI_PARITY may be N/A");
            }
            boolean runtime = phase == Phase.JIT_XINT || phase == Phase.JIT_C2
                    || phase == Phase.JNI_PARITY;
            if (status == Status.PASS && runtime &&
                    (NONE.equals(jdkBinaryRoot) || NONE.equals(behaviorRoot))) {
                throw new IllegalArgumentException("missing JVM/JNI behavioral witness: " + phase);
            }
            if ((!runtime || status != Status.PASS)
                    && (!NONE.equals(jdkBinaryRoot) || !NONE.equals(behaviorRoot))) {
                throw new IllegalArgumentException("unexpected JVM/JNI witness: " + phase);
            }
            if (compiledMethodObserved != (phase == Phase.JIT_C2 && status == Status.PASS)) {
                throw new IllegalArgumentException("C2 witness must name an observed compiled method");
            }
        }
    }

    public record Review(int atoms, boolean patternizeReady,
            boolean structurallyQualified, List<String> blockers, String root) {
        public Review {
            if (atoms < 1) throw new IllegalArgumentException("atoms");
            blockers = List.copyOf(Objects.requireNonNull(blockers, "blockers"));
            root = sha(root, "review root");
        }
        public boolean promotionAuthority() { return false; }
        public boolean independentlyVerified() { return false; }
    }

    private OpenJdkAtomJitPatternGate() { }

    public static Review review(OpenJdkAtomJitPatternGate.Plan plan,
            List<Receipt> submitted) {
        Objects.requireNonNull(plan, "canonical plan");
        List<Receipt> receipts = List.copyOf(Objects.requireNonNull(submitted, "receipts"));
        var targets = plan.targets();
        if (receipts.size() != Math.multiplyExact(targets.size(), Phase.values().length)) {
            throw new IllegalArgumentException("incomplete serial per-atom phase ledger");
        }
        // All files in one canonical work order must be witnessed by the SAME
        // source-built JDK. Per-file Xint/C2 agreement alone admits mixed SDKs.
        String planJdkBinary = null;
        boolean patternReady = true;
        boolean fullyQualified = true;
        ArrayList<String> blockers = new ArrayList<>();
        int index = 0;
        for (var target : targets) {
            boolean precedingComplete = true;
            String interpreterBehavior = null;
            String interpreterJdk = null;
            for (Phase phase : Phase.values()) {
                Receipt row = receipts.get(index++);
                if (!target.logicalPath().equals(row.path())
                        || !target.atomId().equals(row.atomId())
                        || !target.beforeEnvelopeRoot().equals(row.beforeEnvelopeRoot())
                        || phase != row.phase()) {
                    throw new IllegalArgumentException("ledger order/source drift: " + target.atomId());
                }
                boolean notApplicable = phase == Phase.JNI_PARITY
                        && target.lane() != OpenJdkAtomJitPatternGate.SourceLane.JNI_NATIVE;
                if (row.status() == Status.NOT_APPLICABLE && !notApplicable) {
                    throw new IllegalArgumentException("native phase cannot be waived");
                }
                if (notApplicable && row.status() != Status.NOT_APPLICABLE) {
                    throw new IllegalArgumentException("Java-only atom must record JNI N/A with proof");
                }
                boolean complete = row.status() == Status.PASS
                        || row.status() == Status.NOT_APPLICABLE;
                if (row.status() == Status.PASS && !precedingComplete) {
                    throw new IllegalArgumentException("phase passed before prerequisite: " + phase);
                }
                if (row.status() == Status.PASS && phase == Phase.JIT_XINT) {
                    interpreterBehavior = row.behaviorRoot();
                    interpreterJdk = row.jdkBinaryRoot();
                    if (planJdkBinary == null) {
                        planJdkBinary = interpreterJdk;
                    } else if (!planJdkBinary.equals(interpreterJdk)) {
                        throw new IllegalArgumentException(
                                "mixed JVM binaries across canonical atom plan");
                    }
                } else if (row.status() == Status.PASS && phase == Phase.JIT_C2) {
                    if (!row.behaviorRoot().equals(interpreterBehavior)
                            || !row.jdkBinaryRoot().equals(interpreterJdk)) {
                        throw new IllegalArgumentException("JIT behavior or JDK binary mismatch");
                    }
                } else if (row.status() == Status.PASS && phase == Phase.JNI_PARITY) {
                    // Native parity has its own Java/native oracle and need not hash the
                    // same output as an unrelated Java-only JIT compaction workload.
                    if (!row.jdkBinaryRoot().equals(interpreterJdk)) {
                        throw new IllegalArgumentException("JNI/JIT target JDK binary mismatch");
                    }
                }
                if (!complete) {
                    precedingComplete = false;
                    blockers.add(target.atomId() + ":" + phase + ":" + row.status());
                }
                if (phase == Phase.JNI_PARITY && !precedingComplete) {
                    patternReady = false;
                }
            }
            if (!precedingComplete) fullyQualified = false;
        }
        String root = root(plan.root(), receipts);
        return new Review(targets.size(), patternReady, fullyQualified, blockers, root);
    }

    private static String root(String planRoot, List<Receipt> rows) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            frame(digest, "M3_OPENJDK_SERIAL_ATOM_JIT_PATTERN_GATE_V2");
            frame(digest, planRoot);
            for (Receipt row : rows) {
                frame(digest, row.path());
                frame(digest, row.atomId());
                frame(digest, row.beforeEnvelopeRoot());
                frame(digest, row.phase().name());
                frame(digest, row.status().name());
                frame(digest, row.proofRoot());
                frame(digest, row.jdkBinaryRoot());
                frame(digest, row.behaviorRoot());
                frame(digest, Boolean.toString(row.compiledMethodObserved()));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static void frame(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private static String checkedText(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.length() > 4096
                || checked.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String sha(String value, String field) {
        String checked = Objects.requireNonNull(value, field);
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return checked;
    }

    private static String optionalSha(String value, String field) {
        return NONE.equals(value) ? NONE : sha(value, field);
    }
}
