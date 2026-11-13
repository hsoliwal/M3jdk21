# Constructor-local lexicon record digest reuse (R237)

SharedLexiconImage previously created a SHA256 engine and result array for every record while capturing its precompute table. This recipe changes only the existing private constructor: lazily create one engine and one32-byte scratch, update from each existing recordBytes view, and copy the result into the same private recordDigests lane. Existing range/hash/order validation stays before the record engine is initialized;zero records still use only the two existing whole-image hashes.

Public API, fields, version1/2 image format, UTF16 ordering, record IDs, source mappings, ownership and all non-constructor method bytecode remain unchanged. No new retained storage/cache/node, native implementation, global or thread-local engine is introduced. Retained tables remain O(recordCount);per-record byte-buffer views still allocate. This reduces repeated admission work without claiming zero-allocation startup.

## Ownership discriminator and rejected candidate

The first direct-output candidate passed64JUnit executions and showed allocation savings, but those tests missed a private-array boundary: passing recordDigests to MessageDigest.digest(output,offset,len) lets a provider retain that table. A separate two-process diagnostic executes one new retaining-provider regression against the original and that candidate: original1passed;candidate1failed with the exact private-table isolation assertion. This expected counterexample is not counted as a passing product test or production admission. Its actual receipt is prior/provider-retention-counterexample-proof.json;source/runner/reference bindings are retained.

The final implementation writes provider output into construction-local scratch, then uses the existing copy into private backing. The provider can retain scratch without changing the published table. All four final product JVMs pass this regression. Neither the rejected source nor its provider-array exposure is installed as production code. A prior cross-package reference compilation refusal is also retained;the final exact original is mechanically self-type-renamed to SharedLexiconImageReference within its original package, preserving package-private arena access.

## Executed qualification

The canonical M3HashPinnedJavaSnapshotRecipe at761980021499f9339883de544a48a274cad68371 already admits this exact owner;its registry is not modified. The recipe binds full-source/constructor preimages and afterimages,refuses source drift,and reaches a zero-change fixed point. Eight JUnit methods test replay,missing/duplicate owner,wrong AST,source drift,post-scan mutation,frozen atom and exact owner admission.

The actual emitted source is compiled through the existing Maven/RAM compiler with Java21 --release21 -proc:none -g:none -Xlint:all -Werror. Four fresh before/after/after/before JVMs each execute15JUnit methods:**68JUnit executions total (8recipe+60product)**. All five child stages exit0 and are reaped.

The pinned SharedLexiconImageContractTest source is compiled unchanged. Its image/seal and no-SHA-provider helper methods are called;its full filesystem main is not run. Direct ByteBuffer instances are checked at runtime to be java.nio.DirectByteBuffer/MappedByteBuffer subtypes,then passed to the real private constructor. This is an isolated admission-constructor proof,not a FileChannel.map/open syscall or full filesystem test.

Coverage includes all65536UTF16 units in both image versions (131072unit-versioncases),maximum65536record directories,26padding/boundary words,256seeded images/5200rows,empty/count0/first-record boundaries,dimension/checksum/range/hash/order refusals,post-admission and during-open mutation,missing SHA256 provider with restoration,direct-output provider reset behavior,defaultSPI fallback,provider-output retention,and64concurrent independent constructions.

ASM instrumentation of actual compiled owner bytes observes a256-record image:factory invocations258→3,no-argument result-array digest calls258→2,and caller-output digest calls0→256. Empty images remain2factory/2array-digest calls with no record engine. Timing uses uninstrumented classes. Matching API/field and non-constructor fingerprints bind the unchanged implementation surface.

## Measurements and practical limits

Each fresh JVM records56samples across8scenarios (7each),224total. ThreadMXBean observes constructor allocation. Direct buffers and sealed input images are prepared before measurement;the complete constructor,its metadata arrays,whole-image hashes and record views are measured. Instances escape through a volatile sink. JVMs use -Xbatch and CICompilerCount=2;the observed SHA256 provider is SUN. Table entries are pooled per-call medians from two processes per side.

| Corpus | ns/construction before → after | bytes/construction before → after |
| --- | ---: | ---: |
| 1024-ascii | 689150.00 → 603937.50 | 452160 → 116616 |
| 1024-boundary | 1227312.50 → 1071112.50 | 516608 → 122920 |
| 16384-multilingual | 15293800.00 → 12303900.00 | 7349824 → 1714072 |
| 256-long | 8055575.00 → 8166587.50 | 1164864 → 40824 |
| 64-ascii | 54368.75 → 46009.38 | 32320 → 11656 |
| 65536-ascii | 49509500.00 → 41655700.00 | 28321344 → 6825864 |
| empty | 13212.50 → 11331.25 | 1568 → 1568 |
| one | 11190.63 → 11362.50 | 2000 → 2000 |

The64–65536 short/multilingual-record corpora are about12–20% faster and64–77% lower in allocation. The256-long-record corpus allocates about96% less but takes1.4% longer;one-record construction takes1.5% longer with equal allocation. Empty-path timing variation is uncredited. Full process/sample measurements and explicit tradeoffs remain in ADMISSION_DECISION.json and qualification-proof.json. These are local WindowsJava21 constructor measurements,not universal/no-regression timing or total mapped-file startup claims. A provider using the default SPI output implementation can still allocate internal result arrays.

## Reuse and reproducibility

Production calls existing JCA MessageDigest APIs and existing System.arraycopy;no OpenJDK GPL implementation is copied. Reviewed jdk-21+35 commit890adb6410dab4606a4f26a942aed02fb2f55387 MessageDigest/MessageDigestSpi/DigestBase source identities are retained as provenance. Existing M3/Guava comparison notices are preserved,and the actual M3 Apache-2.0 licence is included. See THIRD_PARTY_NOTICES.md,FOSS_REUSE_DECISION.tsv and ALGORITHM_CATALOGUE.json. No external interview-site implementation or runtime dependency is added.

qualification-input.references.json reconstructs every source-bearing field from this crate. Resolve crate-resource-utf8 recursively,serialize compact ASCII JSON preserving insertion order/no finalLF,and verify input Git blobf94086426035a265562171a063c4fb4bfe09f8aa. qualification.py is the exact executed runner;stdin-qualified.py pins that input. bootstrap/,recipe-test/,production-before/,reference-src/,incumbent-tests/ and proof-src/ retain the source closure. The public M3 mirror needs no private Synexia source fetch to reconstruct the product qualification. A separate reference envelope reconstructs the retention diagnostic.

Execution remains host-bound to existing Java21,Maven3.9.14,47SHA256-pinned offline JARs,RAM compiler and read-only installed Snappy DLL. The runner overrides the legacy POM main. Verify explicit paths/native temp routing/capacity;do not download/copy caches or fall back to another drive. Canonical plugin descriptors/resources are packaged separately;full plugin discovery/reactor was not executed. Rollback is a scoped PR revert.

Authoritative product receipt SHA256:f5c86323e72dcfa9523da84cfb5f5a211e049beeae16ca53e3a493a0a4d7e52e. S retained0files;the545MiB total cap and8GiB reserve were enforced. Read-onlyF is charged571469824B from sealed R230 evidence,explicitly not a fresh whole-tree audit. No local clone,snapshot,cache copy or retained build artifact is created. No fullJDK/nativeABI/ARM/Android/macOS/ESP/Pico proof or whole-lexicon completion is claimed. The broader goal and storage protection stay active.
