<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Received M3AST compiler review

**Disposition: PASS for the bounded source-seal and additive plugin-selection changes.**
No remaining blocking defect was found in the reviewed diff. This is a read-only
review of the received target and existing qualification evidence, dated
2026-10-18. The review did not compile, run tests, reapply recipes, or change
product source. The receipt directory was inventoried absent before creation.

## Exact reviewed target

Repository: `/workspace/scratch/65626af64474/m3-ast-shared-20261018/M3jdk21`.
Comparison baseline: `2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`.

| Received file | SHA-256 |
| --- | --- |
| [M3AST.java](sandbox:/workspace/scratch/65626af64474/m3-ast-shared-20261018/M3jdk21/src/jdk.compiler/share/classes/com/sun/source/util/M3AST.java) | `0c451956dbaa3cffcdd5e05705501c192b913048a7afd7562982aef584c9d37a` |
| [M3ASTPC.java](sandbox:/workspace/scratch/65626af64474/m3-ast-shared-20261018/M3jdk21/src/jdk.compiler/share/classes/com/sun/tools/javac/m3/M3ASTPC.java) | `3e43390b9d9df94da9943b101c809f1bdc4c1039e825976b2cda72acb8c56b28` |
| [M3ASTSourceSealTest.java](sandbox:/workspace/scratch/65626af64474/m3-ast-shared-20261018/M3jdk21/test/langtools/tools/javac/m3/M3ASTSourceSealTest.java) | `1b3423999ac2644f17ba6bc743f3873f4130846acc4e346518e16958606e5e1a` |
| [M3ASTPluginSelectionTest.java](sandbox:/workspace/scratch/65626af64474/m3-ast-shared-20261018/M3jdk21/test/langtools/tools/javac/m3/M3ASTPluginSelectionTest.java) | `34efbf105a3a3c7f37a6d67d11e480abd7c4ef84bef1992892613c94b28625b7` |

The API preimage was `6a8bccb501a516cca779e71e24ef814ca40825faf92fed744e48f65ad4c1d854`.
The compiler owner advanced from baseline
`845c70bdc5e77a62282417889dc77bf466fd0c0005f72112e86faf27ec616399`
through the qualified source-seal preimage
`8b426b59796942253765c0fe45725d4b3e0f3640a3dffe4ec3d8c44413109bb3`.
Both received postimages match the frozen candidates. The original JavaCompiler,
M3PrimitiveImageCompiler, module-info.java and M3ASTSharedFrontendTest remain
byte-identical to the baseline.

## Invariants and behavior

- **Existing compiler ownership.** The diff introduces no compiler task, parser,
  attribution pass, tree copy, service loader, persistent cache, or new context.
  M3ASTPC still joins the original BasicJavacTask context (lines 177-209) and
  dispatches its actual event tree (lines 248-267). There is no live-AST sharing
  between separate Maven and Gradle processes.
- **API compatibility and effective options.** All pre-existing API declarations
  remain intact. The only public API addition is a static interface method, so
  existing implementors acquire no abstract obligation. M3ASTPC lines 163-175
  read the original context's existing Arguments entry and its getPluginOpts()
  result, matching only the exact plugin name. The query creates no M3 session or
  listener and performs no source work. Null, foreign, released-context and
  missing-arguments boundaries retain their documented refusals. Selection does
  not assert successful plugin initialization or compilation; an explicitly
  closed M3 handle and a released javac context are different states.
- **Exact source seals.** Lines 141-159 hash the same high byte then low byte for
  every UTF-16 code unit as the previous scalar implementation, including lone
  surrogates and empty input. The temporary byte buffer is bounded at 8192 bytes.
  Lines 350-377 retain the no-extra-read PARSE path and one validated read per
  later event; a changed length now refuses before digest work. Successful source
  results and IOException identity remain memoized within that event.
- **Failure publication and lifetime.** Lines 119-137 reuse the existing abort
  path before javac's later completion event can authorize image publication.
  The original failure is rethrown; cleanup failures remain suppressed through
  lines 276-308. Callback-thread checks, per-event identity-key projection
  memoization (including null/failure/recursion handling), and reference release
  remain unchanged at lines 339-342 and 379-411. Source hashes do not become keys
  for reusing attributed facts across classpaths, processors, phases or tasks.

## Evidence read

The final descriptor image's [build receipt](sandbox:/workspace/scratch/65626af64474/m3-ast-shared-20261018/descriptor-qualification-02/BUILD_RECEIPT.json)
has SHA-256 `4195b08a5a5ebde1ef0325c6d9eb49abfaacc4674160f999aab3926461d61827`.
It records strict compilation of the four actual compiler owners plus the actual
descriptor, with `-Xlint:all -Werror -implicit:none` and an isolated source path.

The [validation receipt](sandbox:/workspace/scratch/65626af64474/m3-ast-shared-20261018/descriptor-qualification-02/validation/VALIDATION_RECEIPT.json)
has SHA-256 `d8ff6b867adf66f631b39ee0b2687c3ff04dc9aa98d6da80fecbd293ed1b927a`.
Its linked logs report the existing sharing suite **70 checks PASS** and source
seals **117 checks PASS**, including 23 digest cases covering all 65,536 code-unit
values, boundary lengths, equal/different-length edits, distinct source objects
with one URI, no partial image after capture refusal, and cleanup suppression.

The separate [plugin-selection receipt](sandbox:/workspace/scratch/65626af64474/m3-ast-shared-20261018/plugin-selection-qualification/RECEIPT.json)
has SHA-256 `eb41bbd74c1108ee982c3c2e703a69695fc3065b7fcb2a08df493ac64d3284a0`
and is bound to that same image build. It reports **462 checks / 24 real-task
cases PASS**, including operand aliases, inline operands, ASCII delimiters,
NUL-separated plugin selections, longer names, effective internal option
overrides, zero query source reads, unchanged owner identities, normal original
task execution and the documented refusal boundaries.

The [JMOD proof](sandbox:/workspace/scratch/65626af64474/m3-ast-shared-20261018/descriptor-qualification-02/candidate/JMOD_PROOF.json)
has SHA-256 `18fabc3dae24b9241b49ba2958d08ce05068231c09c5dec4e88b94025c5b5795`.
It records preserved stock payloads and supported module-hash regeneration with
the original hash coverage. This remains a hybrid Java 21 qualification image,
not a full source-built JDK or full native/HotSpot/jtreg qualification. This
compiler review does not replace the separate Maven or Gradle runtime receipts.

## Bounded known Maven edge

The unchanged [M3AstCompilerConfiguration.java](sandbox:/workspace/scratch/65626af64474/m3-ast-shared-20261018/maven/impl/maven-core/src/main/java/org/apache/maven/plugin/internal/M3AstCompilerConfiguration.java)
has SHA-256 `9e7e79f85ba86ffbd0784aa97b3939b652645fe7cb4370a8d64ddccb7501ebf2`.
Lines 62-68 inspect compilerArgs XML children independently; lines 113-118
recognize plugin-looking text without javac's operand or effective-override
context. For example, `--class-path` followed by the operand `-Xplugin:M3AST`
can suppress automatic `m3.ast=true` injection even though javac selected no
plugin. Internal option override order is likewise outside that helper's model.
This is a pre-existing pre-task configuration limitation, inferred from source;
the Maven edge itself was not executed in this review. The received compiler
query does not introduce it.

**Concrete workaround:** complete all option operands, then include a real final
`<arg>-Xplugin:M3AST</arg>` compiler argument after advanced internal overrides
(retain any intended M3AST plugin arguments in that switch). It must be an actual
switch rather than another option's value. This establishes M3AST selection;
javac still validates all other supplied options normally.

No original JavacTask exists at Maven's configuration hook. The two-file Maven
session-service restoration therefore remains exact: this review authorizes no
second version-pinned option parser and no temporary compiler. The shared
frontend boundary remains the original compiler task and its consumers.

The source-capture cleanup change is also deliberately bounded: it does not
establish a universal success signal for unrelated compiler/processor/listener
failures. No performance speedup, historical hierarchy-module recovery, or global
receiving-stage promotion is inferred from these finite checks.
