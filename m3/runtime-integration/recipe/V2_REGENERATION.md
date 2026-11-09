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

## 2026-10-12 reseal (logical concat length guard)

Master commit 2442a6b242953f4ec8bd63d52ac48708d0383782 adds the checked logical-length/OOME guard to M3StringPool.java. This is an additive six-line change over the v2 recipe state; the source is pinned as an exact superseded SHA-256 (f8bac45d1623d25470e1b79beffa276f271db10e297ef5b434d5335c9d4c908b) in manifest.json. The recipe remains refusal-safe: it recognizes this state as already superseded and does not apply or reverse the older patch over it. Unknown source states remain rejected.

## 2026-10-18 re-seal (A42 flat-needle trigram gate re-land, leaf REGEX-RELAND)

Superseded-custody re-seal against aabb10ebf8 (receiver of Synexia #12397): `runtime.patch` and every
`after` image are untouched (66 targets). `M3StringSearchPrecompute.java` and `String.java` record their
A42 re-land owners as reviewed `superseded` hashes. The pinned master 970a34350eb already failed
`apply.py --check` (`source drift: .../M3String.java`): `M3String.java`, `M3StringPool.java`,
`M3PatternQuery.java` and `Pattern.java` had changed on master after the last seal (3c3d66694a) without a
re-seal, so their master hashes are recorded as `superseded` too (PRE_EXISTING_DRIFT). A full
`a4/reseal.py` regeneration is refused by the current `apply.py` (`modified UTF-8 recipe seal drift:
src/hotspot/share/classfile/javaClasses.cpp`), so the superseded custody of the A41/A42 epoch-1 re-seals
is kept. `apply.py --check` state=superseded and `test_recipe.py` 8/8 on the 66 targets of aabb10ebf8.

## 2026-10-18 re-seal (A43 reverse needle stock lane re-land, leaf REGEX-RELAND)

Superseded-custody re-seal against e977631b9f (receiver of Synexia #12397): `runtime.patch` and every
`after` image untouched (66 targets); `M3StringSearchPrecompute.java` records its A43 owner as a reviewed
`superseded` hash; no other target changed. `apply.py --check` state=superseded and `test_recipe.py` 8/8
on the 66 targets of e977631b9f.

## 2026-10-19 re-seal (A42 epoch 3 after the master merge, leaf REGEX-RELAND)

Superseded-custody re-seal against 49b7e0cb99 (master 2f2beb2eff7a merged in 370b5cfc30): `runtime.patch`
and every `after` image untouched (66 targets). Master changed `M3String.java`,
`M3StringCodePointPrecompute.java` and `String.java` after the 2026-10-18 seals; their master hashes are
recorded as `superseded` (PRE_EXISTING_DRIFT). `M3StringFacts.java` also changed on master (34663f5b), but it
is one of the four modified-UTF-8 fan-in owners whose `superseded` list `apply.py` pins exactly
(`MUTF8_FANIN`), so it is NOT recorded here: `apply.py --check` stays `source drift: .../M3StringFacts.java`
on this branch (live master 2f2beb2eff7a fails the same check with 8 drifted targets, first
`.../M3String.java`). Advancing that entry needs the fan-in owner's recipe, not this re-seal.

## 2026-10-19 re-seal (immediate short String owner, leaf SHORTSTRING)

Superseded-custody re-seal against e5869d9ffbd9fe65c03450e017e6691721ae2d9a (receiver of Synexia #12475): `runtime.patch` and every `after`
image untouched. The immediate-string owner commit changed eight sealed targets (`M3String.java`,
`M3StringOwner.java`, `M3StringPool.java`, `javaClasses.cpp`, `javaClasses.hpp`, `javaClasses.inline.hpp`,
`vmClassMacros.hpp`, `vmSymbols.hpp`); their new hashes are recorded as `superseded`. Pre-existing
drift on master (`M3StringFacts.java`, pinned by `apply.py` `MUTF8_FANIN`) is unchanged by this leaf.

Correction (same leaf): `javaClasses.cpp`, `javaClasses.hpp` and `javaClasses.inline.hpp` are three of the four
modified-UTF-8 fan-in owners whose `superseded` lists `apply.py` pins exactly (`MUTF8_FANIN`, with
`M3StringFacts.java`), so their new hashes are NOT recorded: five targets are re-sealed (`M3String.java`,
`M3StringOwner.java`, `M3StringPool.java`, `vmClassMacros.hpp`, `vmSymbols.hpp`). `apply.py --check` on this
branch reports `modified UTF-8 source drift: .../javaClasses.cpp`; advancing the fan-in needs the fan-in owner
recipe for all four owners together (live master already fails the check on `M3StringFacts.java`).
