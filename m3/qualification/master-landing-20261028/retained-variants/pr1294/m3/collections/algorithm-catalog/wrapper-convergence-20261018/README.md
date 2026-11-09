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
