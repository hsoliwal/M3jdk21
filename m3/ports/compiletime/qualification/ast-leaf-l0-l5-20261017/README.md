<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3AST L0-L5 leaf-first receiver (draft)

This is a bounded independent Apache-2.0 compile-time **evidence index** under existing `m3/ports/compiletime`, not `jdk.internal.mindex.M3AST` in `java.base` (that mapped runtime owner is absent on master), not a public JDK API, and not an assertion that the active M3 String receiving phase has completed. Production order stays **STRING -> arrays -> collections -> AST/compiler**.

Canonical donor and recipe authority: [hsoliwal/com.synexia#12073](https://github.com/hsoliwal/com.synexia/pull/12073), generated-source source commit `ddc695cbea2702cda969ce813682a5fa052b8a83`. This receiver's two Java files are exact output of the source-sealed Maven recipe (`com.synexia.m3.M3ASTLeafIndexRecipe`), not hand-maintained target forks. No Synexia runtime/service dependency. Source/test SHA-256 are pinned in `SOURCE_RECEIPT.tsv`.

## Explicit leaf-first AST levels

| Level | Owner | Meaning |
|---|---|---|
| L0 | file fact/atom | supplied source fingerprint, immutable AST ID/64-bit state planes, offsets |
| L1 | file summary | one rollup for each file |
| L2 | package | combines L1 files in project/module/package sorted scope |
| L3 | module | combines packages |
| L4 | project | combines modules |
| L5 | repository | combines projects, root even for empty input |

This intentionally differs from separate Synexia draft documentation PR #12065 (L0=file, L1=package, L2=component). Use this named version for AST evidence, not a silent conversion of its historical documents.

Atom state is tied to immutable physical slots, **not logical row position**. Parent/child topology uses flat `int[]` row IDs, flattened child-edge offsets, and no per-atom allocation. Filtering uses required/forbidden full 64-bit masks and returns detached `int[]` physical offsets without moving or altering AST data. Source SHA values are authenticated only for syntax/format here: completeness, source-byte identity, compiler effects and semantic equivalence require independent evidence. Contiguous physical windows are safe because file descriptors are deterministically sorted once before rollup. The digest includes caller-provided atom IDs and states, and must not be mistaken for executable equivalence.

## Validation

- **Executed locally with JDK 21**: `javac --release 21 -proc:none -Xlint:all -Werror`; Synexia and M3JDK generated oracles passed **86,095 assertions each**; canonical recipe `--selftest` proved double-apply fixed point and drift/missing refusal; generated output compiled and ran.
- **NOT RUN**: Maven/OpenRewrite integration (no Maven installed locally), full target dependency reactor/JDK image/jtreg/HotSpot, Android/Windows JNI, CPU+memory benchmark and CI exact-head verification.
- Reviewer quick command: `javac --release 21 -proc:none -Xlint:all -Werror -d /tmp/ast-classes m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ASTLeafIndex.java m3/ports/compiletime/src/test/java/com/m3/compiletime/M3ASTLeafIndexSelfTest.java && java -cp /tmp/ast-classes com.m3.compiletime.M3ASTLeafIndexSelfTest`.

Native-filter expansion is deliberately **not implemented** here: existing `M3IndexPathMasks` JNI remains canonical for its own selected index/offset stage. Reusing it later requires an explicit compiletime-to-collections adapter, ABI proof, overhead measurements and evidence of safe array ownership. No duplicate C engine is introduced.

Original first-party expression: Copyright 2026 Hitesh Soliwal and contributors; Apache-2.0 within independent `m3/**` module. Upstream JDK/third-party file licensing remains unchanged. Competitive programming sites are algorithm-taxonomy pointers, never copied solutions.
