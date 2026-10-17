# Native sparse ordinal staging candidate

SPDX-License-Identifier: Apache-2.0
Copyright 2026 Hitesh Soliwal and contributors

The four-word bitmap candidate passed correctness checks but added 48 compiler-reported stack
bytes and increased the dense/distributed local timing samples in pass-0006. Native row-transfer
savings occurred only when entire chunks were rejected. Preserve that source and its measurements.

Evaluate the simpler representation before final receiving: the existing selected[256] array
first stores strictly increasing accepted offsets into the current row-ID buffer. After row
transfer, replace each accepted offset with row_ids[offset] in the same slot. Earlier overwritten
slots cannot affect later offset reads; row_ids and selected are distinct arrays. No new array,
bitmap, node or global state is introduced. Fully accepted chunks still emit row_ids directly,
rejected chunks transfer no row IDs, and mixed chunks transform only the accepted slots.

The input remains the same unsigned-byte required-bit mask. This is another physical encoding
of the current chunk selection, not different object-state semantics or a new public API.
Retain count-drift guards, input validation, stable row order and the original JNI symbol.
Qualify the recipe and Java/JNI corpus, add a wrong-offset-to-ID mutant, compare stack/transfer
counts and local timings, then select by executed evidence. No general speedup is presumed.
