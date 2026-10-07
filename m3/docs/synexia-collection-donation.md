<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Synexia collection donation and primitive reconciliation

Synexia is the canonical donor/convergence and recipe home. M3jdk21 receives
qualified code into its existing `com.m3.collections` package and owns product
integration and acceptance. Eligible first-party code, recipes, tests, fixtures,
and evidence remain reusable under Apache-2.0 with copyright and provenance.
OpenJDK and third-party files retain their own applicable rights and notices.

## Active combined module

The combined source set contains 65 main Java files and 19 test/probe source files.
It has one `M3Collections` factory with 58 public factory methods. Its JAR requires
only `java.base`; there is no Synexia runtime dependency. The module-owned mapping
is `m3/collections/name-mapping.json`; the prior global MIndex/String/AST mapping
remains byte-identical so unrelated frozen migration guards remain meaningful.

| Family | Receiving owner |
| --- | --- |
| Ordered object map / set | `M3LinkedHashMap`, `M3LinkedHashSet` |
| Sorted object map / set | `M3TreeMap`, `M3TreeSet` |
| Linked object list | `M3LinkedList` |
| Weak-key map | `M3WeakHashMap` |
| Blocking deque / transfer queue | `M3BlockingDeque`, `M3TransferQueue` |
| Primitive list / deque | `M3LongArrayList`, `M3LongArrayDeque` |
| Primitive hash set / map / heap | `M3LongHashSet`, `M3LongLongHashMap`, `M3LongPriorityQueue` |
| Lazy state, events, progress | Existing donated `M3Lazy*` and `M3Progress*` owners |

Current primitive hash/set/heap implementations and their tests are retained
byte-for-byte. The list/deque keep current fail-fast iteration and add the donor's
sequence, stack, filtering, and indexed traversal operations to the same owners.
JDK implementations remain available from the factory for dense arrays, identity,
enum, bitset, copy-on-write, and other families where those contracts fit.

## Receiver interface decision

The current target already provides `boolean add(long)` and `boolean addAll(long[])`.
Those signatures are retained, and the newly received `M3LongSequence` uses them.
The intermediate donation commit e567cd9 used void returns in its list/interface;
the two operations therefore account for four descriptor differences from that
intermediate commit. Binary compatibility with those intermediate declarations is
not claimed. Synexia's own `PackedLongList` and `LongSequence` APIs remain unchanged.
The recorded API comparison verifies the actual earlier target baselines separately.

## Qualification and evidence

The combined contract suite contains 99 tests: the prior 85, seven current primitive
collection tests, and seven new integration tests. Strict Java 21 compilation,
real OpenRewrite SDK replay/fixed-point/refusal proof, both standalone lazy tests,
license/provenance packaging, checked JNI, native UBSan, prepared allocation, and
paired diagnostic cost results are recorded in the canonical successor packet and
`m3/collections/qualification/collection-convergence-20261007.json`.

Use `python3 m3/collections/verify_donation.py --cost-probe` with Java 21 and
`JUNIT_CONSOLE_JAR` to verify the installed module. The dedicated workflow pins
Temurin 21.0.12.1+1 and JUnit Console 1.12.2 by SHA-256. Its second, independent job
runs the historical lane recipe with the original Maven/OpenRewrite versions.
That legacy installer is immutable and is not used to rematerialize the expanded
live factory. The broad reactor/jtreg/HotSpot gates are separate.

The cost probe reports thread-allocated construction bytes and warmed operation
measurements. It does not measure retained heap or prove one representation fastest
for every workload. Choose by density, order, mutation, concurrency, and required
contract, using the recorded paired measurements.

## History and promotion boundary

The first source and receiver PRs were recorded as merged by ancestry-only merge
commits that omitted their changed content. The successor restores the omitted
recipe/source work while preserving concurrent changes. Acceptance requires actual
destination file blobs and modes to match the qualified output, not only a merged
PR flag or commit ancestry.

The original 20261007 donation receipts remain immutable historical evidence for
the earlier 79-Java source set. The active source manifest is
`qualification/installed-source-converged-20261007.tsv`, and the active application
receipt is `qualification/synexia-convergence-application-20261007.json`.

The receiving order remains String → arrays → collections → AST/compiler → other
families. This independently built Apache collection module does not promote a
java.base/HotSpot replacement or complete the historical 4,770-path MIndex catalogue.
