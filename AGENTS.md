# M3JDK21 agent invariants

## Product ownership boundary — non-negotiable

**M3JDK21 is the product/runtime owner. Synexia is a donor.**

For any capability transferred from `hsoliwal/com.synexia` into this repository:

- Synexia supplies qualified source ideas, algorithms, tests, recipes, provenance, licences, and evidence.
- M3JDK21 owns the adapted Java, HotSpot, JNI/native, mapped-image, build, test, and runtime implementation.
- `java.base`, HotSpot, JNI, and other JDK runtime code must not acquire a runtime dependency on Synexia modules, Maven artifacts, package names, service graphs, databases, or application frameworks.
- A donor class name is not a target architecture. Reuse behavior and evidence; adapt it into the existing M3JDK21 owner.
- Source lineage remains pinned to the exact Synexia revision/license, but execution authority belongs to the M3JDK21 implementation and its JDK verification gates.

This boundary overrides any source-side wording that could otherwise be read as making Synexia the runtime owner of a capability after it has been ported into M3JDK21.

## M3 String is the canonical String owner

The canonical architectural term is **M3 String**.

Current source may still contain legacy implementation names such as `java.lang.MIndexString`,
`MIndexStringPool`, or `jdk.internal.mindex.MIndex*StringBacking`. Those names are existing
implementation/lineage identifiers; they do **not** authorize a second String architecture.

The invariant is:

```text
java.lang.String
        |
        v
     M3 String
        |
        +-- canonical atoms / ranges / joins
        +-- VM-local canonical overlay
        +-- M3JDK21-owned mapped/shared images
        +-- M3JDK21-owned precomputed facts
        |
        +-- char[] / byte[] materialization only at required compatibility boundaries
```

Do not introduce another String wrapper, another canonical spelling store, or a Synexia-backed
runtime String service.

A source rename from a legacy `MIndexString*` implementation name to `M3String*` is a
cross-layer migration, not a cosmetic edit. It must update all Java, HotSpot, JNI/native, build,
test, recipe, mapping, and evidence references together, preserving exact JDK contracts.

## All M3 String precompute is internal to M3JDK21

All precomputation used by M3 String is owned and executed inside M3JDK21.

This includes, where admitted by exact semantics and bounded memory:

- String-compatible hash and structural hash;
- logical UTF-16 length, coder/encoding facts and code-point facts;
- first/last code-unit and prefix/suffix facts;
- atom/range/join geometry and seam facts;
- literal-search tables and sound candidate filters;
- regex candidate/rejection facts while the exact regex engine remains semantic authority;
- encoding-size/projection facts;
- tuple/trigram/N-gram facts such as M3TQ when retained by the JDK;
- mapped-image facts and validated directories;
- primitive rank/select/count/index lanes used by JDK-owned precompute;
- JIT/JNI/native acceleration metadata that belongs to the matched JDK implementation.

A Synexia precompute module may be a **donor** for an algorithm or proof corpus. It must not become
the runtime precompute owner for M3 String, and M3JDK21 must not call back into Synexia to obtain
String facts.

Precompute is derived state, not a second text identity. Bind every fact to the exact immutable
owner/generation/range/composition and retain an exact fallback. Hash/filter agreement alone is not
proof of String equality or regex truth.

## Arrays are compatibility boundaries, not canonical ownership

`char[]` and `byte[]` remain required Java/JNI compatibility forms.

- M3 String provides their content when an API requires an actual array.
- Array construction/import may be admitted into M3 String canonical storage.
- Internal M3 operations should stay on canonical atoms/ranges/compositions when possible instead
  of flattening through `String.value()`, `toCharArray()`, or a temporary `byte[]`.
- Mutable caller-visible arrays are never the canonical shared M3 String payload.

## Donor intake and recipe discipline

Before changing product source:

1. inventory the current M3JDK21 owner and all Java/HotSpot/JNI/native consumers;
2. identify the exact Synexia donor revision, path, licence, and behavior being reused;
3. reuse or create the narrow source-sealed M3JDK21 recipe;
4. preserve public Java/JNI/VM contracts and exact exception/null/encoding behavior;
5. prove recipe fixed point and drift/refusal behavior;
6. execute the applicable compile, jtreg/runtime, HotSpot/JNI/native, GC/JIT/CDS/serviceability,
   platform, and performance gates;
7. record what actually ran and what remains unverified.

Do not report donor tests, patch-module tests, or ancestry alone as a rebuilt-JDK acceptance result.
