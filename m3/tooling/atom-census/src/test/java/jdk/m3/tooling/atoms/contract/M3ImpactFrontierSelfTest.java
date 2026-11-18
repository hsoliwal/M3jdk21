/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.tooling.atoms.contract;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Dependency-free Java 21 regression corpus for file-local and downstream impact. */
public final class M3ImpactFrontierSelfTest {
    private static final String HASH = "a".repeat(64);
    private static int assertions;

    private M3ImpactFrontierSelfTest() {}

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("m3-frontier-");
        try {
            Path atoms = dir.resolve("ATOMS.tsv");
            Path evidence = dir.resolve("EVIDENCE.tsv");
            Files.writeString(atoms,
                    AtomCoverageLedger.ATOMS_HEADER + "\n"
                    + atom("A", "src/m/F.java", "PATTERN_A", true)
                    + atom("B", "src/m/G.java", "PATTERN_B", true)
                    + atom("C", "src/n/H.java", "UNKNOWN_PATTERN", true)
                    + atom("D", "src/z/I.java", "PATTERN_D", true)
                    + nativeAtom("J", "src/m/Native.java", "JAVA_NATIVE_DECLARATION")
                    + nativeAtom("N", "src/native/native.c", "JNI_EXPORT")
                    + atom("X", "src/m/F.java", "PATTERN_X", false),
                    StandardCharsets.UTF_8);
            Files.writeString(evidence, AtomCoverageLedger.EVIDENCE_HEADER + "\n",
                    StandardCharsets.UTF_8);
            AtomCoverageLedger ledger = AtomCoverageLedger.read(atoms, evidence);
            List<M3ImpactFrontier.Dependency> edges = List.of(
                    new M3ImpactFrontier.Dependency("B", "X"),
                    new M3ImpactFrontier.Dependency("C", "B"),
                    new M3ImpactFrontier.Dependency("A", "C"), // cycle; no infinite loop
                    new M3ImpactFrontier.Dependency("J", "N"), // Java JNI declaration -> native implementation
                    new M3ImpactFrontier.Dependency("B", "J"), // caller of native API
                    new M3ImpactFrontier.Dependency("B", "X")); // duplicate; no extra meaning
            M3ImpactFrontier.Plan first = M3ImpactFrontier.plan(ledger, edges, List.of("A"));
            equal(List.of("A", "B", "C", "X"), first.affectedAtoms());
            equal(List.of("src/m/F.java", "src/m/G.java", "src/n/H.java"),
                    first.affectedFiles());
            equal(List.of("owner-src/m", "owner-src/n"), first.affectedOwners());
            check(!first.blockers().isEmpty(), "incomplete evidence must remain visible");
            check(first.blockers().stream().anyMatch(s -> s.startsWith("C\tUNKNOWN_PATTERN")),
                    "unknown pattern must remain a blocker");
            check(first.blockers().stream().anyMatch(s -> s.startsWith("X\tMISSING_GATE\t")),
                    "evidence missing for affected atom");
            check(first.blockers().stream().noneMatch(s -> s.startsWith("D\t")),
                    "unaffected evidence should not leak into a minimal frontier");
            check(!first.mutationAuthority() && !first.promotionAuthority()
                    && !first.semanticAuthority(), "planning cannot authorize source changes");

            List<M3ImpactFrontier.Dependency> reversed = new ArrayList<>(edges);
            Collections.reverse(reversed);
            equal(first, M3ImpactFrontier.plan(ledger, reversed, List.of("A", "A")));
            equal(first.rootSha256(),
                    M3ImpactFrontier.plan(ledger, reversed, List.of("A")).rootSha256());
            M3ImpactFrontier.Plan onlyB = M3ImpactFrontier.plan(ledger, edges, List.of("B"));
            equal(List.of("A", "B", "C", "X"), onlyB.affectedAtoms());
            // A is reachable from C on the cycle; its sibling X is conservatively revalidated.
            M3ImpactFrontier.Plan nativeImpact = M3ImpactFrontier.plan(ledger, edges, List.of("N"));
            equal(List.of("A", "B", "C", "J", "N", "X"), nativeImpact.affectedAtoms());
            equal(List.of("src/m/F.java", "src/m/G.java", "src/m/Native.java",
                    "src/n/H.java", "src/native/native.c"), nativeImpact.affectedFiles());
            check(nativeImpact.blockers().stream().anyMatch(s -> s.startsWith("N\tMISSING_GATE")),
                    "native ABI gates must remain explicit blockers");
            M3ImpactFrontier.Plan untouched = M3ImpactFrontier.plan(ledger, edges, List.of("D"));
            equal(List.of("D"), untouched.affectedAtoms());
            equal(List.of("src/z/I.java"), untouched.affectedFiles());
            check(untouched.rootSha256().matches("[0-9a-f]{64}"), "receipt hash");
            rejects(() -> M3ImpactFrontier.plan(ledger,
                    List.of(new M3ImpactFrontier.Dependency("NOPE", "A")), List.of("A")));
            rejects(() -> M3ImpactFrontier.plan(ledger, List.of(), List.of("NOPE")));
            rejects(() -> M3ImpactFrontier.plan(ledger, List.of(), List.of()));
            rejects(() -> new M3ImpactFrontier.Dependency("bad\tfield", "A"));
            rejects(() -> new M3ImpactFrontier.Plan(first.ledgerSha256(), first.changedAtoms(),
                    first.affectedAtoms(), first.affectedFiles(), first.affectedOwners(),
                    first.blockers(), first.graphRootSha256(), "0".repeat(64)));
            rejects(() -> new M3ImpactFrontier.Plan(first.ledgerSha256(), first.changedAtoms(),
                    first.affectedAtoms(), first.affectedFiles(), first.affectedOwners(),
                    List.of("not sorted", "ahead"), first.graphRootSha256(), first.rootSha256()));
            System.out.println("M3_IMPACT_FRONTIER_PASS\tassertions=" + assertions);
        } finally {
            try (var children = Files.list(dir)) {
                for (Path p : children.toList()) Files.deleteIfExists(p);
            }
            Files.deleteIfExists(dir);
        }
    }

    private static String atom(String id, String file, String pattern, boolean contractLocked) {
        String owner = file.startsWith("src/z/") ? "owner-src/z"
                : file.startsWith("src/n/") ? "owner-src/n" : "owner-src/m";
        return String.join("\t", id, file, "JAVA_METHOD", owner, "()V", HASH,
                pattern, "contract-locked", Boolean.toString(contractLocked)) + "\n";
    }

    private static String nativeAtom(String id, String file, String kind) {
        String owner = file.endsWith(".c") ? "owner-src/native" : "owner-src/m";
        return String.join("\t", id, file, kind, owner, "native-signature", HASH,
                "JNI_BRIDGE", "jni-abi-locked", "true") + "\n";
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    private static void equal(Object expected, Object actual) {
        check(expected.equals(actual), "expected=" + expected + " actual=" + actual);
    }

    private static void rejects(Runnable action) {
        assertions++;
        try { action.run(); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("expected IllegalArgumentException");
    }
}
