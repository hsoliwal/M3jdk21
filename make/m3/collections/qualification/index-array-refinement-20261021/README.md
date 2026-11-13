# M3 collection index-array refinement

Start with FINAL_REPORT.md. This is generated, bounded-qualified source and review
patches, **not all collections completed, not a new upstream commit and not a merge**.
The product is M3JDK21; the reusable recipe belongs in Synexia.

1. Inspect the source seals and `M3JDK21-index-array-refinement.patch`.
2. Run the focused canonical Maven crate (README under `canonical/`).
3. Compare emitted bytes to `product/SDK_RECEIPT.tsv`; never overwrite a foreign preimage.
4. The two `component-proof/` POMs reproduce bounded Java tests with JDK21 and Maven.
   They intentionally do not claim the original whole-module build/provider policy.
5. Build `baseline/m3_index_mask_compactor.c` with JDK21 JNI headers, C17,
   -Wall -Wextra -Werror -fsanitize=undefined -fno-sanitize-recover=all and -fPIC -shared.
   Run the recorded contract main with -Xcheck:jni and the resulting library path.
   The exact original runtime commands and arguments are in `evidence/RUNTIME_RECEIPTS.json`.

The reproduction-original-paths script records the executed layout; adjust only its
working-root path for a separate machine. No Maven cache, toolchain binary, compiled
class, JNI binary or hidden authentication data is included.

The whole collection programme remains pending as listed in REMAINING_SCOPE.tsv.
