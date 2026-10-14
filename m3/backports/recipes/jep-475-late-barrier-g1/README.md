# JEP 475 — Late Barrier Expansion for G1

Status: exact OpenJDK integration/GA inventory and recipe-generation evidence only. No product
mutation, Java-21 compatibility, runtime-equivalence or promotion claim.

## Upstream authority

- JEP: 475
- implementation JBS: 8334060
- exact OpenJDK implementation commit:
  `0b467e902d591ae9feeec1669918d1588987cd1c`
- commit date: 2024-10-03
- exact implementation denominator: **58 paths**
- final donor state for candidate recipe generation: `jdk-24+36`
- Java-21 baseline: `jdk-21+35`

The 58 paths in `INTEGRATION_PATHS.txt` are the exact files changed by JDK-8334060. They are not
inferred from G1 or C2 directory names.

## Why final-GA postimages are used

The implementation commit defines feature scope, while JDK 24 GA contains follow-up fixes. The
selected-path inventory therefore joins each exact implementation path against both the integration
commit and `jdk-24+36`. Removed paths are held explicitly; no deletion/reintroduction is inferred.

## Mechanical FILE lane

After exact-path classification, CI invokes the existing generator with:

```text
--crate-size 1
--include-native
--include-text
```

Java files become structured hash-pinned Java OpenRewrite atoms. C/C++/headers, AD files, m4/make
inputs and other strict UTF-8 files become source-sealed text/native atoms. Every physical source
edit remains a FILE atom.

The feature itself is not FILE scope. JEP 475 changes G1's C2 barrier model, ADLC-generated machine
rules and architecture-specific assemblers across a multi-module HotSpot/compiler surface. Feature
composition is therefore MULTI_MODULE and stays held until explicit dependency review.

## Required dependency proof before materialization

At minimum the packet must close:

1. shared `BarrierSetC2` / `CardTableBarrierSetC2` deltas against Java 21;
2. C2 matcher, scheduling, oop-map and macro-expansion dependencies;
3. ADLC generation changes;
4. x86, AArch64, ARM, PPC, RISC-V and s390 implementation disposition;
5. IR framework/test-library dependencies;
6. G1 legacy behavior and collector-selection compatibility.

Only then may an ephemeral candidate be composed from the FILE atoms.

## Required verification

```text
exact 58-path inventory
 -> JDK24 GA selected-path classification
 -> one-file recipe crates
 -> dependency/architecture review
 -> source-sealed candidate
 -> fastdebug G1+C2 build
 -> TestG1BarrierGeneration + C2/IR/safepoint regressions
 -> legacy G1 regressions
 -> fixed point
 -> serial promotion
```

Until those gates close, JEP 475 remains `candidate-high-risk` / `NOT_AUTHORIZED`.
