# Bounded regex closure: qualification for review

SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project

SPDX-License-Identifier: Apache-2.0

## Selected change

`java.lang.M3StringRegexDp` now computes epsilon closure from its existing `Plan.star` mask. The private helper keeps an explicit zero-frontier return; nonzero frontiers use a bounded carry operation instead of unioning one closure-table entry per selected bit. The target adds no field, array, cache, owner or native allocation. The native production source remains byte-identical to the pinned baseline.

This refinement is **selected for review**. Qualification covers the exact Java leaf and the complete actual C JNI entry on a host Java21 runtime. A rebuilt M3JDK image, actual `String.matches` dispatch and full jtreg execution remain pending.

Canonical recipe provenance: `m3-catalogue-refinement-20261018/regex`, using the existing hash-pinned Java/text snapshot recipes. Actual SDK008 output was read back and matched against the sealed target source and receiver assets. No new transformation engine is introduced.

## Exact target identities

Public baseline: `2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`.

| Public repository path | SHA-256 and status |
| --- | --- |
| `src/java.base/share/classes/java/lang/M3StringRegexDp.java` | Selected: `ca679153b93b33214e48c3d7cce4414cf9622ff380ee9aea3334c9a680f37779` |
| Same Java owner at the baseline | `f875210dfb605799b6c8dbee53b28ad6383e000d90da775f7fd285464da7b2d3` |
| `src/java.base/share/native/libjava/M3StringRegexDp.c` | Unchanged: `81440eb281b6401777d02a3ae28787f9d87b12ae10af426a7c78f58a5c9a723c` |
| `test/jdk/java/lang/String/M3StringRegexClosureTest.java` | New: `05fbc141e18a298172ae9f6a77d9b8baa56953fa31ef6d1df6559e910ce302c1` |
| `m3/runtime-integration/regex-closure/run_target_java21.py` | New: `8223329c8f6ff8c23cb71897e935119f5a389dae2ffbcfa4cc51e75844d548b8` |
| `m3/runtime-integration/regex-closure/source-seals.json` | New: `0d699392d485562c953b70d80eeeff14dbbd98132f3d1abd152651e4e3bac87c` |

The adjacent [source seals](source-seals.json) bind all runtime fixture sources. This note records qualification separately and does not change those sealed inputs. SDK008 receipt SHA-256: `e80fc5f819d60ab25c5eb4f8fa024df5e851ddb6ae6a2180be908d4c370cf28a`.

## Actual completed checks

| Check | Result and scope |
| --- | --- |
| Recipe output | Selected Java, new jtreg source and eight durable receiver assets matched actual SDK output. The native baseline was staged as an unchanged pinned input. |
| Strict static/compile | PASS: exact outside-leaf Java preservation; entire C unchanged; Java21 with `-Xlint:all -Werror`; strict C17 normal and UBSan builds. |
| Independent Java graph proof | PASS: **793,823 pairs and 3,989,116 checks**, digest `6ab4c691edc34d9`, using verbatim extracted before/after private methods. |
| Actual production C JNI proof | PASS: **71,847 checks per version/build mode**, digest `58653369c414302`, in normal and UBSan builds with `-Xcheck:jni`. |
| JNI bindings | Generated production/adapter headers and exactly the expected two Java exports. |
| Durable receiver | Actual SDK-received runner and source tree passed static, compile, tests and a real child-process CPU/RSS observer check. |
| New jtreg source | Compiled successfully. Runtime execution on a rebuilt M3JDK is **NOT_RUN**. |

The graph proof exhausts all star masks and frontiers for 1 through 9 atoms and adds adversarial 32-atom runs, separators, accepting bit 32 and random frontiers. It checks equality with independent epsilon reachability, extensiveness, idempotence and range.

The JNI proof compiles the complete production C and a test-only forwarding adapter as separate translation units. It uses real test-owned Latin1 and UTF16 LE/BE buffers, offsets, chunk seams, zero/max lengths, all five dot line terminators and surrogates after dead states. Metadata cases include nulls, shape, opcode, closure-table and incoming-state rejection, plus zero-length/address ordering. Separate JVMs prevent before/after library interposition. The declaration-only Java class used to generate the production JNI header is never loaded as `java.base`.

## Why the closure is equivalent

With frontier `B` and starred-atom mask `S`, epsilon edges are exactly `i -> i+1` at starred atoms. The selected expression is:

```java
B | (S ^ (S + (B & S)))
```

Each maximal one-run of `S` either has no seed and contributes nothing, or a carry propagates from its earliest seed through the run exit. A separating zero stops the carry, while `B` preserves other and accepting states. For at most 32 atom bits, the sum stays below `2^33`. The proof does not extend this bound to a separate 64-state owner.

The zero-frontier guard returns from the closure helper only. Text iteration continues, so a later surrogate still produces the existing UNKNOWN refusal. The restricted grammar, required-star admission, 128 pattern-unit and 8,192 text-unit caps, bounded DP work, >32-atom rolling path, Unicode rules, caches, cursor/tuple logic, native fallback and reachability fence retain their existing behavior.

## Measurements and limits

The following are median-of-three-process medians, with seven measured samples per process after fixture warmup, on Linux x86_64 AMD EPYC 9V74 and Temurin21.0.12.1+1. They isolate the exact source-extracted Java closure and exclude preparation, cursor traversal, String dispatch and full-pattern matching.

| 32-atom frontier | Before ns/op | After ns/op |
| --- | --- | --- |
| Zero | 0.173707 | 0.257143 |
| Single | 0.923323 | 0.517042 |
| Sparse | 2.392207 | 0.558843 |
| Dense | 21.984091 | 0.504827 |
| Mixed | 18.666616 | 0.488689 |

The zero case is variable and receives **no speedup claim**: after fork medians were 0.160885, 0.460757 and 0.257143 ns/op. Small leaf timings are sensitive to JIT/loop optimization and scheduling. The larger nonzero-frontier reductions support the bounded helper change; they are not end-to-end regex speedup figures. No Apple M3 hardware or cold-process latency was measured. First-operation fields are fixture diagnostics after setup.

Across the three closure benchmark processes per version, median user CPU was 0.470405 to 0.169877 seconds, system CPU 0.023908 to 0.028109 seconds, maximum RSS 43,720 to 43,356 KiB, and wall time 0.467595 to 0.165360 seconds. A fresh Python observer measured one real child with `resource.RUSAGE_CHILDREN`. Linux maximum RSS is a process high-water mark, not incremental Java retained heap. These whole-process observations include JVM/fixture setup and do not predict product resource savings.

The unchanged native before/after source was also timed as a control. Its case ratios ranged from about 0.91 to 1.51, so variation in unchanged code must not be described as a native optimization. The coordinated agent timing window did not establish physical-host isolation or fixed CPU frequency.

## Reproduction

Use Linux, Python3 with its standard `resource` module, Java21, a C17 compiler with UBSan support, and `nm`. From the public repository root, set `REGEX_JDK` to the JDK directory and `REGEX_EVIDENCE` to a writable output directory, then run the stages serially:

```bash
python3 m3/runtime-integration/regex-closure/run_target_java21.py --stage static --jdk "$REGEX_JDK" --output "$REGEX_EVIDENCE"
python3 m3/runtime-integration/regex-closure/run_target_java21.py --stage compile --jdk "$REGEX_JDK" --output "$REGEX_EVIDENCE"
python3 m3/runtime-integration/regex-closure/run_target_java21.py --stage test --jdk "$REGEX_JDK" --output "$REGEX_EVIDENCE"
python3 m3/runtime-integration/regex-closure/run_target_java21.py --stage benchmark --jdk "$REGEX_JDK" --output "$REGEX_EVIDENCE"
```

The runner reads the pinned baseline from Git. If that commit is unavailable locally, supply `--baseline` with the hash-matched before-image directory described in the adjacent [README](README.md). Every stage checks source/fixture seals and later stages require the preceding PASS receipt. The runner has no private implementation dependency and produces stage receipts, command logs and resource observations.

Remaining product gates are a rebuilt M3JDK image, actual String dispatch, native pool/GC/CDS/HotSpot lifetime, full reactor/platform coverage and jtreg runtime. The historical `M3StringRegexEffectsTest` remains unchanged and pending reconciliation: it expects `caches`, a SoftReference root and 128 effects, while the current helper has `tables`, a direct volatile Tables root and 256 effects. It was not weakened or reported green.
