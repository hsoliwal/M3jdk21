<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3File shadow-stream / delta receiver

This is an M3JDK21 product change, not an M3 + JDK adapter.

One physical stream may feed hashing, parsing, indexing and other layered observers as bytes are
consumed. Observers do not reread the file and do not retain borrowed buffers.

The change preserves the original M3File read boundary and adds only overloads/types. M3File remains
provider-owned through Path/NIO; no competing filesystem tree, path identity or parser is introduced.

DOOP application: traversal is lazy; only demanded files matching the caller policy become M3File
objects. Roots may be independent/parallel, while provider directory walking remains owned by NIO.

Recipe-first: production/test Java postimages are pinned in the adjacent hash-pinned crate.
Future corrections append RECIPE_MEMORY.tsv and a regression guard instead of hand-patching outputs.
