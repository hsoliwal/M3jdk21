# M3 indexed mask windows — source-preserving current-owner receiver (2026-10-17)

Canonical source and Maven/OpenRewrite recipe owner: `hsoliwal/com.synexia`. Public product target: `hsoliwal/M3jdk21`, existing Apache-2.0 `com.m3.collections` module; `java.util` is not replaced.

## Why
Previous source-sealed Java/JNI receiver #1075 introduced `selectWindow` and `selectLongWindow`, but the checked current `master` owner has lost both while preserving the later all-selected fast path from #1076. This recipe reconciles both **without** overwriting newer code: it requires current-source SHA-256 preimages, inserts only private helper/method atoms and JNI window symbols, and refuses drift.

## Exact four-file contract
Existing Java main preimage e4d8fd83ec0acb9b94bdde5a138d797ccd539c0bb87fe60d419a52f910051fcb; postimage 6b5ecac611a413028bafeda19b579086502d5bde604af3df7f98fa4318ebcfbd. Existing C17 source preimage d7786cd5308f4a1e6743680d35e51f5f11a66b106abdb8f170edcb92b901d1cb; postimage 1393c074ab39c52ed8feaf0ab0c56db6c0324258fca6c596a78e7bc5490997ef.

Rows and unsigned byte masks remain caller-owned arrays. Every null/range/mask is validated before output or native calls. Independently checked row/state offsets support dynamic array capacity, slices and views without creating input subarrays. Two-pass sparse selection allocates exactly one detached result array; all-passing rows use one defensive `Arrays.copyOfRange`. Duplicate IDs, signed extrema and input encounter order survive. Java is default; `NATIVE_REQUIRED` fails when library is absent and never runs callbacks in native. Input arrays must remain stable during the call; no snapshot/concurrency claim.

The existing `select`, `selectLong`, `selectedCount` and `selectJava` source bytes and method descriptors remain unchanged. New native symbols are additive; old `selectNative` symbol and body stay intact. JNI copies fixed 256-item regions and retains no Java-array pointer. Heap/performance superiority is **not** asserted by a structural count alone.

## Focused executed proof
- Local exact-owner Java21 `--release 21 -proc:none -Xlint:all -Werror` + header generation: PASS.
- Native C17 strict warnings `-Wconversion -Wshadow -pedantic -Werror`: PASS.
- Independent 1,500-seed differential study with 0, 255, 256, 257, 511, 512, 513 block sizes, independent offsets, zero/high-bit/full masks, signed extremes, duplicates, immutability, invalid parameter refusals: **7,526 Java checks, 10,528 real JNI -Xcheck:jni checks PASS**.
- C17 UBSan-enabled real JNI repeats: **10,528 checks PASS**.

## Still HOLD
The source-sealed recipe requires actual OpenRewrite SDK/JUnit Maven execution, fixed-point and drift refusal; source copy/replay proof does not substitute for SDK execution. The full owning-module Maven/JUnit suite, old historical source-verification roster, installed JDK/jtreg/HotSpot, GC/JMM concurrency, platform portability, JMH/RSS and CI exact-head proofs are independent and not claimed complete. Historical mask-selection manifests are **immutable**. Keep the target receiver draft until all required gates are green and the String → arrays → collections order permits promotion.
