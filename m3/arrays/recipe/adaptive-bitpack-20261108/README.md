# Adaptive signed int32 bitplanes — M3 recipe 2026-11-08

This is **lossless** int32 packing, not approximate LLM weight quantization.
Status: additive Java and bounded JNI/C11 atoms; local Java/JNI and C11 checks
passed. Whole JDK, jtreg, Maven reactor and cross-platform acceptance pending.

## Owner and contract
- Target: existing `m3/arrays` in **M3JDK21**, not an external Java overlay.
- Portable `AdaptivePackedIntArray`: immutable, owned, 0..32-bit unsigned
  deltas from signed minimum; exact random access; LSB-first little-endian
  bytes; zero padding, overflow and geometry validation; no Java array changes.
- `AdaptivePackedIntNative`: required-native JNI batch encode/decode on existing
  `m3_arrays` library; max 4096 elements **and** 4096 bytes. Region copies only,
  no retained JVM/native buffer pointers; no implicit JNI fallback.
- Complexity: min/max scan O(n), encode O(n * width/8), random access O(width/8)
  bounded by 4/5 bytes. No universal performance advantage is claimed.
- Dataset-dependent memory: one million 16-state ints have 4,000,000-byte
  `int[]` payload versus 500,000-byte packed payload, excluding all headers.

## Category-led donor evaluation
| Category / candidate | Contract comparison | Decision |
| --- | --- | --- |
| Apache Lucene PackedInts/BulkOperationPacked (Apache-2.0): https://apache.googlesource.com/lucene-solr/+/7ada4032180b516548fc0263f42da6a7a917f92b/lucene/core/src/java/org/apache/lucene/util/packed/BulkOperationPacked.java | True packed storage; differs in signed minimum, layout and JNI lifecycle | Concept reference only; original implementation, no copied source |
| LeetCode 338 Counting Bits: https://leetcode.com/problems/counting-bits/ | Counting bits, not encoding a signed int array | Reject as code donor; retain width/bit reasoning |
| HackerRank Flipping Bits: https://www.hackerrank.com/challenges/flipping-bits/problem | Unsigned 32-bit semantics, not array codec | Reject as code donor; retain unsigned-carrier semantics |
| GeeksforGeeks Bitwise Operators: https://www.geeksforgeeks.org/computer-science-fundamentals/program-for-bitwise-operators-in-c-c-java-python-c-javascript/ | Masks and shifts, but no codec contract | Concept reference only, no code copy |

## Serial recipe gates
1. Inventory fixed 40/48/56-bit and VInt existing owners; preserve their names
   and contracts; add these two classes and the C codec as isolated atoms.
2. Enforce source/bin compatibility: no modified preexisting Java API or VM array.
3. Differential: 2,442 Java/JNI randomized arrays covering widths 0..32;
   strict C11 compilation with -Wall -Wextra -Werror -Wconversion
   -Wshadow -Wpedantic; assert unmodified destination on C validation failure.
4. Boundary tests: INT_MIN/INT_MAX, 0 and constant bits, partial-byte padding,
   invalid width, nonrepresentable values, size, empty input, ownership.
5. Run `mvn -f m3/arrays/pom.xml -Pnative-linux test` and full SDK regression
   before promotion. Local authoring environment lacked Maven: this gate
   remains pending and is **not** marked passed.
6. Verify donor licensing/NOTICE on any future literal source adoption.
