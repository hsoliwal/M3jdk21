<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 file grammar / parallel continuation

This is a source-changing successor to the combined M3JDK21 PR1624, not another
runtime layer or file store. Parent d7b9e65241b717a387b25234df4cf21531f07d9f and
all of its file-tree/archive, collection-view, AST, MOP and JNI ancestry remain.
Only nine selected Java source/test files change; native production owners remain
unchanged. This candidate is not a claim to finish all wider SDK requirements.

Canonical recipe PR: https://github.com/hsoliwal/com.synexia/pull/12688
Canonical qualification head: 3dacf226b2026cb5bc033311b811d192a855b5f3
Source-first commit: e673cf84ffeaeb685a786a563a3b84aa53009443
Recipe: com.synexia.m3.FileGrammar20261021
Task: synexia-openrewrite-recipes/recipe-crates/m3-file-grammar-20261021

The nine target bytes match the canonical per-file After templates. Maven/OpenRewrite
SDK_EXECUTION remains NOT_RUN; these are reviewable exact candidates, not claimed
executed SDK Results. Reusable recipes/tests/catalogues stay canonical in Synexia.

## Per-file paragraphs in the original one-read pipeline

```java
M3Grammar grammar = M3FileCharShadow.grammar(100_000);
file.observe(byteLimit, progress, M3FileShadow.utf8(grammar));
M3Paragraphs paragraphs = grammar.details();
M3Paragraphs selected = paragraphs
        .filterMask(M3Paragraphs.NON_ASCII, 0)
        .sortedByLength(true);
```

Use one observer per admitted file read, including separate observers per parallel
file task. The example uses existing M3File/UTF-8 callbacks; complete owning-module
provider I/O has not been executed in this continuation. No file text is retained
by grammar metadata, and no source is reread by the observer itself.

TEXT_PARAGRAPHS_V1 stores long half-open UTF-16 starts/ends, one-based inclusive
line ranges, whitespace-token counts and byte flags. Blank lines and U+2029 end
paragraphs; CRLF/CR/LF/NEL/LS delimit lines. CRLF may span callback chunks. Leading
and trailing whitespace in nonblank lines is retained; final line terminators are
excluded. This is structural paragraph discovery, not linguistic parsing or
Markdown/HTML/PDF interpretation. Preserve original decoder replacement policy.
Results require genuine EOF/finish. Bind cache reuse to source generation/digest,
decoder and grammar policy; paths/timestamps alone never certify unchanged text.

M3Paragraphs owns primitive parallel arrays. Filters select int physical rows,
stable sorting permutes only row IDs, and slices share the immutable metadata.
No per-paragraph Node/Entry or another String payload owner is introduced.

## Ordered correction and behavior boundary

The recovered three-patch chain repairs task cleanup in the original
ThreadPerTaskExecutor using OpenJDK 8347039, introduces the array-backed bounded
M3FileTaskWindow, then uses it in original M3FileParallel. Original public parallel
method descriptors remain. Out-of-order worker failure now interrupts waiting on
a blocked earlier task; this is an intentional failure-progress repair, not a claim
that failure timing is identical. Successful result publication stays ordered.
Callbacks must cooperate with interruption; side effects are not rolled back.

## Executed finite proof

Actual Java21 strict component compilation and standalone contracts passed in each
normal and interpreter JVM mode: 830 task-window checks; 415,994 grammar checks;
6,165 actual original Java/C JNI-mask parity checks with -Xcheck:jni and fatal UBSan.
Counts across modes overlap. Original character-shadow declarations were retained,
with one static factory addition. Initial diagnostic-overhead timeout is preserved
in canonical lessons; corrected fresh interpreter execution passes.

The retained read-only verifier recompiles actual selected owners, checks exact
source/dependency Git blobs and refuses occupied output. From the canonical checkout:

```sh
bash synexia-openrewrite-recipes/recipe-crates/m3-file-grammar-20261021/verify_components.sh \
  /absolute/this-M3jdk21 /absolute/fresh-proof
```

Native proof uses unchanged original M3IndexMaskWindow Java/C plus a test-only
bootstrap library loader, not a substitute native implementation. Paragraph filters
remain Java by default; no native speedup or installed-libjava acceptance is asserted.

## Remaining gates / keep draft

NOT_RUN: actual Maven/JUnit/OpenRewrite; full owning file module and end-to-end
provider traversal; complete source-built M3JDK21/HotSpot/libjava and jtreg/JCK;
other platforms/providers; comparative CPU/heap/native-memory measurement; full
canonical registry/provenance admission. Existing inherited PR results are not
fresh runs on this combined tree. Universal grammar, all collection/provider
contracts and all-system indexing completion remain unclaimed. Global receiving
phase is unchanged. No default branch update, merge, rebase or force push.
