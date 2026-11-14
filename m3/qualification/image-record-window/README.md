# Constructor-local record windows (R238)

This refines existing Synexia#17394/M3JDK21#3017. Every R237 source/recipe/proof resource is retained unchanged. The current production afterimage composes two bounded transformations in the same SharedLexiconImage constructor:original→localSHA256/scratchcopy→localreadonlyrecordwindow. No new registry admission,fields,public API,formats,IDs,lookup/copy methods,retained cache/node or native implementation.

The window is created lazily after the first record's range/hash/order validation. Each record independently computes its byte bounds from the already-validated directory,then rewinds the separate view,sets its limit to the validated end and its position to the validated begin. Rewind discards the view's mark without restoring its limit to capacity. SHA256 consumes the remaining bytes. The prior constructor-local32-byte scratch and copy into private recordDigests stay intact.

## Actual recipe composition and proof

Two Maven/OpenRewrite source recipe stages execute8JUnit methods each. Stage1 replays the existing m3-image-record-digest recipe;its emitted bytes become stage2's checked preimage. Stage2 replays m3-image-record-window. Both verify exact source/atom hashes,AST ownership,drift refusal and fixed point. The actual two outputs are compiled in the product runs. The original two-step declarative YAML chain was tested and refused by strict scanner preimages before visitor composition. Its real failed receipt is retained. The public M3ImageAdmissionPrecomputeR238 entry point now uses the same existing hash-pinned recipe with a cumulative original-to-final constructor manifest;the independently proved stage manifests remain unchanged. Four extra JUnit tests load the actual YAML through YamlResourceLoader/Environment and prove final source,final fixed point,drift refusal and the standalone window recipe. Total passing executions:122 (118stage/product+4YAML). This proves the declared YAML entry point in the isolated Maven/RAM runtime;the full repository Maven plugin reactor/discovery was not run.

Six fresh product JVMs run original,digest-only,combined,combined,digest-only,original. Each executes17JUnit methods:**118stage/product JUnit executions (16recipe+102product),plus4YAML=122total**. Eight stage/product children and one additional YAML child exit0 and are reaped. Public/field and all non-constructor bytecode fingerprints match across every owner variant. Source identities:original570a22f733499729bdc0fe48813f755e52ebe963,digestbd331777fd17c85d64ecfcf6e5df4cbdaab58d50,combined4c8e6f8b173a84afa67d85abb04823386dbc363b.

Coverage retains all65536UTF16 units in bothversions/131072unit-version cases,65536-record limit,256seededimages/5200rows,26padding words,64concurrent constructions,default/direct/missing/retaining providers,and existing corruption/refusal checks. It adds22valid physical layouts across both versions and6invalid beyond-logical-limit/overflow counterparts. Valid directories include backward offsets,overlap,containment,gaps,empty spans at zero/interior/end,and zero records with nonempty payload. The tested source's ordering constrains logical record text,not physical offset order.

A provider captures exact constructor input bytes and retains view references. Tests verify readonly content,restricted logical limit with extra capacity,sliced backing at nonzero physical offset,nonzero source position/mark,5distinct record windows before versus1after,and unchanged owner cursor/table/record copies after retained cursor edits and provider removal. The provider's readonly assertion applies to constructor inputs;copyRecord's unchanged separate heap-buffer digest runs after provider removal. The initial over-broad QA assertion failure is retained under prior/.

Both per-record arrays and cursor boundaries matter:readonly views share backing and can escape to a provider. They do not make mapped files OS-immutable or hide bytes within capacity. The independent view protects owner cursor state;the existing output scratch copy protects private precompute backing. The R237 direct-table ownership counterexample and fix remain unchanged.

## Executed work/allocation measurements

ASM instrumentation of actual compiled owner bytes on256records observes:
- SHA factories258 /3 /3 for original/digest/combined;empty remains2.
- Per-record recordBytes calls256 /256 /0.
- Bounded slice calls260 /260 /4,including4whole-image slices that remain.
- Readonly-view calls4 /4 /5:the extra view is reused for allrecords.

All timings use uninstrumented classes. Each JVM records56samples (8corpora×7),336total. Two fresh processes per variant use -Xbatch/CICompilerCount2;allocation is measured with ThreadMXBean. Sealed direct input buffers are prepared outside measured intervals;the complete constructor and its private arrays are measured. The observed SHA provider is SUN. The numbers below are pooled per-call medians.

| Corpus | ns original / digest / combined | bytes original / digest / combined |
| --- | ---: | ---: |
| 1024-ascii | 724412.50 / 587775.00 / 585462.25 | 452160 / 116616 / 51144 |
| 1024-boundary | 1148712.25 / 1102637.38 / 976612.50 | 516608 / 122920 / 57448 |
| 16384-multilingual | 16659850.00 / 11163400.00 / 10434450.50 | 7349824 / 1714072 / 665560 |
| 256-long | 7183912.38 / 6899625.25 / 6907025.13 | 1164864 / 40824 / 24504 |
| 64-ascii | 53204.69 / 45665.64 / 45399.98 | 32320 / 11656 / 7624 |
| 65536-ascii | 50420500.00 / 40043650.50 / 37722200.50 | 28321344 / 6825864 / 2631624 |
| empty | 11068.78 / 11328.14 / 11425.00 | 1568 / 1568 / 1568 |
| one | 16660.94 / 18242.19 / 11981.25 | 2000 / 2000 / 2000 |

For65536ASCII records,the combined path reduces allocation28.32→2.63MB versusoriginal (about91%),or6.83→2.63MB versusdigest-only (about61%). Time50.42→37.72ms is about25% lower thanoriginal;40.04→37.72ms is about5.8% lower thandigest-only. Multilingual and boundary corpora also improve in this run. Long-record timing is about0.11% higher thandigest-only,empty about0.86% higher. Zero/one-record allocation is unchanged;large single-record timing differences are not credited as guaranteed window-reuse gains. These are local WindowsJava21 measurements,not universal/no-regression guarantees or native/mapped-file startup measurements. DefaultSPI implementations may still copy or allocate internally.

The earlier clear/position/limit variant also passed118JUnit but regressed in several timing cases. Full source,receipt and metrics are retained under prior/;rewind/limit/position is the selected variant. ADMISSION_DECISION.json and ALGORITHM_CATALOGUE.json preserve the selection rationale and all3variant measurements.

## Reuse, reproduction and limits

OpenJDK jdk-21+35 commit890adb6410dab4606a4f26a942aed02fb2f55387 Buffer/ByteBuffer-template/MessageDigest/MessageDigestSpi/JCAUtil contracts provide the existing APIs;exact source identities are provenance. No GPLimplementation is copied. Existing M3/Guava Apache-2.0 notices and the actual M3 licence remain. See THIRD_PARTY_NOTICES.md and FOSS_REUSE_DECISION.tsv.

qualification-input.references.json reconstructs every source-bearing input from this crate. Resolve crate-resource-utf8 recursively;compact ASCII JSON,preserve insertion order,no finalLF;verify sealed input53faa54b2f2f73da1bf29afd973fad27390521c7. qualification.py and stdin-qualified.py are the exact runner/input loader. The unchanged original reference and pinned incumbent contract source are included;only its image/seal/provider helpers execute,not its filesystem main.

Execution is host-bound to existing Java21,Maven3.9.14,47SHA256-pinned offlineJARs,RAM compiler and read-only installed Snappy DLL. The legacy POM main is overridden. Verify paths/native temp/capacity before use;do not download/copy caches or fall back to another drive. Actual private constructors use verified directByteBuffer/MappedByteBuffer subtypes;FileChannel.map/open syscalls/fullfilesystem/JDKreactor/JNI/ARM/Android/macOS/ESP/Pico suites were not run. Rollback can revert the R238 refinement while retaining the prior R237 commit.

Authoritative product receipt SHA256:5dec8e4859f5bb361d0734ed687e5f24ff95080e267864da534010b700401c11. S retained0files;545MiBcap and8GiBreserve held. Read-onlyF charge571469824B comes from sealed R230 evidence,not a fresh total audit. No local clone,snapshot,cache copy or retained build artefact is created. Native-cap proposal remains unused/unanswered. Fulllexicon/LangDex/Rapidex/migration/platform goal stays active.

The YAML input is sealed as 1d0dbe34bc1524e6974a199f4940c146b9832ab2;its source-complete references,runner,test,actual descriptors and PASS receipt are under yaml-proof/. Receipt SHA256:31e3581ca831e6596143de1bbabc7ce5f3473568db182b01c5f00f8e74f3f059. No production source changed during the YAML repair.
