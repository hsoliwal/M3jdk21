<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 source-byte chunk windows (opt-in Java21/JNI)

An additive runtime leaf in **M3JDK21**, with canonical source/recipe in Synexia.
`SourceChunkPlan` borrows source identity from the existing immutable
`SourceArtifact.Span` and optional LF boundaries from `ByteLineIndex.nativeIndex`.
No new payload store, index owner, Java/HotSpot modification, native symbol, ABI
revision, Maven module or Synexia runtime dependency is introduced.

For an explicit `1 <= minimumBytes <= maximumBytes <= 16 MiB`, choose the
**last LF boundary in the closed [cursor+min, cursor+max] window**. If none is
present, cut at the hard maximum. The last chunk may be shorter than minimum.
Zero-length inputs have zero chunks. Every source byte belongs to exactly
one chunk; CRLF and invalid UTF-8 bytes are preserved, even at forced cuts.
`chunkAtOffset(length)` returns `chunkCount` (the EOF cursor).

Building uses `ByteLineIndex.javaIndex` or the existing explicitly installed
SHA-256-pinned `ByteLineIndex.nativeIndex`. Afterwards, the chunk plan retains
only a compact `int[]` of increasing end offsets and the original `Span`;
`chunk(i)` is another view, not a new byte[] copy. At most 1,000,000 chunk
ends (4 MiB primitive retention) are allowed; more refuse.

## Independent proof

```sh
python3 m3/ports/capability-runtime/source-chunk-plan/verify.py \
  --root m3/ports/capability-runtime --out /fresh/external/chunk-proof
```

`--java-only` is available on Windows without WSL. The real owner, unchanged
C11 JNI (where enabled), previous LF, line and KMP suites, JMOD and jlinked
stock-JDK21 image are checked first. A separate 2,200-case naive reverse-byte
oracle checks exact line-window cuts; bounded allocations, source ownership,
UTF-8/CRLF, subranges, cancellation, concurrency, and use of the linked image
are additional checks. The actual OpenRewrite SDK, full modified HotSpot / jtreg
and measured memory/throughput crossover remain separate qualification gates.

**Rights / donors:** Synexia `MmapChunker` (Apache-2.0) supplies category and
boundary-policy comparison. Its forward line-alignment can exceed a nominal
chunk size; this stricter bounded JDK port uses the existing byte-line index,
not a copied file-mapping implementation. LeetCode/HackerRank/GFG source
text is not copied. See the canonical category decision catalogue.
