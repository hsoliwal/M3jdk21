# JEP423 product-source execution — mastered DAG follow-up

**2026-10-17: rerun requested; no promotion.**

The historical first replay PR #1010 (run 39007584120) passed exact
OpenJDK JEP 423 lineage validation and generated 62 source-sealed FILE
candidate crates, then stopped in the existing Maven recipe DAG before
touching product code. Its console captured three Java 21 errors in
M3A3BackportPreparation and M3PacketDagComposer.

This branch stacks on the source and test repair PR #1013, itself stacked
on Synexia-generated A3 compiler restoration #1009. All original JEP 423
`LINEAGE.tsv`, `CUMULATIVE_ADMIT_PATHS.txt`,
`PATH_DONOR_OVERRIDES.tsv`, `packet.tsv`, and
`CURRENT_TREE_PROOF_REQUEST.tsv` remain unchanged.

The existing `m3-jep423-current-materialize.yml` workflow is the *only*
authority for conditional source transfer:

1. Diff/fixture tests, exact upstream OpenJDK 21 and cumulative JEP 423 donor fetch.
2. 62 source-pinned one-file Java/native/text OpenRewrite crates.
3. Maven recipe/atom DAG with verified mastery, fail-closed.
4. Preimage check, materialize sources only if exact admission passes, fixed-point.
5. Matched fastdebug OpenJDK image and G1 pinning / JNI / JFR jtreg suite.
6. Generate proof receipt, readback; no master merge and no JEP completion claim
   if any named step fails.

This note is a minimal branch-triggered test packet, not a separate backport
generator, recipe, licence override, or an authorization to merge.
