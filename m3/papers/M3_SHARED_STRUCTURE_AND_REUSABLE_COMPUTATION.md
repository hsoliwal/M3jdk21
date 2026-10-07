# M3: Shared Structure and Reusable Computation in a Java Runtime

**Hitesh Soliwal and Contributors to the Synexia Project**  
Technical design paper • Version 0.1 • 7 October 2026

## Abstract

M3 investigates a runtime architecture in which immutable values preserve shared
payload identity, ranges, and composition, while indexed metadata allows related
operations to reuse prior computation. M3JDK21 is the Java 21 product target.
The initial focus is M3 String inside `java.lang.String`, followed by regular
expressions and shared precompute, collections, and SWT/Eclipse integration.
This paper describes the design premise, intended architecture, compatibility
constraints, and a reproducible evaluation programme. It presents design
hypotheses rather than measured performance results.

## 1. Motivation and scope

Immutability provides an opportunity to attach reusable knowledge to data whose
content does not change. M3 asks whether canonical payload identity and persistent
composition can make that knowledge useful across more operations than a
single-object cache. The intended benefits are less repeated scanning, copying,
allocation, and computation. Those benefits depend on workload reuse and on the
cost of establishing and retaining shared structures.

The starting point is an existing, sophisticated Java runtime, not an assumption
that the JDK lacks optimisation. Compact Strings already address representation
cost through Latin-1/UTF-16 storage [1]. The String API defines the observable
contract [2]. M3 explores a different axis: shared structure and reusable
operation metadata. Any comparison must use a specified OpenJDK baseline and
account for the optimisations that baseline already provides.

This paper focuses on runtime design. It does not establish novelty relative to
all prior research, claim universal speedups, or report a completed compatibility
qualification. A systematic related-work comparison and implementation-linked
results belong in subsequent revisions.

## 2. Design premise

The proposed representation has three cooperating layers:

| Layer | Intended responsibility |
| --- | --- |
| Canonical payload store | Immutable byte/UTF-16 payloads with stable logical identities and explicit lifetime rules. |
| Value structure | Atom references, ranges, and compositions describing a value without requiring a new owned payload for each value. |
| Indexed metadata | Shared hashes, search indexes, and reusable operation plans associated with appropriate payload or composition identities. |

A range describes part of a canonical payload. A composition describes the
ordered combination of ranges or atoms. Metadata records properties that can be
reused when their assumptions remain valid. Stable logical identifiers are
distinct from movable Java object addresses.

For example, repeated values resembling `customer:123` and `customer:456` may
share the prefix payload. That sharing alone does not eliminate the cost of
searching or hashing either complete value. The design question is which
properties can be composed, cached, or indexed economically. A whole-value hash
must still obey the specified String contract; a cryptographic digest generally
cannot be obtained by simply combining component digests.

The canonical design aims to avoid per-value owned payload arrays. APIs or native
boundaries that require contiguous storage may need a compatibility
materialization. That cost, its lifetime, and its frequency must be visible in
the evaluation.

## 3. Runtime architecture and ownership

Synexia is the canonical convergence workspace for reusable algorithms, recipes,
and provenance. M3JDK21 owns the runtime implementation. String precompute resides
within M3JDK21, in `java.base`, HotSpot, or JNI as appropriate; the runtime does not
depend on Synexia.

Three engineering routes support the programme:

1. Explicit M3 views exercise range/composition operations and contract tests.
2. Compiler and transformation recipes lower eligible operations to M3 plans.
3. Runtime integration makes M3 String the internal representation supporting
   `java.lang.String`.

These are intended routes, not interchangeable proofs of integration. A correct
explicit-view implementation does not by itself demonstrate that ordinary String
operations, VM intrinsics, or JNI use it.

VM-local canonical storage is the initial ownership boundary to qualify.
Cross-process or OS-level sharing is an extension requiring its own encoding,
synchronization, security, and lifecycle design. Metadata sidecars need bounded
retention, safe publication, and unambiguous versioning.

## 4. String compatibility

Representation changes must preserve externally observable String behavior [2]:
UTF-16 indexing, Unicode and surrogate handling, equality, hash codes, ordering,
search, case conversion, exceptions, and relevant serialization and native
interfaces. Identity sharing is an implementation opportunity; it does not
replace content equality for arbitrary Java strings.

Integration must also examine interpreter and compiled execution, intrinsics,
GC interactions, class loading, and affected CDS paths. Raw object addresses
cannot serve as durable identities across moving garbage collection. JNI
materialization and acquisition/release behavior require differential tests
against a Java semantic oracle.

Range views create a retention tradeoff: a small live view may retain a much
larger payload. Canonicalization also has lookup and synchronization costs.
Those costs are part of the design rather than incidental implementation details.

## 5. Regex and shared precompute

Regex work includes reusable compiled plans and indexes whose applicability is
defined by pattern content, flags, runtime assumptions, and data identity.
Java Pattern semantics remain the reference for the supported surface [3].
Captures, backreferences, lookaround, matching regions, flags, and error behavior
require explicit qualification.

A prefix/suffix or substring index can help an eligible search but cannot
generally substitute for arbitrary regex evaluation. The planner must identify
valid shortcuts and preserve a semantic fallback. Mutable matcher state belongs
to the relevant invocation or owner rather than an indiscriminately shared cache.

Precompute must be selective. Exhaustive substring metadata can grow
quadratically in input length; arbitrary regex/input combinations cannot be
precomputed without bound. Admission policies therefore need limits, reuse
signals, eviction, and version checks. Cold misses, low-reuse inputs, adversarial
patterns, and concurrent access belong in the test corpus.

## 6. Collections and desktop integration

After runtime qualification, collections can explore reuse of canonical keys,
indexed signals, and search metadata. Equality, ordering, null handling,
iteration, views, concurrency, and serialization contracts remain decisive.
Sharing must not silently turn an equality-based collection into an
identity-based collection.

SWT, Eclipse Platform/UI, Nebula, and GEF are integration targets. Their proposed
role is to consume qualified runtime and collection improvements through bounded
adapters and recipes. Responsiveness, resource use, and target compatibility need
application-level measurements; a String microbenchmark cannot establish an IDE
benefit.

TornadoVM provides a separate exploration surface for suitable accelerated
precompute. CPU execution remains primary. Transfer, preparation, synchronization,
and device-memory costs must be included when evaluating acceleration. Tooling,
provider catalogs, and repository-service forks support the wider programme;
they do not become independent owners of the M3 String runtime.

## 7. Evaluation programme

The evaluation tests hypotheses, including outcomes that may reject them:

| Hypothesis | Experiment | Main counter-cost |
| --- | --- | --- |
| Shared payloads reduce storage for repeated structure | Compare duplicate-rich, composition-rich, and unique-input corpora | Canonical lookup, structure size, and retained payloads |
| Shared metadata reduces repeated-operation work | Measure cold construction and repeated search/hash/regex operations | Index construction, misses, eviction, and native memory |
| Runtime changes preserve required behavior | Differential API tests and relevant JDK/VM/native tests | Semantic differences and integration gaps |
| Runtime benefits reach applications | Collection and SWT/Eclipse workload measurements | Adapter overhead, startup, and UI latency |

Use JMH where appropriate for microbenchmarks [4], together with application
workloads. Bind results to candidate and baseline commits, binary checksums,
hardware/OS, flags, corpus seeds, and reproduction commands. Preserve warmup,
independent forks, raw iterations, and variance. Avoid timing only the warm
operation while hiding preparation.

Measure throughput/latency, CPU time, allocation, retained heap, native memory,
GC, startup, and precompute preparation/storage. Include Latin-1 and UTF-16,
supplementary characters, unpaired surrogates, empty/large inputs, repeated and
one-off workloads, and contention. The planned regex corpus contains at least
10,000 deterministic cases; case count is a coverage target, not a substitute
for semantic breadth.

A simple break-even model makes reuse assumptions explicit. If preparation costs
P, baseline operation cost is B, and reused operation cost including lookup is R,
then reuse can amortize preparation only when B > R and the operation count
exceeds P/(B-R). This model excludes memory opportunity costs and contention;
measurements must include those separately.

The repository's [benchmark and compatibility specification](../release/BENCHMARK_AND_COMPATIBILITY_SPEC.md)
provides the detailed evaluation contract. Quantitative findings should be
published only with their raw evidence and reproducible commands.

## 8. Development plan

The sequence is source inventory and faithful reproduction, String integration,
regex/precompute, runtime qualification, collections, and desktop integration.
Reusable transformations converge in Synexia before product application.
Compiler and contract tests govern promotion; provenance and upstream licensing
remain explicit.

The [home-page roadmap](../../README.md#project-goals-and-plan) records milestone
deliverables and acceptance criteria. This paper is a design account, not an
implementation inventory. Future revisions should link individual architectural
elements to implementation commits and evidence, add related-work analysis, and
report both positive and negative results.

## 9. Invitation

The opportunity is to make immutable structure useful beyond storage: as a basis
for reusable computation across a Java runtime. Readers are invited to examine
the representation, challenge the cost model, identify compatibility boundaries,
and contribute reproducible workloads. The value of M3 will be established by
where this design helps, what it costs, and how faithfully it preserves the
contracts applications rely on.

## References

1. OpenJDK. [JEP 254: Compact Strings](https://openjdk.org/jeps/254).
2. Oracle. [Java SE 21 String API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html).
3. Oracle. [Java SE 21 Pattern API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html).
4. OpenJDK. [Java Microbenchmark Harness](https://github.com/openjdk/jmh).

## Authorship and provenance

This paper records the M3 design direction attributed to Hitesh Soliwal and
Contributors to the Synexia Project. References identify background and contract
sources; citing or integrating upstream work does not transfer its ownership.
Applicable rights and licensing boundaries are described in
[M3-SYNEXIA-NOTICE](../../M3-SYNEXIA-NOTICE.md) and
[M3-SYNEXIA-RIGHTS](../../M3-SYNEXIA-RIGHTS.md).
