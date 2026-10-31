<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# TextTools.tr refinement handoff — 2026-10-19

This handoff binds **one qualified receiving leaf**, `m3/tooling/module-packs/text-tools/src/jdk.m3.text/jdk/m3/text/TextTools.java`, to the selected canonical `tr` implementation and its frozen proof. [RECEIPT.json](RECEIPT.json) records the full preimage/postimage SHA-256, mode, dataset and SDK bindings. These two handoff documents are additional to that Java leaf.

The canonical publication is [`dade40a9f6e3ab054bf67628b4a22bd8ffe21a6a`](https://github.com/hsoliwal/com.synexia/commit/dade40a9f6e3ab054bf67628b4a22bd8ffe21a6a). Its [review catalogue](https://github.com/hsoliwal/com.synexia/blob/dade40a9f6e3ab054bf67628b4a22bd8ffe21a6a/synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017/catalogue/tools4j-unix-20261019/README.md), [campaign](https://github.com/hsoliwal/com.synexia/blob/dade40a9f6e3ab054bf67628b4a22bd8ffe21a6a/synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017/catalogue/tools4j-unix-20261019/CAMPAIGN.json), [pass index](https://github.com/hsoliwal/com.synexia/blob/dade40a9f6e3ab054bf67628b4a22bd8ffe21a6a/synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017/catalogue/tools4j-unix-20261019/PASS_INDEX.json) and [replay instructions](https://github.com/hsoliwal/com.synexia/blob/dade40a9f6e3ab054bf67628b4a22bd8ffe21a6a/synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017/catalogue/tools4j-unix-20261019/REPLAY.md) contain the qualified recipes, immutable input/output bindings, source proofs and recorded limits. The receiving baseline is [`677437be295b23f7ccf7409be287008944d0e8b0`](https://github.com/hsoliwal/M3JDK21/commit/677437be295b23f7ccf7409be287008944d0e8b0).

Each selected leaf passed five existing SDK JUnit cases, the unchanged eight-case preparation contract and nine-case receiver contract. These counts describe each single-file transformation and receiving checks. The offline Maven contract profile could not resolve `exec-maven-plugin:3.5.0`; its unchanged Python contracts were executed directly. The full 20-dataset campaign subsequently reached a fixed point with zero receiving changes. The existing licensing checker passed the exact 379-path canonical publication frontier with zero violations. [RECEIPT.json](RECEIPT.json) pins the source-only proof and current provenance-result hashes.

## Exact source and recipe

The receiving dataset is [`tools4j-tr-sdk-20261019`](https://github.com/hsoliwal/com.synexia/tree/dade40a9f6e3ab054bf67628b4a22bd8ffe21a6a/synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017/datasets/tools4j-tr-sdk-20261019). It changes source SHA-256 `854030b58cbb310f2f231d80088ddf92c16f733d26e7cfa2eaeb744c31e1e7c6` to `736a57774078e3bea6b4987ba92b205b6d9aef76d253e90df5669c3c3300e892` in mode `0644`. The target is byte-identical to the [selected canonical source](https://github.com/hsoliwal/com.synexia/blob/dade40a9f6e3ab054bf67628b4a22bd8ffe21a6a/synexia-unix4j-m3/src/main/java/com/synexia/unix4jm3/TextTools.java) after replacing its package declaration with `jdk.m3.text`.

Replay uses the captured `qualified-text-root` profile. The prerequisite `tools4j-layout-scope-20261019` and `tools4j-layout-java-20261019` datasets add the exact existing text source root to the existing recipe owners. The 91 review files bound by the text dataset's `SDK_INPUTS` remain expanded in canonical Git alongside its seven core dataset files. The input seal has 109 records. [Engine profiles](https://github.com/hsoliwal/com.synexia/blob/dade40a9f6e3ab054bf67628b4a22bd8ffe21a6a/synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017/catalogue/tools4j-unix-20261019/ENGINE_PROFILES.json) and the replay instructions bind these versions and the receiving order.

The existing `tr` method uses private primitive Latin-1 and packed high-scalar lookup. Presence bits keep replacement NUL distinct from an absent mapping and from deletion. The selected v4 preserves last-duplicate-wins behavior, short-target repetition, empty-target deletion, supplementary scalars, line endings, limits and validation order. Its public surface remains unchanged. Original donation and module-pack provenance records retain their historical meaning; this handoff supplies the current refinement binding.

## Correctness and installed-image scope

The actual target namespace passes **5,071 exact differential cases: 4,179 successes and 892 matching refusals**, plus **224 collision cases**. Both sides compile the complete 23-file `jdk.m3.text` Java module, with exact public API parity. The receiving tape SHA-256 is `267bd81c27650267b35280e33bce523988782045fbdbd107d9ff7868621a16b0`.

The same cases pass on the linked M3 VM through a `URLClassLoader` over frozen baseline and actual target class directories with `--limit-modules java.base`. Those differential cases are separate from installed-JMOD dispatch. The unchanged `M3TextSmoke` separately resolves the installed modules and passes **24 checks, including one `tr` case**. The actual unchanged text pack driver builds three modules, native code, JMODs and the linked image; its 43 compiled source inputs remain sealed.

The cross-namespace tape audit finds exactly two helpful-NPE owner-name differences (`null-limits` and `null-frame`). Within each namespace, refusal class, message and phase remain exact. The historical outer wrapper failure that expected the other namespace's aggregate digest is retained, with the independent audit recording the precise cause.

## Measured resource tradeoff

The [selected v4 measurements](https://github.com/hsoliwal/com.synexia/blob/dade40a9f6e3ab054bf67628b4a22bd8ffe21a6a/synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017/datasets/tools4j-tr-synexia-20261019/review/RESOURCE_V4_RESULTS.json) use complete method calls through the same reflection probe on local HotSpot 21.0.12, with current-thread CPU and allocated-byte counters over five alternating rounds after warmup. Ratios below are candidate divided by baseline.

| Frozen workload | Allocated bytes, candidate / baseline | CPU, candidate / baseline |
|---|---:|---:|
| `tiny-ascii` | 0.754 | 2.180 |
| `dense-ascii` | 0.985 | 0.848 |
| `dense-latin1` | 0.093 | 0.646 |
| `supplementary` | 0.177 | 0.837 |
| `mostly-unmapped` | 0.526 | 0.851 |
| `high-alphabet4096` | 0.164 | 1.106 |

Allocation falls in these six measured workloads. CPU increases to **2.180 times baseline for tiny ASCII** and **1.106 times baseline for the 4,096-symbol alphabet**. These measurements describe the finite canonical HotSpot probe; they are separate from installed-module and linked-VM correctness evidence and do not establish retained heap, RSS, cross-platform performance or a universal speedup. The v5 experiment was not selected.

No fresh whole current HotSpot/JDK build, jtreg or full Synexia reactor was performed. The existing sealed M3 JDK image supplies the module build and linked-VM gates. String remains the active global receiving phase.

## Canonical source proof

| Source-only evidence | Bytes | SHA-256 |
|---|---:|---|
| [execution-evidence-v1.tar.gz](https://github.com/hsoliwal/com.synexia/blob/dade40a9f6e3ab054bf67628b4a22bd8ffe21a6a/synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017/catalogue-proof/tools4j-unix-20261019/execution-evidence-v1.tar.gz) | 3,151,462 | `4559cd0211af4e812b168e8cb6f84000f847ada67dca4c4d125f7d78af307f82` |
| [workflow-evidence-v1.tar.gz](https://github.com/hsoliwal/com.synexia/blob/dade40a9f6e3ab054bf67628b4a22bd8ffe21a6a/synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017/catalogue-proof/tools4j-unix-20261019/workflow-evidence-v1.tar.gz) | 1,373,833 | `f437d534a12b116d54b3197cfe29e9da3d88d1b192baecd1fe377611f3a9f3ca` |

The matching manifests and custody receipts are linked in `RECEIPT.json`. The execution archive has 1,757 source members; the workflow archive has 464. Each archive also contains its own manifest. They retain source, probes, readable logs and receipts; compiled binaries, JARs, JMODs and linked images are excluded. Original Apache-2.0 and MIT notices remain attached to the corresponding source.
