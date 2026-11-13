# Compile-time lifetime census of M3JDK first-party code (2026-10-30)

Analyzer: com.synexia.synthesis.lifetime (com.synexia #10869: escape lattice NONE<RETURNED<CALLEE<FIELD<CAPTURED<MAY_ESCAPE,
refcount actions ELIDE/MOVE/RUNTIME), lexical composition (conservative: unknown callees count as escaping), driven by
M3LifetimeCensus (pattern COMPILE_TIME_LIFETIME_DOOP_CENSUS). Sources: 853 files of the superset head (java.base M3*,
m3/collections main + interop/keyed/receiving, m3/arrays, m3/file, m3/fs), 19,411 methods, 9,739 allocation sites.

| DOOP class | rule | sites |
|---|---|---:|
| COLD_EXCEPTION | throw sites and M3NotYetReceived scaffold markers | 5,315 |
| OWNER_SCOPED | escape FIELD: lifetime = the owner -> SoA lane slot of the owner | 524 |
| STACK_LOCAL | escape NONE: scalar-replace or reuse owner scratch | 314 |
| CALLER_SLOT | escape RETURNED: caller-provided destination slot | 343 |
| BORROWED | escape CALLEE: callee-scoped, pass down | 588 |
| CLOSURE | escape CAPTURED | 45 |
| KEEP_OBJECT | MAY_ESCAPE (lexical; the attributed profiler of #10899 cut this ~42% on the synthesis corpus) | 2,610 |

1,814 of the 4,424 non-cold sites (41%) have a lifetime known at compile time with the lexical analysis alone.
Most are arrays already (OWNER_SCOPED int[] 167, long[] 93; STACK_LOCAL int[] 137; CALLER_SLOT int[] 232).
Hot paths (get/put/contains/next/compare/...) allocate little: 140 KEEP_OBJECT sites, mostly API-required facades
(Iterator 30, SimpleImmutableEntry 14, M3LongIterator 14, Entry 4).

DOOP leaves this census supports (recipes, gated): CALLER_SLOT int[] -> caller-provided out-slot; STACK_LOCAL
scratch arrays/StringBuilder -> owner-scoped reusable scratch on non-reentrant cold paths; OWNER_SCOPED small objects
-> lane slots; hot-path facades -> flyweight only where the API contract allows (Map.Entry identity is observable).
