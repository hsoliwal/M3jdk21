# M3 String — Synexia donor-history ledger

## Ownership invariant

**Synexia is the donor. M3JDK21 is the product/runtime owner.**

Synexia history is used to recover algorithm evolution, design intent, discarded approaches,
verification evidence and provenance. It must not become an M3JDK21 runtime dependency.

Inside M3JDK21 the canonical product name is **M3 String**. Existing
`java.lang.MIndexString` names are implementation lineage until a complete Java/HotSpot/JNI/test/
recipe rename is qualified.

## Why history matters

A current Synexia snapshot hides important convergence decisions. String precompute evolved through
several owners before settling on two distinct classes of facts:

1. **constant-size canonical facts** — retain once per canonical storage identity and compose across
   immutable atoms/ranges;
2. **length-proportional analyses** — keep separately bounded, opt-in and caller/precompute owned.

This split is the donor rule for M3JDK21. Do not collapse all precompute into one global cache.

## Relevant donor evolution

| Synexia commit | Donor step | M3JDK21 interpretation |
| --- | --- | --- |
| `2b5af408828a` | Expose search and compiled facts through the indexed-text facade. | Treat precompute as reusable execution facts, not alternate text ownership. |
| `a3663ba9e2cc` | Reuse frozen facts and immutable pair-search memoization. | Cache only immutable canonical facts/results with exact identity. |
| `7a47a8a445ca` | Attach MIndexString to shared mapped payloads and persisted unary/search facts. | M3 String must operate on canonical/mapped storage without forcing String/char[]/byte[] flattening. |
| `45895b6ef125` | Catalogue search donors; add descriptor-based prefix-Z facts. | Inventory algorithms first; keep UTF-16 exactness and explicit metadata budgets. |
| `33ebec8fa64c` | Cache mapped metrics, bit signal and character flags together per atom. | Related constant-size facts should share canonical identity and one preparation boundary. |
| `46d92ec42486` | Compile pattern prefix/reverse-prefix/skip/hash/bit-signal facts once. | Operation/pattern precompute belongs to immutable compiled plans, not each search invocation. |
| `e649343956a2` | Fail-fast rope search from precomputed bit facts. | Negative precompute may prune; positive precompute must still exact-verify canonical text. |
| `bb4033de627c` | Route facade search filters through cached String facts. | Put the filter at the public String-operation dispatch boundary before expensive canonical search. |
| `0cd5b787de63` | Publish shared-reference search and reusable thick fact ranges. | Preserve owner+range identity; slices should reuse source facts/geometry rather than materialize. |
| `6b21032aaee9` | Retain canonical search patterns and reuse operation facts. | One canonical pattern/prepared plan should serve repeated operations. |
| `28d28d2162ba` | Reuse regex literal effects, adaptive search and optional LCP range facts. | Advanced search facts stay layered and opt-in; exact matcher remains semantic authority. |
| `86d729240243` | Extend prefix-Z with borders, periods and repetition facts. | Derived length-proportional analyses remain separate from the constant-size canonical bundle. |
| `10b4894505a6` | Add exact UTF-16 Manacher palindrome facts. | Large O(n) lanes are caller/budget owned; do not stuff them into every M3 String. |
| `d6308bff7e3a` | Add exact suffix-decision DFA precompute in Java/JNI. | Precompute may prove an exact early decision, but only when the proof is mathematically complete and independently validated. |
| `a281c3e8034d` | Expose canonical prefix-Z precompute from MIndexString. | Canonical storage may expose access to bounded analyses without owning their payload lanes. |
| `b9d35ce3c3c2` | Retain canonical MIndexString fact bundles once on the canonical tuple body. | Mature donor state: constant-size facts attach to canonical identity; length-proportional search lanes remain separately bounded. |

## Current M3JDK21 consequence

The first PR #152 implementation used a 4096-slot global direct-mapped precompute cache. The Synexia
history shows that this is an earlier-style cache shape, not the mature convergence point.

PR #152 therefore follows the later donor rule:

- no global cache owns M3 String payloads or extends mapped-storage lifetime;
- constant-size facts are prepared lazily and retained by the canonical M3 String storage object;
- joined values fold complete-atom facts and scan only partial ranges;
- cross-atom surrogate pairs are corrected at composition seams;
- character-presence facts are necessary-condition filters only;
- exact M3 String character semantics remain final truth;
- future Z/Manacher/LCP/regex-effect/search-plan lanes must use separate explicit budgets.

## Faithful-reproduction rule

Donor algorithms may change the internal execution strategy, but every JDK21-visible String result,
exception, UTF-16 behavior and VM/JNI contract must remain faithful to stock JDK21.

**Copy the contract faithfully; adapt the implementation into M3JDK21.**
