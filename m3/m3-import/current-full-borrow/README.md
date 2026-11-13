# Current Synexia full-borrow estate receiver

Status: **source-pinned receiver plan; no family acceptance implied**.

Controlling target policy remains:
- `doc/m3/m3jdk21-porting-invariant.md`
- `doc/m3/name-mapping.json`
- `doc/m3/M3_CONVERGENCE_MODEL.md`

Controlling Synexia ownership remains:
`SYNEXIA-M3JDK21-CANONICAL-OWNERSHIP-1`.

## Source custody

Synexia source revision:

`296323958b1019edd59b60b9c05cb148d024cfe5`

Synexia estate source blob:

`7ffcc48f68e3804899e45c5d058b583ac655d5ea`

Source PR:

`hsoliwal/com.synexia#9860`

The exact estate bytes are mirrored at:

`m3/m3-import/current-full-borrow/m3-estate.tsv`

Synexia receiving-DAG blob:

`3e6c56b8555231fb36d0ac620ab112aa5970d45a`

The exact DAG bytes are mirrored at:

`m3/m3-import/current-full-borrow/m3-dag.tsv`

M3JDK21 does not reinterpret that file as target acceptance. Every copied row begins and remains
source inventory until target-side receipts advance it.

## Copyright / license boundary

Qualified Synexia-original copyrightable expression preserves:

`Copyright 2026 Hitesh Soliwal and contributors`

and Apache-2.0 where the exact source/component is classified first-party Synexia Apache work.

This receiver does not claim copyright over abstract ideas/algorithms as such and does not relicense
OpenJDK or third-party donor bodies. Mixed first-party/donor rows remain review-required and cannot
enter the automatic copy lane.

## Explicit 32-family closure

The source estate now contains **32 explicit families**. `MINDEX_STRING_RUNTIME`,
`MINDEX_AST_RUNTIME`, `DAG`, and `OBJECT` are separately pinned even though broader runtime or
data-structure trees also contain them. The receiver therefore cannot lose those named legacy
families behind umbrella custody.

The mirrored dependency DAG must cover every estate family exactly once and remain acyclic.
Inventory may execute independently; family promotion remains serial and proof-gated.

## Receiving states

A target row may be only one of:

- `SOURCE_PIN_ONLY` — source estate is known; no target materialization accepted;
- `VENDOR_CUSTODY` — exact Apache-2.0 source bytes copied under target custody only;
- `TARGET_ADAPTER` — exact target-specific adapter/receiver exists but product acceptance is open;
- `ACCEPTED` — all required target compiler/runtime/API-ABI/JNI/jtreg/platform/fixed-point receipts exist.

This packet initially records **SOURCE_PIN_ONLY** for every family.

Changing a row to `VENDOR_CUSTODY`, `TARGET_ADAPTER`, or `ACCEPTED` requires an additive
successor receipt. Editing the status alone is invalid.

## Promotion-phase authority

The mirrored serial phase plan is:

`STRING -> ARRAYS -> COLLECTIONS -> AST_COMPILER -> REMAINING_FAMILIES`.

It is stored at:

`m3/m3-import/current-full-borrow/promotion-phases.tsv`.

`ARRAYS` is deliberately a **target promotion phase**, not a new Synexia source family. It consumes
qualified foundation/data-structure/native mechanics, while M3JDK21 retains Java array and VM
authority for fixed length, reified component type, covariance/`ArrayStoreException`, bounds,
identity/clone/overlapping `arraycopy`, GC barriers and JNI acquire/release behavior.

Every phase remains `PLANNED` in this receiver. The phase file cannot advance a family or runtime
state by itself.

## Ordered product work

The existing target order remains authoritative:

`STRING -> ARRAYS -> COLLECTIONS -> AST_COMPILER -> REMAINING_FAMILIES`

Inventory/research may proceed ahead. Runtime promotion may not.

The current String target owner remains M3JDK21's existing `java.lang.M3String` /
`M3StringOwner` / `M3StringAtom` / `M3StringTuple` / `M3StringPool` family. Synexia names do
not define public JDK ABI.

## No runtime dependency

This receiver never creates a Synexia runtime service or Maven dependency behind java.base,
HotSpot, JNI or ordinary String. Qualified source/mechanics are materialized into target-owned
runtime boundaries and independently verified there.

## Immutable source identifiers and receiver verification

The source schemas and canonical invariant path in this packet describe the
recorded Synexia revision. Their `SYNEXIA` spelling is provenance data. Renaming
M3JDK21 implementation packages, classes or directories does not rename those
historical source identifiers. The receiver checker must compare them with the
original values while retaining the exact donor Git blob checks.

The receiver accepts `M3JDK21_SYNEXIA_FULL_BORROW_PIN_V1` in `pin.tsv`, the
`SYNEXIA_M3JDK21_FULL_BORROW_V1` estate schema,
`SYNEXIA_M3JDK21_FULL_BORROW_DAG_V1` DAG schema and
`SYNEXIA_M3JDK21_PROMOTION_PHASE_V1` phase schema. Its canonical source invariant
remains `docs/M3-SCALE/invariants/SYNEXIA-M3JDK21-CANONICAL-OWNERSHIP-1.md`.
The target paths retain their current `m3/m3-import/` names.

Run the receiver from a target checkout with the recorded baseline in its Git
history, then run its independent regression tests:

```sh
python3 -B make/m3/compatibility/check_m3_full_borrow_receiver.py
python3 -B make/m3/compatibility/test_m3_full_borrow_receiver.py
```

The first command checks real target ancestry and the complete packet. The
regression suite reads the checked-in source bytes and exercises schema/hash
drift, cycles, phase order, self-promotion and missing historical evidence.
Its ancestry unit cases explicitly mock success, unrelated history and missing
history, and check the exact Git arguments. These unit cases do not replace the
first command's actual ancestry check.

Passing either check does not advance `SOURCE_PIN_ONLY`, create a runtime port,
or qualify a JDK image. It establishes that this immutable receiving record is
internally consistent and remains within its original scope.


## Source chronology after the historical String receipts

The existing String successor and observation tables now append exact target
source observations for eight restoration edges already present in the product
history. Their original headers and complete historical row bytes are preserved
as prefixes, checked by regression tests. Source paths, recipe names and
`synexia_*` provenance columns keep their historical spelling even when current
product names differ.

| Owner | Observed receiving history | Source-only scope |
| --- | --- | --- |
| `M3String` | [#2536](https://github.com/hsoliwal/M3jdk21/pull/2536), `a1d954c40142227f05e85e219bfe51373f0b8247` | Range-join source restoration |
| `M3StringPool` | [#2536](https://github.com/hsoliwal/M3jdk21/pull/2536), then [#2626](https://github.com/hsoliwal/M3jdk21/pull/2626) | Local/native allocation cleanup and exact tuple-coordinate shortcuts |
| `M3StringAtom` | [#2759](https://github.com/hsoliwal/M3jdk21/pull/2759), `bf040ef338ddfc70a30d7ec7bdff38276f5f0418` | Native allocation failure cleanup |
| `M3StringFacts` | [#2739](https://github.com/hsoliwal/M3jdk21/pull/2739), `b1df0c0d60cbccc14f8ea54461948ae8b124f756` | Existing bigram/trigram fact-mixer accessors |
| `M3TQ` | [#2622](https://github.com/hsoliwal/M3jdk21/pull/2622), then [#2710](https://github.com/hsoliwal/M3jdk21/pull/2710) | Adaptive fact lookup and compact flat-query metadata |
| `M3StringPositionPrecompute` | [#2626](https://github.com/hsoliwal/M3jdk21/pull/2626), `d9b4cb26b0e3d4b9097e05eb7ff193e828f41eb9` | Bulk-window source restoration; the later accounting correction has the separate canonical source binding below |

Each observed source edge was compared with the exact file in its real parent
commit and the exact restoration commit. All six distinct restoration commits
were verified as ancestors of target input
`0163b1474862ca463a64f24b45b0c6ca36aa4e73`. Matching current source coordinates
does not reconstruct those older canonical recipes, prove their semantic
correctness, close the Pool/Atom history gaps, or qualify their runtime behavior.
The appended records explicitly retain `NOT_RECONSTRUCTED`, `UNRECONSTRUCTED`,
`PARTIAL`, `NOT_CLAIMED` or `NOT_ACCEPTED` where applicable.

The subsequent Position accounting correction is a separate source-only edge:
`7b25004afe2b11ad87de90bae698d51b5533afb8` to
`b4c733004be251a751def941b3bdc27a77dc2fab`. Its data-only observation in the checker
binds canonical commit `8ac9585a24f3b69d89874f08e7ae4c9325c2f8cd`, recipe
`com.synexia.m3.MigrationVerificationContinuationJava20261112`, the exact typed
Java `002.txt` resource and manifest, and resource SHA-256
`3dfed2c88a4a78f13701e816ccf550401c2658d8bcbecf91cf35c3b23b8d0cb6`.
The manifest's preimage was verified against the real target input `0163b147`;
its postimage was compared byte for byte with the resource at the immutable
canonical commit. The source PR remains `NOT_RECORDED` at capture and the target
PR is [#2953](https://github.com/hsoliwal/M3jdk21/pull/2953). This new exact source
binding does not reconstruct the older restoration or promote runtime state.

The existing phase checker still rejects changed hashes, unknown or reordered
rows, contradictory predecessors, altered source schemas, removed responsibilities
and attempted runtime promotion. Its final `promotion=false` remains mandatory.
Every data row must have exactly the declared number of columns; missing cells
and extra cells, including empty trailing cells, are refused before interpretation.
The tests also reject a changed single-parent observation relationship and prove
that the original three-row String chain and all other historical table prefixes
remain byte-exact.

```sh
python3 -B m3/m3-import/current-full-borrow/check_string_phase.py
python3 -B -m unittest discover -s m3/m3-import/current-full-borrow \
  -p 'test_string_phase_successors.py' -v
```

These are source custody and refusal checks. A passing source check is not an
installed-image, JNI/GC, JIT, jtreg, performance or phase-admission receipt.
