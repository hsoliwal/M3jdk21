<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# CPU tools: M3JDK21 receiving plan (2026-10-31)

**Status: DOCUMENTATION-ONLY; NOT PRODUCT IMPLEMENTATION OR RECEIVING QUALIFICATION.**

## Locked owners

Existing M3JDK21 modules `jdk.m3.text`, `jdk.m3.structured`, and `jdk.m3.io` own the installed text tools. Preserve `java.lang.String` and Java 21 API/ABI/observable behavior. Source admission must follow the existing M3 String payload/index/offset/mask ownership and receiver policy in repository `AGENTS.md`. No parallel M3 String store or extra Synexia runtime dependency. No recipe source, templates, or execution engines belong in this public product.

## Catalogue frontier

The requested CPU catalogue covers text processing; search/index/filtering; tokenization and lexical statistics; compression/encodings; parsing/schema/type checking; graph/DAG/interval/spatial; solvers/constraints/optimization; rules/workflows/state machines; statistics/information theory; diff/patch/provenance; scheduling/resource control; and verification/recovery. These are **catalogue categories**, not declarations of implemented public APIs.

The earlier Synexia [draft PR #14633](https://github.com/hsoliwal/com.synexia/pull/14633) covers a bounded text family. Independently inventory incumbent M3JDK21 methods and all retained history before considering additional classes or JNI kernels. Exact compatibility/rights/donor fit must be recorded per category.

## Candidate experiment, outside M3JDK21

A separate ChatGPT conversation artifact contains three **candidate-only** Java classes: `M3GraphTools` (packed CSR lanes, deterministic topological order and reachability), `M3IntervalTools` (half-open interval union and intersection), and `M3TextCounts` (`CharSequence` Unicode code points and literal lookup, with no input flattening). They compiled on stock OpenJDK **21.0.12.1** using `--release 21 -Xlint:all -Werror`; the standalone self-test passed **13 assertions**. This is not an executed Synexia recipe, not proof of actual M3JDK String ownership, not a JMOD or JNI result, and not product code. Do not copy these candidate bodies directly into this target.

## Mandatory admission sequence, per source file/atom

1. Inventory exact source revision, existing owners, signatures, protected/public contracts, call sites, historical proof and rights; catalogue problem categories and donor evidence.
2. Freeze input hashes, Unicode/UTF-16 exceptions, offsets/coordinates, ownership, resource bounds, concurrency, and API/ABI behavior.
3. Author and **execute** the named source-sealed Maven/OpenRewrite recipe in `hsoliwal/com.synexia` with positive/refusal/fixed-point tests. Correct the canonical recipe for every diagnosed failure.
4. Receive **only executed postimages**, target-native tests and compact data-only source/recipe/evidence receipts here.
5. Run in order: patch/readback/hash -> lint -> compile -> tests -> JMOD/jlink -> JNI/jtreg/runtime -> performance/heap/native-memory measurements, as applicable.
6. Require deterministic serial per-file promotion, explicit no-op rerun, and target-specific API/ABI/cross-platform acceptance before merging. Preserve the M3JDK String -> arrays -> collections receiving order.

## Gate ledger

| Gate | Status |
|---|---|
| Catalogue request documented | PASS — scope only |
| Existing M3JDK text module owner inspected | PASS — limited source inventory |
| Candidate Java 21 compilation and self-test | PASS — standalone, 13 assertions |
| Full catalogue implementation | NOT DONE |
| Synexia source-sealed recipe execution | NOT RUN |
| Actual M3 String integration and no-copy proof | NOT RUN |
| M3JDK21 JMOD/jlink / JNI / jtreg / full build | NOT RUN |
| Performance and resource qualification | NOT RUN |

This document must not be interpreted as authorization to move, delete, rename or overwrite existing implementation files. All product changes require a separately reviewed executable recipe and verified handoff.
