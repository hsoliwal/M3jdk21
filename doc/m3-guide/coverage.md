<!-- SPDX-License-Identifier: Apache-2.0 -->

# Coverage, evidence and continuation ledger

[Guide](README.md) · [Package accounting](packages/README.md) · [Descriptors](descriptors.md)

## Pinned scope and what the counts mean

Snapshot: `278b221e8365ebeb6095a3a5643b38a0564258b5`, documentation PR base source `d5abed865bd47c4dd5ad78e95704348fe2095ed2`. Root and source subtree identities were reconciled. Large recursive Git-tree responses were split into smaller subtrees until all accepted results had `truncated=false`. The merged inventory has 127,512 tracked entries: 104,606 blobs and 22,906 directories. There are no unresolved tree reads or submodule descendants in this repository inventory.

| Level / evidence unit | Discovered/accounted | Substantive state | Remaining work |
| --- | ---: | --- | --- |
| Repository | 1 | Additive draft guide | Maintainer review; exact-head CI; complete child semantic coverage |
| Workstream reading map | 23 | Partial responsibility/build/boundary summaries | Not an exact project denominator; reconcile script/native-only and nested build units |
| Direct JDK named-module descriptors | 70 / 70 read | Descriptor-level purpose, exports, requires and services documented | Full API/state/lifecycle, inbound consumers and package/test mapping |
| All tracked Maven descriptors | 266 / 266 indexed | Some active manifests read; others path-only | Effective reactor/profile/inheritance and retained-copy classification |
| All tracked JPMS descriptors | 565 / 565 indexed | 70 direct JDK descriptors plus sampled independent descriptors read | Distinguish active, generated, test and custody identities |
| Java files | 74,035 | File-presence accounting | No full-source or declared-namespace audit |
| Directories containing non-descriptor Java source | 13,793 / 13,793 indexed | Every row has purpose/pointer, entry candidate, dependencies, tests, evidence and gap fields | Most rows remain PATH_ONLY or DOC_LINKED, not documented packages |
| Module-info-only directories | 559 / 559 accounted by descriptors | Descriptor paths present | Resolve active membership and semantics |
| M3 independent/tooling/reference source roots | 44 sampled | Component-specific partial contract guide, imports, tests and inline gaps | Complete all type/method/consumer/error/effect contracts |
| M3 runtime package boundaries | 5 sampled | Partial current-source owner guide | Remaining inherited owners and exact target qualification |

Reviewed-complete substantive package coverage is **not established**. Do not report 13,793 packages documented, 49 packages complete, 100% semantic coverage or a passing M3/runtime run. The 44 and 5 sample groups are distinct physical source roots; identical namespace spellings are not merged across independent classpaths. The declared namespace denominator is unresolved, so a truthful package-completion percentage cannot yet be calculated.

## Evidence vocabulary

- PATH_ONLY: a tracked path/type exists at the pinned source revision
- DOC_LINKED: a native package-info.java/package.html is located; its full content and freshness are not automatically reviewed
- MANIFEST_READ: direct manifest declarations were read; effective build execution and behavior remain unverified
- OBSERVED_SAMPLE: a source declaration/body sample supports a bounded explanation; other package members remain open
- NOT_RUN: an executable check did not run in this documentation pass
- UNKNOWN: unresolved identity, contract or coverage, retained in the denominator rather than silently excluded

Existing package/API documentation is authoritative for the source revision where it lives. A linked test is evidence of a test file, not executed-test evidence. Existing receipt text retains its exact original head, environment, command and scope; this guide does not refresh old results by quoting them.

## Documentation task packet

| Field | Editorial scope |
| --- | --- |
| Mode | Additive documentation candidate on the existing draft PR |
| Targets | doc/m3-guide/*.md and doc/m3-guide/packages/*.md |
| Public API / behavior / build scripts | Locked; no source, descriptor, workflow or runtime flag changes |
| Protected inputs | m3/README.md, m3/docs/native-build.md, generated custody documents and source-sealed recipe payloads unchanged |
| Allowed delta | Repository/project/module/package navigation, observed source contracts, coverage and gap ledger |
| Forbidden | Merge/rebase, generated custody rewrite, source/API edits, guessed passing tests, donor-phase promotion, private material publication |
| Verification order | Review documentation delta; structural/link/scope checks; compiler/test/runtime stages remain NOT_RUN |
| Stop condition | Draft documentation published with exact counts and unresolved scope/semantic gates explicit; no claim of whole-task or whole-runtime completion |

This bounded editorial pass is not a completed full M3 execution run. Full compiler/runtime proof artifacts, dedup clusters, semantic fixed points and topology/compute receipts were not produced. Hashes identify inspected bytes; they do not establish behavioral equivalence.

## Prioritized continuation

| Priority | Work | Current disposition | Evidence needed to close |
| --- | --- | --- | --- |
| P0 | Confirm every declared namespace and source-set owner | OPEN | Declaration extraction over the complete pinned tree; distinguish unnamed tests, platform overlays, vendor and generated copies |
| P0 | Finish M3 String/regex owner contracts | PARTIAL | Complete method/API, failure, Unicode, owner/GC/JNI and consumer/test mapping; existing canonical recipe process for any inline edits |
| P0 | Reconcile every active M3 package | PARTIAL | Read all public types and key internal owners, dependencies, state/effects, tests and build boundaries |
| P1 | Reconcile inherited package documentation | LINKED / UNREVIEWED | Review native package-info/package.html and key API contracts; record source-version drift and precise gaps |
| P1 | Resolve all project/module descriptors | INDEXED | Active reactor/profile/JPMS identity; separate fixtures/custody from runnable projects without erasing them |
| P1 | Review inline documentation | OPEN | Method parameters, returns, exceptions, ownership and thread-safety; perform source changes only through owning recipes and refreshed seals |
| P1 | Expand native/script architecture | OPEN | HotSpot/C/C++/JNI/Python/shell entry points, ABI/effects and their actual module boundaries |
| P1 | Map and execute applicable tests | NOT_RUN | Exact final-head build/test environment and separate Java/native/provider/platform receipts |
| P2 | Reconcile historical guides | OPEN | Preserve historical scope while adding current-source successor links; do not rewrite old evidence |

## Second-pass semantic review

The [text/storage and precompute API sections](m3-packages.md#textstorage-api-contracts) now explain eleven complete production owner bodies (six text/storage, five precompute including internal histogram support) and three selected test files. They add source-backed units, ownership, failure, concurrency, allocation, callback and usage contracts. This deepens two package roots; it does not add eleven complete packages or change the unresolved package denominator.

The new review specifically distinguishes immutable results from mutable edit-distance workspaces, Java UTF-8 replacement semantics from UTF-16 units, read-only mapping from immutable-file publication, and DP-cell budgets from total CPU/allocation cost. Tests/examples remain NOT_RUN. Remaining API owners and exact-head execution retain their prior open status.

## Publication and source safety

Only new Markdown under doc/m3-guide is added. Uploaded design essays, private donor content and local working paths are not part of the public payload. All repository citations resolve to the public M3JDK21 tree or existing relative documentation. OpenJDK, third-party and scoped first-party notices remain in place. The catalogue is not a new ownership, licensing or receiving authority.

The PR description records the final documentation commit and checks observed after publication. Merge, release readiness and runtime acceptance remain separate decisions.
