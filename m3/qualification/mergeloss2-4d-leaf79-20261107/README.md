<!-- SPDX-License-Identifier: Apache-2.0 -->
# Merge-loss leaf 79: the shared JNI exception atom and the exact-address lexicon mapping

Origin e47eeda7c05 (2026-10-20) touched six libjava files and added `m3_jni_util.h`. A retain merge later kept the
first parent's copies, so the header never reached master. It also blocks merge-loss leaf 50
(`m3_index_mask_window.c` helpers, owner 0e).

This leaf restores the header and the six files.

- `m3_jni_util.h`: `m3_jni_throw_by_name`, with the same JNI operation order as each incumbent copy (FindClass,
  conditional ThrowNew, DeleteLocalRef). Five files replace their private copies with it; M3StringPool uses it
  for IOException.
- `M3StringPool.c`: the fixed-address lexicon mapping now also builds on macOS, and on Linux headers without
  `MAP_FIXED_NOREPLACE`.
  - It maps with `MAP_SHARED` (plus `MAP_FIXED_NOREPLACE` where defined).
  - It accepts only the exact requested address. On any other address it calls `munmap` and reports
    "Kernel did not honor the fixed M3 lexicon address".
  - On Linux with `MAP_FIXED_NOREPLACE` defined, the flags equal master's. The new check also catches kernels
    older than 4.17, which ignore the flag and treat the address as a hint.
- `M3RX7.c`: both include lines are kept (the add/add hunk).

Gates: GATES.tsv. Not run: a JVM-level probe of the throw paths (the helper is the same three calls).
