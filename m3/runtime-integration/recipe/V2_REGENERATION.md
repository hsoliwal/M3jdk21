<!-- SPDX-License-Identifier: Apache-2.0 -->
# Runtime integration recipe v2 (2026-10-07)

`runtime.patch` and `manifest.json` were regenerated against the current M3JDK21 master so the
installer describes the shipped `java.lang.M3String` runtime instead of the retired
`MIndexString` image.

| field | v1 (retired by this regeneration) | v2 |
| --- | --- | --- |
| schema | implicit | `M3_RUNTIME_INTEGRATION_RECIPE_V2` |
| preimage | `base_commit 3dbb233235` (MIndexString era) | upstream `890adb6410` (jdk-21+35) for every file |
| postimage | MIndexString layout + `superseded` custody lists | exact master image, no `superseded` lists |
| targets | 25 (incl. deleted `MIndexString*.java`) | 64 |
| patch | 155,379 bytes | 562,119 bytes, LF only, `--full-index --binary` |

Scope rule (CPU, `R1_SCOPE_CLASSIFICATION.tsv` in the apex run): every `src/**` file whose
upstream-to-master delta carries an `M3`/`mindex`/`UseM3StringStorage` token (60 files) plus the
four token-free String-runtime hooks (`stringDedup.cpp`, `jvmtiTagMap.cpp`, `finalizerService.cpp`,
`StringConcatHelper.java`). JEP/backport files owned by other hash-pinned crates (ML-KEM, ML-DSA,
Stream Gatherers, JEP 458, security properties, javadoc/jcmd/JFR backports) are excluded.

Proof (`test_recipe.py`, 8 tests): manifest shape; current tree is the exact after-image and a fixed
point; every target hash matches; `--reverse` restores every upstream preimage (added files removed)
and re-apply returns to the exact after-image; patch bytes are content-addressed and CR-free; every
per-file drift is refused without partial writes; a mixed before/after tree is refused; symlink
targets are refused. The former `superseded` entries (jni.cpp, javaClasses.*, String.java, ...)
are folded into the exact after-images; `JNI_SUPERSESSION.md` remains the custody history.
`.gitattributes` marks `*.patch` under this directory `-whitespace` because unified-diff blank
context lines are a single space by format.

## 2026-10-08 re-seal (A1 M3TQ general-pattern gate)

Regenerated after `java/util/regex/M3PatternQuery.java` (new, M3 token) joined the scope and
`Pattern.java` changed: 65 targets. The same regeneration absorbs four `java/lang` after-images
that had drifted on master since the previous re-seal (`AbstractStringBuilder.java`,
`M3StringSearchPrecompute.java`, `String.java`, `StringConcatHelper.java`: String receiver merges
of 2026-10-08), so `apply.py --check` and `test_recipe.py` are green again on this tree. Same
upstream preimage `890adb6410` (jdk-21+35); `target_commit` is the owner commit of the gate.

## 2026-10-08 re-seal (A2 String.split literal lane)

Regenerated after `String.java` and `M3String.java` changed (65 targets unchanged in scope; `target_commit` c2eca9406b). `test_recipe.py` needs `TMP`/`TEMP` on a drive with free space.


## 2026-10-08 re-seal (A3 non-boxing concat combinator)

Regenerated after `StringConcatFactory.java` and `StringConcatHelper.java` changed (65 targets; `target_commit` ec0ead0e3f).

## 2026-10-08 re-seal (A7 invariant 6)

Regenerated after `M3StringPositionPrecompute.java` changed (65 targets; `target_commit` cab2f8c0ef).

## 2026-10-08 re-seal (A4 facts gates for mixed-side consumers)

Regenerated after `M3StringFacts.java` and `String.java` changed (`target_commit` 0d0cc24b51). The same regeneration seals `java/lang/M3StringCodePointPrecompute.java`, an M3-token file that had joined master (code-point geometry receiver) without a re-seal: 66 targets; `test_recipe.py` now expects 66.

## 2026-10-09 re-seal (A8 fused facts scan, A9 equality gates, A6 pool bounds re-applied on master)

The three leaves had reached master only as zero-file superset merges (62153d40f3f, 9660212fce), so their owner and receiver commits were cherry-picked onto master and the binary `M3String.join(String, String)` made refusal-safe (`target_commit` 3c3d66694a). Upstream files stay strictly sealed: the `String.java` and `StringConcatHelper.java` after-images and their `runtime.patch` hunks were regenerated from a synthetic tree (v2 after-state of the other 64 targets plus those two files from the target), so every other hunk is byte-identical to the previous seal. The M3-owned `M3String.java`, `M3StringFacts.java` and `M3StringPool.java` keep their v2 after-images and record the new owners as reviewed `superseded` hashes (`JNI_SUPERSESSION.md`). 66 targets; `apply.py --check` state=superseded; `test_recipe.py` 6/6.

## 2026-10-09 re-seal (A10 mixed-side comparison windows)

Regenerated against 1b68815ddb: `java/lang/M3StringMixedCompare.java` joins the scope (67 targets, `before` null, recorded as its own `superseded` owner per the supersession rule); the upstream `String.java` after-image and hunks were regenerated from a synthetic after-state tree (every other hunk byte-identical); the M3-owned `M3String.java` keeps its v2 after-image and records the new owner as a reviewed `superseded` hash. `apply.py --check` state=superseded; `test_recipe.py` 6/6 (target count 67).

## 2026-10-09 re-seal (A11 atom bulk I/O)

Regenerated against 057e92ecb3: the upstream `String.java` after-image and hunks were regenerated from a synthetic after-state tree (every other hunk byte-identical); the M3-owned `M3String.java`, `M3StringAtom.java`, `M3StringOwner.java`, `M3StringTuple.java` and `M3StringMixedCompare.java` keep their v2 after-images and record the new owners as reviewed `superseded` hashes. 67 targets; `apply.py --check` state=superseded; `test_recipe.py` 6/6.

## 2026-10-09 re-seal (A12 M3-on-M3 in-place comparison)

Regenerated against 85b27faba2: no upstream target changed, so `runtime.patch` and every `after` image are untouched; the M3-owned `M3String.java`, `M3StringAtom.java` and `M3StringMixedCompare.java` record their new owners as reviewed `superseded` hashes. 67 targets; `apply.py --check` state=superseded; `test_recipe.py` 6/6.

## 2026-10-09 re-seal (A13 encode from bulk-read units)

Regenerated against 2e2cda5dc9: no upstream target changed, so `runtime.patch` and every `after` image are untouched; the M3-owned `M3String.java` records its new owner as a reviewed `superseded` hash. 67 targets; `apply.py --check` state=superseded; `test_recipe.py` 6/6.

## 2026-10-09 re-seal (A17 builder compare in place and streams over the bulk-read value)

Regenerated against 9fb43814ef: the upstream `String.java` after-image and hunks were regenerated from a synthetic after-state tree (every other hunk byte-identical); the M3-owned `M3String.java` keeps its v2 after-image and records the new owner as a reviewed `superseded` hash. 67 targets; `apply.py --check` state=superseded; `test_recipe.py` 6/6.

## 2026-10-09 re-seal (A18 regex literal gate over the subject's precompute)

Regenerated against c3b9a15ec4: the upstream `System.java`, `Matcher.java` and `JavaLangAccess.java` after-images and hunks were regenerated from a synthetic after-state tree (every other hunk byte-identical); the M3-owned `M3StringSearchPrecompute.java` keeps its v2 after-image and records the new owner as a reviewed `superseded` hash. 67 targets; `apply.py --check` state=superseded; `test_recipe.py` 6/6.

## 2026-10-09 re-seal (A19 trigram facts counting sort)

Regenerated against e0305ecf9c: no upstream target changed, so `runtime.patch` and every `after` image are untouched; the M3-owned `M3TQ.java` records its new owner as a reviewed `superseded` hash. 67 targets; `apply.py --check` state=superseded; `test_recipe.py` 6/6.

## 2026-10-09 re-seal (A20 regex subject view)

Regenerated against 53fc3b164b: `java/lang/M3StringMatchText.java` joins the scope (68 targets, `before` null, recorded as its own `superseded` owner); the upstream `System.java`, `Matcher.java` and `JavaLangAccess.java` after-images and hunks were regenerated from a synthetic after-state tree (every other hunk byte-identical). `apply.py --check` state=superseded; `test_recipe.py` 6/6 (target count 68).

## 2026-10-09 re-seal (A21 M3 needles read once for flat receivers)

Regenerated against 3ec8389d58: the upstream `String.java` after-image and hunks were regenerated from a synthetic after-state tree (every other hunk byte-identical); the M3-owned `M3String.java` and `M3StringSearchPrecompute.java` keep their v2 after-images and record the new owner as reviewed `superseded` hashes. 68 targets; `apply.py --check` state=superseded; `test_recipe.py` 6/6.

## 2026-10-09 re-seal (A22 builder appends of M3 Strings)

Regenerated against 13f7351705: the upstream `AbstractStringBuilder.java` after-image and hunks were regenerated from a synthetic after-state tree (every other hunk byte-identical); the M3-owned `M3String.java` and `M3StringAtom.java` keep their v2 after-images and record the new owner as reviewed `superseded` hashes. 68 targets; `apply.py --check` state=superseded; `test_recipe.py` 6/6.

## 2026-10-09 re-seal (A23 range hash without facts)

Regenerated against 8a4091fcdc: no upstream target changed, so `runtime.patch` and every `after` image are untouched; the M3-owned `M3String.java` records its new owner as a reviewed `superseded` hash. 68 targets; `apply.py --check` state=superseded; `test_recipe.py` 6/6.

## 2026-10-09 re-seal (A24 one-field consumers from bulk lanes)

Regenerated against 68775c6e7d: the upstream `String.java` after-image and hunks were regenerated from a synthetic after-state tree (every other hunk byte-identical); the M3-owned `M3String.java` and `M3StringCodePointPrecompute.java` keep their v2 after-images and record the new owner as reviewed `superseded` hashes. 68 targets; `apply.py --check` state=superseded; `test_recipe.py` 6/6.

## 2026-10-09 re-seal (A26 Latin-1 atom units widened in bulk for getChars)

Regenerated against dd9120f283: no upstream target changed, so `runtime.patch` and every `after` image are untouched; the M3-owned `M3StringAtom.java` records its new owner as a reviewed `superseded` hash. 68 targets; `apply.py --check` state=superseded; `test_recipe.py` 6/6.

## 2026-10-09 re-seal (A27 flat needles over bulk windows)

Regenerated against d21c8fa886: the upstream `String.java` after-image and hunks were regenerated from a synthetic after-state tree (every other hunk byte-identical); the M3-owned `M3String.java` keeps its v2 after-image and records the new owner as a reviewed `superseded` hash. 68 targets; `apply.py --check` state=superseded; `test_recipe.py` 6/6.

## 2026-10-09 re-seal (A28 case-insensitive folds over bulk windows)

Regenerated against 81caf879c6: the upstream `String.java` after-image and hunks were regenerated from a synthetic after-state tree (every other hunk byte-identical); the M3-owned `M3StringAtom.java`, `M3StringMixedCompare.java` keep their v2 after-images and record the new owner as reviewed `superseded` hashes. 68 targets; `apply.py --check` state=superseded; `test_recipe.py` 6/6.

## 2026-10-09 re-seal (A29 contentEquals receivers in bulk windows)

Regenerated against 8e050d6cd2: the upstream `String.java` after-image and hunks were regenerated from a synthetic after-state tree (every other hunk byte-identical); no M3-owned file changed, so every `superseded` list is untouched. 68 targets; `apply.py --check` state=superseded; `test_recipe.py` 6/6.

## 2026-10-09 re-seal (A30 cross-coder compares in place)

Regenerated against 0b114a9b55: no upstream target changed, so `runtime.patch` and every `after` image are untouched; the M3-owned `M3StringAtom.java` records its new owner as a reviewed `superseded` hash. 68 targets; `apply.py --check` state=superseded; `test_recipe.py` 6/6.

## 2026-10-09 re-seal (A31 single-unit search windows outside the block range)

Regenerated against e03a8ebfa6: no upstream target changed, so `runtime.patch` and every `after` image are untouched; the M3-owned `M3StringPositionPrecompute.java` records its new owner as a reviewed `superseded` hash. 68 targets; `apply.py --check` state=superseded; `test_recipe.py` 6/6.

## 2026-10-10 re-seal (A32 single-unit search block index on the repeat search)

Regenerated against bbf344e829: no upstream target changed, so `runtime.patch` and every `after` image are untouched; the M3-owned `M3StringPositionPrecompute.java` records its new owner as a reviewed `superseded` hash. 68 targets; `apply.py --check` state=superseded; `test_recipe.py` 6/6.

## 2026-10-09 re-seal (A33 graded bulk windows)

Regenerated against 0f48926fc3: the upstream `String.java` after-image and hunks were regenerated from a synthetic after-state tree (every other hunk byte-identical); the M3-owned `M3String.java`, `M3StringAtom.java`, `M3StringMixedCompare.java`, `M3StringPositionPrecompute.java` keep their v2 after-images and record the new owner as reviewed `superseded` hashes. 68 targets; `apply.py --check` state=superseded; `test_recipe.py` 6/6.

## 2026-10-10 re-seal (A34 trigram facts on the repeat use)

Regenerated against b4748064de: no upstream target changed, so `runtime.patch` and every `after` image are untouched; the M3-owned `M3StringSearchPrecompute.java` records its new owner as a reviewed `superseded` hash. 68 targets; `apply.py --check` state=superseded; `test_recipe.py` 6/6.

## 2026-10-10 re-seal (A35 M3-needle window lane)

Regenerated against 6fef93347e: no upstream target changed, so `runtime.patch` and every `after` image are untouched; the M3-owned `M3String.java` records its new owner as a reviewed `superseded` hash. 68 targets; `apply.py --check` state=superseded; `test_recipe.py` 6/6.

## 2026-10-10 re-seal (A36 needle plan band)

Regenerated against 5aee4dc806: no upstream target changed, so `runtime.patch` and every `after` image are untouched; the M3-owned `M3String.java` and `M3StringSearchPrecompute.java` record their new owners as reviewed `superseded` hashes. 68 targets; `apply.py --check` state=superseded; `test_recipe.py` 6/6.

## 2026-10-10 re-seal (A37 dense char walk to the linear lane)

Regenerated against 68804b36e1: no upstream target changed, so `runtime.patch` and every `after` image are untouched; the M3-owned `M3StringPositionPrecompute.java` records its new owner as a reviewed `superseded` hash. 68 targets; `apply.py --check` state=superseded; `test_recipe.py` 6/6.

## 2026-10-10 re-seal (A38 reverse lane on the forward intrinsic)

Regenerated against 657ebe6c7b: no upstream target changed, so `runtime.patch` and every `after` image are untouched; the M3-owned `M3StringPositionPrecompute.java` records its new owner as a reviewed `superseded` hash. 68 targets; `apply.py --check` state=superseded; `test_recipe.py` 6/6.
