# Native M3JDK21 trigram metadata refinement

The runtime change is in existing `src/java.base/share/classes/jdk/internal/mindex/M3TQ.java`.
It is not a new String wrapper or external search framework. M3String canonical payload ownership
and the existing M3StringSearchPrecompute consumer remain unchanged.

Canonical recipe, reviewed before/after sources, algorithm catalogue, raw probes and independent
tests are in [Synexia PR #12717](https://github.com/hsoliwal/com.synexia/pull/12717), commit
`e3adf09c8257dd3058f7a0736afb6af6cab20324`, task
`synexia-openrewrite-recipes/recipes/m3jdk21-tq-primitive-composition-20261021`.
Target parent is `a4415af418a9b0f21a06cd5801d238577fbb6eb7`.

## Selected atoms

AND/OR query composition stages trigram keys in a dynamically grown primitive long array,
using offsets, bulk copies and JDK ArraysSupport.newLength. Input is still traversed once;
normalization, callback order, short-circuit/null handling and defensive output contracts remain.

Facts.concat uses scalar seam keys rather than a temporary array. Private immutable membership
arrays can be shared only when their union is identical and both boundary trigrams are already
represented. Otherwise exact sorted merge is retained. Length/budget checks occur first; exports
remain defensive. This benefits repeated/shared metadata composition without reading the text.
No automatic Facts.concat call is newly installed in the String tuple owner in this increment.

The native file change is one additional JNI **test** binding in the existing independent
`libM3TQTest.cpp` oracle. Both original symbols remain. There is no production native backend,
new native dispatch, per-entry storage, global cache or second text owner.

## Executed qualification

The canonical offline Java replay changes two existing files and then reaches zero changes;
128 refusal/fixed-point checks pass. The class-backed OpenRewrite/JUnit gate is supplied but
NOT_RUN because Maven/dependencies were unavailable; offline execution is not SDK execution.

Baseline, intermediate pass and candidate each pass 498,900 Java assertions and 510,747
JNI-backed assertions with identical public-observation transcripts. The replayed candidate
passes interpreted execution. Actual complete admitted Java sources compile with -Xlint:all
-Werror. C++20 strict compilation, generated headers, -Xcheck:jni and fatal UBSan are used.
Public descriptors compare unchanged. The unsafe-sharing mutant is caught by the permanent
`aa + aa` seam test. Counts are assertions, not independent test-method counts.

The packaged runner was executed on the sealed output bytes:

```sh
M3_JAVA_HOME=/absolute/path/to/java21 bash m3/runtime-integration/check-m3tq-array-composition.sh
```

Its completed log and consumed-source SHA-256 are under
`m3/runtime-integration/evidence/tq-primitive-composition-20261021/`.
This is a host-patched component/JNI-oracle run, not a rebuilt JDK or actual jtreg run.

## Measured costs and pending scope

At 4,096 keys, the three-fork warm probe records shared-concat allocation 98,416 -> 32 bytes/call
and query composition 312,480 -> 242,120. Distinct equal key arrays still merge (98,416 -> 98,384).
Some timings regress: query-512 median 10,610 -> 11,546 ns; independent-concat-64 417 -> 559 ns.
Raw rounds and the escaped-result probe are canonical; there is no universal or whole-JDK speedup claim.

Existing licenses remain; no challenge implementation was copied. Shortstring admission, every
filtered/sorted collection/fork, full regex/long-offset behavior, full source-built JDK/SDK/reactor,
jtreg/TCK, end-to-end String routing/performance, pressure/platform/GC/CDS and production JNI linkage
remain NOT_RUN/HOLD. This task does not advance the receiving phase or finish the entire programme.
