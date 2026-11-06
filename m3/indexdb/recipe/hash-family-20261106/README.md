<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# hash-family-20261106: relocate L-Hash v1, MinHash/LSH and gear CDC into `jdk.m3.indexdb` (recipe RELOCATE_HASH_FAMILY v3)

One leaf of the M3JDK convergence programme: the com.synexia hash family crate `m3-hash-family-20261030` joins
`m3/indexdb` beside `M3IndexDbFingerprintV1`, `M3IndexDbChunkSimHash` and `M3HammingTopK`. Scope as agreed with the
search lane owner (com-synexia-45): this leaf takes **L-Hash v1** (canonical token lane + logic hash), **MinHash with
banded LSH** and **content-defined chunking** (gear rolling hash) with its per-chunk SHA-256. **SimHash64 and the FNV-1a
string hash are not ported here**: they are 45's (`jdk.m3.search.M3SimHash64`, `jdk.m3.search.M3Utf8Fnv1a64`, landed
by #2550 while this leaf ran) and the indexdb fingerprint owner is `M3IndexDbChunkSimHash` (tie rule pinned by #2514).

## Decision per donor class (DECISION.tsv)

| Donor class | Verdict | Target / owner |
|---|---|---|
| `M3LHashTokens` | ABSORB | `jdk.m3.indexdb.M3LHashTokens` |
| `M3LogicHash` | ABSORB + NEC vs `M3IndexDbFingerprintV1.logicHash64` | `jdk.m3.indexdb.M3LogicHash` |
| `M3Fnv1a` | ABSORB as dependency `M3LaneFnv1a` + NEC vs `jdk.m3.search.M3Utf8Fnv1a64` | `jdk.m3.indexdb.M3LaneFnv1a` |
| `M3Lanes` | ABSORB as dependency `M3HashLanes` | `jdk.m3.indexdb.M3HashLanes` |
| `M3Distances` | ABSORB as `M3HashDistances` + NEC member row (hamming) | `jdk.m3.indexdb.M3HashDistances` |
| `M3MinHash` | ABSORB + NEC vs cpu-tools `TextSignatures.minHash64` | `jdk.m3.indexdb.M3MinHash` |
| `M3MinHashLsh` | ABSORB | `jdk.m3.indexdb.M3MinHashLsh` |
| `M3ChunkCdc` | ABSORB + NEC no-fit vs `M3FileRollingIndex` | `jdk.m3.indexdb.M3ChunkCdc` |
| `M3ChunkSha` | ABSORB | `jdk.m3.indexdb.M3ChunkSha` |
| `M3SimHash64`, `M3SimHashContract` | NEC-to-45 (SKIP) | `jdk.m3.search.M3SimHash64` (#2550), `M3IndexDbChunkSimHash` |
| `M3Shingles` | DEFER (SKIP) | needs `M3SimHash64.WORD` and the UTF-16 `ofChars` |
| `M3HashFamily`, `M3HashFamilyRegistry`, `M3HashReceipt`, `M3RegistryContract` | SKIP (MACHINERY) | Synexia-only (crate README receiving note, AGENTS.md #1819 option 2) |
| `M3LogicHashContract`, `M3MinHashContract`, `M3ChunkCdcContract` | ABSORB (test tree) | JUnit 5 wrappers `*ContractTest` |

No ALREADY and no EXTEND verdict: no owner on master implements a ported contract (owner grep over `m3/` and
`jdk/internal/m3` for MinHash, LSH, chunk, rolling, Rabin, Gear, FastCDC, LHash, logicHash; the probes below measure
every overlap that exists).

**FNV scope note.** L-Hash v1 (`ofInts`: H1..H4 and the window lane, `ofLongs`: the H5 fold) and the LSH band keys
(`ofLongs` + `step`) need the word-wise lane FNV-1a of the LOCKED spec (an `int` is sign-extended to 64 bits before the
xor, a `long` is xored whole). 45's `M3Utf8Fnv1a64` hashes UTF-8 bytes (`value & 0xff` per step), a different contract,
so the dependency is relocated under the lane name `M3LaneFnv1a` (the bare FNV-1a name stays with 45); its UTF-16
`ofChars` member travels byte for byte (no hand edit) and is an NEC row with measured agree counts.

## NEC rows (recorded differences, never equivalence decisions)

| Donor member | M3 owner | Probe (differential.sh lane f) |
|---|---|---|
| `M3SimHash64` | `jdk.m3.search.M3SimHash64.of` (#2550, `m3/search/src/main/java/jdk/m3/search/M3SimHash64.java` blob `170aaeba84c7`) | crate oracle corpus: 0/403 equal, mean Hamming 16.31; L lane: 272/20,000 equal, mean 23.85 |
| `M3SimHash64` (fold) | `M3IndexDbChunkSimHash.simHash64` (#2514) | 20,000 windows: 10,143 equal (untied or empty), 9,857 differ exactly on the tie mask, 0 unexplained |
| `M3Fnv1a.ofChars` | `jdk.m3.search.M3Utf8Fnv1a64.hash` (#2550, blob `0954214ced52`) | corpus: 353/403 equal = the 353 ASCII-only files; L lane: 9,944/20,000 equal = the 9,944 ASCII-only texts |
| `M3LogicHash.of` (H1) | `M3IndexDbFingerprintV1.logicHash64` | 20,000 inputs: 0 equal (domain + feature list, avalanche/combine vs FNV-1a over the token lane) |
| `M3MinHash.signature` | cpu-tools `TextSignatures.minHash64` (blob `e5dbbdacf074`) | 1,000 pairs, 128 lanes: mean abs error 0.0258 vs 0.0263, 94 equal estimates |
| `M3Distances.hamming` | `M3IndexDbChunkSimHash.hammingDistance(long,long)`, `M3IndexDbFingerprintV1.hammingDistance`, `jdk.m3.search.M3SimHash64.hamming` | 1,000,000 pairs equal; `hammingAll` 1,000,000 equal |
| `M3ChunkCdc.chunk` | `jdk.m3.file.M3FileRollingIndex` | no fit (Rabin-Karp candidate filter over token handles, no content-defined cut); no probe |

## Donor and relocation (recipe, never by hand)

| Item | Value |
|---|---|
| donor | `hsoliwal/com.synexia` develop `f7b9599a63da1c1264ecacdcb4c9286e055a0948`, crate `synexia-openrewrite-recipes/crates/m3-hash-family-20261030` (added by `4a2e6452a7a`, 2026-10-31); blobs in `PROVENANCE.tsv` |
| recipe | `relocate.py` v3: `python -I m3/indexdb/recipe/hash-family-20261106/relocate.py --donor-git <clone> --donor-rev <rev> [--check]`, derived from the module's own `hamming-topk-20261106/relocate.py` v2 (#2510): SKIP, ORACLE, PROBE, NEC and NAMING rows and the SKIP-type refusal added |
| map | `RELOCATION_MAP.tsv`: 12 FILE rows (9 main, 3 contracts), `PACKAGE com.synexia.rewrite.hash -> jdk.m3.indexdb`, TYPE rows (`M3Fnv1a -> M3LaneFnv1a`, `M3Lanes -> M3HashLanes`, `M3Distances -> M3HashDistances`, the rest kept, `M3HashFamily -> M3HashFamilyContractSupport` scoped to the three contracts), 2 NAMING rows, 7 SKIP, 3 ORACLE, 3 PROBE, 6 NEC rows |
| provenance | `PROVENANCE.tsv`: 12 relocated files `body_match_renamed=TRUE` (3 also plain `body_match=TRUE`), the 2 NAMING substitutions on `M3HashLanes.java`, 7 SKIP and 3 ORACLE pins |
| naming | `jdk_naming.py plan/apply --only m3/indexdb/`: 0 moves, 0 rewrites, 0 refusals, FIXED_POINT 0/0; `relocate.py --check` after it: changed=0 |

The recipe rewrites only the license header (M3JDK style), the package line, the mapped type tokens, the two NAMING
texts (the Javadoc prose `jdk_naming.py` would rewrite: `com.m3.` -> `jdk.m3.`, `M3JDK21` -> `M3JDK 21`) and inserts one
`//FLLDCIM: RELOCATE` anchor naming the donor blob URL; everything else is the donor's bytes, including its `//FLLDCIM:`
anchors and its inline constants. M3JDK-authored files (test tree only): `M3HashFamilyContractSupport` (the donor CLI's
`option`/`hex` bodies verbatim, for the scoped TYPE rows), the JUnit 5 wrappers `M3LogicHashContractTest`,
`M3MinHashContractTest`, `M3ChunkCdcContractTest`, the dump driver `M3HashFamilyDump`, the NEC probe
`M3HashFamilyNecProbe`, and the fixture tables `src/test/resources/jdk/m3/indexdb/hash-family/LHASH_FIXTURES.tsv`
(400 inputs as base64, so no checkout line-ending rule can alter an input) and `LHASH_ORACLE.tsv` (written by the
crate's `oracle_lhash.py` + `cpu_lhash.py`, regenerated byte for byte by differential.sh).

## Gates (GATES.tsv, `verify.sh` end to end)

See `GATES.tsv` for commands and figures: (1) `mvn -f m3/indexdb/pom.xml package` with the module's `-Xlint:all -Werror`,
0 warnings, 79 tests (baseline 76); (2) crate figures reproduced on donor and relocated classes, default VM and -Xint;
(3) dumps byte-identical (full and -Xint scale); (4) Python oracle 100,000/100,000 rows identical; (5) `javap -p`
identical modulo names; (6) existing tests and the #2510 dump digests unchanged; (7) untouched class files
byte-identical to the master build; (8) jdk_naming fixed point; scope `m3/indexdb/**` only.

## Hard-limit notes (recorded, not fixed: donor bodies are kept byte for byte)

Methods over 15 lines: `M3LHashTokens.tokenize` (18), `scan` (17), `numberEnd` (18), `M3MinHashLsh.candidatePairs`
(23). Loops instead of streams on every hot path (34 loop statements over the 9 classes; the crate's HPCPOP anchors
name them). Inline numeric literals the donor did not hoist (token ids 101..106, operator ranges 51..66, the H4
classes 57..61, the 0x20 case fold, 0xffffffff00000000L, the 16-entry initial lanes): 61 occurrences in
`M3LHashTokens` (18), `M3LogicHash` (19), `M3HashDistances` (7), `M3MinHashLsh` (5), `M3ChunkCdc` (5),
`M3ChunkSha` (3), `M3HashLanes` (2), `M3MinHash` (2). A hoisting recipe may name them later; class lengths are all
under 300 lines (largest `M3LHashTokens`, 197).

## Open items

- `M3Shingles` (MinHash text front end) waits for a word-class owner: the landed `jdk.m3.search.M3SimHash64` uses a
  different TOKEN pattern, so a MEMBER row needs a decision first (NEC); MinHash/LSH take 64-bit shingle lanes.
- `M3HashDistances.hamming`/`hammingAll` duplicate the expression of three owners (NEC member row); a later MEMBER row
  may route them to `M3IndexDbChunkSimHash.hammingDistance`.
- `M3LaneFnv1a.ofChars` (UTF-16 units) vs `M3Utf8Fnv1a64` (UTF-8 bytes): the convergence owner decision is 45's.
- The CDC stability law holds on high-entropy content only (lane C counts per content kind in GATES.tsv); FastCDC
  normalized chunking (version 2) remains the crate's open gap.
- The crate's Python reference reads files with universal newlines; the Java atoms decode bytes: inputs with a lone CR
  differ (counted in GATES.tsv); a byte-mode reading in the reference would remove the divergence (com.synexia side).
- `jdk.m3.indexdb` has no `module-info.java`; the new classes join the automatic module as the rest of the package.
