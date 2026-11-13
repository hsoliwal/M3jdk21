<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3JDK21: machine-maintained mechanical superset contract

**Invariant ID:** `M3JDK-MACHINE-MAINTAINED-MECHANICAL-SUPERSET-1`  
**Contract version:** `M3-ATOM-COVERAGE/1`  
**Compatibility floor:** Java/JDK 21  
**Status:** normative architecture plus an initial executable ledger verifier; repository-wide coverage remains open.

## 1. Meaning of M3

This contract defines **M³** operationally as **Machine-Maintained Mechanical**. M3JDK21 is one integrated JDK 21
source, binary and behavioral compatibility superset. It is not a loose collection of renamed libraries and not an
M3 runtime layered over an otherwise independent JDK. Standard Java/JDK APIs, JVM behavior, class-file contracts,
JNI ABI and external observable behavior are locked unless a separately reviewed change explicitly unlocks them.

“Machine-maintained” does not mean “machine-trusted.” Machines may inventory, atomize, classify, generate candidates,
apply qualified recipes and assemble evidence. They may not infer equivalence, silently widen scope or convert absent
evidence into success. Compiler, tests, runtime, ABI checks and retained receipts arbitrate admission.

## 2. Repository-complete invariant

Every tracked file in the admitted scope has a `FILE_CONTAINER` row. Every semantic unit inside that file that can
independently affect compilation, runtime behavior, ABI, packaging or generated output has its own atom row. Covered
families include:

- Java methods, constructors, initializers, lambdas, field initializers, native declarations and declarations;
- JNI exports, C/C++ functions, HotSpot architecture-description rules and assembly routines;
- build rules, scripts, generated-resource declarations and non-code files that affect the product.

Comments and formatting are not split into artificial atoms unless a tool contract makes them semantic. Atomization
must decompose to meaningful contract-bearing leaves and preserve the file, owner, package, module and product
composition above them.

Each atom has one stable ID, source path and SHA-256, kind, owner, semantic signature, closed pattern ID, contract
reference and public-contract lock. The stable ID identifies the capability; it does not have to encode a path, so a
reviewed move can preserve identity while changing the source binding.

## 3. Authoritative ledger and optional annotations

`ATOMS.tsv` and `EVIDENCE.tsv` are the authoritative machine contract. Source-retention `@M3AtomSpec` annotations and
`M3AtomDescriptor`/`M3AtomEvidence` interfaces are optional precise anchors for code that can carry them. They are not
the global truth because Java annotations cannot cover native code, initializer blocks, lambdas, build rules,
resources or unmodified upstream files.

The ledgers are exact-header, strictly sorted, duplicate-free and hash-bound. Missing rows, unknown pattern classes,
unbound contracts and incomplete proof profiles are visible states. The validator may report them; the admission mode
must refuse them.

## 4. Per-atom proof profile

Every atom requires the common gates:

```text
INVENTORIED
SOURCE_IDENTITY
PATTERN_BOUND
CONTRACT_BOUND
DOCUMENTED
PROVENANCE_VERIFIED
LICENSE_VERIFIED
PUBLIC_SURFACE_VERIFIED
CORRECTNESS_TESTED
TEST_COVERAGE_MEASURED
FIXED_POINT_VERIFIED
```

Kind-specific gates are additive. An atom with `public_contract_locked=false` additionally requires a
hash-bound `CONTRACT_CHANGE_AUTHORIZED` gate; changing the boolean alone never unlocks a contract:

- Java: `JAVA_COMPILED`, `JVM_RUNTIME_TESTED`; executable Java also requires `JIT_TESTED` and
  `PERFORMANCE_MEASURED`.
- Native/JNI/HotSpot/assembly: `NATIVE_COMPILED`, `ABI_VERIFIED`, `NATIVE_RUNTIME_TESTED`; executable native atoms
  also require `PERFORMANCE_MEASURED`.
- Build and script atoms: `TOOL_RUNTIME_TESTED` and `PERFORMANCE_MEASURED`.

A gate state is exactly `PASS`, `FAIL`, `NOT_RUN`, `BLOCKED` or `NOT_APPLICABLE`. Only `PASS` and a hash-bound,
rationalized `NOT_APPLICABLE` can satisfy admission. `NOT_RUN` and `BLOCKED` are useful honest states, never implicit
passes. Performance evidence is workload- and environment-scoped; it does not authorize a universal speed claim.

## 5. Ordered mechanical pipeline

The repository pipeline is deliberately serial at each changed atom and deterministic across independent atoms:

```text
1. inventory every tracked source family and seal file identity
2. atomize without changing behavior
3. execute baseline interpreter/C1/C2, native and contract characterization where applicable
4. patternize against the closed catalogue; retain UNKNOWN/RESIDUE as blockers
5. bind owner, interface, annotation metadata, contract and documentation
6. retrieve or improve the canonical Synexia recipe; preserve donor licence and provenance
7. materialize one bounded postimage from exact preimages
8. diff -> lint -> compile -> targeted tests -> JIT/native/ABI/runtime -> performance
9. prove deterministic replay/fixed point and regenerate the coverage ledger
10. receive exact postimages and passive receipts in M3JDK21; admit only complete atoms
```

Pattern similarity is a candidate-discovery mechanism, not replacement authority. An atom may be substituted only
when its full contract and proof profile remain satisfied. Public interfaces and signatures are the semantic skeleton;
inline documentation and ledger references are the semantic memory.

## 6. Custody and donor use

Synexia is the canonical recipe, pattern, correction-memory and convergence workspace. Public M3JDK21 receives
executed source/test/build postimages, applicable licence/NOTICE material, and data-only provenance and target
acceptance receipts. This contract does not authorize recipe or improvement-memory copies in the public product.

OpenJDK-family patches, Apache Harmony, OpenJ9/OMR, GraalVM, ART, research JVMs, libraries and algorithm catalogues may
nominate candidates. Ideas and algorithms may be independently implemented; copied expression remains governed by its
licence. Noncommercial or public-fork status does not remove attribution, notice, source, patent or compatibility
obligations.

## 7. Coverage ratchet

The first complete census is allowed to be incomplete in proof. It must not be incomplete in representation: every
known gap receives an explicit state. Thereafter:

- an atom cannot disappear without a hash-bound move, merge, split, retirement or supersession record;
- a `PASS` cannot silently regress to absence or `NOT_RUN`;
- a pattern, owner, contract or source-hash change invalidates affected evidence until replayed;
- aggregate percentages never substitute for atom rows;
- branch ancestry, a green finite corpus, source similarity and a queued workflow do not establish product admission.

Coverage is recomputed bottom-up: atom -> file -> package -> module -> product. A higher-level PASS is valid only when
its required leaves are admitted or carry explicit reviewed dispositions.

## 8. Initial executable leaf

The initial implementation under `m3/tooling/atom-census` provides:

- a closed atom-kind and proof-gate vocabulary;
- source-only `@M3AtomSpec` plus descriptor/evidence interfaces;
- strict `ATOMS.tsv` and `EVIDENCE.tsv` parsers;
- deterministic semantic SHA-256 over both ledgers;
- `validate`, which preserves and reports incomplete states;
- `admit`, which refuses unknown patterns, missing gates, failures, blocked or unexecuted proof;
- dependency-free Java 21 refusal/admission tests and Maven/JUnit bindings;
- machine-readable schema, status and kind-profile tables checked against the implementation.

This leaf does not claim that M3JDK21 has repository-wide atom coverage. It creates the fail-closed contract under
which that coverage can be accumulated mechanically.

## 9. Current bounded evidence and next leaves

At the exact candidate revisions on 31 October 2026:

- M3JDK21 PR #2240 inventories static methods with bodies in selected Java roots and patternizes them. It is a useful
  catalogue leaf, not an every-file/every-atom census.
- Synexia PR #14999 records broader structural atomization of M3JDK and vendored donors plus a name-based per-atom test
  work list. Those counts are candidate evidence, not target admission.
- Synexia PR #15018 establishes the all-source `ATOMIZE -> JIT TEST -> PATTERNIZE -> CONTRACT/PRODUCT TEST` order and
  keeps full native/product execution on HOLD.

The next mechanical leaves are: import exact source identities from the full-source census; emit one `FILE_CONTAINER`
plus language-specific atoms for every tracked file; join existing test/performance evidence by stable atom ID; generate
all missing-gate rows as explicit `NOT_RUN`; then close gaps family by family without changing public contracts.
