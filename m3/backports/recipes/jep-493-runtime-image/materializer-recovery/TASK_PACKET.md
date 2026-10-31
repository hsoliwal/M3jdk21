# JEP 493 generated-crate materializer recovery

Status: recovery candidate; no OpenJDK product mutation or completion claim.

## Exact base

Current master: `b71ee5bf88398fb80961c13db03e8c675fb445ce`.

Merged PR #144 added a generic FILE-crate executor, tests, JEP493 materialization policy,
receipt/README updates and an exact build/jtreg workflow. Those six files are absent or stale on
the current master tree even though their history remains merged.

## Allowed delta

Recover exactly the six reviewed PR #144 files:

- `.github/workflows/m3-jep493-runtime-image-materialize.yml`
- `m3/backports/materialize_generated_crates.py`
- `m3/backports/test_materialize_generated_crates.py`
- `m3/backports/recipes/jep-493-runtime-image/MATERIALIZATION_POLICY.tsv`
- current JEP493 receipt update
- current JEP493 README materialization section

No `src/**`, `test/**`, POM, dependency, coverage threshold, public API or native ABI change.

## Authority

Physical generated crates remain FILE replay units. JEP493 feature join remains MULTI_MODULE.
The executor has no compatibility or promotion authority. It materializes exact already-reviewed
crate postimages only after hash/preimage/path/symlink/ownership validation and proves fixed point.

## Verification

diff -> Python compile/unit tests -> JEP493 generator/accounting -> packet/DAG proof ->
ephemeral exact materialization -> changed-path fence -> fixed point -> configure with
`--enable-linkable-runtime` -> images -> runtimeImage jtreg -> legacy jlink/JMOD regressions ->
real no-JMOD smoke.

Hosted exact-head evidence is required before any source backport is promoted.
