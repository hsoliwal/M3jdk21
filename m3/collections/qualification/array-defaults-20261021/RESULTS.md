<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->
# Array-first refinement: executed results

## Outcome

The sorted-batch candidate passed the bounded Java 21, actual OpenRewrite SDK,
behavioral, API and allocation gates described below. Exact SDK output was
materialized in the receiver working tree after those gates. This is a review
candidate; full reactor, provider and installed-JDK admission remain unverified.

`M3LongLongTreeMap.putAllSorted` now validates and counts the batch before
mutation, then merges from the right. It reuses the privately owned key/value
arrays when the existing capacity rule permits it. Growth allocates two output
arrays. The old method allocated two duplicate-collapse arrays plus two output
arrays on every nonempty batch. Public names and descriptors are unchanged.

The text pass describes `M3LongTreeSet` as paged sorted primitive arrays with
flat directories and distinguishes the new backward merge from the unchanged
Synexia donor. It preserves the existing `PENDING` TreeSet disposition and all
other family states. Indexed AVL and other specialized owners remain available.

## Exact source changes

Receiver source baseline: `d814e68f98715ebf71743c00b82f5a9a61223af0`.
Canonical engine/host baseline: `a8ca59e6dd2764e6254ea3a5752e69fd638306f2`.

| Receiver path | SHA-256 before | SHA-256 after |
| --- | --- | --- |
| `m3/collections/src/main/java/com/m3/collections/M3LongLongTreeMap.java` | `2673330335f4ea415b49a295d9cde4988c27280b990ab33984a195ca032d5791` | `ba5d4873dc52bb55c571e147ea20648410adc3f38bef84d51789c809f4972298` |
| `m3/collections/JDK_FAMILY_MAP.tsv` | `88ed35987b8956046ac8644ee8200d2aadea7ad9dd7a3175989f1e2043b9b83b` | `6d6b535e84832b85ac261cb07207f168c4fc3f4941ea78304cfce33322232adf` |

The target default branch advanced during preparation to
`a4415af418a9b0f21a06cd5801d238577fbb6eb7`. The two preimages, three directly
compiled supporting interfaces and unchanged JUnit test were re-read at that
revision and matched the frozen sources exactly. Both new receiver packet
directories were absent there. The research catalogue intentionally retains its
original observation revision rather than silently revising historical findings.

## Actual verification

| Gate | Observed result |
| --- | --- |
| Source identities, template manifests, whitespace and XML/JSON checks | Passed before compilation |
| Canonical Java and text host compilation | Java 21, `-Xlint:all -Werror`, passed |
| Java SDK recipe | 5 tests, 0 failures/errors/skips |
| Text SDK recipe | 5 tests, 0 failures/errors/skips |
| SDK execution/refusal | Exact preimage replacement, descriptor execution, fixed point, input reversal, unrelated-source preservation, missing/duplicate/drift refusal and post-scan drift refusal passed |
| Actual receiver baseline and generated candidate compilation | Java 21, `-Xlint:all -Werror`, passed |
| Existing immutable `M3LongLongTreeMapTest` | 3 tests passed on each version; includes the existing 50,000-step differential churn case |
| Additional independent oracle | 17,395 batch calls per version, including 16,128 exhaustive cases and 5,000 mixed-operation steps; 407 rejected-input calls and 5 callback cases |
| Candidate lane-identity checks | 16,774 reuse cases and 621 growth cases passed |
| Old implementation regression | Expected failure at the spare-capacity lane-reuse assertion; preserved as negative evidence |
| Public/protected API | Exact `javap -protected -s` match, SHA-256 `efca5da11e54ba139f25110cee6f53bb60c3d9cbcd767becf7b8ec119b22b5c0` |
| Receiver materialization | Exact bytes from actual SDK Results, followed by SHA-256 and diff/whitespace verification |

The independent oracle uses sequential `java.util.TreeMap.put` for mapping
results. It checks exact old capacity behavior, duplicate final-value wins,
long extrema, same-array source parameters, unchanged inputs, validation
atomicity, structural epochs, fail-fast iterators and callback behavior. The
semantic baseline passed before the candidate was evaluated.

The existing recipe harness and POM are exact copies of the current canonical
collection-refinement host, not a new recipe engine. `evidence/events.jsonl`
contains the actual command arrays and ordered outcomes; `evidence/sdk-results`
retains the actual SDK patches, input pins, refusals and generated outputs.

## Measured allocation

The separate resource probe used Temurin OpenJDK `21.0.8+9`, a supported
`com.sun.management.ThreadMXBean` allocation counter, 512 warmups and 1,024
measured calls. Each call began with a fresh map containing 1,024 even keys,
capacity 4,096, and a sorted batch of 512 new odd keys. Map construction and
batch preparation were outside the measured interval.

| Measurement | Baseline | Candidate |
| --- | ---: | ---: |
| Allocated bytes per measured call, minimum | 73,792 | 0 |
| Allocated bytes per measured call, maximum | 73,792 | 0 |
| Allocated bytes across 1,024 calls | 75,563,008 | 0 |
| Final size | 1,536 | 1,536 |
| Capacity | 4,096 | 4,096 |

This measures temporary allocation for one sufficient-capacity workload. It
does not establish throughput, latency, retained-heap improvement, every growth
case, or superiority over all tree implementations. Batch complexity remains
`O(n + b)`; a single insertion into packed sorted lanes can still move `O(n)`
elements. The retained key/value representation was already array-based.

## Compatibility details and limits

The preserved batch capacity is
`max(oldCapacity, oldSize + distinctIncomingKeys)`, even for replacement-only
batches. Union size is used for the backward write position, not capacity.
Structural modification state changes once only when a new key is introduced.
Complete sortedness validation precedes mutation, and both growth allocations
remain local until successful completion.

The widened capacity sum explicitly rejects an integer-overflow request before
mutation. That is a documented extreme-capacity hardening change. Representable
array lengths remain subject to the VM. Near-two-billion-element behavior was
not runtime tested; identical historical overflow or OOM timing is not claimed.
Removing allocations necessarily changes resource-exhaustion timing.

The original canonical map installer is still in open Synexia PR 12328, while
its receiver PR 1322 was merged. The new named recipe admits the exact current
receiver source, preserves the pending original installer and uses the current
engines. It does not overwrite or modify the unchanged donor implementation.

## Environment and unexecuted gates

The standalone host used Maven 3.9.9, OpenRewrite 8.17.1 and JUnit 5.10.2. The
existing receiver test source ran through JUnit Platform Console Standalone
1.10.2 against the exact compiled baseline/candidate classes. This bounded test
run does not substitute for the collection module's source-built
`5.10.2-m3-source-1` dependency or a full module/reactor test.

Dependency preparation initially failed because the downloaded JDK lacked the
workspace proxy configuration/trust and because Maven's offline prefetch omitted
the dynamic Surefire provider. The failed logs are retained. Configuration of
the existing proxy and system trust store, followed by explicit pinned runner
prefetch, resolved those environmental failures. All reported host compilation
and successful SDK runs then executed offline; TLS checks were retained.

Full Synexia/M3 reactors, all-provider compatibility suites, JDK tier suites,
installed-image acceptance, concurrent-container guarantees and global receiving
phase completion were **NOT_RUN**. No JNI/native source changed. Review PRs remain
draft under the repository's recipe-first admission policy. No runtime phase or
global default-factory promotion is claimed.

The accompanying research packet identifies 15 official challenge patterns and
51 selected current M3 owners/helpers/scaffolds, covering all 21 exact owner
names in the existing 104-row provider table. It is not a complete audit or
conversion of every fork. No challenge solution/editorial source was copied.
