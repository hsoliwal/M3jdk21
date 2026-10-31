# Full-borrow Phase 1 — MIndexString to M3String

Status: qualification packet stacked on the full Synexia borrow-estate receiver.

## Source authority

Canonical donor/convergence workspace: `hsoliwal/com.synexia`.

Pinned estate revision:
`296323958b1019edd59b60b9c05cb148d024cfe5`

First-party Synexia authored source/recipes/tests/docs/manifests are copyrighted work and remain
Apache-2.0 where the source estate classifies them as first-party Apache work. The controlling
notice is:

`Copyright 2026 Hitesh Soliwal and contributors`

Abstract ideas, algorithms, behaviors and architectural concepts are recorded as provenance and are
not mislabeled as copyrighted source expression. OpenJDK and third-party donor bodies retain their
actual copyright/license/NOTICE.

## Target authority

M3JDK21 remains the JDK product/runtime owner.

Public `java.lang.String` remains `String`. The internal target representation is the existing
`java.lang.M3String` family. No Synexia runtime or Maven dependency becomes a java.base runtime
dependency.

The canonical target invariant is:

```text
M3String = canonical owner + packed UTF-16 coordinate
```

Scalar text belongs to canonical atom/mapped owners. Concatenation belongs to persistent tuple
owners. Slices remain owner/range coordinates. Contiguous byte[]/char[] storage is materialized only
at explicit compatibility boundaries such as JNI/encoding.

## Mechanical subcontracts

Phase 1 is not one giant source copy. It is decomposed into independently provable relationships:

1. VALUE_OWNER — MIndexString / MIndexStringPool -> M3String owner/atom/pool/tuple family.
2. RANGE_VIEW — SubMIndexString -> M3String owner+range coordinate; no retained slice payload.
3. FACTS — MIndexString metadata -> M3StringFacts and owner-local range facts.
4. SEARCH_PRECOMPUTE — MIndexString precompute/search -> M3StringSearchPrecompute,
   M3StringPositionPrecompute, M3StringCodePointPrecompute and M3TQ.
5. BRIDGE_SHADOW — MIndexString bridge/shadow operations -> target-owned M3String/String JNI
   compatibility shadows only.

Each relationship is candidate evidence until the target gate is executed.

## Required gates

- source revision/path/blob and Apache-2.0/provenance verification;
- target preimage Git-blob verification;
- exact UTF-16 behavior including unpaired surrogates;
- owner/coordinate lifetime and no-second-payload checks;
- concat/slice/hash/equality/search differential tests;
- java.util.regex remains semantic authority for regex;
- JNI acquire/materialize/release and shadow lifetime proof;
- matched JDK build and jtreg under normal/interpreter/C1/C2;
- GC/CDS/JNI platform checks where the representation crosses those boundaries;
- cold/warm CPU, heap and native retained-memory measurements;
- fixed-point recipe replay and target-tree readback.

No row in this packet grants automatic mutation, behavioral equivalence or promotion authority.
