# M3 direct mask-position receiver qualification — 2026-10-18

Receives canonical Synexia recipe
`com.synexia.m3.M3IndexMaskPositionSelection20261018`
from `hsoliwal/com.synexia#12238`.

## Exact postimages

- `M3CountedBag.java`:
  `ffd6a7340fd60377443252fc52954d740bc758edea4d0778877531cf63976bad`
- `M3IndexMaskCompactor.java`:
  `e8b8c5e455d4b46bb85b5c4ee6c8234643ca6e10f8314a468f455a80067a7f7d`
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
