# M3 String ownership and Synexia donor boundary

Status: **canonical architecture invariant** for M3JDK21 work.

## 1. One sentence

> **Synexia is the donor; M3JDK21 is the runtime owner. M3 String and all of its precompute live inside M3JDK21.**

This statement is intentionally repeated in the root `AGENTS.md`, the whole-JDK architecture,
and the String replacement specification so that later source work cannot accidentally invert the
ownership boundary.

## 2. Runtime ownership

M3JDK21 owns the executable form of every accepted String capability:

- `java.lang.String` integration;
- the canonical M3 String body;
- canonical local/shared atom admission;
- range, slice, repeat and joined composition;
- mapped/shared image formats used by the JDK;
- String precompute and indexes;
- HotSpot field/layout/runtime integration;
- JNI/JVMTI/native String boundaries;
- JIT/GC/CDS/serviceability consumers;
- build, jtreg, runtime and performance qualification.

There is no runtime call from the JDK back into Synexia for these services.

## 3. Synexia's role

Synexia can provide:

- candidate algorithms;
- source implementations used as donors;
- problem/category catalogues;
- hostile/edge-case fixtures;
- OpenRewrite recipes;
- Java/JNI prototypes;
- benchmark hypotheses and measurements;
- exact source hashes/revisions;
- Apache or other applicable licence/NOTICE provenance.

Those inputs are **donor evidence**. They become M3JDK21 product behavior only after target-side
adaptation and target-side verification.

A successful Synexia test does not establish JDK acceptance, and a Synexia class/module does not
become a JDK runtime dependency merely because its algorithm was selected.

## 4. M3 String naming and current legacy source names

The canonical term is **M3 String**.

The current tree contains legacy implementation identifiers including `MIndexString`,
`MIndexStringPool`, and mapped-backing names. Treat them as the current implementation lineage
of M3 String, not as a separate architecture.

Do not create `M3String` beside `MIndexString` as a parallel owner.

If the source symbols are renamed, do it as one recipe-controlled cross-layer migration covering:

- `java.lang.String` fields, constructors and operation bridges;
- M3 String implementation and pool;
- mapped/shared backing;
- HotSpot `java_lang_*` field lookup/layout integration;
- JNI/native entry points and generated headers where applicable;
- jtreg/unit/runtime tests;
- recipes, source hashes, mapping IDs and evidence.

Until that complete migration is admitted, documentation may say **M3 String** while exact source
references retain their current class names.

## 5. Canonical data path

```text
Synexia donor source/evidence
          |
          | source-pinned adaptation
          v
+---------------------- M3JDK21 ----------------------+
|                                                       |
| java.lang.String                                      |
|        |                                              |
|        v                                              |
|     M3 String                                         |
|        |                                              |
|        +-- canonical local atoms                      |
|        +-- M3JDK21 shared/mapped atoms                |
|        +-- ranges / slices / joins                    |
|        +-- internal precompute                        |
|        |      +-- hashes                              |
|        |      +-- encoding/code-point facts           |
|        |      +-- search / regex candidate facts      |
|        |      +-- tuple/trigram/index facts           |
|        |      +-- rank/select/count/index lanes       |
|        |                                              |
|        +-- exact Java/HotSpot/JNI/JIT/GC consumers    |
|                                                       |
+-------------------------------------------------------+
          |
          +--> char[] / byte[] only when the contract requires materialization
```

## 6. Precompute ownership

Precompute is an internal M3JDK21 optimization layer over immutable canonical content. It is not a
service imported from Synexia and not another payload owner.

Every retained fact must declare:

- canonical owner/generation;
- logical range or composition identity;
- exact operation/semantic domain;
- preparation cost;
- memory/retention budget;
- invalidation/lifetime rule;
- concurrency/publication rule;
- exact fallback/verifier.

Candidate-filter facts can reject work only when rejection is sound. Exact String and regex
semantics remain authoritative.

For joins, precompute must account for composition seams. A fact valid for two independent atoms
is not automatically valid for their concatenation without the seam state.

## 7. char[] and byte[] rule

M3 String supplies `char[]` and `byte[]` behavior required by ordinary JDK APIs, but those arrays
are compatibility projections.

- `String.toCharArray()` must return the required caller-owned mutable array.
- `String.getBytes(...)` must produce bytes with exact charset semantics.
- constructors/JNI ingress may canonicalize admitted input into M3 String.
- internal operations should traverse M3 String directly when they do not semantically require a
  contiguous Java array.

Do not expose mutable arrays as canonical shared storage.

## 8. Anti-patterns

Reject changes that:

- make `java.base` depend on a Synexia runtime module;
- put M3 String precompute back into `com.synexia` as the live owner;
- create a second String/interner/spelling store beside M3 String;
- flatten every range/join before operations that can traverse the composition;
- treat hashes, ranks, trigrams or regex effects as exact truth without the required verifier;
- rename only the Java class while leaving HotSpot/JNI/recipe/mapping references stale;
- call a donor benchmark or donor unit test a full M3JDK21 product qualification.

## 9. Review test

A future change should be rejected if the answer to either question is wrong:

1. **Who owns the running implementation?** — M3JDK21.
2. **Where do M3 String precompute facts live?** — inside M3JDK21.

Synexia remains the donor and provenance source.
