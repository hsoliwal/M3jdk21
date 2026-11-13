<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Object bootstrap contract

## Admitted behavioral atom

The only executable change is the bootstrap base-vtable count in `ClassLoader::compute_Object_vtable()`. The six-slot count matches the actual compiled Object method census and the existing vtable-entry size. The renamed local constant and explanatory comment identify that dependency.

No Object source, public signature, adapter implementation, method-finality rule, wait registration, Klass allocation rule or mismatch guard changes. In particular, `toAdapter` remains overridable and no per-instance field or header is introduced. The constant-time bootstrap calculation retains its existing location before early array metadata allocation.

## Per-file transformation invariant

The existing TextSnapshot engine admits the exact target path and before SHA, validates the exact after resource SHA, and emits the whole target as an actual named-recipe Result. The after state is admitted only as a fixed point. The SDK gates cover both states, duplicates, missing and drifted source, unrelated-source retention, independent executions, late-source changes and template corruption.

Ten context-source hashes bind the proof to Object method/field definitions, method/vtable treatment, array metadata allocation, Universe initialization and native symbol registration. A future Object API or HotSpot layout change requires a new source/context evaluation; this recipe does not guess a count for unknown versions.

## Independent implementation evidence

The whole-owner verifier consumes the actual build command, preserves its strict warning and optimization options, and compiles both source versions. The recorded original/generated objects have identical defined symbols and addresses and differ in one immediate byte, 5 to 6. The actual built Object.class supplies the six-method and native wait0 census; the proof does not infer these from the new comment.

The actual-image fixture covers array dispatch, inherited/overridden adapter calls and unrelated Object operations, including monitor ownership and platform/virtual waits. Runtime modes and executable hashes are recorded independently. Full image packaging and CDS remain the parent integration gate.

## Known boundary

The original baseline's Object-startup failure was observed before recovery, but its lost transient crash log is not republished as fresh evidence. The retained pinned source, newly built Object census, strict original/generated owner compilation and fresh actual-image fixture are the independently reproducible evidence for this correction. No latency, RAM-speed or wider runtime correctness claim follows from this prerequisite.
