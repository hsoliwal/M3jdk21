# M3File: bounded multi-hunk source correspondence

Source owner: `jdk.m3.parser.M3File` in the integrated M3JDK21 parser pack.

The existing one-envelope `changeSince`, `utf8ChangeSince`, and
`utf8InputEditSince` contracts are deliberately unchanged. Opt-in
`changeHunksSince(previous, maxComparedUtf16Units, maxLines, maxHunks)`
returns source-relative, half-open ranges for the two immutable source
generations. A `SourceHunks` retains one packed primitive `int[]` with four
indices per hunk. Temporary `ChangeHunk` projections are returned on demand,
not stored per line or source node. Both sources retain the same String owner
as before.

`utf8ChangeHunksSince` provides corresponding *original UTF-8 byte* spans,
only when each file generation was independently admitted from strict UTF-8
bytes. Those spans are absolute within their own generation. They are **not**
consecutive Tree-sitter `TSInputEdit` records. A consumer must separately
rebase and validate edits, point coordinates, original tree ownership and
grammar/parser options before incrementally editing a provider's old tree.

## Deterministic discovery and fallback

1. The original content comparison and URI/budget guard remain authoritative.
   Raw-byte SHA equality or difference is still reported independently.
2. Both source generations use their existing lazily cached line-start maps.
   Temporary primitive linear-probed tables catalogue exact, unique full-line
   candidate anchors. Noncryptographic signatures are merely signals; exact
   `String.regionMatches` decides each candidate.
3. Longest-increasing unique-line order preserves a monotonic mapping even
   if other lines move or repeat.
4. Between anchors, the common prefix/suffix is trimmed to the actual
   different interval, backing off from any valid surrogate-pair split.
5. Exhausting the maximum hunk count, anchor probes or admitted table bound
   returns the single original conservative `ChangeEnvelope` and marks the
   result `coarse()`. Source line-budget and UTF-16 comparison-budget
   violations refuse with an exception.

Every interval outside the reported changes is an **exact UTF-16 match**.
The API does not claim a minimum edit script, parsed-node identity, thread-
safe incremental parser reuse, filesystem atomicity, ABI equivalence,
JDK/JNI installation, or measured performance.

## Proposed M3AST/M3File build connection

The next step is to intersect these source coordinates with M3AST node ranges
within the *same source generation*, expand through javac's authoritative
symbol/type and .class ABI reverse dependencies, and invalidate only proven
affected downstream consumers. Fingerprints should track raw bytes, UTF-16,
structural syntax, logic and ABI separately. A compiler-signed source stamp
must remain distinct from a caller's unverified assertion.

The canonical generator and verification crate live solely in Synexia at
`com.synexia.m3jdk.M3FileMultiHunk20261027`. M3JDK21 receives only
executed Java/test/docs postimages and data-only provenance. This parser
pack is still optional work under the strict String-first SDK phase HOLD.
