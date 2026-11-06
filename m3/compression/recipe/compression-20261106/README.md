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

## Run

```sh
JAVA_HOME=<JDK 21> bash m3/compression/recipe/compression-20261106/verify.sh <donor-git> 6fdee5330b8ab005005785aeff0d1105ba40d12c <scratch> [m2-local [m2-tail]]
python -I m3/compression/recipe/compression-20261106/relocate.py --donor-git <donor-git> --donor-rev 6fdee5330b8ab005005785aeff0d1105ba40d12c --check
```

Figures of the 2026-11-06 run: [GATES.tsv](GATES.tsv). Lessons: [RECIPE_MEMORY.tsv](RECIPE_MEMORY.tsv).
