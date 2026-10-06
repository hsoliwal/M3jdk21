# JDK-internal M3 package boundary

Status: M3JDK architectural invariant.

## Form factor

M3JDK follows the JDK internal-package model rather than reproducing Synexia package names.

| Responsibility | M3JDK home | Rule |
| --- | --- | --- |
| VM-special String value/layout | `java.lang.M3String`, owner/atom/tuple/fixed facts | Package-private implementation coupled to `java.lang.String` and HotSpot |
| Reusable backing contracts | `jdk.internal.mindex` | Non-exported `java.base` internal API |
| Reusable regex/search precompute kernels | `jdk.internal.mindex` | JDK-owned names; no Synexia runtime dependency |
| Representation-coupled bounded String caches | package-private `java.lang.M3String*Precompute` until decoupled | Internal only; migration to `jdk.internal.mindex` must preserve exact owner+coordinate keys |
| Synexia knowledge/reasoning precompute | no String counterpart | Do not port without a concrete non-String JDK consumer |

The package `jdk.internal.mindex` is deliberately **not exported** by
`java.base/module-info.java`. A public Java modifier inside this package only permits use by
other JDK implementation code; it does not make the type Java SE API.

## Canonical identity

M3JDK String precompute keys the existing M3 String identity:

```text
java.lang.String
    -> package-private java.lang.M3String
         -> canonical owner + packed range coordinate
              -> fixed facts / bounded operation precompute
```

Reusable kernels in `jdk.internal.mindex` consume JDK-owned contracts or stable
`CharSequence`/primitive inputs. They do not own canonical spelling.

Synexia's language-local word-ID planes have a different scope. If a future JDK subsystem admits a
language dictionary, its precompute must share that subsystem's one canonical coordinate generation;
it must not create separate RE2/J-, Lucene-, or reasoning-owned vocabularies.

## Domain versus implementation provenance

M3JDK does not attribute ownership of abstract domains to external projects. Language, reasoning,
logic, search, graphs, automata, argumentation and mathematical semantics are general domains.

Provenance/licensing records bind **specific implementation artifacts** only: a pinned repository
revision, source/blob, generated table, encoding, serialization format, test vector, or concrete
solver/algorithm realization. M3JDK may independently implement the same abstract semantics under
JDK-owned internal names when the implementation is independently written and the required
behavioral proof passes.

This distinction is mandatory in mapping documents and code comments.

## Donor mapping

Synexia is the donor/convergence workspace. M3JDK is the target/runtime owner.

- Synexia `MIndexRegexTrigramQuery` -> JDK-internal `M3TQ`.
- Synexia String-search/precompute ideas -> JDK-owned fixed facts or bounded internal search plans.
- Ideas learned from specific reviewed RE2/J or Lucene implementation artifacts enter only through qualified Synexia provenance and are
  independently adapted to a concrete JDK consumer.
- Tweety-style knowledge/reasoning has no `java.lang.String` counterpart.

No `com.synexia.*` import or runtime dependency is permitted in `java.base`.

## Documentation rule

Every new reusable M3 internal package or substantial internal kernel must document:

1. semantic authority and fallback;
2. identity/key domain;
3. retained-memory/budget ownership;
4. donor/provenance mapping;
5. public-API exclusion;
6. VM/JNI materialization boundary when applicable.

The machine-readable companion is `m3/docs/m3-runtime-invariants.tsv`.
