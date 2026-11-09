# M3jdk21 reverse/forward investigation — local inspection receipt

## Result and exact scope

The GitHub connector returned master `f18b9d6b2d43dcec36adc46dc915e623333e613c`, tree
`8b82b91c3be3af7dfad2f3c1d2541ae9167b930d`, and 26 branch tips. PR #1 through #17
were read as history/intent witnesses, not accepted from their descriptions as verified code.
The initial recent-commit search is bounded, not a complete history walk.

The direct candidate-to-target comparison reports **ahead 143, behind 0**, with
`3776d6e674d6c9b04539ca24aca2504aa88d4a57` as its merge base. Nevertheless its canonical
`java.lang.MIndexString` / `MIndexStringPool` and `m3/runtime-integration`,
`m3/runtime-segments`, `m3/runtime-stage1` content is absent from the target comparison.
This is not proof those files once existed on master's first-parent line.

## Root-cause landmarks

`d6390ea3bb348f0c22afdba0819ca4ec0e97970f` (2026-10-02 01:28:12 UTC) explicitly
preserves the current tree while attaching PR #4's `fbe45aec...` head.
`67876a03d823f5ff5ca92ca1545a026d757c4b12` (01:34:08 UTC) has parents, in order:

1. `d6390ea3bb348f0c22afdba0819ca4ec0e97970f`
2. `950ed81e65dee3b16d777a1f2d7dfbad83c48c20` (stage-2 head)
3. `3dbb233235d5e3d539ed2cce645e6e2b2952576c` (segmented experiment head)
4. `3776d6e674d6c9b04539ca24aca2504aa88d4a57` (built MIndex candidate)

Both inspected commit objects have tree `26554d002cf158c97dd4ffbb06b3885b647c0a17`.
The latter is therefore provably tree-identical to its first parent, even though
its message calls the operation a superset. Reachability is preserved; side-tree
content was not integrated by that commit. A simple re-merge or first-parent
revert is not a reliable recovery: plan exact source reconciliation instead.

## Historical baseline, not a new test claim

PR #6's checked-in `results.json` at `3776d6e...` has build_exit=0, image hashes,
86 selected flag-off passes, enabled 16 passes / 2 failures, and explicit remaining
gates. The two failed files are StringJoinerTest (2 expectations) and
StringJoinerOomUtf16Test (4 expectations). Compiled mode and full jtreg/JCK/live SA
remain unverified. Its 25-file exact-source recipe is a donor for reviewed recovery,
not permission to replay it blindly over the current alternate layout.

## Forward lineage to retain

Foundation / singleton reuse (#1/#2); experimental segmented and canonical atoms
(#3/#4); built interpreter safety (#5/#6); shared-atom documentation (#7);
ancestry-convergence metadata (#8–#10); alternate mapped kernel and shadow facade
(#11/#15); migration routes and recipe tooling (#12/#13); migration handoff and
worked documentation (#14/#17); prefix facts and source-bound mappings (#16).
Their current branch/merge metadata must be captured independently. These are
capability families, not a license to concatenate incompatible implementations.

## Files changed

Only new `m3/history/**` files and `.github/workflows/m3-history-audit.yml` are
proposed. No pre-existing runtime, build, workflow, recipe-pinned README, license,
private source, or installed JDK is changed. The new tooling has no apply mode.

## Verification actually executed

In order, against a local publication fixture: proposed diff whitespace check,
Python tabnanny, Python byte compilation, then **31 regression tests** against
isolated real Git repositories. Cases cover all merge parents, octopus/ancestry-only
merges, forward/reverse agreement, binary patches, arbitrary path encoding,
reintroduced files, deterministic artifact hashes, source/index/ref non-mutation,
resource/path/shallow-history refusal, PR pagination and credential-safe redirects.
These are tooling tests, not target JDK tests. Empty diff/lint/compile logs mean
those commands exited successfully; they are not omitted failures.

## Blockers and limits

The execution container has no direct GitHub DNS access and no Maven; the remote
desktop connector reports no attached device. A complete real target Git walk,
Maven lifecycle, actual code atomization and whole-JDK recovery were not executed
locally. The scoped workflow can run the frozen graph audit on GitHub; its later
status and outputs must be recorded separately, never inferred from this receipt.

The audit's output is change/patch inventory, not a full semantic read of every
method. No full cleanup, restoration, runtime acceptance or speedup is claimed.

## Artifact paths

Task/authority: `../../TASK_PACKET.json`; frozen refs: `../../snapshot.json`;
semantic work ledger: `../../ATOM_PLAN.tsv`; recipe and instructions: `../../README.md`.
This directory contains the mandatory M3 local inspection and verification ledgers.
The full Git execution, when run, emits its own independent output contract outside
the source checkout.

## Primary evidence endpoints

- https://api.github.com/repos/hsoliwal/M3jdk21/git/commits/67876a03d823f5ff5ca92ca1545a026d757c4b12
- https://api.github.com/repos/hsoliwal/M3jdk21/git/commits/d6390ea3bb348f0c22afdba0819ca4ec0e97970f
- https://github.com/hsoliwal/M3jdk21/compare/3776d6e674d6c9b04539ca24aca2504aa88d4a57...f18b9d6b2d43dcec36adc46dc915e623333e613c
- https://github.com/hsoliwal/M3jdk21/blob/3776d6e674d6c9b04539ca24aca2504aa88d4a57/m3/runtime-integration/evidence/results.json
- https://github.com/hsoliwal/M3jdk21/blob/3776d6e674d6c9b04539ca24aca2504aa88d4a57/m3/runtime-integration/recipe/manifest.json
- https://git-scm.com/docs/git-rev-list
- https://git-scm.com/docs/git-diff-tree

## Forward observation during publication

Draft PR #20 was created at 2026-10-02 03:23:01 UTC. Its base had advanced to
`4a81f3b3050fa5572ec1ff9368e2c383231db2e6`. The fetched Git commit has the **same**
tree `8b82b91c3be3af7dfad2f3c1d2541ae9167b930d` as the original target. It is another
explicit ancestry-only merge, attaching PR #18's `8960a30...` head while retaining
the first-parent tree. This adds a third confirmed landmark; the endpoint content
finding survives because the two target tree IDs are equal.

PR #18 is marked merged but its metadata is not content-retention proof. PR #19
is a separate draft retained-owner compatibility/recipe increment, observed at
`584932a5f631bf8eb40b9525d18184085be1a404`; its own full-JDK and migration gates remain
open. These observations extend, rather than overwrite, the initial snapshot.
`snapshot-forward-0323.json` supplies a separately frozen #1..#19 witness boundary
and newer target/tip. Use it with the same CLI via `--snapshot` when running the
forward pass. The scoped workflow remains explicitly pinned to the original
snapshot; no later target pass is claimed to have run.

## Observed hosted attempt

GitHub created audit run `36959929426` for code commit
`ffe4e25aaf361c77687758c91e16c9d3905356c1`. The run reports **failure**, and the jobs
endpoint returned `total_count: 0, jobs: []`. Thus neither its fixture stage nor its
real-repository graph stage ran on GitHub. The API evidence here does not establish
why no job started. It must not be called a script test failure or a hosted pass.
Local exact-published-source verification remains **31 tests PASS**.

Audit run: https://github.com/hsoliwal/M3jdk21/actions/runs/36959929426
Jobs evidence: https://api.github.com/repos/hsoliwal/M3jdk21/actions/runs/36959929426/jobs
Forward commit: https://api.github.com/repos/hsoliwal/M3jdk21/git/commits/4a81f3b3050fa5572ec1ff9368e2c383231db2e6
Draft work: https://github.com/hsoliwal/M3jdk21/pull/20
