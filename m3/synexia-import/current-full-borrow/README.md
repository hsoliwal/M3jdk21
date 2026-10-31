# Current Synexia full-borrow estate receiver

Status: **source-pinned receiver plan; no family acceptance implied**.

Controlling target policy remains:
- `m3/docs/m3jdk21-porting-invariant.md`
- `m3/docs/name-mapping.json`
- `m3/docs/SYNEXIA_CONVERGENCE_MODEL.md`

Controlling Synexia ownership remains:
`SYNEXIA-M3JDK21-CANONICAL-OWNERSHIP-1`.

## Source custody

Synexia source revision:

`296323958b1019edd59b60b9c05cb148d024cfe5`

Synexia estate source blob:

`292bc31a96cd3d20c36d617045c6b01a7a7c8faf`

Source PR:

`hsoliwal/com.synexia#9860`

The exact estate bytes are mirrored at:

`m3/synexia-import/current-full-borrow/synexia-estate.tsv`

Synexia receiving-DAG blob:

`3e6c56b8555231fb36d0ac620ab112aa5970d45a`

The exact DAG bytes are mirrored at:

`m3/synexia-import/current-full-borrow/synexia-dag.tsv`

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

`m3/synexia-import/current-full-borrow/promotion-phases.tsv`.

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
