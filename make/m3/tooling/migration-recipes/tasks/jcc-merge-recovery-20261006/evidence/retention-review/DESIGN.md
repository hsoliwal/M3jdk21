# Immutable-baseline extension to the existing M3 retention owner

Status: frozen afterimage resources for independent review and recipe materialization.
Repository: `hsoliwal/M3jdk21`.
Beforeimage commit: `b71ee5bf88398fb80961c13db03e8c675fb445ce`.

## Problem and scope

The current audit reads its capability catalogue and recipe manifests only from the candidate
commit. Removing a catalogue row, deleting a manifest row or rewriting an expected hash can
therefore remove or redefine the very obligations the candidate is meant to satisfy. A merge
may keep another parent's history reachable while dropping its retained files.

This extension changes the existing `m3/history/retention.py`, its existing tests and Maven
entry point, its existing workflow, and the corresponding README. It adds no second audit
owner or workflow. These five `.txt` files are proposed recipe afterimages; they have not been
written to current product targets or published to GitHub by this task. The parent integrates
them into `jcc-merge-recovery-20261006` through the existing
`M3Jdk21HashPinnedTextSnapshotRecipe` and sealed installer. The parent separately owns the JCC
retention manifests and catalogue rows.

Master advanced after this beforeimage was captured. The receiving parent is responsible for
checking these five exact beforeimage identities against the publication parent. This packet
keeps the original `b71ee5...` identity; it does not silently replace it with the newer head.

## Compatibility

`inspect(repo, commit, catalogue)` keeps its original summary fields, row ordering, root,
target-body classification, output files and legacy parsing behavior. The complete original
inspection body after Git commit resolution is unchanged. The existing `safe_path`, `tsv`,
`blob`, `load_capabilities`, `load_manifest` and `write` function bodies are unchanged. The five
original test method bodies are unchanged.

The opt-in signature adds keyword-only `baseline_commits=()` and `include_parents=False`.
`baseline_commits` must be an immutable tuple of full lowercase Git commit IDs. The existing
`GitGraph.commit_id` and `exact_id` reject abbreviated IDs, moving refs, option-like values,
unavailable objects and non-commit IDs. Duplicate explicit IDs refuse; input order is otherwise
canonicalized. `include_parents` must be a bool. An explicitly supplied empty tuple with false
parent inclusion retains the exact legacy path.

## Opt-in algorithm

1. Resolve the exact target and explicit baseline commits through one `GitGraph` instance.
2. When selected, read **every direct parent** from the target commit, including every octopus
   parent. Merge duplicate roles by commit identity; explicit strictness remains authoritative.
3. Read each baseline's catalogue and every referenced manifest from that baseline's own Git
   objects. Baseline obligations are constructed before the candidate's declarations are read.
4. Read the candidate catalogue and manifests separately. Candidate catalogue/manifest absence
   or valid empty declarations are explicit conflicts, while inherited obligations remain visible.
5. Compare capability bindings, manifest paths, target declarations and expected hashes. Preserve
   all incompatible expectations and their sorted source-commit provenance. Never choose a winner
   by input order. Additional capabilities and target rows add obligations without erasing old ones.
6. Read the target tree once for the union of retained target paths. Reuse `GitGraph.blob_digest`
   so repeated origins or shared blob identities do not repeat body hashing.
7. Classify each distinct capability/manifest/path/hash obligation as exact, drifted or missing.
   Check the same Git refs again before sealing the deterministic summary/root.

An explicit baseline with an absent, empty or unreadable catalogue refuses. A present baseline
catalogue whose manifest is missing, empty, malformed or not a regular blob also refuses. Only
an implicit direct parent's absent catalogue is recorded as `ABSENT_IMPLICIT_PARENT`, with that
exact commit and zero invented obligations. This is a direct-parent observation, not a claim that
all earlier ancestor obligations were checked.

## States, conflicts and output

Actual target body changes with an unchanged declaration remain `PRESENT_DRIFTED_REVIEW`.
They are not classified as semantic loss. Missing target paths remain `MISSING`. Rewriting an
expected hash to match changed content adds declaration conflicts while retaining the old expected
hash and old-origin row. Manifest movement is also explicit even when its rows are unchanged.

Conflicts include candidate catalogue/manifest absence or emptiness, removed capabilities,
changed manifest paths, removed target declarations, changed expected hashes, conflicting
baseline manifest bindings, and incompatible expected hashes at a shared target path. Duplicate
capability IDs and repeated/unsorted manifest target rows refuse as malformed declarations.

The opt-in report uses `m3-retained-capability-baselines/1`. The existing six `RETENTION.tsv`
columns are unchanged. `targets` counts distinct obligation variants; `distinct_target_paths`
counts actual paths. `SUMMARY.json` retains sorted baseline identities/roles, catalogue hashes,
absent-parent observations, obligation provenance and conflict details. All of these fields and
the target rows participate in the root.

`--require-no-missing` exits 3 for either missing target rows or declaration conflicts. Invalid
input/read failures exit 2. Body drift by itself preserves the existing nonfatal review state.

The stricter opt-in parser accepts exactly 64 characters from `0123456789abcdef`, including a
valid all-digit digest, and rejects embedded whitespace. It requires canonical text repository
paths and regular Git modes `100644` or `100755`; symlinks are refused. The legacy parser's
different behavior remains untouched. The manifest's beforeimage and resource-name columns are
not semantic-retention authorities; this audit compares target path and expected postimage hash.

## Maven and workflow wiring

The existing retention Maven POM always opts into all direct parents. Setting
`m3.retentionBaseline` activates a property-based profile that appends a structured
`--baseline-commit` argument; the property has no default value. The CLI independently accepts
repeatable `--baseline-commit` arguments for multiple explicit pins.

The existing workflow keeps its PR-head candidate and adds the immutable PR base as an explicit
baseline. Push events add `github.event.before`, so a multi-commit push cannot evade the baseline
by deleting its catalogue before its final commit. An all-zero push creation sentinel adds no
explicit baseline and prints the resulting parent-only scope. Non-push zero IDs refuse. Manual
dispatch offers an optional immutable baseline input. The Bash script validates the ID and uses
an array to pass the Maven property without shell evaluation.

The workflow's existing path filters become `m3/**`, `src/**`, `test/**` and its own path. This
includes retained receiving docs, tests and evidence rather than only history and recipe resources.
The workflow keeps its existing permissions, runner, time limit, checkout, Java and artifact steps.

This is candidate-owned repository audit wiring. It does not establish branch protection or
trusted execution of a candidate-modifiable workflow. Whole-repository historical coverage,
runtime/JNI correctness, JDK builds and independent module gates remain outside this extension.
No waiver, contract-retirement or branch-mutation operation is added.

## Actual focused verification

The first isolated run is retained under `verification-v01/`:

- Static comparison: six existing helper/output function bodies and the legacy inspection body
  unchanged; all five original test methods unchanged; XML parsed.
- `bash -n`: passed against the extracted exact candidate workflow script.
- Python byte compilation: passed for the candidate owner, tests and exact current `git_graph.py`.
- Python unittest execution: **36 methods passed**, comprising the five original methods and
  31 additional methods. Actual unittest duration: 3.618 seconds. Exit 0.
- All recorded candidate/source inputs remained hash-identical after execution.

The executed cases cover declaration erasure at each layer, complete target-directory loss,
resealing, conflict provenance, safe additions, actual drift, exact baseline ownership, every
octopus parent, pre-push retention, explicit/implicit absence, malformed baselines, unsafe IDs and
paths, duplicate obligations, strict digest/mode behavior, ref movement, repeatable CLI inputs,
and deterministic output. The tests mutate only dedicated synthetic Git repositories.

Actual execution receipt SHA-256:
`a4da326176195fccadb0c8c17e5d55bb2c88aeb02fd4a1f62eaa176bb9855551`.
Full unittest stderr log SHA-256:
`9c3678b9fd8a005d21987fcb3b2a91d2f48c957f33de54aa5a863edfd4d371cd`.

The actual first run began under the receiving parent's authorization to test isolated copies
after self-review. Root subsequently confirmed that authorization was sufficient. No failed test
run was discarded; this first run passed. Maven profile argument merging and Maven execution have
not been executed by this packet. The parent must validate those in the admitted Maven environment
and materialize the reviewed targets through the recipe before claiming repository delivery.
