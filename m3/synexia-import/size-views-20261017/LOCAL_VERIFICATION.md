<!-- SPDX-License-Identifier: Apache-2.0 -->
# Actual local candidate verification

Executed on Linux x86_64 with OpenJDK 21.0.12.1 (Debian) and GCC. This tests exact candidate
Java/C templates, not an OpenRewrite application, source-built M3JDK image or full collection
module. Baseline and candidate source closures contain the original four dependency interfaces/
view owners, not replacement production shims. The TreeSet adapter is an independent test oracle.

| Gate | Executed result |
| --- | --- |
| SHA-256/template and Git preimage seals | PASS |
| Java21 --release 21 -Xlint:all -Werror | PASS baseline and candidate |
| JNI headers; GCC C17 strict warnings, syntax, UBSan builds | PASS |
| Baseline filtered/sorted-view corpus | 104,477 checks PASS |
| Candidate same filtered/sorted-view corpus | 104,477 checks PASS |
| Baseline and candidate Java mask corpus | 19,718 checks PASS each |
| Actual JNI mask parity with -Xcheck:jni | 19,718 checks PASS |
| JVMTI vs Instrumentation shallow sizing | 147 checks + 16,000 concurrent calls PASS per configuration |
| Sizing configurations | default; Serial GC; compressed-oops off + 16-byte alignment |
| Missing-native paths | mask 19,719 checks; sizing 5 checks PASS |
| Existing public javap signatures | unchanged for filtered-view, sorted-view and mask owners |
| Canonical Maven/OpenRewrite fixture | NOT EXECUTED locally: Maven unavailable |
| Matched M3JDK/reactor/jtreg/JCK and other native platforms | NOT EXECUTED |

The view corpus covers signed extremes, 512 subsets, four predicate shapes, navigation,
read-only and mutable wrapper behavior, range views, backing updates, stream late binding,
callback traces and exception identity. Generic predicate/comparator JNI callbacks are absent.
Blanket predicate reassociation was rejected because a LongPredicate.and override is observable.

## Resource sample: not a performance claim

A diagnostic probe ran 100,000 size() queries over 128 values after 20,000 warmup queries.
Default VM: both baseline and candidate allocated 256 aggregate bytes; no default allocation
saving was demonstrated. With -XX:-DoEscapeAnalysis, baseline allocated 5,600,256 bytes and
candidate 2,400,256 bytes: 32 fewer bytes/query in that configuration. This isolates removal of
one intermediate filtering cursor; it does not establish retained-heap or application savings.

Single elapsed samples were baseline/candidate 10,594,621/8,385,163 ns under defaults and
12,330,803/9,513,158 ns with escape analysis disabled. These are not JMH multi-fork measurements
and do not support a general speedup claim. JNI sizing and identity-dedup also have real call/
allocation costs and are diagnostics, not an automatic every-operation memory policy.

Raw captured outputs and per-log SHA-256 values are in local-execution.log. Source/test identities
are in target-manifest.tsv and the canonical manifests. Empty compiler logs indicate commands
completed without diagnostics; they are not substitutes for the recipe or whole-product gates.
