<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 release evidence and admission

**Current release state: HOLD.** The [visible queue](QUEUE.md) records pending,
failed and blocked work. A clean metadata check, a merged PR, a completed recipe,
or a checksum manifest does not admit the SDK for release.

M3JDK21 is the integrated Java 21 product. Synexia owns canonical recipe
source, execution, mastery fixtures and improvement memory. This repository
receives executed Java/HotSpot/JNI postimages, target-authoritative tests,
license notices and data-only receipts. Historical target-local recipes stay
preserved until their exact bytes, dependencies and callers are reconciled in
Synexia; the release workflow refuses to execute that residue.

## Three distinct results

| Result | Meaning |
| --- | --- |
| `boundary_ready` | Required policy, claims, naming sequence, clean source and upstream ancestry checks passed. |
| `evidence_integrity_ready` | Boundary checks and every mandatory source-bound receipt/artifact integrity check passed. This does not authenticate self-authored JSON or establish that a command ran. |
| `release_ready` | Strict `--release` validation passed, including fresh official GitHub run/job/step verification against qualified producer policies for every mandatory gate. |

Development mode always reports `release_ready: false`. Missing, stale,
malformed, failed or empty evidence holds release. A GitHub run with zero jobs,
a selected job with no steps, skipped gate steps or incomplete job pagination
cannot qualify. Local receipts remain useful review evidence; they cannot
independently admit a release. Tests of this verifier use clearly synthetic
receipts and simulated API responses; their success is not a product gate.

The current `trusted_gate_producers` object is empty because no producer has
been independently qualified for this release slice. It must stay visibly
pending until the actual workflows, named jobs/steps, source blobs, raw results
and producer ownership are reviewed. Adding `status: QUALIFIED` is a reviewed
policy/source change, never an automated consequence of a green metadata test.

## Visible intermediate output

The workflow retains each attempt under:

```text
m3/release/queue/runs/<run-id>-<attempt>-<source-sha>/<job>/
  admission.json
  queue.tsv
```

The contract job also records source `HEAD`, `TREE`, `WORKTREE` and checksums
when it reaches those steps. Reports are uploaded with `if: always()` so a
failing custody or admission gate remains reviewable. Each TSV row includes
status, source commit/tree, evidence reference, next action and detail.
The static `queue.json` references the existing naming authority and four
queued naming operations; it is a work index, not a competing symbol registry.

New intermediate reports use the visible queue. Historical output under
`target/m3-release-proof`, qualification packets and previous receipts is
preserved in place; compare its recorded source before reuse. No historical
receipt is moved, overwritten or relabeled as current acceptance.

```sh
# Metadata and missing-evidence report; this never admits release.
python3 m3/release/verify_release_admission.py --root . \
  --json-out m3/release/queue/runs/local/admission.json \
  --queue-out m3/release/queue/runs/local/queue.tsv \
  --sha256-out m3/release/queue/runs/local/SHA256SUMS

# Target-only verifier regressions; no source recipe runs here.
python3 -B -m unittest discover -s m3/release -p 'test_*.py' -v

# Explicit custody preflight before a legacy recipe-bearing job.
python3 m3/release/verify_release_admission.py --root . \
  --custody-check control-reactor \
  --json-out m3/release/queue/runs/local/custody.json \
  --queue-out m3/release/queue/runs/local/custody.tsv

# Final gate, only after all independent producers have been qualified.
python3 m3/release/verify_release_admission.py --root . --release \
  --evidence /absolute/path/to/proof-pack/proof-pack.json --verify-github \
  --json-out m3/release/queue/runs/final/admission.json \
  --queue-out m3/release/queue/runs/final/queue.tsv
```

`--verify-github` uses the official `gh api` command with fixed-host,
read-only GET endpoints. It uses the already configured GitHub authentication;
it does not create credentials, mutate workflows, merge branches or publish a
release. Unavailable CLI/authentication/network access leaves release blocked.
The verifier never runs commands supplied by an evidence file.

## Source and output leases

Run final admission from an isolated, complete source checkout. Product builds,
Maven output and virtual environments belong in separate build workspaces. The
verifier inventories ignored files as well as ordinary untracked files; only
`m3/release/queue/runs/` is an exempt generated-report root. There is no generic
`**/target` or build-directory exemption. Sparse/skip-worktree and
assume-unchanged index entries prevent exact-source admission.

Source identity is captured before policy reads, and policy bytes are read from
the immutable recorded Git commit. After producer verification, the verifier
rehashes the proof pack, receipts and raw artifacts, then rechecks source
commit/tree and complete worktree cleanliness. A concurrent commit, tracked
edit, newly ignored input or changed proof artifact holds admission. Results
include the verified source identity and proof-pack SHA-256. Keep the checkout
and proof pack immutable and rerun the final gate before publishing that exact
commit; a result does not authorize a later source revision.

All three output arguments are checked before validation and again before
writing. Within the checkout, they must use the exact visible report subtree
and cannot overwrite a tracked historical report. Output paths may not overlap
Git metadata, the proof input directory or each other; symlink/hard-link aliases
are refused. Safe reports outside the source checkout and proof directory are
also supported. Unsafe destinations produce an explicit refusal on standard
output, exit with status 2, and are not written. Report writes use replacement
rather than truncating an existing inode.

## Proof pack format

`release-contract.json` remains the mandatory-gate authority. A proof pack is
external generated evidence; do not commit a fabricated passing example.
Its JSON object has:

- `schema: M3_RELEASE_PROOF_PACK_V1`;
- `source.repository: hsoliwal/M3jdk21`, exact 40-character `commit` and `tree`;
- `gates`: one entry per mandatory gate, with `id` and a `receipt` descriptor
  containing a relative `path` and lowercase SHA-256.

Each receipt is a `M3_RELEASE_GATE_RECEIPT_V1` object containing the same
source binding, its `gate`, `status: PASS`, integer `exit_code: 0`, the actual
`command` vector, `environment` (`os`, `arch`, `toolchain`), positive integer
`checks.executed`, integer `checks.failed: 0`, nonempty raw-result `artifacts`
with path/hash descriptors, and a completed `execution` record.

For a GitHub Actions execution, record `kind: GITHUB_ACTIONS`, `status:
completed`, path/hash descriptors for the raw `run` and fully paginated `jobs`
responses, positive `job_ids`, and the actual `required_steps` names. The run
must be successful and source-bound; each selected job must match its run and
source and contain successful executed gate steps. A local execution uses
`kind: LOCAL` and remains integrity/review evidence only.

Artifacts must be nonempty regular files below the proof directory. Absolute,
parent-traversal, symlink, duplicate and self-referential artifact paths are
refused. JSON duplicate keys and unrecognized execution providers are refused.
Checksums protect bytes from accidental or deliberate alteration; they do not
authenticate authorship or prove that the recorded result is true.

The source-controlled `trusted_gate_producers[gate]` policy separately pins:

- `status: QUALIFIED` after independent review;
- the exact `.github/workflows/*.yml`/`.yaml` path and current `workflow_blob`;
- approved job names and each job's required executed step names.

Strict admission fetches the actual run and all current-attempt jobs from
GitHub, rechecks repository/source/workflow/attempt identity, and enforces the
approved names and completed results. Stored successful JSON cannot override
a fresh failed, missing, different or rerun workflow. A reviewed workflow must
actually establish its named gate; this verifier is not a semantic proof of
arbitrary workflow code or a substitute for the target tests themselves.

The `PROOF_PACK_SHA256` producer hashes the raw payloads before creating its
own receipt; it must not introduce a self-referential checksum cycle. Preserve
the final pack digest externally with the release review record.

## Actual release scope and remaining gates

The P0 foundation is an independently built immutable-text substrate with a
repository test surface. Its existence does not establish complete ordinary
`java.lang.String` replacement, full regex precompute, full jtreg coverage,
qualified arrays/collections/SWT integration, production readiness or a
performance win over OpenJDK. All existing `NOT_CLAIMED` entries remain so.

Receiving order remains **String → arrays → collections → AST/compiler →
remaining families**. Public JDK package, class, constructor and method names
remain unchanged. A helper rename requires its existing mapping ID, typed
Synexia recipe, full caller/reflection/serialization/JNI closure and exact
receiving tests. A successful name census alone does not authorize a rename.

Keep complete release evidence for source/upstream lineage, rights/licenses,
OpenJDK sanity/platform builds, foundation tests, Java/JNI differential tests,
UTF-16 contracts, source-workspace Maven/OpenRewrite qualification and target
receiving closure, release claims and raw-result checksums. The legacy
`M3_MAVEN_CONTROL_REACTOR` gate ID is retained for continuity; historical
`m3/pom.xml` recipe wiring is custody residue, not an authorized execution route.

Before any performance claim, preserve exact candidate and baseline images,
same-host/toolchain comparisons, cold/warm measurements, CPU time, allocations,
retained Java heap, native/process memory, GC, startup, raw repetitions and
variance. Challenge categories (LeetCode, HackerRank, GeeksforGeeks) guide
candidate comparison; their presence in a catalogue proves neither copying
rights nor superiority. Licensed GitHub donors need per-file provenance and
contract-specific measurement.

## Append-only release history

Preserve the documented OpenJDK ancestor and every development commit. The
release verifier no longer demands one squashed fork commit. Never rebase
`develop`, rewrite public development history, force-push or squash to satisfy
a release gate. Use reviewed additive changes and real merge commits with
verified parents. Tags and released history remain immutable.

Ancestry is necessary evidence of preservation, but does not prove that every
historical behavior survived. The existing canonical history/code-loss recipe
must compare relevant branch changes and actual current owner atoms before
recovery. Never restore an old whole-file snapshot over an evolved owner merely
because the older PR's receipt is missing.

References: [release contract](release-contract.json), [visible queue](QUEUE.md),
[benchmark and compatibility specification](BENCHMARK_AND_COMPATIBILITY_SPEC.md),
[naming contract](../docs/M3JDK21_NAME_AND_PACKAGE_CONTRACT.md),
[GitHub CLI API](https://cli.github.com/manual/gh_api),
[workflow-run API](https://docs.github.com/en/rest/actions/workflow-runs),
[workflow-job API](https://docs.github.com/en/rest/actions/workflow-jobs).
