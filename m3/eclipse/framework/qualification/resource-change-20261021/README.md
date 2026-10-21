<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Resource change snapshots in M3JDK21

This refinement extends the existing Eclipse resource snapshot owner. The original
`capture(root, maxNodes)` method retains its method body, callbacks, traversal,
identity checks, exceptions and original node references. Existing test files are
unchanged. Callers opt into `captureChanges(root, maxNodes)` to obtain full-width
captured flags and a compact work-state byte.

The implementation borrows the full-flag capture idea from target PR1588, pinned at
`520518332206a6ac5a000402ba4b11738e2369a7`; its source blob is
`d88dddd67563d3fcd621e3663892fa1d18ccd152`. Direct copying was rejected because that
candidate introduces `getFlags` callbacks into legacy capture and stores Java int
flags in an eight-byte long lane. This adaptation leaves the old operation intact
and adds exact-size `int[]` and `byte[]` metadata after successful traversal. The
additional retained payload is five bytes per row plus array/object headers.

## Contracts and replaceable atoms

| Atom | Contract |
| --- | --- |
| Existing capture | Breadth-first original references, default Eclipse child visibility, bounded rows, identity-only duplicate/cycle refusal. |
| Metadata capture | One `getFlags` and `getResource` call per row, then one resource type read and one filename read for files. All 32 flag bits survive. |
| Work classification | Eight unsigned bits nominate Java source, class file, build descriptor, container, content reconsideration, removal, context reconsideration and descendant invalidation. |
| Parent nomination | A changed descriptor nominates its parent subtree before propagation so siblings receive consistent context independent of child order. |
| Context propagation | One forward pass over parent-before-child ordinals. A folder-to-file TYPE change still nominates former descendants. |
| Full flag selection | Immutable original references in breadth-first order; no narrowing; required/excluded overlap selects nothing. |
| Work ordinal selection | Reuse existing `M3IndexMaskCompactor` Java/JNI contract. Detached ascending `int[]`; native-required never silently changes to Java. |

Descriptor detection is deliberately documented in the API as a finite,
case-sensitive nomination list. It is not an exhaustive source inventory. A flag
such as CONTENT can report a touch rather than changed bytes. Compiler/build
owners retain dependency analysis, content authority and execution decisions.
The existing M3File and M3AST owners are not duplicated or replaced by this class.

Existing API descriptors and behavior remain; new constants and methods are
additive API visible to reflection. Legacy snapshots allocate no per-row flags
or work-state payload, and attempts to use the new metadata accessors on one
fail explicitly with `IllegalStateException`.

## Recipe and validation

Canonical authoring lives in Synexia's
`synexia-openrewrite-recipes/recipe-crates/m3-integrated-refinement-20261021`.
Its target execution mirror is
`m3/runtime-integration/recipes/m3-integrated-refinement-20261021/canonical`.
The unchanged Java/text snapshot engines enforce exact preimages, postimages,
refusal and fixed-point replay. No Synexia runtime component is introduced.

`M3EclipseResourceChangeSnapshotTest` adds nine tests for legacy callback
preservation, sign-bit flags, frozen metadata, case-sensitive classification,
sibling and ancestor invalidation, folder-to-file changes, traversal refusal,
callback failures and all 65,536 required/excluded byte-mask pairs against an
independent ordinal oracle. Set `-Dm3.resource.requireNative=true` with the
qualified existing mask JNI library for mandatory native parity. A separate JVM
without that library proves explicit unavailable behavior.

The current test results and exact commands belong to the fresh task receipts.
Test descriptions here are specifications, not a claim of full-image acceptance.
Real Eclipse Maven interfaces are used; dynamic proxies exercise the documented
callback boundary, so these tests do not substitute for an Equinox workspace
launch, a full JDK image, or application-scale performance measurements.

The searchable decisions are in
`m3/collections/algorithm-catalog/resource-change-20261021.tsv`. GitHub source was
evaluated under its existing Apache-2.0 rights. LeetCode, HackerRank and
GeeksforGeeks entries are problem-family references only; no unlicensed solution
or editorial code was copied.
