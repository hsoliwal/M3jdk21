// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.pass;

import com.m3.rewrite.scope.M3EditScope;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Canonical bounded multi-pass convergence plan for M3JDK21 tooling.
 *
 * <p>Passes are monotonic in authority. A later pass may see a broader scope, but no pass may
 * retroactively grant broader authority to an earlier recipe. FILE mutation reaches its own fixed
 * point before VISIBILITY/PACKAGE/MODULE work is admitted.
 */
public final class M3MultiPassPlan {
    private final List<Pass> passes;

    private M3MultiPassPlan(List<Pass> passes) {
        this.passes = validate(passes);
    }

    public static M3MultiPassPlan canonical() {
        return new M3MultiPassPlan(List.of(
                new Pass(
                        0,
                        "inventory",
                        M3PassMode.INVENTORY,
                        M3EditScope.FILE,
                        false,
                        List.of(
                                "com.m3.rewrite.verbatim.M3Jdk21SourceFingerprintRecipe",
                                "com.m3.rewrite.atom.M3InventoryPureIntAtomCandidates",
                                "com.m3.rewrite.index.M3SemanticIndexRecipe"),
                        "inventory rows stable for unchanged file preimages"),
                new Pass(
                        1,
                        "file-fixed-point",
                        M3PassMode.TRANSFORM,
                        M3EditScope.FILE,
                        true,
                        List.of("com.m3.rewrite.atom.M3AtomizePureIntReturnRecipe"),
                        "every admitted FILE recipe returns no further source change"),
                new Pass(
                        2,
                        "visibility",
                        M3PassMode.TRANSFORM,
                        M3EditScope.VISIBILITY,
                        false,
                        List.of("com.m3.rewrite.scope.M3VisibilityInventoryRecipe"),
                        "visibility inventory is stable and every requested accessibility delta is explicitly classified"),
                new Pass(
                        3,
                        "package",
                        M3PassMode.RELATION,
                        M3EditScope.PACKAGE,
                        false,
                        List.of("com.m3.rewrite.scope.M3PackageBoundaryRecipe"),
                        "package boundary roots are stable and package/protected relations have no unresolved package-local target"),
                new Pass(
                        4,
                        "module",
                        M3PassMode.RELATION,
                        M3EditScope.MODULE,
                        false,
                        List.of("com.m3.rewrite.index.M3TypeRelationRecipe"),
                        "module-local source type relations have no unresolved in-module target"),
                new Pass(
                        5,
                        "multi-module-fan-in",
                        M3PassMode.FAN_IN,
                        M3EditScope.MULTI_MODULE,
                        false,
                        List.of(
                                "com.m3.rewrite.index.M3WholeSemanticHashRecipe",
                                "com.m3.rewrite.index.M3SemanticIndexM3DbBridgeRecipe",
                                "com.m3.rewrite.dag.M3RecipeDagPlannerRecipe"),
                        "canonical M3IndexDB content hash is stable across repeated fan-in"),
                new Pass(
                        6,
                        "library-api-admission",
                        M3PassMode.ADMISSION,
                        M3EditScope.LIBRARY_API,
                        false,
                        List.of("com.m3.rewrite.scope.M3LibraryApiSurfaceRecipe"),
                        "library API roots are stable and all requested exported-contract deltas are explicitly approved or typed exclusions"),
                new Pass(
                        7,
                        "proof",
                        M3PassMode.VERIFY,
                        M3EditScope.LIBRARY_API,
                        false,
                        List.of("com.m3.rewrite.pass.M3VerificationPlanRecipe"),
                        "diff -> lint -> compile -> tests -> runtime gates satisfy task packet")));
    }

    public List<Pass> passes() {
        return passes;
    }

    public Pass pass(int ordinal) {
        return passes.get(Objects.checkIndex(ordinal, passes.size()));
    }

    public int size() {
        return passes.size();
    }

    private static List<Pass> validate(List<Pass> input) {
        List<Pass> checked = List.copyOf(Objects.requireNonNull(input, "passes"));
        if (checked.isEmpty()) throw new IllegalArgumentException("passes");
        ArrayList<Pass> result = new ArrayList<>(checked.size());
        M3EditScope priorScope = M3EditScope.FILE;
        for (int index = 0; index < checked.size(); index++) {
            Pass pass = Objects.requireNonNull(checked.get(index), "pass");
            if (pass.ordinal() != index) {
                throw new IllegalArgumentException("pass ordinal drift at " + index);
            }
            if (index > 0 && !pass.maximumScope().permits(priorScope)) {
                throw new IllegalArgumentException("pass scope narrows after promotion");
            }
            priorScope = pass.maximumScope();
            result.add(pass);
        }
        return List.copyOf(result);
    }

    public record Pass(
            int ordinal,
            String passId,
            M3PassMode mode,
            M3EditScope maximumScope,
            boolean mutationAuthority,
            List<String> recipeClasses,
            String stopCondition) {
        public Pass {
            if (ordinal < 0) throw new IllegalArgumentException("ordinal");
            passId = token(passId, "passId");
            mode = Objects.requireNonNull(mode, "mode");
            maximumScope = Objects.requireNonNull(maximumScope, "maximumScope");
            recipeClasses = List.copyOf(Objects.requireNonNull(recipeClasses, "recipeClasses"));
            if (recipeClasses.stream().anyMatch(value -> value == null || value.isBlank())) {
                throw new IllegalArgumentException("recipeClasses");
            }
            stopCondition = token(stopCondition, "stopCondition");
            if (mutationAuthority
                    && mode != M3PassMode.TRANSFORM) {
                throw new IllegalArgumentException(
                        "only TRANSFORM passes may carry mutation authority");
            }
        }

        public boolean fileParallel() {
            return maximumScope == M3EditScope.FILE;
        }

        private static String token(String value, String field) {
            String checked = Objects.requireNonNull(value, field).strip();
            if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
                throw new IllegalArgumentException(field);
            }
            return checked;
        }
    }
}
