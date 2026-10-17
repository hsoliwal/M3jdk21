# JEP 423: current-tree source materialization attempt — 2026-10-17

Status: REQUESTED / NO PROMOTION. This note triggers the already-existing GitHub Actions
`m3-jep423-current-materialize.yml` workflow on the named proof branch;
it does not grant a second patch/recipe authority.

## Inputs

- Base: `fix/m3jdk21-a3-cir-literal-20261017` (draft PR #1009), itself based
  on `master@970a34350ebdf6a3f1c5ff5acc8445d0f3515f55`.
- Exact upstream three-commit JEP 423 lineage and 62 admitted FILE paths
  remain defined by `CURRENT_TREE_PROOF_REQUEST.tsv`, `LINEAGE.tsv`,
  `CUMULATIVE_ADMIT_PATHS.txt`, and `PATH_DONOR_OVERRIDES.tsv`.
- JEP 423 baseline: `openjdk/jdk jdk-21+35`.
- Final approved donor commit: `0d5f5e15d43f94a79c6133baecd5af217365d176`.
- Ancestors: `38cfb220ddadbb401cc15f313aadb8234f626210`
  and `8643cc21333c6b51242ed3b9295b25f372244755`.

## Why rerun

Previous hosted run 39006830879 passed source lineage/FILE crate generation but
failed Maven `a3` compilation before candidate source materialization:
`A3MemoryCompiler.java` contained literal LF/CR controls inside char literals.
The prior qualified Synexia Cir recipe already contains the remedy. PR #1009
recovers exact Cir `target-1`, `target-3`, and `target-5` postimages, including
the migration-recipe manifest, without adapting source bytes by hand.

## Acceptance (unchanged)

1. OpenJDK 21 preimages, JEP lineage, 62-file donor/source seals.
2. Synexia Maven/OpenRewrite recipe DAG compiled and tested.
3. Candidate output materialized only after exact preimage acceptance,
   with fixed point and changed-path check.
4. Fastdebug JDK image built from the matched Java+HotSpot sources.
5. Pinned G1 region pinning, JNI critical region, GC/JFR, and preserved
   Java21 regression tests pass.
6. Exact postimage receipt and independent readback.
7. A separate explicit review decides any MODULE/MULTI_MODULE acceptance.
8. No source/tree promotion or user-PR merge occurs automatically.

An unsuccessful stage remains a visible failure and must not be represented as a
completed JEP 423 backport. Its first failing compiler/recipe/native/runtime gate
is the next repair item. Source-import changes remain in this proof branch only.
