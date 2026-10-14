# M3 String regex-effect receiver

Status: source receiver candidate. Full M3JDK21 image, HotSpot, native libjava build,
and target jtreg acceptance remain required. Local patched-module kernel tests and
explicit owner-model fixtures do not grant product promotion.

## Naming and ownership

This receives capabilities from Synexia, not a blind `MIndex* -> M3*` source rename.
The canonical `m3/docs/name-mapping.json` continues to govern. The additive supplement
`m3/docs/name-mapping-regex-effects.json` records this exact receiving scope and the
pinned canonical-map blob; it does not replace historical mappings or claim global
mapping/catalogue reconciliation. The older full-family inventory selected MIndex-prefixed
basenames only, so it does not establish coverage of newer M3RegexDp owners.

- `MIndexString` remains `java.lang.M3String`, unchanged: canonical owner + coordinate.
- Existing `java.lang.M3StringRegexDp` retains parsing and original rolling-DP fallback.
- `jdk.internal.mindex.M3RX` owns only numeric, bounded regex plans/effects. It is internal,
  not a new String class, exposed Java SE API, spelling pool or payload service.
- Native relations belong to `src/java.base/share/native/libjava/M3RX.c`, with the target
  `Java_jdk_internal_mindex_M3RX_composeRows0` symbol and generated JNI header. The existing
  libjava build topology is reused; no Synexia library loader/runtime dependency is added.

Runtime source is a target-owned GPLv2+Classpath implementation. Existing target copyright
headers are preserved. Synexia Apache-2.0 source is a capability/provenance reference, not
silently relicensed into OpenJDK. Challenge sites supply taxonomy only. Reusable recipes,
postimage templates, fixtures and proof custody stay in `hsoliwal/com.synexia` under
`com.synexia.m3.M3Jdk21RegexEffects`.

## What is received

For source lengths at least 256 units and up to 63 already-admitted dot/star atoms, a single
64-bit frontier replaces the two rolling Boolean rows and their per-character updates. Six precomputed consecutive-star masks compute epsilon closure
with fixed word operations. Text still supports nonsurrogate BMP units; dot still excludes
LF, CR, NEL, LS and PS. Surrogates remain UNKNOWN, including late input after a dead frontier.
Unsupported patterns, large patterns and original size/work refusals retain the existing
fallback. Ordinary String.matches does not acquire a new interruption exception.

A fragment effect contains one Boolean reachability row per incoming NFA state, not a single
DFA state. Composition is ordered Boolean relation composition. The identity is epsilon
closure, NOT the raw diagonal matrix. Plans bind effects by exact object identity; logical
lengths are checked, and repetition uses binary lifting. Independent tests verify all-entry
transitions, seams, identities, composition and repetition against a Boolean-array oracle.

Cross-call reuse follows the existing M3StringSearchPrecompute weak-owner cache pattern:
32 numeric-plan slots and 128 owner/range/plan slots. Plan hashes nominate a slot; exact
instruction equality admits a plan. Effect admission requires weak owner identity, exact
packed coordinate and exact plan identity. Short inputs retain the original rolling DP to avoid the measured cold admission overhead.
For admitted longer inputs, first use streams the frontier. A repeated exact
key may prepare effects only within 512 source units, 31 atoms, 128 visited nodes, depth48,
and one million state steps. Tuple slices reuse existing canonical coordinates; no flattening
or second flat segment tree occurs. A hit avoids text reads. Eviction affects performance only.
The primitive-array payload bound, including plans retained by effect entries, is 277,376
bytes; this excludes object/scalar/header/JVM overhead and transient allocations. The existing
per-call parser still allocates its instruction array; this is not a zero-allocation String API.
`clearPrecompute` and optional-allocation OOME fallback are present; global pressure hooks are
not implemented or claimed.

JNI composition is explicit, not automatically selected by String.matches. It checks both
complete input relations, including unreachable rows. Geometry is 1..64 states and exact
array lengths. Three fixed 64-long stack arrays total 1,536 bytes; no dynamic native allocation,
critical pin, retained pointer or new storage lifetime is introduced. Missing native linkage
falls back to Java. Bounded work is not a wall-clock latency or in-call cancellation promise.

## Qualification and reproduction

The canonical recipe manifest binds the old target source and all postimages. Fix the recipe
or templates and emit a fresh candidate; do not hand-edit generated output. Donor verification:

```sh
python3 synexia-openrewrite-recipes/verification/m3jdk21-regex-effects-20261014/verify.py \
  --output /tmp/m3-rx-fresh
mvn -o -f synexia-openrewrite-recipes/verification/m3jdk21-regex-effects-20261014/pom.xml verify
mvn -o -f synexia-openrewrite-recipes/pom.xml -Dtest=M3Jdk21RegexEffectsRecipeTest test
```

The Python command orchestrates the real existing M3SpanRecipeProof/M3GpuToolJoinManifest,
Java21 compilers, patched java.base execution, generated JNI headers, UBSan/ASan and frozen
probes. It does not emulate OpenRewrite SDK or claim Maven execution. Owner-model fixture
results are separately labelled; their test-only heap spelling is never shipped in target src.
Native fault injection uses mock JNIEnv and is not a real-VM out-of-memory experiment.

On the exact target source, retain the native and M3 storage gates:

```sh
make images
make test TEST="jdk/jdk/internal/mindex/M3RXTest jdk/java/lang/String/M3StringRegexEffectsTest jdk/java/lang/String/M3StringRegexDpTest"
```

The receiver jtreg tests request storage enabled/disabled, both CompactStrings modes where
applicable, and require the actual target native symbol. They are included, not claimed run.
Full image/jtreg, Maven/SDK, strict task/provenance promotion, Windows, other platforms, VM
GC/JNI/JVMTI/CDS/serialization and downstream consumer qualification remain pending.

General captures/lookarounds/backreferences, find/region acceleration, supplementary Unicode,
large DFA/NFA indices, JIT/SIMD, compile-time images and whole-estate precompute synchronization
remain deferred. Existing Pattern/Matcher authority and the original M3 fallback are retained.
The catalogue TSV is a receiving record of Synexia selection history, not another catalogue
service. Performance observations are diagnostic model measurements, not full-runtime claims.
