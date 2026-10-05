# In-memory Java 21 recipe laboratory

Task packet: active. Mode: apply on a new candidate branch only. Base: `081780ea02c11e8fd41a5445ac169b6a28ac5d25` (PR #114); complete recovered tree: `02854e6c82fdd4f07fd1bc79a442bc237743950e`.

## Inputs and scope

The user requires the existing atomizer, patternizer/IOP and documentation recipes to improve through bounded in-memory project permutations, compiler diagnostics, behavior comparisons, regression retention and fixed-point tests. Use the actual `M3Java21ConvergenceRecipe`, `M3PureIntConvergenceRecipe`, OpenRewrite 8.17.1 and JavaCompiler; do not create a replacement transformation engine or compiler. The original project is immutable and is never rewritten to match a failing candidate.

Approved scope: existing migration-recipes owners, their test support/resources and this task directory. Preserve all existing tests, public contracts, source-bound images, native ABI, POM versions, coverage thresholds and other workflows. No canonical branch write, merge, rebase, force-push, runtime installation or private Synexia source export. FILE candidate production grants no visibility/package/module/library promotion.

## Actual starting evidence

GitHub Actions artifact `11322552678` from run `37255987007` contains the exact complete base source, Maven 3.9.16 and resolved dependency cache. Its ZIP SHA-256 is `a0662117e005bb2afce6c66678fcef7dd878d0ec142d09c2ee56bbf6f0601676`; every internal manifest entry and reconstructed Git tree were verified locally. This is a source export, not upstream Git ancestry. Local execution uses Debian OpenJDK 21.0.11, not the artifact runner's Temurin image.

The unmodified nine-project control reactor was actually run offline with `-fae clean verify`. It fails compilation in the existing A3 algorithm-catalogue recipe at the generic SourceFile setter chain. Independent indexdb and module-pack test/coverage gates pass. PR #113 already contains the relevant algorithm-owner repair; reuse that exact reviewed source rather than invent a competing fix, retaining provenance and the source-sealed recipe pathway.

## Required laboratory behavior

Compile original and recipe-produced projects independently in memory, collect diagnostics, use isolated class loading, compare public surfaces and declared observations, and retain the original as oracle across every pass. Exercise finite operator/parenthesis/name/layout/declaration-order combinations, deliberately unsafe lookalikes, comments/literals, lexical shadowing, side effects and exception paths. Positive cases must actually transform and acquire the admitted atom/pattern/documentation structure. Negative cases must refuse without code drift.

Replay a frozen recipe/DAG until a verified fixed point or explicit bounded failure; one exhausted pass is not convergence. Compare source maps and per-pass changes, not hash equality alone. Test permitted order permutations only; do not assume dependency-bearing recipes commute. Seeded faulty candidates must be detected by the same behavior checker. Every failure becomes a regression or explicit remaining obligation.

## Verification and output contract

Order: exact diff -> static/diff checks -> compile -> JUnit -> runtime -> source-bound reporting. Full project static-analysis/coverage/image gates remain independently required; a syntax check is not a lint or test pass. Run the existing default reactor and retain all failures rather than adding skip flags or exclusions. The existing >=99% line and branch gates remain unchanged.

Produce STATUS.tsv, FINAL_REPORT.md, RUN_CONTEXT.tsv, PROVENANCE.tsv, VERIFY_CONTRACT.tsv and OUTPUT_CONTRACT.tsv. Preserve the complete programme denominator from `tasks/map-utf/TODO.tsv`, including ordinary String/HotSpot/JIT/GC/JNI/CDS, all collection and compiler/parser/database owners, compatible JEP/non-JEP/Temurin donors, naming/IOP and optional module packs. A green laboratory is not whole-JDK acceptance.
