# M3JDK21 merged #1908 → master source-reconciliation inventory

SPDX-License-Identifier: Apache-2.0

## Immutable comparison basis

- Previously reviewed and now **merged** [M3JDK21 #1908](https://github.com/hsoliwal/M3jdk21/pull/1908), last head `be9430ce6b644f8b8b6648f94911e361a0d454c7`, merged on 2026-10-28; previous narrower [#1923](https://github.com/hsoliwal/M3jdk21/pull/1923) and source-lineage [#1925](https://github.com/hsoliwal/M3jdk21/pull/1925) also merged.
- Comparison against **master at 649a735e23a79a4df46e6c61bfcd2fadf156d90d** (2026-10-29 snapshot). These dates, commit pins and actual observed blobs are frozen; the file deliberately does not call any part of current master lost or obsolete without behavioral comparison.
- Every path in `SOURCE_DIFF.tsv` was read through GitHub at **both** pinned refs, including explicit 404/ABSENT for three paths. Six runtime Java/JNI implementations differ by byte identity. The merged compact-regex test, indexed-view lifecycle test, and combined original-JDK workflow are absent at this master snapshot; the history verifier also differs. The snapshots are not automatically equivalent or non-equivalent at the API/behavioral level.

## Recovered change-history context

The master `Matcher` and `M3TQ` path histories identify current-master first-party regex receipt `c8fb46ad6995189c0d46c55ac8483a6615cfb119` (2026-10-26) after original `M3TQ` primitive-staging commit `876b018258d9da07ffcfc012f0eae1e1a03305bd`. The merged PR includes a **later optimization candidate** that removes redundant flat ScanPlan storage and adds `M3RegexCompactPlanTest`. The parent’s [Synexia #13301](https://github.com/hsoliwal/com.synexia/pull/13301) review evidence reports an original source-built image and 54 selected passing jtreg results for that exact recipe-executed parent; **resource-performance HOLD and incomplete full historical provenance remain explicit**. This saved parent evidence is not fresh master-level acceptance.

The `M3IndexedView` master history identifies a later comparator-metadata fix `16ef8670cc2740f3e3ccd0f26364b2fef8e2c880` (2026-10-28). The merged PR also contains lazy indexed selection-directory allocation and `M3IndexedViewLifecycleTest`. The master comparator fix might be important: **do not install the merged entire file over current master**, which could erase the comparator behavior. Inspect and isolate behavioral atoms using a canonical first-party Synexia recipe.

The historical source verifier on master has an additional symlink-intermediate custody fix `395972bdf90b2d84777a7131bc7f60b47ffc258b` (2026-10-27). The older merged verifier must **not** replace that newer safety behavior. Master has no `m3-regex-collection-superset.yml` at this snapshot, but absence of this workflow alone does not imply a missing regression gate: inventory the latest functional CI tests before adding another workflow.

## Required review gates before restoring any runtime atom

1. Inventory all relevant later master owners, test registrations, existing CI workflows, ABI and historical merge ancestry; compare per-path Git blobs and source slices, not just file names or merge flags.
2. In **private** `hsoliwal/com.synexia`, build a canonical tested Maven/OpenRewrite **file-local contract-preserving** candidate per changed runtime owner. Keep source preimage SHA-256, candidate postimage, exact rejected alternatives, licensing and algorithm category. No challenge-platform donor code copied without its provenance/rights.
3. Select an atom only if it adds missing behavior to **current master** and preserves later comparator/JNI/source-security fixes. Do not overwrite entire old files, backdate a historical receipt, force-push/rebase `develop`, or introduce a runtime layer separate from original M3JDK21.
4. Execute correct original-JDK Java21 source build, JNI native ABI/export/name checks, focused -Xint/-Xmixed tests plus previously absent test oracles, relevant provider tests, JCK/jtreg scope, and monitored CPU/heap/native resource gates. Compare failure cases, null/identity/iterator/callback semantics and source-equivalence claims against exact parent snapshots. Record failures as HOLD, not PASS.
5. Only after both canonical recipe **execution** and product-head qualification succeed, receive the exact first-party postimage into an ordered reviewable PR. Keep full release-level/platform/GC/CDS/provider gates separate.

## Status at this audit

**Ten file identities catalogued; six differing product Java/JNI implementations, one differing verifier, and three absent test/workflow paths. No Java/JNI code is automatically restored. No current-master runtime equivalence test or full source-built JDK CI has been executed by this inventory PR.** This is the no-loss *recovery map* that makes subsequent source-sealed incremental work auditable, not a release approval.
