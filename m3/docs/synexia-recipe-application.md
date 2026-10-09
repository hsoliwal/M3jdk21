# Synexia recipe application receipt

Status: **applied product state; Synexia owns the reusable recipes**.

## Canonical recipe owners

### M3 String history convergence

Repository: `hsoliwal/com.synexia`  
Custody PR: https://github.com/hsoliwal/com.synexia/pull/9597  
Custody branch: `m3/m3jdk21-string-recipe-canonicalize-v2-20261007`  
Pinned custody revision: `e0845c4fd0fe56ecf1fa0e88981620062d9dafee`

This supersedes the earlier custody receipt for PR #9529. PR #9597 is based directly on the
then-current Synexia `develop` head and forward-seals the live M3JDK21 product postimages without
moving runtime ownership out of M3JDK21.

Named recipe:

`com.synexia.rewrite.M3Jdk21StringHistoryConvergence`

The Synexia donor owns:

- hash-pinned Java/text snapshot recipe engines;
- the `m3jdk21-string-history-convergence` hash-pinned Java crate;
- the `m3jdk21-string-history-convergence` hash-pinned text/native/workflow crate;
- reviewed postimages and original preimage SHA-256 values;
- fixed-point / missing-target / drift-refusal recipe tests.

### M3 TQ convergence

Existing TQ recipe custody remains under the earlier Synexia handoff:

- PR: https://github.com/hsoliwal/com.synexia/pull/9491
- branch: `m3/m3jdk21-recipe-custody-20261006`
- pinned revision: `5291e3867224be653da89cd69e3b764b2fab213f`
- recipe: `com.synexia.rewrite.M3Jdk21TqConvergence`

## Applied M3JDK21 state

M3JDK21 runtime branch:

`m3/m3string-runtime-precompute-fix-20261005`

Applied/fixed-point revision:

`7dc72fdf9733951bcc802f499a13871188d2c34a`

Direct manifest comparison after moving recipe custody to Synexia:

- Java recipe targets: **19 canonical targets**.
- Text/native/workflow targets: **11 canonical targets**.
- Current custody was refreshed from the live M3JDK21 branch; unchanged targets remain byte-identical and evolved targets were resealed with their original preimage hashes preserved.

The original `before` hashes remain the exact stacked recipe preimages inherited from the former
M3JDK-local crate. They are not a license to overwrite arbitrary current `master`: the donor recipe
fails closed on any source that matches neither its exact preimage nor its exact postimage.

## M3JDK21 ownership

This repository owns the result after recipe application:

- `java.lang.String` / internal **M3 String** representation;
- canonical owner/atom/tuple/pool implementation;
- String facts, search/position/TQ precompute;
- regex integration;
- HotSpot/JNI/native integration;
- JDK/jtreg/runtime verification.

No Synexia code is loaded by `java.base`, HotSpot or JNI at runtime.

## Custody invariant

Do not copy the canonical recipe crates back under `m3/tooling`. To evolve this implementation:

1. change/prove the Synexia recipe first;
2. apply the reviewed postimage to the exact admissible M3JDK21 source state;
3. update this receipt;
4. rerun M3JDK21 compiler, jtreg, HotSpot, JNI and platform gates.

Target-side source invariants enforce the absence of local duplicate String/TQ recipe crates.


## Authority split after recipe move

- **Synexia owns** `com.synexia.rewrite.M3Jdk21StringHistoryConvergence`, its hash-pinned crates, donor/history evidence, fixed-point/refusal semantics, and reusable recipe evolution.
- **M3JDK21 owns** every applied Java/HotSpot/JNI/native implementation, all running M3 String precompute, JDK semantics, build/jtreg/runtime verification, and product promotion.
- This move creates **no Synexia runtime dependency** in `java.base`, HotSpot, JNI, or native String code.


## Pending canonical receiver refresh

Follow-up Synexia PR: https://github.com/hsoliwal/com.synexia/pull/9672  
Branch: `m3/m3jdk21-string-recipe-receiver-refresh-20261007`  
Pinned refresh revision: `0584a6713e70860dca42410088085d980f1044c2`

This follow-up promotes the receiver's latest workflow and fail-closed invariant gate back into the
already-canonical Synexia recipe established by merged PR #9597.

Readback at M3JDK21 receiver head
`7dc72fdf9733951bcc802f499a13871188d2c34a`:

- Java/runtime/test targets: **19/19 exact**;
- text/native/HotSpot/governance targets: **11/11 exact**;
- total: **30/30 exact postimage SHA-256**.

### Machine-readable receipt

```text
recipe_name	com.synexia.rewrite.M3Jdk21StringHistoryConvergence
synexia_repository	hsoliwal/com.synexia
synexia_commit	0584a6713e70860dca42410088085d980f1044c2
synexia_java_manifest	synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/m3jdk/jdk21-hash-pinned/synexia-m3-string-history-convergence/manifest.tsv
synexia_text_manifest	synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/m3jdk/jdk21-hash-pinned-text/m3-string-history-convergence/manifest.tsv
synexia_java_manifest_sha256	b5ae63e4af47ed5c9ff10645d479d5081e8370990602094ab26da718f100dc6a
synexia_text_manifest_sha256	795ff4342c9e5ed61cdc38123b9df6de0c335d4fb14bfbc9e6c3078d7cae6361
m3jdk21_applied_result_commit	7dc72fdf9733951bcc802f499a13871188d2c34a
runtime_owner	M3JDK21
recipe_owner	Synexia
runtime_dependency_on_synexia	false
target_recipe_disposition	THIN_RECEIVER_ONLY
verification_status	SYNEXIA_CANONICAL_PACKET_30_OF_30_TARGETS_HASH_MATCHED
```


### Superseded local descriptor cleanup

The old target-local descriptors

- `m3-jni-newstring-admission.yml`
- `m3-string-char-boundary.yml`

were removed because their behavior is already subsumed by the canonical Synexia
`M3Jdk21StringHistoryConvergence` packet. M3JDK21 source invariants now forbid those duplicate
recipe-owner entry points from returning.


### Sealed target-state boundary

The hash-pinned recipe target state is
`7dc72fdf9733951bcc802f499a13871188d2c34a`.

Receipt-only commits after that state are deliberately outside the recipe target inventory, so
updating this document does not recursively change the sealed applied-result identity.


## Layered literal split recovery

The Synexia-owned layered recipe

`com.synexia.rewrite.m3jdk.M3StringLiteralSplitRecovery`

was refreshed on Synexia PR #9686 and applied on top of the exact 30/30 base String-convergence
receiver. This is recipe application provenance only; M3JDK21 still owns the resulting runtime.

Applied target branch:

`m3/synexia-literal-split-receiver-20261007`

Pre-receipt applied target head:

`a27a2936030f940bc1e20b37a22d3593f8d637d0`

Exact readback against the refreshed Synexia packet:

- Java/runtime/test targets: **3/3 exact**;
- workflow/invariant/port-map targets: **3/3 exact**;
- total: **6/6 exact postimage blobs**.

The recovered runtime lane is deliberately narrow: M3-backed source, non-empty multi-unit
conservative literal regex, canonical M3 indexOf and substring ranges. One-character split keeps
the existing JDK fast path; empty, escaped, metacharacter and general regex semantics remain
Pattern-owned.

### Machine-readable layered receipt

```text
recipe_name	com.synexia.rewrite.m3jdk.M3StringLiteralSplitRecovery
synexia_repository	hsoliwal/com.synexia
synexia_commit	725b5c688a9000ab908f550237b7642d23b7302f
synexia_pull_request	9686
synexia_java_manifest	synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/m3jdk/jdk21-hash-pinned/synexia-m3-string-literal-split-recovery/manifest.tsv
synexia_text_manifest	synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/m3jdk/jdk21-hash-pinned-text/m3-string-literal-split-recovery/manifest.tsv
synexia_java_manifest_sha256	f2ed439a4b10ea79a239aec8a0381507cc97f5a809b9aeb12cbc3e906105ce45
synexia_text_manifest_sha256	d3263430efa32f0944f7ed733422cd9b39bacefb44a78912c5514a3f7a8b1028
m3jdk21_applied_result_commit	a27a2936030f940bc1e20b37a22d3593f8d637d0
runtime_owner	M3JDK21
recipe_owner	Synexia
runtime_dependency_on_synexia	false
target_recipe_disposition	THIN_RECEIVER_ONLY
verification_status	SYNEXIA_LITERAL_SPLIT_PACKET_6_OF_6_TARGETS_BLOB_MATCHED
```


## Current canonical M3 String recipe packet — PR #9693

This section supersedes the earlier String-history custody/receiver-refresh receipts above. Those
sections remain as append-only provenance.

Canonical Synexia owner:

- repository: `hsoliwal/com.synexia`
- PR: https://github.com/hsoliwal/com.synexia/pull/9693
- branch: `m3/m3jdk21-string-recipe-canonical-20261007`
- pinned recipe revision: `3a5465731b20ab42c5ad70f364604fa2e8176ef8`
- named recipe: `com.synexia.rewrite.M3Jdk21StringHistoryConvergence`

Applied M3JDK21 recipe-target state:

`a21cf6c4a833d659fd985319479197c10f9da443`

The canonical packet now seals:

- **20/20 Java/runtime/test targets**;
- **11/11 text/native/HotSpot/workflow targets**;
- **31/31 total postimage targets**, byte-identical by cross-repository Git blob readback.

The Java packet now explicitly includes the separately bounded
`M3StringCodePointPrecompute.java` owner as an `ABSENT`-before target, alongside the existing
String facts, search/position precompute, regex TQ, builder interop, intern and native encoding
proofs.

Manifest identities:

```text
synexia_java_manifest	synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-string-history-convergence/manifest.tsv
synexia_java_manifest_sha256	b9cd598246c79ea684a9a60f680eea88034b2623ca8964f0eca05487b39cdcd5
synexia_text_manifest	synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-text/m3jdk21-string-history-convergence/manifest.tsv
synexia_text_manifest_sha256	67be97d656394be001c70875c21d4348fc5361c89f7d14cc24999888df807720
```

Application mode is `FIXED_POINT_NO_SOURCE_DELTA`: by the time custody was refreshed, the
M3JDK21 product tree already matched every reviewed Synexia postimage. The application therefore
proved exact fixed point rather than rewriting the runtime a second time.

M3JDK21 retains only:

- the applied Java/HotSpot/JNI/native product implementation;
- target-native source/runtime/jtreg verification;
- `m3/runtime-integration/m3-string-synexia-recipe-application.tsv` as the machine receipt.

The former target-local String-history recipe descriptor, sealed Java/text crates and local
fixed-point recipe test are absent. Reusable recipe evolution remains in Synexia. Runtime M3 String
precompute remains entirely inside M3JDK21; this creates no Synexia runtime dependency.


## Layered HotSpot M3-backed guard — PR #9693

A follow-up compile-level HotSpot repair is owned by the Synexia layered recipe:

`com.synexia.rewrite.M3Jdk21StringHotspotM3BackedGuard`

Canonical Synexia source:

- PR: https://github.com/hsoliwal/com.synexia/pull/9693
- branch: `m3/m3jdk21-string-recipe-canonical-20261007`
- pinned revision: `e46a1580469f82ed6cf29879172fb339928a88b6`
- manifest:
  `synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-text/m3jdk21-string-hotspot-m3-backed-guard/manifest.tsv`
- manifest SHA-256:
  `53fee966e6d6140589d65a5adcb8346e1b40117a79523725a3ccdbca5682af41`

The exact target preimage was M3JDK21 commit
`5f50f4ee84c528717cb2910f37db8b10445edb02`.

Applied source result:

`d073c6fd9be8a5f067f8557abbbe2bc87604790c`

The recipe repairs the tuple-era HotSpot predicate name `is_m3_joined` to the canonical
`is_m3_backed` predicate in:

- CDS archive String sizing;
- StringDedup M3 guard;
- the fail-closed M3 String source invariant.

Direct cross-repository readback is **3/3 exact target blobs**. A repository-wide search after
application finds no live `is_m3_joined` references.

M3JDK21 retains only the applied HotSpot/runtime result plus the layered machine receipt:

`m3/runtime-integration/m3-string-hotspot-m3-backed-guard-application.tsv`

No Synexia code or recipe is loaded by HotSpot at runtime.


## Current invariant refresh — Synexia PR #10001

The canonical String-history recipe remains owned by Synexia. The latest receiver-ahead
governance delta was promoted back to that owner rather than evolving a target-local recipe.

Canonical handoff:

- repository: `hsoliwal/com.synexia`
- PR: https://github.com/hsoliwal/com.synexia/pull/10001
- branch: `m3/m3jdk21-string-history-invariant-sync-20261009`
- pinned revision: `f3a193d46324a3e528c2139295f57915af9eddec`
- recipe: `com.synexia.rewrite.M3Jdk21StringHistoryConvergence`
- Java manifest SHA-256: `373bd85ab43bdfabcb5e1771f98a6b6f87e3dfad8ffc2c08f58834812a713eac`
- text/native/workflow manifest SHA-256: `29ac170672b95fd7039c98a7193f2aec2a29b602f9e69d4248fed0ac9940a850`

Applied M3JDK21 runtime state before receipt-only commits:

`692a4c648ac952bdfc6b75c193c23e466a8696f3`

Application mode remains `FIXED_POINT_NO_SOURCE_DELTA`: the inspected M3 String runtime,
search/position precompute, pool, `AbstractStringBuilder`, `String.java`, differential tests
and workflow were already byte-identical to the Synexia canonical postimages. The receiver-ahead deltas were the fail-closed mandatory-literal regex seam invariant, its\n`Pattern.java` GroupHead/GroupTail implementation and regression test, plus the current donor-lineage\nand precompute port-map receipts. PR #10001 moves all of those back to Synexia and reseals both\ncanonical manifests.

The target-local `M3StringHistoryConvergence` descriptor, hash-pinned Java/text crates and local
fixed-point recipe test are absent from M3JDK21. M3JDK21 retains only the applied product,
target-native verification and this receipt. Runtime ownership remains M3JDK21; recipe ownership
remains Synexia; runtime dependency on Synexia remains false.


## Consolidated focused String recipe custody — Synexia PR #10004

The focused String recipes that had been stranded on side branches / older recipe layouts are now
consolidated on the current Synexia A19 recipe line.

Canonical Synexia handoff:

- repository: `hsoliwal/com.synexia`
- PR: https://github.com/hsoliwal/com.synexia/pull/10004
- branch: `codex/m3-string-recipe-custody-consolidation-20261009`
- base: `codex/m3-tq-facts-radix-20261009` (A19)
- runtime owner remains: **M3JDK21**
- runtime dependency on Synexia: **false**

Restored canonical recipe families:

- `m3-translate-escapes`
- `m3-valueof-char`
- `m3-deprecated-getbytes-bulk`
- `m3jdk21-string-adaptive-search`
- `m3jdk21-string-position-masks`

The consolidation also reconciles the current String-history convergence postimages and registry.
M3JDK21 retains the applied runtime implementation and target-native tests only. Reusable recipe
evolution remains in `com.synexia/synexia-openrewrite-recipes`.

The locale language value-equality repair remains separately fail-closed under Synexia PR #9981
because it is pinned to a different target preimage; it must not be silently folded across that
source seal.


## String I/O boundary recipe custody — Synexia PR #10006

Two historical runtime transitions that were already absorbed by later M3 String product revisions
now have explicit reusable Synexia recipe owners.

Canonical Synexia handoff:

- repository: `hsoliwal/com.synexia`
- PR: https://github.com/hsoliwal/com.synexia/pull/10006
- branch: `codex/m3-string-io-boundaries-20261009`
- stacked on: Synexia PR #10004
- runtime owner: **M3JDK21**
- runtime dependency on Synexia: **false**

Recipes:

- `com.synexia.m3.GenericCharsetDirect`
  - target historical commit: `e02cf835c37e51181e23175e5969cc26fd7c6aba`
  - removes generic-charset `char[]` staging and feeds `CharsetEncoder` through
    `CharBuffer.wrap(M3String)`.
- `com.synexia.m3.JniShadowBulk`
  - target historical commit: `3a050f6fd0307588605080eb5535e487ce8109f9`
  - replaces per-code-unit JNI/native staging with final-array allocation plus one bulk
    `M3String.getBytes` / `getChars` dispatch.

These recipes retain their exact historical before/after SHA-256 source seals. The current M3JDK21
runtime has evolved beyond those isolated postimages, so application status is
`HISTORICAL_SERIAL_STEP_ALREADY_ABSORBED`; do not apply either postimage over a newer receiver
that does not match its exact preimage.
