# Reconciliation with Synexia PR 7388

Reviewed source pin: `hsoliwal/com.synexia@9c36c18a7778e6096148a3bbee8f2af7575138b4`, [draft PR 7388](https://github.com/hsoliwal/com.synexia/pull/7388). The files below identify Apache-2.0 in their source headers. No production source from them is copied into this module. A single pinned `FrozenChars` source is downloaded to ignored test output for differential content/range checks.

| Existing owner | P0 relationship / contract differences |
|---|---|
| `FrozenChars` | Established immutable char[] owner with defensive ingress, range-sharing, read-only views, String-compatible precomputed hash. P0's isolated leaf tests a LATIN1/UTF16_LE byte-backed format; it is not a replacement FrozenChars and does not reproduce its hash/view API. |
| `FrozenBytes` | Existing immutable byte owner and slices. Reuse it for Synexia conversion outputs; P0 has no replacement byte interner or encoded-fact cache. |
| `com.synexia.indexstring.LocalM3StringPiece` | Existing opaque Owner / generation / entryId / start / length identity over FrozenChars, explicit conversion policy and joining via existing owner. P0's `com.m3.text.LocalM3StringPiece` is a separate **provisional format prototype**, with UUID owner and byte-encoding metadata. The names do not establish API compatibility. It must not become a competing canonical owner without an explicit adapter/migration decision. |
| `MIndexJoinedChars` | Existing weakly canonical segment-tuple body and ranges with whole-join encoding. P0's flat directory is only a discontiguous byte-format/lifetime experiment; it has no weak body interning, copy/encoding API parity or prepared search machinery. Do not replace this established owner with the P0 directory. |
| `FrozenByteInterner` | PR 7388 extends the existing owner with bounded local entries, exact collision checks, PackedLongLongOrderedMap indexes, LRU/MRU, explicit admission and lazy policy-keyed encoded facts. P0 deliberately does not implement another cache. Future local-pool work should build on that owner after dependency/license review, preserving the raw-byte lane's independent limits and documented accounting exclusions. |

The parent reported 27 focused tests plus an overlapping existing 13-test gate. Those results are **not independently rerun or claimed here**. Neither those tests nor P0 prove transparent JVM String integration or JNI/performance behavior.

`M3_JDK=<private-built-JDK> python3 m3/compatibility/check-synexia.py` compares P0 content, String hash after materialization and all small ranges against the exact FrozenChars source for every one of the 65,536 UTF16 code units. This verifies only those semantic dimensions. It does not prove owner equivalence, canonicalization, subSequence object identity, encoding, cache admission, pooling, prepared search or binary/API compatibility.

Source SHA-256 values:

- FrozenChars.java: `42ce1c8ac4dc83a850b900b3c5a0b60de98410495c7da0145ff99cc9d19e950f`
- FrozenBytes.java: `2b64eee9b2930963a7442b6691993006cd7db3178975dc7071a8774e2046ba0f`
- LocalM3StringPiece.java: `3a39470d8d239487716f5b295cf4b222ef180013f50cbbfc06e577c344cb2b44`
- MIndexJoinedChars.java: `69669b30d87bdb686c38a125b748fb7defcd75168b50e0ff5a92b5f5e3562180`
- FrozenByteInterner.java: `1cf936e8aab4c4066f58ea41b817646bc06bca65a5c171724c0e6ae972e6eeb6`

Keep both existing public MIndexString families and their lexical-key semantics unchanged. Local record identities are not lexical IDs or portable vocabulary keys. Broader MatIndex/MIndex migration still requires per-owner source mappings and compatibility facades; this P0 cannot establish them by naming similarity.
