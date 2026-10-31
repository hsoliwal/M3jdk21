<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# M3 wrapper and path-mask convergence

This task supplies reviewed source candidates and catalogue evidence to the
existing [`m3-collections-contract-refinement-20261017`](../m3-collections-contract-refinement-20261017/README.md)
recipe owner. The successor dataset is `m3jdk21-wrappers-r2-20261018`, with 28
source targets against `hsoliwal/M3JDK21@07ba6e383343d32c83cfdaf641387bbe72bf325d`:
five existing production Java files, one existing JNI C file, and 22 test sources.
The newly merged bounded-navigation tests remain unchanged receiver oracles
outside these 28 writes. Catalogue and verification metadata are separate text
recipe outputs and are not counted as runtime source targets.

The corrected implementation and its 25 source-bound candidate records are
complete. [`catalogue/CANDIDATE_STATUS.json`](catalogue/CANDIDATE_STATUS.json)
records that result and reserves independent bindings for actual SDK execution,
generated-receiver Java/JNI acceptance, and the immutable canonical publication
pin. Root supplies those receipts separately. No original pending marker or older
receipt should be interpreted as evidence about a different source epoch.

## Reused Synexia invariant and recipe

The user-required M3 invariant is the existing
[Synexia recipe-first invariant](../../../docs/M3-SCALE/invariants/SYNEXIA-LLM-RECIPE-FIRST-2.md):
maintain reusable, validated recipes; refine one source file at a time while
preserving its public contract; compare algorithm choices through the existing
problem-category catalogue and donor evidence; retain failures and their
regressions. Behavioral atoms may be replaced independently when their composed
contract remains established. The
[canonical M3JDK21 ownership invariant](../../../docs/M3-SCALE/invariants/SYNEXIA-M3JDK21-CANONICAL-OWNERSHIP-1.md)
continues to govern source ownership and receiving phases.

This task adds no recipe engine, compiler, parser, catalogue search service, or
Synexia runtime dependency. The existing generic recipe is
`com.synexia.m3jdk.CollectionsContractRefinement20261017`. Its unchanged
`M3HashPinnedJavaSnapshotRecipe` and `M3HashPinnedTextSnapshotRecipe` owners consume
the successor's exact Java/text preimages and postimages. The dataset resides in
the existing crate's `datasets/m3jdk21-wrappers-r2-20261018` directory. This task's
`candidates/` directory is reviewed input; actual SDK Results are the receiving
authority.

From the canonical Synexia repository root, using Java 21, Maven and Python 3:

```sh
cd synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017
mvn -B -ntp -Dm3.dataset=m3jdk21-wrappers-r2-20261018 -Pcontract verify
python3 receive.py --dataset m3jdk21-wrappers-r2-20261018 --target /absolute/m3-checkout --check
python3 receive.py --dataset m3jdk21-wrappers-r2-20261018 --target /absolute/m3-checkout
```

The existing POM pins the SDK dependencies. Cached runs may add `-o`; an
environment-specific Maven repository may be supplied with `-Dmaven.repo.local`.
The target must contain the sealed source epoch or the permitted fixed point;
changed input bytes require a source-sealed successor. These commands are the
existing recipe invocation, not a claim that every command has already passed
for the eventual published receiver.

## Review, correction and evidence

[`catalogue/FINAL_FILE_REVIEW.tsv`](catalogue/FINAL_FILE_REVIEW.tsv) binds all 28
source paths to their 07ba preimages and final candidate hashes. It reuses the
initial per-file atom/donor review and adds the corrected navigation contract.
[`catalogue/COVERAGE.tsv`](catalogue/COVERAGE.tsv) resolves each candidate gate to
its exact log and source IDs; partial candidate coverage does not fill the actual
owning-module, public API, native ABI, or receiver gates.

The initial 26-file uncommitted local donor is preserved by
`FROZEN_INPUT_INVENTORY.json`, `FILE_REVIEW.tsv`, `HISTORICAL_README.md`, and
`history/initial-authoring/`. Its broad `IllegalArgumentException` fallback is
rejected permanently in `REJECTED_CANDIDATES.json`. The corrected source avoids
the comparator retry and swallowed opaque-owner exception, handles stored null
at any comparator position, and retains lazy predicate traversal, live views,
read-only guards, and existing mutation owners. Inherited GitHub, Guava,
LeetCode, HackerRank and GeeksforGeeks donor definitions remain intact; the
successor selection and current proof are linked separately. License and
attribution records retain their existing per-file grants.

The 25 candidate records include strict Java 21 compilation of 141 inputs, 140 new
failure/admission checks, 90,431 unchanged merged bounded-navigation checks,
16,429,309 navigation checks across 7,894,400 queries,32,267 read-only checks,
deep filtering at 65,536 levels, all five Java/five required-JNI mask owners,
621,981 order/window checks, and exact original read/equality tapes. Raw commands,
source identities and logs are retained in `catalogue/candidate-proof/`.

## Admission and runtime limits

Bounded anchors are admitted for exact `TreeSet`, `ConcurrentSkipListSet`,
`TreeMap`, `ConcurrentSkipListMap`, their JDK-owned nested range/key views, and
final `M3TreeSet`/`M3TreeMap` owners. Private M3 filter chains retain their original
wrapped traversal. Custom subclasses and opaque decorators keep their original
query range operation and exception demand; bounded-query enhancement is not
claimed for those owners. Successful comparator call counts are not claimed
identical for every refined lookup. Existing callback-prefix and failure oracles
remain explicit evidence boundaries.

The JNI change shares the existing primitive region gather owner between int
and full signed 64-bit row IDs. No comparator or predicate crosses JNI; no critical
array pin, retained Java pointer or per-row object store is added. Required-native
admission remains explicit. Candidate native execution covers the recorded local
Linux library; independent missing-library, actual receiver ABI/header, and other
platform evidence belong to their own gates. Constant-mask fast paths preserve
mandatory shape/geometry validation, and are not a claimed native speedup.

This is a finite proving-owner pass. It does not establish whole JDK/Guava/SWT/
Eclipse/Spring/Apache SDK closure, thread safety or serialization for arbitrary
wrappers, a full JDK image/JCK, or measured CPU/heap/native-memory improvements.
The separately tracked AST/compiler/build requirements and global receiving
phase retain their existing owners and evidence status.
