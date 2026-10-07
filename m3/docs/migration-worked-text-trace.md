# Worked text contract across the three routes

Status: **explanatory scenarios and acceptance oracles, not an implemented API or executed test report**.
Read with [the migration handoff](mindex-migration-handoff.md) and [shared-atom contract](shared-atom-concatenation.md).

The operation names admit, join, slice and materialize below are conceptual labels. They do not announce new methods. Bind them to the actual mapped API and exact candidate before implementation.

## 1. One value, mixed shared and local storage

Assume a reviewed immutable lexicon generation contains these exact UTF-16 atoms:

| Label | Storage authority | Content | Code-unit length |
| --- | --- | --- | --- |
| A | shared lexicon L, generation 7, record 12 | "red " | 4 |
| B | shared lexicon L, generation 7, record 18 | "fox" | 3 |
| C | VM-local pool P, generation 3, record 91 | " #42" | 4 |

These namespaces, generations and record numbers are fictional teaching values, not real mapping records. The spaces are part of the content.

The value J = A + B + C denotes **"red fox #42"**, length 11. Its segments are A[0,4), B[0,3), C[0,4). Its logical position map is:

| Logical range in J | Backing range | Text |
| --- | --- | --- |
| [0,4) | A[0,4) | "red " |
| [4,7) | B[0,3) | "fox" |
| [7,11) | C[0,4) | " #42" |

A and B are shared hits. C is an exact miss admitted locally. Joining these already admitted atoms must not copy their character payload under the proposed no-copy contract. Local miss admission may copy caller-owned mutable input once to establish immutable ownership. The admission cost must not be hidden inside a “zero-copy join” benchmark.

The descriptor records or retains lifetime owners, checked lengths and ranges. It must not persist Java references or process addresses. A second JVM may map A and B's file-backed pages but cannot dereference this JVM's C handle. To transfer J between processes, use a reviewed content/descriptor protocol: shared records can be re-resolved by compatible image identity; local content must be transmitted/admitted under the receiver's policy. Sending local numeric ID 91 is not sufficient.

### Answers that must remain representation-independent

- J.length is 11 code units
- J.charAt(4) is 'f'; J.charAt(10) is '2'
- slice(J,4,7) denotes "fox"
- slice(J,3,9) denotes " fox #", spanning all three atoms
- Joining a differently segmented sequence with the same exact text has the same content equality, Java-compatible hash and lexicographic order where those operations are promised
- Object identity and intern identity require their own API contract; equal text alone does not prove identical objects or descriptors

No prescribed physical tree shape is implied. A balanced rope, flat directory or another reviewed representation can meet these observable results while having different metadata and indexing costs.

### Same number, different authority

Suppose the number 12 occurs in several inspected families. This teaching example does not claim those actual stores currently contain row 12.

| Coordinate | What it can mean | Why it cannot substitute for another 12 |
| --- | --- | --- |
| com.synexia.indexstring resolver payload/lexical ID 12 | Text admitted under that resolver's contract, with applicable language coordinates | Another resolver can assign 12 to different content |
| Language ID 12 | Language/namespace metadata under its defining registry | It is not a character position or lexical content ID |
| com.synexia.mindex.MIndexString pool content index 12 | Whole-string entry in one process-local pool | It is not the canonical IndexString resolver ID |
| com.synexia.indexstring.MIndexAtomStore atom row 12 | Structural atom in a particular immutable store | Its textual payload, children and domain are separate lanes |
| MIndexDag node row 12 | Node in one graph's node table/version | The node may refer to a different atom row and payload ID |
| Logical text offset 12 | A UTF-16 position inside a specific value/view | It may be out of bounds; it conveys no owner identity |
| Shared image byte offset 12 | Position within a versioned format/file | Width, headers, range checks and generation must be known |

The conversion bridge between runtime pool and canonical IndexString operates on exact logical content with receipts rather than translating equal numeric IDs. Structural admission may reuse text payload while allocating distinct structural coordinates. A metadata cache needs the owner/version in its key; an integer-only key silently crosses identity domains.

## 2. Slice retention is two separate questions

For V = slice(J,4,7), distinguish:

1. **Payload ownership:** which backing remains live so V can still read "fox"?
2. **Wrapper retention:** does V retain the entire source wrapper J, or only canonical atom/range coordinates and their owners?

An explicit source-retaining view may intentionally keep J and consequently C alive even though V reads only B. An owning/canonical substring may retain B's owner and a range without retaining J's wrapper or C. Both can avoid copying payload; their retention semantics are different.

The #7584 route-contract candidate documents this distinction. It does not justify changing an existing API's identity/exception behavior silently. Map these operations independently:
- explicit view, whose source wrapper may remain reachable
- owning String-shaped substring, which preserves its specified code-unit and exception contract
- optional compaction, which intentionally copies to reduce retained storage

Empty and full-range special cases need explicit decisions. A String-compatible full-range substring can require the same object result; a general CharSequence view need not have that identity promise. Do not use one generic fast path for all routes without checking the observable contract.

Acceptance scenario:
- Drop application references to J, A and C while retaining V
- Force the supported lifetime/GC/eviction conditions
- Verify V still returns exact content
- Measure which owners/wrappers remain live
- Compare that result to the chosen API's retention promise; do not declare unexpected retention harmless merely because content is correct

## 3. Regex across the shared/local seam

On J, the illustrative Java-style pattern **(fox) #([0-9]+)** has a match spanning [4,11), group 1 [4,7), and group 2 [9,11). The match crosses the B/C boundary.

A per-atom search that only checks B and C independently misses the complete match. A sound prefilter may identify candidate ranges, but the selected engine must verify the full logical input, including seam state and captures.

For the view W = slice(J,4,11), the same content is "fox #42":
- View-relative full match: [0,7)
- View-relative group 1: [0,3)
- View-relative group 2: [5,7)
- Source-relative coordinates add the view origin 4

A native/GPU/search result must declare whether it returns bytes, code units, code points, atom-local offsets or view-relative logical offsets. Capture offsets cannot be reused across these coordinate systems without a checked conversion.

Replacing group 2 with "7" yields "red fox #7". A range-aware replacement can conceptually reuse prefix J[0,9), then retain/admit replacement "7". It still must preserve the chosen replacement API's group expansion, escaping, match iteration and exception behavior. This example is not an algorithm for arbitrary lookbehind, backreferences or zero-width replacements.

Required negative/edge scenarios:
- Region begins or ends inside an atom
- Same text split at every possible seam
- Empty match at the view boundary
- Anchoring/transparent bounds and flags change
- Replacement contains dollar/backslash syntax
- Engine does not support a requested construct

For Java Pattern compatibility, compare the exact candidate against the supported stock-JDK oracle. A restricted engine must explicitly fall back or reject unsupported features; it cannot silently return “no match.”

## 4. Encoding and split-surrogate traps

For the ASCII J example, whole-stream UTF-8 bytes correspond directly to its characters. That easy case does not prove fragment-by-fragment encoding is generally valid.

Consider H containing one high-surrogate code unit U+D83D and T containing one low-surrogate code unit U+DE00. Their logical concatenation denotes U+1F600:
- UTF-16 length: two code units
- Whole-stream UTF-8: F0 9F 98 80
- Encoding H and T independently with strict malformed-input reporting fails for each isolated fragment
- Encoding the whole logical stream with the same policy succeeds

The encoder must carry pending-surrogate state across segments. With replacement policies, independent encoding can produce replacements rather than the correct supplementary character. A cached atom-level byte representation is not automatically composable at this seam.

Now slice the concatenation to [0,1). Java code-unit slicing can preserve the isolated high surrogate. Exact UTF-16 storage remains valid; strict UTF-8 encoding must report malformed input. A stricter surrogate-safe view may reject the slice earlier, but that is a different contract and must not replace general String.substring behavior.

For stateful charsets, even well-formed character boundaries may require shared encoder state. Cache keys include charset, byte order where relevant, and malformed/unmappable policies. Test whole-value encoding, not only each atom separately.

## 5. Route-by-route execution of the same scenario

| Step | A: explicit view on stock JVM | B: compiler lowering | C: complete modified JDK |
| --- | --- | --- | --- |
| Shared hit/local miss | Application calls reviewed owner API | Lowered ingress calls reviewed runtime membrane | Internal String admission after safe bootstrap |
| Join A+B+C | Explicit range-aware join | Eligible expression lowered while preserving operand evaluation | Ordinary String operation uses integrated storage path if supported |
| Slice [4,7) | Explicit view/owning operation chosen by caller | Lowered substring preserves owning and exact exception contract | Ordinary String substring contract remains public |
| Regex | Range-aware CharSequence consumer or explicit fallback | Preserve selected engine and String API boundary | Integrated String/regex path must satisfy full advertised Java behavior |
| UTF-8 output | Explicit destination encoding/materialization | Preserve encoder policy and exception timing | Standard API semantics, including required mutable output allocation |
| Legacy String-only call | Explicit materialize-to-String | Materialize at unconverted ABI boundary | Public result already String; internal native contiguity can still require a copy |
| Unsupported operation | Caller chooses supported adapter | Refuse transformation or keep original Java | Reject unsupported runtime mode safely; do not advertise unvalidated support |

### Route A: explicit operation boundaries

Conceptual sequence:
1. Obtain A/B from the reviewed mapped owner and C from local admission
2. Join immutable ranges
3. Select explicit view or owning slice
4. Match with a compatible CharSequence consumer
5. Encode directly into the required output or materialize at a String-only boundary

At the inspected #12 mapping, M3-TEXT-EXPLICIT-001 specifically records admission through LocalM3Arena rather than direct mapped lookup. Therefore this whole shared-hit scenario is a target acceptance example, not a claim that #12 already supplies every step.

### Route B: preserve effects before optimizing storage

Suppose the source expression conceptually combines readPrefix(), value.toString() and readSuffix(). The transformer must not reorder or duplicate these calls. If value is null, String concatenation conversion differs from an explicit value.toString() call. Preserve the exact original expression's null/exception semantics rather than applying one blanket null rule.

A receiver evaluated for substring, its begin/end expressions, range checks and any materialization must retain the required evaluation order and exception class. If a transformation cannot establish that, keep the original code and record the refusal. Identity comparisons, monitor use, serialization, reflection and unresolved overloads are separate eligibility checks.

The mapping for the lowered operation links to M3-COMPILER-001 plus the text/slice/owner mappings it relies on. Compiler success does not prove semantic parity.

### Route C: same characters, larger proof obligation

The public value is ordinary java.lang.String. The VM must understand the chosen storage in every advertised execution mode. A direct-buffer descriptor or application CharSequence does not establish that representation.

Run the scenario through interpreter and supported JIT/intrinsic paths, different collectors, JNI/JVMTI and relevant serviceability/archival modes. If the candidate supports only interpreter mode, record those other modes as unsupported/unverified. The historical #6 result must not be relabeled as a current-master full pass.

## 6. Materialization and failure ledger

Before accepting the scenario, record every boundary:

| Boundary | Permitted allocation | Required explanation |
| --- | --- | --- |
| Local admission from mutable input | Immutable owned payload on a miss | Defensive ownership; compare hits before redundant payload allocation where supported |
| Join/slice descriptor | Metadata/owner references | Count segments/depth and retained owners; no blanket O(1) |
| Mutable char[] or byte[] output | Independent writable destination | Mutating output must not mutate J |
| Stock String-only consumer | Ordinary String payload as needed | Explicit compatibility boundary on A/B |
| Native contiguous pointer ABI | Temporary contiguous representation as needed | Owner, encoding, release, exceptions and lifetime |
| Optional compact copy | Deliberate replacement payload | Named retention trade-off, not a hidden join optimization |

Failure cases include length overflow, cache exhaustion, unavailable shard, corrupted generation, concurrent owner close and allocation failure. They must not silently change content or invalidate a live supported view. A “same inode” observation is not by itself immutable-publication or retained-memory proof.

## 7. Evidence needed to close this example

Attach the mapping IDs, exact source/target/image pins and route/mode for:
- Exact content/range/capture/encoding answers above
- Caller mutation and output-array independence
- Shared-hit/local-miss and two-process generation behavior
- Descriptor allocation, copied payload and retained owner measurements
- Unsupported-engine and unresolved-lowering refusal
- Lifetimes under GC, eviction, close and failure
- Exact exception-class behavior where String compatibility is claimed

### Filled planning receipt, explicitly not run

This is a human-readable proposed test record, not a machine acceptance record:

| Field | Planning value |
| --- | --- |
| Scenario | Mixed shared/local J = "red fox #42"; slice [4,11); regex captures and whole-stream encoding |
| Relevant candidate mapping IDs | M3-TEXT-EXPLICIT-001, M3-TEXT-SLICE-001, M3-JOINED-001, M3-RESOLVER-001, M3-DICTIONARY-001, M3-PRECOMPUTE-001 |
| Source inspection context | bc49cd7610bf5974f6baa31ba64e6885f4117528; no implementation selected by this example |
| Target implementation commit | Unassigned; documentation commits are not runtime candidates |
| Image/toolchain/OS/flags | Unassigned |
| Route | A first; B/C require their own receipts |
| Expected result | J length 11; W length 7; W capture 1 [0,3), capture 2 [5,7); exact whole-stream bytes |
| Command/harness | To be implemented and reviewed; none executed here |
| State/counts | Not run; no passing/failing count asserted |
| Artifact | None |
| Review decision | Not accepted; implementation, command and exact-head evidence missing |

Rejection example: a contributor attaches a passing ASCII-only join result from another commit and marks this record verified. Reject promotion: it does not bind the selected candidate, exercise mixed ownership, test captures/encoding seams or establish lifetimes. Keep the limited result as its own scoped receipt rather than deleting it or relabeling it.

Documentation completeness means the scenario and required decisions are explained. Owner-scoped acceptance means the actual selected owner passed its scoped gates. All-family acceptance additionally needs the complete reviewed inventory and family/integration gates. These three statements must remain separate.

These are proposed acceptance scenarios. No box is marked passed by this document.
