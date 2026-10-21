# M3FMT shared immutable lexicon — source-sealed Java/JNI packet (2026-10-21)

**Product:** M3JDK21, one Java/HotSpot/JNI SDK. **Canonical donor/recipe:** Synexia.
M3FMT is a product-owned file format under the existing M3File package, not a
second String owner, third-party runtime service, separate filesystem or JDK wrapper.

## Implemented bounded slice

- Catalog-scoped 64-bit frozen word IDs plus byte-exact literal spans and sorted metadata.
- Deterministic big-endian envelope, canonical unsigned LEB128 ID lane, SHA-256 envelope
  and content roots, exact catalog identity check, bounded lengths and canonical decoding.
- Existing Synexia mapped shared catalog binding after catalog/image hash validation;
  target-owned frozen M3FileDictionary binding recomputes the full ordered lexicon root.
- JNI C17 bulk unsigned-varint pack (Java is oracle, optional native loading); copy/region
  JNI calls never retain Java heap pointers or call into user code.
- UTF-8 decoder is strict for metadata; binary literal lane preserves arbitrary source bytes.
  No global OS string interning is claimed. No text is transmitted for known word IDs.
- Existing public JDK21 java.lang.String/java.util APIs are unchanged; no modified ABI.

## Executed isolated finite proof

Host JDK21 `javac --release 21 -Xlint:all -Werror`, standalone source codec and
contract fixture: **441 checks, Java-only PASS** and **441 checks, JNI-enabled PASS**.
JNI C compiled with `gcc -std=c17 -Wall -Wextra -Wpedantic -Werror`, native library
loaded through `System.loadLibrary`, second Java process read authenticated frames.
These are finite fixture gates, not an installed M3JDK21 runtime/jtreg proof.
The added target frozen-dictionary contract and exact OpenRewrite SDK recipe
are **PENDING execution**; source presence is not their execution proof.

## Recipe authority

Named `com.synexia.m3jdk.M3FmtSharedLexicon20261021`.
Existing `M3HashPinnedJavaSnapshotRecipe` and `M3HashPinnedTextSnapshotRecipe`
hold exact ABSENT-before target admissions and SHA-256 checked target templates.
Maven 21 crate: `synexia-openrewrite-recipes/recipe-crates/m3fmt-shared-lexicon-20261021`.
Source seals cover five new Java files and one new native C file.
An existing postimage is a fixed point; unexpected occupied additions, drift,
duplicate or malformed input must be refused. The source packet is candidate-only.

## Open gates

- Actual OpenRewrite/Maven Recipe.run and fixed-point/refusal tests not executed.
- Complete m3/ports/file reactor / com.m3.file module build not executed.
- Full target native library packaging, Java21 generated JNI header/ABI and
  Linux/Windows/Android matched-image runtime proof pending.
- JDK jtreg, GC/CDS, mutation races, hostile-file TOCTOU and huge-file budgets pending.
- Original M3FileSnapshotCodec and delta/event range integration pending.
- Compiler-derived static regex precompute and exhaustive strings × regex × flags
  trials remain in the existing Synexia mastery engine; not implemented here.
- Filtered/sorted collections are separately handled by existing array/mask owner PRs.
- Cold-preparation/warm-reuse CPU, heap, native memory and network-wire-size benchmarks pending.

No receiving stage advanced; String -> arrays -> collections remains the target policy.
No LeetCode/HackerRank/GeeksforGeeks source, editorial or submission body was copied.
Apache-2.0 first-party provenance and original upstream notices are preserved.
