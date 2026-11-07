<!-- SPDX-License-Identifier: Apache-2.0 -->
# Recipe compression-20261106 (RELOCATE_COMPRESSION v3)

Census pass 2, leaves **P2-080** (crate `m3-compression-reusable-sink-20261104`, first commit) and **P2-125** (crate
`m3-compression-chunk-cache-20261030`, second commit): the opt-in raw-format encoders and the format-neutral
compressed-chunk cache of the first-party donor `hsoliwal/com.synexia`, relocated into a new module `m3/compression`
(`com.m3:m3-compression`, module `org.m3jdk.compression`, exported package `jdk.m3.compression`), recipe-first and
byte-for-byte.

| Owner | Kind | Contract |
|---|---|---|
| `M3Lz4RawBlockEncoder` | public | one LZ4 raw block per call (no frame), output cap on wire bytes, end-of-block rules kept |
| `M3SnappyRawEncoder` | public | one Snappy raw stream per call (varint length, literal and COPY_2 tags), no framing/CRC |
| `M3CompressionByteSink` | package-private | bounded reusable sink: reset keeps at most 64 KiB scratch, cap refusals, `finish` copies |
| `M3LzMatchCandidateIndex` | public (closure) | single-probe four-byte-prefix candidate table, epoch isolation |
| `M3LzByteKernels` | public (closure) | `commonPrefix` (over `Arrays.mismatch`), batch lanes, overlapping `copyMatch` |
| `M3CompressedChunkCache` | public (P2-125) | direct-mapped slots, SHA-256 + exact raw equality per hit, raw + encoded byte budget with LRU eviction, encoder outside the monitor, `trim()` revokes in-flight fills |

## Decisions

* Owner: no M3JDK owner of LZ4/Snappy encoding, LZ match finding, a bounded compression sink or a content-addressed
  compressed-chunk cache exists
  ([DECISION.tsv](DECISION.tsv)); the atoms need `java.base` only, so they form their own leaf module rather than
  joining `m3/algorithms` (which requires `jdk.m3.collections`). The module is a default module of `m3/pom.xml`.
* FOSS: the donor is first-party and not derived from lz4-java, snappy-java or Commons Compress (token check,
  [FOSS_REUSE_DECISION.tsv](FOSS_REUSE_DECISION.tsv)); the published LZ4 block format and Snappy format descriptions
  are the contract. No third-party jar is a dependency; four third-party decoders ran only in a scratch differential.
* Lineage: develop `6fdee5330b8a` blobs equal the crates' sha256-pinned postimages; retained variants are the older
  pre-reuse preimages ([RELOCATION_MAP.tsv](RELOCATION_MAP.tsv) header).
* Closure: both encoders construct `M3LzMatchCandidateIndex` and call `M3LzByteKernels`, so both (and the index
  oracle) are FILE rows. The donor's decoder pair, JNI kernels and four-way index are OMIT rows (blobs pinned), next
  leaves of the family.

## Oracles

* Donor oracles relocated: `M3CompressionWorkspaceSelfTest` (1,600 seeded inputs + a 250,000-byte block, exact wire
  SHA-256 of both codecs, cap and sink checks; 5,170 checks), `M3LzMatchCandidateSelfTest` (1,498,505 checks), with
  their JUnit wrappers; `M3CompressionAllocationProbe` (diagnostic main).
* Format oracle (test scope, new): `M3CompressionFormatOracle` encodes 100,000 seeded inputs (fixed size edges x 4
  fills, 64 KiB distance and block edges, eight 4 MiB blocks, eight seeded content shapes) under the reference
  worst-case bounds and decodes them with `M3CompressionSpecDecoders`, strict decoders written from the two format
  descriptions only (end-of-block rules enforced); every 97th input replays the exact cap and refuses cap - 1.
* Chunk cache (P2-125): the donor oracle `M3CompressedChunkCacheSelfTest` (JDK Deflater / Inflater, 2,032 checks) with
  its wrapper, and the reference-model oracle `M3CompressedChunkCacheModel` (test scope, new): 5 configurations x
  40,000 seeded operations against a model holding slot contents in a `HashMap` and recency in an access-ordered
  `LinkedHashMap` (LRU); hit or miss, returned bytes, counters, entries, payload and encoder calls agree at every
  operation.
* Differential ([differential.sh](differential.sh)): donor closure compiled from the donor tree vs the relocated
  classes: oracle outputs, the M3WR wire corpus, the format oracle digests and the allocation bytes/op are
  byte-identical, and so are the chunk-cache oracle output and the model trace digest over the donor cache; three
  one-line mutants (two encoder spec mutants, one cache eviction-order mutant) are refused.

## Leaf 3: decoder pair and four-way index (2026-11-06, third commit)

The next leaves the first two commits recorded: the donor decoder pair with its four oracles and the four-way candidate
index, relocated by the same `relocate.py` (thirteen OMIT rows of [RELOCATION_MAP.tsv](RELOCATION_MAP.tsv) became FILE
rows; donor commit unchanged, develop `165ceb5a0202` carries identical compression trees). The JNI kernels stay HELD:
they are the native leaf of the family.

| Owner | Kind | Contract |
|---|---|---|
| `M3Lz4RawBlockDecoder` | public | one LZ4 raw block, decoded size out-of-band, allocation cap; truncation, offset 0 or past the output, sequences past the declared size, a block ending after a match and trailing bytes refused (`IllegalArgumentException`) |
| `M3SnappyRawDecoder` | public | one Snappy raw stream; length varint (at most 5 bytes) checked against the cap before allocation; COPY_1/2/4; exact size |
| `M3LzFourWayCandidateIndex` | package-private | four-way bucketed variant of the candidate index (same lanes and slot budget); no encoder uses it |

Oracles (all run in the test phase unless noted):

* Donor oracles relocated with their JUnit wrappers: `M3CompressionEncoderSelfTest` (11,892 checks),
  `M3CompressionAdversarialSelfTest` (232,000 checks: 100,000 random streams, 5,000 bit-flipped blocks, 4 workers),
  `M3CompressionGrammarSelfTest` (4,717 checks), `M3LzFourWayCandidateSelfTest` (630,288 checks, collision witness
  0 vs 156 hits). Mains not run by surefire: `M3CompressionInteropSelfTest` (replays an M3CZ corpus of native codecs) and
  `M3CompressionEncodedCorpusCli` (writes the M3EC corpus for native decoders); both ran in the scratch interop lane.
* `M3CompressionDecoderAgreement` (test scope, new): the donor decoder pair, reached by class name, against the strict
  reference decoders of `M3CompressionSpecDecoders`: the format oracle's 100,000 encoder streams per codec and 20,000
  grammar streams per codec (Snappy COPY_1 / COPY_4 tags and wide literal headers, LZ4 long extension runs and
  overlapping offsets, which neither encoder emits) are accepted by both with equal output; 80,000 malformed streams
  per codec (truncated, offset 0, offset past the output, literal overrun, match overrun, LZ4 255-run length overflow,
  Snappy length-varint overflow, trailing bytes, declared size too long; 10,000 per kind) are refused by both with
  `IllegalArgumentException` and the same refusal class (every refusal message maps to a class set: input end, offset,
  output overrun, end-of-block rule, length header; donor and reference sets must intersect); 200,000 fuzz streams
  through the donor decoders under bounded caps raise nothing but `IllegalArgumentException` and stay within the cap.
  The only admitted disagreement: an LZ4 block the donor accepts and the reference decoder refuses for an encoder-side
  end-of-block rule (155 fuzz streams); the donor decoder, like the reference C decoder, does not enforce those rules.
* `M3LzFourWayEncodingProbe` (test scope, new): an encoder twin (the encoders' parse loops with the candidate index as
  a parameter) reproduces both encoders' wire bytes over the direct-mapped index for all 100,000 inputs; over the
  four-way index its wires round-trip through the donor decoders and the reference decoders. Ratio (informational):
  four-way / direct wire bytes LZ4 1.0067, Snappy 1.0044.
* Differential ([differential.sh](differential.sh) (f)-(i)): donor vs relocated oracle outputs, the M3EC corpus, the
  agreement report and the four-way probe report (wire digests, candidate trace) are byte-identical; four mutants
  (two decoder spec mutants, a decoder refusal-class mutant, a four-way eviction mutant) are refused.
* Scratch interop (outside the module, [GATES.tsv](GATES.tsv) `decoders_native_interop`): third-party and native encoders
  (liblz4 fast and HC, lz4-java, aircompressor, libsnappy) decode through the donor decoders; the four-way wires decode
  through lz4-java (Java and JNI), aircompressor, Commons Compress and snappy-java; the M3EC corpus decodes through
  liblz4 / libsnappy and an M3CZ corpus written by them replays through `M3CompressionInteropSelfTest`.

## Native leaf: JNI prefix kernels (2026-11-06, fourth commit)

The JNI owner of the family and its native directory, relocated by the same `relocate.py` (six OMIT rows became FILE
rows, three native FILE rows and one JNI row were added; the native extension of the recipe handles line-comment C
headers and CMake files and leaves every earlier row's fixed point unchanged).

| Owner | Kind | Contract |
|---|---|---|
| `M3LzNative` | public | explicitly loaded JNI batch prefix matcher: absolute real path, ABI 1, load-time parity proof against `M3LzByteKernels`, batches up to 16 MiB input and 65,536 lanes, every native result re-checked; never selected implicitly |
| `src/main/native/m3_lz_jni.c` | C11 | `m3_lz_prefix` (8-byte `memcpy` word compare, scalar tail) and `Java_jdk_m3_compression_M3LzNative_{nativeAbi,nativeMatches}`: one call-scoped aligned arena filled by `Get*ArrayRegion`, every lane checked, no Java pointer retained |
| `src/main/native/CMakeLists.txt`, `tests/m3_lz_test.c` | build, C oracle | the donor's build file (shared library `m3_lz_compression`, `-Werror` flag set, CTest `m3_lz_test`) |

The Java kernels stay the backend: without `-Pnative-linux` the JNI lanes of `M3LzSelfTest`,
`M3LzFourWayNativeInteropSelfTest` and `M3LzNativeParityProbe` (test scope, new: JNI vs Java prefix lanes over the
format oracle's 100,000 inputs) are skipped and print so. With `-Pnative-linux` (Linux, CMake and a C compiler) the
module pom configures, builds and CTests `src/main/native` and passes `libm3_lz_compression.so` through `m3.lz.library`.
[native-gate.sh](native-gate.sh) (Linux host) compares the donor's and the relocated `javac -h` headers and exported
symbols, compiles both C files with `-Werror`, builds with CMake, runs the oracles under `-Xcheck:jni` against both
libraries (byte-identical output) and the parity probe, and refuses a one-line kernel mutant at load.

## Run

```sh
JAVA_HOME=<JDK 21> bash m3/compression/recipe/compression-20261106/verify.sh <donor-git> 6fdee5330b8ab005005785aeff0d1105ba40d12c <scratch> [m2-local [m2-tail]]
python -I m3/compression/recipe/compression-20261106/relocate.py --donor-git <donor-git> --donor-rev 6fdee5330b8ab005005785aeff0d1105ba40d12c --check
```

Figures of the 2026-11-06 run: [GATES.tsv](GATES.tsv). Lessons: [RECIPE_MEMORY.tsv](RECIPE_MEMORY.tsv).
