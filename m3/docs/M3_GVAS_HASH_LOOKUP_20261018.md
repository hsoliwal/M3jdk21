# M3JDK21 — native GVAS literal forward lookup through primitive hash slots
<!-- SPDX-License-Identifier: Apache-2.0 -->

This is **M3JDK21's existing Java/JNI runtime owner**, not an extra M3
layer or adapter. The first-party donor is the precompiled UTF-8 lexical-rank
index in `com.synexia/synexia-unicodex/PRECOMPILED_WORD_FAST_PATH.md`.
The canonical source-sealed Maven/OpenRewrite recipe is
[hsoliwal/com.synexia PR 12387](https://github.com/hsoliwal/com.synexia/pull/12387).
The product receiver source/test blobs here match the two canonical
template blobs byte-for-byte; the recipe is a build-time source authority,
never a runtime dependency.

## Existing owner, atomic algorithm change

The existing `M3GvasLiteralImage` retains its read-only `M3GVAS01`
format, exact native mapping, admission validation, `findRow(String)`,
`addressOf(String)`, `addressAt(row)`, precomputed Unicode facts,
and all JNI symbols. Its existing binary search remains as complete fallback.

After image admission, a **bounded optional immutable `int[]` slot array**
contains only row+1 (zero means empty). It uses the original image's mapped
UTF-16 code units to hash each row; UTF-16 comparisons remain authoritative
on candidates. Slots never own duplicated text, entry objects, or `String`
values. Load <=50%; probe depth >=64, <64 rows, >1,048,576 rows,
low heap headroom or primitive allocation pressure all refuse optional
index construction without refusing valid image admission.

A query hashes the caller's exact UTF-16, probes those integer slots,
verifies the candidate against the same mapped UTF-16 spelling, and
returns the existing frozen row index. An exhausted probe budget
delegates to the original sorted binary search, not an approximate match.
`addressAt(row)` remains O(1) arithmetic through the strict mapped
native image. No semantic concept identity or Unicode normalization is
equated to a spelling hash.

## Tests and acceptance

The original `M3GvasLiteralImageContractTest` is expanded in place to
exercise Java String-hash collision examples (Aa/BB variants), forced
primitive index preparation for the bounded default fixture, 2,048
distinct negative probes, exact Unicode, surrogate seams, immutable
address calculations, malformed image refusal and JNI three-process proof.

Run `M3_JDK=/path/to/java21 python3 m3/verify-gvas-literal-precompute.py`
to compile actual Java21 reader/writer/tests and C11 JNI, then run
`-Xcheck:jni`, native address collision, digest mismatch, corrupted row
validation, and `-Xint`/`-Xbatch`/`-XX:-CompactStrings` cooperating
processes at one exact numerical fixed address.

### Evidence boundary
A standalone stock-Java21 algorithm shape oracle (not modified product)
passed 105,494 differential checks against frozen binary search over
5,494 UTF-16 keys. One diagnostic host, 250k warm successful lookups:
166.18ns hashed, 313.62ns binary. This does NOT establish modified
M3JDK21 JNI/HotSpot/JIT speed, GC, load or cross-platform acceptance.
Keep the PR **draft** until recipe fixed-point, product JNI proof,
matched M3 String/HotSpot jtreg and before-after JMH/RSS/alloc gates pass.

## Follow-on rather than duplicate implementations

Attach shared generation-qualified word IDs and actual UnicodeX X-spine
concept pointers to the already-existing M3StringPool/M3WordRows/
M3FixedAddressMapping owners. Stable concept/word IDs remain distinct;
one lexical spelling may map to multiple concepts. The broader filtered
and sorted collection owners are already implemented under
`m3/collections`; handle improvements by primitive-array offset/mask
atoms and their existing recipe-controlled catalogue, not new wrappers.
