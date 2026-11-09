# Executed local evidence — 2026-11-02

## Exact identity and scope

Original original-owner `PmemCorpusSearch.java` remains **byte-for-byte unchanged**:

- Existing SHA-256 `3493a0f911023d8863d86cdf1b8f19c08d25d4aa5a76be8cfacb3bdc1bc64deb`.
- Prior Git blob `31f6ab6e419033630c6b09a8700c6a064dea6339`.
- `javap -public` before and after compares identical, and compiled `PmemCorpusSearch.class` SHA-256 matches exactly (`2739b19787ce677425e91695214b14251a1953a2be7d8937b67abe9899a8ae64`) under the restricted source/stub build.
- Existing JNI C17 files and exported symbols are not changed by the proposed patch.

Two originally absent source files:

| Path | SHA-256 | Git blob |
|---|---|---|
| `m3-pmem-sdk/campaign/src/main/java/com/synexia/m3/pmem/campaign/PmemCorpusSelectedSearch.java` | `a88eb92d8cf00411171d20648f90ce51c7b472f6604d29073bcd82d976f9628f` | `5b4d1a8b935c97fdda93fddd57e9c4ef715829da` |
| `m3-pmem-sdk/campaign/src/test/java/com/synexia/m3/pmem/campaign/PmemCorpusSelectedJUnitTest.java` | `5cec3d16e533b6630598b35dfa61c0ce74ec43f1083b6d6c492fe3ca7696ebb3` | `7c58d0d2004ce23a339a01705cc10935125d601f` |

The canonical `M3HashPinnedJavaSnapshotRecipe` manifest and source templates exactly match those bytes. Recipe assets are committed first in the two-commit patch.

## Executed Java 21 (restricted stand-ins)

- OpenJDK `21.0.12.1`.
- `javac --release 21 -Xlint:all -Werror` compiled original unchanged owner, new package-private class and independent brute-force test.
- JIT and interpreted (`java -Xint`) 64 MiB heap outputs match exactly:
  - `PASS selected-scan Java21; assertions=3167 corpus=20000 selected=400 hits=35`
  - `PASS PmemCorpusSearch strict Java21; assertions=2775 files=20000 hits=2758`
- Repeated randomized selected subsets validate exact first-hit byte positions, selection and pattern identity, source generation binding, early-stop completeness, zero/empty/UTF-8/binary query, invalid-index preflight, defensive-copy and cancellation.
- `PmemCorpusSelectedJUnitTest` compiles under strict Java21 and four authored test methods were invoked against API-compatible FILE/JUnit stand-ins. The optional test created **20,000 physical tiny Java files**; this does not constitute genuine PMEMSDK FILE/JNI validation or the JUnit Jupiter engine.
- XML POM and both workflow/recipe YAML files were parsed successfully.

## Unexecuted acceptance gates (HOLD)

1. Real Maven + OpenRewrite SDK `Recipe.run`, original pipeline first pass, fixed point, occupied/hostile path refusal, Surefire/JUnit engine.
2. Original PMEMSDK FILE and POSIX/JNI native suite (compiler, JMOD/jlink), sanitizer, JNI parity and 64KiB boundary cases.
3. Full canonical source provenance/history-loss audit and actual public-fork result-only integration.
4. Actual 1 GiB mapped-source RSS and throughput, complete quality plugin receipts, Chrome on-device model context and 20,000 real model calls.
5. GitHub PR creation/push: **blocked at authoring time by GitHub REST 403 API rate limit**, not reported as completed.

The local proof is deliberately a source-and-algorithm oracle with stand-in holders. Tests are not falsely attributed to real native/JUnit execution.
