# M3 direct mask-position receiver qualification — 2026-10-18

Receives canonical Synexia recipe
`com.synexia.m3.M3IndexMaskPositionSelection20261018`
from `hsoliwal/com.synexia#12238`.

## Exact postimages

- `M3CountedBag.java`:
  `2334edd988b5512c9ef6b7092a8ab5ac99e36df09629661ae3779f7090c5bc54`
- `M3IndexMaskCompactor.java`:
  `477135e13481942fef3b95b96a08d39c9c86dd92a649959c6d035b19b5784031`
- `m3_index_mask_compactor.c`:
  `8610e9fcedcc8eb698acbf3161bca8ea7d16194e61c66915f9e25fe47aae27f7`

The production change removes the always-on identity `int[]` input from
`M3CountedBag.selectedDistinct`. New Java and JNI selectors return logical
positions directly from the byte-state lane.

## Compatibility proof

New JNI binaries advertise `positionsAbiNative() == 1` and emit positions
directly. The workflow also builds the exact pre-change C source from base
commit `275b610f26c191cd70420172ac65b634a8a4df49` and runs current Java against
that old library. The missing new symbol must be detected lazily and the
existing native selector must remain the `NATIVE_REQUIRED` fallback.

The old fallback is compatibility only; current binaries do not allocate or
transfer the identity input.

## Problem catalogue

Stable mask compaction / selected-position extraction. LeetCode 283,
HackerRank Java BitSet and GeeksforGeeks stable partition are taxonomy only.
Implementation remains first-principles on the existing M3 Java/JNI owner.
