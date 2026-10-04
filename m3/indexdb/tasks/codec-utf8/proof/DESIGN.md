# Exact-branch database proof

Continue PR82 at 83288e6458408a1a182d8c99624418a82f670a3d.
Observed PR-triggered run37179958840 failed without a job. No cause or source/test
verdict is inferred. An attempted rerun was refused; do not retry in a loop.

The existing workflow executes push events only for the PR79 branch. Add exactly
the existing PR82 branch to the SAME push allowlist. Keep every job, permission,
command, source-SHA selection, pinned JDK hash and unchanged0.99 coverage gate.
This is an additional execution route, not a bypass of tests or a permission
expansion. Source snapshots and artifact receipts still name the actual tested SHA.

Reuse M3Jdk21HashPinnedTextSnapshotRecipe with crate codec-proof to own the one-file
workflow change. Keep the old ABSENT-before workflow crate untouched for provenance.
ProofTest tests replay, no-op, drift refusal and identical job body. This packet
adds no product implementation, public API, parser, dependency, or function type.

Maven/JUnit success and coverage success must be reported separately. Do not mark
the PR ready without its remaining recipe and coverage obligations. The exact
branch may be removed by a later separately-reviewed cleanup; never erase history.
