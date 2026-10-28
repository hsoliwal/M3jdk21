<!-- Copyright 2026 Hitesh Soliwal and contributors
     SPDX-License-Identifier: Apache-2.0 -->
# M3File UTF-8 incremental edit scripts

`M3File.utf8InputEditScriptSince(previous, maxComparedUtf16Units, maxLines, maxHunks)`
composes existing source-authoritative `utf8ChangeHunksSince` with a bounded,
sequential parser-edit coordinate projection. It adds no independent parser,
source buffer, JNI symbol, OpenRewrite dependency or AST representation to the
integrated M3JDK21 parser module.

## Problem and contract

Multi-hunk positions are absolute in **two different immutable source
generations**. In particular, `currentStartByte` is *not* a valid old-tree
byte position for the second edited hunk. Feeding those absolute pairs
directly to `ts_tree_edit` can corrupt incremental source correspondence.

The new `Utf8EditScript` retains **11 primitive ints per step** and a reference
to the original URI/UTF-8 digest receipts. It orders steps by decreasing
original byte start, so previously edited text always lies strictly after the
next old-tree edit. Consequently, the next unchanged prefix—including its
LF-based UTF-8 points—still has original positions. A `Utf8EditStep` exposes:

- `inputEdit()`: the `startByte`, `oldEndByte`, and **intermediate-state-relative**
  `newEndByte` plus the three LF-based UTF-8 byte-column points;
- `currentStartByte`/`currentEndByte`: the replacement slice in the *target*
  generation, not an intermediate byte or tree coordinate.

All actual input bytes remain owned by the existing source provider. This
class does not encode/store a second source or automatically edit any native
tree. An external parser must own and validate its original parser tree,
language/ABI, exact UTF-8 input generation, encoding, lifetime, parser options,
and source fingerprints before applying a script. An M3File SHA-256 receipt is
not proof that some independently supplied parser tree came from that source.

Resource guards reject missing original UTF-8 geometry, unrelated URIs,
malformed UTF-8, bad budgets, and transient byte-length or line-row counts
that exceed the caller's limits. Source/target and *intermediate* byte length
and LF line counts are bounded. When line-anchor refinement exhausts the
hunk budget, a single `coarse()` edit is retained instead of a fabricated
minimal edit list. Empty edits preserve raw-digest status.

## Source and coordinate invariants

For each actual step against the intermediate UTF-8 source:

1. `edit.startByte`/`edit.oldEndByte` and their zero-based `(row, byte-column)`
   points refer to the **intermediate** source receiving the edit.
2. `edit.newEndByte = edit.startByte + (currentEndByte - currentStartByte)`.
3. `edit.newEndPoint` is the point reached by inserting only that target
   source slice at `edit.startPoint`; it is not the full target generation's
   end point at that absolute hunk offset.
4. Replacing the old intermediate byte span by the target slice for every
   descending step reconstructs exactly the final admitted original UTF-8
   bytes, including supplementary Unicode, CR and CRLF cases.

These are Tree-sitter **shape-compatible** numbers, not an embedded Tree-sitter
API, parser resource identity, CST fidelity proof, or grant of compiler ABI
reuse. The existing one-envelope `utf8InputEditSince` API is unchanged.

## Qualification and remaining gates

The Synexia-owned source-sealed Maven/OpenRewrite recipe and tests produce the
Java implementation and target contract test as reviewable postimages. The
pure-Java contract replays sequential byte edits against independently decoded
intermediate sources and compares both old/new points; a private external
fixture also edits genuine Tree-sitter JSON trees and compares fresh versus
incremental parses. Tree-sitter JNI runtime and its jars are **test-only**
donors; no donor expressive implementation or binary enters the SDK.

Full original M3JDK21 receiver, compiler ABI dependency invalidation,
installed-image/HotSpot/JNI/jtreg/JCK, additional parser/language versions,
threaded ownership and performance remain independent **DRAFT/HOLD** gates.
