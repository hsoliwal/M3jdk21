# Filtered wrapper reconciliation and bounded mask projection

The pinned product baseline contains four duplicate Java 21 declarations in
`M3FilteredViews`: sorted reversal, two endpoint removals, and navigable reversal.
The receiving change removes the older duplicates and keeps the later reconciled
predicate pipeline. It also preserves the subsequent comparator-aware splitting
enhancement. Eight unchanged suites pass against the historical reconciled source
and the candidate (1,653,638 checks each); the comparator-split suite adds 6,324
checks on the candidate. Their stable-read traces agree. The outer public class
API is unchanged. This repairs compilation; it is not a speed claim.

`M3MaskViewportKernel` and its existing JNI counterpart now distinguish disjoint
and overlapping physical windows when arrays alias. The native implementation
reads bounded regions instead of acquiring entire parent arrays. Its validation
pass completes before writes; overlapping aliases retain a selected-result
snapshot. Logical offsets, duplicates, sorted/permuted order and every state bit
are preserved. These internal kernels are qualified independently; this receipt
does not claim they are wired into every collection wrapper.

Reproduce the mandatory Java/JNI and UBSan checks on Linux with a JDK 21 and GCC:

```sh
python3 m3/collections/verify_mask_viewport.py \
  --jdk /absolute/path/to/jdk21 --out /absolute/path/to/fresh-proof
```

The original 144,449-check oracle and the new 7,610-check window/alias oracle pass
on both original and refined kernels. Instrumented JNI confirms bounded reads,
zero result allocation for disjoint outputs, and no writes on late validation
failure. Public native symbols are unchanged. This is a proven allocation bound,
not a throughput measurement.

Canonical recipe ownership remains in Synexia's existing
`synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017`.
The task `mask-window-jdt-20261019` retains contracts, pre/post images, actual
OpenRewrite execution, fixed-point/refusal checks and receiver evidence. A narrow
recipe-owner refinement admits the existing `m3maskviewport.c` path; other source
admission rules are preserved. Catalogue decisions are in
`algorithm-catalog/mask-viewport-regions-20261019.tsv` and
`algorithm-catalog/filtered-reconciliation-20261019.tsv`.

This finite pass does not complete all collection families, advance the product's
String-first promotion phase, or qualify a full source-built M3JDK21 image.
