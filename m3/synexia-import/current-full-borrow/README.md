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

`ecfc155eea38b9d4c85e644761a3b2fd2d2f9d1f`

Synexia estate source blob:

`2ae93e0bd4fd03760df5aa5d99b90aafb36f1ba0`

Estate publication commit:

`897b05a4798f514a82b9939851209d2b271926b2`

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

## OpenRewrite recipe mirror coverage — explicit gap

The source estate's `OPENREWRITE_RECIPES` family is a source inventory, not evidence that the entire recipe module has been copied or accepted. A recursive GitHub tree comparison at the pinned source snapshot returned a **truncated** source tree response: 34,388 source blobs were observed, while the target's `m3/vendor/synexia/synexia-openrewrite-recipes` tree contains 15 blobs. Thirteen relative paths have identical Git blob IDs; two existing files differ. At least 34,373 observed source blobs are absent from the target mirror; because the source response was truncated, this is a lower bound, not a complete missing-file count.

The two known divergences are recorded in `recipe-mirror-divergences.tsv`. Their target adaptations are not overwritten or declared equivalent here. The family remains `SOURCE_PIN_ONLY`; the receiver must use the canonical Synexia recipe/qualified output pipeline, and any selected mirror or generated output needs its own exact provenance and target tests. Do not bulk-copy the entire 34k+ observed recipe module into the JDK tree.

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
