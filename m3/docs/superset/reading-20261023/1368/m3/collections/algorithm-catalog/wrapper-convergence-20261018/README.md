<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Wrapper, index and JNI algorithm catalogue

This catalogue records the completed three-atom refinement series against the **tested integration base `f29b0afe7ad253cc0562bc8dfdd48fc1acec710e`**. It preserves the original discovery records and their statuses, then adds an explicit [accepted-atoms table](ACCEPTED_ATOMS.tsv). The first target pull request contains the strict read-only iterator repair. The stable index-order and JNI row-transfer changes belong to its **companion index/JNI pull request**.

The [qualification overview](../../qualification/wrapper-convergence-20261018/README.md) and [current acceptance record](../../qualification/wrapper-convergence-20261018/ACCEPTANCE.json) describe execution for the series. A file inventory or a historical candidate label does not qualify an implementation.

## Searchable records and revision boundaries

| Record | Exact scope |
| --- | --- |
| [Original owner catalogue](catalogue-review/OWNER_CATALOG.tsv) | **139 owners: 129 Java and 10 C**, pinned to `0d73745d7203123533dce8f8fb264ba2746f6d2d`. Five wrapper bodies were fully read for stated operation scopes; 134 owners remained inventory-only in that catalogue. |
| [Integration census](catalogue-integration-f29/OWNER_CENSUS.tsv) | **143 owners: 133 Java and 10 C**, pinned to tested f29. Relative to the original census: 131 unchanged bodies, eight changes, four additions and no removals. This is not a census of later master revisions. |
| [Integration source reads](catalogue-integration-f29/SOURCE_REVIEW.tsv) | Ten complete source reads with scoped discovery: six changed non-wrapper owners and four additions. Two changed wrappers link to their separate review; the other 131 owners remain inventory-only in this overlay. No transformation was admitted by that reading alone. |
| [Donor/candidate comparison](index-candidates/CATALOGUE.tsv) | Sixteen problem-category rows covering stable indirect order, ordered compaction, live/validated wrappers, sorted storage, word masks and bounds. Source/license pins and rejected alternatives are retained exactly. |
| [Family catalogue](catalogue-review/FAMILY_CATALOG.tsv) and [provenance links](catalogue-review/PROVENANCE_LINKS.tsv) | Existing family, ownership, mapping and recipe references. A discovered donor pointer or family classification does not establish API substitutability. |

Java owner counts include module/package descriptors. Test owners, optional interop modules, qualification mirrors and java.base are outside these censuses. Old statements such as `ADAPT_PROOF_REQUIRED` or “parent execution pending” identify their original review phase. They remain unchanged; [ACCEPTED_ATOMS.tsv](ACCEPTED_ATOMS.tsv) and the current acceptance record provide the later disposition.

## Exactly three accepted production atoms

| Atom | Result and preserved boundary | Series part |
| --- | --- | --- |
| `readonly-iterator` | `M3FilteredLists.StrictReadOnlyList` always refuses ordinary-iterator removal with `UnsupportedOperationException`. This is a disclosed correction to the owner's stronger existing read-only contract. Reads still use the original cursor and its callback effects; one guard wrapper is allocated per traversal. | Wrapper PR |
| `stable-order-groups` | Reuses first-party group reversal for the opposite direction of an already sorted projection. Equal-value occurrences remain in their original order. Current address-kernel access and the unknown-order merge path remain authoritative. | Companion index/JNI PR |
| `native-row-suffix` | Starts row-ID transfer at the already known first match and adjusts the local index. State reads, output batching, bounds, cursor packing, validation and both JNI exports retain their existing paths. | Companion index/JNI PR |

The accepted-atoms table gives full pre/post SHA-256 values and the exact source path for each atom. Qualification of these three operations does not qualify every operation in their source files.

## Donor reuse and refused substitutions

The selected stable-group donor is the public [M3 #1164 history](https://github.com/hsoliwal/M3jdk21/pull/1164), with [projection blob at `9b7620f5`](https://github.com/hsoliwal/M3jdk21/blob/9b7620f592248e858628c2489866ec73227d519c/m3/collections/src/main/java/com/m3/collections/M3LongIndexProjection.java). [HISTORY.tsv](index-candidates/HISTORY.tsv) records its equality with the historical canonical Synexia template, while [DONOR_NOTES.md](index-candidates/DONOR_NOTES.md) explains the contract comparison.

Historical two-word native compaction was refused because it loses the current cross-region output batching and predates the bounded surface. Guava, Eclipse Collections, Commons Collections, Spring and Boost remain pinned comparison references; no external implementation body was copied for these changes. LeetCode, HackerRank and GeeksforGeeks supplied problem categories, with their problem/editorial material treated as non-code references. Applicable notices are retained in the [series attribution](../../qualification/wrapper-convergence-20261018/THIRD_PARTY_NOTICES.md).

## Unresolved wrapper findings

[RECONCILIATION.md](catalogue-review/RECONCILIATION.md) and [its execution table](catalogue-review/RECONCILIATION_EXECUTION.tsv) separate original and f29 observations. The f29 numeric bounded-map and bounded-set witnesses pass, while the broad recovery still loses comparator failure identity in the reproduced map witness. The mutable counterpart has a separately recorded source argument; the compound runtime witness aborts before its mutable assertion.

Interior-null navigation, ordinary-iterator predicate work and backing fail-fast witnesses remain unresolved. The admitted removal guard does not repair those paths. The versioned `readonly-policy` gate isolates the accepted guard/read-effect scope while retaining the original compound and standalone failing witnesses.

## Exact copy custody

[CANONICAL_COPY_MANIFEST.tsv](CANONICAL_COPY_MANIFEST.tsv) maps **31 unchanged canonical files** to their public paths with byte counts and SHA-256 values. [CANONICAL_COPIES.sha256](CANONICAL_COPIES.sha256) checks those copies from this directory. [RECIPE_CUSTODY_COPY_MANIFEST.tsv](RECIPE_CUSTODY_COPY_MANIFEST.tsv) separately records the **61 public recipe-custody source copies**. The canonical publication commit is recorded in [HANDOFF.json](../../qualification/wrapper-convergence-20261018/HANDOFF.json).

These exact archives retain their original path references, comments and historical statuses. Workspace/canonical paths in frozen records identify original custody; the public copy manifests, qualification overview and [evidence directory](../../qualification/wrapper-convergence-20261018/evidence/) provide the public handoff. Selected index metadata is copied here; executable templates reside in the qualified recipe custody rather than being duplicated into this catalogue.

Example searches from this directory:

```sh
rg 'MASK_SELECTION|JNI_|INDIRECT_STABLE_SORT' catalogue-review/OWNER_CATALOG.tsv
rg 'INVENTORIED_ONLY_PENDING_REVIEW' catalogue-integration-f29/OWNER_CENSUS.tsv
rg 'REJECT_|EVIDENCE_ONLY' index-candidates/CATALOGUE.tsv
rg 'QUALIFIED_FOR_THIS_SERIES' ACCEPTED_ATOMS.tsv
```

For each subsequent pass, select one owner and operation, consult its retained comparison, establish the contract/effect boundary, and qualify a replaceable recipe through the serial source-patch, lint, compile, test and runtime gates. Preserve unsuccessful alternatives and historical receipts so later passes use the accumulated review.

=======
<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# Wrapper and path-mask convergence: actual receiving acceptance

The corrected Java/JNI work is **implemented and qualified for the sealed collections scope**.
The actual receiver at M3JDK21
`2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49` passed **255 Maven/JUnit tests**, the
named Java and checked-JNI contracts, existing public/protected API retention,
generated JNI header/export comparison, C17/UBSan builds, and final source readback.
The [final receiving status](FINAL_RECEIVING_STATUS.json) binds those executed
receipts to the [337-row source closure](runtime-proof/received/SOURCE_MANIFEST.json).

The receiving sequence has three independently qualified datasets. Their
sealing epochs remain distinct; newer target preimages were checked before reuse.

| Dataset | Original sealing revision | Actual first / repeated changes | Scope |
| --- | --- | --- | --- |
| `m3jdk21-wrappers-r2-20261018` | `07ba6e383343d32c83cfdaf641387bbe72bf325d` | 28 / 0 | Six existing runtime files and 22 test files |
| `m3jdk21-long-bridge-20261018` | `2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49` | 1 / 0 | Existing long-mask facade reuses the qualified Java kernel |
| `m3jdk21-wrapper-proof2-20261018` | `2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49` | 3 / 0 | Source manifest, unchanged verification runner and README |

Each lane passed **5 actual OpenRewrite SDK tests, 8 sealing cases and 9 isolated
receiver cases**. The [SDK acceptance index](runtime-proof/SDK_ACCEPTANCE.json)
retains each lane's generated-input/result manifests, successful XML report,
Maven log, refusal/fixed-point receipt, and actual receiver receipt. The initial
`m3jdk21-wrappers-20261018` dataset is rejected and excluded from promotable inputs.

## Acceptance boundaries

The unchanged **historical donation verifier has not been executed** in this
scoped run, including its exact JUnit 1.12.2 lane. Static use of its existing
admission inputs refuses both source epochs: the exact baseline has 14 source
drifts and 185 undeclared Java files; the received tree has 15 and 207, respectively,
against 113 declared rows. Its manifest seal separately passes. The
[historical preflight](runtime-proof/historical/HISTORICAL_DONATION_PREFLIGHT.json)
records those facts. This packet does not report that guard as passed, replace it,
or alter its implementation or manifest to hide the discrepancy.

The immutable canonical revision is bound externally by
`m3/collections/verification/wrapper-convergence-20261018/SOURCE_PIN.json` at
publication. The core source packet does not contain its own eventual commit hash.
Scoped runtime acceptance does not itself advance broader target receiving phases.

## Read and search the packet

| File | Purpose |
| --- | --- |
| [FINAL_RECEIVING_STATUS.json](FINAL_RECEIVING_STATUS.json) | Actual 2f acceptance, precise scope, API additions, JNI, allocation and independent guard limits |
| [FINAL_RECEIVING_FILE_REVIEW.tsv](FINAL_RECEIVING_FILE_REVIEW.tsv) | 29 per-file runtime/test changes with original seals, actual receiver preimages and postimages |
| [FINAL_FILE_REVIEW.tsv](FINAL_FILE_REVIEW.tsv) | Unchanged 28-file 07ba sealing review; preserved as its original epoch |
| [DONOR_EVALUATION.tsv](DONOR_EVALUATION.tsv) / [JSON](DONOR_EVALUATION.json) | Searchable categories, donor revisions, rights, decisions and successor comparisons |
| [COVERAGE.tsv](COVERAGE.tsv) | Named candidate and actual receiver gates, source IDs, receipts and unexecuted broader scopes |
| [SOURCE_PROVENANCE.json](SOURCE_PROVENANCE.json) | Preserved source lineage and exact current SDK, runtime, bridge and allocation bindings |
| [runtime-proof/README.md](runtime-proof/README.md) | Human-readable execution index with command logs and qualification summaries |
| [runtime-proof/EVIDENCE_INDEX.tsv](runtime-proof/EVIDENCE_INDEX.tsv) | Exact SHA-256 and original locations for retained execution artifacts |
| [CANDIDATE_STATUS.json](CANDIDATE_STATUS.json) | Original 28-source/25-record candidate proof plus pointers to its accepted successor |
| [REJECTED_CANDIDATES.json](REJECTED_CANDIDATES.json) | Permanent comparator/opaque-owner counterexample and corrected receiving binding |
| [NAVIGATION_CORRECTION.md](NAVIGATION_CORRECTION.md) | Nullable navigation proof, reviewed owner domain and exception/callback boundaries |
| [candidate-proof/](candidate-proof/) | Original 25 candidate records, 141 compile identities, logs, baseline callback tapes and protected probes |
| [history/candidate-56-20261018/](history/candidate-56-20261018/) | Byte-exact original 56-file metadata freeze, including its then-pending receiver status |
| [FILE_REVIEW.tsv](FILE_REVIEW.tsv), [FROZEN_INPUT_INVENTORY.json](FROZEN_INPUT_INVENTORY.json), [HISTORICAL_README.md](HISTORICAL_README.md) | Preserved initial 26-file donor review, source inventory and inherited historical claims |
| [LATEST_AST_SCOPE.md](LATEST_AST_SCOPE.md) / [JSON](LATEST_AST_SCOPE.json) | Separate latest hierarchy PR and cache-candidate qualification boundaries |
| [AST_BUILD_STATUS.md](AST_BUILD_STATUS.md) / [TSV](AST_BUILD_STATUS.tsv) | Separate broader compiler/build requirements; collection evidence does not fill those gates |

For example, `rg 'BOUNDED_FILTERED_NAVIGATION|CONSTANT_MASK_STAGE|LONG_BRIDGE' DONOR_EVALUATION.tsv`
finds the relevant catalogue decisions. This extends the existing data lineage;
it introduces no catalogue engine and claims no automatic registration in the
Java catalogue/search APIs.

## Selected behavioral atoms

**Filtered navigation.** Read-only sets retain direct external navigation before
predicate/range demand. For admitted owners, map and mutable-set navigation reuse
an actual stored neighbor as a range anchor while retaining the original wrapped
iterator. A stored null may occupy any comparator position; presence is proved
from actual members. An arbitrary `IllegalArgumentException` is never treated as
range metadata. The rejected broad-catch implementation could retry a failing
comparator or swallow an opaque range-override exception; its exact probe and
failure logs remain permanent records. The corrected failure/admission contract
passes 140 actual checks, and the unchanged merged bounded contract passes 90,431.

Admission is limited to exact JDK tree/skip-list owners, their JDK-owned nested
range/key views, final M3 tree owners, and the reviewed private M3 wrapper chains.
Custom subclasses and opaque decorators retain their original query range path.
Bounded-query enhancement is not claimed for those opaque owners, and successful
comparator call counts are not claimed identical for every refined lookup.

**Strict read-only and deep views.** Sorted, reversed, sequenced, default-method
and endpoint writes refuse before mutation callbacks, including no-op calls.
Private generic filter chains use predicate metadata instead of recursive wrapper
frames. Original predicate order, exception identity, representative probes and
live backing remain the contract. The actual runner passes 32,267 read-only
checks and deep composition at 65,536 levels with a 256 KiB stack. Historical
byte-identical read/equality callback tapes remain source-bound candidate evidence.

**Primitive paths and masks.** Full signed 64-bit row IDs reuse physical int
offsets and 32-bit state words without narrowing row values. Stable selection
retains duplicates and path encounter order. Mandatory shape/coordinate validation
precedes constant-mask fast paths; identity and impossible stages do not excuse
invalid coordinates. Bounded windows retain cursor, consumed-coordinate preflight,
guard cells and output-alias refusal. The long facade that arrived in the newer
target is adapted separately: it keeps its public API, validation order/messages,
window semantics and mismatch diagnostic while reusing `selectLongJava`. Its
unchanged existing self-test passes 7,127 checks and 1,500 randomized cases on the
actual compiled receiver; its separate qualification also compares 1,259 exact
behavior observations and byte-identical public descriptors/constants.

**JNI.** Int and long paths share the existing region-based gather owner. The
original int symbol/prototype remains, and the long symbol is additive. There are
no comparator/predicate callbacks, retained Java pointers, critical-array pins or
new per-row object stores. Actual required-native execution uses checked JNI and
UBSan; a separate missing-library run confirms explicit refusal. This establishes
the recorded Linux backend scope, with no native-speed claim.

**Sorted direction changes.** An already sorted projection reverses equal-value
groups while preserving occurrences within a group. The O(n) direction change
retains O(n) output indexes; encounter-order sorting keeps its existing O(n log n)
owner. Java/common order proof passes 621,981 checks and required-JNI/common proof
passes 758,662. The API comparison retains all prior public/protected members and
types; it explicitly classifies four additions: `removeFirst`/`removeLast`
overrides, `selectLong`, and `filteredPathMasks`.

## Measured allocation scope

The existing `M3OrderWindowProof` allocation mode was reused with identical proof
bytecode against the matched before/after all-main classes. On the specified
32,750-entry ascending slice converted to descending, invoking-thread allocated
bytes fell from **262,064 to 131,048 bytes per call**, a difference of **131,016
bytes per call (49.9938946%)**. Three fresh JVMs per lane each produced three
100-operation samples; every sample in each lane had that lane's stated value.
Source/class rechecks passed. The [raw samples](runtime-proof/performance/RAW_SAMPLES.json),
[commands](runtime-proof/performance/COMMANDS.json), and
[derived result](runtime-proof/performance/RESULTS.json) preserve the measurement.
This result concerns allocation for that operation and input. It establishes no
elapsed-time, CPU, JNI, retained-heap, GC or broad collection performance gain.

## Donor and recipe lineage

Existing M3 and Synexia implementations are the source starting points. The
versioned Java 21 NavigableSet/Comparator/JNI contracts and exact OpenJDK TreeMap
and Guava source pins remain recorded in the donor catalogue. LeetCode 34/27,
HackerRank Lower Bound/Java BitSet and GeeksforGeeks Implement Lower Bound remain
reference-only discovery categories. No challenge, editorial or submission body
was copied by this task. Original per-file rights and notices remain intact; a
reference comparison does not upgrade a runtime dependency or relicense a donor.

The inherited README's claims about M3JDK21 #1204 and canonical commit
`72580cf231dc025965370933f099e914ddc407be` remain historical claims whose bodies were
not independently established by this catalogue-authoring pass. Current acceptance
comes from exact reviewed sources and actual executed receipts. The existing
Synexia recipe-first and canonical ownership invariants remain authoritative.
This finite pass establishes no whole JDK/Guava/Eclipse/Spring/Apache SDK closure,
arbitrary concurrency/serialization guarantees, full JDK image, jtreg/JCK or
other-platform native support.

## Catalogue delivery status

The first catalogue TEXT delivery failed the unchanged path-admission guard. Runtime acceptance remains intact. The corrected filenames and two bounded TEXT datasets are described in [the packaging correction](PACKAGING_CORRECTION.md); their actual SDK/receiving pass is pending. Preserved raw receipts retain their original execution paths and resolve delivery filenames through `DELIVERY_PATHS.json`.

## Later compiler and bootstrap source boundary

The additive [latest runtime scope](LATEST_RUNTIME_SCOPE.md) records exact serial documentation, monitored-scope, native-feature, TQ-custody and Object-vtable repairs. The c857 image assembly and default/CDS-on/CDS-off boot probes pass; targeted jtreg remains NOT_RUN after a retained preexecution sparse-group refusal. Earlier rows and finite collections claims remain unchanged; these gates do not establish whole-JDK or performance acceptance.
>>>>>>> 82eb7fc30da9d3cf5556a11fb4a2b4ec0f09f385
