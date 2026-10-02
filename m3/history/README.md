# M3 commit-chain and patch-signal audit

Status: review-only history tooling. This is not acceptance or restoration of a modified JDK.
Original design direction: Hitesh Soliwal. Independent tooling: Apache-2.0 under `m3/LICENSE`.

## Why this exists

The reverse pass found `67876a03d823f5ff5ca92ca1545a026d757c4b12`, which attaches
historical PR #3/#5/#6 heads while reusing its first parent's tree. The preceding
`d6390ea3bb348f0c22afdba0819ca4ec0e97970f` does the same for #4. This preserves
reachability, not the side-parent implementation. Neither an ancestry check nor
an empty first-parent diff establishes a semantic superset.

The evidence-backed built *candidate* is `3776d6e674d6c9b04539ca24aca2504aa88d4a57`.
Its checked-in receipt reports 86 selected flag-off passes and 16 enabled passes
with two failing StringJoiner tests. It is interpreter-only and is NOT a fully
accepted last-good JDK. The user has not supplied a separately verified exact
last-good revision. Never turn these historical receipts into current-head passes.

The inspected master is `f18b9d6b2d43dcec36adc46dc915e623333e613c`, tree
`8b82b91c3be3af7dfad2f3c1d2541ae9167b930d`. Direct comparison against the candidate
finds missing canonical runtime classes and runtime evidence/test directories.
This is a net tree difference; it does not assert that master previously contained
those files and a particular first-parent commit deleted them.

## Scope and reuse

Reuse native Git for graph traversal and binary-capable exact patches, Python's
standard library for records, and the existing standalone Maven exec-plugin
convention for recipe invocation. Do not build another parser/refactoring engine.
The existing `m3/migration`, `m3/tooling`, foundation recipes and name-mapping
remain their respective authorities. This module supplies PSE/CCPS evidence to
those owners; it does not replace their scanners, recipes or mapping schema.

`git_graph.py` owns immutable Git-object reads and NUL-safe raw-diff decoding.
`audit.py` owns deterministic graph reduction and evidence publication.
`test_audit.py` owns synthetic repository contract tests.
`snapshot.json` owns the frozen upstream/candidate/target/ref boundary.
`ATOM_PLAN.tsv` records proposed semantic work and coupled promotion boundaries.
These are cohesive responsibilities, not arbitrary tiny-method extraction.

## Execution

Python 3.10+ and Git are required. Run from a complete local clone; the output
must be a new directory outside both the worktree and Git administration paths:

```sh
python3 -B -m unittest discover -s m3/history -p 'test_*.py' -v
python3 -B m3/history/audit.py --repo . --snapshot m3/history/snapshot.json --output /tmp/m3-history-audit
```

The default snapshot is immutable. It intentionally does not follow a moving
master or branch name. Missing commits, shallow history, unrelated roots,
pre-existing outputs, source-local outputs and budget overruns are refused.
Cancellation propagates between Git subprocesses; each subprocess has a timeout.
There is no apply/merge/reset/rebase/delete mode and no worker writes to canon.
Do not run against a concurrently modified object database; the tool detects ref
movement but cannot make noncooperating writers transactional.

The Maven entry point reuses exec-maven-plugin 3.6.4 already used by
`m3/migration/pom.xml`, without making Maven an OpenJDK bootstrap dependency:

```sh
mvn -f m3/history/pom.xml verify -Dm3.repo=/absolute/clone -Dm3.output=/tmp/m3-history-audit
```

Maven acceptance must be recorded separately from direct Python execution.

## Reverse and forward passes

1. Validate full object IDs and the common upstream boundary. Freeze the set of
   branch tips from the snapshot, not timestamps and not a recent-search limit.
2. Collect the union of reachable commits excluding upstream ancestry. Order
   parents before children with a SHA-tiebroken topological sort; the reverse
   ledger is the exact reversed sequence. Side branches older than the candidate
   remain visible, because time filtering would lose their later-merged work.
3. Inspect **every parent edge**, including all octopus parents. Record full
   old/new modes and object IDs, status, path, and SHA-256-addressed binary-capable
   patch. Never substitute a combined merge diff or first-parent-only traversal.
4. Flag first-parent-tree reuse with different side-parent trees as a review
   obligation, not an automatic accusation or semantic-equivalence conclusion.
5. Compare candidate and target trees at every fork-touched path. Emit exact
   equality, missing content or different-content review, not an automatic merge.
6. Hash changed blobs once and reuse the facts. Exact clusters are proposals;
   normalized/semantic clustering is explicitly not performed by this tool.
7. Emit ledgers, coverage limits and a digest of all artifacts. Ref movement,
   incomplete reads or output-budget failures leave an INCOMPLETE marker and no
   complete audit receipt. Restart uses a fresh output; existing output is never
   overwritten or silently treated as a successful checkpoint.

Git references: `git rev-list`, `git diff-tree`, `git cat-file` and `git ls-tree`.
The command log and implementation digest are part of each run's provenance.
Mechanical traversal coverage is not semantic review, compile success, test
success or runtime acceptance. `FINAL_REPORT.md` explicitly keeps these separate.
The optional PR collector records current PR #1..#17 metadata as a separately
captured witness. A PR head outside the frozen branch snapshot is a coverage gap,
not silently folded into an earlier snapshot.

## M3 authority and locks

Task source: the supplied `M3Scale(20261002-025837).txt`, v3.0 Locked Canon,
SHA-256 `5ec31d51207bda02d2f1f9a7fcd940f2ae5e1bfc9f0ef9358139fe62b47f393b`.
The supplied Copy is an expanded witness, not identical canon. CodingAgent is
an interpretation/extension witness; its refined semantic-leaf guidance is
recorded without silently replacing locked law. Original uploads and private
Synexia source bodies are not republished here.

Public API, external behavior, java.base/HotSpot/JNI production files, installed
JDK, existing workflows, existing recipe pins and excluded paths are LOCKED.
Only this new tooling, its docs and its scoped read-only workflow are candidates.
Docs precede code. Verification proceeds diff -> lint/syntax -> byte compilation
-> fixture tests -> actual history read. A successful history read never promotes
runtime source. One worker, one shard, no distributed execution or model calls.

## Recovery/patternization order

Read `ATOM_PLAN.tsv`. First establish the exact candidate/target contract and
restore the *evidence and recipe custody* through a reviewed proposal. Then
reconcile storage admission, ownership/lifetime, UTF-16 ranges, composition,
precomputed facts, Java operation bridges and the coordinated VM/JNI boundaries.
The String layout and its VM consumers form one coupled promotion boundary,
not independent file-level merges. Keep the mapped-kernel/shadow-route work and
the prefix/migration work as separate candidates; do not overwrite newer work.

Use existing exact-source/OpenRewrite recipes for transformations. Every proposed
atom needs a stable name, input/output/effect/exception/concurrency/resource
contract, pattern participant role, source lineage, tests and exact-head receipt.
Unknown semantics remain typed residue. Hash equality is byte identity, not a
proof of behavioral equivalence. Do not add foreign annotation dependencies to
java.base simply to label patterns; use this ledger/Javadoc first.

No blind cherry-pick, blanket revert, current/incoming/both text concatenation,
ancestry-only 'superset' acceptance, or replacement String.class is authorized.
