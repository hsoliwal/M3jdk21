# Correction: the Java snapshot owner requires a Java compilation unit

This additive admission supersedes the PlainText-to-Java-owner assumption in `20261005-m3-ci-admission.md`. Full scanner review found an explicit `J.CompilationUnit` input requirement. The earlier assessment was incorrect. No producer gate or target mutation ran under that assumption, and no executed failure is invented for this static review correction.

The existing `M3RecipeLiteralSyntaxRepairRecipe` and `M3SharedLiteralSyntaxRecipe` were also inventoried. They own fixed historical targets and cannot admit these two fixtures without a source change. Preserve those owners and the Java snapshot owner's typed-input guard.

## Admitted minimal extension

The unchanged current Java snapshot owner will generate a candidate for the valid Java source `synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3HashPinnedTextSnapshotRecipe.java`. Its only behavior extension is in the private text-path admission predicate: add the two exact M3 malformed fixture paths already listed in the original task packet, following the existing explicitly fenced SWT bootstrap path convention. Preserve all eight existing SWT exceptions, every other path/resource/hash/scanner condition, and the public API.

The generated text owner then produces the Cij two-file Java fixture family and the Cit three-file Python/workflow family. Their manifests continue to bind exact preimages and postimages. All Java fixture postimages require actual JavaParser compilation units with exact printed bytes and actual compilation/tests in M3's current migration-recipes module. PlainText generation alone does not satisfy these obligations.

The bootstrap itself is a reusable class-backed recipe with its own exact precondition, fixed point, refusal cases and output receipt. The extended owner's tests must retain existing scope, reject unrelated Java paths and prevent similar-looking paths from receiving admission. No new parser, compiler, recipe engine or convergence lab is created. The existing Java owner's source-kind gate remains unchanged.

The source PR may carry the recipe-generated text-owner candidate and the three linked recipe crates with their proof. The target receives only its five qualified adaptations and the corresponding portable producer/proof lineage. The source owner change is tooling; it is not a JDK product implementation or an authorization to modify other Java targets.

All original source/target pins, immutable trial rules, consumer gates, full-output export obligations and limits remain in force. Independent family admission is not a filesystem transaction. No source is eligible for protected-branch promotion merely because a bounded candidate proof passes.
