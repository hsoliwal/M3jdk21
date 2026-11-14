# Fixed-alphabet payload Unicode escaping (R235)

M3PrecomputePayload's private appendString previously called String.format for each generic Unicode escape. The qualified recipe replaces that formatter with four direct nibble lookups into the immutable 16-character lowercase hexadecimal String alphabet. This follows Jackson's nibble-emission pattern without taking over its serializer or escape policy. No new fields, arrays, cache owner, dependency or public API are introduced.

## Contract

The exact existing output remains: named quote/backslash/control escapes, raw slash, raw DEL(U+007f), four lowercase hexadecimal digits for other controls and every unit above U+007f, including lone surrogates. This changes no Unicode metadata admission/normalization policy. appendString returns the same builder, retains existing prefix and adds an opening quote before a null-value failure. Public canonicalJson still leaves its first admission render unretained, caches backing on the second render and returns fresh API String wrappers. All other methods and fields remain bytecode-identical under the qualifier's fingerprint.

Java 21's [Formatter specification](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Formatter.html) says hexadecimal integer conversion does not apply localization. Tests still compare ten FORMAT locales, with restoration in finally. The canonical parser and explicit refusal diagnostics remain unchanged.

## Recipe and executed proof

The canonical M3HashPinnedJavaSnapshotRecipe source blob 761980021499f9339883de544a48a274cad68371 already admits this exact target. Its four-source engine closure is retained in bootstrap/recipe-engine/. The m3-payload-hex-nibbles resource binds exact full-source/atom preimages and afterimages. Eight JUnit methods verify replay, zero-change fixed point, missing/duplicate owner, wrong AST, source drift, post-scan mutation, frozen atom and exact existing owner admission. No registry expansion is needed.

The actual emitted source is compiled with Java 21 --release21 -proc:none -g:none -Xlint:all -Werror. Four fresh JVMs execute before/after/after/before: 14 JUnit methods each. Three methods invoke unchanged M3PrecomputePayloadTest (16 internal checks), ParserContractTest (102) and FieldPatternTest (132,072). Those 132,190 internal checks per JVM are separate from the 64 total JUnit executions (8 recipe + 56 product); they are not double-counted as JUnit tests.

Coverage includes every 65,536 UTF16 code unit through the actual private method and public canonical round trip, 4,096 seeded full-UTF16 strings, ten locales / 60 samples, malformed-surrogate and chunk composition, rawDEL and exact lowercase escaping, null/partial builder mutation, same-builder returns, typed fields/defensive arrays, fresh API String identities and cache phases, 8,192 concurrent reads, matching public/field and non-append method fingerprints. The actual compiled Jackson 2.16.1 lowercase alphabet is compared across all 65,536 nibble combinations. ASM instrumentation on a separate classloader observes 4 formatter calls before and 0 after for 4 generic escape units; ordinary ASCII remains 0. Timings use uninstrumented owner classes.

## Measurements

Each JVM records 168 samples (672 total): append/first/second/warm operations, six corpora, seven samples each. Append uses a capacity-prepared builder. Cold and second-render payload arrays are prepared outside measured intervals; second-render preparation performs the first render beforehand. Warm measurements use an already-cached payload. Public result wrappers escape through a volatile sink; allocation is observed with ThreadMXBean. JVM compilation uses -Xbatch and CICompilerCount=2.

Values are per-call medians pooled across two fresh processes per side. These are local Windows Java 21 measurements, not a universal timing guarantee.

| Operation/corpus | ns/call before → after | bytes/call before → after |
| --- | ---: | ---: |
| append/ascii | 691.5 → 641.5 | 0 → 0 |
| append/latin1 | 34861.5 → 1150.4 | 18944 → 0 |
| append/multilingual | 22299.8 → 726.2 | 11840 → 0 |
| append/supplementary | 40007.5 → 1211.5 | 21312 → 0 |
| append/unpaired | 17219.1 → 722.7 | 9472 → 0 |
| append/controls | 10961.6 → 1270.4 | 5184 → 0 |
| first/ascii | 1225.8 → 1142.1 | 328 → 328 |
| first/latin1 | 34994.8 → 1546.4 | 19928 → 984 |
| first/multilingual | 21712.1 → 1307.6 | 12440 → 600 |
| first/supplementary | 42706.1 → 1994.4 | 22320 → 1008 |
| first/unpaired | 18136.2 → 1197.9 | 10056 → 584 |
| first/controls | 10961.6 → 1749.2 | 5760 → 576 |
| second/ascii | 1323.3 → 1149.0 | 320 → 320 |
| second/latin1 | 34431.3 → 1938.9 | 19920 → 976 |
| second/multilingual | 22265.7 → 1318.5 | 12432 → 592 |
| second/supplementary | 40637.5 → 2010.2 | 22312 → 1000 |
| second/unpaired | 17902.4 → 1129.2 | 10048 → 576 |
| second/controls | 11595.0 → 1705.2 | 5752 → 568 |

All 24 operation/corpus answers match across runs. Direct append allocates 0 B afterimage; escaping still needs public output storage in cold renders. Warm rendering retains 24 B/call on both sides, and its implementation is unchanged. Warm timing variation is recorded in the receipt and is not credited to this optimization. Full per-process/sample measurements are retained in qualification-proof.json; selection and medians are in ADMISSION_DECISION.json.

## Donor and notices

FasterXML/jackson-core commit 249716c8c2b141e4cab646f9d161fda0da97ae03 (release 2.16.1), JsonStringEncoder._appendByte and CharTypes.HClower/copyHexChars(false), provides the direct-nibble/fixed-alphabet mechanism. Both complete source files and Apache-2.0 LICENSE are retained. The pre-existing pinned Jackson 2.16.1 qualification JAR supplies the executed alphabet and its META-INF/NOTICE and FastDoubleParser-NOTICE, copied verbatim in RAM to the package. No JAR or FastDoubleParser implementation is redistributed. Source-root and source META-INF notice probes were absent; NOTICE_PROVENANCE.json records the actual JAR binding. Production remains JDK-only. New authored work retains Copyright 2026 Hitesh Soliwal, Synexia, Apache-2.0. See THIRD_PARTY_NOTICES.md and FOSS_REUSE_DECISION.tsv.

## Source-complete reproduction and limits

qualification.py is the exact executed Maven/RAM runner. qualification-input.references.json reconstructs every source-bearing field from files inside this crate. Resolve crate-resource-utf8 recursively, serialize compact ASCII JSON with original insertion order and no finalLF, and verify sealed input Git blob 189aaf83dda0689415d2f288f7791ba563f95794. stdin-qualified.py accepts its 1024-character hex lines and pins the exact input. bootstrap/ includes the existing RAM compiler, POM and canonical recipe sources; recipe-test/, incumbent-tests/ and proof-src/ contain the complete tested source. The public M3 mirror needs no private Synexia source fetch.

Execution is deliberately host-bound: existing Java 21, Maven 3.9.14, 47 exact SHA256-pinned offline JARs and installed read-only Snappy DLL are prerequisites. The legacy POM main is overridden by qualification.py to the RAM compiler. Verify the runner's explicit paths, native temp routing and capacity gates before invoking; do not download/copy caches or fall back on another drive. This is not a portable one-command full-JDK build. Canonical plugin YAML/resources are packaged separately from the source replay tests; full reactor/plugin discovery was not executed. Source drift refuses; repeated replay is a fixed point. Rollback is a revert of this scoped PR.

The authoritative executed receipt SHA256 is 8f04011255cfb2a02b00017d1f79b9783d7fcdb7594d5df5dd57a98fb7657b1c. This proves one existing private serialization atom, not all lexicon migration, corpus admission, native ABI, full JDK image/reactor or ARM/Android/macOS/ESP/Pico behavior. R234's instance-comparator PRs remain separate. Read-only agent findings are preserved in INVENTORY_AGENTS.json; their source inventory is not presented as executed tests.

All 5 child stages exited 0 and were reaped. S retained 0 files before/after; approved 545 MiB total task cap and 8 GiB reserve were enforced. Read-only F is charged 571,469,824 B from the sealed R230 receipt, explicitly not a fresh audit (the full audit timed out previously). No clones, snapshots, cache copies or local retained build artifacts were created. The native cap proposal remains unanswered/unused and full optimization goal remains active.
