<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Native String ownership and composed-value qualification

M3JDK21 owns these changes directly in `java.base` and its existing JNI String
integration. Ordinary admitted `String` objects continue to retain their M3
owner/range and the shared empty compatibility sentinel. Tuple composition keeps
the original child coordinates and payload owners. No additional text store,
payload array, owner class, production native symbol, field, or method descriptor
is introduced by these repairs.

The native ownership atom starts from PR #1635 at
`a115db5d8d9a53c20bf6f6880286a967a80a8dda`. Tuple accounting and code-point capacity
are separate successor atoms starting from the already received native ownership
postimages. The canonical Synexia Maven/OpenRewrite receipt binds the exact
before/after sources and is authoritative for receiving. The candidate documents
and test fixtures do not become a second runtime or recipe implementation.

## Ownership transfer and defect repairs

All six local atom factories own their native address until Java owner
construction succeeds. A `finally` path now frees the address on an unsuccessful
exit, including validation errors and owner-construction failure. Once an owner
exists, the pool owns the address and byte reservation until its weak cleanup
record is published. A failed weak-reference allocation releases both directly;
a failed list publication uses the existing idempotent record release. A
successfully published record retains cleanup responsibility if construction of
the returned M3 value subsequently fails. The single-unit lane uses this same
handoff while preserving its existing accounting and admission behavior.

Tuple publication follows the corresponding counting rule. Incrementing the
retention counter does not itself publish a cleanup record. A failure before
successful list publication returns that increment. After publication, the
existing queue owns the decrement. The zero-retention-cap path still builds a
composition with the same children and no cached tuple. Tuple balancing,
canonical lookup, layouts and the existing store remain unchanged.

The code-point factory receives a length and coder calculated by an earlier pass
over a caller-owned mutable `int[]`. Rereading changed data can require a
supplementary pair where only one unit was allocated. Checking the final output
length after writing is too late. The repaired factory requires one remaining
unit before a BMP write; a supplementary pair requires a UTF-16 coder and two
remaining units, with widened arithmetic for the capacity comparison. Existing
invalid-code-point, Latin-1 and length-mismatch exception categories remain.
This is an explicit native memory-safety repair. It does not promise a stable
snapshot of a concurrently mutated input array.

## Evidence and its exact scope

All following counts are per process, with CompactStrings enabled and disabled
in separate processes. Java source and fixture compilation used `-Xlint:all
-Werror`; the test JNI library used C17, `-Wall -Wextra -Werror`.

| Evidence | Candidate result | Baseline/control result | Scope |
| --- | --- | --- | --- |
| Native owner-construction/reference/wrapper injection | 174 checks over 37 scenarios | 174 checks explicitly observe the original leaks | Exact copied Atom/Pool bodies with test-only value/fact dependencies and allocation ledger |
| Native publication regression | 40 checks across five pool ingress lanes | Expected exit 1 at the reservation-release assertion | Real pool operations; one isolated bucket has a throwing `add` fixture; current authoring execution uses the copied owner closure |
| Tuple constructor/wrapper injection | 15 checks | 15 checks explicitly observe the original count leak | Exact tuple/pool bodies, injected constructor boundary, test-only value dependency |
| Tuple publication and structural sharing | 102 checks | Expected exit 1 at the tuple-count assertion | Real pool and cleanup records; retry retains the exact original child coordinates and native payload owners |
| Zero-cap tuple composition | 98 checks | 98 checks | No cache retention; composition still shares its children |
| Code-point bounded-write witness | 95 checks: 12 invalid cases, two mutations between real passes, four valid cases | 95 checks observe intercepted unsafe attempts or incorrect Latin-1 truncation | Every baseline byte write is checked against its allocation before Unsafe; an out-of-range baseline write is never executed |
| Candidate native code-point regression | 39 checks: 12 invalid and five valid cases | No raw-Unsafe baseline run | Candidate Atom/Pool execute their actual native access with uninstrumented bodies; ordinary String admission is not active on this boot-JDK control |
| Critical-first JNI stock control | 1,262 checks over 180 fixtures | Both compact modes pass | Actual native test library and stock Java; zero M3 fixtures, so this is only bridge/oracle validation |

The isolated dependency closure contains the actual Atom, Pool, Owner, Tuple,
Immediate and FrozenLexiconIndex sources. Its M3 value/fact dependency stubs are
test-only and explicitly labeled. The native allocation probes, constructor
hooks and bounded-write barrier do not ship in the product. Baseline witness
success and expected negative-control exits are distinct from fixture authoring
errors. Two early strict-compilation attempts caught serializable fixture fields
and an exposed default constructor; those fixtures were corrected without
lowering compiler gates. The append-only authoring record retains those attempts.

## Installed-image tests

`M3StringCriticalBulkTest` invokes `GetStringCritical` before `GetStringChars`
through the existing bridge symbols. Independent UTF-16 arrays provide the
content oracle. It checks the original M3 value, owner, coordinate and shared
empty sentinel after native access and round-trip creation. The mapped fixture
constructs an ordinary String from a two-part mapped composition, then checks
content and mapping lifetime across GC. That fixture owns an immutable mapping;
it does not establish OS-wide interning.

The source-built image must run that JNI test together with
`M3StringNativePublicationFailureTest`, `M3StringTuplePublicationFailureTest` and
`M3StringCodePointBoundsTest`, under their declared jtreg modes. The code-point
test's `requireM3` modes additionally require actual ordinary String admission.
The canonical receiving evidence records those product results separately from
the authoring table above. A patched boot JDK, compiler pass or stock native
control cannot stand in for installed-image execution.

These changes preserve existing explicit JNI compatibility projections. They do
not make a discontiguous composition a physical Java array or a universally
contiguous JNI pointer, enable the currently excluded M3 C1/C2 paths, prove
OS-wide canonicalization, or remove ordinary admission refusal. No throughput,
retained-heap or process-native-memory improvement is claimed by these bounded
failure-path and correctness checks. String qualification remains the active
receiving phase.
