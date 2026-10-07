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
