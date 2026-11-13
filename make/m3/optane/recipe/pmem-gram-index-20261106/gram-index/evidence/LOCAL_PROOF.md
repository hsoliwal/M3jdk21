<!-- SPDX-License-Identifier: Apache-2.0 -->
# Verified local Java21/C17 evidence — 2026-11-02

## Frozen source, ownership and recipe custody

- Input branch: local exact replica of PMEMSDK selected-search additive patch, based on public-first-hit source identity from Synexia PR #15383 (remote follow-up still unverified).
- Original public `PmemCorpusSearch.java` SHA-256: `3493a0f911023d8863d86cdf1b8f19c08d25d4aa5a76be8cfacb3bdc1bc64deb`.
- Existing package-private `PmemCorpusSelectedSearch.java` SHA-256: `a88eb92d8cf00411171d20648f90ce51c7b472f6604d29073bcd82d976f9628f`.
- NEW package-private `PmemCorpusGramIndex.java` SHA-256: `523a4a541952c163caf6614583d92421a35193b186656ff33b5dedccba0b6174`.
- NEW JUnit source SHA-256: `29110712516b787265c19a2622a37edcf07265dccbb87287e834782b79e29cc9`.
- The new Java and JUnit postimages exactly match their source SHA-256 recipe templates; original public and JNI owners have no changed bytes.
- The canonical `M3HashPinnedJavaSnapshotRecipe` in Synexia owns two ABSENT-before target paths. Its real Maven/OpenRewrite `Recipe.run` proof is **authored, NOT executed** in this environment.

## Checks actually executed, not simulated as full product

- OpenJDK 21.0.12.1, strict `javac --release 21 -Xlint:all -Werror` on exact owner bytes and independent fixtures: **PASS**.
- `PmemGramIndexSmoke` over 20,000 seeded byte arrays: **16,961 assertions PASS**, Java JIT, Java `-Xint`, checked C17 JNI brute-force oracle in JIT and `-Xint`; all four logs are byte-identical.
- Source seal: every input file is SHA-256 checked twice before the immutable index becomes visible. No source bytes are retained in the index. Candidate prefilter uses the rarest necessary binary byte pair; on seeded data `18,163` of `20,000` files were eliminated before the exact matcher; **all 1,819 expected hits were retained**. For this particular small random corpus, retained primitive storage was **4,478,362 bytes**.
- The unchanged source-search baseline compiled and passed `2,775` assertions. The selected-search baseline passed `3,167` assertions. Both original Java source files are unchanged.
- Four authored JUnit-shaped test methods compiled and ran against **API-compatible local FILE/JUnit stand-ins** (including 20,000 physical small source files): PASS. **Not** actual Jupiter/Maven nor production FILE mapping.
- The separate `verify-mmap-1gb.sh` test created one REAL sparse **1,000,000,000-byte** FILE mapping, arranged as 20,000 logical 50,000-byte slices. With Java heap `-Xmx128m`, every source slice was checked in two serial passes. It returned all `1,819` expected hits and pruned `18,181` other files, with **407,614 bytes** retained for primitive index lanes in this nearly all-zero corpus. One observed wall run: **5.90 seconds**, process peak RSS **1,060,572 KiB**. Those figures cover only this sparse test file, not source text, compilation/quality plugins or model inference. The file was deleted after the test.
- C17 JNI test code uses an independent brute-force matcher, with checked JNI array release and binary/Unicode/sorted selected-query oracle cases; it is a **test-only JNI library**, not a new production PMEMSDK native export or an executed original native SDK.

## Pending / HOLD — no implied deployment

- Real Maven/OpenRewrite SDK first-pass and fixed-point test, including stale/occupied output refusal.
- Real Jupiter execution against the original source SDK, real `PmemBackend.FILE` and `PmemBackend.POSIX`/PMDK, native JNI/C17/JMOD, jlink, UBSan/ASan and test-receiver ABI parity.
- True 20,000 *50 KB* Java source corpus with realistic bigram distributions; distinct cold/warm workload profiles, CPU/NUMA placement and peak memory/throughput figures.
- Actual Chrome local model admission and **20,000 model calls**, complete source-bound quality-plugin reports, M3AST semantic atom/contract checks, and original Maven recipe-generated source changes.
- Full canonical Synexia history/provenance and receiving results-only gates in `hsoliwal/M3jdk21`, Eclipse and any other product target.
- A NEW remote GitHub PR for this patch cannot be claimed while the connected account receives **HTTP 403 rate-limit exceeded**. Local Git history and two-commit patch are the current output.

## Reproduction (from a complete compatible Synexia checkout)

```bash
bash synexia-openrewrite-recipes/recipe-crates/m3-pmem-gram-index-20261102/verify-java21-jni.sh
bash synexia-openrewrite-recipes/recipe-crates/m3-pmem-gram-index-20261102/verify-mmap-1gb.sh
M3_USE_PMDK=off mvn -B -ntp -f m3-pmem-sdk/pom.xml verify
mvn -B -ntp -f m3-pmem-sdk/campaign/pom.xml -Dtest=PmemCorpusGramIndexJUnitTest -Dpmem.gram.native=true test
mvn -B -ntp -f synexia-openrewrite-recipes/recipe-crates/m3-pmem-gram-index-20261102/pom.xml verify
mvn -B -ntp -f m3-pmem-sdk/campaign/pom.xml -Dtest=PmemCorpusGramIndexJUnitTest -Dpmem.gram.scale=true test
```

Passes in test fixtures never authorize source change or donor code copying. Keep every receiving PR DRAFT/HOLD until real engine/source-native gates and provenance complete.
