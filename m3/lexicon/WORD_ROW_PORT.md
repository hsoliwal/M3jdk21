<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 word-ID direct-address precompute — first receiving slice

Status: **implemented, bounded receiver**; **not** java.lang.String/HotSpot integration.
Canonical donor: `hsoliwal/com.synexia`, `develop` at
`8200bd6a92b5e4f81522c177ed0e668bf03664aa`.
M3JDK21 receiving baseline: `master` at
`d5abed865bd47c4dd5ad78e95704348fe2095ed2`.

## What is carried forward

- `IndexId` — stable language ID in the upper 16 bits, optional overlay bit 47,
  stable 47-bit local coordinate. Packed values are created only at API boundaries;
  hot readers use checked `int` local IDs.
- `IndexWordRowsImage` — exact format-3, 128-byte stride, cache-line-aligned
  64-byte scalar core, big-endian DataOutputStream layout; variable relation
  lists use existing absolute offset/count pairs and occupy the **same** mapping.
- `MappedIndexWordRows` and `MIndexLanguageHotCursor` — fixed address arithmetic,
  frequency/lexical ranks, prefix/suffix interval checks, corpus frequency,
  UTF-16/code-point/UTF-8 sizes, bit signals, Java hash, membership/concept/
  subject/expansion CSR lanes, and worker-owned cursor scratch.
- Optional original C11/JNI batch reader projects only flags from the same
  `ByteBuffer` mapping. Every local ID is Java-validated before dispatch,
  JNI independently validates coordinates, and no native pointer escapes a call.
  The Java path needs no native library and is the default.

`M3WordRows` neither materializes spelling nor creates a second canonical
word table. A live reader retains one read-only OS mapping and its immutable
version/fingerprint metadata. It does not cache rows, token arrays, strings,
or per-word objects. Publishers must use a genuinely immutable image file;
`READ_ONLY` consumer mapping alone does not make externally mutable files safe.

## Compatibility and refusal

Only byte-format **3** is admitted. Old 1/2 are refused rather than silently
reinterpreted; their existing Synexia readers remain normative pending separate
source-bound equivalence proof. Fail closed on corrupt headers, sections,
offsets/counts, overflow, illegal ranks, negative lengths/frequencies,
foreign language, wrong dictionary generation, overlay IDs and invalid local ID.

The JDK's public `String`, `Pattern`, `Matcher`, `String.intern`, `==` and
`java.lang.M3String` owner+coordinate contract are untouched. The existing
`M3LexiconPrecompute.IndexWordFacts/IndexWordSignal` typed receiver and
`synexia-precompute-field-map.tsv` retain source-document identities; this
new class consumes the older **frozen row image**, not a replacement metadata
interner. Do not treat row identity as Java String identity or word meaning.

## Test and JNI admission

```sh
M3_JDK=/path/to/jdk21 ./m3/lexicon/run-wordrows-proof.sh
```

This compiles the two Java classes plus the synthetic endian-aware format-3
image oracle with `javac --release 21 -Xlint:all -Werror`, generates JNI headers
using `javac -h`, compiles native C11 with `-Wall -Wextra -Werror`, then checks
Java/JNI parity under normal, `-Xint`, `-XX:-CompactStrings` and C2-focused JVM
modes with `-Xcheck:jni`. The independent fixture pins format-3 offsets,
partial cursor semantics, relation bounds, 10,000 repeated ID probes and
corruption/refusal behavior. Synthetic data is not a real Synexia production
image or a whole-M3JDK acceptance test.

## Required next receiving passes (NOT IMPLEMENTED BY THIS SLICE)

1. Byte-identical real Synexia format-3 image corpus and versioned source SHA;
   full 1/2 decoder fallback and donor-owner cross-validation.
2. Generation-bound M3StringPool lexicon word IDs and exact retained facts,
   shared native/mmap lifetime, source and language namespace mapping.
3. Remaining `MIndexWord*` signal, frequency, morphology, phonetic, probability,
   relations, Luhn/context, lexical knowledge and 100-language source pipelines.
4. Compiler-known word-expression and regex/NFA precompute across immutable
   M3String compositions, without violating JDK Matcher observable state.
5. Full JDK image, jtreg, GC/JIT/CDS/JNI/JVMTI multi-platform and setup-inclusive
   CPU/native/RSS/heap measurements with Maven/OpenRewrite recipe admission.

The original donor's direct-address arithmetic and fixed layout are ported;
full legacy precompute capability equivalence is **not** claimed.
