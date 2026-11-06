<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# Recipe PRECOMPUTE_JOINED_LANES v1 (2026-11-06)

Recovery and port of the four stacked Synexia M3FS precompute recipes onto the M3JDK owner
`jdk.m3.fs.M3FsPrecomputeBundle` (module `jdk.m3fs`, `m3/fs`), one commit per recipe, in the Synexia stacking order:
each Synexia preimage is the previous recipe's postimage (sha256 chain in the Synexia hash-pinned manifests:
`2bc2c108 -> eec030a3` history, `eec030a3 -> 7a531a19` memchr bitmap, `7a531a19 -> 31d0be0b` key/value lanes,
`31d0be0b -> 03c1d164` hash-lane cache; owner blobs `099634440b44 -> 32c9b0d01484 -> a34b8031a324 -> 162fd0b0b5b1 ->
cbda6a670661`, Synexia develop `b65df53330e3`).

The M3JDK copy diverged from the Synexia owner (renamed, laneX accessor helpers, packed `LaneTable`, ordinal-keyed
catalog masks, no joined lanes), so every Synexia hunk was re-sealed against the M3JDK file before its delta was
applied: `EDITS.tsv` pins the sha256 of the M3JDK preimage and postimage per stage and file, `stages/*.edits` holds
the anchored edits (the Synexia hunk, or the mapped hunk), `DECISIONS.tsv` the verdict per Synexia hunk.

| stage | Synexia crate | M3JDK receiver | verdict |
|---|---|---|---|
| 01-joined-directory-history | m3fs-joined-directory-history-20261102 (42466557a7f) | Bundle: Role +4 (appended), MappedView accessors, JoinedContentSegments, MappedJoinedContentSegments, JoinedBytePresence | DIVERGED->mapped; grepJoinedSegments x2 HANDOFF_45; Writer/Catalog targets DEFERRED |
| 02-memchr-bitmap | m3fs-memchr-bitmap-20261102 (27b0600d0a8 + m3_fs_jni.c text recipe) | Bundle: JoinedBytePresence.candidateRows zero-mask break; native m3_contains first-byte memchr skip | APPLICABLE->applied (both) |
| 03-key-value-lanes | m3fs-key-value-lanes-20261102 (489d837bb8d) | Bundle: openRawSegments LinkedHashMap -> private M3KeyValueSlots open-addressing lanes | APPLICABLE->applied |

`python -I apply.py [--check] [--through RECIPE] [--replay [--emit DIR]]` applies or checks the chain (refuses a
drifted preimage, second run changed=0). `qualification/differential.sh` compiles every stage image on its own and
compares the digests of `JoinedLanesDifferential` (performance stages must not change them);
`qualification/native-gate.sh` runs the relocated C oracle on the real old and new `m3_contains` under UBSan; the key/value
template of the Synexia crate runs on the `M3KeyValueSlots` class extracted from the stage image. `verify.sh` runs the
gates of `GATES.tsv`. Every edited declaration carries a `//FLLDCIM: MRL - <crate>` anchor.
