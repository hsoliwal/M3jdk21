# Upstream source-backed 3-way patching and current backport proof recovery

Status: candidate / exact-current-source repair, **not** JEP product admission.

## Motivation and exact ancestor

OpenJDK JDK22–27 source integration frequently meets a current-tree file that
preserves M3/Java21-specific code while an upstream donor changed other lines.
The source-based backport intake must **retain both independent edits** rather
than bulk-copy a donor file or falsely claim that a context mismatch proves the
change incompatible.

The incumbent owner is `m3/backports/commit_patch_atoms.py`, already
source-custodied by Synexia [PR #11638](https://github.com/hsoliwal/com.synexia/pull/11638)
and M3 receiver [PR #935](https://github.com/hsoliwal/M3jdk21/pull/935).
This work adapts its existing target-local `apply_patch` execution, not a new
diff engine or a portable recipe implementation.

### Before

Normal Git `apply --check` refuses a small donor patch on a target with an
independently added header, returning `CONFLICT_CONTEXT` even though Git can
merge the two original source histories correctly. A named historical fixture
was red in hosted backport admission. Another CLI refusal fixture wrote literal
`\\n` into its selected path instead of the intended physical LF, so the
test failed on path parsing rather than checking the output symlink guard.

### After

1. Retain the exact strict Git `apply --check` / `apply` fast path.
2. Only when strict matching fails and the **exact OpenJDK parent blob** is
   available, store that blob in a newly initialized throwaway Git repository.
3. Let Git itself perform `git apply --3way` against the committed current
   target preimage. Accept only a clean three-way outcome; do not use
   `--unidiff-zero`, zero-context matching, fuzzy text anchors, or
   source-overwriting conflict resolution.
4. A three-way conflict returns the original typed `CONFLICT_CONTEXT`.
   The throwaway workspace is destroyed. No file in the real target checkout
   changes; only source-sealed candidate bytes and hashed evidence are written
   under the separate scratch output root.
5. Extend the retained regression: donor modified `int v=1` to `int v=2`,
   while M3 retained its independent header. Both survive in candidate output.
   Without donor parent, or with a wrong parent, the patch is refused.
   Incompatible target edits also remain typed conflicts.
6. Recover the physical LF filename and selected-path fixtures so the
   existing NUL-delimited Git-path and symlink non-write-through tests exercise
   their actual intended conditions.

The SHA-256 candidate and unchanged `APPLICABILITY.tsv` schema remain
authoritative. A clean merge is **not** a proof of Java21 compatibility or a
license to merge/execute HotSpot changes.

## Nebula admission gate

A separate source error in the same Maven/backport test phase contained a
literal LF inside an unterminated Python string in
`m3/backports/nebula_proof_admission.py`. Its exact target-only one-byte
escape repair restores deterministic JSON output while retaining the entire
original HOLD receipt and all fail-closed Nebula/Synexia authority checks.
The original test is extended to assert real JSON plus one physical LF.

## Proof and promotion

- `python3 -m compileall -q m3/backports`
- `python3 -m unittest discover -s m3/backports -p 'test_*.py' -v`
- `mvn -B -ntp -f m3/backports/pom.xml verify`
- Follow with existing full Maven recipe/JUnit, A3, source-sealed
  fixed point and matched OpenJDK configure/make/jtreg/runtime gates for any
  specific materialized JEP packet.

This PR is stacked on prior exact five-file lost-source recovery PR #1019.
Treat all unrun/failed steps as PENDING; no automatic promotion or source
replacement is authorized.
