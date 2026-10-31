# M3JDK21 catalogue convergence: implementation and review

This series refines M3JDK21's existing Java, JNI and HotSpot owners and adds the
requested collection-view and bounded literal-search capabilities. M3JDK21 is
the product. Synexia supplies the canonical convergence workspace, reusable
recipes, algorithm catalogue and donor custody used to produce these changes.
The resulting JDK image contains the runtime implementation; it has no runtime
dependency on the recipe workspace.

The companion canonical work is [com.synexia PR #12468](https://github.com/hsoliwal/com.synexia/pull/12468).
Its existing `synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017`
crate owns the reproducible transformations and evidence. This document explains
the implementation and its practical limits. `QUALIFICATION.json` in this
directory records the final source, build and test identities; it is the
authority for the acceptance status of a particular image.

## Source series and fleet reconciliation

The receiving baseline is M3JDK21 commit
`a62756a22a379d7d08c1e633e55b93155cea9ff5`. The original qualification used
`a3e88976ee8664d1ef5d0cade96aec1206b2133d` and comprised 96 changed paths. The
intervening fleet delivered part of those changes and improved several shared
owners. A complete path comparison reduced the current functional series to
52 deltas: 30 additions and 22 modifications. Forty-four original paths stay
outside that delta.

The current series preserves the fleet's complete porting invariant, existing
mapping records, stronger RX7 codec launcher, six-slot Object vtable, updated
String pool/search preparation, and the current collection-mask dependencies.
The String bridge receives only the prepared-facts accessor while retaining
both current `mayContain` search gates. Current filtered-list predicate fusion
also remains intact.

A separate five-file build/metadata successor addresses two reproduced current
baseline failures. Four builder sources contain 21 non-ASCII em dashes in line
comments that the standard ASCII JDK compilation rejects. The correction changes
only those comment characters. Real javac token and parse-tree comparisons
preserve 10,824 token tuples and all 169 JavaDoc blocks. The fifth file appends
four missing `REVIEW_REQUIRED` ledger rows already demanded by the current
mapping and unchanged intake checker; all 23 existing rows remain byte-identical.
Those rows grant no license, perform no source copy and accept no pending port.

The functional and build/metadata datasets have disjoint targets and are
received serially. Historical qualification receipts retain their original
source and toolchain epochs. A later read of master at
`677437be295b23f7ccf7409be287008944d0e8b0` found every proposed preimage unchanged
and every proposed new path absent. The review branch retains its qualified
parent so later unrelated fleet commits remain base-branch work.

## Implementation

### Rope navigation and bounded byte searching

`java.lang.M3String` caches the complete clipped leaf interval reached by a
cursor. Forward and reverse reads can reuse that interval until they leave it;
a root traversal is needed on a cache miss. Existing cursor fields and all 49
compared public descriptors are preserved. The change does not flatten the rope,
add a second text owner or change substring coordinates.

`jdk.internal.m3.M3CompositeBytes` gains reusable prepared literal metadata and
two cursors sharing one private KMP continuation state:

| Route | Input access | Retained search state | Coordinates |
| --- | --- | --- | --- |
| Resident composite | Borrow one read-only piece at a time | Prepared needle/prefix table and current traversal state | Absolute byte offsets in the selected composite range |
| `InputStream` | Positive short reads into a bounded buffer | Prepared needle/prefix table and at most 64 KiB read scratch | Caller-supplied starting byte coordinate through an exclusive limit or EOF |

KMP carries the matched-prefix length across every piece/read boundary. It does
not need overlapping copies of the input and does not perform a binary lookup
for each byte. Preparing a needle costs O(m); scanning costs O(n + m) including
preparation. Prepared needles are limited to 1 MiB, with approximately five bytes
of byte-array/prefix-table payload per pattern byte, excluding object headers.
Existing resident storage and piece metadata are separate from search-state
memory. Transient allocation is measured below and is not claimed to be zero.

The cursors support strict clipped ranges, overlapping or disjoint occurrences,
empty-pattern boundaries, long offsets, and bounded interruption polling. The
stream cursor labels its current position; it does not seek or skip to the
supplied starting coordinate. The caller retains stream ownership. Reads can
prefetch beyond a returned match, so subsequent matches must use the same cursor
to retain buffered bytes. Ordinary I/O exceptions retain the stream's own
consumption semantics. Valid `InterruptedIOException.bytesTransferred` bytes are
retained before propagation and the interrupt flag is restored. A zero-progress
nonempty read is refused instead of retried forever. Blocking-read interruption
still depends on the particular stream implementation. Each cursor has one
caller.

The original 20 outer storage methods keep their compared bodies/descriptors.
The new prepared-search API is additive. The 380,535-case independent oracle
covers seams, ranges, overlap, EOF, empty patterns, cancellation/resume and
malformed-read behavior. The original five foundation fixtures remain present.

### Regex behavior and state masks

Java's `Matcher` consumes a `CharSequence`; contiguous storage is not a general
requirement of that API. A fixed overlapping window is nevertheless insufficient
for arbitrary Java regex behavior: unbounded matches, anchors, lookaround,
captures and greedy selection can depend on text beyond a chosen window. The
catalogue therefore rejects the proposed universal fixed-window shortcut.
See the [Java 21 Matcher contract](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Matcher.html).

The prepared trigram change is a conservative negative gate on eligible String
inputs. It reads already prepared facts for the exact owner/range coordinate;
the search path does not admit or prepare new facts. An absent packed trigram
can reject a candidate. A positive result or collision continues through the
existing exact regex path. Mutable inputs retain their existing route. Tests
exercise groups, regions, reuse, `hitEnd`, `requireEnd`, compact/noncompact
Strings and interpreted/default execution. Existing public String, Pattern and
Matcher descriptors remain unchanged; the internal JavaLangAccess/default
accessor addition is identified separately in the source catalogue.

RX7 reuses the qualified Java/JNI token, lexical and transition-composition
owners. State and kind masks, spelling identifiers and compact transition
arrays carry the supported regular/token programs into the native kernels.
Bounds, malformed programs, state widths, array lengths and exported JNI
signatures are checked explicitly. Unsupported regex behavior retains its
declared refusal/fallback path. This is not an implementation of every Java
Pattern feature over an arbitrary stream.

### Dynamic arrays, offsets and collection wrappers

The collection work uses existing dynamic arrays, bitmap ordinals, index
projections and path masks. It does not add an entry-node storage backend.

The refinements preserve callback order and count, comparator order, live-view
behavior, exception translation and mutation rules. In particular:

- A readonly indexed iterator uses one object instead of an iterator plus
  wrapper, while retaining repeated live predicate evaluation and the original
  unsupported-remove behavior.
- Spliterator traversal saves/restores its active callback state across
  reentrant calls. Existing identity-based collection behavior remains intact.
- Sorted projections keep stable equal-key groups when traversed in reverse.
- Plain owned BitSet ordinals can be projected directly, avoiding a per-element
  byte-mask allocation. Trust propagates through both operands of bitmap
  algebra. Opaque predicates and externally aliased bitmap state keep their
  existing evaluation/materialization behavior.
- Current-master required/excluded mask selection and output-versus-mask alias
  refusal remain in the dependency closure. Projection output is separately
  allocated and does not violate the new alias rule.

The additive intrinsic `java.util.CollectionViews` lives in `java.base`. Its
seven factories provide filtered Collection, Set, SortedSet and NavigableSet
views, plus sorted, filtered and filtered-sorted snapshots. Live filtered views
and explicit snapshots retain their respective semantics. The standalone
`com.m3.collections.M3CollectionViews` API remains usable with its existing
module setup. A rejected attempt to hard-link that standalone facade to the
new JDK class is retained in the evidence; it would have broken stock-JDK
compatibility.

The installed-image acceptance gate checks `jrt:/java.base` origin, seven factory
descriptors, baseline/direct/mixed routes, reentrant callbacks and default,
interpreted, C1 and C2 execution. Host dependency qualification separately
covers the exact 11 production owners and necessary fixture support. It does
not stand in for a full test of unrelated providers added by the fleet.

### Native object metrics

The former object-size JNI path requested a JVMTI environment for every call
without disposing the returned environment. It now caches a VM-lifetime
environment with acquire/release publication. Racing cold-start losers dispose
their extra environment; the winner remains live for the VM. Existing Java
signatures, null checks, lookup failures and object-size failures are retained.
A refused loser disposal is reported explicitly instead of silently claiming
successful cleanup.

The exact JNI/JVMTI-table oracle observes 100 environments for 100 old calls and
one environment for 100 new calls. A forced 16-way cold race creates 16, disposes
15, and still has one live environment after 17,000 calls. Fault injection and
sanitizers cover error and race paths; the Java fixture makes 48,048 calls on 16
threads under `-Xcheck:jni`. LeakSanitizer was not run successfully in the ptrace
environment; the failed attempt remains distinct from the passing ASan/UBSan
results. No HotSpot object-layout or Java API change is claimed.

## Measured benefits and costs

The comparisons use pinned source, checked results, explicit CPU/allocation
counters and retained raw observations. They are host workload measurements,
not production-wide speed claims or measurements of the final distributed
image. Pattern preparation is outside timed literal scans; cursor allocation
is included.

| Operation | Observed benefit | Retained cost or limit |
| --- | --- | --- |
| Reverse rope traversal | About 4.84-4.90x CPU improvement for tested eight-unit leaves and 5.78-8.35x for tested 64-unit leaves | Forward geometric ratio 0.949; some fresh/random cases regress. One sequential fork per variant on a shared host. |
| Resident piece KMP, 8 KiB pieces | 4.29-4.54x CPU improvement for misses; 4.33-5.85x for late hits against a prepared KMP comparator using the unchanged `byteAt` API | Early hits cost 1.30-2.41x as much CPU and 144-256 allocated bytes versus 32. This is a new-API comparison, not an old search API A/B. |
| Four-byte piece scan | Tested miss CPU improves 2.99-3.89x | Per-piece ByteBuffer views allocate 917,592 bytes per operation versus 32 for the comparator. Bounded retained memory does not mean zero transient allocation. |
| Readonly indexed iteration | Allocation reduces from 48 to 24 bytes in the measured workload | Callback transcripts and exception behavior are unchanged; no universal throughput claim. |
| Owned bitmap projection | A tested 65,536-slot projection saves 65,552 allocated bytes per operation | Opaque control retains 106,640 bytes and 8,195 callbacks. One cached zero-allocation control has a 24.8% CPU regression in the second fork. |
| JVMTI environment reuse | One published environment replaces repeated per-call environments | Cold races still allocate temporary environments; loser disposal can report failure. |

The bitmap all-selected shortcut has a much larger measured ratio because it
reuses an existing result. The catalogue labels that shortcut explicitly; it
does not present the ratio as general materialization speed. Negative cases
remain inputs for subsequent catalogue passes rather than being discarded.

## Qualification and reproducibility

The canonical sequence is: exact preimage/absence and mode admission, actual
Maven/OpenRewrite transformation, source sealing, strict receiving, then replay
with zero further changes. Production source is written by the receiver from
actual SDK Results. Immutable Git checkout materialization is tracked separately
from source transformation. Public-descriptor, token and behavioral comparisons
are scoped to the operations they establish.

The historical 96-file epoch completed a full Linux fastdebug JDK image,
including HotSpot and both CDS archives. Its targeted 11-test jtreg run, original
243-test standalone collection suite, installed-image collection routes and
RX7 JNI contracts are retained with their original image/source hashes. Those
results are never relabeled as a new image's results.

The current source seal checks every tracked `src/` and `make/` blob against the
qualified base plus the explicit emitted deltas. Its image build uses the normal
JDK build with `--disable-javac-server` for the sandbox's unavailable loopback
service and serial image packaging after a reproduced parallel packaging
failure. No source encoding, compiler warning, consistency guard, GC, CDS or
JNI check is disabled to admit a source change. Failed build/runner attempts and
the successful corrective steps remain in the proof record.

The current acceptance record separates these gates:

| Gate | Meaning |
| --- | --- |
| Canonical SDK/sealing/receiver | The selected transformation is actually executable, preimage-bound and idempotent. |
| String source controls | 31 lineage/refusal tests; 14 phase rows; immutable historical receipts and current successor endpoints. |
| Collection host closure | Current mask dependencies, 16 before/after execution-mode transcripts, native required/excluded and alias contracts. |
| Built-image jtreg | Targeted String/regex, storage, literal search and object-metrics tests using actual image owners. |
| Built-image CollectionViews | Actual intrinsic class origin and factories, standalone dependency closure and callback/mask fixtures. |
| Built-image RX7 | Actual `libjava` exports, token/lexical/transition fixtures and Java/native ArrayPower contracts. |

The 4,770-path/33-family source inventory and ordered family receiver remain
source-pin controls. They do not establish that every file was refined, every
provider was built, or a family is complete. The active family and existing
receiving order remain unchanged.

For replay, use the companion crate's documented Maven/provider setup and its
strict `receive.py` with the immutable target revision. Apply
`fleet-convergence-20261019`, then the separately qualified build/metadata
successor, then review metadata. Run each receiver in check mode, apply it, and
require a zero-change replay. Reproduce the image with a Java 21 boot JDK and
the recorded configure/build commands. Portable image-test runners and exact
test sources are retained in the proof archives. Compiled JARs, classes, native
libraries and Maven caches are regenerated rather than shipped in the PR.

## Catalogue, donors and continued refinement

The canonical catalogue records problem category, incumbent operation, donor
revision, license/notice custody, candidate complexity, behavioral obligations,
validation result, measured regressions and selection rationale. Its searchable
indexes reuse existing atom-catalogue owners. Large-file inventory is partitioned
serially within the existing node bound; opaque or parser-refused sources are
identified as such rather than assigned a fabricated AST fingerprint.

Representative discovery references are [LeetCode substring search](https://leetcode.com/problems/find-the-index-of-the-first-occurrence-in-a-string/),
[HackerRank DNA health](https://www.hackerrank.com/challenges/determining-dna-health/problem),
and [GeeksforGeeks KMP](https://www.geeksforgeeks.org/dsa/kmp-algorithm-for-pattern-searching/).
They supply problem/algorithm comparisons; editorial or submission source is
not copied. Qualified GitHub/Synexia source donors and inherited notices are
identified by exact commits and blobs in the companion catalogue. The main
donor families include the existing M3String cursor/prepared-facts work,
collection view/predicate/mask owners, native RX7 contracts and foundation
dynamic-array/storage components.

The invariant for another pass is straightforward: take one source file,
identify the problem category and behavioral atoms, consult the accumulated
catalogue, compare suitable donors and the incumbent, apply an independently
replaceable change through the canonical recipe, and record the resulting
evidence. Preserve the original public contract and composed behavior. Split
atoms when doing so improves reuse, replacement or verification; do not impose
an arbitrary atom-count limit or fragment a coherent operation without benefit.

Original notices and unresolved rights decisions remain visible, including the
Apache-2.0 origin of the additive intrinsic collection owner. Namespace changes
do not grant target-distribution or relicensing authority. Reviewable draft PRs
retain the applicable rights, full platform/architecture and broader-family
gates. This series makes no universal-regex, zero-allocation, all-provider or
whole-repository completion claim.
