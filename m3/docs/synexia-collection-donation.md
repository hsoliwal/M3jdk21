<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Synexia collection donation into M3JDK21

This packet applies qualified Synexia collections in the existing `m3/collections`
module and `com.m3.collections` package. `M3Collections` keeps its sparse lane and
stable-handle factories and adds the complete donor Java-collection factory surface.

The executable map is [`synexia-donation.json`](../collections/synexia-donation.json).
It binds every source path, original blob/SHA-256, corrected source hash, target
type/path/hash and canonical recipe. Existing String, file, AST, data-structure and
precompute maps remain in [`name-mapping.json`](name-mapping.json).

| Contract | M3 owner / factory |
| --- | --- |
| Primitive long list / ring deque | `M3LongArrayList`, `M3LongArrayDeque` |
| Ordered hash map / set | `M3LinkedHashMap`, `M3LinkedHashSet` |
| Sorted navigable map / set | `M3TreeMap`, `M3TreeSet` |
| List and deque with slot links | `M3LinkedList` |
| Weak-key map | `M3WeakHashMap` |
| Blocking deque / transfer rendezvous | `M3BlockingDeque`, `M3TransferQueue` |
| Lock-based concurrent maps and sets | `M3LockedMap`, `M3LockedTreeMap`, `M3LockedTreeSet` |
| Already dense JDK arrays, rings, heaps, enums and copy-on-write families | `M3Collections` returns the existing JDK implementation |

The M3 names identify concrete contracts. The ordered hash backend remains
`M3LinkedHashMap`; `hashMap()` and `linkedHashMap()` share it. No extra collection
hierarchy or naming-only subclass is needed to reuse an already dense JDK class.
Locked APIs explicitly block; they do not claim nonblocking progress.

## Correctness and resources

The donor correction recipe retains regressions for late-binding spliterators,
event callback failure, and callback-driven slot reuse. Semantic package/type
changes use the real OpenRewrite SDK, then the exact Java snapshot recipe applies
target outputs with absent-before/unchanged-owner/preimage checks. Replay, drift,
missing-input and protected-existing-source checks remain separate gates.

Run all installed module contracts, packaging and checked JNI acceptance with:

```sh
JAVA_HOME=/path/to/jdk-21 JUNIT_CONSOLE_JAR=/path/to/junit-console.jar \
  python3 m3/collections/verify_donation.py --cost-probe
```

The earlier `verify.py` remains the sealed primitive-lane installer. The new verifier
checks the exact installed source manifest, compiles the named module and all tests,
discovers every JUnit contract, checks packaged attribution, and runs existing native
parity and prepared-allocation probes. Its source is canonical recipe-owned verification
material; it does not copy a generic transformation engine into the receiver.
`M3CollectionCostProbe` records cold construction/admission allocation separately
from warm access/traversal, with identical paired workloads and VM information.
It is diagnostic evidence, not a universal speed ranking; thread allocation is not
retained heap or native process memory. The qualification/application receipts are
under `m3/collections/qualification/`.

## Full donor continuation

The existing [porting invariant](m3jdk21-porting-invariant.md) remains authoritative:
String -> arrays -> collections -> AST/compiler -> remaining families. This
separately built module does not advance JDK backend/runtime promotion. MIndexString,
canonical String/regex precompute, array VM/JNI contracts, MIndexAST/compiler,
files, database/storage, generic precompute and non-prefix dependencies retain their
existing owner maps and pending target gates. The 4,770-path historical catalogue is
preserved and is not relabeled complete by this bounded collection packet.

Synexia is the donor-convergence and reusable recipe owner; M3JDK21 is the polished
product target. Eligible Synexia implementation, recipes, tests and documentation
retain Apache-2.0 and their copyright/provenance. A recipe does not relicense its
input or OpenJDK-derived output. Runtime source has no Synexia package dependency.
